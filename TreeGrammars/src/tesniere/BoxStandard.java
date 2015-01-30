package tesniere;

import java.util.*;

import settings.Parameters;
import tsg.Label;
import tsg.TSNodeLabel;
import tsg.corpora.Auxify;
import util.Utility;

public class BoxStandard extends Box {
	
	TreeSet<ContentWord> fwList;
	
	public BoxStandard() {
		//this.fwList = new TreeSet<FullWord>(); 
		originalCat = -2;
		derivedCat = -2;
	}
	
	public BoxStandard(ContentWord fw) {
		this.fwList = new TreeSet<ContentWord>(); 
		this.fwList.add(fw);
		originalCat = -2;
		derivedCat = -2;
	}
	
	public BoxStandard(TreeSet<ContentWord> fwList) {
		this.fwList = fwList; 
		originalCat = -2;
		derivedCat = -2;
	}
	
	public BoxStandard(TreeSet<ContentWord> fwList, TreeSet<FunctionalWord> ewList) {
		this.fwList = fwList; 
		this.addEmptyWordPunctuationList(ewList);
		originalCat = -2;
		derivedCat = -2;
	}
	
	public BoxStandard(ContentWord fw, TreeSet<FunctionalWord> ewList) {		
		this.fwList = new TreeSet<ContentWord>();
		this.fwList.add(fw);
		this.addEmptyWordPunctuationList(ewList);
		originalCat = -2;
		derivedCat = -2;
	}	
	
	public void setFullWord(ContentWord fw) {
		this.fwList = new TreeSet<ContentWord>();
		this.fwList.add(fw);
	}
	
	public void addFullWord(ContentWord fw) {	
		if (fwList==null) fwList = new TreeSet<ContentWord>();
		this.fwList.add(fw);
	}
	
	public String getFullWordsAsString() {
		if (this.fwList==null) return null;				
		if (fwList.size() == 1) return fwList.first().getLex();													
		Iterator<ContentWord> iter = fwList.iterator();
		String fullWords = iter.next().getLex();
		while(iter.hasNext()) {
			fullWords += " " + iter.next().getLex();
		}
		return fullWords;
	}
	
	public void addFullWordsAndMakePreviousEmpty(TreeSet<ContentWord> fwList) {
		ContentWord firstFW = this.fwList.first(); 
		if (ewList==null) ewList = new TreeSet<FunctionalWord>();
		this.ewList.add(firstFW.convertToEmptyWord());
		this.fwList.clear();
		this.fwList.addAll(fwList);
	}

	@Override
	public int startPosition(boolean includePunctuation, boolean includeDepOfCurrentBox, boolean includeDepOfRecursiveBox) {
		int min = Integer.MAX_VALUE;
		if (includePunctuation && punctList!=null) min = punctList.first().getPosition();
		if (ewList!=null) min = Math.min(min, ewList.first().getPosition());
		if (fwList!=null) min = Math.min(min, fwList.first().getPosition());
		if (includeDepOfCurrentBox && dependents!=null) {
			int firstDepPos = dependents.first().startPosition(includePunctuation, includeDepOfRecursiveBox, includeDepOfRecursiveBox);
			min = Math.min(min, firstDepPos);
		}
		return min;
	}
	
	@Override
	public int endPosition(boolean includePunctuation, boolean includeDepOfCurrentBox, boolean includeDepOfRecursiveBox) {
		int max = -1;
		if (includePunctuation && punctList!=null) max = punctList.last().getPosition();		
		if (ewList!=null) max = Math.max(max, ewList.last().getPosition());
		if (fwList!=null) max = Math.max(max, fwList.last().getPosition());
		if (includeDepOfCurrentBox && dependents!=null) {
			int lastDepPos = dependents.last().endPosition(includePunctuation, includeDepOfRecursiveBox, includeDepOfRecursiveBox);
			max = Math.max(max, lastDepPos);
		}
		return max;
	}
	
	@Override
	public int countAllNodes() {				
		if (dependents==null) return 1;
		int result = 1;
		for(Box b : dependents) result += b.countAllNodes();
		return result;
	}
	
