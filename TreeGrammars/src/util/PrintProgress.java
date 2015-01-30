package util;

import settings.Parameters;

public class PrintProgress {
		
	final String space = "   ";
	protected long currentIndex = 0;
	protected long lastPrintedIndex = -1;
	protected int printEvery;
	protected String startMessage;
	
	public PrintProgress() {
		this("", 1, 0);
	}
	
	public PrintProgress(String startMessage) {
		this(startMessage, 1, 0);			
	}
	
	public PrintProgress(String startMessage, int startIndex) {
		this(startMessage, 1, startIndex);			
	}
	
	public PrintProgress(String startMessage, int printEvery, long startIndex) {
		if (currentIndex != 0) end();
		this.currentIndex = startIndex;
		this.printEvery = printEvery;		
		this.startMessage = startMessage;
		Parameters.logStdOutPrint(startMessage + space);
	}
	
	public synchronized void next() {
		currentIndex++;
		checkIfTimeToPrintAndPrint();			
	}
	
	public void next(int toAdd) {
		currentIndex += toAdd;
		checkIfTimeToPrintAndPrint();	
	}
	
	protected void checkIfTimeToPrintAndPrint() {
		if (currentIndex % printEvery == 0) {
			print(false, false);		
		}
	}
	
	public void suspend() {
		print(true, false);
		Parameters.logStdOutPrintln(" ... suspended");
	}
	
	public void resume() {
		Parameters.logStdOutPrint(startMessage + space);
		print(false, true);
	}
	
	protected void print(boolean toLog, boolean noBackSpace) {
		//long lastPrintedIndex = index==0 ? 0 : index - printEvery;
		int backSpaces = (noBackSpace || lastPrintedIndex==-1) ? 0 : Long.toString(lastPrintedIndex).length()+1;
		String back = Utility.fillChar(backSpaces, '\b');
		System.out.print(back);
		System.out.print(currentIndex + " ");
		if (toLog)
			Parameters.logPrint(currentIndex + " ");
		lastPrintedIndex = currentIndex;
	}
	
	public long currentIndex() {
		return currentIndex;
	}
		
	public void end() {		
		end("...done");
	}
	
	public void end(String endMessage) {
		//if (currentIndex != lastPrintedIndex) 
		print(true, false);
		Parameters.logStdOutPrintln(" " + endMessage);
		currentIndex = 0;
		lastPrintedIndex = -1;
	}
	
}
