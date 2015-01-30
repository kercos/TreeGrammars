package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import settings.Parameters;
import tesniere.Box;
import tesniere.Conversion;
import tsg.HeadLabeler;
import tsg.TSNodeLabel;
import util.file.FileUtil;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
	
	@SuppressWarnings("serial")
	public class TesniereTreeViewer extends TreeViewer<Box> {
	
		private static HeadLabeler HL;				
		
		private static TSNodeLabel initialSentence = initialSentence();    
	
		public static TSNodeLabel initialSentence() {
			try {
				return new TSNodeLabel("(S (RB Please) (, ,) (VP (VB load) (NP (NP (DT a) (NN file) (PP (IN with) (NP (JJ Penn-style) (NN structures)))) (CC or) (NP (DT a) (NN file) (PP (IN with) (NP (JJ TDS-xml) (NN structures)))) (VP (VBD located) (PP (IN in) (NP (JJ your) (NN computer)))))) (. .))");
				//return new TSNodeLabel("(S (NP-SBJ (DT The) (NNP Lorillard) (NN spokeswoman) ) (VP (VBD said) (SBAR (-NONE- 0) (S (NP-SBJ-1 (NN asbestos) ) (VP (VBD was) (VP (VP (VBN used) (NP (-NONE- *-1) ) (PP-TMP (IN in) (NP (DT the) (JJ early) (NN 1950s) ))) (CC and) (VP (VBN replaced) (NP (-NONE- *-1) ) (PP-TMP (IN in) (NP (CD 1956) )))))))) (. .) )");
				//return new TSNodeLabel("");
				//return new TSNodeLabel("(NP (NP (NNS activities) ) (SBAR (WHNP-137 (WDT that) ) (S (NP-SBJ (-NONE- *T*-137) ) (VP (VBP encourage) (, ,) (VBP promote) (CC or) (VBP advocate) (NP (NN abortion) )))))");
				//return new TSNodeLabel("(SINV (`` ``) (S-TPC (NP-SBJ (PRP I)) (VP (VBP think) (SBAR (S (NP-SBJ (DT the) (NN market)) (VP (VBD had) (VP (VBN been) (VP (VBG expecting) (S (NP-SBJ (DT the) (NNP Fed)) (VP (TO to) (VP (VB ease) (ADVP (ADVP-TMP (RBR sooner)) (CC and) (ADVP (ADVP (NP (DT a) (RB little)) (RBR more)) (SBAR (IN than) (S (NP-SBJ (PRP it)) (VP (VBZ has) (VP (PP-TMP (TO to) (NP (NN date))))))))))))))))))) (, ,) ('' '') (VP (VBD said)) (NP-SBJ (NP (NNP Robert) (NNP Johnson)) (, ,) (NP (NP (NN vice) (NN president)) (PP (IN of) (NP (JJ global) (NNS markets))) (PP (IN for) (NP (NNP Bankers) (NNP Trust) (NNP Co))))) (. .))");
				//return new TSNodeLabel("(NP (DT this) (NN consumer) (NNS electronics) (CC and) (NNS appliances) (NN retailing) (NN chain) )");
				//return new TSNodeLabel("(S (VB-H load) (NP (DT a) (NN-H corpus)))");
				//return new TSNodeLabel("(S (RB Please) (, ,) (VB-H load) (NP (NP (DT a) (NN-H file) (PP (IN with) (NP-H (JJ Penn-style) (NN-H structures)))) (CC-H or) (NP (DT a) (NN-H file) (PP (IN with) (NP-H (JJ TDS-xml) (NN-H structures)))) (VP (VBD-H located) (PP (IN in) (NP-H (JJ your) (NN-H computer))))) (. .))");				
				//return new TSNodeLabel("(S (VP (VBG having) (NP (NP (DT an) (JJ RTC-owned) (NN bank) (CC or) (NN thrift) (NN issue) (NN debt)))))");
				//return new TSNodeLabel("(S (NP-SBJ (NNP McDermott) (NNP International) (NNP Inc.) ) (VP (VBD said) (SBAR (-NONE- 0) (S (NP-SBJ (PRP$ its) (NNP Babcock) (CC &) (NNP Wilcox) (NN unit) ) (VP (VBD completed) (NP (NP (DT the) (NN sale) ) (PP (IN of) (NP (PRP$ its) (NNP Bailey) (NNP Controls) (NNP Operations) )) (PP (TO to) (NP (NNP Finmeccanica) (NNP S.p) (. .) (NNP A.) )) (PP (IN for) (NP (QP ($ $) (CD 295) (CD million) ) (-NONE- *U*) ))))))) (. .) )");
				//return new TSNodeLabel("(S (NP (PRP-H You)) (VP-H (MD-H will) (VP (VBN-H see) (NP (PRP him)) (PP (IN when) (NP (PRP-H he)) (VP-H (VBN-H comes))))))");
				//return new TSNodeLabel("(S (NP-SBJ (DT The) (JJ new) (NN-H company)) (VP-H (VBD-H said) (SBAR (S-H (NP-SBJ (PRP-H it)) (VP-H (VBZ-H believes) (SBAR (S-H (NP-SBJ (EX-H there)) (VP-H (VBP-H are) (NP-PRD (NP-H (QP (JJR fewer) (IN than) (CD-H 100)) (JJ potential) (NNS-H customers)) (PP (IN for) (NP-H (NP-H (NNS-H supercomputers)) (VP (VBN-H priced) (PP (IN between) (NP-H (QP-H ($ $) (CD 15) (CD million) (CC-H and) ($ $) (CD 30) (CD million)))) (PRN (: --) (NP-H (RB presumably) (DT the) (NNP Cray-3) (NN price) (NN-H range)))))))))))))) (. .))");
				//return new TSNodeLabel("(S (PP-MNR (IN By) (S-NOM-H (VP-H (VBG-H addressing) (NP (DT those) (NNS-H problems))))) (PRN (, ,) (NP-SBJ (NNP Mr.) (NNP-H Maxwell)) (VP-H (VBD-H said)) (, ,)) (NP-SBJ (DT the) (JJ new) (NNS-H funds)) (VP-H (VBP-H have) (VP (VBN-H become) (`` ``) (ADJP-PRD (RB extremely) (JJ-H attractive) (PP (TO to) (NP-H (NP-H (JJP (JJ Japanese) (CC-H and) (JJ other)) (NNS-H investors)) (PP-LOC (IN outside) (NP-H (DT the) (NNP-H U.S.)))))))) (. .) ('' ''))");
				//return new TSNodeLabel("(S (NP-SBJ (NP-H (DT The) (NNP U.S.) (NNP-H Chamber)) (PP (IN of) (NP-H (NNP-H Commerce)))) (, ,) (S-ADV (ADVP-TMP (RB-H still)) (ADJP-PRD-H (VBN-H opposed) (PP (TO to) (NP-H (DT any) (NN mininum-wage) (NN-H increase))))) (, ,) (VP-H (VBD-H said) (SBAR (S-H (NP-SBJ (DT the) (NN compromise) (NN-H plan) (S (VP-H (TO to) (VP-H (VB-H lift) (NP (DT the) (NN wage) (NN-H floor)) (NP-EXT (CD 27) (NN-H %)) (PP-MNR (IN in) (NP-H (CD two) (NNS-H stages))) (PP-TMP (IN between) (NP-H (NP-H (NNP-H April) (CD 1990)) (CC and) (NP (NNP-H April) (CD 1991)))))))) (`` ``) (VP-H (VP-H (MD will) (VP-H (VB-H be) (ADJP-PRD (JJ-H impossible) (SBAR (IN for) (S-H (NP-SBJ (JJ many) (NNS-H employers)) (VP-H (TO to) (VP-H (VB-H accommodate)))))))) (CC and) (VP (MD will) (VP-H (VB-H result) (PP-CLR (IN in) (NP-H (NP-H (NP-H (DT the) (NN-H elimination)) (PP (IN of) (NP-H (NNS-H jobs))) (PP (IN for) (NP-H (JJ American) (NNS-H workers)))) (CC and) (NP (NP-H (JJR higher) (NNS-H prices)) (PP (IN for) (NP-H (JJ American) (NNS-H consumers)))))))))))) (. .))");
				//return new TSNodeLabel("(S (NP-SBJ (PRP-H It)) (VP-H (VBZ-H 's) (NP-PRD (NP-H (DT the) (JJ petulant) (NN-H complaint)) (PP (IN of) (NP-H (NP-H (DT an) (JJ impudent) (NN-H American)) (SBAR (WHNP (WP-H whom)) (S-H (NP-SBJ (NNP-H Sony)) (VP-H (VBD-H hosted) (PP-TMP (IN for) (NP-H (DT a) (NN-H year))) (SBAR-TMP (IN while) (S-H (NP-SBJ (PRP-H he)) (VP-H (VBD-H was) (PP-PRD (IN on) (NP-H (DT a) (NNP Luce) (NNP-H Fellowship))) (PP-LOC (IN in) (NP-H (NNP-H Tokyo)))))) (: --) (PP (IN to) (NP-H (NP-H (DT the) (NN-H regret)) (PP (IN of) (NP-H (DT both) (NNS-H parties)))))))))))) (. .))");
				//return new TSNodeLabel("(S (NP-SBJ (NNP-H Japan)) (VP-H (CONJP (RB-H not) (RB only)) (VP-H (VBZ-H outstrips) (NP (DT the) (NNP-H U.S.)) (PP-LOC (IN-H in) (NP (NN investment) (NNS-H flows)))) (CONJP (CC-H but) (RB also)) (VP (VBZ-H outranks) (NP (PRP-H it)) (PP-LOC (IN-H in) (NP (NP-H (NN-H trade)) (PP-CLR (IN-H with) (NP (JJS most) (JJP (JJ Southeast) (JJ-H Asian)) (NNS-H countries)))))))  (. .))");
				//return new TSNodeLabel("(NP (PP (NNP-H Peter) (POS 's)) (NN-H book))");
				//return new TSNodeLabel("(S (NP (PRP I)) (VP-H (VB-H believe) (SBAR (IN that) (NP (PRP he)) (VBZ-H speaks))))");
				//return new TSNodeLabel("(S (NP (PRP$ my) (JJ old) (NN-H friend)) (VP-H (VBZ-H is) (VP (VBG-H singing) (NP (JJ this) (JJ nice) (NN-H song)))))");
				//return new TSNodeLabel("(S (NP (NNP Mary)) (VP-H (VBZ-H is) (VP (VBG-H singing) (NP (DT an) (JJP (JJ old) (CC and) (JJ beautiful)) (NN-H song)))))");
				//return new TSNodeLabel("(S (NP (NNP Alfred) (CC-H and) (NNP Bernard)) (VP-H (VB-H fall)))");
				//return new TSNodeLabel("(S (NP-H (DT a) (NN-H lunch)) (JJP (JJ good) (CC-H but) (JJ expensive)))");
				//return new TSNodeLabel("(S (NP (NNS children)) (VP-H (VB laugh) (CC-H and) (VB sing)))");
				//return new TSNodeLabel("(SINV (`` ``) (S-TPC (NP-SBJ (DT The) (NN morbidity) (NN-H rate)) (VP-H (VBZ-H is) (NP-PRD (DT a) (JJ striking) (NN-H finding)) (PP-LOC (IN among) (NP-H (NP-H (DT-H those)) (PP (IN of) (NP-H (PRP-H us))) (SBAR (WHNP (WP-H who)) (S-H (VP-H (VBP-H study) (NP (JJ asbestos-related) (NNS-H diseases))))))))) (, ,) ('' '') (VP-H (VBD-H said)) (NP-SBJ (NNP Dr.) (NNP-H Talcott)) (. .))");
				//return new TSNodeLabel("(S (NP-SBJ (DT The) (NN-H indicator)) (VP-H (VP-H (VBD-H reached) (NP (DT a) (NN-H peak)) (PP-TMP (IN-H in) (NP (NNP-H January) (CD 1929)))) (CC and) (VP (ADVP-TMP (RB-H then)) (VBD-H fell) (ADVP-MNR (RB-H steadily)) (PP-TMP (PP (IN-H up) (PP (TO-H to))) (CC and) (PP (IN-H through)) (NP-H (DT the) (NN-H crash))))) (. .))");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
	    public TesniereTreeViewer() {
	        super();	        	        	                
	    }
	    
	    public void setInitialSentence() {
	        ArrayList<Box> treebank = new ArrayList<Box>();
	        try {
				treebank.add(Conversion.getTesniereStructure(initialSentence, HL));
			} catch (Exception e) {
				e.printStackTrace();
			}	
			loadTreebank(treebank);
	    }
	    
	    
		protected void loadTreebank(File treebankFile) {                		
			corpusFile = treebankFile;
			ArrayList<Box> treebank = null;
			String firstLineFile = FileUtil.getFirstLineInFile(treebankFile);
			if (firstLineFile.startsWith("<?xml")) treebank = Conversion.getTreebankFromXmlFile(treebankFile);
			else {
				try {
					treebank = Conversion.getTesniereTreebank(treebankFile, HL);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			loadTreebank(treebank);
		}
		
		@Override
		protected void setCheckBoxOptions() {
			optionCheckBoxs = new JCheckBox[]{
					new JCheckBox("Skewed Lines", false),
					new JCheckBox("Display Categories", false)
			};
			
		}
	    
	    protected void initializeTreePanel() {
	    	//Set up the drawing area.
	    	TesniereTreePanel tesniereTreePanel = new TesniereTreePanel();
	    	tesniereTreePanel.skewedLinesCheckBox = optionCheckBoxs[0];
	    	tesniereTreePanel.displayCategories = optionCheckBoxs[1];
	    	treePanel = tesniereTreePanel;
	        treePanel.setBackground(Color.white);
	        treePanel.setFocusable(false);	        
	    }
	    
	    public static void doBeforeClosing() {	    	
	    	Parameters.logPrintln("--- end of log");
        	Parameters.closeLogFile();
	    }
		
	    /**
	     * Create the GUI and show it.  For thread safety,
	     * this method should be invoked from the
	     * event-dispatching thread.
	     */
	    private static void createAndShowGUI() {
	        //Create and set up the window.
	        JFrame frame = new JFrame("Tesniere Tree Viewer");
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        
	        final TesniereTreeViewer treeViewer = new TesniereTreeViewer();	        
	        Dimension area = treeViewer.treePanel.area;
	        frame.setMinimumSize(new Dimension(area.width+5, area.height+5));			
	        treeViewer.setOpaque(true); //content panes must be opaque
	        frame.setContentPane(treeViewer);
	        
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
	        JMenuItem exportAllToXml = new JMenuItem("Export All to XML");
	        JMenuItem quit = new JMenuItem("Quit");
	        final JFileChooser fc = new JFileChooser();
	        
	        openTreebank.setAccelerator(KeyStroke.getKeyStroke('O', java.awt.event.InputEvent.ALT_MASK));
	        exportEps.setAccelerator(KeyStroke.getKeyStroke('E', java.awt.event.InputEvent.ALT_MASK));
	        exportPdf.setAccelerator(KeyStroke.getKeyStroke('P', java.awt.event.InputEvent.ALT_MASK));	   
	        exportAllToPdf.setAccelerator(KeyStroke.getKeyStroke('A', java.awt.event.InputEvent.ALT_MASK));
	        exportAllToXml.setAccelerator(KeyStroke.getKeyStroke('X', java.awt.event.InputEvent.ALT_MASK));
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
	        exportAllToXml.addActionListener(new ActionListener() {
	        	public void actionPerformed(ActionEvent e) {
	        		int returnVal = fc.showSaveDialog(treeViewer.treePanel);	                
	                if (returnVal == JFileChooser.APPROVE_OPTION) {
	                	File selectedFile = fc.getSelectedFile();
	                	selectedFile = FileUtil.changeExtension(selectedFile, "xml");
	                    ((TesniereTreePanel)treeViewer.treePanel).exportAllToXML(selectedFile);
	                }
	            }
	        });
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
	        file.add(exportAllToXml);
	        file.add(quit);
	        openTreebank.setMnemonic('O');	        
	        exportPdf.setMnemonic('P');
	        exportAllToPdf.setMnemonic('A');
	        exportAllToXml.setMnemonic('X');
	        quit.setMnemonic('Q');	        	        
	
	        menuBar.add(file);
	        frame.setJMenuBar(menuBar);
	        
	        //Display the window.
	        frame.pack();
	        frame.setVisible(true);
	    }
	
	    public static void main(String[] args) {
	        //Schedule a job for the event-dispatching thread:
	        //creating and showing this application's GUI.    	    		    	
	    	HL =  new HeadLabeler(new File("resources/fede.rules_04_08_10"));
	    	TesniereTreePanel.printOnCosoleCurrentTree = false;
	    	String usage = "USAGE: java [-Xmx100M] -jar TDS.jar [-log fileLog]";
	    	InputStream is = ClassLoader.getSystemResourceAsStream("fede.rules_04_08_10");
	        //HL = new HeadLabeler(is);
	    	File logFile = new File("Viewers/TDS/TDS_last.log");
	    	//File logFile = null;
	    	if (args.length>1) {
	    		if (args.length!=2) {
	    			System.err.println("Incorrect number of arguments.");
					System.err.println(usage);
					System.exit(-1);
	    		}
	    		String option = args[0]; 
	    		if (!option.equals("-log")) {	    			
	    			System.err.println("Not a valid option: " + option);
					System.err.println(usage);
					System.exit(-1);
	    		}
	    		logFile = new File(args[1]);
	    	}
	    	if (logFile!=null) Parameters.openLogFile(logFile);	    	
	        javax.swing.SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	                createAndShowGUI();
	            }
	        });	 
	    }
	
	}
	