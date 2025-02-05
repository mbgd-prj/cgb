package cgat.seq;

/**
A class representing a reverse complement of a given RawSequence.
The original sequence is ratained as is, and some methods are
overridden for returning correct result.
*/
public class ReversedRawSequence extends Sequence {
	RawSequence _rawseq;
	ReversedRawSequence(RawSequence seq) {
		_rawseq = seq;
	}
	public String getName() {
		return _rawseq.getName();
	}
	public String getSeqString() {
		return _getReversedString(_rawseq.getSeqString());
	}
	/** reversed subsequence on original coordinate (Firstly subsequenced, then reversed;
			modified from the original implementation in CGAT) */
	public String getSubSeqStringOrig(int from, int to) {
		return _getReversedString(_rawseq.getSubSeqString(from, to));
	}
	/** reversed subsequence on reversed coordinate (Firstly reversed, then subsequenced;
			modified from the original implementation in CGAT) */
	public String getSubSeqString(int from, int to) {
		return _getReversedString(_rawseq.getSubSeqString(length() - 1 - to, length() - 1 - from));
	}

	/* reversed subsequence (Firstly subsequenced, then reversed */
	private String _getReversedString(String str) {
		return _getReversedSubString(str, 0, str.length()-1);
	}
	private String _getReversedSubString(String str, int from, int to) {
		Alphabet alpha = getAlphabet();
		StringBuffer strbuf = new StringBuffer();
		for (int i = to; i >= from; i--) {
			char c = str.charAt(i);
			if (alpha != null) {
				try {
					c = alpha.complement(c);
				} catch (Exception e) {
					System.out.println(str);
					System.out.println(str.length());
					System.out.println(e);
					System.out.println(c);
					System.out.println(i);
					System.exit(1);
				}
			}
			strbuf.append(c);
		}
		return(strbuf.toString());
	}
	public int getSubSeqStringLength(int from, int to) {
		return _rawseq.getSubSeqStringLength(from, to);
	}
	public String getAltNameSequence() {
		return getAltNameSubSequence(1, length());
	}
	public String getAltNameSubSequence(int from, int to) {
		return _getReversedAltNameSubSequence(
			_rawseq.getSubSeqString(from, to));
	}
	private String _getReversedAltNameSubSequence(String str) {
		Alphabet alpha = getAlphabet();
		StringBuffer strbuf = new StringBuffer();
		for (int i = str.length()-1; i >= 0; i--) {
			char c = str.charAt(i);
			if (alpha != null) {
				c = alpha.complement(c);
				String s = alpha.getName(c);
				strbuf.append(s);
			} 
		}
		return(strbuf.toString());
	}

	public Alphabet getAlphabet() {
		return _rawseq.getAlphabet();
	}
	public boolean isCircular() {
		return _rawseq.isCircular();
	}
	public int length() {
		return _rawseq.length();
	}
	public char charAt(int i) {
		return getAlphabet().complement(_rawseq.charAt(length() - 1 - i));
	}
	public char symbolAt(int i) {
		Alphabet alpha = getAlphabet();
		return alpha.complement(_rawseq.symbolAt(length() - i + 1));
	}
	public void reverse() {
	}
	public Sequence getReverse() {
		return _rawseq;
	}
}
