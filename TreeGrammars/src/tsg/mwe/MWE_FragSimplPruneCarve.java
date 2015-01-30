package tsg.mwe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

import tsg.TSNodeLabel;
import util.Log;
import util.PrintProgress;

public class MWE_FragSimplPruneCarve {
	
	double lowerThreshold, upperThreshold;
	
	HashMap<TSNodeLabel, int[]> fragsFreq = new HashMap<TSNodeLabel, int[]>();  

	public MWE_FragSimplPruneCarve(File inputFile, File outputFile, double lowerThreshold) throws Exception {
		this.lowerThreshold = lowerThreshold;
		this.upperThreshold = 1d - lowerThreshold;
		readFragments(inputFile);
		simplifyFrags(new File(outputFile + ".simplifyLog"));
		pruneFrags(new File(outputFile + ".pruningLog"));
		carveFrags(new File(outputFile + ".carvingLog"));
		printRemainingFrags(outputFile);
	}



	private void readFragments(File inputFile)  throws Exception {
		PrintProgress progress = new PrintProgress("Reading frags", 1000, 0);
		Scanner scan = new Scanner(inputFile);
		int countFrags = 0;
		while(scan.hasNextLine()) {
			progress.next();
			countFrags++;
			String line = scan.nextLine();
			String[] lineSplit = line.split("\t");
			TSNodeLabel frag = TSNodeLabel.newTSNodeLabelStd(lineSplit[0]);
			int freq = Integer.parseInt(lineSplit[1]);			
			fragsFreq.put(frag, new int[]{freq});
		}
		progress.end();	
		System.out.println("Total frags: " + countFrags);
	}
	
