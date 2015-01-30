package util.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

import util.PrintProgress;

public class CleanParallel {
	
	/**
	 * 
	 * @param inputFileSrc
	 * @param inputFileTrg
	 * @param outputFileSrc
	 * @param outputFileTrg
	 * @throws FileNotFoundException
	 *  lowercase the text
	 *  strip empty lines and their correspondences
	 *  remove lines with XML-Tags (starting with "<") 
	 */
	@SuppressWarnings("resource")
	public static void clean(File inputFileSrc, File inputFileTrg, File outputFileSrc, File outputFileTrg) throws FileNotFoundException {		
		Scanner scanInputSrc = new Scanner(inputFileSrc);
		Scanner scanInputTrg = new Scanner(inputFileTrg);
		PrintWriter pwSrc = new PrintWriter(outputFileSrc);
		PrintWriter pwTrg = new PrintWriter(outputFileTrg);
		PrintProgress progress = new PrintProgress("Processing line", 100, 0);
		int emptyLines = 0;
		int xmlLines = 0;
		while(scanInputSrc.hasNextLine()) {
			progress.next();
			String lineSrc = scanInputSrc.nextLine().trim().toLowerCase();
			String lineTrg = scanInputTrg.nextLine().trim().toLowerCase();
			// ignore lines starting with <
			if (lineSrc.isEmpty() || lineTrg.isEmpty()) {
				emptyLines++;
				continue;
			}				
			if (lineSrc.charAt(0)=='<' || lineTrg.charAt(0)=='<') {
				xmlLines++;
				continue;
			}			
			pwSrc.println(lineSrc);
			pwTrg.println(lineTrg);
		}
		progress.end();
		pwSrc.close();
		pwTrg.close();
		System.out.println("Removed empty lines: " + emptyLines);
		System.out.println("Removed lines starting with '<': " + xmlLines);
	}

	public static void main(String[] args) throws FileNotFoundException {
		//String dir = "/Volumes/HardDisk/Scratch/CORPORA/TED_Parallel/en-it/";
		//args = new String[]{dir+"train.tags.en-it.it", dir+"train.tags.en-it.en", dir+"merged-it-en"};
		
		String usage = "CleanParallel inputFileSrc inputFileTrg outputFileSrc outputFileTrg";
		if (args.length!=4) {
			System.err.println("Wrong number of parameters");
			System.err.println(usage);
			return;
		}		
		clean(new File(args[0]),new File(args[1]),new File(args[2]),new File(args[3]));

	}

}
