package ngram;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

import settings.Parameters;
import tsg.TSNodeLabel;
import util.Utility;
import util.file.FileUtil;

public class BlippTreebank87 {
	
	public static final String BASE_PATH = "/disk/scratch/fsangati/JointProjects/NgramParsing/data/bllip/";
	public static final String ORIGINAL_PATH = BASE_PATH + "original/";
	public static final String CLEAN_PATH = BASE_PATH + "clean/";
	public static final String CLEAN_PATH_SIMPLE = BASE_PATH + "cleanSimple/";
	public static final String traceTag = "-NONE-";

	
	public static void cleanTreebankCompress() throws Exception {
		
		File logFile = new File(BASE_PATH + "cleanTreebank.log");
		new File(CLEAN_PATH).mkdir();
		Parameters.openLogFile(logFile);
		
		File baseOriginalDir = new File(ORIGINAL_PATH);
		File[] yearsDir = baseOriginalDir.listFiles();
		Arrays.sort(yearsDir);
		int totalInputFileCounter = 0;
		int totalOutputFileCounter = 0;
		int totalTreeCounter = 0;
		for(File yDir : yearsDir) {
			String yearDirName = yDir.getName();
			if (!yearDirName.startsWith("1")) // 1987, 1988, 1989
				continue;
			
			Parameters.reportLine("Opening year dir: " + yDir);
			Parameters.reportLine("Creating new dir: " + yDir);
			String newYearPath = CLEAN_PATH + yearDirName + "/";
			File yDirOutput = new File(newYearPath);
			yDirOutput.mkdir();			
			File[] subDirs = yDir.listFiles();
			Arrays.sort(subDirs);			
			for(File subD : subDirs) {
				String subDname = subD.getName();
				if (!subDname.startsWith("w")) //w7_001 ... w7_127,  w8_001 ... w8_108,  w9_001 ... w9_041
					continue;			
				File newFile = new File(newYearPath + subDname);
				Parameters.reportLine("\tReading sub-dir: " + subD);
				Parameters.reportLine("\tCreating new clean file: " + newFile);
				totalOutputFileCounter++;
				PrintWriter pw = FileUtil.getPrintWriter(newFile);
				File[] singleFiles = subD.listFiles();
				Arrays.sort(singleFiles);
				int fileCounter = 0;
				int treeCounter = 0;
				for(File f : singleFiles) {
					String fName = f.getName();
					if (!fName.startsWith("w")) //w8_001.022
						continue;
					//System.out.println("Reading " + fName);					
					fileCounter++;					
					treeCounter += copyTreesOneLineAndClean(f, pw);
					
				}				
				pw.close();
				Parameters.reportLine("\t\tTotal files: " + fileCounter);
				Parameters.reportLine("\t\tContaing trees: " + treeCounter + "\n");
				totalInputFileCounter += fileCounter;
				totalTreeCounter += treeCounter;
			}
			
		}
		
		Parameters.reportLine("\n\n---------------------------------");
		Parameters.reportLine("Total input files: " + totalInputFileCounter);
		Parameters.reportLine("Total output files: " + totalOutputFileCounter);
		Parameters.reportLine("Total trees: " + totalTreeCounter + "\n");

		
		Parameters.closeLogFile();
		
	}
	
	public static int copyTreesOneLineAndClean(File inputFile, PrintWriter pw) throws Exception {
		Scanner reader = FileUtil.getScanner(inputFile);
		int treeCounter = 0;
		int parenthesis = 0;
		String tree = "";
		while(reader.hasNextLine()) {
			String line = reader.nextLine();
			if (line.length()==0) continue;
			if (line.indexOf('(')==-1 && line.indexOf(')')==-1) continue;
			int parethesisInLine = Utility.countParenthesis(line);			
			parenthesis += parethesisInLine;
			tree += line;
			if (parenthesis==0) {
				if (line.length()==0) continue;
				tree = tree.trim();				
				tree = tree.replaceAll("\\\\", "");
				tree = tree.replaceAll("\n", "");
				tree = tree.replaceAll("\\s+", " ");
				TSNodeLabel t = new TSNodeLabel(tree);
				t.pruneSubTrees(traceTag);
				t.removeSemanticTags();
				t.removeRedundantRules();
				//t.removeNumberInLabels();
				t.removeDashesInLabels();
				pw.println(t.toString());
				tree = "";
				treeCounter++;
			}				
		}
		reader.close();
		return treeCounter;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		cleanTreebankCompress();
	}

}
