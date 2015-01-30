package tsg.incremental.old;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;

import tsg.Label;
import tsg.TermLabelShort;
import util.ShortArray;
import util.Utility;

public class MultiFringeShort  {	
		
	static final short emptyTerminal = TermLabelShort.getTermLabelId("", true);
	static final short nullTerminal = 0;
	
	static int maxCombinationsToSimplify = 2;	
	static boolean tryToSimplify = true;
	
	static Set<Short> posSet;
	static HashMap<Short, HashSet<Short>> posSetLexTable;
	//static HashMap<Short, HashSet<Short>> nonPosFirstLexTable;
	
	short rootLabel;
	short firstTerminalLabel;
	
	TerminalsMultiSetTable firstTerminalMultiSet;
	LinkedList<TerminalsMultiSetTable> otherTerminalMultiSet;
	
	public MultiFringeShort(short rootLabel, short firstTerminalLabel) {
		this.rootLabel = rootLabel;
		this.firstTerminalLabel = firstTerminalLabel;
		firstTerminalMultiSet = new TerminalsMultiSetTable();
		otherTerminalMultiSet = new LinkedList<TerminalsMultiSetTable>();

	}
	
	public MultiFringeShort(short rootLabel, short firstTerminalLabel,TerminalsMultiSetTable firstTerminalMultiSet,
			LinkedList<TerminalsMultiSetTable> otherTerminalMultiSet) {
		this.rootLabel = rootLabel;
		this.firstTerminalLabel = firstTerminalLabel;
		this.firstTerminalMultiSet = firstTerminalMultiSet;
		this.otherTerminalMultiSet = otherTerminalMultiSet;
	}
	
	/*
	public int firstTermMultiSetSize() {
		return firstTerminalMultiSet.size();
	}
	*/
	
	public MultiFringeShort(String root, String firstTerm, String[][][][] rest) {
		this(TermLabelShort.acquireTerminalLabelId(root), 
				TermLabelShort.acquireTerminalLabelId(firstTerm));
		for(String[][][] tmst : rest) {
			TerminalsMultiSetTable TMS = new TerminalsMultiSetTable();
			for(String[][] tableEntry : tmst) {
				String key = tableEntry[0][0];	
				short nextInLine = TermLabelShort.acquireTerminalLabelId(key);
				TerminalMatrix tm = new TerminalMatrix();
				for(int i=1; i<tableEntry.length; i++) {
					String[] futures = tableEntry[i];
					LinkedList<Short> row = new LinkedList<Short>();
					for(String f : futures) {
						row.add(TermLabelShort.acquireTerminalLabelId(f));
					}
					tm.rows.add(new ShortArray(row));
				}
				TMS.put(nextInLine, tm);
			}
			otherTerminalMultiSet.add(TMS);
		}
		firstTerminalMultiSet = otherTerminalMultiSet.removeFirst();
	}

	public MultiFringeShort clone() {
		MultiFringeShort result = new MultiFringeShort(rootLabel, firstTerminalLabel);
		result.firstTerminalMultiSet = firstTerminalMultiSet.clone();
		for(TerminalsMultiSetTable otmst : otherTerminalMultiSet) {
			result.otherTerminalMultiSet.add(otmst.clone());
		}
		return result;
	}
	

	public void changeNextInLine(short newSecondTerminal) {
		TerminalMatrix tm = firstTerminalMultiSet.getUniqueEntry().getValue();		
		HashMap<Short, TerminalMatrix> newTable =
			new HashMap<Short, TerminalMatrix>();
		newTable.put(newSecondTerminal,tm);
		firstTerminalMultiSet.table = newTable;
	}
	
	/**
	 * 
	 * @param mf1
	 * @param mf2
	 * @return 0 if incompatible, 1 if mf1 contains mf2, 2 if mf2 contains mf1
	 */
	public static int subFringe(MultiFringeShort mf1, MultiFringeShort mf2) {
		int l1 = mf1.numberOfTables();
		int l2 = mf2.numberOfTables();
		if (l1==l2) 
			return subFringeEqualSize(mf1,mf2);
		if (l1>l2) {
			if (!compatible(mf1,mf2))
				return 0;
			return mf1.contains(mf2) ? 1 : 0;
		}			
		//l2>l1
		if (compatible(mf2,mf1))
			return mf2.contains(mf1) ? 2 : 0;
		return 0;
	}
	
