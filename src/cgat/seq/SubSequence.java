package cgat.seq;

public class SubSequence extends Sequence {
	SeqRegion reg;
	Sequence seq;
	SubSequence rev;
	String subseqName;

	public SubSequence(Sequence _seq, int from, int to) {
		seq = _seq;
		reg = new SeqRegion(from, to);
	}
	public SubSequence(Sequence _seq, int from, int to, int dir) {
		reg = new SeqRegion(from, to);
		if (dir < 0) {
			seq = _seq.getReverse();
		} else {
			seq = _seq;
		}
	}
	public SubSequence(Sequence _seq, SeqRegion _reg) {
		reg = _reg;
		if (reg.isReverse()) {
			seq = _seq.getReverse();
		} else {
			seq = _seq;
		}
	}
	public void setName(String name) {
		subseqName = name;
	}
	public String getName() {
		if (subseqName != null) {
			return subseqName;
		}
		return seq.getName();
	}
	public void setRegion(int from, int to) {
		reg.setFrom(from);  reg.setTo(to);
	}
	public SeqRegion getRegion() {
		return reg;
	}
	public String getSeqString() {
		return seq.getSubSeqString(reg.getFrom(), reg.getTo());
	}
	public RawSequence createSubSequence(int from,  int to) {
		return seq.createSubSequence(reg.getFrom() + from - 1,
				reg.getFrom() + to - 1);
	}
	public String getSubSeqString(int _from, int _to)
			throws IndexOutOfBoundsException {
		// substring is always linear string
		if (_from > length() || _to > length()) {
			throw new IndexOutOfBoundsException();
		}
		return(seq.getSubSeqString(reg.getFrom() + _from - 1,
					reg.getFrom() + _to - 1));
	}
	public int getSubSeqStringLength(int _from, int _to)
			throws IndexOutOfBoundsException {
		if (_from > length() || _to > length()) {
			throw new IndexOutOfBoundsException();
		}
		return seq.getSubSeqStringLength(reg.getFrom() + _from - 1,
					reg.getFrom() + _to - 1);
	}
	public String getAltNameSequence() {
		return seq.getAltNameSubSequence(reg.getFrom(), reg.getTo());
	}
	public String getAltNameSubSequence(int from, int to) {
		return seq.getAltNameSubSequence(reg.getFrom() + from - 1,
			reg.getTo() + to - 1);
	}
	public int length() {
		return seq.getSubSeqStringLength(reg.getFrom(), reg.getTo());
	}
	public Sequence getReverse() {
		if (rev == null) {
			rev = new SubSequence(
				seq.getReverse(), reg.getFrom(), reg.getTo());
			rev.rev = this;
		}
		return rev;
	}

	public Alphabet getAlphabet() {
		return seq.getAlphabet();
	}

	/** a subsequence is always linear */
	public boolean isCircular() {
		return false;
	}
}
