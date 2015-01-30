package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tdg.DSConnl;
import tdg.corpora.DepCorpus;
import tdg.corpora.MstSentenceUlabAdvanced;
import tsg.TSNodeLabel;
import util.file.FileUtil;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

@SuppressWarnings("serial")
public class DepTreeViewer extends TreeViewer<DSConnl> {
	
	private static DSConnl initialSentence = 		
		new DSConnl(
    		new String[]{"Load", "a", "corpus", "in", "Connl", "format"},
    		new String[]{"V","D","N","P","N","N"},
    		new int[]{0,3,1,3,6,4},
    		new String[]{"ROOT", "NMOD", "SBJ", "NMOD", "NMOD", "PMOD"}
		);    

    public DepTreeViewer() {
        super();                  
    }
    
    public void setInitialSentence() {
    	ArrayList<DSConnl> treebank = new ArrayList<DSConnl>();
        treebank.add(initialSentence);
        loadTreebank(treebank);
    }
    
    
	protected void loadTreebank(File treebankFile) {                	
		corpusFile = treebankFile;
		ArrayList<DSConnl> treebank = DSConnl.getConnlTreeBank(treebankFile);
		loadTreebank(treebank);
	}
	
	@Override
	protected void setCheckBoxOptions() {
		optionCheckBoxs = new JCheckBox[]{
				new JCheckBox("Skewed Lines", false),
				new JCheckBox("Hide Relations", false)
		};
		
	}
    
	protected void initializeTreePanel() {
    	//Set up the drawing area.
		DepTreePanelLabeled depTreePanelLabeled = new DepTreePanelLabeled();
		depTreePanelLabeled.skewedLinesCheckBox = optionCheckBoxs[0];
		depTreePanelLabeled.hideRelationsCheckBox = optionCheckBoxs[1];
    	treePanel = depTreePanelLabeled;
        treePanel.setBackground(Color.white);
        treePanel.setFocusable(false);        
    }
	
    public static void doBeforeClosing() {	    	
    	//Parameters.logPrintln("--- end of log");
    	//Parameters.closeLogFile();
    } 
    
