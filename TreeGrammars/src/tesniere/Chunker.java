package tesniere;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Map.Entry;

import symbols.SymbolString;
import tsg.HeadLabeler;
import tsg.TSNodeLabel;
import tsg.corpora.Wsj;

public class Chunker {
	
	public final static SymbolString O = new SymbolString("O"); // punctuation
	public final static SymbolString C = new SymbolString("C"); //coordination
	public final static SymbolString N = new SymbolString("N"); //new chunk same level
	public final static SymbolString Nm = new SymbolString("-N"); //new chunk lower level
	public final static SymbolString I = new SymbolString("I"); // continue chunk same level
	public final static SymbolString Ip = new SymbolString("+I"); //continue chunk higher level
	

	private static int getMaxChunkRecursionLevel(Box structure) {
		TreeSet<Box> allBlocks = structure.collectBoxStructure();
		int max = 0;
		for(Box b : allBlocks) {
			if (b.isJunctionBox()) continue;
			Box parent = b.governingParent();
			if (parent==null) continue;
			BoxStandard bs = (BoxStandard)b;
			int level = 0;
			while (areRecursive(parent, b)) {
				level++;
				parent = parent.governingParent();
				if (parent==null) break;
			}
			if (level>max) max = level;
		}
		return max;
	}
	
	public static boolean areRecursive(Box b1, Box b2) {
		return b1.startPosition()<b2.startPosition() &&
		b1.endPosition()>b2.endPosition();
	}
	
