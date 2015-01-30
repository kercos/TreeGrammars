package viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tesniere.Box;

public abstract class TreeViewer<T> extends JPanel implements ChangeListener, ActionListener {
	
	private static final long serialVersionUID = 1L;

	protected TreePanel<T> treePanel;
	protected File corpusFile;	
	
	protected JPanel controlPanel;
	protected JSpinner controlSentenceNumber;	
	protected JButton plus;
	protected JButton minus;
	protected JLabel sizeReport;	
	protected JLabel statusBar;
		
	protected JCheckBox[] optionCheckBoxs;
	
	
	public TreeViewer() {
		super(new BorderLayout());
		
		setCheckBoxOptions();
		
		buildComponents();		
		
		//Put the drawing area in a scroll pane.
        JScrollPane scroller = new JScrollPane(treePanel);
        scroller.setPreferredSize(new Dimension(800,600));

        //Lay out        
        statusBar = new JLabel("");
        add(controlPanel, BorderLayout.NORTH);
        add(scroller, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        
        setInitialSentence();
	}
	
	protected abstract void setCheckBoxOptions();
	
	protected abstract void setInitialSentence();
	
	protected abstract void initializeTreePanel();
	
	protected void loadTreebank(ArrayList<T> treebank) {
		int size = treebank.size();
		treePanel.loadTreebank(treebank);
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1,1,size,1);
    	controlSentenceNumber.setModel(spinnerModel);    	
    	sizeReport.setText("of: " + size);    	
		treePanel.init();
		statusBar.setText(treePanel.flatSentence);
	}
	
    protected void buildComponents() {
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
                
        if (optionCheckBoxs!=null) {
        	for(JCheckBox cb : optionCheckBoxs) {        		 
                cb.addActionListener(this);
                cb.setFocusable(false);                
                leftControlPanel.add(cb);                
        	}
        }        
        
        plus = new JButton("+");                
        minus = new JButton("-");
        plus.addActionListener(this);        
        minus.addActionListener(this);
        plus.setFocusable(false);
        minus.setFocusable(false);
        centerControlPanel.add(new JLabel("Zoom: "));
        centerControlPanel.add(plus);
        centerControlPanel.add(minus);        
        
        rightControlPanel.add(controlSentenceNumber);  
        sizeReport = new JLabel();        
        sizeReport.setFocusable(false);
        rightControlPanel.add(sizeReport); 
        
        initializeTreePanel();
    }
    
	public void stateChanged(ChangeEvent e) {
		Integer s = (Integer) controlSentenceNumber.getValue();
		int index = s-1;
		treePanel.goToSentence(index);		
		//statusBar.setText(treebankComments.get(index));
		treePanel.init(); 
		statusBar.setText(treePanel.flatSentence);
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		
		if (optionCheckBoxs!=null) {
        	for(JCheckBox cb : optionCheckBoxs) {
        		if (source==cb) {
        			treePanel.init();
        			controlSentenceNumber.requestFocus();
        			return;
        		}                
        	}
        }

		int fs;
		if (source==plus) {
			fs = treePanel.increaseFontSize();
		}
		else {
			fs = treePanel.decreaseFontSize();		
		}
		System.out.println("Font size: " + fs);
		
		treePanel.init();
		controlSentenceNumber.requestFocus();
	}
	
	
}