	/**
	 * 
	 * @param mf1
	 * @param mf2
	 * @return 0 if incompatible, 1 if mf1 contains mf2, 2 if mf2 contains mf1
	 */
	public static int subFringeEqualSize(MultiFringeShort mf1, MultiFringeShort mf2) {
		//if ( mf1.numberOfTables()!=mf2.numberOfTables()) 
		//	return 0;
		int index = compatibleEqualSize(mf1, mf2);
		switch(index) {
			case 0:
				return 0;
			case 1:
				return mf1.containsEqualSize(mf2) ? 1 : 0;
			case 2:
				return mf2.containsEqualSize(mf1) ? 2 : 0;
			case 3:
				if (mf1.containsEqualSize(mf2))
					return 1;
				if (mf2.containsEqualSize(mf1))
					return 2;
		}		
		return 0;
	}
	
	/**
	 * 
	 * @param mf1
	 * @param mf2
	 * @return 0 if not compatible, 1 if mf1 keySet contains mf2 keySet, 
	 * 2 if mf2 keySet contains mf1 keySet, 3 if the two keySets are identical.
	 */
	private static int compatibleEqualSize(MultiFringeShort mf1, MultiFringeShort mf2) {
		int index = compatibleEqualSize(mf1.firstTerminalMultiSet,mf2.firstTerminalMultiSet);
		if (index==0)
			return 0;
		boolean maybeEqual = index==3;
		ListIterator<TerminalsMultiSetTable> it1 = 
			mf1.otherTerminalMultiSet.listIterator();
		ListIterator<TerminalsMultiSetTable> it2 = 
			mf2.otherTerminalMultiSet.listIterator();
		while(it1.hasNext()) {
			int newIndex = compatibleEqualSize(it1.next(),it2.next());
			if (newIndex==0)
				return 0;
			//if (newIndex==3) // nothing happens
			if (newIndex<3) {
				if (maybeEqual) {
					index = newIndex;
					maybeEqual = false;
				}
				else if (index!=newIndex)
					return 0;
			}
		}
		return index;
	}
	
	private static boolean compatible(MultiFringeShort mf1, MultiFringeShort mf2) {
		if (!compatible(mf1.firstTerminalMultiSet,mf2.firstTerminalMultiSet))
			return false;
		ListIterator<TerminalsMultiSetTable> it1 = 
			mf1.otherTerminalMultiSet.listIterator();
		ListIterator<TerminalsMultiSetTable> it2 = 
			mf2.otherTerminalMultiSet.listIterator();
		// it1 > it2
		while(it2.hasNext()) {
			if (!compatible(it1.next(),it2.next()))
				return false;
		}
		return it1.next().hasEmptyFringe();
	}

	/**
	 * 
	 * @param mf1
	 * @param mf2
	 * @return 0 if not compatible, 1 if tms1 keySet contains tms2 keySet, 
	 * 2 if tms2 keySet contains tms1 keySet, 3 if the two keySets are identical.
	 */
	private static int compatibleEqualSize(
			TerminalsMultiSetTable tms1,
			TerminalsMultiSetTable tms2) {		
		Set<Short> ks1 = tms1.table.keySet();
		Set<Short> ks2 = tms2.table.keySet();
		if (ks1.equals(ks2)) return 3;
		if (ks1.containsAll(ks2)) return 1;
		if (ks2.containsAll(ks1)) return 2;
		return 0;
	}
	
	private static boolean compatible(
			TerminalsMultiSetTable tms1,
			TerminalsMultiSetTable tms2) {		
		return (tms1.table.keySet().containsAll(tms2.table.keySet()));
	}

	public boolean containsEqualSize(MultiFringeShort mf2) {
		if (!this.firstTerminalMultiSet.contains(mf2.firstTerminalMultiSet))
			return false;
		ListIterator<TerminalsMultiSetTable> thisIter = 
				this.otherTerminalMultiSet.listIterator();
		ListIterator<TerminalsMultiSetTable> otherIter = 
				mf2.otherTerminalMultiSet.listIterator();
		while(thisIter.hasNext()) {
			if (!thisIter.next().contains(otherIter.next()))
				return false;
		}
		return true;
	}
	
	public boolean contains(MultiFringeShort mf2) {
		if (!this.firstTerminalMultiSet.contains(mf2.firstTerminalMultiSet))
			return false;
		ListIterator<TerminalsMultiSetTable> thisIter = 
				this.otherTerminalMultiSet.listIterator();
		ListIterator<TerminalsMultiSetTable> otherIter = 
				mf2.otherTerminalMultiSet.listIterator();
		// otherIter > thisIter
		while(otherIter.hasNext()) {
			if (!thisIter.next().contains(otherIter.next()))
				return false;
		}
		return true;
	}

