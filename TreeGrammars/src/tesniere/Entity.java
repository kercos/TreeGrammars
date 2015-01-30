package tesniere;

/**
 * @author chiara
 * 
 * An entity is a any kind of simple or complex nodo in Tesniere.
 *
 */
public abstract class Entity implements Comparable<Entity> {
	
	public abstract int startPosition();
	
	public abstract int endPosition();
	
	public int compareTo(Entity anotherEntity) {		
		int thisStart = this.startPosition();
		int otherStart = anotherEntity.startPosition();		
		if (thisStart<otherStart) return -1;
		if (thisStart>otherStart) return 1;
		int thisEnd = this.endPosition();
		int otherEnd = anotherEntity.endPosition();
		return (thisEnd<otherEnd ? -1 : (thisEnd==otherEnd) ? 0 : 1);
	}
	
	public static int compare(Entity e1, Entity e2) {
		return e1.compareTo(e2);
	}
	
	public static int leftOverlapsRight(Entity e1, Entity e2) {
		int e1l = e1.startPosition();
		int e1r = e1.endPosition();
		int e2l = e2.startPosition();
		int e2r = e2.endPosition();
		if (e1l<e2l && e1r<e2r) return -1;
		if (e1l>e2l && e1r>e2r) return 1;
		return 0;
	}
		
	public boolean isBox() {
		return this instanceof Box;
	}
	
	public boolean isWord() {
		return this instanceof Word;
	}
	
	public boolean isJunctionBox() {
		return this instanceof BoxJunction;
	}
	
	public boolean isStandardBox() {
		return this instanceof BoxStandard;
	}
	
	public boolean isFunctionalWord() {
		return this instanceof FunctionalWord;
	}
	
	public boolean isContentWord() {
		return this instanceof ContentWord;
	}
	
	public boolean isPunctWord() {
		if (! (this instanceof FunctionalWord)) return false;
		return ((FunctionalWord)this).isPunctuation();
	}	
	
	
}
