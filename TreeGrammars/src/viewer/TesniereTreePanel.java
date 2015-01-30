package viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.swing.JCheckBox;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import settings.Parameters;
import tesniere.*;
import util.Utility;
import util.file.FileUtil;

@SuppressWarnings("serial")
public class TesniereTreePanel extends TreePanel<Box> {
	
	int sentenceLength, boxCount;
	int[][] boxesBorders;
	int[] boxesDepths;
	int[] boxesCoordGapsAbove;
	Box[] boxesArray;
	Word[] wordsArray;
	String[] wordsArrayString;
	//int[] wordsDepths;
	IdentityHashMap<Word, Box> wordBoxMapping;
	IdentityHashMap<Box, Integer> boxIndexTable;
	int[] XLeftArray, YArray, XMiddleArray, wordLengthsArray;	
		
	int interBorder = 4;	
	int catBoxSize = 4;
	int coordGapYSize = 3 * catBoxSize;
	int coordGapXSize = 3 * catBoxSize;
	int spaceBetweenBoxes = wordSpace - 2*interBorder; // 6
	int halfSpaceBetweenBoxes = spaceBetweenBoxes/2; //3
	//int wordSpace = 14;
	
	Color coordColor = Color.orange;
	Color verbColor = Color.red;
	Color adverbColor = Color.orange;
	Color nounColor = Color.blue;
	Color adjColor = Color.green;
	Color[] colorsCat = new Color[]{verbColor, adverbColor, nounColor, adjColor};
	Color contenteWordColor = Color.black;
	Color functionalWordColor = Color.gray;
	Color conjunctionColor = coordColor;
	Color punctColor = Color.magenta;
	Color[] colorsWord = new Color[]{contenteWordColor, functionalWordColor, conjunctionColor, punctColor};
	String[] lettersCat = new String[]{"V","A","N","J"};
	
	public JCheckBox skewedLinesCheckBox;
	public JCheckBox displayCategories;
	
	public static boolean printOnCosoleCurrentTree;
	
	
	public TesniereTreePanel() {
		levelSizeFactor = 3;
	}
	
	public TesniereTreePanel(ArrayList<Box> treebank) {
		loadTreebank(treebank);
	}
	
	public void loadTreebank(ArrayList<Box> treebank) {
		this.treebank = treebank;
		lastIndex = treebank.size()-1; 
		sentenceNumber=0;
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
            		        
        //g2.drawRect(leftMargin, topMargin, area.width, area.height);
        
        int i=0;
		while(i<sentenceLength) {					
			Word w = wordsArray[i];
			g2.setColor(colorsWord[w.wordType()]);
			g2.drawString(w.getLex(), XLeftArray[i], YArray[i]);
			g2.setColor(Color.black);
			i++;
		}
		i=0;
		for(Box b : boxesArray) {			
			int[] borders = boxesBorders[i]; //Xtl, Ytl, width, height, middleX
			if (b.isJunctionBlock()) {
				g2.setColor(coordColor);      
				g2.drawRect(borders[0], borders[1], borders[2], borders[3]);
				g2.setColor(Color.black);
			}
			else {				
				g2.drawRect(borders[0], borders[1], borders[2], borders[3]);				
			}			
			int cat = b.derivedCat;
			if (cat!=-1) {
				g2.setColor(colorsCat[cat]);
				Rectangle r = new Rectangle(borders[4]-catBoxSize, 
						borders[1]-catBoxSize, catBoxSize*2, catBoxSize);
				g2.fill(r);		
				g2.setColor(Color.black);
				if (displayCategories.isSelected()) {
					g2.setFont(smallFont);
					g2.drawString(lettersCat[cat], borders[4] + catBoxSize + 2, borders[1]-2);
				}
				g2.setFont(font);
			}
			cat = b.originalCat;
			if (cat!=-1) {
				g2.setColor(colorsCat[cat]);
				Rectangle r = new Rectangle(borders[4]-catBoxSize, 
						borders[1]+borders[3], catBoxSize*2, catBoxSize);
				g2.fill(r);		
				g2.setColor(Color.black);
				if (displayCategories.isSelected()) {
					g2.setFont(smallFont);
					g2.drawString(lettersCat[cat], borders[4] + catBoxSize + 2, borders[1]+borders[3]+smallFontSize-2);
				}
				g2.setFont(font);
			}			
			if (b.dependents!=null) {
				for(Box d : b.dependents) {
					int dIndex = boxIndexTable.get(d);
					int[] depBorders = boxesBorders[dIndex];
					drawLine(depBorders[4],depBorders[1],
							borders[4],borders[1]+borders[3], g2);
				}
			}
			i++;
		}				  									
		
	}
	
