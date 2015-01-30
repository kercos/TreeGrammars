package tsg;

import java.util.*;

public class TreeAnalizer {
	
	Hashtable<String, Hashtable> treeStructure;

	public TreeAnalizer() {
		treeStructure = new Hashtable();
	}
	
	public TreeAnalizer(Hashtable template_freq) {
		treeStructure = new Hashtable();
		addTreeInStructure(template_freq.keySet());
	}
	
	public void addTreeInStructure(Set treeSet) {
		for(Iterator i = treeSet.iterator(); i.hasNext(); ) {
			String tree = (String)i.next();
			//if (tree.indexOf('"')!=-1) continue;
			TSNode TN = new TSNode(tree);
			insertTreeInStructure(TN, treeStructure);			
		}
	}
	
	public String[] getNClosestTrees(int N, String tree) {
		TSNode TN = new TSNode(tree);
		HashSet closestStructure = retriveClosestStrucure(TN, treeStructure);
		return getNClosestTrees(N, TN, closestStructure);
	}
	
	public Object[][] getClosestTrees(String tree) {
		TSNode TN = new TSNode(tree);
		HashSet closestStructure = retriveClosestStrucure(TN, treeStructure);
		return getClosestTree(TN,closestStructure);
	}
	
	private void insertTreeInStructure(TSNode TN, Hashtable structure) {		
		TSNode NonEmptyDaughter = null;
		for(int i=0; i<TN.daughters.length; i++) {
			TSNode D = TN.daughters[i];
			if (D.daughters != null) {
				NonEmptyDaughter = D;
				break;
			}
		}
		String label = TN.label;
		Hashtable newStructure = (Hashtable)structure.get(label);
		if (newStructure == null) {
			newStructure = new Hashtable();
			structure.put(label, newStructure);
		}
		if (NonEmptyDaughter==null) {
			while(TN.parent!=null) TN = TN.parent;
			HashSet<TSNode> treeSet = (HashSet<TSNode>)newStructure.get("");
			if (treeSet == null) {
				treeSet = new HashSet<TSNode>();
				newStructure.put("", treeSet);
			}
			treeSet.add(TN);
			return;
		}
		insertTreeInStructure(NonEmptyDaughter, newStructure);
	}
	
	private static HashSet retriveClosestStrucure(TSNode TN, Hashtable structure) {
		TSNode NonEmptyDaughter = null;
		for(int i=0; i<TN.daughters.length; i++) {
			TSNode D = TN.daughters[i];
			if (D.daughters != null) {
				NonEmptyDaughter = D;
				break;
			}
		}
		String label = TN.label;
		Hashtable newStructure = (Hashtable)structure.get(label);
		if (newStructure == null) return null;
		if (NonEmptyDaughter==null) {
			HashSet treeSet = (HashSet)newStructure.get("");
			return treeSet;					
		}
		return retriveClosestStrucure(NonEmptyDaughter, newStructure);
	}
	
	private static Object[][] getClosestTree(TSNode TN, HashSet treeSet) {
		if (treeSet==null) return null;
		while(TN.parent!=null) TN = TN.parent;
		LinkedList<Object[]> TN_LeafHightSet = getLeavesHightSet(TN);
		int totalTNLeaves = TN_LeafHightSet.size();
		HashSet bestPeers = new HashSet();
		int max_equal_leaves = -1;
		int min_peer_size = -1;
		for(Iterator i = treeSet.iterator(); i.hasNext(); ) {		
			TSNode peer = (TSNode)i.next();						
			LinkedList<Object[]> peer_LeafHightSet = getLeavesHightSet(peer);				
			int equal_leaves = match(TN_LeafHightSet, peer_LeafHightSet);
			int peer_size = peer_LeafHightSet.size();
			if (equal_leaves >= max_equal_leaves) {
				if (equal_leaves > max_equal_leaves || peer_size < min_peer_size) {
					bestPeers.clear();
					max_equal_leaves = equal_leaves;
					min_peer_size = peer_size;
					bestPeers.add(peer.toString());
				}
				else if (peer_size == min_peer_size) bestPeers.add(peer.toString());
			}
		}		
		int bestPeersSize = bestPeers.size();
		if (bestPeersSize==0) return null;		
		String[] pearsResults = (String[])bestPeers.toArray(new String[bestPeersSize]);
		Integer[] statistics = new Integer[]{max_equal_leaves, totalTNLeaves, min_peer_size};
		return new Object[][]{pearsResults, statistics};
		
	}
	
	private static String[] getNClosestTrees(int N, TSNode TN, HashSet treeSet) {
		while(TN.parent!=null) TN = TN.parent;
		LinkedList<Object[]> TN_LeafHightSet = getLeavesHightSet(TN);
		//int totalTNLeaves = TN_LeafHightSet.size();
		TSNode bestPeers[] = new TSNode[N];
		int bestScores[] = new int[N];
		Arrays.fill(bestScores, -1);
		for(Iterator i = treeSet.iterator(); i.hasNext(); ) {		
			//refreshAllUnmatched(TN_LeafHightSet);
			TSNode peer = (TSNode)i.next();			
			LinkedList<Object[]> peer_LeafHightSet = getLeavesHightSet(peer);	
			//int totalPeerLeaves = peer_LeafHightSet.size();
			int score = match(TN_LeafHightSet, peer_LeafHightSet);
			//double precision = ((double) equal_leaves)/totalPeerLeaves;
			//double recall = ((double) equal_leaves)/totalTNLeaves;
			//double score = (precision + recall)/2 ;
			if (score < bestScores[N-1]) continue;
			int index = 0;
			while(index<N) {
				int indexScore = bestScores[index];
				if (score > indexScore) {
					if (indexScore!=-1) {
						for(int backIndex = N-2; backIndex>index-1; backIndex--) {
							bestScores[backIndex+1] = bestScores[backIndex];
							bestPeers[backIndex+1] = bestPeers[backIndex];
						}						
					}
					bestScores[index] = score;
					bestPeers[index] = peer; 
					break;
				}
				index++;
			}
		}
		String[] result = new String[N];
		for(int i=0; i<N; i++) {
			if (bestPeers[i] == null) break;
			result[i] = bestPeers[i].toString();	
		} 
		return result;
	}
	
	private static int match(LinkedList<Object[]> node, LinkedList<Object[]> peer) {
		int equal_count = 0;
		for(ListIterator<Object[]> i = node.listIterator(); i.hasNext();) {
			Object[] node_element = i.next();
			for(ListIterator<Object[]> j = peer.listIterator(); j.hasNext();) {
				Object[] peer_element = j.next();
				if (Arrays.equals(node_element, peer_element)) {					
					equal_count++;
					peer_element[2] = new Boolean(true);
					break;
				}
			}
		}
		return equal_count;
	}
	
	private static LinkedList<Object[]> getLeavesHightSet(TSNode TN) {
		List terminals = TN.collectTerminals();
		LinkedList<Object[]> result = new LinkedList<Object[]>();
		for(ListIterator i = terminals.listIterator(); i.hasNext(); ) {
			TSNode L = (TSNode)i.next(); 
			Integer H = new Integer(L.hight());
			Boolean matched = new Boolean(false); 
			result.add(new Object[]{L.label, H, matched});
		}
		return result;
	}
	
	/*private static void refreshAllUnmatched(LinkedList<Object[]> list) {
		for(Iterator<Object[]> i = list.iterator(); i.hasNext(); ) {
			Object[] element = i.next();
			element[2] = new Boolean(false);
		}
	}*/
	
	public static void main(String[] args){
	}
	
}
