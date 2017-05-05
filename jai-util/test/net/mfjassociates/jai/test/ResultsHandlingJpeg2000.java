package net.mfjassociates.jai.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;

public class ResultsHandlingJpeg2000 {

	public static void main(String[] args) throws IOException {
		createImageWriteParams();
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
	
	private static final String CSV_ROW_FORMAT = "\"{8}\",{0},{1},{2,choice,0<{2,number,'##.###'}|10000<{2,number,'#.###E0'}},{3},{4},\"{5}\",{6,number,#.###},{7,number,#.###},\"{9}\"";
	private static final String JUNIT_NAME_FORMAT = "[{7}: cbs={0}, ndl={1}, er={2,choice,0<{2,number,'##.###'}|10000<{2,number,'#.###E0'}}, ct={3}, ll={4}, filter={5}, compression={6,number,#.###}]";
	private static final String CSV_ROW_HEADER = "\"Filename\",\"CodeBlockSize\",\"DecomposLevels\",\"EncodingRate\",\"CompTransform\",\"Lossless\",\"Filter\",\"Compression\",\"Size\",\"JUnitName\"";
	private static final Path outdir=Paths.get("H:\\git\\jai-tools\\jai-util\\outimgs");
	private static final Path resultsCsv=Paths.get("results.csv");
	private static void createImageWriteParams() throws IOException {
		boolean[] testLossless=new boolean[]{true, false};
		int[] codeBlockSizes=new int[]{4, 8, 32, 64};
		int[] ndls=new int[] {1, 5, 7, 10};
		String[] filters=new String[]{J2KImageWriteParam.FILTER_53, J2KImageWriteParam.FILTER_97};
		float[] compressions=new float[] {0.1f, 0.5f, 0.75f, 1f};
		PrintStream csv=new PrintStream(Files.newOutputStream(resultsCsv));
		csv.println(CSV_ROW_HEADER);
		int testno=0;
		for (boolean aLossless : testLossless) {
			double[] encodingRates=aLossless?new double[]{Double.MAX_VALUE}:new double[] {.1d, 5d, 100d, Double.MAX_VALUE};
			for (int cbs : codeBlockSizes) {
				for (int ndl : ndls) {
					for (double encodingRate : encodingRates) {
						for (String filter : filters) {
							for (float compression : compressions) {
								String fn=MessageFormat.format("cbs{0}ndl={1}er={2,choice,0<{2,number,'##.###'}|10000<{2,number,'#.###E0'}}ct={3}ll={4}f={5}c={6,number,#.###}", 
										new CodeBlockSize(cbs), ndl, encodingRate, true, aLossless, filter, compression)+".jp2";
								String junitName=MessageFormat.format(JUNIT_NAME_FORMAT, 
										new CodeBlockSize(cbs), ndl, encodingRate, true, aLossless, filter, compression, testno++);
								Path imagePath=outdir.resolve(fn);
								long size=Files.size(imagePath);
								String row=MessageFormat.format(CSV_ROW_FORMAT,
										cbs, ndl, encodingRate, true, aLossless, filter, compression, size, imagePath.getFileName(), junitName);
								csv.println(row);
								
							}
							//return parms;
						}
					}
				}
			}
		}
		csv.close();
	}
//	private static String outImageFilename() {
//		String fn=MessageFormat.format("cbs{0}ndl={1}er={2,choice,0<{2,number,'##.###'}|10000<{2,number,'#.###E0'}}ct={3}ll={4}f={5}c={6,number,#.###}", 
//				codeBlockSize, numDecompositionLevels, encodingRate, componentTransformation, lossless, filter, saveCompression);
//		return fn+".jp2";
//	}
	
}
