package viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JPanel;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;
import util.file.FileUtil;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

@SuppressWarnings("serial")
public abstract class TreePanel<T> extends JPanel {

	Dimension area = new Dimension(0,0);
	public static int minAreaWidth = 175;
	public static int minAreaHeight = 80;
	
	ArrayList<T> treebank;
	
	String flatSentence;
	int sentenceNumber;
	int lastIndex;
	
	int fontSize = 15;
	int smallFontSize = 11;
	int fontDescendent;
    Font font, smallFont;    
    FontMetrics metrics;
    
    int topMargin = 60;
    int bottomMargin = 60;	
	int leftMargin = 20; 
	int rightMargin = 20;
	int wordSpace = 10;	
	int textTopMargin = 4;
	int levelSizeFactor = 2;
	int fontHight, doubleFontHight, tripleFontHight, levelSize;
	
	public abstract void loadTreebank(ArrayList<T> corpus);
		
	public void init() {
		loadFont();
		loadSentence();
        setPreferredSize(area);        
        revalidate();
        repaint();            
	}
    
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
	
	public abstract void render(Graphics2D g2);
	
	public int sentenceNumber() {
		return sentenceNumber();
	}
	
	public int increaseFontSize() {
		return ++fontSize;
	}
	
	public int decreaseFontSize() {
		if (fontSize==1) return 1;
		return --fontSize;
	}
	
	public int nextSentence() {
		if (sentenceNumber==lastIndex) return sentenceNumber=0;
		return ++sentenceNumber;
	}
	
	public int previousSentence() {
		if (sentenceNumber==0) return sentenceNumber=lastIndex;
    	return --sentenceNumber;
	}
	
	public int goToSentence(int n) {
		if (n<0) return sentenceNumber=0;
		if (n>lastIndex) return sentenceNumber=lastIndex;		    
    	return sentenceNumber = n;
	}
	
	public void loadFont() {
		font = new Font("Serif", Font.PLAIN, fontSize);
		smallFont = new Font("Serif", Font.PLAIN, smallFontSize);
    	metrics = getFontMetrics(font);
    	fontDescendent = metrics.getDescent();
    	fontHight = metrics.getHeight();    	
    	doubleFontHight = 2 * fontHight;
    	tripleFontHight = 3 * fontHight;
    	levelSize = fontHight * levelSizeFactor;	
	}
	
    /**
     * Exports the current graph to EPS.
     *
     * @param file the eps file to export to.
     * @throws IOException if IO goes wrong.
     */
    public void exportToEPS(File file) {

        //EpsGraphics dummy = new EpsGraphics("Title", new ByteArrayOutputStream(),
        //    0, 0, 1, 1, ColorMode.BLACK_AND_WHITE);

        //render(dummy);
    	try {
    		EpsGraphics g2 = new EpsGraphics("Title", new FileOutputStream(file), 0, 0,
    				(int) area.getWidth() + 2, (int) area.getHeight(), ColorMode.COLOR_RGB);
    		render(g2);

            g2.flush();
            g2.close();
    	} catch(IOException e) {
    		FileUtil.handleExceptions(e);
    	}        
    }
    
	public void exportToPDF(File selectedFile) {
		// step 1: creation of a document-object
		if (selectedFile.exists()) selectedFile.delete();
		Document document = new Document(new Rectangle((int)area.getWidth(), (int)area.getHeight()));
		try {
			// step 2:
			// we create a writer
			PdfWriter writer = PdfWriter.getInstance(
			// that listens to the document
					document,
					// and directs a PDF-stream to a file
					new FileOutputStream(selectedFile));
			// step 3: we open the document
			document.open();
			// step 4:
			// we create a template and a Graphics2D object that corresponds
			// with it
			PdfContentByte cb = writer.getDirectContent();
			Graphics2D g2 = cb.createGraphics((int)area.getWidth(), (int)area.getHeight());
			//paint(g2);
			render(g2);
			g2.dispose();
		} catch (DocumentException de) {
			System.err.println(de.getMessage());
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}
 
		// step 5: we close the document
		document.close();
	}
	
	public void exportAllToPdf(File outputFile) {
		if (outputFile.exists()) outputFile.delete();
		int currentSentence = sentenceNumber;		
		Color bgColor = new Color(0xFF, 0xDE, 0xAD);
		try {
			Document document = new Document();
			PdfWriter writer = PdfWriter.getInstance(document,new FileOutputStream(outputFile));
			document.open();
			PdfContentByte cbL = writer.getDirectContent();
			//Chunk intro = new Chunk("Conversion of sec-00 of the WSJ in TDS");
			//document.add(intro);
			for(sentenceNumber = 0; sentenceNumber<treebank.size(); sentenceNumber++) {
				//System.out.println(sentenceNumber);
				loadSentence();						
				com.lowagie.text.Rectangle r = new com.lowagie.text.Rectangle((int)area.getWidth(), (int)area.getHeight());
				Chunk sentenceLabel = new Chunk("Sentence # " + (sentenceNumber+1));
				sentenceLabel.setBackground(bgColor);				
				document.setPageSize(r);
				document.newPage();
				document.add(sentenceLabel);
				Graphics2D g2 = cbL.createGraphics((int)area.getWidth(), (int)area.getHeight());
				render(g2);
				g2.dispose();
			}
			document.close();
		}		
		catch (DocumentException de) {System.err.println(de.getMessage());} 
		catch (IOException ioe) {System.err.println(ioe.getMessage());}					
		sentenceNumber = currentSentence;
	}
	

	public abstract void loadSentence();
}
