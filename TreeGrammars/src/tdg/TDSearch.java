package tdg;

import java.io.File;
import java.util.ArrayList;

import settings.Parameters;
import tdg.corpora.*;

public class TDSearch {

	public static void main(String[] args) {
		Parameters.lengthLimitTraining = 40;
		
		File trainingFile = new File(WsjD.WsjCOLLINS97_ulab + "wsj-02-21.ulab");
		ArrayList<TDNode> wsj_02_21 = 
			DepCorpus.readTreebankFromFileMST(trainingFile, Parameters.lengthLimitTraining, false, true);
		
		String parentLexPos = "would|MD";
		//String daughterLexPos = "Another|DT-T";
		String daughterLexPos = "manager|NN";
		
		for(TDNode t : wsj_02_21) {
			TDNode[] structure = t.getStructureArray();
			for(TDNode n : structure) {
				if (n.lexPosTag(2).equals(daughterLexPos) 
						&& n.parent!=null && n.parent.lexPosTag(2).equals(parentLexPos)) {
					System.out.println(t.toString());
				}
			}
		}
	}
	
}