	/**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
    	JFrame frame = new JFrame("Dependency Tree Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final DepTreeViewer treeViewer = new DepTreeViewer();
        Dimension area = treeViewer.treePanel.area;
        frame.setMinimumSize(new Dimension(area.width+5, area.height+5));
        treeViewer.setOpaque(true); //content panes must be opaque
        frame.setContentPane(treeViewer);
        
        //Create and set up the window.
        
        frame.addWindowListener (new WindowAdapter () {
            public void windowClosing (WindowEvent e) {
            	doBeforeClosing();
            }
        });

        
      //Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem openTreebank = new JMenuItem("Open Treebank");        
        JMenuItem exportEps = new JMenuItem("Export to EPS");
        JMenuItem exportPdf = new JMenuItem("Export to PDF");
        JMenuItem exportAllToPdf = new JMenuItem("Export All to PDF");
        //JMenuItem exportAllToXml = new JMenuItem("Export All to XML");
        JMenuItem quit = new JMenuItem("Quit");
        final JFileChooser fc = new JFileChooser();
        
        openTreebank.setAccelerator(KeyStroke.getKeyStroke('O', java.awt.event.InputEvent.ALT_MASK));
        exportEps.setAccelerator(KeyStroke.getKeyStroke('E', java.awt.event.InputEvent.ALT_MASK));
        exportPdf.setAccelerator(KeyStroke.getKeyStroke('P', java.awt.event.InputEvent.ALT_MASK));	   
        exportAllToPdf.setAccelerator(KeyStroke.getKeyStroke('A', java.awt.event.InputEvent.ALT_MASK));
        //exportAllToXml.setAccelerator(KeyStroke.getKeyStroke('X', java.awt.event.InputEvent.ALT_MASK));
        quit.setAccelerator(KeyStroke.getKeyStroke('Q', java.awt.event.InputEvent.ALT_MASK));
        
        openTreebank.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showOpenDialog(treeViewer);	                
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                	File selectedFile = fc.getSelectedFile();
                	treeViewer.loadTreebank(selectedFile);
                }               
            }
        });
        exportEps.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(treeViewer.treePanel);	                
                if (returnVal == JFileChooser.APPROVE_OPTION) {          
                	File selectedFile = fc.getSelectedFile();
                	selectedFile = FileUtil.changeExtension(selectedFile, "eps");
                    treeViewer.treePanel.exportToEPS(selectedFile);
                }
            }
        });
        exportPdf.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(treeViewer.treePanel);	                
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                	File selectedFile = fc.getSelectedFile();
	                selectedFile = FileUtil.changeExtension(selectedFile, "pdf");
	                treeViewer.treePanel.exportToPDF(selectedFile);
                }	                    
            }
        });
        exportAllToPdf.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(treeViewer.treePanel);	                
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                	File selectedFile = fc.getSelectedFile();
	                selectedFile = FileUtil.changeExtension(selectedFile, "pdf");
                    treeViewer.treePanel.exportAllToPdf(selectedFile);
                }
            }
        });
        /*exportAllToXml.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(treeViewer.treePanel);	                
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                	File selectedFile = fc.getSelectedFile();
                	selectedFile = FileUtil.changeExtention(selectedFile, "xml");
                    ((TesniereTreePanel)treeViewer.treePanel).exportAllToXML(selectedFile);
                }
            }
        });*/
        quit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	doBeforeClosing();
                System.exit(0);
            }
        });
            
        file.add(openTreebank);
        file.add(exportEps);
        file.add(exportPdf);
        file.add(exportAllToPdf);
        //file.add(exportAllToXml);
        file.add(quit);
        openTreebank.setMnemonic('O');	        
        exportPdf.setMnemonic('P');
        exportAllToPdf.setMnemonic('A');
        //exportAllToXml.setMnemonic('X');
        quit.setMnemonic('Q');	        	        

        menuBar.add(file);
        frame.setJMenuBar(menuBar);
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
	
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    /*private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Dependency Tree Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        
        
		final DepTreeViewer treeViewer = new DepTreeViewer();
        treeViewer.setOpaque(true); //content panes must be opaque
        frame.setContentPane(treeViewer);
        //newContentPane.setPreferredSize(new Dimension(800,600));
        
      //Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem openTreebank = new JMenuItem("Open Treebank");        
        JMenuItem exportEps = new JMenuItem("Export to EPS");
        JMenuItem exportPdf = new JMenuItem("Export to PDF");
        JMenuItem exportOddsToPdf = new JMenuItem("Export loops to PDF");
        final JFileChooser fc = new JFileChooser();
        
        openTreebank.setAccelerator(KeyStroke.getKeyStroke('O', java.awt.event.InputEvent.ALT_MASK));
        exportEps.setAccelerator(KeyStroke.getKeyStroke('E', java.awt.event.InputEvent.ALT_MASK));
        exportPdf.setAccelerator(KeyStroke.getKeyStroke('P', java.awt.event.InputEvent.ALT_MASK));
        openTreebank.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showOpenDialog(treeViewer);
                File selectedFile = fc.getSelectedFile();
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                	treeViewer.loadTreebank(selectedFile);
                }               
            }
        });
        exportEps.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(treeViewer.treePanel);
                File selectedFile = fc.getSelectedFile();
                selectedFile = FileUtil.changeExtention(selectedFile, "eps");
                if (returnVal == JFileChooser.APPROVE_OPTION)                    
                    treeViewer.treePanel.exportToEPS(selectedFile);                    
            }
        });
        exportPdf.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(treeViewer.treePanel);
                File selectedFile = fc.getSelectedFile();
                selectedFile = FileUtil.changeExtention(selectedFile, "pdf");
                if (returnVal == JFileChooser.APPROVE_OPTION)
                    treeViewer.treePanel.exportToPDF(selectedFile);
            }
        });
        exportOddsToPdf.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(treeViewer.treePanel);
                File selectedFile = fc.getSelectedFile();
                File loopFile = FileUtil.postpendAndChangeExtension(selectedFile, "_loops", "pdf");
                File nonProjFile = FileUtil.postpendAndChangeExtension(selectedFile, "_nonProj", "pdf");
                selectedFile = FileUtil.changeExtention(selectedFile, "pdf");
                if (returnVal == JFileChooser.APPROVE_OPTION)
                    treeViewer.treePanel.exportOddsToPdf(loopFile, nonProjFile, corpusFile);
            }
        });
            
        file.add(openTreebank);
        file.add(exportEps);
        file.add(exportPdf);
        file.add(exportOddsToPdf);
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


        JMenu window = new JMenu("Window");

        menuBar.add(file);
        menuBar.add(window);
        frame.setJMenuBar(menuBar);
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }*/

	
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
