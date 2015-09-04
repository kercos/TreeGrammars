package tdg;

import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;

import depParsing.DepEval;

import tdg.corpora.DepCorpus;


public class EvalD {

	public EvalD(File gold, File test) {
		ArrayList<TDNode> goldCorpus = 
			DepCorpus.readTreebankFromFileMST(gold, 1000, false, false);
		ArrayList<TDNode> testCorpus = 
			DepCorpus.readTreebankFromFileMST(test, 1000, false, false);
		ListIterator<TDNode> goldListIterator = goldCorpus.listIterator();
		ListIterator<TDNode> testListIterator = testCorpus.listIterator();
		int total = 0, correct = 0;
		while(goldListIterator.hasNext()) {
			TDNode goldTree = goldListIterator.next();
			TDNode testTree = testListIterator.next();			
			int[] correctTotal = testTree.UAS_Total(goldTree);
			correct += correctTotal[0];
			total += correctTotal[1];
		}
		float UAS = (float) correct / total;
		System.out.println(total + " " + correct + " " + UAS);
	}
	
	public static void main(String[] args) {
		File gold = new File("/scratch/fsangati/RESULTS/MST/COLLINS99/tr02-21_MST_0.2/MST.22.gold.ulab");
		File test = new File("/scratch/fsangati/RESULTS/MST/COLLINS99/tr02-21_MST_0.2/MST.22.parsed.ulab");
		DepEval.MSTevalUAS(test, gold);
		//depEval.MSTevalUAS(new File(args[0]), new File(args[1]));
	}
	
}
