package viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;

import javax.swing.JCheckBox;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import tdg.corpora.MstSentenceUlab;
import tdg.corpora.MstSentenceUlabAdvanced;

@SuppressWarnings("serial")
public class DepTreePanel extends TreePanel<MstSentenceUlabAdvanced> {

	MstSentenceUlabAdvanced sentence;
	
	int[] XLeftWordArray, XLeftPosArray, YArray, XMiddleWordArray, posWordLengthArray;	    
	Integer[] wrongNodes;

	public JCheckBox skewedLinesCheckBox;
	public static int wrongNodesBorder = 2;
	
	public DepTreePanel() {
		levelSizeFactor = 3;
	}
	
	public DepTreePanel(ArrayList<MstSentenceUlabAdvanced> treebank) {
		loadTreebank(treebank); 
	}
	
	public void loadTreebank(ArrayList<MstSentenceUlabAdvanced> treebank) {
		this.treebank = treebank;
		lastIndex = treebank.size()-1; 
	}
	
	public void setWrongNodes(Integer[] wrongNodes) {
    	this.wrongNodes = wrongNodes;
    }
	
	public MstSentenceUlab getSentence(int n) {
		return treebank.get(n);
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
        g2.setStroke(new BasicStroke());
        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
        //		RenderingHints.VALUE_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        //		RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_RENDERING,
        //		RenderingHints.VALUE_RENDER_QUALITY);        
        //g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
        //        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            		
        drawWordsAndArrows(g2);
        drawWrongNodesBoxes(g2);
		drawCrossingIndexes(g2);
	}
	
	
    
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);        
        Graphics2D g2 = (Graphics2D) g;        
        render(g2);
    }    	
		
	private void drawWordsAndArrows(Graphics2D g2) {
		for(int i=0; i<sentence.length; i++) {			
			if (sentence.loopIndexes.get(i)) g2.setColor(Color.red);
			else g2.setColor(Color.black);
			g2.drawString(sentence.postags[i], XLeftPosArray[i], YArray[i]);
			g2.drawString(sentence.words[i], XLeftWordArray[i], YArray[i] + fontHight);
			g2.setColor(Color.black);
			int pIndex = sentence.indexes[i];   
			if (pIndex==0) continue;
			pIndex--; // 0-based
			if (sentence.loopIndexes.get(i)) continue; 
			drawLine(XMiddleWordArray[pIndex],YArray[pIndex] + textTopMargin + fontHight,
				XMiddleWordArray[i],YArray[i]-fontHight - textTopMargin, g2);
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
	
	private void drawWrongNodesBoxes(Graphics2D g2) {
		if (wrongNodes==null) return;
		g2.setColor(Color.red);
		for(int w : wrongNodes) {
			g2.drawRect(Math.min(XLeftPosArray[w], XLeftWordArray[w]), 
					YArray[w] - fontHight + wrongNodesBorder, 
					posWordLengthArray[w], doubleFontHight);
		}
		g2.setColor(Color.black);
	}
	
	private void drawCrossingIndexes(Graphics2D g2) {
		g2.setColor(Color.red);
		for(int i=0; i<sentence.length; i++) {
			BitSet iBs = sentence.constituents[i];
			int p=-1, n;
			do {
				n = iBs.nextSetBit(p+1);
				if (n==-1) break;
				if (p!=-1 && n!=p+1) {
					g2.drawLine(
							XMiddleWordArray[p], YArray[p],
							XMiddleWordArray[n], YArray[n] );
				}
				p=n;				
			} while(true);
		}
		g2.setColor(Color.black);
	}
	    	
    public void loadSentence() {        	
    	sentence = treebank.get(sentenceNumber);
				
		XLeftWordArray = new int[sentence.length];
		XLeftPosArray = new int[sentence.length];
		XMiddleWordArray = new int[sentence.length];
		YArray = new int[sentence.length];		
		posWordLengthArray = new int[sentence.length];
		int treeWidth = 0;
		int treeHeight = 0;
		int previousWordLength=0;
		int previousXLeft = leftMargin;
		for(int i=0; i<sentence.length; i++) {
			int wordLength = metrics.stringWidth(sentence.words[i]);
			int posLength = metrics.stringWidth(sentence.postags[i]);
			int posWordLength = Math.max(wordLength, posLength);
			posWordLengthArray[i] = posWordLength;
			XMiddleWordArray[i] = posWordLength/2 + previousXLeft + previousWordLength + wordSpace;
			XLeftWordArray[i] = XMiddleWordArray[i] - (wordLength/2);
			XLeftPosArray[i] = XMiddleWordArray[i] - (posLength/2);			
			YArray[i] = topMargin + fontHight + sentence.depths[i]*levelSize;
			if (YArray[i]>treeHeight) treeHeight = YArray[i];			
			previousWordLength = posWordLength;
			previousXLeft = XLeftWordArray[i];			
			if (i==sentence.length-1) treeWidth = previousXLeft + previousWordLength + wordSpace + posWordLength;
		}
		treeWidth += leftMargin + rightMargin;
		treeHeight += bottomMargin;
		area.width = treeWidth;
        area.height = treeHeight; 
    }

	
	public void exportOddsToPdf(File loopFile, File nonProjectiveFile, File corpusFile) {
		int currentSentence = sentenceNumber;
		Color bgColor = new Color(0xFF, 0xDE, 0xAD);
		try {
			Document lDocument = new Document();
			Document npDocument = new Document();
			PdfWriter lWriter = PdfWriter.getInstance(lDocument,new FileOutputStream(loopFile));
			PdfWriter npWriter = PdfWriter.getInstance(npDocument,new FileOutputStream(nonProjectiveFile));
			lDocument.open();
			npDocument.open();
			PdfContentByte cbL = lWriter.getDirectContent();
			PdfContentByte cbNP = npWriter.getDirectContent();
			Chunk loopSentenceLabel = new Chunk("Sentences with loops, in file " + corpusFile.getName());				
			Chunk npSentenceLabel = new Chunk("Sentences with non-projectivity, in file " + corpusFile.getName());
			lDocument.add(loopSentenceLabel);
			npDocument.add(npSentenceLabel);
			for(sentenceNumber = 0; sentenceNumber<treebank.size(); sentenceNumber++) {
				loadSentence();						
				boolean loops = sentence.hasLoops;
				boolean projective = sentence.isProjective();
				if (!loops && projective) continue;
				Rectangle r = new Rectangle((int)area.getWidth(), (int)area.getHeight());
				Chunk sentenceLabel = new Chunk("Sentence # " + (sentenceNumber+1));
				sentenceLabel.setBackground(bgColor);				
				if (loops) {
					lDocument.setPageSize(r);
					lDocument.newPage();
					lDocument.add(sentenceLabel);
					Graphics2D g2 = cbL.createGraphics((int)area.getWidth(), (int)area.getHeight());
					render(g2);
					g2.dispose();
				}
				if (!projective) {
					npDocument.setPageSize(r);
					npDocument.newPage();
					npDocument.add(sentenceLabel);
					Graphics2D g2 = cbNP.createGraphics((int)area.getWidth(), (int)area.getHeight());
					render(g2);
					g2.dispose();
				}				
			}
			lDocument.close();
			npDocument.close();
		}		
		catch (DocumentException de) {System.err.println(de.getMessage());} 
		catch (IOException ioe) {System.err.println(ioe.getMessage());}					
		sentenceNumber = currentSentence;
	}



}