	public boolean hasEmptyFringe() {
		return firstTerminalMultiSet.table.containsKey(emptyTerminal);
	}
	
	public int numberOfTables() {
		return otherTerminalMultiSet.size()+1;
	}
	
	public boolean isSimpleMultiFringe() {
		return otherTerminalMultiSet.isEmpty();
	}
	
	public int averageLength() {
		int avgLength = firstTerminalMultiSet.averageLength();
		if (isSimpleMultiFringe())
			return avgLength;
		for (TerminalsMultiSetTable tmt : otherTerminalMultiSet) {
			avgLength += tmt.averageLength();
		}
		return avgLength;
	}
	
	public int totalFringes() {		
		int[] sizes = new int[numberOfTables()];
		sizes[0] = firstTerminalMultiSet.totalFringes();
		int i=0;
		for(TerminalsMultiSetTable tmt : otherTerminalMultiSet) {
			sizes[++i] = tmt.totalFringes();
		}
		return Utility.product(sizes);
	}
	
	public String toString(int indent) {
		StringBuilder sb = new StringBuilder();
		String indentString = "";
		for(int i=0; i<indent; i++) {
			indentString += "\t";
		}
		sb.append(indentString + "ROOT: " + TermLabelShort.toString(rootLabel) + "\n");
		sb.append(indentString + "FIRST_TERMINAL: " + TermLabelShort.toString(firstTerminalLabel) + "\n");
		//sb.append("HAS_EMPTY_FRINGE: " + hasEmptyFringe + "\n");
		sb.append(indentString + "MATRIX #1:\n" + firstTerminalMultiSet.toString(indentString) + "\n");
		int i=2;
		for(TerminalsMultiSetTable tmst : otherTerminalMultiSet) {			
			sb.append(indentString + "MATRIX #" + i + ":\n" + tmst.toString(indentString) );
			i++;
		}
		
		return sb.toString();
	}
	
	public String toString() {
		return toString(0);
	}
	
	public boolean addFringe(LinkedList<Short> otherTerms) {
		assert this.isSimpleMultiFringe();
		if (otherTerms.isEmpty()) {
			boolean newElement = !hasEmptyFringe();
			if (newElement)
				firstTerminalMultiSet.table.put(emptyTerminal, null);
			return newElement;
		}
		short nextInLine = otherTerms.removeFirst();
		return firstTerminalMultiSet.addTerminalsRow(nextInLine, new ShortArray(otherTerms));
	}

	
	/*
	private boolean isSimplificable() {
		if (this.otherTerminalMultiSet.size()>1) 
			return false;
		
		Entry<Short,TerminalMatrix> firstUniqueEntry = firstTerminalMultiSet.getUniqueEntry();
		if (firstUniqueEntry==null)
			return false;
		Short firstKey = firstUniqueEntry.getKey();
		if (firstKey==emptyTerminal) {
			return true;
		}
		
		Entry<Short,TerminalMatrix> secondUniqueEntry = otherTerminalMultiSet.getFirst().getUniqueEntry();
		if (secondUniqueEntry==null)
			return false;
		Short secondKey = secondUniqueEntry.getKey();
		if (secondKey==emptyTerminal) {
			return true;
		}
										
		TerminalMatrix firstTM = firstUniqueEntry.getValue();
		if (firstTM.size()>1)
			return false;				
		
		TerminalMatrix secondTM = secondUniqueEntry.getValue();
		if (secondTM.size()>1)
			return false;
		
		return true;
	}
	*/
	
	public void forceSimplify(boolean recursive) {
		
		assert (firstTerminalMultiSet.size()==1);
		
		if(this.isSimpleMultiFringe())
			return;
		
		Entry<Short,TerminalMatrix> firstUniqueEntry = firstTerminalMultiSet.getUniqueEntry();
		assert (firstUniqueEntry!=null);
		
		TerminalsMultiSetTable nextTerminalMultiSet = otherTerminalMultiSet.removeFirst();
		
		TerminalMatrix firstTM = firstUniqueEntry.getValue();
		firstTM.combineAll(nextTerminalMultiSet.table.entrySet());
		
		//forceSimplify(recursive);			
		
	}
	
