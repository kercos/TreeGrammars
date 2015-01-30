package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import tsg.Label;
import tsg.TSNodeLabel;
import tsg.TermLabel;
import util.file.FileUtil;

public class AddStop {

	static final Label STOPlabel = Label.getLabel("||STOP||");
	
	public static void addStop(File inputFile, File outputFile) throws Exception {
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			TSNodeLabel tree = new TSNodeLabel(scan.nextLine());
			if (tree.prole()!=1) {
				System.err.println("Root has not unique daughter: " + tree);
				return;
			}
			addTop(tree);
			pw.println(tree.toString());
		}
		pw.close();
	}
	
	private static void addTop(TSNodeLabel tree) {		
		TSNodeLabel stopPos = new TSNodeLabel(STOPlabel, false);
		TSNodeLabel stopLex = new TSNodeLabel(STOPlabel, true);
		stopPos.assignUniqueDaughter(stopLex);
		TSNodeLabel firstDaughter = tree.firstDaughter();
		TSNodeLabel[] newDaughters = new TSNodeLabel[]{firstDaughter, stopPos};
		tree.assignDaughters(newDaughters);		
	}

	public static void main(String[] args) throws Exception {
		File inputDir = new File("/afs/inf.ed.ac.uk/user/f/fsangati/CORPORA/WSJ/chelba/ORIGINAL");
		String outputDir = "/afs/inf.ed.ac.uk/user/f/fsangati/CORPORA/WSJ/chelba/STOP/";
		File[] inputFiles = inputDir.listFiles();
		for(File f : inputFiles) {
			String fileName = f.getName();
			if (fileName.contains("flat"))
				continue;
			File of = new File(outputDir + fileName);
			addStop(f,of);
		}
	}
	

	
}
