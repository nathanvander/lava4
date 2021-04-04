package lava4;

/**
* A Cell is an amorphous primitive thing that can be a number, a reference or a String.
* It is just an array of chars with no formal type.  Even though this in untyped, the length
* indicates the type.  A length of 1 is a reference, usually an object or array.  A length of 2
* is an int. A length of 3..64 is a string.
*
* There is an ambiguity here between small ints and refs.  A single char between 0 and 32767 can
* represent either an int or a ref.  Single chars between 32768 and 65535 are not allowed to prevent
* ambiguity.  A negative int will always be 2 chars, for example, -1 is 65535,65535
*/
public class Cell {
	public static long L32=4294967296L;
	public static long L31=2147483648L;
	public static Cell NIL = new Cell((char)0);		//use for null

	//this is not final because we want to append to it
	private char[] dna;
	private int hashCode;	//for strings, to make it easy to compare. I need a better solution

	//----------------------------
	//as a number. Negative numbers are in 2's complement
	public Cell(int i) {
		long lo = (long)i;
		if (lo<0) {lo=lo+L32;}
		dna = new char[] { (char)(lo / 65536),(char)(lo % 65536) };
	}

	public void increment() {
		int i =(int)dna[1];
		if (i>65534) {
			i++;
			dna[1]=(char)(i-65536);
			dna[0]++;
		} else {
			dna[1]++;
		}
	}

	public void decrement() {
		int i =(int)dna[1];
		if (i<1) {
			i--;
			dna[1]=(char)(i+65536);
			dna[0]--;
		} else {
			dna[1]--;
		}
	}

	//if you want to subtract, then change the sign
	public void add(short s) {
		int i =(int)dna[1];
		i=i+s;
		if (i>65535) {
			dna[1]=(char)(i-65536);
			dna[0]++;
		} else if (i<0) {
			dna[1]=(char)(i+65536);
			dna[0]--;
		} else {
			dna[1]=(char)i;
		}
	}

	public int toInt() {
		if (dna.length==1) {
			return (int)dna[0];
		} else {
			long lo = dna[0]*65536 + dna[1];
			if (lo>L31) {
				lo=lo-L32;
			}
			return (int)lo;
		}
	}
	//------------------
	//as a ref
	//A Ref is usually an object or array
	//This could be ref to a name if this is stored in a class
	public Cell(char c) {
		dna=new char[]{c};
	}

	//return the initial char
	public char toChar() {
		return dna[0];
	}

	public int length() {
		return dna.length;
	}
	//--------------------
	//I arbitrarily limit strings to a length of 64. We could handle longer strings
	//it is illegal to have cells with a length of less than 3 because it can get confused with a number
	public Cell(String s) {
		if (s==null) {
			dna = new char[]{(char)0};
		} else if (s.length()==1) {
			dna = new char[]{s.charAt(0),(char)32,(char)32};
		} else if (s.length()==2) {
			dna = new char[]{s.charAt(0),s.charAt(1),(char)32};
		} else if (s.length()>64) {
			throw new IllegalArgumentException("string is too long: "+s.length());
		} else {
			dna = s.toCharArray();
		}
		hashCode = s.hashCode();
	}

	public String toString() {
		if (dna.length==1) {
			return "ref "+Integer.toString((int)dna[0]);
		} else if (dna.length==2) {
			return Integer.toString( toInt());
		} else {
			return String.valueOf(dna);
		}
	}

	public char charAt(int i) {return dna[i];}

	public boolean equals(Object o) {
		if (o==null) return false;
		else {
			if (!(o instanceof Cell)) {
				return false;
			} else {
				Cell c2 = (Cell)o;
				if (c2.length()==1 && (dna[0]==c2.dna[0]) ) {
					return true;
				} else if ( (c2.length()==2) && (dna[0]==c2.dna[0]) && (dna[1]==c2.dna[1])) {
					return true;
				} else if (length()==c2.length() && hashCode==c2.hashCode) {
					//for strings
					return true;
				} else {
					return false;
				}
			}
		}
	}

	//---------------------
	//for use as StringBuilder
	//this is not the most efficient algorithm since every time you append
	//it creates a new array and copies everything over
	//but this is simpler than keeping track of a byte heap
	//It is a good idea to end a string with a space so you can append
	//things to it
	public Cell append(String s) {
		char[] schars = s.toCharArray();
		char[] dna2 = new char[ dna.length + s.length()];
		System.arraycopy(dna,0,dna2,0,dna.length);
		System.arraycopy(schars,0,dna2,dna.length,s.length());
		//replace current dna with new one
		this.dna = dna2;
		return this;
	}

	public Cell append(int i) {
		String si = Integer.toString(i);
		return append(si);
	}

	//this could be used to append a single letter to a string
	public Cell append(byte b) {
		char[] dna2 = new char[ dna.length + 1];
		System.arraycopy(dna,0,dna2,0,dna.length);
		dna2[dna2.length-1]=(char)b;
		//replace current dna with new one
		this.dna = dna2;
		return this;
	}

	public Cell append(Cell ce) {
		char[] dna2 = new char[ dna.length + ce.dna.length];
		System.arraycopy(dna,0,dna2,0,dna.length);
		System.arraycopy(ce.dna,0,dna2,dna.length, ce.dna.length);
		//replace current dna with new one
		this.dna = dna2;
		return this;
	}

	//====================
	public static void main(String[] sa) {
		Cell c = new Cell("hello");
		Cell c2 = new Cell(" world");
		c.append(c2);
		System.out.println(c);
	}
}