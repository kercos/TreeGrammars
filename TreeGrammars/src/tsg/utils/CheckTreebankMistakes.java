package tsg.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class CheckTreebankMistakes {

	
	public static void main(String[] args) {
		File inputTreebank = new File(args[0]);
		boolean addTop = false;
		if (args.length>1) {
			addTop = Boolean.parseBoolean(args[1]);
		}
		Scanner scan = FileUtil.getScanner(inputTreebank);
		int count = 0;
		while(scan.hasNextLine()) {
			count++;		
			String line = scan.nextLine();
			if (addTop)
				line = AddTop.fixLine(line);
			try {
				new TSNodeLabel(line);
			} catch (Exception e) {
				System.err.println("Found error in tree index: " + count);
				System.err.println("Tree: " + line);
			}				
		}		
		System.out.println("Read " + count + " trees.");		
	} 
	
}
