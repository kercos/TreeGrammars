package unsupervised;

import java.io.File;
import java.util.ArrayList;

import symbols.SymbolString;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;

public class ChunkDependencies {

	
	public static File corpusFile = new File(Wsj.Wsj10 + "wsj-originalReadableCleaned.mrg");
	public static ArrayList<SymbolString[]> posSequenceSentences;
	public static String[] punctPosTags = 
		new String[]{"''", ",", "-LCB-", "-LRB-", "-RCB-", "-RRB-", ".", ":", "``"};
	
	public static void getPosSequences() throws Exception {
		posSequenceSentences = new ArrayList<SymbolString[]>();
		ArrayList<TSNodeLabel> corpus = TSNodeLabel.getTreebank(corpusFile);
		for(TSNodeLabel t : corpus) {
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			SymbolString[] posSequence = new SymbolString[lex.size()];
			posSequenceSentences.add(posSequence);
			int i=0;
			for(TSNodeLabel  l : lex) {
				posSequence[i] = new SymbolString(l.label());
				i++;
			}			
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		getPosSequences();
	}
	
}
