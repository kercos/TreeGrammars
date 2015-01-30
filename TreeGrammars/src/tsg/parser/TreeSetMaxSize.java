package tsg.parser;
import java.util.*;

public class TreeSetMaxSize<T> extends TreeSet<T>{
	private static final long serialVersionUID = 0L;
	int maxSize;
	
	public TreeSetMaxSize(int maxSize) {		
		super();
		this.maxSize = maxSize;
	}

}
