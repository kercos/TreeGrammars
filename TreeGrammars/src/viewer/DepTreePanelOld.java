package viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.util.ArrayList;

import tdg.TDNode;

@SuppressWarnings("serial")
public class DepTreePanelOld extends TreePanel {

	ArrayList<TDNode> treebank;
	
	int sentenceLength;
	TDNode[] nodesArray;
	String[] wordsArray, posArray;    	
	int[] XLeftWordArray, XLeftPosArray, YArray, XMiddleArray;    
	
	public DepTreePanelOld(ArrayList<TDNode> treebank) {
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
        		RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
        		RenderingHints.VALUE_RENDER_QUALITY);        
        //g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
        //        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            		
		for(int i=0; i<sentenceLength; i++) {
			g2.drawString(posArray[i], XLeftPosArray[i], YArray[i]);
			g2.drawString(wordsArray[i], XLeftWordArray[i], YArray[i] + fontHight);
			TDNode p = nodesArray[i].parent;  
			if (p==null) continue;
			int pIndex = p.index;
			g2.drawLine(XMiddleArray[i],YArray[i]-fontSize - textTopMargin,
					XMiddleArray[pIndex],YArray[pIndex] + textTopMargin + fontHight);
		}	    
	}
    
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);        
        Graphics2D g2 = (Graphics2D) g;
        render(g2);		                      
    }    	
	    	
    public void loadSentence() {        	
    	TDNode t = treebank.get(sentenceNumber);
        nodesArray = t.getStructureArray();
		sentenceLength = nodesArray.length;
		wordsArray = new String[sentenceLength];
		posArray = new String[sentenceLength];
		XLeftWordArray = new int[sentenceLength];
		XLeftPosArray = new int[sentenceLength];
		XMiddleArray = new int[sentenceLength];
		YArray = new int[sentenceLength];		
		int treeWidth = 0;
		int treeHeight = 0;
		int previousWordLength=0;
		int previousXLeft = leftMargin;
		for(int i=0; i<sentenceLength; i++) {
			wordsArray[i] = nodesArray[i].lex;
			posArray[i] = nodesArray[i].postag;
			int wordLength = metrics.stringWidth(wordsArray[i]);
			int posLength = metrics.stringWidth(posArray[i]);
			int posWordLength = Math.max(wordLength, posLength);
			XMiddleArray[i] = posWordLength/2 + previousXLeft + previousWordLength + wordSpace;
			XLeftWordArray[i] = XMiddleArray[i] - (wordLength/2);
			XLeftPosArray[i] = XMiddleArray[i] - (posLength/2);
			YArray[i] = topMargin + fontHight + nodesArray[i].depth()*levelSize;
			if (YArray[i]>treeHeight) treeHeight = YArray[i];
			if (i==sentenceLength-1) treeWidth = previousXLeft + previousWordLength + wordSpace + posWordLength;
			previousWordLength = posWordLength;
			previousXLeft = XLeftWordArray[i];			
		}
		treeWidth += leftMargin + rightMargin;
		treeHeight += bottomMargin;
		area.width = treeWidth;
        area.height = treeHeight; 
    }


}
