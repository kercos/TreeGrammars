package tsg.parsingExp;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import tsg.Label;
import tsg.TSNodeLabel;
import util.Utility;

public class DefaultParseMaker {

	private Hashtable<String,Label> lexPosTable;
	private Label mostFrequentPos;
	private Label mostFrequentStartingNode;
	private ArrayList<TSNodeLabel> treebank;
	
	public DefaultParseMaker(ArrayList<TSNodeLabel> treebank) {
		this.treebank = treebank;
		buildDefaultPosTagger();
	}
	
	public Label getMostFrequentPos() {
		return mostFrequentPos;
	}
	
	public TSNodeLabel defaultParse(String[] originalTestSentenceWords, String topSymbol) {
		TSNodeLabel result = new TSNodeLabel(Label.getLabel(topSymbol), false);
		TSNodeLabel sNode = new TSNodeLabel(mostFrequentStartingNode, false);
		result.daughters = new TSNodeLabel[]{sNode};
		sNode.parent = result;
		sNode.daughters =  new TSNodeLabel[originalTestSentenceWords.length];		
		for(int i=0; i<originalTestSentenceWords.length; i++) {
			String word = originalTestSentenceWords[i];
			Label posLabel = getDefaultPos(word);
			TSNodeLabel posNode = new TSNodeLabel(posLabel, false);
			posNode.parent = sNode;
			sNode.daughters[i] = posNode;
			Label lexLabel = Label.getLabel(word); 
			TSNodeLabel lexNode = new TSNodeLabel(lexLabel, true);
			posNode.daughters = new TSNodeLabel[]{lexNode};
			lexNode.parent = posNode;
		}
		return result;
	}
	
	public Label getDefaultPos(String word) {
		Label result = lexPosTable.get(word);
		if (result==null) {
			return mostFrequentPos;
		}
		return result;
	}
	
	private void buildDefaultPosTagger() {		
		Hashtable<String,Hashtable<String,int[]>> lexPosTableCount = 
			new Hashtable<String,Hashtable<String,int[]>>();
		Hashtable<String, int[]> posCount = new Hashtable<String, int[]>(); 
		Hashtable<String, int[]> startNodeCount = new Hashtable<String, int[]>();
		for(TSNodeLabel t : treebank) {
			String startNode = t.firstDaughter().label();
			Utility.increaseInTableInt(startNodeCount, startNode);
			ArrayList<TSNodeLabel> lex = t.collectLexicalItems();
			for (TSNodeLabel l : lex) {
				String word = l.label();
				String pos = l.parent.label();
				Utility.increaseInTableInt(posCount, pos);
				Hashtable<String,int[]> wordPosTable = lexPosTableCount.get(word);
				if (wordPosTable == null) {
					wordPosTable = new Hashtable<String,int[]>();
					lexPosTableCount.put(word, wordPosTable);
				}
				Utility.increaseInTableInt(wordPosTable, pos);
			}
		}
		mostFrequentStartingNode = Label.getLabel(getMostFrequentKey(startNodeCount)); 
		mostFrequentPos = Label.getLabel(getMostFrequentKey(posCount));
		lexPosTable = new Hashtable<String,Label>();
		for(Entry<String,Hashtable<String,int[]>> e : lexPosTableCount.entrySet()) {
			String word = e.getKey();
			Hashtable<String,int[]> wordPosTable = e.getValue();
			Label mostFreqWordPos = Label.getLabel(getMostFrequentKey(wordPosTable));
			lexPosTable.put(word, mostFreqWordPos);
		}
	}



	private String getMostFrequentKey(Hashtable<String, int[]> table) {
		int maxCount = -Integer.MAX_VALUE;
		String result = null;
		for(Entry<String,int[]> e : table.entrySet()) {
			int count = e.getValue()[0];
			if (count>maxCount) {
				maxCount = count;
				result = e.getKey();
			}			
		}
		return result;
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
