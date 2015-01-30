package tsg.corpora;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;



public class FTB {

	
	public static void adjustParenthesisation(File startDir, File outputDir) {
		outputDir.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);						
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;						
			File newFile = new File(outputDir + "/" + inputFile.getName());
			Scanner scan = FileUtil.getScanner(inputFile);
			PrintWriter pw = FileUtil.getPrintWriter(newFile);
			while(scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.isEmpty()) continue;
				pw.println(adjustParenthesisation(line));
			}
			pw.close();
		}
	}
	
	public static void addTop(File startDir, File outputDir) {
		outputDir.mkdirs();
		File[] fileList = startDir.listFiles();
		Arrays.sort(fileList);						
		for(File inputFile : fileList) {
			if (inputFile.isDirectory()) continue;						
			File newFile = new File(outputDir + "/" + inputFile.getName());
			ConstCorpus.addTop(inputFile, newFile);
		}
	}
	
	public static String adjustParenthesisation(String line) {
		if (line.startsWith("(  (")) {
			int lastPIndex = line.lastIndexOf(')');
			return line.substring(3, lastPIndex);
		}		
		return line;
	}
	
	public static void makeCharBased(File inputFile, File outputFile) throws Exception {		
		if (inputFile.isDirectory()) {
			File[][] srcDstFiles = Wsj.getFilePairs(inputFile, outputFile);
			int size = srcDstFiles[0].length;
			for(int i=0; i<size; i++) {
				makeCharBased(srcDstFiles[0][i], srcDstFiles[1][i]);				
			}
			return;
		}
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		ArrayList<TSNodeLabel> treebank = TSNodeLabel.getTreebank(inputFile);
		for(TSNodeLabel t : treebank) {
			t.makeTreeCharBased();			
			pw.println(t.toString());
		}
		pw.close();
	}
	
	public static void main(String[] args) throws Exception {
		
		String baseDir = "/scratch/fsangati/CORPUS/FrenchTreebank/";
		String OriginalUTF8Dir = baseDir + "OriginalUTF8/";
		String CleanDir = baseDir + "Clean/";
		String CleanTopDir = baseDir + "CleanTOP/";
		String CleanTopCharbasedDir = baseDir + "CleanTOP_CharBased/";
		
		//adjustParenthesisation( new File(OriginalUTF8Dir), new File(CleanDir));
		//addTop( new File(CleanDir), new File(CleanTopDir));
		makeCharBased(new File(CleanTopDir), new File(CleanTopCharbasedDir));
	}

	
}