	public boolean tryToSimplifyOld() {
		
		//if (maxCombinationsToSimplify==-1) 
		//	return false;
		
		if (this.otherTerminalMultiSet.size()>1) // no more than two blocks
			return false;
		
		Entry<Short,TerminalMatrix> firstUniqueEntry = firstTerminalMultiSet.getUniqueEntry();
		if (firstUniqueEntry==null)
			return false;
		
		
		Entry<Short,TerminalMatrix> secondUniqueEntry = otherTerminalMultiSet.getFirst().getUniqueEntry();
		if (secondUniqueEntry==null)
			return false;
		
		TerminalMatrix firstTM = firstUniqueEntry.getValue();
		int sizeFirst = firstTM.size();
		
		TerminalMatrix secondTM = secondUniqueEntry.getValue();
		int sizeSecond = secondTM.size();
		
		int comb = sizeFirst*sizeSecond;
		
		if (comb>maxCombinationsToSimplify)
			return false;
		
		firstTerminalMultiSet = firstTerminalMultiSet.clone();
		firstUniqueEntry = firstTerminalMultiSet.getUniqueEntry();
		if (firstUniqueEntry==null) {
			System.err.println("Prob");
		}
		TerminalMatrix uniqueFirstTm = firstUniqueEntry.getValue();
		short uniqueSecondKey = secondUniqueEntry.getKey();
		uniqueFirstTm.combine(uniqueSecondKey,secondTM);
		
		/*
		TerminalMatrix tmCopy = new TerminalMatrix();		
		LinkedList<Short> uniqueTermList = new LinkedList<Short>();
		uniqueTermList.addAll(firstTM.rows.iterator().next()); 
		uniqueTermList.add(secondUniqueEntry.getKey());
		uniqueTermList.addAll(secondTM.rows.iterator().next());
		tmCopy.rows.add(uniqueTermList);
		firstTerminalMultiSet = new TerminalsMultiSetTable();
		firstTerminalMultiSet.table.put(firstKey,tmCopy);
		*/
		
		otherTerminalMultiSet = new LinkedList<TerminalsMultiSetTable>();				
		return true;		
		
	}
	
	public boolean tryToSimplify() {
		
		if (otherTerminalMultiSet.isEmpty())
			return true;
		
		if (!tryToSimplify)
			return false;
		
		TerminalsMultiSetTable first = firstTerminalMultiSet;
		TerminalsMultiSetTable second = otherTerminalMultiSet.getFirst();
		
		if (maxCombinationsToSimplify!=-1) {			
			int firstCount = first.totalFringes();
			if (firstCount>maxCombinationsToSimplify)
				return false;
						
			int secondCount = second.totalFringes();
			if (secondCount>maxCombinationsToSimplify)
				return false;
			
			int comb = firstCount*secondCount;
			if (comb>maxCombinationsToSimplify)
				return false;
		}
				
		TerminalsMultiSetTable combTable = new TerminalsMultiSetTable();
		for(Entry<Short,TerminalMatrix> e1 : first.entrySet()) {
			short firstKey = e1.getKey();
			if (firstKey==emptyTerminal) {
				for(Entry<Short,TerminalMatrix> e2 : second.entrySet()) {
					short secondKey = e2.getKey();
					if (secondKey==emptyTerminal)
						combTable.addEmptyTerminal();
					else {
						TerminalMatrix secondValue = e2.getValue();
						TerminalMatrix combTM = combTable.get(secondKey);
						if (combTM==null) {
							combTM = secondValue.clone();
							combTable.put(secondKey,combTM);
						}
						else {
							combTM.addAll(secondValue);
						}						
					}
				}
			}
			else {	
				TerminalMatrix firstValue = e1.getValue();
				for(Entry<Short,TerminalMatrix> e2 : second.entrySet()) {
					short secondKey = e2.getKey();
					if (secondKey==emptyTerminal) {
						TerminalMatrix combTM = combTable.get(firstKey);
						if (combTM==null) {
							combTM = firstValue.clone();
							combTable.put(firstKey,combTM);
						}
						else {
							combTM.addAll(firstValue);
						}
					}
					else {
						TerminalMatrix secondValue = e2.getValue();
						TerminalMatrix combTM = firstValue.clone();						
						combTM.combine(secondKey, secondValue);
						TerminalMatrix presentTM = combTable.get(firstKey);
						if (presentTM==null) {
							combTable.put(firstKey,combTM);
						}
						else {
							presentTM.addAll(combTM);							
						}
					}
				}
			}
		}
		
		firstTerminalMultiSet = combTable;
		otherTerminalMultiSet.removeFirst();
						
		return tryToSimplify();		
		
	}

	
	public boolean checkScan(short nextLex) {
		return firstTerminalMultiSet.table.get(nextLex) != null;
	}

