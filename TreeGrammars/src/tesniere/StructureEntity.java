package tesniere;

import java.util.Arrays;

/**
 * @author chiara
 * 
 * A StructureEntity is composed of an array of Entities 
 * in which there is either a single FullWord or a sequence of at least one StructureEntity (at any position),
 * and any possible number of EmptyWords (possibly zero). 
 * - one FullWord
 * - a sequence of at least two Entities of which at least one StructureEntity 
 * (at any position) and zero or more EmptyWords 
 *
 */
public class StructureEntity extends Entity {
	
	Entity[] entityList;
	int[] headIndexes;
	boolean isCoordinative;
	
	public StructureEntity(Entity[] entityList) {
		this.entityList = entityList;		
	}
	
	public StructureEntity(Entity[] entityList, int[] headIndexes) {
		this.entityList = entityList;
		this.headIndexes = headIndexes;
	}
	
	public StructureEntity(Entity[] entityList, int[] headIndexes, boolean isCoordinative) {
		this.entityList = entityList;
		this.headIndexes = headIndexes;
		this.isCoordinative = isCoordinative;
	}
		
	public void setHeadIndexes(int[] headIndexes) {
		this.headIndexes = headIndexes;
	}
	
	public void makeCoordinative() {
		isCoordinative = true;
	}
	
	public boolean isCoordinative() {
		return isCoordinative;
	}
	
	public String toString() {
		return Arrays.toString(entityList);
	}
}
