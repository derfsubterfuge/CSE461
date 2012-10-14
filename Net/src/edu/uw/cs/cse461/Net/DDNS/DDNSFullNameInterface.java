package edu.uw.cs.cse461.Net.DDNS;

/**
 * Except for the root, a FullName is an absolute DDNS name ending with a '.'.
 * The name of the root is the empty string, "" (with no trailing period).
 * <p>
 * Example: FullName("a.b.c") is "a.b.c.".
 * <br>Example: FullName("a.b.c.") is "a.b.c.".
 * <br>Example: FullName(".a.b.c.") is "a.b.c.".
 * <br>Example: FullName(".") is "".
 *<p> 
 * A DDNSFullNameInterface can be used as a key in a map.
 * @author zahorjan
 *
 */
public interface DDNSFullNameInterface extends Comparable<DDNSFullNameInterface> {

	public DDNSFullNameInterface parent();
	public DDNSFullNameInterface nextAncestor(DDNSFullNameInterface descendant);

	public boolean isChildOf(DDNSFullNameInterface parent);
	public boolean isDescendantOf(DDNSFullName parent);
	public boolean equals(Object other);
	
	public int compareTo(DDNSFullNameInterface other);
	public int hashCode();
	
	public String toString();
}