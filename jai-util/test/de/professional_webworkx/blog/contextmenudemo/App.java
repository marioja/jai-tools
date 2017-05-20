package de.professional_webworkx.blog.contextmenudemo;

import de.professional_webworkx.blog.contextmenudemo.model.Person;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 *
 * @author Patrick Ott <Patrick.Ott@professional-webworkx.de>
 */
public class App extends Application {
	@Override
	public void start(Stage primaryStage) {
		List<Person> persons = new ArrayList<>();
		persons.add(new Person("Patrick", "Ott", "patrick.ott@professional-webworkx.de"));
		persons.add(new Person("Hans", "Meier", "hans.meier@professional-webworkx.de"));
		ObservableList<Person> observableList = FXCollections.observableList(persons);
		ListView<Person> personList = new ListView<Person>(observableList);
		final ContextMenu contextMenu = new ContextMenu();
		MenuItem print1 = new MenuItem("Print on Laser Printer");
		MenuItem print2 = new MenuItem("Print on Ink Printer");
		contextMenu.getItems().addAll(print1, print2);
		MenuItem editItem = new MenuItem("Show E-Mail Address");
		editItem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent t) {
				Person selectedItem = personList.getSelectionModel().getSelectedItem();
				System.out.println(selectedItem.geteMail());
			}
		});
		contextMenu.getItems().add(editItem);
		personList.setContextMenu(contextMenu);
		personList.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
			@Override
			public void handle(ContextMenuEvent event) {
				System.out.println("Handle invoked");
				contextMenu.show(personList, event.getScreenX(), event.getScreenY());
				event.consume();
			}
		});
		VBox root = new VBox(personList);
		Scene scene = new Scene(root, 1024, 768);
		primaryStage.setScene(scene);
		primaryStage.setTitle("ContextMenu Demo");
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}