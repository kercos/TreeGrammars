package tsg.evalHeads;
import java.util.*;

import tsg.*;
import tsg.corpora.*;
import util.*;

public class EvalDependency {
	

	public static int[] evalHeads(ConstCorpus Eval, ConstCorpus Gold, int limit) {
		ListIterator<TSNode> i = Eval.treeBank.listIterator();
		ListIterator<TSNode> j = Gold.treeBank.listIterator();
		int[] score = new int[2]; //total correct
		while(i.hasNext() && j.hasNext() && i.previousIndex()!=limit) {			
			TSNode EvalTree = i.next();
			TSNode GoldTree = j.next();
			updateScore(EvalTree, GoldTree, score);				
		}
		return score;
	}
	
	public static int[] evalDependencyWorseScores(ConstCorpus Eval, ConstCorpus Gold, int limit) {
		ListIterator<TSNode> i = Eval.treeBank.listIterator();
		ListIterator<TSNode> j = Gold.treeBank.listIterator();
		int[] score = new int[2]; //total correct
		float worse_ratio = 1.f;
		int worse_ratio_index = -1, worse_ratio_tot=0, worse_ratio_rec=0;
		PrintProgressStatic.start("Evaluating heads on sentence: ");
		while(i.hasNext() && j.hasNext() && i.previousIndex()!=limit) {			
			TSNode EvalTree = i.next();
			TSNode GoldTree = j.next();
			int temp_tot = score[0];
			int temp_rec = score[1];
			updateScore(EvalTree, GoldTree, score);
			PrintProgressStatic.next();
			int delta_tot = score[0]-temp_tot;
			int delta_rec = score[1]-temp_rec;
			if (delta_tot>0) {
				float ratio = (float)delta_rec/delta_tot;
				if (ratio<0.7) {
					System.out.println(	"Worse ratio index: " + i.previousIndex() + 
							" (" + delta_rec + "/" + delta_tot + " --> " + ratio + " %)");					
				}
				/*if (ratio<worse_ratio) {
					worse_ratio = ratio;
					worse_ratio_tot = delta_tot;
					worse_ratio_rec = delta_rec;
					worse_ratio_index = i.previousIndex();
				}*/
			}						
		}
		PrintProgressStatic.end();
		System.out.println(	"Worse ratio index: " + worse_ratio_index + 
							" (" + worse_ratio_rec + "/" + worse_ratio_tot + " --> " + worse_ratio + " %)");
		return score;
	}
	
	private static void updateScore(TSNode EvalTree, TSNode GoldTree, int[] score) {
		if (EvalTree.daughters==null) return;
		int indexHeadEval=-1, indexHeadGold=-1;
		int headNumberEval=0, headNumberGold=0;
		//if (EvalTree.prole()!=GoldTree.prole()) {
		//	System.out.println("Problems:" + "\n" + EvalTree + "\n" + GoldTree);
		//}
		for(int i=0; i<EvalTree.daughters.length; i++) {	
			TSNode Evaldaughter = EvalTree.daughters[i];
			TSNode Golddaughter = GoldTree.daughters[i];
			if (Evaldaughter.isHeadMarked()) {
				indexHeadEval=i;
				headNumberEval++;
			}
			if (Golddaughter.isHeadMarked()) {
				indexHeadGold=i;
				headNumberGold++;
			}
			updateScore(Evaldaughter, Golddaughter, score);
		}
		if (headNumberGold==1 && EvalTree.daughters.length>1 && headNumberEval!=0) {
			score[0]++;
			if (indexHeadGold==indexHeadEval) score[1]++;
		}		
	}
	
}
