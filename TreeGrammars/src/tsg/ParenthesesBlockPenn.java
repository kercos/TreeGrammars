package tsg;

import java.util.Vector;


public class ParenthesesBlockPenn {
	
    @SuppressWarnings("serial")
	public static class WrongParenthesesBlockException extends Exception {

		public WrongParenthesesBlockException(String message) {
			super(message);
		}

	}

	private static final char L_PAREN    = '(';
    private static final char R_PAREN    = ')';    

	public String label;
	public Vector<ParenthesesBlockPenn> subBlocks;	
	int index;
    	
	public static ParenthesesBlockPenn getParenthesesBlocks(String s) throws WrongParenthesesBlockException {
		s = s.trim();
		if (s.charAt(0)!='(') {
			throw new WrongParenthesesBlockException(
			"Expression should start with '('");
		}
		return new ParenthesesBlockPenn(s.toCharArray(), 1);
	}
	
	public ParenthesesBlockPenn() {
		
	}
	
	public ParenthesesBlockPenn(char[] s, int i) throws WrongParenthesesBlockException {
		subBlocks = new Vector<ParenthesesBlockPenn>();
		this.index = i;
		do {
			char c = s[index];
			switch (c) {
			case ' ':
				index++;				
				continue;
			case L_PAREN:
				addNewBlock(s);				
				break;
			case R_PAREN:				
				if (subBlocks.isEmpty()) subBlocks = null;
				return;
			default:
				if (label==null) {
					acquireLabel(s,c); //first label after open parenthesis
				}
				else {
					acquireTerminal(s,c); //following labels
				}
			}
		} while(true);
	}
	
	private void addNewBlock(char[] s) throws WrongParenthesesBlockException {
		ParenthesesBlockPenn SB = new ParenthesesBlockPenn(s, ++index); 
		this.subBlocks.add(SB);
		this.index = SB.index+1;
	}
	
	private void acquireLabel(char[] s, char c) {
		label = "";
		do {
			label += c;
			c = s[++index];
		} while(c!=' ' && c!=L_PAREN && c!=R_PAREN);
	}
	
	private void acquireTerminal(char[] s, char c) {
		ParenthesesBlockPenn term = new ParenthesesBlockPenn();
		term.label="";
		do {
			term.label += c;
			c = s[++index];
		} while(c!=' ' && c!=L_PAREN && c!=R_PAREN);
		this.subBlocks.add(term);
	}
	
	
    /*public ParenthesesBlockPenn(String s) throws Exception {
        Stack<ParenthesesBlockPenn> stack = new Stack<ParenthesesBlockPenn>();
        ParenthesesBlockPenn currentBlock = new ParenthesesBlockPenn();
        stack.push(currentBlock);
        for (i = 1; i < s.length(); i++) {
        	char c = s.charAt(i);
        	if (c==' ') continue;
            if (c == L_PAREN)   {            	            	
            	ParenthesesBlockPenn newBlock = new ParenthesesBlockPenn();
            	currentBlock.subBlocks.add(newBlock);
            	stack.push(newBlock);           
            	currentBlock = newBlock;
            }
            else if (c == R_PAREN) {      
            	currentBlock = stack.pop();
            	currentBlock.label = currentBlock.label.trim();   
            	if (currentBlock.subBlocks.isEmpty()) {
            		String[] labelSplit = currentBlock.label.split("\\s+");
            		/*if (labelSplit.length!=2) {
            			throw new WongParenthesesBlockException(
            					"Final Blocks should have 2 words separated by string: " 
            					+ currentBlock.label);
            		}*/
    /*        		currentBlock.label = labelSplit[0];
            		for(int j=1; j<labelSplit.length; j++) {            			
            			ParenthesesBlockPenn terminalBlock = new ParenthesesBlockPenn(null, labelSplit[j]);            			
            			currentBlock.subBlocks.add(terminalBlock);
            		}        			        			
            	}
            	currentBlock.subBlocks.trimToSize();                	    
                if (!stack.isEmpty()) currentBlock = stack.peek();	            	
            }
            else {
            	if (currentBlock.subBlocks.isEmpty()) {
            		currentBlock.label += c;
            	}
            	else {
            		ParenthesesBlockPenn subBlock = new ParenthesesBlockPenn();
            		do {
            			subBlock.label += c;
            			i++;
            			if (i==s.length()) {
            				throw new WongParenthesesBlockException(
                					"Missing  at least one closing bracket.");
            			}
            			c = s.charAt(i);
            		} while(c!=' ' && c!=L_PAREN && c!=R_PAREN);
            		i--; //read parenthesis if no space was there
            	}
            }
        }
        return currentBlock;
    }*/
    
    
    public boolean isTerminal() {
    	return subBlocks==null;
    }
    
    public String toString() {
    	if (subBlocks==null) return label;
    	StringBuilder result = new StringBuilder(L_PAREN + label + " ");
		int size = subBlocks.size();
		int i=0;
    	for(ParenthesesBlockPenn p : subBlocks) {
    		result.append(p.toString());
    		if (++i!=size) result.append(" ");
    	}
    	result.append(R_PAREN);
    	return result.toString();
    }

    public static void main(String[] args) throws Exception {
    	//String p = "(S (NP-SBJ (NP (NNP Pierre) (NNP Vinken) ) (, ,) (ADJP (NP (CD 61) (NNS years) ) (JJ old) ) (, ,) ) (VP (MD will) (VP (VB join) (NP (DT the) (NN board) ) (PP-CLR (IN as) (NP (DT a) (JJ nonexecutive) (NN director) )) (NP-TMP (NNP Nov.) (CD 29) ))) (. .) )";
    	String p = "(NP (NNP Pierre) (NNP ) )";
    	//String p = "(a (b (c (d e) ) ) ) )";
    	ParenthesesBlockPenn pb = getParenthesesBlocks(p);
    	System.out.println(p);
    	System.out.println(pb);
    	System.out.println(pb.toString().equals(p));
    }

}





