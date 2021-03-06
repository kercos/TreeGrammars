package util;

import java.util.Stack;
import java.util.Vector;


public class ParenthesesBlock {
    private static final char L_PAREN    = '(';
    private static final char R_PAREN    = ')';    
    
	int startIndex;
	int endIndex;
	String label;
	Vector<ParenthesesBlock> subBlocks;
    
	public ParenthesesBlock(int startIndex) {
		this.startIndex = startIndex;
		subBlocks = new Vector<ParenthesesBlock>();
		label = "";
	}
	
    public static ParenthesesBlock getParenthesesBlocks(String s) {
        Stack<ParenthesesBlock> stack = new Stack<ParenthesesBlock>();
        ParenthesesBlock currentBlock = new ParenthesesBlock(0);
        stack.push(currentBlock);
        for (int i = 1; i < s.length(); i++) {
        	char c = s.charAt(i);
            if (c == L_PAREN)   {            	            	
            	ParenthesesBlock newBlock = new ParenthesesBlock(i);
            	currentBlock.subBlocks.add(newBlock);
            	stack.push(newBlock);           
            	currentBlock = newBlock;
            }
            else if (c == R_PAREN) {      
            	currentBlock = stack.pop();
            	currentBlock.endIndex = i;
            	currentBlock.subBlocks.trimToSize();
            	currentBlock.label = currentBlock.label.trim();       
            	if (!stack.isEmpty()) currentBlock = stack.peek();
            }
            else {
            	if (currentBlock.subBlocks.isEmpty()){
            		currentBlock.label += c;
            	}
            }
        }
        return currentBlock;
    }
    
    public String toString() {
    	String result = L_PAREN + label;
    	if (!subBlocks.isEmpty()) {
    		result += " ";
	    	for(ParenthesesBlock p : subBlocks) {
	    		result += p.toString() + " ";
	    	}
    	}
    	result += R_PAREN;
    	return result;
    }

    public static void main(String[] args) {
    	String p = "(<T S[dcl] 0 2> (<T S[dcl] 1 2> (<T NP 0 1> (<T N 1 2> (<L N/N NNP NNP Mr. N_142/N_142>) (<L N NNP NNP Vinken N>) ) ) (<T S[dcl]\\NP 0 2> (<L (S[dcl]\\NP)/NP VBZ VBZ is (S[dcl]\\NP_87)/NP_88>) (<T NP 0 2> (<T NP 0 1> (<L N NN NN chairman N>) ) (<T NP\\NP 0 2> (<L (NP\\NP)/NP IN IN of (NP_99\\NP_99)/NP_100>) (<T NP 0 2> (<T NP 0 1> (<T N 1 2> (<L N/N NNP NNP Elsevier N_109/N_109>) (<L N NNP NNP N.V. N>) ) ) (<T NP[conj] 1 2> (<L , , , , ,>) (<T NP 1 2> (<L NP[nb]/N DT DT the NP[nb]_131/N_131>) (<T N 1 2> (<L N/N NNP NNP Dutch N_126/N_126>) (<T N 1 2> (<L N/N VBG VBG publishing N_119/N_119>) (<L N NN NN group N>) ) ) ) ) ) ) ) ) ) (<L . . . . .>) ) ";
    	//String p = "(a (b (c (d (e) (f) ) ) ) )";
    	ParenthesesBlock pb = getParenthesesBlocks(p);
    	System.out.println(p);
    	System.out.println(pb);
    	System.out.println(pb.toString().equals(p));
    }

}





