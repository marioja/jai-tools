package net.mfjassociates.jai;

import static net.mfjassociates.jai.util.ImageHandler.saveImage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
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
import javax.xml.transform.TransformerException;

import org.apache.commons.math3.util.Precision;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.jfif.JfifDirectory;
import com.sun.javafx.iio.ImageStorageException;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import net.mfjassociates.fx.FXUtils.ProgressResponsiveTask;
import net.mfjassociates.fx.FXUtils.ResponsiveTask;
import net.mfjassociates.jai.PreferencesController.JaiPreferences;
import net.mfjassociates.jai.util.ImageHandler.BasicImageInformation;

public class ImageUtilController {
	
	private static final String HOME_DIR = System.getProperty("user.home");
	
	// preferences
	private static final String LAST_DIRECTORY_PREF = "last_directory";

	private static final String APPLICATION_INFORMATION = "Application Information";
    private Preferences userPreferences = Preferences.userNodeForPackage(this.getClass());
	
	@FXML private ImageView imageView;
	@FXML private Label statusMessageLabel;
	@FXML private ProgressBar progressBar;
	@FXML private RadioMenuItem metadataMenu;
	@FXML private RadioMenuItem base64Menu;
	@FXML private Label resolutionLabel;
	@FXML private ScrollPane leftsp;
	private byte[] imageBytes=null;
	private InputStream bais=null;
	private String imageName=null;
	private PreferencesController preferencesController=null;
	private JaiPreferences jaiPrefs=new JaiPreferences(userPreferences);
	// private float saveCompression=userPreferences.getFloat(SAVE_COMPRESSION_PREF, RECOMMENDED_JPEG_QUALITY);
	// private float displayCompression=userPreferences.getFloat(DISPLAY_COMPRESSION_PREF, RECOMMENDED_DISPLAY_QUALITY);
	private ExtensionFilter[] imageIOBasedExtensionFilters=createImageIOBasedExtensionFilter();

	private ExtensionFilter selectedExtensionFilter=null;
	private Label base64Label=new Label();
	private GridPane metadataGridPane=new GridPane();

	private StringProperty base64String=new SimpleStringProperty();
//	private BooleanProperty noMetadata=new SimpleBooleanProperty();
	private IntegerProperty xdensity=new SimpleIntegerProperty(-1);
	private IntegerProperty ydensity=new SimpleIntegerProperty(-1);
	
	@FXML
	private void initialize() {
		progressBar.managedProperty().bind(progressBar.visibleProperty());

		metadataGridPane.hgapProperty().set(3);
		metadataGridPane.vgapProperty().set(3);
		metadataGridPane.visibleProperty().set(false);
		
		metadataMenu.disableProperty().bind(metadataGridPane.visibleProperty().not());
		
		base64Label.setWrapText(true);
		base64Label.textProperty().bind(base64String);
		
		leftsp.setContent(base64Label);
		
		resolutionLabel.textProperty().bind(new StringBinding(){
			
			{super.bind(xdensity, ydensity);}

			@Override
			protected String computeValue() {
				String result=null;
				if (xdensity.get()!=-1 && ydensity.get()!=-1) {
					result=String.format(" xres: %1$d, yres: %2$d", xdensity.get(), ydensity.get());
				}
				return result;
			}
			
		});
	}
	
	// Property functions
	
	// base64String
	public final String getBase64String() {return base64String.get();}
	public final void setBase64String(String aMetadata) {base64String.set(aMetadata);}
	public final StringProperty base64StringProperty() {return base64String;}

	// xdensity
	public final int getXdensity() {return xdensity.get();}
	public final void setXdensity(int anXdensity) {xdensity.set(anXdensity);}
	public final IntegerProperty xdensityProperty() {return xdensity;}

	// ydensity
	public final int getYdensity() {return ydensity.get();}
	public final void setYdensity(int aYdensity) {ydensity.set(aYdensity);}
	public final IntegerProperty ydensityProperty() {return ydensity;}

	@FXML private void closeFired(ActionEvent event) {
		Platform.exit();
	}
	
