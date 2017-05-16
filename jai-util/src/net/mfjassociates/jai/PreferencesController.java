package net.mfjassociates.jai;

import java.util.List;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import org.apache.commons.math3.util.Precision;

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
import net.mfjassociates.jai.PreferencesController.JaiPreferences.PREFERENCES_NAMES;
import net.mfjassociates.jai.PreferencesController.JaiPreferences.RESIZE_UNIT;

public class PreferencesController {
	
	public static final String SAVE_COMPRESSION_PREF = "save_compression";
	public static final String DISPLAY_COMPRESSION_PREF = "display_compression";
	public static final float RECOMMENDED_JPEG_QUALITY = 0.75f;
	public static final float RECOMMENDED_DISPLAY_QUALITY = 1.0f;

    private Preferences userPreferences;
	private Dialog<JaiPreferences> dialog;
	
	public PreferencesController(Preferences aUserPreferences) {
		this.userPreferences=aUserPreferences;
	}
	
	public void setDialog(Dialog<JaiPreferences> aDialog) {
		this.dialog=aDialog;
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.APPLY) {
				JaiPreferences jaiPrefs=new JaiPreferences(userPreferences);
				boolean modified=false;
				// get new values
				jaiPrefs.saveCompression.setString(saveCompressionTextField.getText());
				jaiPrefs.displayCompression.setString(displayCompressionTextField.getText());
				// return as result
				return jaiPrefs;
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

	static class JaiPreference<T> {
		private T pref;
		private String prefName;
		private T defaultValue;
		private Preferences prefs;
		private boolean modified;

		public JaiPreference(PREFERENCES_NAMES prefName, Preferences aPrefs) {
			// this(prefName.getDefaultValue(), prefName.prefName);
			this.defaultValue = prefName.getDefaultValue();
			this.prefName = prefName.prefName;
			this.prefs = aPrefs;
			this.modified = true;
		}
		// public JaiPreference(T aDefaultValue, String aPrefName) {
		// this.defaultValue=aDefaultValue;
		// this.prefName=aPrefName;
		//// this.pref=aPref;
		// this.modified=false;
		// }

		public T get() {
			if (pref == null) {
				checkIfChanged(null);// set pref to stored preference
			}
			return pref;
		}

		public boolean isModified() {
			return modified;
		}

		@SuppressWarnings("unchecked")
		public <E extends Enum<E>> String getString() {
			Class<? extends Object> clazz = defaultValue.getClass();
			if (Number.class.isAssignableFrom(clazz) || String.class.isAssignableFrom(clazz)) {
				return prefs.get(prefName, defaultValue.toString());
			} else if (Enum.class.isAssignableFrom(clazz)) {
				return prefs.get(prefName, ((Enum<E>) defaultValue).name());
			} else {
				throw new IllegalArgumentException(
						"The preference must be of type Number, String or an enum: " + clazz.getCanonicalName());
			}
		}

		public <E extends Enum<E>> void setString(String aPref) {
			if (!checkIfChanged(aPref)) {// if value changed
				prefs.put(prefName, aPref);
				this.modified = true;
			}
		}
		/**
		 * If aPref is not null, check if this preference has changed (returned as boolean)
		 * and set the field pref to the new value.
		 * If aPref is null, then set the pref field to the stored preferences value and return false
		 * @param aPref
		 * @return if the preference has changed
		 */
		@SuppressWarnings("unchecked")
		private boolean checkIfChanged(String aPref) {
			boolean areEqual=false;
			Class<? extends Object> clazz = defaultValue.getClass();
			if (Float.class.isAssignableFrom(clazz)) {
				Float oldValue=Float.parseFloat(getString());
				if (aPref != null) {
					pref = (T) Float.valueOf(aPref);
					areEqual = Precision.equals(oldValue, (float)pref, 4);
				} else {
					pref = (T) oldValue;
				}
			} else if (Double.class.isAssignableFrom(clazz)) {
				Double oldValue=Double.parseDouble(getString());
				if (aPref != null) {
					pref = (T) Double.valueOf(aPref);
					areEqual = Precision.equals(oldValue, (double)pref, 4);
				} else {
					pref = (T) oldValue;
				}
			} else if (Long.class.isAssignableFrom(clazz)) {
				Long oldValue = Long.parseLong(getString());
				if (aPref !=null) {
					pref = (T) Long.valueOf(aPref);
					areEqual = oldValue == ((long)pref);
				} else {
					pref = (T) oldValue;
				}
			} else if (Integer.class.isAssignableFrom(clazz)) {
				Integer oldValue = Integer.parseInt(getString());
				if (aPref !=null) {
					pref = (T) Integer.valueOf(aPref);
					areEqual = oldValue == ((int)pref);
				} else {
					pref = (T) oldValue;
				}
			} else if (Enum.class.isAssignableFrom(clazz)) {
				String oldValue = getString();
				if (aPref !=null) {
					pref = (T) Enum.valueOf(((Enum<?>) defaultValue).getClass(), aPref);
					areEqual = oldValue.equals(aPref);
				} else {
					pref = (T) Enum.valueOf(((Enum<?>) defaultValue).getClass(), oldValue);
				}
//				String newValue = aPref;
//				pref = (T) Enum.valueOf(((Enum<?>) defaultValue).getClass(), newValue);
//				String oldValue = getString();
//				areEqual = oldValue.equals(newValue);
			} else {
				throw new IllegalArgumentException(
						"The preference must be of type Number, String or an enum: " + clazz.getCanonicalName());
			}
			return areEqual;
		}
	}

	static class JaiPreferences {
		
		public JaiPreferences(Preferences aPrefs) {
			this.prefs=aPrefs;
		}
		private Preferences prefs;
		public static enum RESIZE_UNIT {
			pixel, percent;
		}

		static enum PREFERENCES_NAMES {
			saveCompression("save_compression", new Float(.75f)), displayCompression("display_compression",
					new Float(1f)), dpi("dots_per_inch", 300), resize("resize",
							Arrays.asList(new Float[] { -1f, -1f })), resizeUnits("resize_units", RESIZE_UNIT.pixel);
			private <T> PREFERENCES_NAMES(String aPrefName, T aDefaultValue) {
				this.prefName = aPrefName;
				this.defaultValue = aDefaultValue;
			}

			@SuppressWarnings("unchecked")
			public <T> T getDefaultValue() {
				return (T) defaultValue;
			}

			private String prefName;
			private Object defaultValue;
		}

		public JaiPreference<Float> saveCompression = new JaiPreference<Float>(PREFERENCES_NAMES.saveCompression, prefs);
		public JaiPreference<Float> displayCompression = new JaiPreference<Float>(PREFERENCES_NAMES.displayCompression, prefs);
		public JaiPreference<Integer> dpi = new JaiPreference<Integer>(PREFERENCES_NAMES.dpi, prefs);
		public JaiPreference<List<Float>> resize = new JaiPreference<List<Float>>(PREFERENCES_NAMES.resize, prefs);
		public JaiPreference<RESIZE_UNIT> resizeUnit = new JaiPreference<RESIZE_UNIT>(PREFERENCES_NAMES.resizeUnits, prefs);
	}

	public static void main(String[] args) throws BackingStoreException {
		Preferences prefs = Preferences.userNodeForPackage(Object.class);
//		dap(prefs);
		Stream.of(prefs.keys()).forEach(key -> {
			System.out.println(String.format("Pref(%1$s)=%2$s", key, prefs.get(key, null)));
		});
		JaiPreference<JaiPreferences.RESIZE_UNIT> a = new JaiPreference<RESIZE_UNIT>(PREFERENCES_NAMES.resizeUnits, prefs);
		System.out.println("MY-PREF(before set)=" + a.getString());
		a.setString(JaiPreferences.RESIZE_UNIT.percent.name());
		System.out.println("MY-PREF(after  set)=" + a.getString());
	}

	private static void dap(Preferences prefs) throws BackingStoreException {
		prefs.clear();
		System.out.println("Preferences cleared");
	}
}
