package net.mfjassociates.jai.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.sun.javafx.iio.ImageStorageException;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class SimpleGridPaneFX extends Application {

	private static final byte[] bytes;
	private static final InputStream bais;
	static {
		byte[] ibytes=null;
		try {
			ibytes=Files.readAllBytes(Paths.get("selena.jp2"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		bytes=ibytes;
		bais=new ByteArrayInputStream(bytes);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		GridPane g=new GridPane();
		g.setGridLinesVisible(true);
		StringBuffer sb=new StringBuffer();
		for (int i = 0; i < 10; i++) {
			sb.append("The cat chased the mouse. ");
		}
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				Label l=new Label(String.format("Label%1$d-%2$d",r, c));
				if (r==1 && c==1) {
					l=new Label(sb.toString());
					l.setWrapText(true);
				}
				l.setPrefWidth(Region.USE_COMPUTED_SIZE);
				g.add(l,  r, c);
			}
		}
		Scene scene=new Scene(g, 400, 400);
		g.maxWidthProperty().bind(scene.widthProperty());
		g.maxHeightProperty().bind(scene.heightProperty());
		primaryStage.setTitle("Image Test");
		primaryStage.setScene(scene);
		primaryStage.show();

	}

	public static void main(String[] args) {
		launch(args);
	}
	
	@SuppressWarnings("restriction")
	private Image loadImage() throws Exception {
		Image fximage=null;
		fximage=new Image(bais);// use javafx
		bais.reset();
		ImageInputStream imageis=ImageIO.createImageInputStream(bais);
		ImageReader reader=ImageIO.getImageReaders(imageis).next();
		reader.setInput(imageis);
		String formatName=reader.getFormatName();
		if (fximage.isError()) {
			if (fximage.getException() instanceof ImageStorageException) { // use imageIO
				System.out.println(fximage.getException().getLocalizedMessage()+": using ImageIO");
				fximage=SwingFXUtils.toFXImage(reader.read(0), null);
			} else throw fximage.getException();
		}
		imageis.close();
		reader.dispose();
		System.out.println(String.format("Read %1$s image.",formatName));
		return fximage;
	}

}
