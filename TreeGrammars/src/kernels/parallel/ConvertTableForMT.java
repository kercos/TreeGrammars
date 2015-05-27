package kernels.parallel;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import util.IdentityArrayList;
import util.PrintProgress;
import util.file.FileUtil;

public class ConvertTableForMT {

	private static void converTables(File inputFile) {
		File outputFileRank = FileUtil.changeExtension(inputFile, ".rank.gz");
		File outputFileProb = FileUtil.changeExtension(inputFile, ".prob.gz");
		
		HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> 
			inputTable = ParallelSubstrings.readTableFromFile(inputFile);
		
		removeSingletons(inputTable);
		
		PrintWriter pw_rank = FileUtil.getGzipPrintWriter(outputFileRank);
		PrintWriter pw_prob = FileUtil.getGzipPrintWriter(outputFileProb);
		
		PrintProgress pp = new PrintProgress("Printing Rank and pro files:", 10000, 0);
		
		for(Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> 
			e : inputTable.entrySet()) {
			
			String sourceString = e.getKey().toString(' ');
			HashMap<IdentityArrayList<String>, TreeSet<Integer>> value = e.getValue();
			int tot = ParallelSubstrings.getAllIndexes(value).size();
			TreeMap<Double, IdentityArrayList<String>> targetsProb = new TreeMap<Double, IdentityArrayList<String>>(); 
			for(Entry<IdentityArrayList<String>, TreeSet<Integer>> f : value.entrySet()) {
				IdentityArrayList<String> target = f.getKey();
				double setSize = f.getValue().size();
				double prob = setSize/tot;
				targetsProb.put(prob,target);
			}
			int rank = 1;			
			for(Entry<Double, IdentityArrayList<String>> g : targetsProb.descendingMap().entrySet()) {
				String targetString = g.getValue().toString(' ');
				pw_rank.println(rank + " ||| " + sourceString + " ||| " + targetString);
				//RANK ||| SOURCE MWE ||| TARGET MWE
				
				double prob = g.getKey();
				pw_prob.println(sourceString + " ||| " + targetString + " ||| " + prob);
				//source MWE ||| target MWE ||| probability
				rank++;
				pp.next();
			}
		}
		pp.end();
		
		pw_rank.close();
		pw_prob.close();
	}

	
	private static void removeSingletons(
			HashMap<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> inputTable) {
		
		Iterator<Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>>> iterA = inputTable.entrySet().iterator();
		while(iterA.hasNext()) {
			Entry<IdentityArrayList<String>, HashMap<IdentityArrayList<String>, TreeSet<Integer>>> nextA = iterA.next();
			HashMap<IdentityArrayList<String>, TreeSet<Integer>> subTable = nextA.getValue();
			Iterator<Entry<IdentityArrayList<String>, TreeSet<Integer>>> iterB = subTable.entrySet().iterator();
			while(iterB.hasNext()) {
				if (iterB.next().getValue().isEmpty())
					iterB.remove();
			}
			
			if (subTable.isEmpty())
				iterA.remove();
		}
		
	}


	public static void main(String[] args) {
		File inputTable = new File(args[0]);
		converTables(inputTable);
	}


}
