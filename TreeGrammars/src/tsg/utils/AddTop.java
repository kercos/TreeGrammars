package tsg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

import util.file.FileUtil;

public class AddTop {

	public static void main(String[] args) {
		boolean separateOutput = args.length > 1;
		File inputFile = new File(args[0]);
		File outputFile = separateOutput ? new File(args[1]) : new File(args[0] + ".tmp");
		Scanner scan = FileUtil.getScanner(inputFile);
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			line = fixLine(line);
			pw.println(line);			
		}
		pw.close();
		if (!separateOutput)
			outputFile.renameTo(inputFile);
	}
	
	public static String fixLine(String line) {
		if (line.isEmpty() || line.startsWith("(TOP"))
			return line;
		if (line.startsWith("(ROOT "))
			return "(TOP " + line.substring(6);
		if (line.startsWith("( ("))
			return "(TOP " + line.substring(2);
		else if (line.startsWith("(("))
			return "(TOP " + line.substring(1);
		else
			return "(TOP " + line + ")";
	}
	
}
