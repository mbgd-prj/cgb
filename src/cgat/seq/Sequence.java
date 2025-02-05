package cgat.seq;

public abstract class Sequence {
	/** Get the name of the sequence */
	public abstract String getName();

	/** Get the sequence as a string */
	public abstract String getSeqString();

	/** Create a subsequence as a new raw sequence */
	public RawSequence createSubSequence(int from, int to)
			throws IndexOutOfBoundsException {
		RawSequence seq = new RawSequence(getName(),
				getSubSeqString(from,to), getAlphabet());
		return seq;
	}

	/** Create a subsequence as a string */
	public abstract String getSubSeqString(int from, int to);

	/** Get the length of given subsequence */
	public abstract int getSubSeqStringLength(int from, int to);

	/** Get a sequence of alternative code (e.g. 3 letters AA code) */
	public abstract String getAltNameSequence();

	/** Get a subsequence of alternative code */
	public abstract String getAltNameSubSequence(int from, int to);

	/** Get a reverse complement of the current sequence */
	public abstract Sequence getReverse();

	public abstract Alphabet getAlphabet();

	/**
		Return a String representing the sequence as FASTA format.
	*/
	public String toFasta() {
		StringBuffer str = new StringBuffer();
		str.append(">" + getName() + "\n");
		str.append(getSeqString());
		return str.toString();
	}
	/**
		Same as toFasta().
	*/
	public String toString() {
		return toFasta();
	}

	/** Get the length of the sequence */
	public int length() {
		return getSeqString().length();
	}

	/** Get a symbol at idx (beginning from 0)*/
	public char charAt(int idx) {
		return getSeqString().charAt(idx);
	}

	/** Get a symbol at idx (beginning from 1)*/
	public char symbolAt(int idx) {
		return getSeqString().charAt(idx-1);
	}
	public abstract boolean isCircular();
	int getType() {
		return(FastaFile.type.UNKNOWN);
	}
}
