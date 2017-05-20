package net.mfjassociates.jai;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class ImageUtil extends Application {
	
	ImageUtilController imageUtilController;
	
	private Object createControllerForType(Class<?> type) {
		if (imageUtilController==null) {
			imageUtilController = new ImageUtilController();
		}
		return imageUtilController;
	}
	@Override
	public void start(Stage primaryStage) {
		try {
//			BorderPane root = (BorderPane)FXMLLoader.load(getClass().getResource("ImageUtil.fxml"));
			FXMLLoader loader=new FXMLLoader(getClass().getResource("ImageUtil.fxml"));
			loader.setControllerFactory(this::createControllerForType);
			BorderPane root = loader.load();
			ImageUtilController iuc=(ImageUtilController) loader.getController();
			loader=new FXMLLoader(getClass().getResource("LeftContextMenu.fxml"));
			loader.setControllerFactory(this::createControllerForType);
			ContextMenu leftContextMenu = loader.load();
			loader=new FXMLLoader(getClass().getResource("RightContextMenu.fxml"));
			loader.setControllerFactory(this::createControllerForType);
			ContextMenu rightContextMenu = loader.load();
			Scene scene = new Scene(root,400,400);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("Java Advanced Imaging Utility");
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
