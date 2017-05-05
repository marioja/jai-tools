package net.mfjassociates.jai.test;

import java.awt.image.BufferedImage;
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
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class SimpleFX extends Application {

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
		ImageView iv=new ImageView(); //new ImageView(new Image("selena.jp2"));
		Image image = loadImage();
		iv.setImage(image);
		Group g=new Group(iv);
		Scene scene=new Scene(g, image.getWidth(), image.getHeight());
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
