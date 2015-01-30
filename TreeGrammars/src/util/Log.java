package util;

import java.io.PrintWriter;

public class Log {

	public static void printlnPwStdOut(PrintWriter pw, String line) {
		pw.println(line);
		System.out.println(line);
	}
	
}
