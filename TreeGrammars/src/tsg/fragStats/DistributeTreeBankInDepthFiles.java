package tsg.fragStats;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.TreeSet;

import tsg.ComparatorTSNodeLabelFreq;
import tsg.TSNodeLabel;
import util.Duet;
import util.file.FileUtil;

public class DistributeTreeBankInDepthFiles {

	
	public static void distribute(File treebankFile) throws Exception {
		File parentDir = treebankFile.getParentFile().getCanonicalFile();
		String newDirPath = parentDir + "/depths/";
		File newDir = new File(newDirPath);
		newDir.mkdir();
		String ouputFilePathPrefix = newDirPath + "fragments_depth";
		distributeUnsorted(treebankFile, ouputFilePathPrefix);
	}
	
	public static void distributeUnsorted(File treebankFile, String ouputFilePathPrefix) throws Exception {
		
		int maxDepth = 100;
		int maxBranching = 100;
		int foundMaxBranching = 0;
		int foundMaxDepth = 0;
		
		int[] depthTotalTypes = new int[maxDepth];
		int[] depthTotalTokens = new int[maxDepth];
		int[][] branchingTotalTokens = new int[maxDepth][maxBranching];
		int[] depthTypesMoreThanOnce = new int[maxDepth];
		
		File[] ouputFileArray = new File[maxDepth];
		PrintWriter[] printWriterArray = new PrintWriter[maxDepth];
		for(int i=0; i<maxDepth; i++) {
			String depthNumber = i<10 ? "0" + i : ""+i;
			File outputFile = new File(ouputFilePathPrefix + "_" + depthNumber);
			ouputFileArray[i] = outputFile;
			printWriterArray[i] = FileUtil.getPrintWriter(outputFile);
		}
				
		Scanner scan = FileUtil.getScanner(treebankFile);
		
		while(scan.hasNextLine()) {
			
			String line = scan.nextLine();
			if (line.equals("")) continue;
			String[] lineSplit = line.split("\t");
			TSNodeLabel tree = new TSNodeLabel(lineSplit[0], false);
			
			int depth = tree.maxDepth();
			if (depth>foundMaxDepth) foundMaxDepth = depth;
			
			int branching = tree.maxBranching();
			if (branching>foundMaxBranching) foundMaxBranching = branching;
			
			printWriterArray[depth].println(line);
			
			int freq = (lineSplit.length==2) ? Integer.parseInt(lineSplit[1]) : 1;			
			depthTotalTokens[depth] += freq;
			depthTotalTypes[depth] ++;
			branchingTotalTokens[depth][branching] += freq;
			if (freq>1) depthTypesMoreThanOnce[depth]++;
		}
		scan.close();
		
		for(int i=1; i<maxDepth; i++) {
			printWriterArray[i].close();
			if (i>foundMaxDepth) {
				ouputFileArray[i].delete();
			}						
		}
		
		File depthReportFile = new File(new File(ouputFilePathPrefix).getParentFile() + "/depthReport.txt");
		File branchingReportFile = new File(new File(ouputFilePathPrefix).getParentFile() + "/branchingReport.txt");
		PrintWriter depthReportPW = FileUtil.getPrintWriter(depthReportFile);
		PrintWriter branchingReportPW = FileUtil.getPrintWriter(branchingReportFile);
		depthReportPW.println("Depth\tTotalTypes\tTotalTokens\tTotalType>1");
		branchingReportPW.println("Depth\tTotalTokens");
		
		for(int i=1; i<maxDepth; i++) {
			depthReportPW.println(i + "\t" + depthTotalTypes[i] + "\t" + depthTotalTokens[i] + "\t" + depthTypesMoreThanOnce[i]);
			branchingReportPW.print(i);
			for(int b=1; b<foundMaxBranching; b++) branchingReportPW.print("\t" + branchingTotalTokens[i][b]);
			branchingReportPW.println();			
		}
		depthReportPW.close();
		branchingReportPW.close();
	}		
	
	