	public MultiFringeShort scan(short nextLex) {
		//assert(checkScan(nextLex));
		MultiFringeShort resultFringe = new MultiFringeShort(this.rootLabel, nextLex);
		resultFringe.firstTerminalMultiSet = firstTerminalMultiSet.subCloneShiftLeft(nextLex);
		for(TerminalsMultiSetTable tmst : otherTerminalMultiSet) {
			resultFringe.otherTerminalMultiSet.add(tmst); //.clone()
		}
		if (resultFringe.firstTerminalMultiSet.hasOnlyEmptyFringe() && !resultFringe.isSimpleMultiFringe())
			resultFringe.firstTerminalMultiSet = resultFringe.otherTerminalMultiSet.removeFirst();
		if (!resultFringe.isSimpleMultiFringe()) // && resultFringe.isSimplificable())
			resultFringe.tryToSimplify();
			//System.out.println("Simplified Scan!");
		return resultFringe;	
	}

	public boolean checkSubDown(MultiFringeShort nextMultiFringe) {
		return firstTerminalMultiSet.table.get(nextMultiFringe.rootLabel) != null;
	}
	
	public MultiFringeShort subDown(MultiFringeShort nextMultiFringe) {
		//assert(checkSubDown(nextMultiFringe));
		MultiFringeShort resultFringe = new MultiFringeShort(this.rootLabel, nextMultiFringe.firstTerminalLabel);
		short subLabel = nextMultiFringe.rootLabel;
		TerminalsMultiSetTable shiftTable = firstTerminalMultiSet.subCloneShiftLeft(subLabel);
		if (!nextMultiFringe.firstTerminalMultiSet.hasOnlyEmptyFringe())
			resultFringe.otherTerminalMultiSet.add(nextMultiFringe.firstTerminalMultiSet); //.clone()
		for(TerminalsMultiSetTable tmst : nextMultiFringe.otherTerminalMultiSet)
			resultFringe.otherTerminalMultiSet.add(tmst); //.clone()
		if (!shiftTable.hasOnlyEmptyFringe())
			resultFringe.otherTerminalMultiSet.add(shiftTable);			
		for(TerminalsMultiSetTable tmst : this.otherTerminalMultiSet)
			resultFringe.otherTerminalMultiSet.add(tmst); //.clone()
		if (resultFringe.otherTerminalMultiSet.isEmpty())
			resultFringe.firstTerminalMultiSet.table.put(emptyTerminal, null);
		else
			resultFringe.firstTerminalMultiSet = resultFringe.otherTerminalMultiSet.removeFirst();
			
		if (!resultFringe.isSimpleMultiFringe())// && resultFringe.isSimplificable())
			resultFringe.tryToSimplify();
		
		return resultFringe;		
	}
	

	public boolean checkSubUp(MultiFringeShort nextMultiFringe) {
		short uniqueKey = nextMultiFringe.firstTerminalMultiSet.getUniqueKey();
		return 	uniqueKey!=nullTerminal && uniqueKey!=emptyTerminal && 
			TermLabelShort.isLexical(uniqueKey) && this.hasEmptyFringe() && 
				nextMultiFringe.firstTerminalMultiSet.table.get(this.rootLabel) != null;
	}
	
	public MultiFringeShort subUp(MultiFringeShort nextMultiFringe) {
		//assert(checkSubUp(nextMultiFringe));
		short nextLex = nextMultiFringe.firstTerminalMultiSet.getUniqueKey();
		MultiFringeShort resultFringe = new MultiFringeShort(nextMultiFringe.rootLabel, nextLex);
		resultFringe.firstTerminalMultiSet = nextMultiFringe.firstTerminalMultiSet.subCloneShiftLeft(nextLex);		
		for(TerminalsMultiSetTable tmst : nextMultiFringe.otherTerminalMultiSet) {
			resultFringe.otherTerminalMultiSet.add(tmst); //.clone()
		}		
		if (resultFringe.firstTerminalMultiSet.hasOnlyEmptyFringe() && !resultFringe.isSimpleMultiFringe())
			resultFringe.firstTerminalMultiSet = resultFringe.otherTerminalMultiSet.removeFirst();
		//if (!resultFringe.isSimpleMultiFringe())
		//	if (resultFringe.tryToSimplify())
		//		System.out.println("Simplified subup!");	
		return resultFringe;
	}
	
