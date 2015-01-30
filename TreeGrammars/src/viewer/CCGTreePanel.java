package viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;

import javax.swing.JCheckBox;

import ccg.CCGInternalNode;
import ccg.CCGNode;
import ccg.CCGTerminalNode;

import tdg.corpora.MstSentenceUlabAdvanced;
import tsg.TSNode;

public class CCGTreePanel extends TreePanel<CCGNode> {
	
	private static final long serialVersionUID = 1L;

	int sentenceLength, nodesCount;
	IdentityHashMap<CCGNode, Integer> indexTable;
	CCGNode[] nodesArray, terminalsArray;
	String[] labelArray;    	
	int[] XLeftArray, YArray, XMiddleArray, wordLengthsArray;

	public JCheckBox skewedLinesCheckBox;
	public JCheckBox showHeadsCheckBox;
	
	public CCGTreePanel() {
		
	}
	
	public CCGTreePanel(ArrayList<CCGNode> treebank) {
		loadTreebank(treebank);
	}
	
	public void loadTreebank(ArrayList<CCGNode> treebank) {
		this.treebank = treebank;
		lastIndex = treebank.size()-1; 
	}
	
	public void init() {
		loadFont();
		loadSentence();
        setPreferredSize(area);
        revalidate();
        repaint();
	}
	
	@Override
	public void render(Graphics2D g2) {
		g2.setFont(font); 
        g2.setColor(Color.black);      
        g2.setStroke(new BasicStroke());
        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
        //		RenderingHints.VALUE_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        //  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_RENDERING,
        //		RenderingHints.VALUE_RENDER_QUALITY);        
        //g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
        //        RenderingHints.VALUE_INTERPOLATION_BICUBIC);                                   
            		
        
		for(CCGNode n : nodesArray) {
			boolean isHead = showHeadsCheckBox.isSelected() && n.isHead();
			int i = indexTable.get(n);
			if (isHead) g2.setColor(Color.BLUE);
			g2.drawString(labelArray[i], XLeftArray[i], YArray[i]);
			if (n.isTerminal()) {
				String word = ((CCGTerminalNode)n).word();
				int wordLength = metrics.stringWidth(word);
				int wordXStart = XMiddleArray[i] - wordLength/2;
				g2.drawString(word, wordXStart, YArray[i] + fontHight);
			}			 
			if (isHead) g2.setColor(Color.black);
			CCGNode p = n.parent();  
			if (p==null) continue;
			int pIndex = indexTable.get(p);
			drawLine(XMiddleArray[i],YArray[i]-fontSize - textTopMargin,
					XMiddleArray[pIndex],YArray[pIndex] + textTopMargin, g2);
		}	
		
	}
	
	private void drawLine(int x1, int y1, int x2, int y2, Graphics2D g2) {
		if (!skewedLinesCheckBox.isSelected()) {
			int yM = y1 + (y2-y1)/2;
			g2.drawLine(x1,y1,x1,yM);
			g2.drawLine(x1,yM,x2,yM);
			g2.drawLine(x2,yM,x2,y2);
		}
		else {
			g2.drawLine(x1,y1,x2,y2);
		}
	}
    
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);        
        Graphics2D g2 = (Graphics2D) g;
        render(g2);
    }
	    	
    public void loadSentence() {
    	CCGNode t = treebank.get(sentenceNumber);
    	indexTable = new IdentityHashMap<CCGNode, Integer>();
    	nodesCount = t.countAllNodes();
    	sentenceLength = t.countTerminalNodes();
    	terminalsArray = t.collectTerminalNodes().toArray(new CCGNode[sentenceLength]);
    	nodesArray = t.collectAllNodes().toArray(new CCGNode[nodesCount]);        	
    	int maxDepth = t.maxDepth();
    	for(int i=0; i<nodesCount; i++) {
    		CCGNode n = nodesArray[i];
    		indexTable.put(n, i);
    	}
    	
		labelArray = new String[nodesCount];
		XLeftArray = new int[nodesCount];
		XMiddleArray = new int[nodesCount];
		wordLengthsArray = new int[nodesCount];
		Arrays.fill(XLeftArray, -1);
		YArray = new int[nodesCount];	
		int previousWordLength=0;
		int previousXLeft = leftMargin;
		for(int j=0; j<sentenceLength; j++) {
			CCGNode n = terminalsArray[j];
			int i = indexTable.get(n);
			labelArray[i] = n.category();
			int catLength = metrics.stringWidth(labelArray[i]);
			int wordLengthColumn = getWordLengthColumn(n, catLength);
			if (n.isTerminal()) {
				int wordLength = metrics.stringWidth(((CCGTerminalNode)n).word());
				if (wordLength > wordLengthColumn) wordLengthColumn = wordLength; 
			}			    			
			wordLengthsArray[i] = wordLengthColumn;    			
			XMiddleArray[i] = wordLengthColumn/2 + previousXLeft + previousWordLength + wordSpace;
			XLeftArray[i] = XMiddleArray[i] - (catLength/2);
			YArray[i] = topMargin + fontHight + n.hight() * levelSize;
			previousWordLength = wordLengthColumn;
			previousXLeft = XLeftArray[i];			
		}    		 
		for(CCGNode n : nodesArray) {
			if (n.isTerminal()) continue;
			int i = indexTable.get(n);
			updateValues((CCGInternalNode)n,i);    			    			    			    	
		}
		
		
		area.width = previousXLeft + previousWordLength + wordSpace + rightMargin;
        area.height = topMargin + 2*fontHight + maxDepth*levelSize + bottomMargin;
    }
    
    private int getWordLengthColumn(CCGNode n, int wordLength) {
    	while (n.parent()!=null && n.isUniqueDaughter()) {
    		n = n.parent();
    		int length = metrics.stringWidth(n.category());
    		if (length > wordLength) wordLength = length;
    	}
    	return wordLength;
    }
    
    private void updateValues(CCGInternalNode n, int i) {        	
    	if (XLeftArray[i]!=-1) return;
		labelArray[i] = n.category();    			
		YArray[i] = topMargin + fontHight + n.hight() * levelSize;			
    	int wordLength = metrics.stringWidth(labelArray[i]);
    	wordLengthsArray[i] = wordLength;
    	CCGNode[] daughters = n.daughters();
    	CCGNode firstDaughter = daughters[0];
    	int iDF = indexTable.get(firstDaughter);
    	if (!firstDaughter.isTerminal()) {
    		updateValues((CCGInternalNode)firstDaughter, iDF);
    	}
    	if (daughters.length==1) {        		        		
    		XMiddleArray[i] = XMiddleArray[iDF];  
    		XLeftArray[i] = XMiddleArray[i] - (wordLength/2);
    	}
    	else {        		
    		CCGNode lastDaughter = daughters[n.prole()-1];        		
    		int iDL = indexTable.get(lastDaughter);        		
    		if (!lastDaughter.isTerminal()) {
    			updateValues((CCGInternalNode)lastDaughter, iDL);
    		}
    		XMiddleArray[i] = XLeftArray[iDF] + (XLeftArray[iDL] + wordLengthsArray[iDL] - XLeftArray[iDF]) / 2;
    		XLeftArray[i] = XMiddleArray[i] - (wordLength/2);
    	}        	
    }
}
