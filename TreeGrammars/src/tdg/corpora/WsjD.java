package tdg.corpora;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tdg.TDNode;

public class WsjD {
	public static String WsjDepBase = Parameters.corpusPath + "WSJ/DEPENDENCY/";
	public static String WsjYM = WsjDepBase + "YM/";
	public static String WsjPenn2Malt = WsjDepBase + "PENN2MALT/";
	public static String WsjCONLL07 = WsjDepBase + "CONLL07/";
	public static String WsjCOLLINS97 = WsjDepBase + "COLLINS97/";
	public static String WsjCOLLINS97_ulab = WsjDepBase + "COLLINS97_ulab/";
	public static String WsjCOLLINS99_ulab = WsjDepBase + "COLLINS99_ulab/"; 
	public static String WsjCOLLINS99Arg_ulab = WsjDepBase + "COLLINS99Arg_ulab/"; //with arguments marks
	public static String WsjCOLLINS99Ter_ulab = WsjDepBase + "COLLINS99Ter_ulab/"; //with terminal marks
	public static String WsjCOLLINS99ArgTer_ulab = WsjDepBase + "COLLINS99ArgTer_ulab/"; //with arguments and terminals	
	public static String WsjMSTulab = WsjDepBase + "MST_ulab/";
	public static String WsjFAMILIARITYPosSpine = WsjDepBase + "FamiliarityTop_SpinePOS/";
	
	
	public static void makeWsjCOLLINS99Ter(File inputFile, File outputFile) {
		ArrayList<TDNode> depCorpus = DepCorpus.readTreebankFromFileMST(inputFile, 1000, false, false);
		for(TDNode t : depCorpus) {
			t.markTerminalNodesPOS();
		}
		DepCorpus.toFileMSTulab(outputFile, depCorpus, false);
	}
	
	public static void makeWsjCOLLINS99ArgTer() {
		File WsjCollins99ArgDir = new File(WsjCOLLINS99Arg_ulab);
		File[] fileList = WsjCollins99ArgDir.listFiles();
		for(File inputFile : fileList) {
			File outputFile = new File(WsjCOLLINS99ArgTer_ulab + inputFile.getName());
			ArrayList<TDNode> depCorpus = DepCorpus.readTreebankFromFileMST(inputFile, 1000, false, false);
			for(TDNode t : depCorpus) {
				t.markTerminalNodesPOS();
			}
			DepCorpus.toFileMSTulab(outputFile, depCorpus, false);
		}	
	}
	
	public static void main(String[] args) {
		makeWsjCOLLINS99ArgTer();
	}
}
