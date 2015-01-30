package tsg.parser;
import java.util.*;
import java.io.*;

import settings.Parameters;

public class DiskCacheManager extends CacheManager{	
	Hashtable<ArrayList<Integer>,File> fileIndex;
	int fileCounter;	
	static final String workingPath = "/scratch/fsangati/workingPath/";
	
	
	public DiskCacheManager(TestSet test) {
		super(test);
		fileIndex = new Hashtable<ArrayList<Integer>,File>();
		fileCounter = 0;
		
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
		if (this.fileIndex.containsKey(subString)) return 1;
		return -1;
	}
	
	/**
	 * Store a specific cell in the cache
	 * @param cellToStore
	 */
	public void writeToCache(Cell cellToStore) {
		File cellFile = new File(workingPath + "cell" + "_" + cellToStore.yield.size() + "_" + (++fileCounter));
		cellToStore.toBinaryFile(cellFile);
		fileIndex.put(cellToStore.yield, cellFile);
	}
	
	/**
	 * Read a specific cell identified by the input yield form the cache
	 * @param yield
	 * @return
	 */
	public Cell readFromCache(ArrayList<Integer> yield) {
		File requestedFile = this.fileIndex.get(yield);
		return Cell.fromBinaryFile(requestedFile);
	}

}
