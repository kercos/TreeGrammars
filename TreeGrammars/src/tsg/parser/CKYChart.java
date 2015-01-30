package tsg.parser;
import java.util.*;

import tsg.corpora.*;

/**
 * This class represent a CKY chart table filled by CKYCells.
 * @author fsangati
 *
 */


public class CKYChart {
	static Grammar grammar;
	static CacheManager cache;
	Cell[][] chart;
	ArrayList<Integer> sentenceIndexes;
	int length;
	
	public CKYChart(ArrayList<Integer> sentenceIndexes) {
		this.sentenceIndexes = sentenceIndexes;
		length = sentenceIndexes.size();
		chart = new Cell[this.sentenceIndexes.size()][this.sentenceIndexes.size()];
		buildChart();
	}
	
	private void buildChart() {		
		for(int  w=0; w<this.length; w++) {
			// span = 1;
			chart[w][w] = new Cell(sentenceIndexes.get(w));
			chart[w][w].updateUnaryOld();
		}
		for (int span=2; span<=this.length; span++) {
			for(int start=0; start<=this.length-span; start++) {
				int end = start+span;
				ArrayList<Integer> subString = new ArrayList<Integer>(sentenceIndexes.subList(start, end));
				int cacheInquire = cache.inquire(subString);
				if (cacheInquire<1) { //need to build the cell from scratch
					Cell newCell = new Cell(subString);
					for(int split=start; split<end-1; split++) {
						newCell.updateBinary(chart[start][split], chart[split+1][end-1], split-start); //relative split
					}
					newCell.updateUnaryOld();
					chart[start][end-1] = newCell;
					if (cacheInquire==-1) { //need to store the cell in cache
						cache.writeToCache(newCell);
					}
				}
				else { //the cell is present in cache
					chart[start][end-1] = cache.readFromCache(subString);
				}
			}
		}		
	}
	
	private String getDerivation(int yIndex, int xIndex, int root, int rootDerivationIndex, double[] checkProb) {
		Cell cell = chart[yIndex][xIndex];
		DerivationForest rootForest = cell.DerivationForestSet.get(root);
		int[] derivationIndexes = rootForest.derivations[rootDerivationIndex];
		//split, leftRoot, leftIndex, rightRoot, rightIndex
		//  0       1         2           3          4
		if (derivationIndexes[1]==-1) { //(unary) lexical production
			checkProb[0] += grammar.getLexLogProb(root, cell.yield.get(0));
			return "(" + grammar.catArray[root] + " " + grammar.lexArray[cell.yield.get(0)] + ")";
		}
		
		int split = derivationIndexes[0];
		int leftCellIndexY =  yIndex;
		int leftCellIndexX =  split + yIndex;
		int upCellIndexX = xIndex;
		int upCellIndexY = yIndex + split + 1;
		if (split==-1) { //unary internal production
			checkProb[0] += grammar.getUnaryLogProb(root, derivationIndexes[1]);
			return "(" + grammar.catArray[root] + " " +
			getDerivation(yIndex, xIndex, derivationIndexes[1], derivationIndexes[2], checkProb) + ")";
		}
		//binary (internal) production
		checkProb[0] += grammar.getBinaryLogProb(root, derivationIndexes[1], derivationIndexes[3]);
		return "(" + grammar.catArray[root] + " " +
		getDerivation(leftCellIndexY, leftCellIndexX, derivationIndexes[1], derivationIndexes[2], checkProb) +
		getDerivation(upCellIndexY, upCellIndexX, derivationIndexes[3], derivationIndexes[4], checkProb) + ")";
	}
	
	public String outputBestNDerivation() {
		String result = "";
		Cell cornerCell = chart[0][chart.length-1];
		Integer topIndex = grammar.catIndex.get(ConstCorpus.topTag);		
		DerivationForest topForest = cornerCell.DerivationForestSet.get(topIndex);
		if (topForest==null) return "No parse for: \"" + cornerCell.getYieldInWord() + "\"\n\n";
		//result += "InsideProb: " + Math.exp(topForest.logInsideProb) + "\n";
		for(int i=0; i<topForest.derivations.length; i++) {
			//result += "vitprob=" + Math.exp(topForest.derivationLogProb[i]) + "\n";
			double[] checkProb = new double[]{0.0};
			result += this.getDerivation(0, chart.length-1, topIndex, i, checkProb) + "\n";
			double error = Math.abs(checkProb[0]-topForest.derivationLogProb[i]); 
			if (error>0.0000001) {
				System.out.println("Prob error! " + error);
			}
		}
		//result += "\n";
		return result;
	}
	
	/*public void checkDerivationConsistency() {
		for(int i=0; i<this.chart.length; i++) {
			for(int j=i; j<this.chart.length; j++) {
				chart[i][j].checkDerivationConsistency();
			}
		}
	}*/

}
