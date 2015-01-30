package util;

public class Timer {
	
	long startTime;
	long totalTime;
	boolean isRunning;
	
	public Timer() {
	}
	
	public Timer(boolean startImmediatelly) {
		if (startImmediatelly)
			start();
	}
	
	public boolean start() {
		if (isRunning)
			return false;
		startTime = System.currentTimeMillis();		
		return isRunning = true;
	}
	
	public long stop() {
		if (!isRunning)
			return 0;		
		long currentTime = System.currentTimeMillis();
		long ellapsedTime = currentTime - startTime;
		totalTime += ellapsedTime;
		isRunning = false;
		return ellapsedTime;
	}
	
	public void reset() {
		isRunning = false;
		totalTime = 0;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public long getTotalTime() {
		return totalTime;
	}
	
	public int getTotalTimeSec() {
		return (int)(getTotalTime()/1000);
	}
	
	public long getEllapsedTime() {
		return System.currentTimeMillis() - startTime;
	}
	
	public int getEllapsedTimeSec() {
		return (int)(getEllapsedTime()/1000);
	}
	
}