	private int[] getBorders(Box b) {
		//Xtl, Ytl, width, height		
		int x, y, width, height;
		if (b.isJunctionBlock()) {			
			int startIndex = b.startPosition(false, false, true);
			int endIndex = b.endPosition(false, false, true);
			int extraXGapsRight = b.junctionBoxDownHavingRightmostWord(wordsArray[endIndex], true);
			int extraXGapsLeft = b.junctionBoxDownHavingLeftmostWord(wordsArray[startIndex], true);
			x = XLeftArray[startIndex] - interBorder - halfSpaceBetweenBoxes - 
				(1+extraXGapsLeft)*coordGapXSize;
			width = XLeftArray[endIndex] + wordLengthsArray[endIndex] - x + interBorder + 
				halfSpaceBetweenBoxes + (1+extraXGapsRight)*coordGapXSize;
			Box lowestBox = getLowestBox(b);
			int lowerBoxDepth = boxesDepths[boxIndexTable.get(lowestBox)];
			int indexLowestWord = lowestBox.startPosition();			
			Word upperWord = b.getUpperWord();
			Box boxOfUpperWord = wordBoxMapping.get(upperWord);
			int coordGapAbove = boxOfUpperWord.numberOfCoordinationGapsAboveBeforeReaching(b);
			int indexUpperWord = upperWord.getPosition();			
			y = YArray[indexUpperWord] - fontSize - interBorder - coordGapAbove*coordGapYSize;		
			int coordGapLow = b.getInnerMaxCoordBox(lowerBoxDepth, false, false);
			int totalYGaps = coordGapAbove+coordGapLow+1;
			if (coordGapLow>0 ) {
				int lowestBoxGapsAbove = boxesCoordGapsAbove[boxIndexTable.get(lowestBox)];
				int currentBoxGapsAbove = boxesCoordGapsAbove[boxIndexTable.get(b)];
				if (lowestBoxGapsAbove-currentBoxGapsAbove==totalYGaps) {
					totalYGaps--;
				}
			}
			height = YArray[indexLowestWord] - YArray[indexUpperWord] + 
				fontSize + fontDescendent + 2 * interBorder + (totalYGaps)*coordGapYSize;
		}
		else {
			Word lw = b.leftMostWord(false,false);
			Word rw = b.rightMostWord(false,false);
			int lwP = lw.getPosition();
			int rwP = rw.getPosition();
			x = XLeftArray[lwP] - interBorder;
			y = YArray[lwP] - fontSize - interBorder;
			width = XLeftArray[rwP] + wordLengthsArray[rwP] - x + interBorder;
			height = fontSize + 2*interBorder + fontDescendent;
		}
		int middleX = x + width/2;
		return new int[] {x, y, width, height, middleX};
	}
	
	private Box getLowestBox(Box b) {
		int maxDepth = -1;
		Box lowestBox = null;
		ArrayList<Box> lowestBoxes = b.getLowestBlocks();
		for(Box l : lowestBoxes) {
			int depth = YArray[l.getUpperWord().getPosition()];
			//int depth = boxesDepths[boxIndexTable.get(l)];
			if (depth>maxDepth) {
				maxDepth = depth;
				lowestBox = l;
			}
		}
		return lowestBox;
	}

