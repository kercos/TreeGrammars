package tsg.utils;

import java.io.File;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.file.FileUtil;

public class FindFragmentInTreebank {

	public static void main(String[] args) throws Exception {
		args = new String[]{
				"/disk/scratch/fsangati/PLTSG/MB_ROARK_Right_H0_V1_UkM4_UkT4/wsj-02-21.mrg",
				"(S (S NP (S@ (VP (AUX \"were\") VP@))) S@)"};
		File treebankFile = new File(args[0]);
		String fragmentString = args[1];
		TSNodeLabel fragment = new TSNodeLabel(fragmentString, false);
		Scanner scan = FileUtil.getScanner(treebankFile);
		TSNodeLabel shortest = null;
		int minLength = Integer.MAX_VALUE;
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			TSNodeLabel t = new TSNodeLabel(line);
			if (t.containsRecursiveFragment(fragment)) {
				System.out.println(t.toFlatSentence());
				int length = t.countLexicalNodes();
				if (length<minLength) {
					minLength = length;
					shortest = t;
				}
			}
		}
		System.out.println("\nShortest:");
		System.out.println(shortest.toStringTex(false,true));
		System.out.println(shortest.toFlatSentence());
		
	}
	
}
