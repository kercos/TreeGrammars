package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class MakeTreebankFlat {

	public static void main(String[] args) throws Exception {
		
		File inputFile = new File(args[0]);
		File outputFile = new File(args[1]);
		int lengthLimit = -1;
		if (args.length>2) 
			lengthLimit = Integer.parseInt(args[2]);
		
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			if (lengthLimit==-1) {			
				pw.println(t.toFlatSentence());
			}
			else {
				if (t.countLexicalNodes()>lengthLimit)
					continue;
				pw.println(t.toFlatSentence());
			}
		}
		
		pw.close();
		
	}
	
}