	@Override
	public int countAllWords() {		
		int result = fwList==null ? 0 : fwList.size();
		if (ewList!=null) result += ewList.size();
		if (punctList!=null) result += punctList.size();
		if (dependents==null) return result;
		for(Box b : dependents) {
			result += b.countAllWords();
		}
		return result;
	}
	
	@Override
	void collectBoxStructure(TreeSet<Box> set) {
		set.add(this);		
		if (dependents==null) return;
		for(Box b : dependents) b.collectBoxStructure(set);
	}
	
	@Override
	public int maxDepth() {
		if (dependents==null) return 0;
		int maxDepth = -1;
		for(Box d : dependents) {
			int md = d.maxDepth();
			if (md > maxDepth) maxDepth = md;
		}
		maxDepth++;
		return maxDepth;
	}	

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();	
		result.append("StdBox:");
		if (fwList!=null) result.append(" Full Words: " + fwList);
		result.append(" Node " + originalTSNodeLabel.toString());
		if (this.ewList!=null) result.append(" Empty Words: " + ewList);
		if (this.punctList!=null) result.append(" Punctuation Words: " + punctList);
		if (this.dependents!=null) result.append(" Dependents: " + dependents);
		result.append(" In/Out Cat: " + originalCat + "/" + derivedCat);
		return result.toString();
	}

	
	@Override
	public String toXmlString(int indentLevel) {
		int indentLevelPlus = indentLevel + 1;
		int indentLevelPlusPlus = indentLevel + 2;
		String indentString = Utility.fillChar(indentLevel, '\t');		
		String indentStringPlus = Utility.fillChar(indentLevelPlus, '\t');
		String indentStringPlusPlus = Utility.fillChar(indentLevelPlusPlus, '\t');
		StringBuilder sb = new StringBuilder();
		sb.append(indentString + "<" + "StandardBox" + " OrigNodeLabel=" + dq + originalTSNodeLabel.toString() + dq + 
				" OrigCat=" + dq + originalCat + dq + " DerivedCat=" + dq + derivedCat + dq + " >\n");
		if (fwList!=null) {
			sb.append(indentStringPlus + "<FullWords>\n");
			for(ContentWord fw : fwList) sb.append(indentStringPlusPlus + fw.toXmlString() + "\n");
			sb.append(indentStringPlus + "</FullWords>\n");
		}
		if (ewList!=null) {
			sb.append(indentStringPlus + "<EmptyWords>\n");
			for(FunctionalWord ew : ewList) sb.append(indentStringPlusPlus + ew.toXmlString() + "\n");
			sb.append(indentStringPlus + "</EmptyWords>\n");
		}
		if (punctList!=null) {
			sb.append(indentStringPlus + "<PunctWords>\n");
			for(FunctionalWord ew : punctList) sb.append(indentStringPlusPlus + ew.toXmlString() + "\n");
			sb.append(indentStringPlus + "</PunctWords>\n");
		}		
		if (dependents!=null) {
			sb.append(indentStringPlus + "<DependentBlocks>\n");
			for(Box b : dependents) sb.append(b.toXmlString(indentLevelPlusPlus));		
			sb.append(indentStringPlus + "</DependentBlocks>\n");
		}		
		sb.append(indentString + "</StandardBox>\n");
		return sb.toString();
	}
	
	public static BoxStandard getStandardBoxFromXmlScan(Scanner scan, String firstLine) {
		String[] firstLineSplit = firstLine.split("\\s");
		BoxStandard result = new BoxStandard();
		result.originalTSNodeLabel = Label.getLabel(getValueOfXmlAttribute(firstLineSplit[1]));
		result.originalCat = Integer.parseInt(getValueOfXmlAttribute(firstLineSplit[2]));
		result.derivedCat = Integer.parseInt(getValueOfXmlAttribute(firstLineSplit[3]));
		String line = scan.nextLine().trim();
		if (line.equals("<FullWords>")) {				
			do {
				line = scan.nextLine().trim();
				if (line.equals("</FullWords>")) break;
				result.addFullWord(ContentWord.getFullWordFromXmlLine(line));
			} while(true);
			line = scan.nextLine().trim();
		}
		if (line.equals("<EmptyWords>")) {				
			do {
				line = scan.nextLine().trim();
				if (line.equals("</EmptyWords>")) break;
				result.addEmptyOrPunctWord(FunctionalWord.getEmptyWordFromXmlLine(line,1));
			} while(true);
			line = scan.nextLine().trim();
		}
		if (line.equals("<PunctWords>")) {				
			do {
				line = scan.nextLine().trim();
				if (line.equals("</PunctWords>")) break;
				result.addPunctuationWord(FunctionalWord.getEmptyWordFromXmlLine(line,3));
			} while(true);
			line = scan.nextLine().trim();
		}
		if (line.equals("<DependentBlocks>")) {				
			do {
				line = scan.nextLine().trim();
				if (line.equals("</DependentBlocks>")) break;					
				result.addDependent(Box.getBoxFromXmlScan(scan, line));
				continue;
			} while(true);
			line = scan.nextLine().trim();
		}
		// line: </StandardBox>
		return result;
	}

	@Override
	public boolean isJunctionBlock() {		
		return false;
	}
	
	@Override
	public void fillWordArrayTable(Word[] wordsArray,
			IdentityHashMap<Word, Box> table) {
		if (fwList!=null) {
			for(ContentWord fw : fwList) {
				wordsArray[fw.getPosition()] = fw;
				table.put(fw, this);
			}			
		}
		if (ewList!=null) {
			for(FunctionalWord w : ewList) {
				wordsArray[w.getPosition()] = w;
				table.put(w, this);
			}
		}
		if (punctList!=null) {
			for(FunctionalWord w : punctList) {
				wordsArray[w.getPosition()] = w;
				table.put(w, this);
			}
		}
		if (dependents!=null) {
			for(Box b : dependents) b.fillWordArrayTable(wordsArray,table);
		}

	}
	
	@Override
	public void fillWordArray(Word[] wordsArray) {
		if (fwList!=null) {
			for(ContentWord fw : fwList) {
				wordsArray[fw.getPosition()] = fw;
			}			
		}
		if (ewList!=null) {
			for(FunctionalWord w : ewList) {
				wordsArray[w.getPosition()] = w;
			}
		}
		if (punctList!=null) {
			for(FunctionalWord w : punctList) {
				wordsArray[w.getPosition()] = w;
			}
		}
		if (dependents!=null) {
			for(Box b : dependents) b.fillWordArray(wordsArray);
		}

	}

	@Override
	public boolean equals(Object obj) {
		if (this==obj) return true;
		if (obj instanceof BoxStandard) {
			BoxStandard otherBox = (BoxStandard)obj;
			if ((this.fwList==null) != (otherBox.fwList==null)) return false;
			boolean bothNull = this.fwList==null && otherBox.fwList==null;
		    return (bothNull || fwList.equals(otherBox.fwList)) && 
		    	ewList.equals(otherBox.ewList) &&
		    	punctList.equals(otherBox.punctList);
		}
		return false;
	}
	
	@Override
	public Word leftMostWord(boolean includeDepStdBox, boolean includeDepJunc) {
		Word result = null;
		if (ewList!=null) result = ewList.first();
		if (fwList!=null) {
			Word fwFirst = fwList.first();
			if (result==null || fwFirst.position<result.position) result = fwFirst;			
		}
		if (includeDepStdBox && this.dependents!=null) {
			Word firstDepWord = this.dependents.first().leftMostWord(includeDepStdBox, includeDepJunc);
			if (result==null || firstDepWord.position<result.position) result = firstDepWord;
		}
		return result;
	}

	@Override
	public Word rightMostWord(boolean includeDepStdBox, boolean includeDepJunc) {
		Word result = null;
		if (ewList!=null) result = ewList.last();
		if (fwList!=null) {
			Word fwLast = fwList.last();
			if (result==null || fwLast.position>result.position) result = fwLast;			
		}
		if (includeDepStdBox && this.dependents!=null) {
			Word lastDepWord = this.dependents.last().rightMostWord(includeDepStdBox, includeDepJunc);
			if (result==null || lastDepWord.position>result.position) result = lastDepWord;
		}
		return result;
	}
	
	@Override
	public void lowestBlocks(ArrayList<Box> list) {
		if (dependents==null) {
			list.add(this);
			return;
		}
		for(Box d : dependents) d.lowestBlocks(list); 
	}
	
	@Override
	public void lowestBlocksWithDependents(ArrayList<Box> list) {
		if (dependents==null) {
			list.add(this);
			return;
		}
		for(Box d : dependents) d.lowestBlocksWithDependents(list);
	}

	@Override
	public int getWidth() {
		return 1;
	}

	@Override
	public int getWidthWithDependents() {
		if (dependents==null) return 1;
		int maxDepWidth = -1;
		for(Box d : dependents) {
			int w = d.getWidthWithDependents(); 
			if (w>maxDepWidth) maxDepWidth = w;
		}		
		return maxDepWidth+1;
	}

	@Override
	public Word getUpperWord() {
		if (fwList!=null) return fwList.first();
		if (ewList!=null) return ewList.first();		
		return punctList.first();
	}

	static String[] subsentenceLabelsSorted = new String[]{"S", "SBAR"};
	public static boolean isSubS(String l) {
		int dashIndex = l.indexOf('-'); 
		if (dashIndex!=-1) l = l.substring(0, dashIndex);
		return Arrays.binarySearch(subsentenceLabelsSorted, l)>=0;
	}	
	
	@Override
	public void uniteVerbs() {		
		if (dependents==null) return;			
		ContentWord lastFullWord = fwList==null ? null : fwList.last();
		String posAuxLabel = lastFullWord==null ? null : Auxify.auxify(lastFullWord.getPos(), lastFullWord.getLex());
		if (posAuxLabel!=null) {
			int lastFullWordPos = lastFullWord.getPosition();
			ArrayList<Box> dependentsList = new ArrayList<Box>(dependents);
			ArrayList<String> unifiedVerbs = new ArrayList<String>();
			//ArrayList<String> newAuxLabels = new ArrayList<String>();
			unifiedVerbs.add(lastFullWord.word);
			//newAuxLabels.add(posAuxLabel);
			ListIterator<Box> li = dependentsList.listIterator();
			while(li.hasNext()) {
				Box b = li.next();
				int boxStartPos = b.startPosition(false, true, true); // might have left dependents (was closely watching)					
				if (boxStartPos > lastFullWordPos+1) break;
				if (boxStartPos < lastFullWordPos) continue; // || boxStartWord.isEmpty()
				if (b.isJunctionBlock()) continue;
				else {
					BoxStandard dep = (BoxStandard) b;
					if (dep.fwList==null) break;						
					ContentWord nextFullWord = dep.fwList.first();
					if (dep.startPosition(true, false, false) != nextFullWord.position) break;
					String connectionNodeLabel = dep.originalTSNodeLabel.toString();				
					if (nextFullWord.posLabel.toString().startsWith("RB")) { // RB RBR RBS
						Parameters.logPrintln("Skipping " + connectionNodeLabel + " to unify verbs: " + dep.originalTSNodeLabel.toString());
						if (boxStartPos==lastFullWordPos+1) lastFullWordPos++;
						continue;
					}
					if (!connectionNodeLabel.equals("VP")) break;					
					if (!nextFullWord.isVerbOrAdjVerb()) {
						Parameters.logPrintln("VP without a verb: " + nextFullWord.word);
						break;
					}
					// ok unite verbs
					lastFullWord.posLabel = Label.getLabel(posAuxLabel);
					this.addFullWordsAndMakePreviousEmpty(dep.fwList);
					li.remove();				
					if (dep.dependents!=null) {
						for(Box d : dep.dependents) {
							li.add(d);
							d.parent = this;
						}
						for(int i=0; i<dep.dependents.size(); i++) li.previous();
					}
					this.addEmptyWordPunctuationList(dep.ewList);
					this.addEmptyWordPunctuationList(dep.punctList);												
					unifiedVerbs.add(nextFullWord.word);
					posAuxLabel = Auxify.auxify(nextFullWord.getPos(), nextFullWord.getLex());
					if (posAuxLabel==null) break;
					//newAuxLabels.add(posAuxLabel);
					lastFullWordPos++;		
				}			
			};
			int size = unifiedVerbs.size(); 
			if (size>1) {
				if (dependentsList.isEmpty()) dependents=null;
				else dependents = new TreeSet<Box>(dependentsList);
				//Iterator<String> labelerIter = newAuxLabels.iterator();
				StringBuilder lineReport = new StringBuilder("Unified verbs:");
				int i=1;
				for(String w : unifiedVerbs) {
					//if (labelerIter.hasNext() && i!=size) w.posLabel = Label.getLabel(labelerIter.next());
					lineReport.append(" " + w);
					i++;
				}
				Parameters.logPrintln(lineReport.toString());
			}
		}						
		if (dependents==null) return;
		for(Box d : dependents) d.uniteVerbs();
	}
	
	@Override
	public Box uniteCompoundCoordinatedVerbs() {
		if (dependents==null) return this;			
		ContentWord lastFullWord = fwList==null ? null : fwList.last();
		String posAuxLabel = lastFullWord==null ? null : Auxify.auxify(lastFullWord.getPos(), lastFullWord.getLex());
		if (posAuxLabel!=null) {
			int lastFullWordPos = lastFullWord.getPosition();
			for(Box b : dependents) {
				int boxStartPos = b.startPosition(false, true, true);
				if (boxStartPos > lastFullWordPos+1) break;
				if (boxStartPos < lastFullWordPos) continue; // || boxStartWord.isEmpty()
				if (!b.isJunctionBlock()) {
					BoxStandard dep = (BoxStandard) b;		
					if (dep.fwList==null) break;					
					ContentWord nextFullWord = dep.fwList.first();
					if (dep.startPosition(true, false, false) != nextFullWord.position) break;
					String connectionNodeLabel = dep.originalTSNodeLabel.toString();				
					if (nextFullWord.posLabel.toString().startsWith("RB")) { // RB RBR RBS
						Parameters.logPrintln("Skipping " + connectionNodeLabel + " to unify verbs: " + dep.originalTSNodeLabel.toString());
						if (boxStartPos==lastFullWordPos+1) lastFullWordPos++;
					}
					continue;
				}
				BoxJunction dep = (BoxJunction) b;	
				//Box firstBlock = dep.conjuncts.first();
				//if (firstBlock.isJunctionBlock()) break;					
				String connectionNodeLabel = dep.originalTSNodeLabel.toString();
				boolean isVP = connectionNodeLabel.equals("VP");
				if (!isVP) break;				
				// ok unite verbs
				ArrayList<String> unifiedVerbsList = new ArrayList<String>();				
				unifiedVerbsList.add(lastFullWord.word);
				lastFullWord.posLabel = Label.getLabel(posAuxLabel);
				for(Box c : dep.conjuncts) {
					//if (c.isJunctionBlock()) return this;
					//BoxStandard cStdBox = (BoxStandard)c;
					//unifiedVerbsList += " " + cStdBox.fwList.first().getLex();
					c.appendFullWordStringList(unifiedVerbsList);
				}					
				if (this.ewList!=null) {
					if (dep.ewList==null) dep.ewList = new TreeSet<FunctionalWord>();
					dep.ewList.addAll(this.ewList);
				}
				if (this.punctList!=null) {
					if (dep.punctList==null) dep.punctList = new TreeSet<FunctionalWord>();
					dep.punctList.addAll(this.punctList);
				}
				for(ContentWord fw : fwList) {
					if (dep.ewList==null) dep.ewList = new TreeSet<FunctionalWord>();
					dep.ewList.add(fw.convertToEmptyWord());
				}
				dep.parent = this.parent;
				if (this.parent!=null) this.parent.replaceBlock(this,dep);
				if (dependents.size()>1) {
					if (dep.dependents==null) dep.dependents = new TreeSet<Box>();
					for(Box bj : dependents) {
						if (bj==dep) continue;						
						dep.dependents.add(bj);
						bj.parent = dep;
					}
				}
				Parameters.logPrintln("Unified compound verbs: " + unifiedVerbsList);
				dep.uniteCompoundCoordinatedVerbs();
				return dep;
			}
		}
		ArrayList<Box> depArray = new ArrayList<Box>(dependents); 
		for(Box b : depArray) b = b.uniteCompoundCoordinatedVerbs();
		return this;
	}
	
	public static String[] RBverbNegationSorted = new String[]{"n't","not"};
	public static boolean isRBverbNegationSorted(String l) {
		return Arrays.binarySearch(RBverbNegationSorted, l)>=0;
	}
	
	@Override
	public void fixRBJJ() {
		if (dependents==null) return;
		ArrayList<Box> dependentsList = new ArrayList<Box>(dependents);
		boolean modified = false;
		for(ListIterator<Box> li = dependentsList.listIterator(); li.hasNext(); ) {
			Box b = li.next();
			if (b.isJunctionBlock()) continue;
			BoxStandard RBbox = (BoxStandard) b;
			if (RBbox.ewList!=null) continue;
			ContentWord wordRB = RBbox.fwList.first();
			int wordRBposition = wordRB.getPosition();
			if (RBbox.dependents==null && wordRB.posLabel.toString().startsWith("RB")) { //RB RBR RBS
				/*if (this.ewList!=null && this.ewList.first().posLabel.toString().equals("IN")
						&& this.ewList.first().getPosition()==wordRBposition+1) { // RB* IN
					li.remove();
					this.addEmptyOrPunctWord(wordRB.convertToEmptyWord());
					modified = true;
					Parameters.logPrintln("Fixed RB* IN: " + wordRB + " " + this.ewList.first());
					break;
				}
				else { // RB_JJ*/
					if (!li.hasNext() || isRBverbNegationSorted(wordRB.word)) break;
					b = li.next();
					if (b.isJunctionBlock()) continue;				
					BoxStandard JJdep = (BoxStandard) b;
					if (JJdep.ewList!=null) continue;
					ContentWord wordJJ = JJdep.fwList.first();
					if (wordJJ.posLabel.toString().equals("JJ") && wordJJ.getPosition()==wordRBposition+1) {
						li.previous();
						li.previous();
						li.remove();
						li.next();					
						JJdep.addDependent(RBbox);
						modified = true;
						Parameters.logPrintln("Fixed RB* JJ: " + wordRB.posParentLabel + " " + wordJJ.posParentLabel);
						break;
					}
				//}
			}
		}	
		if (modified) {
			if (dependentsList.isEmpty()) dependents = null;
			else dependents = new TreeSet<Box>(dependentsList);
		}
		if (dependents==null) return;
		for(Box d : dependents) d.fixRBJJ();
	}	
	
	// 0,1,2,3: verb, adverb, nount, adj
	
	public int getOriginalCat() {
		if (originalCat!=-2) return originalCat;
		if (fwList==null) return originalCat = -1;
		ContentWord firstFullWord = fwList.first(); 		
		if  (firstFullWord.isVerbOrAdjVerb()) return originalCat = 0;
		if (firstFullWord.isAdverb()) return originalCat = 1;
		if (firstFullWord.isNounOrNPnumb()) return originalCat = 2;
		if (firstFullWord.isAdjective()) return originalCat = 3;		
		return originalCat = -1;
	}
	
	@Override
	public boolean replaceBlock(Box box, Box newBox) {
		if (this.dependents==null) return false;
		if (this.dependents.remove(box)) {
			this.dependents.add(newBox);
			return true;
		}
		return false;		
	}

	public boolean matchFullWordsStructure(BoxStandard otherCStd) {
		if (this.fwList==null || otherCStd.fwList==null) return this.fwList==null && otherCStd.fwList==null;
		if (this.fwList.size() != otherCStd.fwList.size()) return false;
		Iterator<ContentWord> thisCStdFWIter = this.fwList.iterator();
		Iterator<ContentWord> otherCStdFWIter = otherCStd.fwList.iterator();
		while(thisCStdFWIter.hasNext()) {
			if (thisCStdFWIter.next().position != otherCStdFWIter.next().position) return false;
		}
		return true;
	}
	
}