	protected class TerminalsMultiSetTable {
		
		HashMap<Short, TerminalMatrix> table = 
				new HashMap<Short, TerminalMatrix>();
		

		public boolean isEmpty() {
			return table.isEmpty();
		}
		
		public void merge(TerminalsMultiSetTable otherTMS) {
			for(Entry<Short, TerminalMatrix> e : otherTMS.table.entrySet()) {
				short oKey = e.getKey();
				TerminalMatrix oTm = e.getValue();
				TerminalMatrix tm = this.table.get(oKey);
				if (tm==null)
					this.table.put(oKey, oTm);
				else
					tm.rows.addAll(oTm.rows);
			}
		}
		
		public boolean contains(TerminalsMultiSetTable otherTMS) {
			HashMap<Short, TerminalMatrix> otherTable = otherTMS.table;			
			//if (table.keySet().containsAll(otherTable.keySet())) {
				for(Entry<Short, TerminalMatrix> e : otherTable.entrySet()) {
					short key = e.getKey();
					if (key==emptyTerminal)
						continue;
					TerminalMatrix tm2 = e.getValue();
					TerminalMatrix tm1 = table.get(key);
					if (!tm1.rows.containsAll(tm2.rows))
						return false;
				}
				return true;
			//}
			//return false;
		}

		public int averageLength() {
			int avgLength = 0;
			int rows = 0;
			for(TerminalMatrix tm : table.values()) {
				if (tm==null)
					continue;
				for(ShortArray r : tm.rows) {
					avgLength += r.size();
					rows++;
				} 
			}
			return 1+avgLength/rows;
		}

		public int size() {
			return table.size();
		}
		
		public int totalFringes() {			
			int result = 0;
			for(TerminalMatrix tm : table.values()) {
				if (tm==null || tm.isEmpty())
					result++;
				else
					result += tm.size();
			}
			return result;
		}
		
		public Set<Entry<Short, TerminalMatrix>> entrySet() {
			return table.entrySet();
		}
		
		public boolean containsEmptyTerminal() {
			return table.keySet().contains(emptyTerminal);
		}
		
		public TerminalMatrix get(short key) {
			return table.get(key);
		}
		
		public TerminalsMultiSetTable clone() {
			TerminalsMultiSetTable copy = new TerminalsMultiSetTable();
			for(Entry<Short, TerminalMatrix> e : this.table.entrySet()) {
				short key = e.getKey();
				if (key==emptyTerminal)
					copy.addEmptyTerminal();
				else
					copy.table.put(key, e.getValue().clone());
			}
			return copy;
		}
		
		public void addEmptyTerminal() {
			table.put(emptyTerminal, null);
		}
		
		public void put(short nextInLine, TerminalMatrix tm) {
			table.put(nextInLine, tm);
		}

		
		public Entry<Short,TerminalMatrix> getUniqueEntry() {
			if (table.keySet().size()>1) return null;
			return table.entrySet().iterator().next();
		}
		
		public short getUniqueKey() {
			if (table.keySet().size()>1) return nullTerminal;
			return table.keySet().iterator().next();
		}
		
		public TerminalMatrix getUniqueValue() {
			if (table.keySet().size()>1) return null;
			return table.values().iterator().next();
		}
		
		public boolean hasOnlyEmptyFringe() {
			short uniqueKey = getUniqueKey();
			return uniqueKey!=nullTerminal && uniqueKey==emptyTerminal;
		}
		
		public boolean hasEmptyFringe() {
			return this.table.containsKey(emptyTerminal);
		}
		
		public TerminalsMultiSetTable subCloneShiftLeft(short keyLabel) {
			TerminalsMultiSetTable shiftTable = new TerminalsMultiSetTable(); 
			TerminalMatrix matrix = table.get(keyLabel);
			for(ShortArray r : matrix.rows) {
				ShortArray rowCopy = new ShortArray(r.array);
				if (rowCopy.isEmpty())
					shiftTable.table.put(emptyTerminal, null);				
				else {				
					short nextInLine = rowCopy.removeFirst();
					shiftTable.addTerminalsRow(nextInLine, rowCopy);
				}
			}			
			return shiftTable;
		}
		
