package tsg.evalHeads;

import java.io.File;
import java.io.IOException;
import java.util.*;

import tsg.*;
import tsg.corpora.ConstCorpusTiger;
import util.file.FileUtil;

public class TigerDB2Heads {
	
	public static int[] readIndexes() {
		File indexFile = new File("sentenceIndexes.txt");
		String line = "";
		try {
			Scanner scan = new Scanner(indexFile);			
			while ( scan.hasNextLine()) line += scan.nextLine();														
			scan.close();
		} catch (IOException e) {FileUtil.handleExceptions(e);}
		line = line.trim();
		String[] indexeStrings = line.split(", ");
		int[] result = new int[indexeStrings.length];
		for(int i=0; i<indexeStrings.length; i++) {
			result[i] = Integer.parseInt(indexeStrings[i]);
		}
		return result;
	}
		
	@SuppressWarnings("unchecked")
	public static void assignHeads(File indexBinaryFIle, File tigerTBCorpusFile, File outputFile) {
		
		Vector<BitSet> structure = (Vector<BitSet>)FileUtil.fromBinaryFile(indexBinaryFIle);		
		ConstCorpusTiger tigerTBCorpus = new ConstCorpusTiger(tigerTBCorpusFile, "tigerTBCorpus");
		int[] indexes = readIndexes();		
		tigerTBCorpus = tigerTBCorpus.returnIndexes(indexes);
		tigerTBCorpus.removeDoubleQuoteTiger();
		int index = 0;
		for(TSNode t : tigerTBCorpus.treeBank) {
			BitSet bs = structure.get(index);
			t.assignHeadFromBitSet(bs);			
			index++;
		}		
		tigerTBCorpus.toFile_Complete(outputFile, true);
	}
		
	public static void main(String[] args) {
		//args[0] = sec23 file 
		//args[1] = new file
		if (args.length!=2) {
			System.err.println("The parameters are not correct.");
			System.err.println("Please use: java -jar TigerDB2Heads.jar <tigerTBCorpusFile> <outputFile>");
			return;
		}
		File tigerTBCorpusFile = new File(args[0]);
		if (!tigerTBCorpusFile.isFile()) {
			System.err.println("File " + args[0] + " does not exist.");
			return;
		}
		File outputFile = new File(args[1]);
		File headIndexFile = new File("HeadIndexBinaryFile");
		assignHeads(headIndexFile, tigerTBCorpusFile, outputFile);
	}

}
