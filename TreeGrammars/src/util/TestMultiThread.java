package util;

import java.util.HashMap;

import tsg.TermLabel;


public class TestMultiThread extends Thread {

	static int mapSize = 2*1000*1000;
	static int threadMapSize = 1*1000*1000;
	static long threadCheckingTime = 1000;
	static long maxMsecPerThread = 20*1000;
	
	int threads;
	HashMap<TermLabel,TermLabel> globalMap;
	
	public TestMultiThread(int threads) {
		this.threads = threads;
		initMap();
	}
	
	public void initMap() {
		PrintProgress progress = new PrintProgress("Initializing Map", 10000, 0);
		globalMap = new HashMap<TermLabel,TermLabel>();
		for(int i=0; i<mapSize; i++) {			
			globalMap.put(makeTermLabel(i), makeTermLabel(i+1));
			progress.next();
		}
		progress.end();
	}
	
	private static TermLabel makeTermLabel(int i) {
		return TermLabel.getTermLabel(Integer.toString(i), false);
	}
	
	public void run() {	
		
		System.out.print("Starting threads... ");
		TestThread[] threadsArray = new TestThread[threads];
		for(int i=0; i<threads; i++) {
			TestThread t = new TestThread(i);
			threadsArray[i] = t;
			t.start();
		}
		System.out.println("done");
		
		try {
			Thread.sleep(1000);				
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	
		boolean someRunning = false;		
		do {						
			someRunning = false;
			for(int i=0; i<threads; i++) {
				TestThread t = threadsArray[i];
				if (t.isAlive()) {
					someRunning = true;					
					Timer wordTimer = t.threadTimer;
					long runningTime = wordTimer.getEllapsedTime();
					if (!t.isInterrupted() && runningTime > maxMsecPerThread) {
						t.interrupt();
						log("Thread " + i+ " interrupted (running sec. " + runningTime/1000 + ")");
					}
										
				}				
			}
			
			if (!someRunning)
				break;
			
			try {
				Thread.sleep(threadCheckingTime);				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		} while(true);
	}
	
	static synchronized void log(String s) {
		System.out.println(s);
	}
	
	protected class TestThread extends Thread {
		
		int threadIndex;
		Timer threadTimer;
		HashMap<TermLabel,TermLabel> threadMap;
		
		public TestThread(int threadIndex) {
			this.threadIndex = threadIndex;
		}
		
		public void run() {
			threadTimer = new Timer(true);
			while(true) {
				populateMap();
				//log("Thread " + threadIndex + " map pupulated " +
				//		"(running time " + threadTimer.getEllapsedTime()/1000 + ")");
				if (Thread.interrupted()) {	
					threadTimer.reset();
					threadTimer.start();
					log("Thread " + threadIndex + " restarted");										
			    }
				if (Utility.randomInteger(100)==1) {
					log("Thread " + threadIndex + " starting something heavy ");
					doSomethingHeavy();
					log("Thread " + threadIndex + " stopped something heavy ");
				}
			}
		}
		
		private void doSomethingHeavy() {
			HashMap<String,String> table = new HashMap<String,String>();
			String prefix1 = "fdasfdsafdsa";
			String prefix2 = "fdagrqewgbbg";
			for(int i=0; i<10000000; i++) {
				String intString = Integer.toString(i);
				table.put(prefix1+intString, prefix2+intString);
			}
			
		}

		private void populateMap() {
			threadMap = new HashMap<TermLabel,TermLabel>();
			for(int i=0; i<threadMapSize; i++) {
				TermLabel key = makeTermLabel(Utility.randomInteger(threadMapSize));
				TermLabel value = globalMap.get(key);
				if (value!=null) {
					threadMap.put(key,value);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		new TestMultiThread(5).run();
	}
	
}