		public boolean addTerminalsRow(short firstInLine,
				ShortArray otherTerms) {
			
			TerminalMatrix matrix = table.get(firstInLine);
			if (matrix==null) {
				matrix = new TerminalMatrix();
				table.put(firstInLine, matrix);
			}
			return matrix.rows.add(otherTerms);
			
		}
		
		public String toString(String indentString) {
			StringBuilder sb = new StringBuilder();			
			for(Entry<Short, TerminalMatrix> e : table.entrySet()) {
				short key = e.getKey();
				sb.append(indentString + "\t" + TermLabelShort.toString(key) + "\n");
				if (key!=emptyTerminal)
					sb.append(e.getValue().toString(indentString));
			}
			return sb.toString();
		}
		
		public String toString() {
			return toString("");
		}

		public int[] removeNonCompatibleFringes(
				LinkedList<Short> sentenceTail) {
			
			int totalFringes = 0;
			int removedFringes = 0;
			LinkedList<Short> restOfSentenceTail = new LinkedList<Short>(sentenceTail);			
			short nextWord = sentenceTail.isEmpty() ? nullTerminal : restOfSentenceTail.removeFirst();
			Iterator<Short> keyIter = table.keySet().iterator();
			while(keyIter.hasNext()) {
				short nextInLine = keyIter.next();
				TerminalMatrix tm = table.get(nextInLine);
				int tmSize = tm==null ? 1 : tm.size();
				totalFringes += tmSize;
				if (nextInLine==emptyTerminal) {
					continue;
				}
				boolean nextIsLex = TermLabelShort.isLexical(nextInLine);
				boolean skipAny = false;
				if (nextIsLex) {
					if (nextInLine!=nextWord) {
						keyIter.remove();		
						removedFringes += tmSize;
						continue;
					}
					//nextIsLex = true;
					skipAny = true;
				}
				/*
				else if (posSet.contains(nextInLine)) {
					if (!posSetLexTable.get(nextInLine).contains(nextWord)) {
						keyIter.remove();		
						removedFringes += tmSize;
						continue;
					}
					nextIsLexOrPos = true;
					skipAny = false;
				}
				*/
				//removedFringes += tm.removeNonCampatibleFringes(
				//		nextIsLex ? restOfSentenceTail : sentenceTail, skipAny);
				removedFringes += tm.removeNonCampatibleFringes(restOfSentenceTail, skipAny);
				if (tm.isEmpty())
					keyIter.remove();		
				
			}
			return new int[]{totalFringes, removedFringes};
		}

		public boolean retainLexAnd(Set<Short> allowedNonLex) {
			Iterator<Short> keyIter = this.table.keySet().iterator();
			boolean removedSome = false;
			while(keyIter.hasNext()) {
				short key = keyIter.next();
				if (TermLabelShort.isLexical(key))
					continue;
				if (!allowedNonLex.contains(key)) {
					removedSome = true;
					keyIter.remove();
				}
			}
			return removedSome;
		}

		
	}
	
	protected class TerminalMatrix  {
		
		private HashSet<ShortArray> rows = new HashSet<ShortArray>();

		@Override
		public TerminalMatrix clone() {
			TerminalMatrix copy = new TerminalMatrix();
			for(ShortArray row : rows) {
				copy.rows.add(row.clone());
			}
			return copy;
		}

		public int removeNonCampatibleFringes(
				LinkedList<Short> sentenceTail, 
				boolean skipAny) {
					
			int removedRows = 0;
			Iterator<ShortArray> rowIter = rows.iterator();			
			do {
				ShortArray row = rowIter.next();
				if (!containsInOrder(sentenceTail, row, skipAny)) {
					rowIter.remove();
					removedRows++;
				}
			} while(rowIter.hasNext());
			return removedRows;
		}

