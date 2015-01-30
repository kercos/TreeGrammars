package util;

import settings.Parameters;

public class PrintProgressPercentage extends PrintProgress {
	
	long lastIndex;
	int lastPrintedPercentage = -1; 
	boolean printOnlyPercentage;
	
	public PrintProgressPercentage(long lastIndex, boolean printOnlyPercentage) {
		this("", 1, 0, lastIndex, false);
	}
	
	public PrintProgressPercentage(long lastIndex) {
		this("", 1, 0, lastIndex, false);
	}
	
	public PrintProgressPercentage(String startMessage, long lastIndex) {
		this(startMessage, 1, 0, lastIndex, false);			
	}
	
	public PrintProgressPercentage(String startMessage, long startIndex, long lastIndex) {
		this(startMessage, 1, startIndex, lastIndex, false);			
	}
	
	public PrintProgressPercentage(String startMessage, int printEvery, long startIndex, long lastIndex) {
		this(startMessage, printEvery, startIndex, lastIndex, false);		
	}
	
	public PrintProgressPercentage(String startMessage, int printEvery, long startIndex, long lastIndex, boolean printOnlyPercentage) {
		super(startMessage, printEvery, startIndex);
		if (currentIndex != 0) end();
		this.lastIndex = lastIndex;				
		this.printOnlyPercentage = printOnlyPercentage;	
	}

	
	protected void print(boolean toLog, boolean noBackSpace) {
		//long lastPrintedIndex = index==0 ? 0 : index - printEvery;
		int backSpacesIndex=0;
		int backSpacesPercentage=0;
		int spaceLength=0;
		if (!noBackSpace && lastPrintedIndex!=-1) {
			if (!printOnlyPercentage) {
				backSpacesIndex = Long.toString(lastPrintedIndex).length();			
				spaceLength = space.length();
			}
			backSpacesPercentage = Integer.toString(lastPrintedPercentage).length() + 1;
		}
		int backSpaces = backSpacesIndex + backSpacesPercentage + spaceLength;
		String back = Utility.fillChar(backSpaces, '\b');
		System.out.print(back);
		//System.out.println("\n" + "back " + backSpaces);
		int percentage = computePercentage();
		if (printOnlyPercentage)
			System.out.print(percentage + "%");
		else
			System.out.print(currentIndex + space + percentage + "%");
		if (toLog)
			Parameters.logPrint(currentIndex + space + percentage + "%");
		lastPrintedIndex = currentIndex;
		lastPrintedPercentage = percentage;
	}
	
	private int computePercentage() {
		return (int)(((double)currentIndex)/lastIndex*100);
	}
	
	public static void main(String[] args) throws InterruptedException {
		PrintProgressPercentage progress = new PrintProgressPercentage("Test", 1, 0, 100, true);
		for(int i=0; i<100; i++) {
			progress.next();	
			Thread.sleep(100);
		}
		progress.end();
	}

}
