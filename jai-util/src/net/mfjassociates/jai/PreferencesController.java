package net.mfjassociates.jai;

import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.Pair;
import javafx.util.StringConverter;
import net.mfjassociates.jai.util.ImageHandler;

public class PreferencesController {
	
	public static final String SAVE_COMPRESSION_PREF = "save_compression";
	public static final String DISPLAY_COMPRESSION_PREF = "display_compression";
	public static final float RECOMMENDED_JPEG_QUALITY = 0.75f;
	public static final float RECOMMENDED_DISPLAY_QUALITY = 1.0f;

    private Preferences userPreferences;
	private Dialog<Pair<Float, Float>> dialog;
	
	public PreferencesController(Preferences aUserPreferences) {
		this.userPreferences=aUserPreferences;
	}
	
	public void setDialog(Dialog<Pair<Float, Float>> aDialog) {
		this.dialog=aDialog;
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.APPLY) {
				// get new values
				String aSaveCompression=saveCompressionTextField.getText();
				String aDisplayCompression=displayCompressionTextField.getText();
				// save to preferences (if required)
				String saveCompression = userPreferences.get(SAVE_COMPRESSION_PREF, Float.toString(RECOMMENDED_JPEG_QUALITY));
				String displayCompression = userPreferences.get(DISPLAY_COMPRESSION_PREF, Float.toString(RECOMMENDED_DISPLAY_QUALITY));
				float oldCompression=Float.parseFloat(saveCompression);
				float newCompression=Float.parseFloat(aSaveCompression);
				Float returnedSaveCompression=null;
				Float returnedDisplayCompression=null;
				if (!ImageHandler.equals(oldCompression, newCompression, 4)) {
					userPreferences.put(SAVE_COMPRESSION_PREF, aSaveCompression);
					returnedSaveCompression=newCompression;
				}
				oldCompression=Float.parseFloat(displayCompression);
				newCompression=Float.parseFloat(aDisplayCompression);
				if (!ImageHandler.equals(oldCompression, newCompression, 4)) {
					userPreferences.put(DISPLAY_COMPRESSION_PREF, aDisplayCompression);
					returnedDisplayCompression=newCompression;
				}
				// return as result
				return new Pair<>(returnedSaveCompression, returnedDisplayCompression);
			}
			return null; // it was not apply that was pressed, return null
		});
	}

    private static class CompressionConverter extends StringConverter<Float> {

		@Override
		public String toString(Float object) {
			if (object == null) return "0.0";
			return object.toString();
		}

		@Override
		public Float fromString(String string) {
			Float value=Float.parseFloat(string);
			if (value<0.0 || value > 1.0) throw new NumberFormatException(value.toString()+": new value is less than 0.0 or greater than 1.0");
			return value;
		}
		
	}

	@FXML private TextField saveCompressionTextField;
	@FXML private TextField displayCompressionTextField;
	@FXML private DialogPane dialogPane;
	
	@FXML
	private void initialize() {
//		if (true) return;
		Parent parent = dialogPane.getParent();
		// if scenebuilder had support for OK button I would use it and this would not be necessary
		Button button = (Button) dialogPane.lookupButton(ButtonType.APPLY);
		if (button!=null) button.setDefaultButton(true); 
		String saveCompression = userPreferences.get(SAVE_COMPRESSION_PREF, Float.toString(RECOMMENDED_JPEG_QUALITY));
		String displayCompression = userPreferences.get(DISPLAY_COMPRESSION_PREF, Float.toString(RECOMMENDED_DISPLAY_QUALITY));
		
		saveCompressionTextField.setTextFormatter(new TextFormatter<Float>(new CompressionConverter()));
		saveCompressionTextField.setText(saveCompression);
		displayCompressionTextField.setTextFormatter(new TextFormatter<Float>(new CompressionConverter()));
		displayCompressionTextField.setText(displayCompression);

		Platform.runLater(() -> saveCompressionTextField.requestFocus());
	}
}
