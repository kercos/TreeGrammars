package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tdg.DSConnl;
import tdg.corpora.DepCorpus;
import tdg.corpora.MstSentenceUlabAdvanced;
import util.file.FileUtil;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

@SuppressWarnings("serial")
public class DepTreeViewerLabeled extends JPanel implements ChangeListener, ActionListener {

	private DepTreePanelLabeled treePanel;    
	private JPanel controlPanel;
	private JSpinner controlSentenceNumber;
	private JCheckBox straightLinesCheckBox;
	private JButton plus;
	private JButton minus;
	private JLabel sizeReport;
	static File corpusFile;
	
	private static DSConnl initialSentence = 
		new DSConnl(
    		new String[]{"Load", "a", "corpus"},
    		new String[]{"V","D","N"},
    		new int[]{0,3,1}
	    	//new String[]{"The", "cat", "saw", "the", "hungry", "dog", "EOS"},
	    	//new String[]{"D","N","V","D","J","N",""},
	    	//new int[]{2,3,7,6,6,3,0}
			//new String[]{"Obama", "won", "the", "presidential", "election","EOS"},
		    //new String[]{"N","V","D","J","N",""},
		    //new int[]{2,6,5,5,2,0}
				
		);    

    public DepTreeViewerLabeled() {
        super(new BorderLayout());                  
        ArrayList<DSConnl> treebank = new ArrayList<DSConnl>();
        treebank.add(initialSentence);        
                       
        buildComponents();        
        
        //Put the drawing area in a scroll pane.
        JScrollPane scroller = new JScrollPane(treePanel);
        scroller.setPreferredSize(new Dimension(800,600));

        //Lay out        
        add(controlPanel, BorderLayout.NORTH);
        add(scroller, BorderLayout.CENTER);
        
        loadTreebank(treebank);
    }
    
    
	protected void loadTreebank(File treebankFile) {                		
		corpusFile = treebankFile;
		ArrayList<DSConnl> treebank = DSConnl.getConnlTreeBank(treebankFile);
		loadTreebank(treebank);
	}
	
	private void loadTreebank(ArrayList<DSConnl> treebank) {
		int size = treebank.size();
		treePanel.loadTreebank(treebank);
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1,1,size,1);
    	controlSentenceNumber.setModel(spinnerModel);    	
    	sizeReport.setText("of: " + size);
		treePanel.init();
	}
    
    private void buildComponents() {
    	//Set up control panel  
        controlSentenceNumber = new JSpinner();
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
                
        straightLinesCheckBox = new JCheckBox("Skewed Lines"); 
        straightLinesCheckBox.addActionListener(this);
        straightLinesCheckBox.setFocusable(false);
        leftControlPanel.add(straightLinesCheckBox);
                
        plus = new JButton("+");                
        minus = new JButton("-");
        plus.addActionListener(this);        
        minus.addActionListener(this);
        plus.setFocusable(false);
        minus.setFocusable(false);
        centerControlPanel.add(new JLabel("zoom: "));
        centerControlPanel.add(plus);
        centerControlPanel.add(minus);        
        
        rightControlPanel.add(controlSentenceNumber);  
        sizeReport = new JLabel();        
        sizeReport.setFocusable(false);
        rightControlPanel.add(sizeReport); 
    	
    	//Set up the drawing area.
        treePanel = new DepTreePanelLabeled();
        treePanel.setBackground(Color.white);
        treePanel.setFocusable(false);
        
        
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
        
		final DepTreeViewerLabeled treeViewer = new DepTreeViewerLabeled();
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
                selectedFile = FileUtil.changeExtension(selectedFile, "eps");
                if (returnVal == JFileChooser.APPROVE_OPTION)                    
                    treeViewer.treePanel.exportToEPS(selectedFile);                    
            }
        });
        exportPdf.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		int returnVal = fc.showSaveDialog(treeViewer.treePanel);
                File selectedFile = fc.getSelectedFile();
                selectedFile = FileUtil.changeExtension(selectedFile, "pdf");
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
                selectedFile = FileUtil.changeExtension(selectedFile, "pdf");
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
    }


	public void stateChanged(ChangeEvent e) {
		Integer s = (Integer) controlSentenceNumber.getValue();
		treePanel.goToSentence(s-1);		
		treePanel.init(); 
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source==straightLinesCheckBox) {
			treePanel.straightLines = !treePanel.straightLines;  
		}
		else {
			int fs;
			if (source==plus) {
				fs = treePanel.increaseFontSize();
			}
			else {
				fs = treePanel.decreaseFontSize();		
			}
			System.out.println("Font size: " + fs);
		}		
		treePanel.init();
		controlSentenceNumber.requestFocus();
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
