package net.mfjassociates.jai;

import static net.mfjassociates.jai.PreferencesController.DISPLAY_COMPRESSION_PREF;
import static net.mfjassociates.jai.PreferencesController.RECOMMENDED_DISPLAY_QUALITY;
import static net.mfjassociates.jai.PreferencesController.RECOMMENDED_JPEG_QUALITY;
import static net.mfjassociates.jai.PreferencesController.SAVE_COMPRESSION_PREF;
import static net.mfjassociates.jai.util.ImageHandler.saveImage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.util.Pair;
import net.mfjassociates.jai.util.ImageHandler;

public class ImageUtilController {
	
	private static final String HOME_DIR = System.getProperty("user.home");
	
	// preferences
	private static final String LAST_DIRECTORY_PREF = "last_directory";
    private Preferences userPreferences = Preferences.userNodeForPackage(this.getClass());
	
	@FXML private ImageView imageView;
	@FXML private Label base64Label;
	@FXML private Label statusMessageLabel;
	private byte[] image_bytes=null;
	private String imageName=null;
	private PreferencesController preferencesController=null;
	private float saveCompression=userPreferences.getFloat(SAVE_COMPRESSION_PREF, RECOMMENDED_JPEG_QUALITY);
	private float displayCompression=userPreferences.getFloat(DISPLAY_COMPRESSION_PREF, RECOMMENDED_DISPLAY_QUALITY);
	private ExtensionFilter[] imageIOBasedExtensionFilters=createImageIOBasedExtensionFilter();

	private ExtensionFilter selectedExtensionFilter=null;
	
	@FXML
	private void initialize() {
		
	}

	@FXML private void closeFired(ActionEvent event) {
		Platform.exit();
	}
	
	private Object createControllerForType(Class<?> type) {
		if (preferencesController==null) {
			preferencesController=new PreferencesController(userPreferences);
		}
		return preferencesController;
	}

