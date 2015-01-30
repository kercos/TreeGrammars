package viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;

import javax.swing.JCheckBox;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import tsg.TSNodeLabel;

@SuppressWarnings("serial")
public class ConstTreePanel extends TreePanel<TSNodeLabel> {	
	
	int sentenceLength, nodesCount;
	IdentityHashMap<TSNodeLabel, Integer> indexTable;
	TSNodeLabel[] nodesArray, lexicalsArray;
	String[] labelArray;    	
	int[] XLeftArray, YArray, XMiddleArray, wordLengthsArray;
	ArrayList<TSNodeLabel> wrongNodes;
	
	public JCheckBox skewedLinesCheckBox;
	public JCheckBox showHeads;
	
	public static int wrongNodesBorder = 2;
	
	public ConstTreePanel() {
		
	}
	
	public ConstTreePanel(ArrayList<TSNodeLabel> treebank) {
		loadTreebank(treebank);
	}
	
	public void setSentenceNumber(int sn) {
		this.sentenceNumber = sn;
	}
	
	public void loadTreebank(ArrayList<TSNodeLabel> treebank) {
		this.treebank = treebank;
		lastIndex = treebank.size()-1; 
		//sentenceNumber=0;
	}
	
	public void setWrongNodes(ArrayList<TSNodeLabel> wrongNodes) {
		this.wrongNodes = wrongNodes;		
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
            		
        
		for(TSNodeLabel n : nodesArray) {
			boolean isHead = showHeads.isSelected() && n.isHeadMarked();
			int i = indexTable.get(n);
			if (isHead) g2.setColor(Color.RED);
			g2.drawString(labelArray[i], XLeftArray[i], YArray[i]);
			if (isHead) g2.setColor(Color.black);
			TSNodeLabel p = n.parent;  
			if (p==null) continue;
			int pIndex = indexTable.get(p);
			drawLine(XMiddleArray[i],YArray[i]-fontSize - textTopMargin,
					XMiddleArray[pIndex],YArray[pIndex] + textTopMargin, g2);			
		}	
		if (wrongNodes!=null) markWrongNodes(g2);
		
	}
	
	public void renderText(Graphics2D g2, String text) {
		g2.setFont(font); 
		g2.setColor(Color.black);
		g2.drawString(text, 0, 0);
	}
	