		private boolean containsInOrder(LinkedList<Short> sentenceTail,
				ShortArray subSequence, boolean skipAny) {
			int tailLength = sentenceTail.size();
			int tailIndex = 0;
			short[] tailArray = ShortArray.toArray(sentenceTail);
			subSequenceLoop: 
			for(short term : subSequence.array) {
				if (TermLabelShort.isLexical(term)) {
					while(tailIndex!=tailLength) {
						if (tailArray[tailIndex++]==term) {
							skipAny = false;
							continue subSequenceLoop;
						}
						if (!skipAny)
							return false;
					}
					return false;
				}
				if (posSet.contains(term)) {
					if (skipAny) {
						while (tailIndex!=tailLength) {
							short nextWord = tailArray[tailIndex++];
							boolean wordHasPos = posSetLexTable.get(term).contains(nextWord);
							if (!wordHasPos)
								continue;
							continue subSequenceLoop;
						}
						return false;
					}
					if (tailIndex!=tailLength) {
						short nextWord = tailArray[tailIndex++];
						boolean wordHasPos = posSetLexTable.get(term).contains(nextWord);
						if (!wordHasPos)
							return false;							
						continue subSequenceLoop;
					}
					return false;
				}
				// term is not a pos
				if (skipAny) {
					while (tailIndex!=tailLength) {
						//Short nextWord = 
						tailIndex++;
						//HashSet<Short> termSeenFirstLex = nonPosFirstLexTable.get(term);
						//if (termSeenFirstLex==null || !termSeenFirstLex.contains(nextWord))
						//	continue;
						continue subSequenceLoop;
					}
					return false;
				}
				if (tailIndex!=tailLength) {
					//Short nextWord = 
					tailIndex++;;
					//HashSet<Short> termSeenFirstLex = nonPosFirstLexTable.get(term);
					//if (termSeenFirstLex==null || !termSeenFirstLex.contains(nextWord))
					//	return false;
					skipAny = true;
					continue subSequenceLoop;
				}
				return false;
			}
			return true;
		}

		public boolean isEmpty() {			
			return rows.isEmpty();
		}

		public int size() {
			return rows.size();
		}
		
		public boolean addAll(TerminalMatrix otherTM) {
			return this.rows.addAll(otherTM.rows);
		}
		
		public void combine(short nextInLine, TerminalMatrix otherTM) {
			HashSet<ShortArray> newRows = new HashSet<ShortArray>();;
			for(ShortArray r1 : rows) {
				for(ShortArray r2 : otherTM.rows) {
					ShortArray combination = new ShortArray(r1.array);
					combination.add(nextInLine);
					combination.addAll(r2.array);
					newRows.add(combination);					
				}				
			}
			rows = newRows;
		}
		
		public void combineAll(Set<Entry<Short, TerminalMatrix>> table) {
			HashSet<ShortArray> newRows = new HashSet<ShortArray>();
			for(Entry<Short, TerminalMatrix> e : table) {
				short key2 = e.getKey();
				if (key2==emptyTerminal) {
					newRows.addAll(rows);
				}
				else {
					for(ShortArray r1 : rows) {
						for(ShortArray r2 : e.getValue().rows) {
							ShortArray combination = new ShortArray(r1.array);							
							combination.add(key2);
							combination.addAll(r2.array);
							newRows.add(combination);					
						}				
					}
				}				
			}			
			rows = newRows;
		}

		
		public String toString(String indent) {
			StringBuilder sb = new StringBuilder();			
			for(ShortArray r : rows) {
				sb.append(indent + "\t\t" + TermLabelShort.toString(r.array).toString() + "\n");
			}
			return sb.toString();
		}
		
		public String toString() {
			return toString("");
		}
	}


	public static void main(String[] args) {
		String rootA = "VP@";
		String firstTermA = "\"Tuesday\"";
		String[][][][] restA = new String[][][][]{
				{
					{{"VP@"},{}}
				},
				{
					{{"VP@"},{}},
					{{"\"\""}}
				},
				{
					{{"VP@"},{}},
					{{"PP"},{}},
					{{"NP@"},{}},
					{{"\"\""}},
					{{"\"on\""},{"NP"}},
					{{"NP"},{}},
				},
				{
					{{"VP@"},{}},
					{{"S@"},{}},
					{{"\"\""},{}},
					{{"\".\""},{}}
				}
		};
		String rootB = "VP@";
		String firstTermB = "\"Tuesday\"";
		String[][][][] restB = new String[][][][]{
				{
					{{"VP@"},{}}
				},
				{
					{{"VP@"},{}},
					{{"\"\""}}
				},
				{
					{{"VP@"},{}},
					{{"PP"},{}},
					{{"NP@"},{}},
					{{"\"\""}},
					{{"\"on\""},{"NP"}},
					{{"NP"},{}},
				},
				{
					{{"VP@"},{}},
					{{"S@"},{}},
					{{"\"\""},{}},
					{{"\".\""},{}}
				}
		};
		
		MultiFringeShort mfA = new MultiFringeShort(rootA, firstTermA, restA);
		MultiFringeShort mfB = new MultiFringeShort(rootB, firstTermB, restB);
		System.out.println(MultiFringeShort.subFringe(mfA, mfB));
	}

	
	
	
}
