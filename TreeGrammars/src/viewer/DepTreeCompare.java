package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;

import tdg.TDNode;
import tdg.corpora.ConnlToUlab;
import tdg.corpora.DepCorpus;
import tdg.corpora.MstSentenceUlab;
import tdg.corpora.MstSentenceUlabAdvanced;
import tdg.corpora.WsjD;
import tsg.TSNode;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;
import util.file.FileUtil;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.ParseException;
import java.util.*;

@SuppressWarnings("serial")
public class DepTreeCompare extends JPanel 
	implements ChangeListener, ActionListener, ComponentListener {
	
	private JPanel controlPanel;
	private JSpinner controlSentenceNumber;
	private JButton plus, minus;
    private DepTreePanel goldTreePanel, testTreePanel;    			
    private JSplitPane splitPane;
    private JScrollPane goldScroller, testScroller;
    private JCheckBox skewedLinesCheckBox;
    private JTextField uasScore;
    private JLabel sizeReport;
    private int currentSentenceNumber;
    private int size;
    private ArrayList<MstSentenceUlabAdvanced> goldTreebank, testTreebank;
    
    private static MstSentenceUlabAdvanced initialSentence = new MstSentenceUlabAdvanced(
    		new String[]{"Load", "gold_and_test", "corpora"},
    		new String[]{"V","J","N"},
    		new int[]{0,3,1}
    );    

    public DepTreeCompare() {
        super(new BorderLayout());        
        size = 1;
        goldTreebank = new ArrayList<MstSentenceUlabAdvanced>();
        testTreebank = new ArrayList<MstSentenceUlabAdvanced>();
        goldTreebank.add(initialSentence);
        testTreebank.add(initialSentence);
        buildComponents();
        
        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        resetTreebanks();
        init();        
    }
    
    private void loadGoldCorpus(File goldFile) {
    	boolean isConnl = DepCorpus.isConnlFormat(goldFile);
    	goldTreebank = 
			(isConnl) ?
			MstSentenceUlabAdvanced.getTreebankAdvancedFromConnl(goldFile,"UTF-8") :		
			MstSentenceUlabAdvanced.getTreebankAdvancedFromMst(goldFile,"UTF-8");		
    }
    
    private void loadtestCorpus(File testFile) {
    	boolean isConnl = DepCorpus.isConnlFormat(testFile);
    	testTreebank = 
			(isConnl) ?
			MstSentenceUlabAdvanced.getTreebankAdvancedFromConnl(testFile,"UTF-8") :		
			MstSentenceUlabAdvanced.getTreebankAdvancedFromMst(testFile,"UTF-8");	
    }
    
    private void resetTreebanks() {
    	size = goldTreebank.size();
    	goldTreePanel.loadTreebank(goldTreebank);
    	testTreePanel.loadTreebank(testTreebank);
    	currentSentenceNumber = 0;
    	controlSentenceNumber.setValue(1);
    	SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1,1,size,1);
    	controlSentenceNumber.setModel(spinnerModel);    	
    	sizeReport.setText("of: " + size);
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
                
        skewedLinesCheckBox = new JCheckBox("Straight Lines:"); 
        skewedLinesCheckBox.addActionListener(this);
        skewedLinesCheckBox.setFocusable(false);
        leftControlPanel.add(skewedLinesCheckBox);
                
        plus = new JButton("+");                
        minus = new JButton("-");
        plus.addActionListener(this);        
        minus.addActionListener(this);
        plus.setFocusable(false);
        minus.setFocusable(false);
        centerControlPanel.add(new JLabel("zoom: "));
        centerControlPanel.add(plus);
        centerControlPanel.add(minus);        
        
        JLabel uasLabel = new JLabel("UAS: ");
        uasScore = new JTextField(7);
        uasLabel.setFocusable(false);
        uasScore.setFocusable(false);
        uasScore.setEditable(false);
        rightControlPanel.add(uasLabel);
        rightControlPanel.add(uasScore);
        rightControlPanel.add(controlSentenceNumber);
        sizeReport = new JLabel();        
        rightControlPanel.add(sizeReport);              
				
        //Set up the drawing area.
        goldTreePanel = new DepTreePanel();   
        goldTreePanel.skewedLinesCheckBox = skewedLinesCheckBox;
        goldTreePanel.setBackground(Color.white);        
        goldTreePanel.setFocusable(false);        

        
        testTreePanel = new DepTreePanel();
        testTreePanel.skewedLinesCheckBox = skewedLinesCheckBox;
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
    

    private void init() {    	
    	MstSentenceUlab s1 = goldTreePanel.getSentence(currentSentenceNumber);
        MstSentenceUlab s2 = testTreePanel.getSentence(currentSentenceNumber);
        Integer[] wrongNodes = MstSentenceUlab.wrongWords(s1, s2);
    	goldTreePanel.init();    	
    	testTreePanel.setWrongNodes(wrongNodes);
        testTreePanel.init();                
        int length = s1.length;
        int uas = length - wrongNodes.length;
        uasScore.setText(uas + "/" + length);
    }

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source==skewedLinesCheckBox) {
			//goldTreePanel.straightLines = !goldTreePanel.straightLines;  
			//testTreePanel.straightLines = !testTreePanel.straightLines;
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
		System.out.println("stateChanged");
		Object source = e.getSource();
		if (source==skewedLinesCheckBox) {
			//goldTreePanel.straightLines = !goldTreePanel.straightLines;  
			//testTreePanel.straightLines = !testTreePanel.straightLines;
		}
		else {
			// sentence counter
			currentSentenceNumber = (Integer) controlSentenceNumber.getValue() - 1;
			goldTreePanel.goToSentence(currentSentenceNumber);
			testTreePanel.goToSentence(currentSentenceNumber);		
		}
		init();
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
        
		final DepTreeCompare contentPane = new DepTreeCompare();
        contentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(contentPane);
        
      //Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem loadGoldFile = new JMenuItem("Load Gold");
        JMenuItem loadtestFile = new JMenuItem("Load test");
        JMenuItem compareCorpora = new JMenuItem("Compare");
        JMenuItem exportEps = new JMenuItem("Export to EPS");
        JMenuItem exportPdf = new JMenuItem("Export tp PDF");
        final JFileChooser fc = new JFileChooser();
                
        exportEps.setAccelerator(KeyStroke.getKeyStroke('E', java.awt.event.InputEvent.ALT_MASK));
        exportPdf.setAccelerator(KeyStroke.getKeyStroke('P', java.awt.event.InputEvent.ALT_MASK));
        loadGoldFile.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showOpenDialog(contentPane);
                File selectedFile = fc.getSelectedFile();
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                	contentPane.loadGoldCorpus(selectedFile);
                }               
            }
        });
        loadtestFile.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showOpenDialog(contentPane);
                File selectedFile = fc.getSelectedFile();
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                	contentPane.loadtestCorpus(selectedFile);
                }               
            }
        });
        compareCorpora.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		contentPane.resetTreebanks();
        		contentPane.init();           
            }
        });
        exportEps.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(contentPane);
                File selectedFile = fc.getSelectedFile();
                File selectedFileGold = FileUtil.appendAndAddExtention(selectedFile, "_gold", "eps");
                File selectedFiletest = FileUtil.appendAndAddExtention(selectedFile, "_test", "eps");
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                	contentPane.goldTreePanel.exportToEPS(selectedFileGold);
                	contentPane.testTreePanel.exportToEPS(selectedFiletest);
                }                
            }
        });
        exportPdf.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(contentPane);
                File selectedFile = fc.getSelectedFile();
                File selectedFileGold = FileUtil.appendAndAddExtention(selectedFile, "_gold", "pdf");
                File selectedFiletest = FileUtil.appendAndAddExtention(selectedFile, "_test", "pdf");
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                	contentPane.goldTreePanel.exportToPDF(selectedFileGold);
                	contentPane.testTreePanel.exportToPDF(selectedFiletest);
                }                
            }
        });
            
        file.add(loadGoldFile);
        file.add(loadtestFile);
        file.add(compareCorpora);
        
        file.add(exportEps);
        file.add(exportPdf);
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
