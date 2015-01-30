package viewer;

import javax.swing.*;

import tsg.TSNode;
import tsg.corpora.ConstCorpus;
import tsg.corpora.Wsj;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

@SuppressWarnings("serial")
public class ConstTreeViewerOld extends JPanel implements MouseListener, KeyListener {
	
    private ConstTreePanel treePanel;    			

    public ConstTreeViewerOld(ArrayList<TSNode> treebank) {
        super(new BorderLayout());        

        //Set up the drawing area.
        treePanel = new ConstTreePanel(treebank);
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
        JFrame frame = new JFrame("Constituency Tree Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        
        
        //Create and set up the content pane.
        File trainCorpus = new File(Wsj.WsjOriginalReadable + "wsj-00.mrg");
		ArrayList<TSNode> treebank = new ConstCorpus(trainCorpus, "Wsj-00").treeBank;
        JComponent newContentPane = new ConstTreeViewerOld(treebank);
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
