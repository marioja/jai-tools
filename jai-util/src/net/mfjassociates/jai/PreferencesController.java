package net.mfjassociates.jai;

import static net.mfjassociates.jai.PreferencesController.JaiPreferences.PREFERENCES_NAMES.DISPLAY_COMPRESSION;
import static net.mfjassociates.jai.PreferencesController.JaiPreferences.PREFERENCES_NAMES.DPI;
import static net.mfjassociates.jai.PreferencesController.JaiPreferences.PREFERENCES_NAMES.RESIZE_HEIGHT;
import static net.mfjassociates.jai.PreferencesController.JaiPreferences.PREFERENCES_NAMES.RESIZE_UNITS;
import static net.mfjassociates.jai.PreferencesController.JaiPreferences.PREFERENCES_NAMES.RESIZE_WIDTH;
import static net.mfjassociates.jai.PreferencesController.JaiPreferences.PREFERENCES_NAMES.SAVE_COMPRESSION;
import static net.mfjassociates.jai.PreferencesController.JaiPreferences.RESIZE_UNIT.PERCENT;
import static net.mfjassociates.jai.PreferencesController.JaiPreferences.RESIZE_UNIT.PIXEL;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import org.apache.commons.math3.util.Precision;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;
import net.mfjassociates.jai.PreferencesController.JaiPreferences.PREFERENCES_NAMES;
import net.mfjassociates.jai.PreferencesController.JaiPreferences.RESIZE_UNIT;

public class PreferencesController {
	
	public static final String SAVE_COMPRESSION_PREF = "save_compression";
	public static final String DISPLAY_COMPRESSION_PREF = "display_compression";
	public static final float RECOMMENDED_JPEG_QUALITY = 0.75f;
	public static final float RECOMMENDED_DISPLAY_QUALITY = 1.0f;

    private Preferences userPreferences;
    private JaiPreferences jaiPrefs;
    private ImageView imageView;
	
	@FXML private TextField saveCompressionTextField;
	@FXML private TextField displayCompressionTextField;
	@FXML private DialogPane dialogPane;
	@FXML private Label statusMessageLabel;
	@FXML private TextField dpiTextField;
	@FXML private TextField widthTextField;
	@FXML private TextField heightTextField;
	@FXML private ComboBox<RESIZE_UNIT> resizeUnitsComboBox;
	

	public PreferencesController(Preferences aUserPreferences, ImageView anImageView) {
		this.userPreferences=aUserPreferences;
		this.imageView=anImageView;
	}
	
