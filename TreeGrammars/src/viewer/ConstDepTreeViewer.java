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
import tsg.TSNodeLabel;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.ParseException;
import java.util.*;

@SuppressWarnings("serial")
public class ConstDepTreeViewer extends JPanel 
	implements ChangeListener, ActionListener {
	
	private JPanel controlPanel;
	private JSpinner controlSentenceNumber;
	private JButton plus, minus;
    private ConstTreePanel constTreePanel;
    private DepTreePanel depTreePanel;    			
    private JSplitPane splitPane;
    private JScrollPane constScroller, depScroller;
    private ArrayList<TSNodeLabel> constTreebank;
    private ArrayList<MstSentenceUlabAdvanced> depTreebank;
    private JCheckBox straightLinesCheckBox, showHeadsCheckBox;
    private int size;	

    public ConstDepTreeViewer(ArrayList<TSNodeLabel> constTreebank, 
    		ArrayList<MstSentenceUlabAdvanced> depTreebank) {
    	
        super(new BorderLayout());        
        
        size = constTreebank.size();
        this.constTreebank = constTreebank;
        this.depTreebank = depTreebank;
        buildComponents();
        
        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        
        constTreePanel.init();
        depTreePanel.init();
    }
    
    private void buildComponents() {
    	//Set up control panel
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
                
        straightLinesCheckBox = new JCheckBox("Straight Lines:"); 
        straightLinesCheckBox.addActionListener(this);
        straightLinesCheckBox.setFocusable(false);
        leftControlPanel.add(straightLinesCheckBox);
        
        showHeadsCheckBox = new JCheckBox("Show heads:");
        showHeadsCheckBox.addActionListener(this);
        showHeadsCheckBox.setFocusable(false);
        leftControlPanel.add(showHeadsCheckBox);
                
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
        rightControlPanel.add(new JLabel("of " + size));   
        
				
        //Set up the drawing area.
        constTreePanel = new ConstTreePanel(constTreebank);        
        constTreePanel.setBackground(Color.white);        
        constTreePanel.setFocusable(false);        

        
        depTreePanel = new DepTreePanel(depTreebank);
        depTreePanel.setBackground(Color.white);
        depTreePanel.setFocusable(false);

        

        //Put the drawing area in a scroll pane.
        constScroller = new JScrollPane(constTreePanel);
        constScroller.setFocusable(false);
        
        depScroller = new JScrollPane(depTreePanel);
        depScroller.setFocusable(false);

        
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        		constScroller, depScroller);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(300);
        splitPane.setFocusable(false);
        splitPane.setPreferredSize(new Dimension(800, 600));

    }

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source==straightLinesCheckBox) {
			constTreePanel.straightLines = !constTreePanel.straightLines;  
			depTreePanel.straightLines = !depTreePanel.straightLines;
		}
		else if (source==showHeadsCheckBox) {
			constTreePanel.showHeads = !constTreePanel.showHeads;
		}
		else {
			int fs;
			if (source==plus) {
				fs = constTreePanel.increaseFontSize();
				depTreePanel.increaseFontSize();
			}
			else {
				fs = constTreePanel.decreaseFontSize();
				depTreePanel.decreaseFontSize();		
			}
			System.out.println("Font size: " + fs);
		}		
		constTreePanel.init();
		depTreePanel.init();
		controlSentenceNumber.requestFocus();
	}

	public void stateChanged(ChangeEvent e) {	
		Integer s = (Integer) controlSentenceNumber.getValue();
		constTreePanel.goToSentence(s-1);
		depTreePanel.goToSentence(s-1);		
		constTreePanel.init();
    	depTreePanel.init(); 
	}
	
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     * @throws Exception 
     */
    private static void createAndShowGUI() throws Exception {
        //Create and set up the window.
        JFrame frame = new JFrame("ConstDep Tree Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        
        
        //Create and set up the content pane.
        //File constCorpusFile = new File("Wsj/wsj-00_short.mrg");
        //File constCorpusFile = new File("Wsj/wsj-00_collins97.mrg");
        //File constCorpusFile = new File("Wsj/wsj-00_traces.mrg");
        File constCorpusFile = new File("Wsj/wsj-00.mrg");
        //File constCorpusFile = new File("/scratch/fsangati/CORPUS/TUT/2.1/TUTinPENN-rel2_1newspaper.penn");

        
		ArrayList<TSNodeLabel> constTreebank = TSNodeLabel.getTreebank(constCorpusFile);
		
		File depCorpusFile = new File("Wsj/wsj-00.ulab");
		//File depCorpusFile = new File("Wsj/wsj-00_collins97.ulab");		
		//File depCorpusFile = new File("/scratch/fsangati/CORPUS/TUT/2.1/TUT-rel2_1newspaper.conll");

		boolean isConnl = DepCorpus.isConnlFormat(depCorpusFile);	
		ArrayList<MstSentenceUlabAdvanced> depTreebank = 
			(isConnl) ?
			MstSentenceUlabAdvanced.getTreebankAdvancedFromConnl(depCorpusFile,"UTF-8") :		
			MstSentenceUlabAdvanced.getTreebankAdvancedFromMst(depCorpusFile,"UTF-8");
		
        JComponent newContentPane = new ConstDepTreeViewer(constTreebank, depTreebank);
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
        
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
	
    public static void main(String[] args)  {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
					createAndShowGUI();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });
    }

}
