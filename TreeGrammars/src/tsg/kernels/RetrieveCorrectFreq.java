package tsg.kernels;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

import settings.Parameters;
import tsg.TSNodeLabel;
import tsg.TSNodeLabelStructure;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;
import tsg.fragStats.DistributeTreeBankInDepthFiles;
import util.ObjectInteger;
import util.ObjectLong;
import util.PrintProgressStatic;
import util.file.FileUtil;

public abstract class RetrieveCorrectFreq extends Thread {

	ArrayList<TSNodeLabel> treebank;
	File fragmentsFile, outputFile; 
	boolean partialFragments;
	int threads;
	
	public RetrieveCorrectFreq(ArrayList<TSNodeLabel> treebank, File fragmentsFile,
			File outputFile, boolean partialFragments, int threads) {
		this.treebank = treebank;
		this.fragmentsFile = fragmentsFile;
		this.outputFile = outputFile;
		this.partialFragments = partialFragments;
		this.threads = threads;
	}
	
	public void run() {		
		if (outputFile.exists()) retriveCorrectFreqResume();
		else retriveCorrectFreq();
	}
	
	public abstract void retriveCorrectFreqResume();	
	public abstract void retriveCorrectFreq();

	
	
}
