package net.mfjassociates.jai.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.jaiimageio.jpeg2000.J2KImageWriteParam;

import javafx.scene.image.Image;
import net.mfjassociates.jai.util.ImageHandler;

@RunWith(value=Parameterized.class)
public class ImageHandlerJpeg2000Test {
	
	private static byte[] image_bytes;
	private static ImageReader reader;
	private static ImageInputStream imageIS;
	private static Path testImagePath=Paths.get("test.jp2");
	private static Path outputDir=Paths.get("outimgs");
	
	@Parameter(value=0)
	public CodeBlockSize codeBlockSize;
	
	@Parameter(value=1)
	public Integer numDecompositionLevels;

	@Parameter(value=2)
	public Double encodingRate; // for lossy only

	@Parameter(value=3)
	public Boolean componentTransformation;

	@Parameter(value=4)
	public Boolean lossless;

	@Parameter(value=5)
	public String filter;
	
	@Parameter(value=6)
	public Float saveCompression;
	
	@Parameters(name="{index}: cbs={0}, ndl={1}, er={2,choice,0<{2,number,'##.###'}|10000<{2,number,'#.###E0'}}, ct={3}, ll={4}, filter={5}, compression={6,number,#.###}")
	public static Collection<Object[]> data() {
		return createImageWriteParams();
	}
	private static class CodeBlockSize {
		private int[] sizes;
		public CodeBlockSize(int size) {
			sizes=new int[] {size, size};
		}
		@Override
		public String toString() {
			return Arrays.toString(sizes);
		}
		
		public int[] toArray() {
			return sizes;
		}
	}
	private static List<Object[]> createImageWriteParams() {
		List<Object[]> parms=new ArrayList<Object[]>();
		boolean[] testLossless=new boolean[]{true, false};
		int[] codeBlockSizes=new int[]{4, 8, 32, 64};
		int[] ndls=new int[] {1, 5, 7, 10};
		String[] filters=new String[]{J2KImageWriteParam.FILTER_53, J2KImageWriteParam.FILTER_97};
		float[] compressions=new float[] {0.1f, 0.5f, 0.75f, 1f};
		
		for (boolean aLossless : testLossless) {
			double[] encodingRates=aLossless?new double[]{Double.MAX_VALUE}:new double[] {.1d, 5d, 100d, Double.MAX_VALUE};
			for (int cbs : codeBlockSizes) {
				for (int ndl : ndls) {
					for (double encodingRate : encodingRates) {
						for (String filter : filters) {
							for (float compression : compressions) {
								List<Object> parm=new ArrayList<Object>();
								parm.add(new CodeBlockSize(cbs));
								parm.add(ndl);
								parm.add(encodingRate);
								parm.add(true); // component transformation
								parm.add(aLossless);
								parm.add(filter);
								parm.add(compression);
								parms.add(parm.toArray(new Object[]{}));
							}
							//return parms;
						}
					}
				}
			}
		}
		return parms;
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		image_bytes=Files.readAllBytes(testImagePath);
		imageIS=ImageHandler.createImageIS(image_bytes);
		reader=ImageHandler.createReader(imageIS);
		Files.createDirectories(outputDir);
		Files.walk(outputDir).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);// clean directory
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		imageIS.close();
		reader.dispose();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

//	@Test
//	public final void testCreateImageIS() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	@Test
//	public final void testCreateReader() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	@Test
//	public final void testSaveImage() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	@Test
//	public final void testSetupSaveImageWriteParam() {
//		fail("Not yet implemented"); // TODO
//	}
//
	@Test
	public final void testWriteImageFile() throws FileNotFoundException, IOException {
		ImageWriter writer = ImageHandler.createWriter("jpeg 2000");
		ImageWriteParam iwp = writer.getDefaultWriteParam();
		if (iwp instanceof J2KImageWriteParam) {
			J2KImageWriteParam j2kiwp=(J2KImageWriteParam) iwp;
			ImageHandler.setupSaveImageWriteParam(j2kiwp, saveCompression);
			j2kiwp.setLossless(lossless);
			j2kiwp.setCodeBlockSize(codeBlockSize.toArray());
			j2kiwp.setComponentTransformation(componentTransformation);
			j2kiwp.setEncodingRate(encodingRate);
			j2kiwp.setFilter(filter);
			j2kiwp.setNumDecompositionLevels(numDecompositionLevels);
			Path outImagePath=outputDir.resolve(outImageFilename());
			ImageHandler.writeImageFile(outImagePath.toFile(), reader, writer, j2kiwp);
		}
		writer.dispose();
	}
	private String outImageFilename() {
		String fn=MessageFormat.format("cbs{0}ndl={1}er={2,choice,0<{2,number,'##.###'}|10000<{2,number,'#.###E0'}}ct={3}ll={4}f={5}c={6,number,#.###}", 
				codeBlockSize, numDecompositionLevels, encodingRate, componentTransformation, lossless, filter, saveCompression);
		return fn+".jp2";
	}
	public static void main(String[] args) throws IOException {
		Set<String> supportedTypes=new HashSet<String>();
//		MessageFormat mf=new MessageFormat("{0,choice,0<{0,number,'##.###'}|10000<{0,number,'#.###E0'}}");
//		MessageFormat mf=new MessageFormat("{0,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.}");
//		MessageFormat mf=new MessageFormat("{0,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more {0,number,'#.##E0'}}");
//		mf=new MessageFormat("{0,number,#.#####E0}");
//		ChoiceFormat2 cf = new ChoiceFormat2("-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more {0,number,'#.##E0'}");
//		cf = new ChoiceFormat2("-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more {0,number}");
//		mf.setFormatByArgumentIndex(0, cf);
//		ChoiceFormat mf=new ChoiceFormat("-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.");
//		System.out.println(mf.format(new Double[]{365.4d}));
		byte[] bytes=Files.readAllBytes(Paths.get("selena.jp2"));
		ImageInputStream imageis=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes));
		ImageReader reader=ImageIO.getImageReaders(imageis).next();
		String formatName=reader.getFormatName();
		Image fximage=new Image((InputStream)imageis);
//		BufferedImage image = reader.read(0);
//		fximage=SwingFXUtils.toFXImage(image, null);
		
	}
}
