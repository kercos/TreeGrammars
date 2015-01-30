package tsg.parser;
import java.util.*;
import java.io.*;

public abstract class CacheManager {	
	TestSet test;
	
	
	public CacheManager(TestSet test) {
		this.test = test;		
	}
	
	/**
	 * This method tells whether a specific subString Cell is present in the cache (1)
	 * or is nor present in the cache (-1, 0) and needs to be present (-1) or doesn't
	 * need to be present (0);
	 * @param subString
	 * @return
	 */
	public int inquire(ArrayList<Integer> subString) {
		return 0;
	}
	
	/**
	 * Store a specific cell in the cache
	 * @param cellToStore
	 */
	public void writeToCache(Cell cellToStore) {
	}
	
	/**
	 * Read a specific cell identified by the input yield form the cache
	 * @param yield
	 * @return
	 */
	public Cell readFromCache(ArrayList<Integer> yield) {
		return null;
	}

}
