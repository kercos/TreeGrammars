package tsg.corpora;

import util.*;
import util.file.FileUtil;

import java.io.*;
import java.util.*;

import settings.Parameters;
import tsg.*;


public class ConstCorpus implements Serializable{
	
	private static final long serialVersionUID = 0L;
	public ArrayList<TSNode> treeBank;	
	public static String unknownTag = "*UNKNOWN*";
	public static String numberTag = "*NUMBER*";
	public static String anonimousLex = "*LEX*";
	public static String topTag = "TOP";
	
	public ConstCorpus() {
		treeBank = new ArrayList<TSNode>();
	}
	
	public ConstCorpus(File inputPath) {
		this(inputPath, FileUtil.defaultEncoding, true);		
	}
	
	public ConstCorpus(File inputPath, String encoding) {
		this(inputPath, encoding, true);		
	}
	
	public ConstCorpus(File inputPath, String encoding, boolean addTop) {
		treeBank = new ArrayList<TSNode>();
		readInputFile(inputPath, addTop, encoding);
	}
	
	public static ConstCorpus fromBinaryFile(File inputFile) {
		ConstCorpus treeBank = new ConstCorpus(); 
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile));
			treeBank = (ConstCorpus) in.readObject();
		} catch (Exception e) {FileUtil.handleExceptions(e);}
		return treeBank;
	}
	
	public void toBinaryFile(File outputFile) {
		try{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFile));
			out.writeObject(this);
		} catch (Exception e) {FileUtil.handleExceptions(e);}		
	}
	
	/**
     * Given a LinkedList of TreeNode as input, the method return a deep clone
     * of the LinkedList, so then corresponding TreeNode in the two list don't
     * refer to the same object. 
     */		
	public ConstCorpus deepClone() {
		ConstCorpus clone = new ConstCorpus();
		for(ListIterator<TSNode> i = treeBank.listIterator(); i.hasNext(); ) {
			TSNode element = (TSNode)i.next();
			clone.treeBank.add(new TSNode(element));
		}
		return clone;
	}
	
	private void readInputFile(File inputFile, boolean addTop, String encoding) {
		if (inputFile.isDirectory()) {
			File[] fileList = inputFile.listFiles();
			Arrays.sort(fileList);
			for(int i=0; i<fileList.length; i++) {
				readInputFile(fileList[i], addTop, encoding);
			}
			return;
		}
		Scanner scan = FileUtil.getScanner(inputFile, encoding);
		int parenthesis = 0;
		String sentence = "";			
		while ( scan.hasNextLine()) {		
			String line = scan.nextLine();		
			if (line.startsWith("*")) continue;
			parenthesis += Utility.countParenthesis(line);
			sentence += line;
			if (parenthesis==0) {
				if (line.length()==0) continue;
				sentence = sentence.trim();			
				sentence = sentence.replaceAll("\n", "");
				sentence = sentence.replaceAll("\\s+", " ");					
				sentence = adjustParenthesisation(sentence);
				sentence = Utility.cleanSlash(sentence);
				addInputTree(sentence, addTop);
				sentence = "";
			}									
		}				
		scan.close();
	}
	
	public static String adjustParenthesisation(String line) {
		if (line.startsWith("((")) {
			int lastPIndex = line.lastIndexOf(')');
			return line.substring(1, lastPIndex);
		}
		if (line.startsWith("( (")) {
			int lastPIndex = line.lastIndexOf(')');
			return line.substring(2, lastPIndex);		
		}
		// french TB
		/*if (line.startsWith("(  (")) {
			int lastPIndex = line.lastIndexOf(')');
			return line.substring(2, lastPIndex);
		}*/
		return line;
	}
	
	public void buildLabelsStatistics(File reportFile, File internalLable, File posTagLable) {
		TreeSet<String> internalLabels = new TreeSet<String>();
		TreeSet<String> posTagLabels = new TreeSet<String>();
		Hashtable<String, Integer> internalLabelTable = new Hashtable<String, Integer>();
		Hashtable<String, Integer> posTagLabelTable = new Hashtable<String, Integer>();		
		for(TSNode inputTree : treeBank) {
			List<TSNode> nodes = inputTree.collectAllNodes();
			for(TSNode n : nodes) {
				if (n.isLexical) continue;
				String label = n.label;
				if (n.isPrelexical()) {
					posTagLabels.add(label);
					Utility.increaseStringInteger(posTagLabelTable, label, 1);
				}
				else {
					internalLabels.add(label);
					Utility.increaseStringInteger(internalLabelTable, label, 1);
				}
			}
		}
		Parameters.internalLabels = internalLabels.toArray(new String[internalLabels.size()]);
		Parameters.posTagLabels = posTagLabels.toArray(new String[posTagLabels.size()]);
		String report 	= "Internal labels (" + internalLabels.size() + "): "
						+ internalLabels.toString()
						+ "\n\n" + "posTags labels (" + posTagLabels.size() + "): " 
						+ posTagLabels.toString();
		Utility.hashtableOrderedToFile(internalLabelTable, internalLable);
		Utility.hashtableOrderedToFile(posTagLabelTable, posTagLable);
		FileUtil.appendReturn(report, reportFile);
	}
	
	private void addInputTree(String line, boolean addTop) {
		if (addTop) line = addTop(line);
		TSNode TreeLine = new TSNode(line);
		treeBank.add(TreeLine);
	}
	
	/**
     * This method formats the bracketting of a (WSJ) parsed sentence adding
     * the initial non-terminal 'TOP' at the root.   
     */	
	public static String addTop(String line) {
		if (line.startsWith("(" + topTag)) return line;
		line = "(" + topTag + " " + line + ")"; //adding TOP
		return line;
	}
	
	public static void addTop(File inputFile, File outputFile) {
		Scanner reader = FileUtil.getScanner(inputFile);
		PrintWriter writer = FileUtil.getPrintWriter(outputFile);		
		while(reader.hasNextLine()) {			
			String line = reader.nextLine();
			if (line.length()==0) continue; 
			writer.println(addTop(line));
		}
		reader.close();
		writer.close();
	}
	
	public void removeTop() {
		for(TSNode t : this.treeBank ) {
			t.removeTop();
		}
	}
	
	public String[] maxOfMinDepth() {
		String[] result = new String[2];
		int maxOfMinDepth = 0;
		for(ListIterator<TSNode> i=treeBank.listIterator(); i.hasNext(); ) {
			TSNode inputTree = (TSNode)i.next();
			int momd = inputTree.maxOfMinDepth();
			if (momd > maxOfMinDepth) {
				maxOfMinDepth = momd;
				result[0] = inputTree.toString();
			}
		}
		result[1] = Integer.toString(maxOfMinDepth);
		return result;
	}
	
	public int maxOfSerialUnaryProduction() {
		int max = -1;
		TSNode maxTree = null;
		for(ListIterator<TSNode> i=treeBank.listIterator(); i.hasNext(); ) {
			TSNode inputTree = (TSNode)i.next();
			int temp = inputTree.maxSerialUnaryProduction()[1]; 
			if (temp>max) {
				max = temp;
				maxTree = inputTree;
			}
		}
		System.out.println(maxTree);
		return max;
	}
	
	public void removeHeadAnnotations() {
		for(TSNode inputTree : treeBank) {
			inputTree.removeHeadAnnotations();
		}
	}
	
	public void fixUnaryHeadConsistency() {
		for(TSNode inputTree : treeBank) {
			inputTree.fixUnaryHeadConsistency();
		}
	}
	
	public void correctHeadAnnotation() {
		int i = 0;
		String errorLines = "";
		for(TSNode inputTree : treeBank) {
			i++;	
			int[] corrections = inputTree.checkHeadConsistency(true);
			if (corrections[0]>0) {
				errorLines += "Corrected head assignemnt at: " + i + 
				"(Total Wrong Heads: " + corrections[0] + 
				"\tof which unary: " + corrections[1] + "\n";				
			}
		}
		FileUtil.appendReturn(errorLines, Parameters.logFile);
	}
	
	public void removeArgumentInHeads() {
		for(TSNode inputTree : treeBank) {
			inputTree.removeArgumentInHeads();
		}
	}
	
	public void checkHeadAnnotationStatistics(File statisticsFile) {
		int totalWrongHeads=0, totalUnaryWrongHeads=0, totalHeadPositions=0, totalWrongTrees=0;
		PrintWriter writer = null;
		if (statisticsFile!= null) writer = FileUtil.getPrintWriter(statisticsFile);
		int index = -1;
		for(TSNode inputTree : treeBank) {
			index++;
			int[] wrongHeads = inputTree.checkHeadConsistency(false);
			totalWrongHeads += wrongHeads[0];
			totalUnaryWrongHeads += wrongHeads[1];
			if (wrongHeads[0]!=wrongHeads[1]) {
				totalWrongTrees++;
				if (statisticsFile!= null)  writer.println("Wrong assignment at index " + index + "\n" +
						"Total Wrong Heads: " + wrongHeads[0] + "\tof which unary: " + wrongHeads[1] + "\n" +
						inputTree.toString(true, false));
			}
			totalHeadPositions += inputTree.countNodes(true,false,false);			
		}
		float missingRate = ((float)totalWrongHeads)/totalHeadPositions;
		String report = "\n\n";
		report += "Head annotation statistics:\n";
		report +=		"Total trees: " + this.size() + "\n"
						+ "Trees with non-superfluous wrong heads: " + totalWrongTrees + "\n"
						+ "Total wrong heads: " + totalWrongHeads + "\n"
						+ "Total unary wrong heads: " + totalUnaryWrongHeads + "\n"
						+ "Total head positions: " + totalHeadPositions + "\n"
						+ "Missing rate: " + missingRate + "\n";
		if (statisticsFile!= null) {
			writer.println(report);
			writer.close();
		}
		else {
			System.out.println(report);
		}		
	}
	
	/**
	 * Substitute the internal nodes of the parsetrees in the training corpus
	 * with the postags of the word according to the current head annotation.
	 */
	public void percolatePosTagsInCorpus() {
		for(TSNode TreeLine : this.treeBank) {
			TreeLine.percolatePosTags();
		}
	}
	
	public void removeTreesLongerThan(int nWords, String[] nonCountCatInLength) {
		for(ListIterator<TSNode> i = treeBank.listIterator(); i.hasNext(); ) {
			TSNode inputTree = i.next();
			int sentenceLength = inputTree.countLexicalNodesExcludingCatLabels(nonCountCatInLength); 
			if (sentenceLength>nWords) i.remove();
		}
	}
	
	public void replaceNumberWithUniqueTag() {
		for(TSNode inputTree : treeBank) {
			inputTree.replaceNumbers(numberTag);
		}
	}
	
	public void removeRedundantRules() {
		for(TSNode inputTree : treeBank) {
			inputTree.removeRedundantRules();
		}
	}
	
	public void checkRedundentRules() {
		int treeIndex = 0;
		for(TSNode inputTree : treeBank) {
			if (inputTree.hasRedundentRules()) {
				System.out.println("RR: " + treeIndex + " : " + inputTree.toString() );
			}
			treeIndex++;
		}
	}
	
	public void makePosTagsLexicon() {
		for(TSNode inputTree : treeBank) {
			inputTree.makePosTagsLexicon();
		}
	}
	
	public void unMakePosTagsLexicon(ConstCorpus originalTrainingCorpus) {
		ListIterator<TSNode> i = treeBank.listIterator();
		ListIterator<TSNode> o = originalTrainingCorpus.treeBank.listIterator();
		while(i.hasNext() && o.hasNext()) {
			i.next().unMakePosTagsLexicon(o.next());
		}
	}
	
	public void copyHeadDependencyFrom(ConstCorpus originalCorpus) {
		int treeIndex = 0;
		for(TSNode inputTree : treeBank) {
			TSNode originalTree = originalCorpus.treeBank.get(treeIndex);
			inputTree.copyHeadAnnotation(originalTree);
			treeIndex ++;
		}
	}
	
	public void removeTraces(String traceTag) {
		for(TSNode inputTree : treeBank) {
			inputTree.pruneSubTrees(traceTag);
		}
	}
	
	public void removeDoubleQuoteTiger() {
		for(TSNode inputTree : treeBank) {
			inputTree.pruneSubTrees("''");
			inputTree.pruneSubTrees("``");
		}
	}
	
	public void removeSemanticTags() {
		for(TSNode inputTree : treeBank) {
			inputTree.removeSemanticTags();
		}
	}
	
	public void removeSemanticTags(String tag) {
		for(TSNode inputTree : treeBank) {
			inputTree.removeSemanticTags(tag);
		}
	}
	
	public void removeNumbersInLables() {
		for(TSNode inputTree : treeBank) {
			inputTree.removeNumberInLabels();
		}
	}
	
	public void replaceLabels(String oldLabel, String newLabel) {
		for(TSNode inputTree : treeBank) {
			inputTree.replaceLabels(oldLabel, newLabel);
		}
	}
	
	public void assignRandomHeads() {
		this.removeHeadAnnotations();
		for(TSNode inputTree : treeBank) {
			inputTree.assignRandomHeads();
		}
	}
	
	public void assignFirstLeftHeads() {
		this.removeHeadAnnotations();
		for(TSNode inputTree : treeBank) {
			inputTree.assignFirstLeftHeads();
		}
	}
	
	public void assignFirstRightHeads() {
		this.removeHeadAnnotations();
		for(TSNode inputTree : treeBank) {
			inputTree.assignFirstRightHeads();
		}
	}
	
	public Hashtable<String, Integer> buildLexFreq() {
		Hashtable<String, Integer> lexFreq = new Hashtable<String, Integer>();
		for(TSNode inputTree : treeBank) {
			List<TSNode> lex = inputTree.collectLexicalItems();
			for(TSNode l : lex) {
				Utility.increaseStringInteger(lexFreq, l.label(), 1);
			}
		}
		return lexFreq;
	}
	
	public void toFile_Flat(File outputFile) {
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		for(TSNode inputTree : treeBank) {
			String line = inputTree.toFlat();
			out.write(line + "\n");
		}
		out.close();	
	}
	
	public void toFile_Complete(File outputFile, boolean heads) {
		toFile_Complete(outputFile, heads, true);
	}
	
	public void toFile_Complete(File outputFile, boolean heads, boolean top) {
	PrintWriter out = FileUtil.getPrintWriter(outputFile);
		for(TSNode inputTree : treeBank) {
			if (!top && inputTree.label.equals("TOP")) {
				inputTree = inputTree.firstDaughter();
				inputTree.headMarked = false;
			}
			String line = inputTree.toString(heads, false);
			out.write(line + "\n");
		}
		out.close();
	}		
	
	public void toFile_ExtractWords(File outputFile) {
		PrintWriter out = FileUtil.getPrintWriter(outputFile);
		for(TSNode inputTree : treeBank) {
			out.write(inputTree.toExtractWord() + "\n");
		}
		out.close();
	}
	
	
	/**
	*  Given an input TreeNode, the method returns a LinkedList of TreeNodes
	*  containing all the TreeNodes in the treebank containing the constructor.
	*/
	public List<Integer> containsConstructor(TSNode constructor) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		int index = -1;
		for(TSNode TN : treeBank) {
			index++;
			if (TN.containsConstructor(constructor)) result.add(index);
		}
		return result;
	}
	
	/**
	*  Given an input TreeNode, the method returns a LinkedList of TreeNodes
	*  containing all the TreeNodes in the treebank containing the spine.
	*/
	public List<TSNode> containsSpine(TSNode treeSpine) {
		ArrayList<TSNode> result = new ArrayList<TSNode>();
		for(TSNode TN : treeBank) {
			if (TN.containsSpine(treeSpine)) result.add(TN);
		}
		return result;
	}
	
	public int size() {		
		return treeBank.size();
	}
	
	/**
	 * Build a new corpus with the last `toReturn` trees in the
	 * current corpus
	 * @param toReturn
	 * @return
	 */
	public ConstCorpus returnLast(int toReturn) {
		ConstCorpus result = new ConstCorpus();
		int endIndex = this.treeBank.size();
		int index = endIndex-toReturn;
		while (index<endIndex) {
			result.treeBank.add(this.treeBank.get(index));
			index++;
		}
		return result;
	}
	
	/**
	 * Build a new corpus with the first `toReturn` trees in the
	 * current corpus
	 * @param toReturn
	 * @return
	 */
	public ConstCorpus returnFirst(int toReturn) {
		ConstCorpus result = new ConstCorpus();	
		for (int index = 0 ; index<toReturn; index++) {
			result.treeBank.add(this.treeBank.get(index));
		}
		return result;
	}
	
	public void keepRandomFraction(float fraction) {
		int toKeep = (int)((double)this.treeBank.size() * fraction);
		Collections.shuffle(this.treeBank);
		ArrayList<TSNode> newTreebank = new ArrayList<TSNode>();
		for (int index = 0 ; index<toKeep; index++) {
			newTreebank.add(this.treeBank.get(index));
		}
		this.treeBank = newTreebank;
	}
	
	public void splitRandom(int sizeFirst, ConstCorpus firstCorpus, ConstCorpus secondCorpus) {
		firstCorpus = new ConstCorpus();
		secondCorpus = new ConstCorpus();
		Collections.shuffle(this.treeBank);
		for (int index = 0 ; index<sizeFirst; index++) {
			firstCorpus.treeBank.add(this.treeBank.get(index));
		}
		for (int index = sizeFirst ; index<this.size(); index++) {
			secondCorpus.treeBank.add(this.treeBank.get(index));
		}		
	}
	
	/**
	 * Increase the corpus by replacing it with `factor` instances of itself.
	 * @param factor
	 */
	public void multiplyCorpus(int factor) {
		ArrayList<TSNode> newTreeBank = new ArrayList<TSNode>();
		for(TSNode tree : this.treeBank) {	
			for(int i=0; i<factor; i++) {
				newTreeBank.add(new TSNode(tree));
			}					
		}
		this.treeBank = newTreeBank;
	}
	
	public void removeIndexes(int[] array) {
		Arrays.sort(array);
		ArrayList<TSNode> newTreeBank = new ArrayList<TSNode>();
		int treeIndex = -1;
		int arrayIndex = 0;
		for(TSNode tree : this.treeBank) {
			treeIndex++;
			if (arrayIndex<array.length && treeIndex==array[arrayIndex]) {
				arrayIndex++;
				continue;
			}
			newTreeBank.add(tree);			
		}
		this.treeBank = newTreeBank;
	}
	
	// 0 based indexes
	public ConstCorpus returnIndexes(int[] indexes) {
		ConstCorpus result = new ConstCorpus();
		for(int i : indexes) {
			result.treeBank.add(this.treeBank.get(i));
		}
		return result;
	}
	
	public void hasNonMinimalHeads(boolean countPunctuation) {
		int index = 0;
		for(TSNode n : this.treeBank) {
			index++;
			n.hasNonMinimalHeads(countPunctuation, index);
		}
	}
	
	public static void main(String args[])  throws Exception{
		File inputFile = new File(Wsj.WsjOriginalCleanedCollins97 + "wsj-00.mrg");
		ConstCorpus c = new ConstCorpus(inputFile, FileUtil.defaultEncoding);
		c.fixUnaryHeadConsistency();
		c.hasNonMinimalHeads(false);		
	}

}
