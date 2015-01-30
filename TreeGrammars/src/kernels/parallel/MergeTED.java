package kernels.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

import util.PrintProgress;

public class MergeTED {
	
	static char[] ignoreChars = new char[]{'<','/'};
	static {
		Arrays.sort(ignoreChars);
	}
	
	public static void mergeTed(File source, File target, File merged) throws FileNotFoundException {		
		Scanner scanSource = new Scanner(source, "utf-8");
		Scanner scanTarget = new Scanner(target, "utf-8");
		PrintWriter pw = new PrintWriter(merged);
		PrintProgress progress = new PrintProgress("Processing line", 100, 0);
		outer: while(scanSource.hasNextLine()) {
			progress.next();
			String lineSource = scanSource.nextLine().trim();
			String lineTarget = scanTarget.nextLine().trim();
			// ignore lines starting with <
			if (lineSource.isEmpty())
				continue;
			for(char c : ignoreChars) {
				if (lineSource.charAt(0)==c) {
					if (lineTarget.charAt(0)!=c) {
						System.err.println("Sync error at line: " + progress.currentIndex());
						return;
					}
					continue outer;
				}
			}			
			lineSource = cleanLine(lineSource);
			lineTarget = cleanLine(lineTarget);
			pw.println(lineSource + "\n" + lineTarget + "\n");
		}
		progress.end();
		pw.close();
	}

	private static String cleanLine(String line) {
		int commaIndex = line.indexOf(':');
		if (commaIndex==-1)
			return line;
		String[] wordsBeforeComma = line.substring(0, commaIndex).split("\\s");
		boolean allCaps = true;
		for(String w : wordsBeforeComma) {
			if (!w.isEmpty() && Character.isLowerCase(w.charAt(0))) {
				allCaps = false;
				break;
			}
		}		
		if (allCaps)
			return line.substring(commaIndex+1).trim();
		return line;
	}

	public static void main(String[] args) throws FileNotFoundException {
		//String dir = "/Volumes/HardDisk/Scratch/CORPORA/TED_Parallel/en-it/";
		//args = new String[]{dir+"train.tags.en-it.it", dir+"train.tags.en-it.en", dir+"merged-it-en"};
		
		String usage = "MergeTED source targe merged";
		if (args.length!=3) {
			System.err.println("Wrong number of parameters");
			System.err.println(usage);
			return;
		}		
		mergeTed(new File(args[0]),new File(args[1]),new File(args[2]));

	}

}
