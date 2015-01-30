package util;

import settings.Parameters;

public class PrintProgressStatic {
	
	private static int index = 0;
	private static int printEvery;
	public static boolean useBackcarriege = true;
	private static final String separationOutput = useBackcarriege ? "\b" : " ";
	
	public static void start(String startMessage) {
		start(startMessage, 1, 0);			
	}
	
	public static void start(String startMessage, int startIndex) {
		start(startMessage, 1, startIndex);			
	}
	
	public static void start(String startMessage, int printEvery, int startIndex) {
		if (index != 0) end();
		index = startIndex;
		PrintProgressStatic.printEvery = printEvery;
		reportLog(startMessage + "        ");
	}
	
	public static void next() {
		index++;
		if (index % printEvery == 0) {
			deleteAndPrintCurrent();
		}
	}
	
	public static void deleteAndPrintCurrent() {
		int lastPrintedIndex = index==0 ? 0 : index - printEvery;
		int backSpaces = (lastPrintedIndex==0) ? 0 : Integer.toString(lastPrintedIndex).length();
		for(int i=0; i<backSpaces; i++) reportLog(separationOutput);
		System.out.print(index);
	}
	
	public static int currentIndex() {
		return index;
	}
		
	public static void end() {		
		end("...done");
	}
	
	public static void end(String endMessage) {
		deleteAndPrintCurrent();
		System.out.print(" " + endMessage + "\n");
		index = 0;
	}
	
	public static void reportLog(String log) {
		System.out.println(log);
		Parameters.appendReturnInLogFile(log);
	}
}
