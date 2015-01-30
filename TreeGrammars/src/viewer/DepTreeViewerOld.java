package viewer;

import javax.swing.*;

import tdg.TDNode;
import tdg.corpora.DepCorpus;
import tdg.corpora.WsjD;
import util.file.FileUtil;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("serial")
public class DepTreeViewerOld extends JPanel implements MouseListener, KeyListener {

	private DepTreePanelOld treePanel;    			

    public DepTreeViewerOld(ArrayList<TDNode> treebank) {
        super(new BorderLayout());        

        //Set up the drawing area.
        treePanel = new DepTreePanelOld(treebank);
        treePanel.setBackground(Color.white);
        treePanel.setFocusable(true);
        treePanel.addMouseListener(this);
        treePanel.addKeyListener(this);

        //Put the drawing area in a scroll pane.
        JScrollPane scroller = new JScrollPane(treePanel);
        scroller.setPreferredSize(new Dimension(800,600));

        //Lay out this demo.
        //add(instructionPanel, BorderLayout.PAGE_START);
        add(scroller, BorderLayout.CENTER);
        treePanel.init();
    }


    //Handle mouse events.
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
        	int s = treePanel.nextSentence();        	
        	System.out.println("Next sentence: " + s);
        } else {
        	int s = treePanel.previousSentence();
        	System.out.println("Previous sentence: " + s);
        }        
        
        treePanel.init();
    }

    public void mouseClicked(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mousePressed(MouseEvent e){}

	public void keyPressed(KeyEvent e) {}

	public void keyReleased(KeyEvent e) {}

	public void keyTyped(KeyEvent e) {
		int id = e.getID();
		if (id == KeyEvent.KEY_TYPED) {
            char c = e.getKeyChar();
            switch(c) {			
			case '+': 
				int s = treePanel.increaseFontSize();
				System.out.println("Font size: " + s);
				treePanel.init();
				break;
			case '-':				
				s = treePanel.decreaseFontSize();
				System.out.println("Font size: " + s);
				treePanel.init();
				break;
			case 'n':
			case 'N':
				s = treePanel.nextSentence();        	
	        	System.out.println("Next sentence: " + s);
            	treePanel.init();
            	break;
            case 'p':
            case 'P':
            	s = treePanel.previousSentence();        	
            	System.out.println("Previous sentence: " + s);
            	treePanel.init();
            	break;
            case 's':
            case 'S':
            	File outputFile = new File("tmp/image1.eps");
            	treePanel.exportToEPS(outputFile);
            	break;
            }
        } else {
            int keyCode = e.getKeyCode();
        }		
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
        
        //Create and set up the content pane.
        File trainCorpus = new File(WsjD.WsjCOLLINS99Arg_ulab + "wsj-00.ulab");
		ArrayList<TDNode> treebank = 
			DepCorpus.readTreebankFromFileMST(trainCorpus, 100, false, false);
        JComponent newContentPane = new DepTreeViewerOld(treebank);
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
        //newContentPane.setPreferredSize(new Dimension(800,600));
        
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
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
