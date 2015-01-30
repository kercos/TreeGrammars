package tsg.trevor;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class SplitTrevorFile {

	static final String workingDirPath = "/Users/fedja/Work/Edinburgh/Trevor/";
	static final File trevorFile  = new File(workingDirPath + "tsg_grammar");
	static final File headFile = new File(workingDirPath + "tsg_grammar_head");
	static final File fragsFile = new File(workingDirPath + "tsg_grammar_frags");
	static final File TBFile = new File(workingDirPath + "tsg_grammar_TB");
	static final File fragsFileClean = new File(workingDirPath + "tsg_grammar_frags_clean");
	static final File TBFileClean = new File(workingDirPath + "tsg_grammar_TB_clean");
	
	public static void spliFile() {
		Scanner scan = FileUtil.getScanner(trevorFile);
		PrintWriter pwHead = FileUtil.getPrintWriter(headFile);
		PrintWriter pwFrags = FileUtil.getPrintWriter(fragsFile);
		PrintWriter pwTB = FileUtil.getPrintWriter(TBFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine().trim();
			if (line.startsWith("<"))
				pwHead.println(line);
			else if (line.startsWith("("))
				pwTB.println(line);
			else
				pwFrags.println(line);
		}
		pwHead.close();
		pwFrags.close();
		pwTB.close();
	}
	
	public static void cleanFragments() throws Exception {
		Scanner scan = FileUtil.getScanner(fragsFile);
		PrintWriter pw = FileUtil.getPrintWriter(fragsFileClean);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();			
			String[] lineSplit = line.split(" ",2);
			String freq = lineSplit[0];
			String fragString = lineSplit[1];
			TSNodeLabel frag = new TSNodeLabel(fragString);
			cleanTrevorFrag(frag);
			pw.println(frag.toString(false,true) + "\t" + freq);
		}	
		pw.close();
	}
	
	private static void cleanTrevorFrag(TSNodeLabel frag) {
		String stringLabel = frag.label();
		if (stringLabel.indexOf('@')!=-1) {			
			stringLabel = stringLabel.replaceAll("@", "");
			frag.relabel(stringLabel);
			frag.isLexical = false;
		}
		if (!frag.isTerminal()) {
			for(TSNodeLabel d : frag.daughters)
				cleanTrevorFrag(d);
		}		
	}
	
	public static void cleanTB() throws Exception {
		Scanner scan = FileUtil.getScanner(TBFile);
		PrintWriter pw = FileUtil.getPrintWriter(TBFileClean);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();			
			TSNodeLabel frag = new TSNodeLabel(line);
			cleanTrevorFrag(frag);
			pw.println(frag.toString());
		}	
		pw.close();
	}
	
	

	public static void main(String[] args) throws Exception {
		//spliFile();
		//cleanFragments();
		cleanTB();
	}
}
