package viewer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tdg.corpora.DepCorpus;
import tdg.corpora.MstSentenceUlabAdvanced;
import tsg.TSNodeLabel;
import tsg.TSNodeLabel;
import tsg.corpora.ConstCorpus;
import util.file.FileUtil;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

@SuppressWarnings("serial")
public class ConstTreeViewerInput extends JPanel implements ChangeListener, ActionListener {

	private ConstTreePanel treePanel;    
	private JPanel controlPanel;
	private JSpinner controlSentenceNumber;
	private JCheckBox straightLinesCheckBox, showHeadsCheckBox;
	private JButton plus, minus, loadButton;
	private JLabel sizeReport;	
	private JLabel statusBar;
	private JTextArea inputTextArea;
	private JScrollPane textAreaScollPane;
	private ArrayList<String> treebankComments;
	static String corpusString;		
	
	JCheckBox[] optionCheckBoxs = new JCheckBox[]{
			new JCheckBox("Skewed Lines", false),
			new JCheckBox("Show Heads", false)
	};
	
	private static TSNodeLabel initialSentence = initialSentence();    

	public static TSNodeLabel initialSentence() {
		try {
			return new TSNodeLabel("(S (V Input) (NP (D a) (N treebank)))");
			//return new TSNodeLabel("(S (NP-SBJ (NNP-H Japan)) (VP-H (CONJP (RB-H not) (RB only)) (VP-H (VBZ-H outstrips) (NP (DT the) (NNP-H U.S.)) (PP-LOC (IN-H in) (NP (NN investment) (NNS-H flows)))) (CONJP (CC-H but) (RB also)) (VP (VBZ-H outranks) (NP (PRP-H it)) (PP-LOC (IN-H in) (NP (NP-H (NN-H trade)) (PP-CLR (IN-H with) (NP (JJS most) (JJP (JJ Southeast) (JJ-H Asian)) (NNS-H countries)))))))  (. .))");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
    public ConstTreeViewerInput() {
        super(new BorderLayout());                  
        ArrayList<TSNodeLabel> treebank = new ArrayList<TSNodeLabel>();
        treebankComments = new ArrayList<String>();
        treebankComments.add("load a corpus");
        treebank.add(initialSentence);        
                       
        buildComponents();        
        
        //Put the drawing area in a scroll pane.
        JScrollPane scroller = new JScrollPane(treePanel);
        scroller.setPreferredSize(new Dimension(800,600));

        //Lay out        
        statusBar = new JLabel("Input a treebank");
        add(controlPanel, BorderLayout.NORTH);
        add(scroller, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        
        loadTreebank(treebank);
    }
    
    
	protected void loadTreebank(String treebankString) {                		
		corpusString = treebankString;
		ArrayList<TSNodeLabel> treebank = new ArrayList<TSNodeLabel>();
		treebankComments = new ArrayList<String>();
		try {
			TSNodeLabel.getTreebankCommentFromString(treebankString, 
					treebank, treebankComments);
		} catch (Exception e) {
			e.printStackTrace();
		}
		loadTreebank(treebank);
	}
	
	private void loadTreebank(ArrayList<TSNodeLabel> treebank) {
		int size = treebank.size();
		treePanel.loadTreebank(treebank);
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1,1,size,1);
    	controlSentenceNumber.setModel(spinnerModel);    	
    	sizeReport.setText("of: " + size);
    	statusBar.setText(treebankComments.get(0));
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
        JPanel bottomControlPanel = new JPanel();
        controlPanel.add(leftControlPanel, BorderLayout.WEST);
        controlPanel.add(centerControlPanel, BorderLayout.CENTER);
        controlPanel.add(rightControlPanel, BorderLayout.EAST);
        controlPanel.add(bottomControlPanel, BorderLayout.SOUTH);
        
        //left control panel
        straightLinesCheckBox = new JCheckBox("Skewed Lines"); 
        straightLinesCheckBox.addActionListener(this);
        straightLinesCheckBox.setFocusable(false);
        leftControlPanel.add(straightLinesCheckBox);
        
        showHeadsCheckBox = new JCheckBox("Show Heads:");
        showHeadsCheckBox.addActionListener(this);
        showHeadsCheckBox.setFocusable(false);
        leftControlPanel.add(showHeadsCheckBox);
                
        //ceter control panel
        plus = new JButton("+");                
        minus = new JButton("-");
        plus.addActionListener(this);        
        minus.addActionListener(this);
        plus.setFocusable(false);
        minus.setFocusable(false);
        centerControlPanel.add(new JLabel("zoom: "));
        centerControlPanel.add(plus);
        centerControlPanel.add(minus);        
        
        //right control panel
        rightControlPanel.add(controlSentenceNumber);  
        sizeReport = new JLabel();        
        sizeReport.setFocusable(false);
        rightControlPanel.add(sizeReport);
        
        //south control panel
        inputTextArea = new JTextArea("(S (V Input) (NP (D a) (N treebank)))", 3, 80);
        inputTextArea.setLineWrap(true);
        textAreaScollPane = new JScrollPane(inputTextArea);
        //inputTextArea.setFocusable(false);
        loadButton = new JButton("Load");
        loadButton.addActionListener(this);
        loadButton.setFocusable(false);
        textAreaScollPane.setMaximumSize(new Dimension(500,100));
        bottomControlPanel.add(textAreaScollPane);
        bottomControlPanel.add(loadButton);
    	
    	//Set up the drawing area.
        treePanel = new ConstTreePanel();
        treePanel.setBackground(Color.white);
        treePanel.setFocusable(false);
        
        treePanel.skewedLinesCheckBox = optionCheckBoxs[0];
        treePanel.showHeads = optionCheckBoxs[1];
        
        
    }
	
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Constituency Tree Viewer Input");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        
        
		final ConstTreeViewerInput treeViewer = new ConstTreeViewerInput();
        treeViewer.setOpaque(true); //content panes must be opaque
        frame.setContentPane(treeViewer);
        //newContentPane.setPreferredSize(new Dimension(800,600));
        
      //Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu commands = new JMenu("Commands");        
        JMenuItem exportEps = new JMenuItem("Export to EPS");
        JMenuItem exportPdf = new JMenuItem("Export to PDF");
        final JFileChooser fc = new JFileChooser();
        
        exportEps.setAccelerator(KeyStroke.getKeyStroke('E', java.awt.event.InputEvent.ALT_MASK));
        exportPdf.setAccelerator(KeyStroke.getKeyStroke('P', java.awt.event.InputEvent.ALT_MASK));
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
            
        commands.add(exportEps);
        commands.add(exportPdf);
        commands.setMnemonic('F');
        commands.setMnemonic('P');

        JMenuItem quit = new JMenuItem("Quit");
        //quit.setMnemonic('Q');
        commands.add(quit);
        quit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        });


        JMenu window = new JMenu("Window");

        menuBar.add(commands);
        menuBar.add(window);
        frame.setJMenuBar(menuBar);
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }


	public void stateChanged(ChangeEvent e) {
		Integer s = (Integer) controlSentenceNumber.getValue();
		int index = s-1;
		treePanel.goToSentence(index);		
		statusBar.setText(treebankComments.get(index));
		treePanel.init(); 
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source==optionCheckBoxs[0] || source==optionCheckBoxs[1]) {
			treePanel.init();
			controlSentenceNumber.requestFocus();
			return;
		}		
		else if (source==loadButton) {
			loadTreebank(inputTextArea.getText());
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