	public static void checkRecursiveChunks(String[] args) throws Exception {		
		File treebankFile = new File(Wsj.WsjOriginal + "wsj-complete.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_12_04_10"));
		ArrayList<Box> treebank = Conversion.getTesniereTreebank(treebankFile, HL);
		int maxRecursiveLevel = -1;
		int indexMostRecursiveStrucutre = -1;
		Box mostRecursiveStructure = null;
		int index = 0;		
		for(Box b : treebank) {
			index ++;
			int rl = getMaxChunkRecursionLevel(b);
			if (rl>=2) {
				System.out.println(index + " " + rl);
			}
			if (rl>maxRecursiveLevel) {
				mostRecursiveStructure = b;
				maxRecursiveLevel = rl;
				indexMostRecursiveStrucutre = index;
			}
		}
		System.out.println("maxRecursiveLevel " + maxRecursiveLevel);
		System.out.println("indexMostRecursiveStrucutre " + indexMostRecursiveStrucutre);
		System.out.println("mostRecursiveStructure " + mostRecursiveStructure.toXmlString(0));
	}
	
	private static SymbolString[] wordChunkLabeler(Word[] wordsArray, IdentityHashMap<Word, Box> wordBoxMapping) {
		int length = wordsArray.length;
		SymbolString[] result = new SymbolString[length];
		Box previousWordBox = null;
		LinkedList<Box> upperLevels = new LinkedList<Box>();
		for(int i=0; i<length; i++) {
			SymbolString chunkTag = null;
			Word word = wordsArray[i];
			Box wordBox = wordBoxMapping.get(word);
			if (word.isPunctuation()) {
				//chunkTag = "O";
				result[i] = O;
				continue;
			}
			if (wordBox.isJunctionBlock()) {
				//chunkTag = "C";
				result[i] = C;
				continue;
			}
			if (previousWordBox==null) chunkTag = N;
			else if (!upperLevels.isEmpty() && upperLevels.peek()==wordBox) {
				chunkTag = Ip;
				upperLevels.pop();
			}	
			else if (!previousWordBox.isJunctionBlock() && areRecursive(previousWordBox, wordBox)) {
				chunkTag = Nm;
				upperLevels.push(previousWordBox);
			}
			else if (hasPreviousEmptyFullWord(word, wordBox)) chunkTag = I;
			else chunkTag = N;
			previousWordBox = wordBox;
			result[i] = chunkTag;
		}
		return result;
	}
	
	/*public static String[] wordChunkLabelerOld(Word[] wordsArray, IdentityHashMap<Word, Box> wordBoxMapping) {
		int length = wordsArray.length;
		String[] result = new String[length];
		Word previousWord = null;
		Box previousWordBox = null;
		//Box previousWordBoxOuterJunction = null;
		//boolean previousIsPunctOrCoord = false;
		LinkedList<BoxJunction> openJunction = new LinkedList<BoxJunction>();
		LinkedList<Word> openWordJunction = new LinkedList<Word>();
		LinkedList<Box> upperLevels = new LinkedList<Box>();
		for(int i=0; i<length; i++) {
			String chunkTag = null;
			Word word = wordsArray[i];
			Box wordBox = wordBoxMapping.get(word);
			IdentitySet<Box> wordBoxOuterJunctions = new IdentitySet<Box>();
			if (wordBox.isConjunct) {
				Box outerJunction = wordBox;
				while (outerJunction.isConjunct) {					
					outerJunction = outerJunction.parent;
					wordBoxOuterJunctions.add(outerJunction);
				}
			}
			TreeSet<EmptyWord> conjunctions = wordBox.isJunctionBlock() ? 
					((BoxJunction)wordBox).conjunctions : null;
			if (conjunctions!=null && conjunctions.contains(word)) {
					chunkTag = "C";
					//previousIsPunctOrCoord=true;
			}
			else if (word.isPunctuation()) {
				chunkTag = "O";	
				if (wordBox.isJunctionBlock() && previousWord!=null && previousWord==openWordJunction.peek()) {
					openJunction.pop();
					openJunction.push((BoxJunction)wordBox);
					openWordJunction.pop();
					openWordJunction.push(word);
				}
				//previousIsPunctOrCoord=true;
			}
			else {				
				if (previousWord==null) {// || (previousIsPunctOrCoord && previousWordBox.isJunctionBlock())) { 					
					chunkTag = "N";
					if (word.isEmpty() && wordBox.isJunctionBlock()) {
						if (wordBox==openJunction.peek()) {
							openJunction.pop();
							openWordJunction.pop();
						}
						openJunction.push((BoxJunction)wordBox);
						openWordJunction.push(word);
					}
				}
				else if (!upperLevels.isEmpty() && upperLevels.peek()==wordBox) {
					chunkTag = "+I";
					upperLevels.pop();
					if (word.isEmpty() && wordBox.isJunctionBlock()) {
						if (wordBox==openJunction.peek()) {
							openJunction.pop();
							openWordJunction.pop();
						}
						openJunction.push((BoxJunction)wordBox);
						openWordJunction.push(word);
					}
				}
				else if (previousWordBox==wordBox && !previousWord.isPunctuation()) {
					chunkTag = "I";
					if (word.isEmpty() && wordBox.isJunctionBlock()) {
						if (wordBox==openJunction.peek()) {
							openJunction.pop();
							openWordJunction.pop();
						}
						openJunction.push((BoxJunction)wordBox);
						openWordJunction.push(word);
					}
				}
				else if (wordBox.isConjunct && wordBoxOuterJunctions.contains(openJunction.peek())) {
					openJunction.pop();
					Word lastOpenWordJunction = openWordJunction.pop();					
					if (previousWord==lastOpenWordJunction) {
						chunkTag = "I";
						if (word.isEmpty() && wordBox.isJunctionBlock()) {
							openJunction.push((BoxJunction)wordBox);
							openWordJunction.push(word);
						}
					}
					else {
						chunkTag = "+I";
						if (upperLevels.isEmpty()) {
							//System.err.print("Problem!");
						}
						else upperLevels.pop();
						//upperLevels.pop();						
					}											
				}
				else if (areRecursive(previousWordBox, wordBox) &&
							((!previousWordBox.isJunctionBlock() && 
									(upperLevels.isEmpty() || upperLevels.peek()!=previousWordBox)) || 
							previousWord==openWordJunction.peek())) {					
					chunkTag = "-N";
					upperLevels.push(previousWordBox);
					if (word.isEmpty() && wordBox.isJunctionBlock()) {
						if (wordBox==openJunction.peek()) {
							openJunction.pop();
							openWordJunction.pop();
						}
						openJunction.push((BoxJunction)wordBox);
						openWordJunction.push(word);
					}
				}								
				else {
					if (hasPreviousEmptyFullWord(word, wordBox)) chunkTag = "I";
					else chunkTag = "N";
					if (word.isEmpty() && wordBox.isJunctionBlock()) {
						if (wordBox==openJunction.peek()) {
							openJunction.pop();
							openWordJunction.pop();
						}
						openJunction.push((BoxJunction)wordBox);
						openWordJunction.push(word);
					}
				}
				//previousIsPunctOrCoord=false;
				//}
			}				
			result[i] = chunkTag;
			previousWord = word;
			previousWordBox = wordBox;
			//previousWordBoxOuterJunction = wordBoxOuterJunction;
		}
		return result;
	}*/
	
	private static boolean hasPreviousEmptyFullWord(Word word, Box wordBox) {
		if (wordBox.isJunctionBox()) return false;
		BoxStandard boxStd = (BoxStandard)wordBox;
		if (boxStd.ewList!=null && boxStd.ewList.first().position<word.position) return true;
		if (boxStd.fwList!=null && boxStd.fwList.first().position<word.position) return true;
		return false;
	}

	
	
	static void moveEmptyWordsFromJunctions(Box b) {
		if (b.isJunctionBlock()) {
			BoxJunction bJun = (BoxJunction)b;
			if (bJun.conjuncts!=null) {
				for(Box c : bJun.conjuncts) moveEmptyWordsFromJunctions(c);
			}
			if (bJun.ewList!=null) {
				IdentityHashMap<FunctionalWord, BoxStandard> newEmptyWordMapping = 
					new IdentityHashMap<FunctionalWord, BoxStandard>();
				for(FunctionalWord ew : bJun.ewList) {
					int ewPosition = ew.position;
					int minDist = Integer.MAX_VALUE;
					//boolean multMinDist = false;
					BoxStandard minDistBox = null;
					for(Box c : bJun.conjuncts) {
						if (c.isJunctionBlock()) {
							BoxJunction cJun = (BoxJunction)c;
							BoxStandard[] leftRightMostConjunct = new BoxStandard[]{
									cJun.getLeftMostStadardBoxConjunct(),
									cJun.getRightMostStadardBoxConjunct()
							};
							if (leftRightMostConjunct[0]==leftRightMostConjunct[1]) {
								leftRightMostConjunct = new BoxStandard[]{leftRightMostConjunct[0]};
							}
							for(BoxStandard cStd : leftRightMostConjunct) {
								int dist = Math.abs(
										cStd.getLeftMostWordExcludingPunctuation().position - ewPosition);
								/*int distRight = Math.abs(
										cStd.getRightMostWordExcludingPunctuation().position - ewPosition);
								int dist = Math.min(distLeft, distRight);*/
								if (dist<minDist) {
									//multMinDist = false;
									minDist = dist;
									minDistBox = cStd;
								}
								//else if (distLeft==minDist) multMinDist = true;
							}												
						}
						else {
							BoxStandard cStd = (BoxStandard)c;
							int dist = Math.abs(
									cStd.getLeftMostWordExcludingPunctuation().position - ewPosition);
							/*int distRight = Math.abs(
									cStd.getRightMostWordExcludingPunctuation().position - ewPosition);
							int dist = Math.min(distLeft, distRight);*/
							if (dist<minDist) {
								//multMinDist = false;
								minDist = dist;
								minDistBox = cStd;
							}
							//else if (distLeft==minDist) multMinDist = true;
						}						
					}
					/*if (multMinDist) {
						System.err.println("multMinDist");
					}*/
					newEmptyWordMapping.put(ew, minDistBox);					
				}
				bJun.ewList = null;
				for(Entry<FunctionalWord, BoxStandard> e : newEmptyWordMapping.entrySet()) {
					e.getValue().addEmptyOrPunctWord(e.getKey());
				}
			}			
			
		}
		if (b.dependents!=null) {
			for(Box d : b.dependents) {
				moveEmptyWordsFromJunctions(d);
			}
		}
	}
	
	/**
	 * Tagging the words of the sentences with chunk information.
	 * Each word in the sentence is annotated with <w,t,c>
	 * w: word
	 * t: pos-tag
	 * c: chunk-tag 
	 * 	O:	punctuation outside any block
	 *  C:	conjunction word
	 * 	I:	continuation of the chunk from previous word 
	 * 	+I:	continuation of the chunk going one level up from previous word
	 *  N:	new chunk at the same level of the chunk of the previous word
	 *  -N:	new chunk at one level down from previous word
	 * @param b
	 * @return
	 */
	public static SymbolString[] wordChunkLabeler(Box b) {
		Box clone = b.clone();
		moveEmptyWordsFromJunctions(clone);
		int length = clone.countAllWords();
		Word[] wordsArray = new Word[length];
		IdentityHashMap<Word, Box> wordBoxMapping = new IdentityHashMap<Word, Box>();		
		clone.fillWordArrayTable(wordsArray, wordBoxMapping);
		return wordChunkLabeler(wordsArray, wordBoxMapping);
	}	

	/*public static String[] wordChunkLabelerOld(Box b) {
		int length = b.countAllWords();
		Word[] wordsArray = new Word[length];
		IdentityHashMap<Word, Box> wordBoxMapping = new IdentityHashMap<Word, Box>();		
		b.fillWordArrayTable(wordsArray, wordBoxMapping);
		return wordChunkLabelerOld(wordsArray, wordBoxMapping);
	}*/
	
	public static String[] wordChunkLabelerFullOutput(Box b) {
		int length = b.countAllWords();
		Word[] wordsArray = new Word[length];
		IdentityHashMap<Word, Box> wordBoxMapping = new IdentityHashMap<Word, Box>();		
		b.fillWordArrayTable(wordsArray, wordBoxMapping);
		SymbolString[] tags = wordChunkLabeler(wordsArray, wordBoxMapping);
		String[] result = new String[length];
		for(int i=0; i<length; i++) {
			result[i] = writeWordChunkTag(wordsArray[i], tags[i]);
		}
		return result;
	}
	
	public static String writeWordChunkTag(Word word, SymbolString chunkTag) {
		return "<" + word.getLex() + "," + word.getPos() + "," + chunkTag + ">";
	}
	
	public static boolean isConsistentChunking(SymbolString[] chunking) {
		int plusCount = 0;
		int minusCount = 0;
		for(SymbolString l : chunking) {
			if (l==Nm) minusCount++;
			else if (l==Ip) plusCount++;
		}
		return plusCount==minusCount;
	}
	
	public static void makeChunkTagging() throws Exception {
		File treebankFile = new File(Wsj.WsjOriginal + "wsj-02-21.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_12_04_10"));
		ArrayList<Box> treebank = Conversion.getTesniereTreebank(treebankFile, HL);
		int i = 1;
		int errorsCount = 0;
		for(Box b : treebank) {
			//Box b = treebank.get(4);
			SymbolString[] wordsChunking = wordChunkLabeler(b);
			boolean correct = isConsistentChunking(wordsChunking);
			if (!correct) {
				errorsCount++;
				System.out.println(i + "\t" + Arrays.toString(wordsChunking));
			}								
			i++;
			//if (!correct) break;
		}
		System.out.println("Wrong tagging: " + errorsCount);
	}
	
	/*public static void compareChunkTagging() throws Exception {
		File treebankFile = new File(Wsj.WsjOriginal + "wsj-00.mrg");
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_12_04_10"));
		ArrayList<Box> treebank = Conversion.getTesniereTreebank(treebankFile, HL);
		int i = 1;
		for(Box b : treebank) {
			//Box b = treebank.get(4);
			SymbolString[] wordsChunking = wordChunkLabeler(b);
			SymbolString[] wordsChunkingOld = wordChunkLabelerOld(b);
			boolean correct = isConsistentChunking(wordsChunking);
			boolean correctOld = isConsistentChunking(wordsChunkingOld);
			if (!Arrays.equals(wordsChunking, wordsChunkingOld)) {
				System.out.println(i  + "\n\t" + Arrays.toString(wordsChunking) + " " + correct +
						"\n\t" + Arrays.toString(wordsChunkingOld) + " " + correctOld);
			}
					
			i++;
			//if (!correct) break;
		}		
	}*/
	
	public static boolean hasCorrectChunking(Box b) {
		SymbolString[] wordsChunking = wordChunkLabeler(b);
		return isConsistentChunking(wordsChunking);
	}
	
	public static int[] getChunkTaggingScores(Box b1, Box b2) {
		SymbolString[] wordsChunking1 = wordChunkLabeler(b1);
		SymbolString[] wordsChunking2 = wordChunkLabeler(b2);
		return getChunkTaggingScores(wordsChunking1, wordsChunking2);
	}
	
	public static int[] getChunkTaggingScores(SymbolString[] wordsChunking1, SymbolString[] wordsChunking2) {
		int length = wordsChunking1.length; 
		if (length != wordsChunking2.length) {
			System.err.println("Lengths don't match");
			return null;
		}
		int correct = 0;
		for(int i=0; i<length; i++) {
			if (wordsChunking1[i]==wordsChunking2[i]) correct++;			
		}
		return new int[]{correct, length};
	}
	
	private static void chunkSentence(TSNodeLabel structure) throws Exception {
		HeadLabeler HL =  new HeadLabeler(new File("resources/fede.rules_30_06_10"));
		Box b = Conversion.getTesniereStructure(structure, HL);
		String[] wordsChunking = wordChunkLabelerFullOutput(b);
		System.out.println(Arrays.toString(wordsChunking));				
	}

	public static void main(String[] args) throws Exception {
		//checkRecursiveChunks(args);
		//makeChunkTagging();
		//compareChunkTagging();
		//TSNodeLabel structure = new TSNodeLabel("(S (NP-SBJ (NNP Mr.) (NNP Phelan)) (VP (VBZ has) (VP (VBD had) (NP (NP (NN difficulty)) (S-NOM (VP (VBG convincing) (NP (DT the) (NN public)) (SBAR (IN that) (S (NP-SBJ (DT the) (NNP Big) (NNP Board)) (VP (VBZ is) (ADJP-PRD (JJ serious) (PP (IN about) (S-NOM (VP (VBG curbing) (NP (NN volatility))))))))) (, ,) (SBAR-ADV (ADVP (RB especially)) (IN as) (S (NP-SBJ (DT the) (NN exchange)) (ADVP (RB clearly)) (VP (VBZ relishes) (NP (NP (PRP$ its) (NN role)) (PP (IN as) (NP (NP (DT the) (NN home)) (PP (IN for) (NP (NP (QP ($ $) (CD 200) (CD billion))) (PP (IN in) (NP (NP (NN stock-index) (NNS funds)) (, ,) (SBAR (SBAR (WHNP (WDT which)) (S (VP (VBP buy) (NP (NP (JJ huge) (NNS baskets)) (PP (IN of) (NP (NNS stocks)))) (S-PRP (VP (TO to) (VP (VB mimic) (NP (NP (JJ popular) (NN stock-market) (NNS indexes)) (PP (IN like) (NP (DT the) (NP (NNP Standard) (CC &) (NNP Poor) (POS 's)) (CD 500)))))))))) (, ,) (CC and) (SBAR (WHNP (WDT which)) (S (ADVP-TMP (RB sometimes)) (VP (VBP employ) (NP (NN program) (NN trading))))))))))))))))))))) (. .))");
		TSNodeLabel structure = new TSNodeLabel("(S (NP (NNP Mary)) (VP-H (VBZ-H is) (VP (VBG-H singing) (NP (DT an) (JJP (JJ old) (CC and) (JJ beautiful)) (NN-H song)))))");
		chunkSentence(structure);
	}



	
	
}
