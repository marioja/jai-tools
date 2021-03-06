package net.mfjassociates.jai.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//import org.imgscalr.Scalr;
//import org.imgscalr.Scalr.Method;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ImageHandler {

	private static final String JPEG_TREE = "javax_imageio_jpeg_image_1.0";
	
	public static class BasicImageInformation {
		public int xdensity=-1;
		public int ydensity=-1;
	}

	public static InputStream createIS(byte[] input_image_bytes) {
		return new ByteArrayInputStream(input_image_bytes);
	}
	public static ImageInputStream createImageIS(InputStream is) throws IOException {
		ImageInputStream imageis = null;
		imageis = ImageIO.createImageInputStream(is);
		return imageis;
	}

	public static ImageReader createReader(ImageInputStream imageis) {
		ImageReader reader=null;
		reader=ImageIO.getImageReaders(imageis).next();
		
		reader.setInput(imageis);
		return reader;
	}

	public static String saveImage(File outputImageFile, String outFormatName, byte[] input_image_bytes, float saveCompression, Integer dpi) throws IOException, FileNotFoundException {
	
		ImageInputStream imageis = createImageIS(createIS(input_image_bytes));
		ImageReader reader = createReader(imageis);
		ImageWriter writer = createWriter(outFormatName);
		ImageWriteParam iwp = writer.getDefaultWriteParam();
		String compressionType = setupSaveImageWriteParam(iwp, saveCompression);
	
		writeImageFile(outputImageFile, reader, writer, iwp, dpi, outFormatName);
		
		imageis.close();
		reader.dispose();
		writer.dispose();
		return compressionType;
	}

	public static ImageWriter createWriter(String outFormatName) {
		ImageWriter writer = ImageIO.getImageWritersByFormatName(outFormatName).next();
		return writer;
	}
	
	public static String diplayImageMetadata(ImageReader reader, BasicImageInformation ii) throws IOException, TransformerException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IIOMetadata metadata = reader.getImageMetadata(0);
		Node node = metadata.getAsTree(metadata.getNativeMetadataFormatName());
		if (node instanceof IIOMetadataNode) {
			IIOMetadataNode iionode = (IIOMetadataNode) node;
			NodeList apps = iionode.getElementsByTagName("app0JFIF");
			if (apps!=null && apps.getLength()>0) {
				Node app = apps.item(0);
				NamedNodeMap attrs = app.getAttributes();
				Node xdensity = attrs.getNamedItem("Xdensity");
				Node ydensity = attrs.getNamedItem("Ydensity");
				String sxdensity=xdensity.getNodeValue();
				String sydensity=ydensity.getNodeValue();
				try {
					ii.xdensity=Integer.parseInt(sxdensity);
					ii.ydensity=Integer.parseInt(sydensity);					
				} catch (NumberFormatException e) {
				}
			}
		}
		

		// use the XSLT transformation package to output the DOM tree we just
		// created
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(new DOMSource(node), new StreamResult(baos));
		return new String(baos.toByteArray());
	}

	public static String setupSaveImageWriteParam(ImageWriteParam iwp, float saveCompression) {
		if (!iwp.canWriteCompressed()) return null;
		iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		String compressionType=iwp.getCompressionTypes()[0];
		iwp.setCompressionType(compressionType);
		iwp.setCompressionQuality(saveCompression);
		return compressionType;
	}

	public static void writeImageFile(File imageFile, ImageReader reader, ImageWriter writer, ImageWriteParam iwp, Integer dpi, String outFormatName)
			throws FileNotFoundException, IOException {
		
		BufferedImage bufferedImage = reader.read(0);
//		BufferedImage scaledImage = Scalr.resize(bufferedImage, Method.ULTRA_QUALITY, bufferedImage.getWidth()/3, bufferedImage.getHeight()/3);
		IIOImage iioImage = new IIOImage(bufferedImage, null, null);
		
		
		if ("JPEG".equalsIgnoreCase(outFormatName) && dpi!=null) {
			// Metadata (dpi)
			IIOMetadata data = writer.getDefaultImageMetadata(new ImageTypeSpecifier(bufferedImage), iwp);
			Element tree = (Element) data.getAsTree(JPEG_TREE);
			Element jfif = (Element) tree.getElementsByTagName("app0JFIF").item(0);
			jfif.setAttribute("Xdensity", dpi.toString());
			jfif.setAttribute("Ydensity", dpi.toString());
			jfif.setAttribute("resUnits", "1"); // density is dots per inch
			data.setFromTree(JPEG_TREE, tree);
			iioImage.setMetadata(data);
		}

		FileOutputStream fos=new FileOutputStream(imageFile);
		ImageOutputStream imageos = ImageIO.createImageOutputStream(fos);
		writer.setOutput(imageos);
		writer.write(null, iioImage, iwp);
		imageos.close();
		fos.close();
		return;
	}

	/** Positive zero bits. */
	public static final int POSITIVE_ZERO_FLOAT_BITS   = Float.floatToRawIntBits(+0.0f);
	/** Negative zero bits. */
	public static final int NEGATIVE_ZERO_FLOAT_BITS   = Float.floatToRawIntBits(-0.0f);
	/** Offset to order signed double numbers lexicographically. */
	public static final int SGN_MASK_FLOAT = 0x80000000;

//	/**
//	 * Based on {@link org.apache.commons.math3.util.Precision#equals(float, float, int)}
//	 * @param x - first float to compare
//	 * @param y - second float to compare
//	 * @param maxUlps - See {@link org.apache.commons.math3.util.Precision#equals(float, float, int)} for details
//	 * @return a boolean indicating if the comparison is equal
//	 */
//	public static boolean equals(final float x, final float y, final int maxUlps) {
//	
//	    final int xInt = Float.floatToRawIntBits(x);
//	    final int yInt = Float.floatToRawIntBits(y);
//	
//	    final boolean isEqual;
//	    if (((xInt ^ yInt) & SGN_MASK_FLOAT) == 0) {
//	        // number have same sign, there is no risk of overflow
//	    	// isEqual = FastMath.abs(xInt - yInt) <= maxUlps;
//	    	// Replace code above with (to remove dependency on commons-math3)
//	    	final int x1 = (xInt - yInt);
//	        final int i = x1 >>> 31;
//	        isEqual = ((x1 ^ (~i + 1)) + i) <= maxUlps;
//	
//	    } else {
//	        // number have opposite signs, take care of overflow
//	        final int deltaPlus;
//	        final int deltaMinus;
//	        if (xInt < yInt) {
//	            deltaPlus  = yInt - POSITIVE_ZERO_FLOAT_BITS;
//	            deltaMinus = xInt - NEGATIVE_ZERO_FLOAT_BITS;
//	        } else {
//	            deltaPlus  = xInt - POSITIVE_ZERO_FLOAT_BITS;
//	            deltaMinus = yInt - NEGATIVE_ZERO_FLOAT_BITS;
//	        }
//	
//	        if (deltaPlus > maxUlps) {
//	            isEqual = false;
//	        } else {
//	            isEqual = deltaMinus <= (maxUlps - deltaPlus);
//	        }
//	
//	    }
//	
//	    return isEqual && !Float.isNaN(x) && !Float.isNaN(y);
//	
//	}

}
