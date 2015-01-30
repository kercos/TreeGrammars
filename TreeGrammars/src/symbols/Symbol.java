package symbols;

import java.io.Serializable;

public abstract class Symbol implements Serializable {
	
	private static final long serialVersionUID = 0L;

	int id;
	
	public abstract String toString();
	
	public abstract Object getOriginalObject();
}