	@FXML private void preferencesFired(ActionEvent event) throws IOException {
		// old way to do dialogs
		/*Stage dialogStage=new Stage();
		FXMLLoader loader=new FXMLLoader(getClass().getResource("Preferences.fxml"));
		loader.setControllerFactory(this::createControllerForType);
		Parent dialog = loader.load();
		dialogStage.setScene(new Scene(dialog));
		dialogStage.initOwner(imageView.getScene().getWindow());
		dialogStage.showAndWait();*/
		// new way to do dialog (post 8u40 update)
		Dialog<Pair<Float, Float>> dialog = new Dialog<>();
		FXMLLoader loader=new FXMLLoader(getClass().getResource("Preferences.fxml"));
		loader.setControllerFactory(this::createControllerForType);
		DialogPane dialogPane = loader.load();
		dialog.setDialogPane(dialogPane);
		PreferencesController pc=(PreferencesController) loader.getController();
		pc.setDialog(dialog);
		Optional<Pair<Float, Float>> result=null;
		try {
			 result = dialog.showAndWait();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (result.isPresent()) {
			Float sc = result.get().getKey();
			Float dc = result.get().getValue();
			if (sc!=null) saveCompression=sc;
			if (dc!=null) {
				displayCompression=dc;
				setupImage();
			}
//			System.out.println(String.format("save compression=%1$f, display compression=%2$f.", result.get().getKey(), result.get().getValue()));
		}
	}
	
	@FXML private void openFired(ActionEvent event) throws IOException {
		File imageFile=configureFileChooser("Open Image File", imageView.getScene().getWindow(), DIALOG_TYPE.open);
		if (imageFile!=null) {
			setupImage(imageFile);
		}
	}
	private void setupImage(File imageFile) {
		try {
			image_bytes=Files.readAllBytes(imageFile.toPath()); // remember last read image
			imageName=imageFile.getName();
			setupImage();
		} catch (NoSuchElementException | IOException e) {
			e.printStackTrace();
			String errorMessageFormat="Unable to process file '%1$s'";
			System.err.println(String.format(errorMessageFormat, imageFile.getAbsolutePath()));
			statusMessageLabel.setText(String.format(errorMessageFormat, imageFile.getName()));
			return;
		}
	}
	/**
	 * Setup the image represented by the saved byte array image_bytes into the imageView and
	 * base64Label.
	 * @throws IOException if there is an error reading from the input stream
	 * @throws NoSuchElementException if there are no readers found that can handle this input stream
	 */
	private void setupImage() throws IOException {
		if (image_bytes==null) return;
		ByteArrayInputStream bais = new ByteArrayInputStream(image_bytes);
		ImageInputStream imageis = ImageIO.createImageInputStream(bais);
		String formatName=null;
		ImageReader reader=null;
		String imageSize=String.format("%1$,d", image_bytes.length);
		reader=ImageIO.getImageReaders(imageis).next();
		formatName=reader.getFormatName();

		reader.setInput(imageis);
		BufferedImage image = reader.read(0);
		// convert to jpg for display as jp2 is not viewable or convert if display compression in prefs not 1.0
		if ("jpeg 2000".equals(formatName) || !ImageHandler.equals(displayCompression, 1.0f, 4)) {
			IIOImage iioImage = new IIOImage(image, null, null);
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
			ByteArrayOutputStream baos=new ByteArrayOutputStream();
			ImageOutputStream imageos = ImageIO.createImageOutputStream(baos);
			writer.setOutput(imageos);
			JPEGImageWriteParam iqp = new JPEGImageWriteParam(null);
			iqp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			iqp.setCompressionType(iqp.getCompressionTypes()[0]);
			iqp.setCompressionQuality(displayCompression);
//			if (ImageHandler.equals(displayCompression, 1.0f, 4)) {
//				iqp=null; // no compression
//			}
			writer.write(null, iioImage, iqp);
			imageos.close();
			writer.dispose();
			imageSize=String.format("%1$,d(viewed jpg %2$,d)", image_bytes.length, baos.toByteArray().length);
		}
		imageis.close();
		imageView.setImage(SwingFXUtils.toFXImage(image, null));
		String base64String=new String(Base64.getEncoder().encode(image_bytes));
		base64Label.setText(base64String);
		statusMessageLabel.setText(String.format("Image %1$s: image size=%2$s, base64 size=%3$,d, type=%4$s, compression=%5$f", imageName, imageSize, base64String.length(), formatName, displayCompression));
	}

	@FXML private void saveFired(ActionEvent event) {
	}
	
	@FXML private void saveAsFired(ActionEvent event) throws IOException {
		File imageFile=configureFileChooser("Save Image As File", imageView.getScene().getWindow(), DIALOG_TYPE.saveAs);
		String outFormatName=selectedExtensionFilter.getDescription();
		if (imageFile!=null) {
			String compressionType = saveImage(imageFile, outFormatName, image_bytes, saveCompression);
			String imageSize=String.format("%1$,d(%2$,d saved)", image_bytes.length, imageFile.length());
			statusMessageLabel.setText(String.format("Image %1$s: image size=%2$s, type=%3$s, compression=%4$f, compression type=%5$s", imageFile.getName(), imageSize, outFormatName, saveCompression, compressionType));
		}
	}

	private enum DIALOG_TYPE {
		open, saveAs;
	}
	
	/**
	 * Call the {@link javafx.stage.FileChooser#showOpenDialog(Window)} or
	 * {@link javafx.stage.FileChooser#showSaveDialog(Window)} for an image
	 * file.  Configure the file chooser using the parameters and set the
	 * extension filters to the list of extensions supported by the current
	 * ImageIO registry.
	 * 
	 * @param title - title of the window
	 * @param window - parent window
	 * @param dialogType - either open or saveAs
	 * @return the file selected for open or save or null if none
	 * @throws IOException
	 */
	private File configureFileChooser(String title, Window window, DIALOG_TYPE dialogType) throws IOException {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialDirectory(new File(userPreferences.get(LAST_DIRECTORY_PREF, HOME_DIR)));
        if (dialogType == DIALOG_TYPE.saveAs) {
            if (imageName!=null && !imageName.isEmpty()) fileChooser.setInitialFileName(imageName);
            // set to last opened file type
            fileChooser.setSelectedExtensionFilter(computeSelectedExtensionFilter(imageName));
        }
        fileChooser.getExtensionFilters().addAll(
        		imageIOBasedExtensionFilters
//            new FileChooser.ExtensionFilter("All Images", "*.*"),
//            new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg"),
//            new FileChooser.ExtensionFilter("JP2", "*.jp2"),
//            new FileChooser.ExtensionFilter("BMP", "*.bmp"),
//            new FileChooser.ExtensionFilter("GIF", "*.gif"),
//            new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        File userFile=null;
        try {
        	switch (dialogType) {
			case open:
	            userFile=fileChooser.showOpenDialog(window);
				break;

			case saveAs:
	            userFile=fileChooser.showSaveDialog(window);
				break;
			}
		} catch (IllegalArgumentException e) {
	        fileChooser.setInitialDirectory(new File(HOME_DIR));                 
			// Try again
        	switch (dialogType) {
			case open:
	            userFile=fileChooser.showOpenDialog(window);
				break;

			case saveAs:
	            userFile=fileChooser.showSaveDialog(window);
				break;
			}
		}
        if (userFile!=null) {
        	String newLastDirectory=userFile.getParentFile().getAbsolutePath();
        	if (!fileChooser.getInitialDirectory().getAbsolutePath().equals(newLastDirectory)) {
        		// changed directory, remember it
        		userPreferences.put(LAST_DIRECTORY_PREF, newLastDirectory);
        	}
        	if (dialogType==DIALOG_TYPE.saveAs) selectedExtensionFilter=fileChooser.getSelectedExtensionFilter();
        }
		
		return userFile;
	}

	/**
	 * Computer the FileChooser.Extension based on the filename
	 * @param anImageName - image filename
	 * @return - the matching FileChooser.Extension or null if not found
	 */
	private ExtensionFilter computeSelectedExtensionFilter(String anImageName) {
		for (ExtensionFilter extensionFilter : imageIOBasedExtensionFilters) {
			List<String> extensions = extensionFilter.getExtensions();
			for (String extension : extensions) {
				if (anImageName.endsWith(extension.substring(2))) return extensionFilter;
			}
		}
		return null;
	}

	private ExtensionFilter[] createImageIOBasedExtensionFilter() {
		Set<ExtensionFilter> filters=new LinkedHashSet<ExtensionFilter>();
		filters.add(new FileChooser.ExtensionFilter("All Images", "*.*"));
		IIORegistry registry = IIORegistry.getDefaultInstance();
		Iterator<ImageReaderSpi> readersSpi = registry.getServiceProviders(ImageReaderSpi.class, false);
		for (Iterator<ImageReaderSpi> iterator = readersSpi; iterator.hasNext();) {
			ImageReaderSpi readerSpi = iterator.next();
			String[] suffixes = readerSpi.getFileSuffixes();
			Set<String> extensions=new LinkedHashSet<String>();
			for (String suffix : readerSpi.getFileSuffixes()) {
				if (suffix==null || suffix.isEmpty()) suffix="*"; // make it *.* as file extension
				extensions.add("*."+suffix);
			}
			filters.add(new FileChooser.ExtensionFilter(readerSpi.getFormatNames()[0], extensions.toArray(new String[]{})));
		}
		
		return filters.toArray(new ExtensionFilter[]{});
	}
	
}
