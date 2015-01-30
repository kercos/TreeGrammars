package tsg;

import java.util.Comparator;

import util.Duet;

@SuppressWarnings("unchecked")
public class ComparatorTSNodeLabelFreq implements Comparator {

	public int compare(Object o1, Object o2) {
		Duet<TSNodeLabel, int[]> duet1 = (Duet<TSNodeLabel, int[]>)o1;
		Duet<TSNodeLabel, int[]> duet2 = (Duet<TSNodeLabel, int[]>)o2;
		Integer freq1 = duet1.getSecond()[0];
		Integer freq2 = duet2.getSecond()[0];
		return freq1<freq2 ? -1 : 1; 
	}

}