	public void setDialog(Dialog<JaiPreferences> aDialog) {
		aDialog.setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.APPLY) {
				// get new values
				jaiPrefs.getSaveCompression().setString(saveCompressionTextField.getText());
				jaiPrefs.getDisplayCompression().setString(displayCompressionTextField.getText());
				jaiPrefs.getDpi().setString(dpiTextField.getText());
				jaiPrefs.getResizeWidth().setString(widthTextField.getText());
				jaiPrefs.getResizeHeight().setString(heightTextField.getText());
				jaiPrefs.getResizeUnit().setString(resizeUnitsComboBox.getValue().name());
				
				// return as result
				return jaiPrefs;
			}
			return null; // it was not apply that was pressed, return null
		});
	}

    private static class CompressionConverter extends StringConverter<Float> {

		@Override
		public String toString(Float object) {
			if (object == null) {
				return "0.0";
			}
			return object.toString();
		}

		@Override
		public Float fromString(String string) {
			Float value=Float.parseFloat(string);
			if (value<0.0) {
				value = 0f;
			}
			if (value > 1.0) {
				value = 1f;
			}
			return value;
		}
		
	}
    
    private static class DpiConverter extends StringConverter<Integer> {

		@Override
		public String toString(Integer object) {
			if (object == null) {
				return "0";
			}
			return object.toString();
		}

		@Override
		public Integer fromString(String string) {
			Integer value=Integer.parseInt(string);
			if (value<0) {
				value=0;
			}
			return value;
		}
		
	}
    
    
    private static class SizeConverter extends StringConverter<Float> {
    	
    	private ObjectProperty<RESIZE_UNIT> resizeUnit=new SimpleObjectProperty<>();
    	
    	// resizeUnit property
    	public RESIZE_UNIT getResizeUnit() {return resizeUnit.get();}
    	public void setResizeUnit(RESIZE_UNIT aUnit) {this.resizeUnit.set(aUnit);}
    	public ObjectProperty<RESIZE_UNIT> resizeUnitProperty() {return resizeUnit;}
    	
		@Override
		public String toString(Float object) {
			if (object == null) {
				return "0.0";
			}
			return object.toString();
		}

		@Override
		public Float fromString(String string) {
			Float value=Float.parseFloat(string);
			if (value<0.0) value=0f;
			// if units is percent check size
			if (resizeUnit.get().equals(PERCENT)) {
				if (value > 100.0) value=100f;
			}
			return value;
		}
		
	}
    
	@FXML
	private void initialize() {

		// if scenebuilder had support for OK button I would use it and this would not be necessary
		Button button = (Button) dialogPane.lookupButton(ButtonType.APPLY);
		if (button!=null) {
			button.setDefaultButton(true); 
		}
		button = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
		if (button!=null) {
			button.setCancelButton(true); 
		}
		resizeUnitsComboBox.getItems().clear();
		resizeUnitsComboBox.getItems().addAll(RESIZE_UNIT.values());
		jaiPrefs=new JaiPreferences(userPreferences);
		
		saveCompressionTextField.setTextFormatter(new TextFormatter<Float>(new CompressionConverter()));
		saveCompressionTextField.setText(jaiPrefs.getSaveCompression().getString());
		displayCompressionTextField.setTextFormatter(new TextFormatter<Float>(new CompressionConverter()));
		displayCompressionTextField.setText(jaiPrefs.getDisplayCompression().getString());
		resizeUnitsComboBox.setValue(jaiPrefs.getResizeUnit().get());
		final JaiPreference<Float> rw = jaiPrefs.getResizeWidth();
		final JaiPreference<Float> rh = jaiPrefs.getResizeHeight();
		resizeUnitsComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldv, newv) -> {
			// when switching from pixel to percent, calculate percent
			if (oldv!=null && oldv.equals(PIXEL) && newv!=null && newv.equals(PERCENT)) {
				if (imageView.getImage()!=null) {
					rw.setString(Double.toString(jaiPrefs.getResizeWidth().get()/imageView.getImage().getWidth()*100d));
					widthTextField.setText(rw.getString());
					rh.setString(Double.toString(jaiPrefs.getResizeHeight().get()/imageView.getImage().getHeight()*100d));
					heightTextField.setText(rh.getString());
				}
			}
			// when switching from percent to pixel, calculate pixels based on percent
			if (oldv!=null && oldv.equals(PERCENT) && newv!=null && newv.equals(PIXEL)) {
				if (imageView.getImage()!=null) {
					rw.setString(Double.toString(jaiPrefs.getResizeWidth().get()*imageView.getImage().getWidth()/100d));
					widthTextField.setText(rw.getString());
					rh.setString(Double.toString(jaiPrefs.getResizeHeight().get()*imageView.getImage().getHeight()/100d));
					heightTextField.setText(rh.getString());
				}
			}
		});

		// set width to stored value unless it is zero (default) in that case set it to the image width if loaded
		SizeConverter sc = new SizeConverter();
		sc.resizeUnitProperty().bind(resizeUnitsComboBox.valueProperty());
		widthTextField.setTextFormatter(new TextFormatter<Float>(sc));
		if (Precision.equals(0f, rw.get(), 4) && imageView.getImage()!=null) {
			String widthString=Double.toString(imageView.getImage().getWidth());
			rw.setString(widthString);
		}
		widthTextField.setText(rw.getString());

		sc = new SizeConverter();
		sc.resizeUnitProperty().bind(resizeUnitsComboBox.valueProperty());
		heightTextField.setTextFormatter(new TextFormatter<Float>(sc));
		if (Precision.equals(0f, rh.get(), 4) && imageView.getImage()!=null) {
			String heightString=Double.toString(imageView.getImage().getHeight());
			rh.setString(heightString);
		}
		heightTextField.setText(rh.getString());
		dpiTextField.setTextFormatter(new TextFormatter<Integer>(new DpiConverter()));
		dpiTextField.setText(jaiPrefs.getDpi().getString());

		Platform.runLater(saveCompressionTextField::requestFocus);
	}

	@FXML private void resetToDefaultsFired(ActionEvent event) throws BackingStoreException {
		userPreferences.clear();
		Platform.runLater(() -> statusMessageLabel.setText("Preferences reset to defaults and saved"));
		initialize();
	}

	static class JaiPreference<T> {
		private T pref;
		private String prefName;
		private T defaultValue;
		private Preferences prefs;
		private boolean modified;

		public JaiPreference(PREFERENCES_NAMES prefName, Preferences aPrefs) {

			this.defaultValue = prefName.getDefaultValue();
			this.prefName = prefName.prefName;
			this.prefs = aPrefs;
			this.modified = true;
		}

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
				String prefValue = prefs.get(prefName, defaultValue.toString());
				return prefValue;
			} else if (Enum.class.isAssignableFrom(clazz)) {
				String prefValue = prefs.get(prefName, ((Enum<E>) defaultValue).name());
				return prefValue;
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
		public void reset() {
			prefs.remove(prefName);
			this.pref=this.defaultValue;
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
					areEqual = Precision.equals(oldValue, (Float)pref, 4);
				} else {
					pref = (T) oldValue;
				}
			} else if (Double.class.isAssignableFrom(clazz)) {
				Double oldValue=Double.parseDouble(getString());
				if (aPref != null) {
					pref = (T) Double.valueOf(aPref);
					areEqual = Precision.equals(oldValue, (Double)pref, 4);
				} else {
					pref = (T) oldValue;
				}
			} else if (Long.class.isAssignableFrom(clazz)) {
				Long oldValue = Long.parseLong(getString());
				if (aPref !=null) {
					pref = (T) Long.valueOf(aPref);
					areEqual = oldValue == ((Long)pref);
				} else {
					pref = (T) oldValue;
				}
			} else if (Integer.class.isAssignableFrom(clazz)) {
				Integer oldValue = Integer.parseInt(getString());
				if (aPref !=null) {
					pref = (T) Integer.valueOf(aPref);
					areEqual = oldValue == ((Integer)pref);
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
		
		private Preferences prefs;
		private JaiPreference<Float> saveCompression;
		private JaiPreference<Float> displayCompression;
		private JaiPreference<Integer> dpi;
		private JaiPreference<Float> resizeWidth;
		private JaiPreference<Float> resizeHeight;
		private JaiPreference<RESIZE_UNIT> resizeUnit;

		public JaiPreferences(Preferences aPrefs) {
			this.prefs=aPrefs;
			saveCompression = new JaiPreference<>(SAVE_COMPRESSION, prefs);
			displayCompression = new JaiPreference<>(DISPLAY_COMPRESSION, prefs);
			dpi = new JaiPreference<>(DPI, prefs);
			resizeWidth = new JaiPreference<>(RESIZE_WIDTH, prefs);
			resizeHeight = new JaiPreference<>(RESIZE_HEIGHT, prefs);
			resizeUnit = new JaiPreference<>(RESIZE_UNITS, prefs);
		}
		enum RESIZE_UNIT {
			PIXEL, PERCENT;
		}

		enum PREFERENCES_NAMES {
			SAVE_COMPRESSION("save_compression", .75f),
			DISPLAY_COMPRESSION("display_compression", 1f),
			DPI("dots_per_inch", 300),
			RESIZE_WIDTH("resize_width", 0f), 
			RESIZE_HEIGHT("resize_height", 0f), 
			RESIZE_UNITS("resize_units", RESIZE_UNIT.PIXEL);
			
			private String prefName;
			private Object defaultValue;

			private <T> PREFERENCES_NAMES(String aPrefName, T aDefaultValue) {
				this.prefName = aPrefName;
				this.defaultValue = aDefaultValue;
			}

			@SuppressWarnings("unchecked")
			public <T> T getDefaultValue() {
				return (T) defaultValue;
			}

		}

		public Preferences getPrefs() {
			return prefs;
		}
		public void setPrefs(Preferences prefs) {
			this.prefs = prefs;
		}
		public JaiPreference<Float> getSaveCompression() {
			return saveCompression;
		}
		public void setSaveCompression(JaiPreference<Float> saveCompression) {
			this.saveCompression = saveCompression;
		}
		public JaiPreference<Float> getDisplayCompression() {
			return displayCompression;
		}
		public void setDisplayCompression(JaiPreference<Float> displayCompression) {
			this.displayCompression = displayCompression;
		}
		public JaiPreference<Integer> getDpi() {
			return dpi;
		}
		public void setDpi(JaiPreference<Integer> dpi) {
			this.dpi = dpi;
		}
		public JaiPreference<RESIZE_UNIT> getResizeUnit() {
			return resizeUnit;
		}
		public void setResizeUnit(JaiPreference<RESIZE_UNIT> resizeUnit) {
			this.resizeUnit = resizeUnit;
		}
		public JaiPreference<Float> getResizeWidth() {
			return resizeWidth;
		}
		public void setResizeWidth(JaiPreference<Float> resizeWidth) {
			this.resizeWidth = resizeWidth;
		}
		public JaiPreference<Float> getResizeHeight() {
			return resizeHeight;
		}
		public void setResizeHeight(JaiPreference<Float> resizeHeight) {
			this.resizeHeight = resizeHeight;
		}
		
	}

	public static void main(String[] args) throws BackingStoreException {
		Preferences prefs = Preferences.userNodeForPackage(Object.class);
//		dap(prefs);
		Stream.of(prefs.keys()).forEach(key -> 
			System.out.println(String.format("Pref(%1$s)=%2$s", key, prefs.get(key, null)))
		);
		JaiPreference<JaiPreferences.RESIZE_UNIT> a = new JaiPreference<>(PREFERENCES_NAMES.RESIZE_UNITS, prefs);
		System.out.println("MY-PREF(before set)=" + a.getString());
		a.setString(JaiPreferences.RESIZE_UNIT.PERCENT.name());
		System.out.println("MY-PREF(after  set)=" + a.getString());
	}

	private static void dap(Preferences prefs) throws BackingStoreException {
		prefs.clear();
		System.out.println("Preferences cleared");
	}
}