	private Object createControllerForType(Class<?> type) {
		if (preferencesController==null) {
			preferencesController=new PreferencesController(userPreferences, imageView);
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
		Dialog<JaiPreferences> dialog = new Dialog<>();
		FXMLLoader loader=new FXMLLoader(getClass().getResource("Preferences.fxml"));
		loader.setControllerFactory(this::createControllerForType);
		DialogPane dialogPane = loader.load();
		dialog.setDialogPane(dialogPane);
		PreferencesController pc=(PreferencesController) loader.getController();
		pc.setDialog(dialog);
		Optional<JaiPreferences> result=null;
		try {
			 result = dialog.showAndWait();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (result.isPresent()) {
			   jaiPrefs=result.get();
			//   Float sc = result.get().saveCompression.get();
			//   Float dc = result.get().displayCompression.get();
			//   if (sc!=null) saveCompression=sc;
			   if (jaiPrefs.getDisplayCompression().isModified()) {
				setupImageTask();
			}
//			System.out.println(String.format("save compression=%1$f, display compression=%2$f.", result.get().getKey(), result.get().getValue()));
		}
	}
	@FXML private void aboutFired(ActionEvent event) {
		Package p = getClass().getPackage();
		Alert alert=new Alert(AlertType.INFORMATION, String.format("Application Name: %1$s\nVendor: %2$s\nVersion: %3$s",p.getImplementationTitle(), p.getImplementationVendor(), p.getImplementationVersion()));
		alert.setHeaderText(APPLICATION_INFORMATION);
		alert.setTitle("About");
		alert.showAndWait();
	}
	@FXML private void base64Fired(ActionEvent event) {
		Platform.runLater(() -> leftsp.setContent(base64Label));
	}
	@FXML private void metadataFired(ActionEvent event) {
		Platform.runLater(() -> leftsp.setContent(metadataGridPane));
	}
	@FXML private void openFired(ActionEvent event) throws IOException {
		File imageFile=configureFileChooser("Open Image File", imageView.getScene().getWindow(), DIALOG_TYPE.open);
		if (imageFile!=null) {
			setupImage(imageFile);
		}
	}
	
	private void setupImage(File imageFile) {
		ProgressResponsiveTask<byte[], IOException> readImageFileTask=new ProgressResponsiveTask<byte[], IOException>(
				rt -> { // succeeded
					imageBytes=rt.getValue();
					// ByteArrayInputStream will mark position 0 when created which is what we want here for reset
					bais=new ByteArrayInputStream(imageBytes); // remember input stream, only need to reset if required more than once on same data
					imageName=imageFile.getName();
					setupImageTask();
					progressBar.setVisible(false);
					progressBar.progressProperty().unbind();
					return false;// do not reset cursor, more to process
				}, 
				rt -> {// failed
					if (rt.getException()!=null) {
						statusMessageLabel.setText(rt.getException().getMessage());
					}
					progressBar.setVisible(false);
					progressBar.progressProperty().unbind();
					return null;// Void
				}, 
				rt -> { // long running read file method with calls to myUpdateProgress
					int length=Integer.MAX_VALUE;
					BufferedInputStream fis=new BufferedInputStream(new FileInputStream(imageFile));
					ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);

					byte[] buffer = new byte[4096];
					int totalBytes = 0, readBytes;
					do {
						readBytes = fis.read(buffer, 0, Math.min(buffer.length, length - totalBytes));
						totalBytes += Math.max(readBytes, 0);
						if (readBytes > 0) {
							baos.write(buffer, 0, readBytes);
						}
						rt.myUpdateProgress(totalBytes);
					} while (totalBytes < length && readBytes > -1);

					if (length != Integer.MAX_VALUE && totalBytes < length) {
						throw new IOException("unexpected EOF");
					}
					
					return baos.toByteArray();
				}, 
				imageView.getScene(),
				imageFile.length()
		);
		progressBar.setProgress(0d);
		progressBar.progressProperty().bind(readImageFileTask.progressProperty());
		progressBar.setVisible(true);
		Thread readImageFileThread=new Thread(readImageFileTask);
		readImageFileThread.setDaemon(true);
		readImageFileThread.start();
		/*image_bytes=Files.readAllBytes(imageFile.toPath()); // remember last read image
		// ByteArrayInputStream will mark position 0 when created which is what we want here for reset
		bais=new ByteArrayInputStream(image_bytes); // remember input stream, only need to reset if required more than once on same data
		imageName=imageFile.getName();
		setupImageTask();
		try {
		} catch (NoSuchElementException | IOException e) {
			e.printStackTrace();
			String errorMessageFormat="Unable to process file '%1$s'";
			statusMessageLabel.setText(String.format(errorMessageFormat, imageFile.getName()));
			return;
		}*/
	}
	private static class ImageInformation {
		
		public ImageInformation(Image aFxImage, String aFormatName, String anImageSize, Metadata aMetadata) {
			this.fximage=aFxImage;
			this.formatName=aFormatName;
			this.imageSize=anImageSize;
			this.metadata=aMetadata;
		}
		public Image fximage;
		public String formatName;
		public String imageSize;
		public Metadata metadata;
	}

	private void loadBase64Task(ImageInformation ii) {
		Task<String> loadBase64Task = new Task<String>() {

			@Override
			protected String call() throws Exception {
				Platform.runLater(() ->statusMessageLabel.setText("Creating base 64 string..."));
				return new String(Base64.getEncoder().encode(imageBytes));
			}
			
		};
		loadBase64Task.setOnSucceeded(event -> /*Platform.runLater(()->*/{
			try {
				String b64=loadBase64Task.get();
				base64String.set(b64);
				statusMessageLabel.setText(
						String.format("Image %1$s: image size=%2$s, base64 size=%3$,d, type=%4$s, compression=%5$f",
								imageName, ii.imageSize, loadBase64Task.get().length(), ii.formatName, jaiPrefs.getDisplayCompression().get()));
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		})/*)*/;
		ResponsiveTask<String, IOException> loadBase64Task2 = new ResponsiveTask<String, IOException>(
				rt->{ // succeeded callback, by returning true waiting cursor will be reset back to default
					base64String.set(rt.getValue());
					statusMessageLabel.setText(
							String.format("Image %1$s: image size=%2$s, base64 size=%3$,d, type=%4$s, compression=%5$f",
									imageName, ii.imageSize, rt.getValue().length(), ii.formatName, jaiPrefs.getDisplayCompression().get()));
					return true; // reset cursor to default (no longer waiting)
				},
				rt->{ // failed callback
					if (rt.getException()!=null) {
						statusMessageLabel.setText(rt.getException().getMessage());
					}
					return null;// Void
				},
				()->{
					Platform.runLater(() ->statusMessageLabel.setText("Creating base 64 string..."));
					return new String(Base64.getEncoder().encode(imageBytes));
				}, // ThrowingSupplier, this will return the String
				imageView.getScene() // scene to set the cursor to wait and back to default.
			);
		Thread base64Thread=new Thread(loadBase64Task);
		base64Thread.setDaemon(true);
		base64Thread.start();
	}
	private void setupImageTask() {
		ResponsiveTask<ImageInformation, IOException> setupImageTask = new ResponsiveTask<ImageInformation, IOException>(
			rt->{ // succeeded callback, by returning true waiting cursor will be reset back to default
				ImageInformation ii = rt.getValue();
				if (ii==null) return true; // no image yet displayed
				populateMetadata(ii.metadata);
				if (metadataGridPane.isVisible()) {
				} else {
					metadataMenu.setSelected(false);
					base64Menu.setSelected(true);
				}
				
				imageView.setImage(ii.fximage);
				statusMessageLabel.setText(
						String.format("Image %1$s: image size=%2$s, base64 size=%3$,d, type=%4$s, compression=%5$f",
								imageName, ii.imageSize, 0, ii.formatName, jaiPrefs.getDisplayCompression().get()));
				loadBase64Task(ii);
				return true; // reset cursor to default (no longer waiting)
			},
			rt->{ // failed callback
				if (rt.getException()!=null) {
					statusMessageLabel.setText(rt.getException().getMessage());
				}
				return null;// Void
			},
			()->setupImage(), // ThrowingSupplier, this will return the ImageInformation object
			imageView.getScene() // scene to set the cursor to wait and back to default.
		);
		imageView.setImage(null);
		base64String.set(null);
		metadataGridPane.visibleProperty().set(false);
		Thread setupThread=new Thread(setupImageTask);
		setupThread.setDaemon(true);
		setupThread.start();
	}
	
	private static class ModifyiableLabel extends Label {
		public ModifyiableLabel(String message) {
			super(message);
			this.setWrapText(true);
		}
	}

	/**
	 * Populate the image metadata into the metadataGridPane.
	 * 
	 * @param metadata
	 */
	private void populateMetadata(Metadata metadata) {
		if (metadata==null) {
			metadataGridPane.visibleProperty().set(false);
			return;
		}
		int row=0;
		metadataGridPane.getChildren().clear();
		metadataGridPane.add(new ModifyiableLabel("Directory"), 0, row);
		metadataGridPane.add(new ModifyiableLabel("Tag Id"), 1, row);
		metadataGridPane.add(new ModifyiableLabel("Tag Name"), 2, row);
		metadataGridPane.add(new ModifyiableLabel("Extracted Value"), 3, row);
		row++;
		for (Directory dir : metadata.getDirectories()) {
			for (Tag tag : dir.getTags()) {
				metadataGridPane.add(new ModifyiableLabel(tag.getDirectoryName()), 0, row);
				metadataGridPane.add(new ModifyiableLabel(tag.getTagTypeHex()), 1, row);
				metadataGridPane.add(new ModifyiableLabel(tag.getTagName()), 2, row);
				metadataGridPane.add(new ModifyiableLabel(tag.getDescription()), 3, row);
				row++;
			}
			if (dir.hasErrors()) {
				for (String error : dir.getErrors()) {
					Label errorLabel=new ModifyiableLabel(error);
					GridPane.setColumnSpan(errorLabel, 4);
					metadataGridPane.add(errorLabel, 0, row);
					row++;
				}
			}
		}
		JfifDirectory jfif = metadata.getFirstDirectoryOfType(JfifDirectory.class);
		if (jfif!=null) {
			try {
				xdensityProperty().set(jfif.getInt(JfifDirectory.TAG_RESX));
				ydensityProperty().set(jfif.getInt(JfifDirectory.TAG_RESY));
			} catch (MetadataException e) {
			}
		}
		metadataGridPane.visibleProperty().set(true);
	}

	/**
	 * Setup the image represented by the saved byte array image_bytes into the imageView and
	 * base64Label.
	 * @throws IOException if there is an error reading from the input stream
	 * @throws ImageProcessingException 
	 * @throws TransformerException 
	 * @throws NoSuchElementException if there are no readers found that can handle this input stream
	 */
	@SuppressWarnings("restriction")
	private ImageInformation setupImage() throws IOException {
		if (imageBytes==null) return null;

		Image fximage=null;
		Metadata localMetadata=null;
		
		// reset width and height to default (0)
		jaiPrefs.getResizeWidth().reset();
		jaiPrefs.getResizeHeight().reset();

		bais.reset(); // always reset in case setupImage() is being called on same image

		// read metadata
		try {
			bais.reset();
			localMetadata = ImageMetadataReader.readMetadata(bais);
		} catch (ImageProcessingException e) {
		} finally {
			bais.reset();
		}

		// we always need ImageIO to determine filetype
		ImageInputStream imageis = ImageIO.createImageInputStream(bais);
		final String formatName;
		ImageReader reader=null;
		String imageSize=String.format("%1$,d", imageBytes.length);
		reader=ImageIO.getImageReaders(imageis).next();
		reader.setInput(imageis);
		formatName=reader.getFormatName();
		BasicImageInformation ii=new BasicImageInformation();
		Platform.runLater(() -> {
			xdensity.set(ii.xdensity);
			ydensity.set(ii.ydensity);
		});
		if (Precision.equals(jaiPrefs.getDisplayCompression().get(), 1.0f, 4)) {// no compression
			bais.reset();
			Platform.runLater(() -> statusMessageLabel.setText("Loading image..."));
			fximage = new Image(bais); // use javafx image processing
			if (fximage.isError()) {
				if (fximage.getException() instanceof ImageStorageException) { // use ImageIO since javafx failed
					bais.reset();
					fximage = SwingFXUtils.toFXImage(reader.read(0), null);
				} else {
					throw new RuntimeException("Unexpected error from JavaFX Image conversion: "+fximage.getException().getLocalizedMessage(), fximage.getException());
				}
			}
		} else { // regenerate fximage using displayCompression
			Platform.runLater(() -> statusMessageLabel.setText("Loading image..."));
			IIOImage iioImage = new IIOImage(reader.read(0), null, null);
			imageis.close();
			reader.dispose();
			
			Platform.runLater(() -> statusMessageLabel.setText(String.format("Changing compression to %1$d...",jaiPrefs.getDisplayCompression().get())));
			// write new JPEG image with displayCompression
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
			ByteArrayOutputStream baos=new ByteArrayOutputStream();
			ImageOutputStream imageos = ImageIO.createImageOutputStream(baos);
			writer.setOutput(imageos);
			JPEGImageWriteParam iqp = new JPEGImageWriteParam(null);
			iqp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			iqp.setCompressionType(iqp.getCompressionTypes()[0]);
			iqp.setCompressionQuality(jaiPrefs.getDisplayCompression().get());
			writer.write(null, iioImage, iqp);
			imageos.close();
			writer.dispose();
			
			// create fximage from new JPEG compressed stored in ByteArrayOutputStream
			imageis = ImageIO.createImageInputStream(new ByteArrayInputStream(baos.toByteArray()));
			reader=ImageIO.getImageReaders(imageis).next();
			reader.setInput(imageis);
			fximage = SwingFXUtils.toFXImage(reader.read(0), null);
			imageSize=String.format("%1$,d(compressed jpg %2$,d)", imageBytes.length, baos.toByteArray().length);
		}
		
		imageis.close();
		reader.dispose();
//		Platform.runLater(() -> statusMessageLabel.setText("Creating base 64 string..."));
//		String base64String2 = new String(Base64.getEncoder().encode(imageBytes));
		Platform.runLater(() -> statusMessageLabel.setText(statusMessageLabel.getText()+"Done"));
		return new ImageInformation(fximage, formatName, imageSize, localMetadata);
	}

	@FXML private void saveFired(ActionEvent event) {
		Alert alert=new Alert(AlertType.INFORMATION, "Not implemented yet");
		alert.showAndWait();
	}
	
	@FXML private void pasteBase64Fired(ActionEvent event) {
		try {
			imageBytes=Base64.getDecoder().decode(Clipboard.getSystemClipboard().getString());
			bais=new ByteArrayInputStream(imageBytes);
			imageName="*Clipboard*";
			setupImageTask();
		} catch (RuntimeException e) {
			statusMessageLabel.setText(String.format("Error while pasting from clipboard: %1$s", e.getLocalizedMessage()));
		}
	}

	@FXML private void copyBase64Fired(ActionEvent event) {
		if (base64String.get()!=null && !base64String.get().isEmpty()) {
			Map<DataFormat, Object> content=new HashMap<DataFormat, Object>();
			content.put(DataFormat.PLAIN_TEXT, base64String.get());
			Clipboard.getSystemClipboard().setContent(content);
			statusMessageLabel.setText(String.format("%1$,d characters written to the clipboard.", base64String.get().length()));
		}
	}
	
	@FXML private void saveAsFired(ActionEvent event) throws IOException {
		File imageFile=configureFileChooser("Save Image As File", imageView.getScene().getWindow(), DIALOG_TYPE.saveAs);
		String outFormatName=selectedExtensionFilter.getDescription();
		if (imageFile!=null) {
			String compressionType = saveImage(imageFile, outFormatName, imageBytes, jaiPrefs.getSaveCompression().get(), jaiPrefs.getDpi().get());
			String imageSize=String.format("%1$,d(%2$,d saved)", imageBytes.length, imageFile.length());
			statusMessageLabel.setText(String.format("Image %1$s: image size=%2$s, type=%3$s, compression=%4$f, compression type=%5$s", imageFile.getName(), imageSize, outFormatName, jaiPrefs.getSaveCompression().get(), compressionType));
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
