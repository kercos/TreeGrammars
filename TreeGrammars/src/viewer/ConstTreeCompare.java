package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import tdg.TDNode;
import tdg.corpora.ConnlToUlab;
import tdg.corpora.DepCorpus;
import tdg.corpora.MstSentenceUlab;
import tdg.corpora.MstSentenceUlabAdvanced;
import tdg.corpora.WsjD;
import tsg.TSNode;
import tsg.TSNodeLabel;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;
import tsg.parseEval.EvalC;
import util.file.FileUtil;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

@SuppressWarnings("serial")
public class ConstTreeCompare extends JPanel 
	implements ChangeListener, ActionListener, ComponentListener {
	
	private JPanel controlPanel;
	private JSpinner controlSentenceNumber;
	private JButton plus, minus;
    private ConstTreePanel goldTreePanel, testTreePanel;    			
    private JSplitPane splitPane;
    private JScrollPane goldScroller, testScroller;
    private JCheckBox skewedLinesCheckBox;
    private JComboBox constUnit;
    private JTextField diffScore;
    private JLabel sizeReport;
    private int currentSentenceNumber;
    private int size;
	private static EvalC evaluation;
    
    public static TSNodeLabel initialSentence = initialSentence();
    
	public static TSNodeLabel initialSentence() {
		try {
			return new TSNodeLabel("(S (V load) (NP (D a) (N corpus)))");
			//return new TSNodeLabel("(S (NP-SBJ (NNP-H Japan)) (VP-H (CONJP (RB-H not) (RB only)) (VP-H (VBZ-H outstrips) (NP (DT the) (NNP-H U.S.)) (PP-LOC (IN-H in) (NP (NN investment) (NNS-H flows)))) (CONJP (CC-H but) (RB also)) (VP (VBZ-H outranks) (NP (PRP-H it)) (PP-LOC (IN-H in) (NP (NP-H (NN-H trade)) (PP-CLR (IN-H with) (NP (JJS most) (JJP (JJ Southeast) (JJ-H Asian)) (NNS-H countries)))))))  (. .))");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}   

    public ConstTreeCompare() {
        super(new BorderLayout());        
        size = 1;        
        ArrayList<TSNodeLabel> goldTreebank = new ArrayList<TSNodeLabel>();
        ArrayList<TSNodeLabel> testTreebank = new ArrayList<TSNodeLabel>();
        goldTreebank.add(initialSentence);
        testTreebank.add(initialSentence);
        evaluation = new EvalC(goldTreebank, testTreebank, null, null);        
        buildComponents();
        
        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        resetTreebanks(true);
        init();        
    }
    
    private void resetTreebanks(boolean resetNumber) {
    	evaluation.getTreebanks();
		if (!evaluation.preprocessCorporaAndCheckCompatibility()) {
			JOptionPane.showMessageDialog(this, "Gold and Test files have different sizes.");
        	return;
        }
    	size = evaluation.size();
    	goldTreePanel.loadTreebank(evaluation.getGoldCorpus());
    	testTreePanel.loadTreebank(evaluation.getTestCorpus());    	
    	if (resetNumber) {
    		currentSentenceNumber = 0;
        	controlSentenceNumber.setValue(1);
    		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1,1,size,1);
    		controlSentenceNumber.setModel(spinnerModel);
    	}    	    	
    	sizeReport.setText("of: " + size);    	
    }
    
    private void init() {    	
    	TSNodeLabel s1 = goldTreePanel.getSentence(currentSentenceNumber);
    	TSNodeLabel s2 = testTreePanel.getSentence(currentSentenceNumber);
    	ArrayList<TSNodeLabel>[] goldtestDiff = EvalC.getDiff(s1, s2);
    	goldTreePanel.setWrongNodes(goldtestDiff[0]);    	
    	goldTreePanel.init();    	
    	testTreePanel.setWrongNodes(goldtestDiff[1]);
        testTreePanel.init();                     
        diffScore.setText(goldtestDiff[0].size() + "/" + goldtestDiff[1].size());
    }
    
    private void buildComponents() {
    	SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1,1,size,1);  
        controlSentenceNumber = new JSpinner(spinnerModel);
        controlSentenceNumber.setPreferredSize(new Dimension(100,30));
        controlSentenceNumber.addChangeListener(this);
        controlSentenceNumber.setFocusable(true);    
        
        controlPanel = new JPanel(new BorderLayout());
        controlPanel.setFocusable(false);
        JPanel leftControlPanel = new JPanel();
        JPanel centerControlPanel = new JPanel();
        JPanel rightControlPanel = new JPanel();
        controlPanel.add(leftControlPanel, BorderLayout.WEST);
        controlPanel.add(centerControlPanel, BorderLayout.CENTER);
        controlPanel.add(rightControlPanel, BorderLayout.EAST);
                
        JLabel skewedLinesLabel = new JLabel("Skewed:");
        skewedLinesCheckBox = new JCheckBox(""); 
        skewedLinesCheckBox.addActionListener(this);
        skewedLinesCheckBox.setFocusable(false);
        leftControlPanel.add(skewedLinesLabel);
        leftControlPanel.add(skewedLinesCheckBox);
        
        JLabel constUnitLabel = new JLabel("   ConstUnit:");
        String[] types = { "Words", "Chars", "Yield"};
        constUnit = new JComboBox(types);
        constUnit.setSelectedIndex(EvalC.CONSTITUENTS_UNIT);
        constUnit.addActionListener(this);
        constUnit.setFocusable(false);
        leftControlPanel.add(constUnitLabel);
        leftControlPanel.add(constUnit);
                
        plus = new JButton("+");                
        minus = new JButton("-");
        plus.addActionListener(this);        
        minus.addActionListener(this);
        plus.setFocusable(false);
        minus.setFocusable(false);
        centerControlPanel.add(plus);
        centerControlPanel.add(minus);        
        
        JLabel diffsLabel = new JLabel("Diffs: ");
        diffScore = new JTextField(7);
        diffsLabel.setFocusable(false);
        diffScore.setFocusable(false);
        diffScore.setEditable(false);
        rightControlPanel.add(diffsLabel);
        rightControlPanel.add(diffScore);
        rightControlPanel.add(controlSentenceNumber);
        sizeReport = new JLabel();        
        rightControlPanel.add(sizeReport);              
				
        //Set up the drawing area.
        goldTreePanel = new ConstTreePanel();       
        goldTreePanel.skewedLinesCheckBox = skewedLinesCheckBox;
        goldTreePanel.showHeads = new JCheckBox("heads", false);
        goldTreePanel.setBackground(Color.white);        
        goldTreePanel.setFocusable(false);        

        
        testTreePanel = new ConstTreePanel();
        testTreePanel.skewedLinesCheckBox = skewedLinesCheckBox;
        testTreePanel.showHeads = new JCheckBox("heads", false);
        testTreePanel.setBackground(Color.white);
        testTreePanel.setFocusable(false);

        
        //Put the drawing area in a scroll pane.
        goldScroller = new JScrollPane(goldTreePanel);
        goldScroller.setFocusable(false);
        
        testScroller = new JScrollPane(testTreePanel);
        testScroller.setFocusable(false);

        
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        		goldScroller, testScroller);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(300);
        splitPane.setFocusable(false);
        splitPane.setPreferredSize(new Dimension(800, 600));
        splitPane.addComponentListener(this);
    }
    

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source==constUnit) {
			EvalC.CONSTITUENTS_UNIT = constUnit.getSelectedIndex();
			resetTreebanks(false);
		}
		else {
			int fs;
			if (source==plus) {
				fs = goldTreePanel.increaseFontSize();
				testTreePanel.increaseFontSize();
			}
			else {
				fs = goldTreePanel.decreaseFontSize();
				testTreePanel.decreaseFontSize();			
			}
			System.out.println("Font size: " + fs);
		}	
		init();
		controlSentenceNumber.requestFocus();
	}

	public void stateChanged(ChangeEvent e) {
		//System.out.println("stateChanged");
		Object source = e.getSource();
		if (source==skewedLinesCheckBox) {
			
		}
		else {
			// sentence counter
			currentSentenceNumber = (Integer) controlSentenceNumber.getValue() - 1;
			goldTreePanel.goToSentence(currentSentenceNumber);
			testTreePanel.goToSentence(currentSentenceNumber);		
		}
		init();
	}
	
	public void exportToPDF(File selectedFile) {
		// step 1: creation of a document-object
		if (selectedFile.exists()) selectedFile.delete();
		int goldWidth = (int)goldTreePanel.area.getWidth(); 
		int goldHeight = (int)goldTreePanel.area.getHeight();
		int testWidth = (int)testTreePanel.area.getWidth(); 
		int testHeight = (int)testTreePanel.area.getHeight();
		int totalWidth = Math.max(goldWidth, testWidth);
		int totalHeight = goldHeight + testHeight;
		Document document = new Document(new Rectangle(totalWidth, totalHeight));		
		try {
			PdfWriter writer = PdfWriter.getInstance(document,new FileOutputStream(selectedFile));
			document.open();
			PdfContentByte cb = writer.getDirectContent();
			
			PdfTemplate tpGold = cb.createTemplate(goldWidth, goldHeight);
			Graphics2D g2gold = tpGold.createGraphics(goldWidth, goldHeight);
			goldTreePanel.render(g2gold);
			g2gold.dispose();
			cb.addTemplate(tpGold, 0, testHeight);
			
			PdfTemplate tpTest = cb.createTemplate(testWidth, testHeight);
			Graphics2D g2test = tpTest.createGraphics(testWidth, testHeight);
			testTreePanel.render(g2test);
			g2test.dispose();
			cb.addTemplate(tpTest, 0, 0);
			
		} catch (DocumentException de) {
			System.err.println(de.getMessage());
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}
 
		// step 5: we close the document
		document.close();
	}
	
	public void exportAllToPDF(File selectedFile) {
		// step 1: creation of a document-object
		if (selectedFile.exists()) selectedFile.delete();		
		Font font = new Font(Font.TIMES_ROMAN, 15, Font.NORMAL);
		Color bgColor = new Color(0xFF, 0xDE, 0xAD);	
		Document document = new Document();		
		try {
			PdfWriter writer = PdfWriter.getInstance(document,new FileOutputStream(selectedFile));
			document.open();
			PdfContentByte cb = writer.getDirectContent();
			
			for(int sn = 0; sn < size; sn++) {
				goldTreePanel.setSentenceNumber(sn);
				testTreePanel.setSentenceNumber(sn);
				goldTreePanel.loadSentence();				
				testTreePanel.loadSentence();
				TSNodeLabel s1 = goldTreePanel.getSentence(sn);
		    	TSNodeLabel s2 = testTreePanel.getSentence(sn);
		    	ArrayList<TSNodeLabel>[] goldtestDiff = EvalC.getDiff(s1, s2);
		    	goldTreePanel.setWrongNodes(goldtestDiff[0]);    			    	   
		    	testTreePanel.setWrongNodes(goldtestDiff[1]);		        
				
				int goldWidth = (int)goldTreePanel.area.getWidth(); 
				int goldHeight = (int)goldTreePanel.area.getHeight();
				int testWidth = (int)testTreePanel.area.getWidth(); 
				int testHeight = (int)testTreePanel.area.getHeight();
				int totalWidth = Math.max(200, Math.max(goldWidth, testWidth));				
				int totalHeight = goldHeight + testHeight;
				
				//Chunk sentenceLabel = new Chunk("Sentence # " + (sentenceNumber+1), smallfont);
				Chunk sentenceLabel = new Chunk("Sentence # " + (sn+1), font);
				sentenceLabel.setBackground(bgColor);				
				Rectangle r = new Rectangle(totalWidth, totalHeight);								
				document.setPageSize(r);
				document.newPage();
				document.add(sentenceLabel);
				
				PdfTemplate tpGold = cb.createTemplate(goldWidth, goldHeight);
				Graphics2D g2gold = tpGold.createGraphics(goldWidth, goldHeight);
				goldTreePanel.render(g2gold);
				g2gold.dispose();
				cb.addTemplate(tpGold, 0, testHeight);
				
				PdfTemplate tpTest = cb.createTemplate(testWidth, testHeight);
				Graphics2D g2test = tpTest.createGraphics(testWidth, testHeight);
				testTreePanel.render(g2test);
				g2test.dispose();
				cb.addTemplate(tpTest, 0, 0);
			}			
			
		} catch (DocumentException de) {
			System.err.println(de.getMessage());
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}
 
		// step 5: we close the document
		document.close();
	}
	
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Dependency Tree Compare");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        
        
		final ConstTreeCompare contentPane = new ConstTreeCompare();
        contentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(contentPane);
        
      //Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem loadGoldFile = new JMenuItem("Load Gold File");
        JMenuItem loadtestFile = new JMenuItem("Load Test File");
        JMenuItem loadParamFile = new JMenuItem("Load Parameter File");
        JMenuItem compareCorpora = new JMenuItem("Compare");
        JMenuItem compareCorporaWithOutput = new JMenuItem("Compare with output");
        JMenuItem exportPdf = new JMenuItem("Export to PDF");
        JMenuItem exportAllToPdf = new JMenuItem("Export ALL to PDF");
        final JFileChooser fc = new JFileChooser();
                
        exportPdf.setAccelerator(KeyStroke.getKeyStroke('P', java.awt.event.InputEvent.ALT_MASK));
        loadGoldFile.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showOpenDialog(contentPane);
                File selectedFile = fc.getSelectedFile();
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                	evaluation.setGoldFile(selectedFile);
                }               
            }
        });
        loadtestFile.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showOpenDialog(contentPane);
                File selectedFile = fc.getSelectedFile();
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                	evaluation.setTestFile(selectedFile);
                }               
            }
        });
        loadParamFile.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showOpenDialog(contentPane);
                File selectedFile = fc.getSelectedFile();
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                	EvalC.readParametersFromFile(selectedFile);
                	contentPane.resetTreebanks(false);
                	contentPane.init();
                }               
            }
        });
        compareCorpora.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		contentPane.resetTreebanks(true);
        		contentPane.init();
            }
        });        
        compareCorporaWithOutput.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(contentPane);        		
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                	File selectedFile = fc.getSelectedFile();
                    selectedFile = FileUtil.changeExtension(selectedFile, "evalC");
                    contentPane.resetTreebanks(true);
                    if (evaluation.areComparable()) {                    	
                    	evaluation.setOutputFile(selectedFile);
                    	evaluation.makeEval();
                    	contentPane.init();
                    }
                    else {
            			JOptionPane.showMessageDialog(contentPane, "Gold and Test files are not compatible.");
            		}
                }                
            }
        });
        exportPdf.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(contentPane);                
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                	File selectedFile = fc.getSelectedFile();
                	selectedFile = FileUtil.changeExtension(selectedFile, "pdf");
                	contentPane.exportToPDF(selectedFile);
                }                
            }
        });
        exportAllToPdf.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(contentPane);                
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                	File selectedFile = fc.getSelectedFile();
                	selectedFile = FileUtil.changeExtension(selectedFile, "pdf");
                	contentPane.exportAllToPDF(selectedFile);
                }                
            }
        });
            
        file.add(loadGoldFile);
        file.add(loadtestFile);
        file.add(loadParamFile);
        file.add(compareCorpora);
        
        file.add(compareCorporaWithOutput);                
        file.add(exportPdf);
        file.add(exportAllToPdf);
        file.setMnemonic('F');
        file.setMnemonic('P');

        JMenuItem quit = new JMenuItem("Quit");
        //quit.setMnemonic('Q');
        file.add(quit);
        quit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        });


        menuBar.add(file);
        frame.setJMenuBar(menuBar);
        
        
        //compare the window.
        frame.pack();
        frame.setVisible(true);
    }
    
	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}

	public void componentResized(ComponentEvent e) {
		//System.out.println("resized");
		Dimension newDim = splitPane.getSize();		
		splitPane.setDividerLocation(newDim.height/2);
		splitPane.repaint();
	}

	
	
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

}
