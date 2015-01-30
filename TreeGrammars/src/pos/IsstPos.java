package pos;

import java.util.Arrays;

public class IsstPos extends OpenClosePos {
	
	public static final String[] closeClassPosSorted = new String[]{"AP",
		"C","DD","DE","DI","DR","DT","E","EOS","PD","PI","PP","PQ","PR","PT",
		"PU","RD","RI"};
	public static final String[] openClassPosSorted = new String[]{
		"A","B","I","N","NO","S","SA","SP","SW","V","X"};
	public static final String[] allPosSorted = new String[]{"A","AP","B","C","DD","DE",
		"DI","DR","DT","E","EOS","I","N","NO","PD","PI","PP","PQ","PR","PT","PU","RD","RI","S",
		"SA","SP","SW","V","X"};
	public static final String[] allPosShortTags = new String[]{"A","A","B","C","D","D",
		"D","D","D","E","EOS","I","N","N","P","P","P","P","P","P","PU","R","R","S","SA","S","S",
		"V","X"};
	public static final String[] verbPosSorted = new String[]{"V"};
	
	public IsstPos() {
		
	}
	
	public String getSuperShortPos(String pos) {
		int index = Arrays.binarySearch(allPosSorted, pos);
		if (index<0) {
			System.err.println("Pos: " + pos + " doesn't exist!");
			System.exit(0);
		}
		return allPosShortTags[index];
	}
	
	public boolean isOpenClass(String pos) {
		return Arrays.binarySearch(openClassPosSorted, pos)>=0;
	}
	
	public static void main(String[] args) {
	}

}