	@SuppressWarnings("unchecked")
	public static void distributeSorted(File treebankFile, 
			String ouputFilePathPrefix) throws Exception {
		int maxDepth = 100;
		int maxBranching = 100;
		int foundMaxBranching = 0;
		TreeSet<Duet<TSNodeLabel, int[]>>[] depthCollectors = new TreeSet[maxDepth];
		int[] depthTotalFreq = new int[maxDepth];
		int[][] branchingTotalFreq = new int[maxDepth][maxBranching];
		int[] depthTypesMoreThanOnce = new int[maxDepth];
		ComparatorTSNodeLabelFreq c = new ComparatorTSNodeLabelFreq();
		for(int i=0; i<maxDepth; i++) {
			depthCollectors[i] = new TreeSet<Duet<TSNodeLabel, int[]>>(c);
		}
		Scanner scan = FileUtil.getScanner(treebankFile);
		while(scan.hasNextLine()) {
			String line = scan.nextLine();
			if (line.equals("")) continue;
			String[] lineSplit = line.split("\t");
			TSNodeLabel tree = new TSNodeLabel(lineSplit[0], false);
			int depth = tree.maxDepth();
			int branching = tree.maxBranching();
			if (branching>foundMaxBranching) foundMaxBranching = branching;
			int freq = (lineSplit.length==2) ? Integer.parseInt(lineSplit[1]) : 1;
			int[] freqArray = new int[]{freq};
			Duet<TSNodeLabel, int[]> d = new Duet<TSNodeLabel, int[]>(tree, freqArray);			
			depthCollectors[depth].add(d);
			depthTotalFreq[depth] += freq;
			branchingTotalFreq[depth][branching] += freq;
			if (freq>1) depthTypesMoreThanOnce[depth]++;
		}
		scan.close();
		File depthReportFile = new File(new File(ouputFilePathPrefix).getParentFile() + "/depthReport.txt");
		File branchingReportFile = new File(new File(ouputFilePathPrefix).getParentFile() + "/branchingReport.txt");
		PrintWriter depthReportPW = FileUtil.getPrintWriter(depthReportFile);
		PrintWriter branchingReportPW = FileUtil.getPrintWriter(branchingReportFile);
		depthReportPW.println("Depth\tTotalTypes\tTotalTokens\tTotalType>1");
		branchingReportPW.println("Depth\tTotalTokens");
		for(int i=0; i<maxDepth; i++) {
			TreeSet<Duet<TSNodeLabel, int[]>> collector = depthCollectors[i];
			if (collector.isEmpty()) continue;
			String depthNumber = i<10 ? "0" + i : ""+i;
			depthReportPW.println(i + "\t" + collector.size() + "\t" + depthTotalFreq[i] + "\t" + depthTypesMoreThanOnce[i]);
			branchingReportPW.print(i);
			for(int b=1; b<foundMaxBranching; b++) branchingReportPW.print("\t" + branchingTotalFreq[i][b]);
			branchingReportPW.println();
			File ouputFile = new File(ouputFilePathPrefix + "_" + depthNumber);
			PrintWriter pw = FileUtil.getPrintWriter(ouputFile);
			for(Duet<TSNodeLabel, int[]> d : collector) {
				pw.println(d.getFirst().toString(false, true) + "\t" + d.getSecond()[0]);
			}
			pw.close();
		}
		depthReportPW.close();
		branchingReportPW.close();
	}
	

	
	public static void main1(String[] args) throws Exception {
		String workingDir = "/home/fsangati/RESULTS/TSG/TSGkernels/subBranch/20/";
		File treebankFile = new File(workingDir + "fragments_subBranch_MUB_20_freq.txt");
		String ouputFilePathPrefix = workingDir + "depths/fragments_MUB_freq_depth";
		distributeUnsorted(treebankFile, ouputFilePathPrefix);
	}
	
	public static void main(String[] args) throws Exception {		
		File treebankFile = new File(args[0]);
		String ouputFilePathPrefix = treebankFile.getParent() + "/depths/";
		new File(ouputFilePathPrefix).mkdir();
		ouputFilePathPrefix += "fragments_depth";
		distributeUnsorted(treebankFile, ouputFilePathPrefix);
	}
	
}
