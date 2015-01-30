package viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.JCheckBox;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import tdg.DSConnl;
import tdg.corpora.MstSentenceUlab;
import tdg.corpora.MstSentenceUlabAdvanced;

@SuppressWarnings("serial")
public class DepTreePanelLabeled extends TreePanel<DSConnl> {

	DSConnl sentence;
	
	int[] XLeftArray, XLeftWordArray, XLeftPosArray, XLeftLabelArray, YArray, XMiddleWordArray, posWordLabelLengthArray;	    
	BitSet wrongIndexes;
	BitSet wrongPos;
	BitSet wrongLabels;	
	
	public JCheckBox skewedLinesCheckBox;
	public JCheckBox hideRelationsCheckBox;
	public static int wrongNodesBorder = 2;
	
	
	
	public DepTreePanelLabeled() {		
	}
	
	public DepTreePanelLabeled(ArrayList<DSConnl> treebank) {
		loadTreebank(treebank); 
	}
	
	public void loadTreebank(ArrayList<DSConnl> treebank) {
		this.treebank = treebank;
		lastIndex = treebank.size()-1; 
	}
	
	public void setWrongIndexesPosLabels(BitSet[] wrongIndexesPosLabels) {
    	this.wrongIndexes = wrongIndexesPosLabels[0];
    	this.wrongPos = wrongIndexesPosLabels[1];
    	this.wrongLabels = wrongIndexesPosLabels[2];
    }
	
	public DSConnl getSentence(int n) {
		return treebank.get(n);
	}
	
	public void init() {
		levelSizeFactor = (hideRelationsCheckBox.isSelected()) ? 3 : 4;
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
        drawLoopNodesBoxes(g2);
		//drawCrossingIndexes(g2);
	}
    
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);        
        Graphics2D g2 = (Graphics2D) g;        
        render(g2);
    }    	
		
	private void drawWordsAndArrows(Graphics2D g2) {
		int[] indexes = sentence.getIndexes();
		for(int i=0; i<sentence.length; i++) {			
			
			int hightPlus = 0;
			
			if (!hideRelationsCheckBox.isSelected()) {
				g2.setColor(wrongLabels!=null && wrongLabels.get(i) ? Color.red : Color.black);
				g2.drawString(sentence.labels[i], XLeftLabelArray[i], YArray[i]);
				hightPlus += fontHight;
			}
			
			g2.setColor(wrongPos!=null && wrongPos.get(i) ? Color.red : Color.black);
			g2.drawString(sentence.posTags[i], XLeftPosArray[i], YArray[i]+ hightPlus);			
			hightPlus += fontHight;
			
			g2.setColor(Color.black);
			g2.drawString(sentence.words[i], XLeftWordArray[i], YArray[i] + hightPlus);
						
			int pIndex = indexes[i];   
			if (pIndex==0) continue;
			pIndex--; // 0-based
			if (sentence.loopIndexes.get(i)) continue; 
			
			g2.setColor(wrongIndexes!=null && wrongIndexes.get(i) ? Color.red : Color.black);
			drawLine(XMiddleWordArray[pIndex],YArray[pIndex] + textTopMargin + hightPlus,
				XMiddleWordArray[i],YArray[i] - fontHight - textTopMargin, g2);
			
			g2.setColor(Color.black);
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
	
	private void drawLoopNodesBoxes(Graphics2D g2) {
		if (sentence.loopIndexes==null || sentence.loopIndexes.isEmpty()) return;
		g2.setColor(Color.red);
		int w = sentence.loopIndexes.nextSetBit(0);
		while(w!=-1) {			
			g2.drawRect(XLeftArray[w], YArray[w] - fontHight + wrongNodesBorder, 
					posWordLabelLengthArray[w], tripleFontHight);
			w = sentence.loopIndexes.nextSetBit(w+1);
		}
		g2.setColor(Color.black);
	}
	
	/*private void drawCrossingIndexes(Graphics2D g2) {
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
	}*/
	    	
    public void loadSentence() {        	
    	sentence = treebank.get(sentenceNumber);
    	XLeftArray = new int[sentence.length];
		XLeftWordArray = new int[sentence.length];
		XLeftPosArray = new int[sentence.length];
		XLeftLabelArray = new int[sentence.length];
		XMiddleWordArray = new int[sentence.length];
		YArray = new int[sentence.length];		
		posWordLabelLengthArray = new int[sentence.length];
		int treeWidth = 0;
		int treeHeight = 0;
		int previousWordLength=0;
		int previousXLeft = leftMargin;
		for(int i=0; i<sentence.length; i++) {
			int wordLength = metrics.stringWidth(sentence.words[i]);
			int posLength = metrics.stringWidth(sentence.posTags[i]);
			int labelLength = metrics.stringWidth(sentence.labels[i]);
			int posWordLabelLength = Math.max(Math.max(wordLength, posLength), labelLength);
			posWordLabelLengthArray[i] = posWordLabelLength;
			XMiddleWordArray[i] = posWordLabelLength/2 + previousXLeft + previousWordLength;
			if (i!=0 && sentence.depths[i]==sentence.depths[i-1]) XMiddleWordArray[i] += wordSpace; 
			else XMiddleWordArray[i] += wordSpace/3;
			int xLeftWord = XLeftWordArray[i] = XMiddleWordArray[i] - (wordLength/2);
			int xLeftPos = XLeftPosArray[i] = XMiddleWordArray[i] - (posLength/2);			
			int xLeftLabel = XLeftLabelArray[i] = XMiddleWordArray[i] - (labelLength/2);
			XLeftArray[i] = Math.min(xLeftWord, Math.min(xLeftPos, xLeftLabel));
			YArray[i] = topMargin + sentence.depths[i]*levelSize;
			if (YArray[i]>treeHeight) treeHeight = YArray[i];			
			previousWordLength = posWordLabelLength;
			previousXLeft = XLeftWordArray[i];			
			if (i==sentence.length-1) treeWidth = previousXLeft + previousWordLength + wordSpace + posWordLabelLength;
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
