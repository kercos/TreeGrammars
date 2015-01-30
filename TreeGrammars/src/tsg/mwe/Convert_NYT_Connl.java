package tsg.mwe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

import util.PrintProgress;

public class Convert_NYT_Connl {
	
	String wordSuffix = ".words";
	String lemmaSuffix = ".lemmas";
	String posSuffix = ".pos";
	
	//File wordFile, lemmaFile, posFile;

	public Convert_NYT_Connl(String inputFile, File outputFile) throws FileNotFoundException {
		//wordFile = new File(inputFile+wordSuffix);
		//lemmaFile = new File(inputFile+lemmaSuffix);
		File posFile = new File(inputFile+posSuffix);
		//Scanner scanWord = new Scanner(wordFile);
		//Scanner scanLemma = new Scanner(lemmaFile);
		Scanner scanPos = new Scanner(posFile);
		PrintWriter pw = new PrintWriter(outputFile);
		PrintProgress progress = new PrintProgress("Processing line", 100, 0);
		while(scanPos.hasNextLine()) {
			//String wordLine = scanWord.nextLine();
			//String[] wordLineSplit = wordLine.split(" ");
			//String lemmaLine = scanLemma.nextLine();
			//String[] lemmaLineSplit = lemmaLine.split(" ");
			String posLine = scanPos.nextLine();
			progress.next();
			String[] posLineSplit = posLine.split(" ");
			for(String wp : posLineSplit) {
				String[] wpSplit = wp.split("/");
				pw.println(wpSplit[0] + "\t" + wpSplit[1]);
			}			
			pw.println();
		}
		progress.end();	
		pw.close();
	}

	public static void main(String[] args) throws Exception {
		String usage = "java Convert_NYT_Connl inputFile outputFile";
		if (args.length!=2) {
			System.err.println(usage);
			return;
		}
		new Convert_NYT_Connl(args[0], new File(args[1]));
		/*
		String l = "Following/VBG are/VBP excerpts/NNS from/IN a/DT speech/NN that/IN Gov./NNP George/NNP W./NNP Bush/NNP of/IN Texas/NNP delivered/VBN in/IN Fargo/NNP ,/, N.D./NNP ,/, last/JJ Thursday/NNP ./.";
		String[] ls = l.split(" ");
		String[] lss = ls[0].split("/");
		System.out.println(Arrays.toString(ls));
		System.out.println(Arrays.toString(lss));
		*/
	}
	
}
