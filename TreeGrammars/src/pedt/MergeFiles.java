package pedt;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import util.file.FileUtil;

public class MergeFiles {
	
	static String[] extensions = new String[]{".p",".a",".t"};
	static String[] endings = new String[]{"</pdata>","</adata>","</tdata>"};
	static int[] startingLines = new int[]{7,9,11};	

	private static void mergeTrees(File dir, File outputFile, int type) {
		File[] fileList = dir.listFiles();
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		boolean first = true;
		ArrayList<File> filteredFiles = new ArrayList<File>(); 
		for(File f : fileList) {
			if (f.getName().endsWith(extensions[type]) && !f.equals(outputFile))				
				filteredFiles.add(f);
		}
		Iterator<File> fileIter = filteredFiles.iterator();
		while(fileIter.hasNext()) {
			File f = fileIter.next();
			Scanner scan = FileUtil.getScanner(f);
			if (first) {
				for(int i=0; i<startingLines[type]; i++) {
					pw.println(scan.nextLine());
				}
				first = false;
			}
			else {
				for(int i=0; i<startingLines[type]; i++) {
					scan.nextLine();
				}
			}
			while(scan.hasNext()) {
				String line = scan.nextLine();
				String lineTrim = line.trim();
				if (!lineTrim.equals("</trees>") && !lineTrim.equals(endings[type])) {
					pw.println(line);
				}
			}
			if (!fileIter.hasNext()) {
				pw.println("  </trees>");
				pw.println(endings[type]);
			}
		}
		pw.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = "/scratch/fsangati/CORPUS/PEDT/data/000copy/";
		File dir = new File(path);
		int type = 2;		
		File outputFile = new File(path + "wsj-00" + extensions[type]);
		mergeTrees(dir, outputFile, type);
	}	

	

}
