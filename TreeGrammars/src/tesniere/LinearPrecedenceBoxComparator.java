package tesniere;

import java.util.Comparator;

public class LinearPrecedenceBoxComparator implements Comparator<Box> {

	public int compare(Box o1, Box o2) {
		Word w1 = o1.leftMostWord(false, false);
		Word w2 = o2.leftMostWord(false, false);
		return w1.compareTo(w2);		
	}
	
	public int leftOverlapsRight(Box b1, Box b2) {
		int w1l = b1.leftMostWord(false, false).position;
		int w1r = b1.rightMostWord(false, false).position;
		int w2l = b2.leftMostWord(false, false).position;
		int w2r = b2.rightMostWord(false, false).position;
		if (w1l<w2l && w1r<w2r) return -1;
		if (w1l>w2l && w1r>w2r) return 1;
		return 0;
	}
	
	public final static String[] leftOverlapsRight = new String[]{"left", "overlap", "right"};
	
	public String leftOverlapsRightString(Box b1, Box b2) {
		int result = leftOverlapsRight(b1, b2) + 1;		
		return leftOverlapsRight[result];		
	}
	
	

}
