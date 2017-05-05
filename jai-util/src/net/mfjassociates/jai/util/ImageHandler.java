package net.mfjassociates.jai.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class ImageHandler {

	public static ImageInputStream createImageIS(byte[] input_image_bytes) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(input_image_bytes);
		ImageInputStream imageis = null;
		imageis = ImageIO.createImageInputStream(bais);
		return imageis;
	}

	public static ImageReader createReader(ImageInputStream imageis) {
		ImageReader reader=null;
		reader=ImageIO.getImageReaders(imageis).next();
		
		reader.setInput(imageis);
		return reader;
	}

	public static String saveImage(File outputImageFile, String outFormatName, byte[] input_image_bytes, float saveCompression) throws IOException, FileNotFoundException {
	
		ImageInputStream imageis = createImageIS(input_image_bytes);
		ImageReader reader = createReader(imageis);
		ImageWriter writer = createWriter(outFormatName);
		ImageWriteParam iwp = writer.getDefaultWriteParam();
		String compressionType = setupSaveImageWriteParam(iwp, saveCompression);
	
		writeImageFile(outputImageFile, reader, writer, iwp);
		
		imageis.close();
		reader.dispose();
		writer.dispose();
		return compressionType;
	}

	public static ImageWriter createWriter(String outFormatName) {
		ImageWriter writer = ImageIO.getImageWritersByFormatName(outFormatName).next();
		return writer;
	}

	public static String setupSaveImageWriteParam(ImageWriteParam iwp, float saveCompression) {
		iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		String compressionType=iwp.getCompressionTypes()[0];
		iwp.setCompressionType(compressionType);
		iwp.setCompressionQuality(saveCompression);
		return compressionType;
	}

	public static void writeImageFile(File imageFile, ImageReader reader, ImageWriter writer, ImageWriteParam iwp)
			throws FileNotFoundException, IOException {
		
		IIOImage iioImage = new IIOImage(reader.read(0), null, null);
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

	/**
	 * Based on {@link org.apache.commons.math3.util.Precision#equals(float, float, int)}
	 * @param x
	 * @param y
	 * @param maxUlps
	 * @return
	 * @see {@link org.apache.commons.math3.util.Precision#equals(float, float, int)}
	 */
	public static boolean equals(final float x, final float y, final int maxUlps) {
	
	    final int xInt = Float.floatToRawIntBits(x);
	    final int yInt = Float.floatToRawIntBits(y);
	
	    final boolean isEqual;
	    if (((xInt ^ yInt) & SGN_MASK_FLOAT) == 0) {
	        // number have same sign, there is no risk of overflow
	    	// isEqual = FastMath.abs(xInt - yInt) <= maxUlps;
	    	// Replace code above with (to remove dependency on commons-math3)
	    	final int x1 = (xInt - yInt);
	        final int i = x1 >>> 31;
	        isEqual = ((x1 ^ (~i + 1)) + i) <= maxUlps;
	
	    } else {
	        // number have opposite signs, take care of overflow
	        final int deltaPlus;
	        final int deltaMinus;
	        if (xInt < yInt) {
	            deltaPlus  = yInt - POSITIVE_ZERO_FLOAT_BITS;
	            deltaMinus = xInt - NEGATIVE_ZERO_FLOAT_BITS;
	        } else {
	            deltaPlus  = xInt - POSITIVE_ZERO_FLOAT_BITS;
	            deltaMinus = yInt - NEGATIVE_ZERO_FLOAT_BITS;
	        }
	
	        if (deltaPlus > maxUlps) {
	            isEqual = false;
	        } else {
	            isEqual = deltaMinus <= (maxUlps - deltaPlus);
	        }
	
	    }
	
	    return isEqual && !Float.isNaN(x) && !Float.isNaN(y);
	
	}

}