	private void drawLine(int x1, int y1, int x2, int y2, Graphics2D g2) {
		y1 -= catBoxSize;
		y2 += catBoxSize;
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
    	Box t = treebank.get(sentenceNumber);    	
    	if (printOnCosoleCurrentTree) System.out.println(t);
    	boxIndexTable = new IdentityHashMap<Box, Integer>();
    	wordBoxMapping = new IdentityHashMap<Word, Box>();
    	boxCount = t.countAllNodes();
    	sentenceLength = t.countAllWords();
    	boxesBorders = new int[boxCount][];    	
    	wordsArray = new Word[sentenceLength];
    	wordsArrayString = new String[sentenceLength];
    	t.fillWordArrayTable(wordsArray, wordBoxMapping);
    	boxesArray = t.collectBoxStructure().toArray(new Box[boxCount]);   
    	boxesDepths = new int[boxCount];
    	boxesCoordGapsAbove = new int[boxCount];
    	Arrays.fill(boxesCoordGapsAbove, -1);
    	int maxDepth = t.maxDepth();    	
		XLeftArray = new int[sentenceLength];
		XMiddleArray = new int[sentenceLength];
		wordLengthsArray = new int[sentenceLength];
		Arrays.fill(XLeftArray, -1);
		YArray = new int[sentenceLength];	
		int treeWidth=0, treeHeight;
		int previousWordLength=0;
		int previousXLeft = leftMargin;
		for(int i=0; i<boxCount; i++) {
			Box b = boxesArray[i];
			boxIndexTable.put(b, i);		
			boxesDepths[i] = b.getDepth();
    		boxesCoordGapsAbove[i] = b.numberOfCoordinationGapsAbove();
    	}
		int previousBoxDepth = -1;
		boolean previousIsRightmost = false;
		int previousExtraGapXCoord = 0;
		for(int j=0; j<sentenceLength; j++) {
			Word w = wordsArray[j];
			wordsArrayString[j] = w.getLex();
			Box boxW = wordBoxMapping.get(w);
    		int boxWIndex = boxIndexTable.get(boxW);
    		int coordGaps = boxesCoordGapsAbove[boxWIndex];
    		int wDepths = boxesDepths[boxWIndex];    		
			int wordLength = metrics.stringWidth(w.getLex());
			boolean currentIsLeftmost = w==boxW.leftMostWord(false,false);
			boolean adjacentBlock = previousIsRightmost && currentIsLeftmost && wDepths==previousBoxDepth;
			wordLengthsArray[j] = wordLength;    			
			int currentExtraGapXCoord = boxW.junctionBoxUpHavingLeftmostWord(w);
			if (currentExtraGapXCoord>0 && previousIsRightmost) currentExtraGapXCoord++;
			XMiddleArray[j] = wordLength/2 + previousXLeft + previousWordLength + 
				wordSpace + (currentExtraGapXCoord+previousExtraGapXCoord) * coordGapXSize;
			if (adjacentBlock) XMiddleArray[j] += wordSpace;
			XLeftArray[j] = previousXLeft = XMiddleArray[j] - (wordLength/2);
			YArray[j] = topMargin + fontSize + wDepths * levelSize + coordGaps * coordGapYSize;
			if (w.isEmpty() && boxW.isJunctionBlock()) YArray[j] += coordGapYSize;
			previousWordLength = wordLength;				
			if (j==sentenceLength-1) treeWidth = previousXLeft + previousWordLength + wordSpace;
			previousBoxDepth = wDepths;
			previousIsRightmost = w==boxW.rightMostWord(false,false);
			previousExtraGapXCoord = boxW.junctionBoxUpHavingRightmostWord(w);
			if (previousExtraGapXCoord>0 && currentIsLeftmost) previousExtraGapXCoord++;
		}
		// PUT PUNCTUATION AT THE SAME LEVEL OF PREVIOUS WORD
		/*for(int j=0; j<sentenceLength; j++) {
			Word w = wordsArray[j];
			if (w.isPunctuation() && j>0) {
				YArray[j] = YArray[j-1];
			}
		}*/
		for(int i=0; i<boxCount; i++) {
    		Box b = boxesArray[i];
    		boxesBorders[i] = getBorders(b);
    	}
		flatSentence = Utility.joinStringArrayToString(wordsArrayString, " ");
		treeWidth += rightMargin;
		treeHeight = topMargin + fontSize + maxDepth*(levelSize+coordGapYSize) + bottomMargin;
		area.width = Math.max(treeWidth, minAreaWidth);
        area.height = Math.max(treeHeight, minAreaHeight);
    }

	public void exportAllToXML(File outputFile) {
		PrintWriter pw = FileUtil.getPrintWriter(outputFile);
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
		pw.println("<corpus id=\"Penn_WSJ_TDS_1.0\" >");
		int index = 1;
		for(Box t : treebank) {
			pw.println("\n<TDS id=\"tds_" + index + "\" >");
			pw.print(t.toXmlString(0));
			pw.println("</TDS>");
			index++;
		}
		pw.println("\n</corpus>");
		pw.close();		
	}
    
}