	private void markWrongNodes(Graphics2D g2) {
		for(TSNodeLabel n : nodesArray) {
			int i = indexTable.get(n);
			if (wrongNodes.contains(n)) {
				g2.setColor(n.isPreLexical() ? Color.green : Color.red);
				g2.drawRect(XLeftArray[i]-wrongNodesBorder, 
						YArray[i] - fontHight + wrongNodesBorder, 
						wordLengthsArray[i]+2*wrongNodesBorder, fontHight + wrongNodesBorder);
				g2.setColor(Color.black);
			}
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
    	TSNodeLabel t = treebank.get(sentenceNumber);
    	indexTable = new IdentityHashMap<TSNodeLabel, Integer>();
    	nodesCount = t.countAllNodes();
    	sentenceLength = t.countLexicalNodes();
    	lexicalsArray = t.collectLexicalItems().toArray(new TSNodeLabel[sentenceLength]);
    	nodesArray = t.collectAllNodes().toArray(new TSNodeLabel[nodesCount]);        	
    	int maxDepth = t.maxDepth();
    	for(int i=0; i<nodesCount; i++) {
    		TSNodeLabel n = nodesArray[i];
    		indexTable.put(n, i);
    	}
    	
		labelArray = new String[nodesCount];
		XLeftArray = new int[nodesCount];
		XMiddleArray = new int[nodesCount];
		wordLengthsArray = new int[nodesCount];
		Arrays.fill(XLeftArray, -1);
		YArray = new int[nodesCount];	
		int treeWidth=0, treeHeight;
		int previousWordLength=0;
		int previousXLeft = leftMargin;
		for(int j=0; j<sentenceLength; j++) {
			TSNodeLabel n = lexicalsArray[j];
			int i = indexTable.get(n);
			labelArray[i] = n.label();
			int wordLength = metrics.stringWidth(labelArray[i]);
			int wordLengthColumn = getWordLengthColumn(n, wordLength);    			
			wordLengthsArray[i] = wordLengthColumn;    			
			XMiddleArray[i] = wordLengthColumn/2 + previousXLeft + previousWordLength + wordSpace;
			XLeftArray[i] = XMiddleArray[i] - (wordLength/2);
			YArray[i] = topMargin + fontHight + n.height() * levelSize;			
			previousWordLength = wordLengthColumn;
			previousXLeft = XLeftArray[i];			
			if (j==sentenceLength-1) treeWidth = previousXLeft + previousWordLength + wordSpace;
		}    		 
		for(TSNodeLabel n : nodesArray) {
			if (n.isLexical) continue;
			int i = indexTable.get(n);
			updateValues(n,i);    			    			    			    	
		}
		
		
		treeWidth += rightMargin;
		treeHeight = topMargin + fontHight + maxDepth*levelSize + bottomMargin;
		area.width = treeWidth;
        area.height = treeHeight;
    }
    
    private int getWordLengthColumn(TSNodeLabel n, int wordLength) {
    	while (n.parent!=null && n.isUniqueDaughter()) {
    		n = n.parent;
    		int length = metrics.stringWidth(n.label());
    		if (length > wordLength) wordLength = length;
    	}
    	return wordLength;
    }
    
    private void updateValues(TSNodeLabel n, int i) {        	
    	if (XLeftArray[i]!=-1) return;
		labelArray[i] = n.label();    			
		YArray[i] = topMargin + fontHight + n.height() * levelSize;			
    	int wordLength = metrics.stringWidth(labelArray[i]);
    	wordLengthsArray[i] = wordLength;
    	TSNodeLabel[] daughters = n.daughters;
    	TSNodeLabel firstDaughter = n.daughters[0];
    	int iDF = indexTable.get(firstDaughter);
    	if (!firstDaughter.isLexical) updateValues(firstDaughter, iDF);
    	if (daughters.length==1) {        		        		
    		XMiddleArray[i] = XMiddleArray[iDF];  
    		XLeftArray[i] = XMiddleArray[i] - (wordLength/2);
    	}
    	else {        		
    		TSNodeLabel lastDaughter = n.daughters[n.prole()-1];        		
    		int iDL = indexTable.get(lastDaughter);        		
    		if (!lastDaughter.isLexical) updateValues(lastDaughter, iDL);
    		XMiddleArray[i] = XLeftArray[iDF] + (XLeftArray[iDL] + wordLengthsArray[iDL] - XLeftArray[iDF]) / 2;
    		XLeftArray[i] = XMiddleArray[i] - (wordLength/2);
    	}        	
    }

	public TSNodeLabel getSentence(int n) {
		return treebank.get(n);
	}
	
	public void exportAllToPdf(File outputFile, File corpusFile) {
		// step 1: creation of a document-object
		Color bgColor = new Color(0xFF, 0xDE, 0xAD);	
		Document document = new Document();
		Font font = new Font(Font.TIMES_ROMAN, 15, Font.NORMAL);		
		try {
			PdfWriter writer = PdfWriter.getInstance(document,new FileOutputStream(outputFile));
			//document = new Document(new Rectangle((int)area.getWidth(), (int)area.getHeight()));			
			document.open();
			PdfContentByte cb = writer.getDirectContent();						
			Chunk introPhrase = new Chunk("Structures in file " + corpusFile.getName(), font);								
			document.add(introPhrase);
			for(sentenceNumber = 0; sentenceNumber<treebank.size(); sentenceNumber++) {
				loadSentence();
				//Chunk sentenceLabel = new Chunk("Sentence # " + (sentenceNumber+1), smallfont);
				Chunk sentenceLabel = new Chunk("Sentence # " + (sentenceNumber+1), font);
				sentenceLabel.setBackground(bgColor);				
				Rectangle r = new Rectangle((int)area.getWidth(), (int)area.getHeight());								
				document.setPageSize(r);
				document.newPage();
				document.add(sentenceLabel);
				Graphics2D g2 = cb.createGraphics((int)area.getWidth(), (int)area.getHeight());
				render(g2);
				g2.dispose();
			}
			document.close();
		} catch (DocumentException de) {
			System.err.println(de.getMessage());
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}		
	}


}
