package util;

import java.text.DecimalFormat;

public class SimpleTimer {
	
	static DecimalFormat threeDigitFormatter = new DecimalFormat("0.00");
	
	long startTime=0;
	long totalTime=0;
	boolean isRunning;
	
	public SimpleTimer() {
	}
	
	public boolean isStarted() {
		return startTime!=0;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public SimpleTimer(boolean startImmediatelly) {
		if (startImmediatelly)
			start();
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
		isRunning = true;
	}
	
	public long stop() {
		if (!isStarted()) return 0;
		isRunning = false;		
		totalTime = System.currentTimeMillis() - startTime;
		startTime = 0;
		return totalTime;
	}
	
	public long readTotalTimeMsec() {
		return totalTime;
	}
	
	public int readTotalTimeSec() {
		return (int)(totalTime/1000);
	}
	
	public String readTotalTimeSecFormat() {
		double sec = ((double)totalTime)/1000;
		return threeDigitFormatter.format(sec);
	}
	
	public long checkEllapsedTimeMsec() {	
		if (!isStarted() || !isRunning) return 0;
		return System.currentTimeMillis() - startTime;
	}
	
	public int checkEllapsedTimeSec() {
		return (int)(checkEllapsedTimeMsec()/1000);
	}	
	
	public float checkEllapsedTimeSecDecimal() {
		return checkEllapsedTimeMsec()/1000f;
	}
	
	public String checkEllapsedTimeSecFormat() {
		if (!isStarted())
			return "-";
		double sec = ((double)checkEllapsedTimeMsec())/1000;
		return threeDigitFormatter.format(sec);
	}

	
	
	public static void main(String[] args) {
		SimpleTimer st = new SimpleTimer();
		System.out.println(st.checkEllapsedTimeSec());
		//System.out.println(threeDigitFormatter.format(2.688));
	}


	
}
