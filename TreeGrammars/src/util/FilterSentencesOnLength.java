package util;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class FilterSentencesOnLength {

	public static void main(String[] args) throws Exception {
		int legnthLimit = 10;
		File inputTreebankFile = 
			new File("/disk/scratch/fsangati/PLTSG/Chelba/f23-24.unk10.txt");		
		File outputTreebankFile = 
			new File("/disk/scratch/fsangati/PLTSG/Chelba/f23-24.unk10_upTo" 
			+ legnthLimit + ".txt");
		File outputFlatFile = 
			new File("/disk/scratch/fsangati/PLTSG/Chelba/f23-24.unk10_upTo" 
			+ legnthLimit + ".flat.txt");
		
		Scanner scan = FileUtil.getScanner(inputTreebankFile);
		PrintWriter pwTB = FileUtil.getPrintWriter(outputTreebankFile);
		PrintWriter pwFlat = FileUtil.getPrintWriter(outputFlatFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			if (tree.countLexicalNodes()>legnthLimit)
				continue;
			pwTB.println(line);
			pwFlat.println(tree.toFlatSentence());
		}
		pwTB.close();
		pwFlat.close();
		
	}
	
}
