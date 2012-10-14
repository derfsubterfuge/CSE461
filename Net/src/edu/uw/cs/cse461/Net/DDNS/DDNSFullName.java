package edu.uw.cs.cse461.Net.DDNS;

/**
 * Try to keep straight when we have a fully qualified name, and when we may not,
 * using the compiler's type checking system.
 * <p>
 * Except for the root, a FullName is an absolute name ending with a '.'.
 * The name of the root is the empty string, "".
 * <p>
 * Example: FullName("a.b.c") is "a.b.c.".
 * <br>Example: FullName("a.b.c.") is "a.b.c.".
 * <br>Example: FullName(".a.b.c.") is "a.b.c.".
 * <br>Example: FullName(".") is "".
 * @author zahorjan
 *
 */
public class DDNSFullName implements DDNSFullNameInterface {
	public static final char NAMESEP = '.';
	public static final DDNSFullName ROOTNAME = new DDNSFullName("");
	
	private String delegate;
	/**
	 * Deals with optional trailing '.', by applying one if needed.  The root name, "", never has a dot.
	 * All other names are of form "a.b.c.".
	 * @param name
	 * @return
	 */
	public DDNSFullName(String name) {
		delegate = name;
		if ( delegate == null ) delegate = "";
		else if ( delegate.isEmpty() || delegate.equals(".")) delegate = "";
		else {
			if ( delegate.startsWith(".") ) delegate = delegate.substring(1);
			if ( !delegate.endsWith(".") ) delegate = delegate + ".";
		}
	}
	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface#parent()
	 */
	@Override
	public DDNSFullNameInterface parent() {
		if ( delegate == null ) return null;
		int index = delegate.indexOf(NAMESEP);
		if ( index < 0 || index >= delegate.length()-1  ) return new DDNSFullName("");
		return new DDNSFullName(delegate.substring(index+1));
	}
	
	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface#isChildOf(edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface)
	 */
	@Override
	public boolean isChildOf(DDNSFullNameInterface parent) {
		if (delegate.length() < parent.toString().length()+2 ) return false;
		if ( !delegate.endsWith(parent.toString()) ) return false;
		String child = delegate.substring(0, delegate.length()-parent.toString().length());
		if ( !child.endsWith(".") || child.equals(".") ) return false;
		child = child.substring(0,child.length()-1);
		if ( child.indexOf('.') >= 0 ) return false; // it's an ancestor, but not a child
		return true;
	}
	
	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface#isDescendantOf(edu.uw.cs.cse461.Net.DDNS.DDNSFullName)
	 */
	@Override
	public boolean isDescendantOf(DDNSFullName parent) {
		return delegate.endsWith(parent.toString())
				&& ( delegate.length() == parent.delegate.length() || delegate.charAt( delegate.length() - parent.delegate.length() - 1) == '.');
	}
	
	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface#nextAncestor(edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface)
	 */
	@Override
	public DDNSFullName nextAncestor(DDNSFullNameInterface descendant) {
		int lastDelim = descendant.toString().lastIndexOf(NAMESEP, descendant.toString().length() - toString().length() - 2);
		return new DDNSFullName(descendant.toString().substring(lastDelim+1));
	}
	
	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		return delegate != null && delegate.equals(((DDNSFullNameInterface)other).toString());
	}
	
	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface#compareTo(edu.uw.cs.cse461.Net.DDNS.DDNSFullName)
	 */
	@Override
	public int compareTo(DDNSFullNameInterface other ) {
		if ( other == null ) return 1;
		return delegate.compareTo( other.toString() );
	}
	
	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface#hashCode()
	 */
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface#toString()
	 */
	@Override
	public String toString() {
		return delegate;
	}
}
