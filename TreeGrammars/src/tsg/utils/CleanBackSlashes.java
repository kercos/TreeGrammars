package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import util.file.FileUtil;

public class CleanBackSlashes {

	public static void main(String[] args) {
		boolean separateOutput = args.length > 1;
		File inputFile = new File(args[0]);
		File outputFile = separateOutput ? new File(args[1]) : new File(args[0] + ".tmp");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.isEmpty()) continue;
			pw.println(line.replaceAll("\\\\", ""));			
		}
		pw.close();
		if (!separateOutput)
			outputFile.renameTo(inputFile);
	}
	
}
