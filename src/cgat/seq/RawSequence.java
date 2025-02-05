package cgat.seq;

public class RawSequence extends Sequence {
	Alphabet alpha;
	String name;
	String seqString;
	int length = -1;
	boolean circular = false;
	ReversedRawSequence revSeq;

	public RawSequence () {
	}
	public RawSequence(String name, String seq) {
		setName(name); setSeqString(seq); setAlphabet(new Alphabet());
	}
	public RawSequence(String name, String seq, Alphabet alpha) {
		setName(name); setSeqString(seq); setAlphabet(alpha);
	}
	public void setSequence(Sequence seq){
		setName(seq.getName());
		setSeqString(seq.getSeqString());
	}
	public void setName(String _name) {
		name = new String(_name);
	}
	public String getName() {
		return(name);
	}
	public void setSeqString(String _seq) {
		seqString = new String(_seq);
		length = -1;
	}
	public void checkSeqString() {
		StringBuffer newseq = new StringBuffer();
		if (alpha == null) {
			return;
		}
		for (int i = 0; i < seqString.length(); i++) {
			char c = seqString.charAt(i);
			if (alpha.contains(c)) {
				c = alpha.get(alpha.toIdx(c));
				newseq.append(c);
			}
		}
		seqString = newseq.toString();
	}
	public String getSeqString() {
		return(seqString);
	}
	public String getSubSeqString(int from, int to)
			throws IndexOutOfBoundsException {
System.out.println("00#### "+from+" "+to+" "+isCircular());
		if (isCircular()) {
			if (from < 1) {
				from += length();
			} else if (from > length()) {
				from -= length();
			}
			if (to < 1) {
				to += length();
			} else if (to > length()) {
				to -= length();
			}
		} else {
			if (from < 1) {
				from = 1;
			} else if (from > length()) {
				from = length();
			}
			if (to < 1) {
				to = 1;
			} else if (to > length()) {
				to = length();
			}
		}
System.out.println("#### "+from+" "+to+" "+isCircular());
		if (from <= to) {
			return getSeqString_substring(from - 1, to);
		} else if (from > to && isCircular()) {
System.out.println("#### "+from+" "+to);
			return (getSeqString_substring(from - 1) +
				getSeqString_substring(0, to));
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	/** simple function for taking substring; pos is 0-based */
	String getSeqString_substring(int from) {
		return(getSeqString().substring(from));
	}
	String getSeqString_substring(int from, int to) {
		return(getSeqString().substring(from, to));
	}
	public int getSubSeqStringLength(int from, int to)
			throws IndexOutOfBoundsException {
		if (from <= to) {
			return to - from + 1;
		} else if (isCircular()) {
			return (length() - from + 1 + to);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	public String getAltNameSequence()
				throws IndexOutOfBoundsException{
		return getAltNameSubSequence(1, length());
	}
	public String getAltNameSubSequence(int from, int to)
				throws IndexOutOfBoundsException{
		if (from <= to) {
			return _getAltNameSubSequence(from-1, to);
		} else if (isCircular()) {
			return _getAltNameSubSequence(from-1, length())
				+ _getAltNameSubSequence(0, to);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}
	private String _getAltNameSubSequence(int from, int to) {
		String str = getSeqString();
		StringBuffer retstr = new StringBuffer();
		for (int i = from; i < to; i++) {
			retstr.append(alpha.getName(str.charAt(i)));
		}
		return retstr.toString();
	}
	public Alphabet getAlphabet() {
		return alpha;
	}
	public void setAlphabet(Alphabet _alpha) {
		alpha = _alpha;
	}
/*
	public void setDirection(int dir) {
		direction = dir;
	}
	public int getDirection() {
		return direction;
	}
*/
	public void setCircular(boolean isCirc) {
		circular = isCirc;
	}
	public boolean isCircular() {
		return circular;
	}
	/**
		Return a String representing the sequence as FASTA format.
	*/
	public String toFasta() {
		StringBuffer str = new StringBuffer();
		str.append(">"+name+"\n");
		str.append(seqString);
		return str.toString();
	}
	/**
		Same as toFasta().
	*/
	public String toString() {
		return toFasta();
	}
	/**
		Return the length of the current sequence.
	*/
	public int length() {
		if (length < 0) {
			length = seqString.length();
		}
		return(length);
	}
	/**
		Returns the character of sequence at the specified index.
		An index is beginning from 0, as compatible with
		java.lang.String.
	*/
	public char charAt(int i) {
		return seqString.charAt(i);
	}
	/**
		Returns the character of sequence at the specified index.
		An index is beginning from 1, as compatible with
		bio.java.SymbolList.
	*/
	public char symbolAt(int i) {
		return seqString.charAt(i-1);
	}
	public Sequence getReverse() {
		if (revSeq == null) {
			revSeq = new ReversedRawSequence(this);
		}
		return (Sequence) revSeq;
	}

/*
//	public Object clone() throws CloneNotSupportedException {
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
//			throw e;
		}
		return null;
	}
*/
	public static void main(String[] args){
		RawSequence seq = new RawSequence("name", "AgaCGTGACAttaG");
		System.out.println(seq);
		System.out.println(seq.getName());
		Sequence subseq = new SubSequence(seq, 2,6);
		seq.setCircular(true);
		System.out.println(seq.isCircular());
		System.out.println(subseq);
		subseq = new SubSequence(seq, 10, 3);
		System.out.println(subseq);
	}
}
