package tsg.parser;
import java.util.*;

import settings.Parameters;

public class RamCacheManager extends CacheManager{	
	Hashtable<ArrayList<Integer>,Cell> cellIndex;
	
	
	public RamCacheManager(TestSet test) {
		super(test);
		cellIndex = new Hashtable<ArrayList<Integer>,Cell>();		
	}
	
	/**
	 * This method tells whether a specific subString Cell is present in the cache (1)
	 * or is nor present in the cache (-1, 0) and needs to be present (-1) or doesn't
	 * need to be present (0);
	 * @param subString
	 * @return
	 */
	public int inquire(ArrayList<Integer> subString) {
		if (!Parameters.cachingActive || subString.size() < TestSet.minSubStringLengthToCache) return 0;
		if (!test.containsSubString(subString)) return 0;
		if (this.cellIndex.containsKey(subString)) return 1;
		return -1;
	}
	
	/**
	 * Store a specific cell in the cache
	 * @param cellToStore
	 */
	public void writeToCache(Cell cellToStore) {
		cellIndex.put(cellToStore.yield, cellToStore);
	}
	
	/**
	 * Read a specific cell identified by the input yield form the cache
	 * @param yield
	 * @return
	 */
	public Cell readFromCache(ArrayList<Integer> yield) {
		return cellIndex.get(yield);
	}

}
