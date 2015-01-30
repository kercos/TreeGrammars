package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class GetTreeLengthTreebank {

	public static void main(String[] args) throws Exception {
		
		File inputFile = new File(args[0]);
		int lengthLimit = -1;
		if (args.length>2) 
			lengthLimit = Integer.parseInt(args[2]);
		
		Scanner scan = FileUtil.getScanner(inputFile);
		ArrayList<Integer> lenghtList = new ArrayList<Integer>(); 
		
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel tree = new TSNodeLabel(line);
			lenghtList.add(tree.countLexicalNodes());
		}
		
		Collections.sort(lenghtList);
		System.out.println(lenghtList);
		
	}
	
}
