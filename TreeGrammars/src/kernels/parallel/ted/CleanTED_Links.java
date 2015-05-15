package kernels.parallel.ted;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

import util.PrintProgress;

public class CleanTED_Links {
	
	static char[] ignoreChars = new char[]{'<','/'};
	static {
		Arrays.sort(ignoreChars);
	}
	
	public static void cleanTed(File inputFile, File outputFile) throws FileNotFoundException {		
		@SuppressWarnings("resource")
		Scanner scanInput = new Scanner(inputFile, "utf-8");
		PrintWriter pw = new PrintWriter(outputFile);
		PrintProgress progress = new PrintProgress("Processing line", 100, 0);
		outer: while(scanInput.hasNextLine()) {
			progress.next();
			String lineInput = scanInput.nextLine().trim();
			// ignore lines starting with <
			if (lineInput.isEmpty())
				continue;
			for(char c : ignoreChars) {
				if (lineInput.charAt(0)==c) {
					continue outer;
				}
			}			
			pw.println(lineInput);
		}
		progress.end();
		pw.close();
	}
	
	public static void cleanTed_TreeTagger() throws FileNotFoundException {
		String dir = "/Volumes/HardDisk/Scratch/CORPORA/TED_Parallel/en-it/giza_align/";
		File fileEN = new File(dir + "train.tags.en-it.tok.selection.onewpl.TTposlemmas.en");
		File fileIT = new File(dir + "train.tags.en-it.tok.selection.onewpl.TTposlemmas.it");
		// word pos lemma
		
		HashSet<String> enPosSet = new HashSet<String>(); 
		HashSet<String> itPosSet = new HashSet<String>();
		HashSet<String> itPosSetSmall = new HashSet<String>();
		
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(fileEN);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			enPosSet.add(lineSplit[1]);
		}
		
		scan = new Scanner(fileIT);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			String tag = lineSplit[1];
			itPosSet.add(tag);
			int commaIndex = tag.indexOf(':');
			if (commaIndex!=-1) {
				tag = tag.substring(0, commaIndex);
				itPosSetSmall.add(tag);
			}
			else
				itPosSetSmall.add(tag);
		}	
		
		System.out.println("EN tag set (" + enPosSet.size() + "): " + enPosSet);
		System.out.println("IT tag set (" + itPosSet.size() + "): " + itPosSet);
		System.out.println("IT tag set small (" + itPosSetSmall.size() + "): " + itPosSetSmall);
		
	}

	public static void main(String[] args) throws FileNotFoundException {
		//String dir = "/Volumes/HardDisk/Scratch/CORPORA/TED_Parallel/en-it/";
		//args = new String[]{dir+"train.tags.en-it.it", dir+"train.tags.en-it.en", dir+"merged-it-en"};
		
		/*
		String usage = "CleanTED inputFile outputFile";
		if (args.length!=2) {
			System.err.println("Wrong number of parameters");
			System.err.println(usage);
			return;
		}		
		cleanTed(new File(args[0]),new File(args[1]));
		*/
		
		cleanTed_TreeTagger();
	}

}
