package tsg.incremental.old;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import util.file.FileUtil;

public class IndexFinder {

	
	static final int[] indexes = new int[]{24, 35, 42, 62, 157, 196, 216, 219, 265, 299, 316, 452, 538, 541, 569, 647, 692, 702,
			713, 763, 784, 849, 872, 925, 932, 970, 1005, 1098, 1146, 1198, 1422, 1456, 1490, 1513, 1633};
	
	public static void findIndexes(String[] args) {
		Scanner scanA = FileUtil.getScanner(new File(args[0]));
		Scanner scanB = FileUtil.getScanner(new File(args[1]));
		ArrayList<Integer> indexes = new ArrayList<Integer>(); 
		while(scanA.hasNextLine()) {
			String lineA1 = scanA.nextLine();
			String lineA2 = scanA.nextLine();
			scanB.nextLine();
			String lineB2 = scanB.nextLine();
			if (lineA2.contains("true") && lineB2.contains("false"))
				indexes.add(Integer.parseInt(lineA1.substring(11)));
		}
		System.out.println(indexes);
	}
	
	public static void extractTreeFragIndexes(String[] args) {
		File inputFile = new File("log_all_coverage.txt");
		File outputTrees = new File("tree.mrg");
		File outputFrags = new File("lexFragSmall.mrg");
		
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pwTree = FileUtil.getPrintWriter(outputTrees);
		PrintWriter pwFrags = FileUtil.getPrintWriter(outputFrags);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.startsWith("Sentence #")) {
				int i = Integer.parseInt(line.substring(11));
				if (Arrays.binarySearch(indexes, i)>=0) {
					String treeLine = scan.nextLine();
					pwTree.println(treeLine);
					while(scan.hasNextLine()) {
						line = scan.nextLine();
						if (line.isEmpty())
							break;
						if (line.startsWith("\t"))
							pwFrags.println(line);
					}
				}				
			}
		}
		pwTree.close();
		pwFrags.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		findIndexes(args);
		//extractTreeFragIndexes(args);
	}
	


}
