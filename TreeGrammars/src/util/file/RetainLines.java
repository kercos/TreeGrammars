package util.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

import util.PrintProgress;

public class RetainLines {
		
	public static void retainFiles(File inputFile, File outputFile, File indexFile, boolean baseZero) throws FileNotFoundException {		
		
		HashSet<Integer> indexes = getIndexes(indexFile);
		
		@SuppressWarnings("resource")
		Scanner scanInput = new Scanner(inputFile, "utf-8");
		PrintWriter pw = new PrintWriter(outputFile);
		PrintProgress progress = new PrintProgress("Processing line", 100, 0);
		int lineIndex = baseZero ? -1 : 0;
		while(scanInput.hasNextLine()) {
			lineIndex++;
			progress.next();
			String lineInput = scanInput.nextLine().trim();
			if (indexes.contains(lineIndex))
				pw.println(lineInput);
		}
		progress.end();
		pw.close();
	}

	private static HashSet<Integer> getIndexes(File indexFile) throws FileNotFoundException {
		HashSet<Integer> indexes = new HashSet<Integer>();
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(indexFile, "utf-8");
		while(scan.hasNextLine()) {
			Integer i = Integer.parseInt(scan.nextLine());
			indexes.add(i);
		}
		return indexes;
	}

	public static void main(String[] args) throws FileNotFoundException {
		//String dir = "/Volumes/HardDisk/Scratch/CORPORA/TED_Parallel/en-it/";
		//args = new String[]{dir+"train.tags.en-it.it", dir+"train.tags.en-it.en", dir+"merged-it-en"};
		
		String usage = "RetainLines inputFile outputFile lineIndexFile baseZero (default=false)";
		if (args.length!=3 && args.length!=4) {
			System.err.println("Wrong number of parameters");
			System.err.println(usage);
			return;
		}		
		boolean baseZero = args.length==4 ? Boolean.parseBoolean(args[3]) : false;
		retainFiles(new File(args[0]),new File(args[1]), new File(args[2]), baseZero);

	}

}