	private void simplifyFrags(File fileLog) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(fileLog);
		PrintProgress progress = new PrintProgress("Simplifing frags", 1000, 0);
		int simplifiedFrag = 0;
		int simplifiedAndAlreadyPresentFrags = 0;
		for(Entry<TSNodeLabel, int[]> e : fragsFreq.entrySet()) {
			progress.next();
			TSNodeLabel frag = e.getKey();
			int[] freq = e.getValue();
			boolean pruned = frag.prunePhraseNodeNotYieldingLexicalItems();
			if (pruned) {
				if (fragsFreq.containsKey(frag)) {
					freq[0] = -1;
					simplifiedAndAlreadyPresentFrags++;
				}
				simplifiedFrag++;
			}			
		}		
		progress.end();	
		Log.printlnPwStdOut(pw, "Total frags simplified: " + simplifiedFrag);
		Log.printlnPwStdOut(pw, "Of which already present: " + simplifiedAndAlreadyPresentFrags);
		pw.close();
	}
	
	
	private void pruneFrags(File fileLog) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(fileLog);
		
		int prunedFrag = 0;
		PrintProgress progress = new PrintProgress("Pruning frags", 1000, 0);		
		//PRUNING FRAGS
		outer : for(Entry<TSNodeLabel, int[]> e : fragsFreq.entrySet()) {
			progress.next();
			TSNodeLabel frag = e.getKey();
			String specificFrag = frag.toStringStandard();
			int[] freq = e.getValue();
			int specificFreq = freq[0];
			if (specificFreq==-1)
				continue;
			ArrayList<TSNodeLabel> interns = frag.collectInternalNodes();
			
			for(TSNodeLabel i : interns) {
				TSNodeLabel[] d = i.daughters;
				i.daughters = null;
				int[] prunedFragFreq = fragsFreq.get(frag);
				boolean removedSpecific = false;
				if (prunedFragFreq!=null) {
					int genericFreq = prunedFragFreq[0];
					if (genericFreq!=-1) {
						double ratio = (double)specificFreq / (double)genericFreq;
						if (ratio>upperThreshold) {
							prunedFrag++;
							prunedFragFreq[0] = -1;
							// removed more generic						
							pw.println(specificFrag + "\t" + specificFreq + " " + "SPECIFIC [KEPT]" + " " + ratio);
							pw.println(frag.toStringStandard() + "\t" + genericFreq + " " + "GENERIC [REMOVE]" + " " + ratio);
							// print specifc (kept) and generic (removed)
						}
						else if (ratio<lowerThreshold) {
							prunedFrag++;
							freq[0] = -1;
							removedSpecific = true;							
							// removed more specific
							pw.println(frag.toStringStandard() + "\t" + genericFreq + " " + "GENERIC [KEPT]" + " " + ratio);
							pw.println(specificFrag + "\t" + specificFreq + " " + "SPECIFIC [REMOVED]" + " " + ratio);
							// print generic (kept) and specific (removed)
						}
					}						
				}				
				i.daughters = d;
				if (removedSpecific)
					continue outer;
			}
		}		
		progress.end();		
		Log.printlnPwStdOut(pw, "Total pruned frags: " + prunedFrag);
		pw.close();
	}
	

	private void carveFrags(File fileLog) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(fileLog);
		
		int totalCarvedFrag = 0;
		PrintProgress progress = new PrintProgress("Carving frags", 1000, 0);		
		//PRUNING FRAGS
		outer : for(Entry<TSNodeLabel, int[]> e : fragsFreq.entrySet()) {
			progress.next();
			TSNodeLabel frag = e.getKey();
			String specificFrag = frag.toStringStandard();
			int[] freq = e.getValue();
			int specificFreq = freq[0];
			if (specificFreq==-1)
				continue;
			//interns might be too drastic
			//ArrayList<TSNodeLabel> interns = frag.collectInternalNodes();
			for(TSNodeLabel carvedFrag : frag.daughters) { 
				int[] carvedFragFreq = fragsFreq.get(carvedFrag);
				boolean removedSpecific = false;
				if (carvedFragFreq!=null) {
					int genericFreq = carvedFragFreq[0];
					if (genericFreq!=-1) {
						double ratio = (double)specificFreq / (double)genericFreq;
						if (ratio>upperThreshold) {
							totalCarvedFrag++;
							carvedFragFreq[0] = -1;
							// removed more generic						
							pw.println(specificFrag + "\t" + specificFreq + " " + "SPECIFIC [KEPT]" + " " + ratio);
							pw.println(carvedFrag.toStringStandard() + "\t" + genericFreq + " " + "GENERIC [REMOVE]" + " " + ratio);
							// print specifc (kept) and generic (removed)
						}
						else if (ratio<lowerThreshold) {
							totalCarvedFrag++;
							freq[0] = -1;
							removedSpecific = true;							
							// removed more specific
							pw.println(carvedFrag.toStringStandard() + "\t" + genericFreq + " " + "GENERIC [KEPT]" + " " + ratio);
							pw.println(specificFrag + "\t" + specificFreq + " " + "SPECIFIC [REMOVED]" + " " + ratio);
							// print generic (kept) and specific (removed)
						}
					}						
				}		
				if (removedSpecific)
					continue outer;
			}
		}		
		progress.end();		
		Log.printlnPwStdOut(pw, "Total carved frags: " + totalCarvedFrag);
		pw.close();
	}



	private void printRemainingFrags(File outputFile) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(outputFile);
		int countFrags = 0;
		System.out.println("Printing remainng frags to: " + outputFile);
		for(Entry<TSNodeLabel, int[]> e : fragsFreq.entrySet()) {
			int f = e.getValue()[0];
			if (f!=-1) {
				pw.println(e.getKey().toStringStandard() + "\t" + f);
				countFrags++;
			}
		}
		System.out.println("Remaing frags: " + countFrags);
		pw.close();
	}

	public static void main(String[] args) throws Exception {
		new MWE_FragSimplPruneCarve(new File(args[0]), new File(args[1]), Double.parseDouble(args[2]));
	}
}
