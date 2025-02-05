package cgdp.corealign;

import lombok.Data;

class Region {
	int begin, end;

	Region() {
	}

	Region(int _begin, int _end) {
		begin = _begin;
		end = _end;
	}
}

/**
 * Sequence region
 */
@Data
class SeqRegion extends Region {
	int begin, end;
	static int offset = 1;

	SeqRegion() {
	}

	SeqRegion(int _begin, int _end) {
		begin = _begin;
		end = _end;
	}

	static void zeroBased() {
		offset = 0;
	}

	boolean overlap() {
		return true;
	}

	boolean overlap(SeqRegion seqReg) {
		return overlap(seqReg, 1);
	}

	boolean overlap(SeqRegion seqReg, int minOvlp) {
		if (seqReg == null) {
			return false;
		}
		return (seqReg.end() - begin() + offset >= minOvlp && end() - seqReg.begin() + offset >= minOvlp);
	}

	SeqRegion overlapRegion(SeqRegion seqReg) {
		return overlapRegion(seqReg, 1);
	}

	SeqRegion overlapRegion(SeqRegion seqReg, int minOvlp) {
		if (!overlap(seqReg)) {
			return null;
		}
		SeqRegion ovlpReg = new SeqRegion();
		ovlpReg.setRegion(Math.max(begin(), seqReg.begin()),
				Math.min(end(), seqReg.end()));
		return (ovlpReg);
	}

	boolean includes(SeqRegion seqReg) {
		return (includes(seqReg, 0));
	}

	boolean includes(SeqRegion seqReg, int addwin) {
		return (begin() <= seqReg.begin() - addwin && end() >= seqReg.end() + addwin);
	}

	SeqRegionReal getSeqRegionRatio(SeqRegion subRegion) {
		double beginR = (double) (subRegion.begin() - begin()) / length();
		double endR = (double) (subRegion.end() - begin()) / length();
		//System.out.println("THIS:"+this);
		//System.out.println("SUB:"+subRegion);
		//System.out.println("RATIO:"+beginR+"<>"+endR+"<");
		return (new SeqRegionReal(beginR, endR));
	}

	SeqRegion getSubSeqRegion(SeqRegionReal seqRegionRatio) {
		int subRegBegin = begin0() + (int) (length() * seqRegionRatio.begin);
		int subRegEnd = begin0() + (int) (length() * seqRegionRatio.end);
		return new SeqRegion(subRegBegin, subRegEnd);
	}

	int begin0() {
		return begin - offset;
	}

	int begin() {
		return begin;
	}

	int end0() {
		return end;
	}

	int end() {
		return end;
	}

	void setRegion(SeqRegion seqreg) {
		setRegion(seqreg.begin(), seqreg.end());
	}

	void setRegion(int _begin, int _end) {
		begin = _begin;
		end = _end;
	}

	void setCenterPos(int centerPos) {
		int shift = centerPos - (begin + end) / 2;
		begin += shift;
		end += shift;
	}

	int length() {
		return (end() - begin() + offset);
	}

	public String toString() {
		return begin() + " " + end();
	}
}

class SeqRegionReal extends Region {
	double begin, end;

	SeqRegionReal(double _begin, double _end) {
		begin = _begin;
		end = _end;
	}

	double begin() {
		return begin;
	}

	double end() {
		return end;
	}

	public String toString() {
		return begin() + " " + end();
	}
}

/*
 * OBS: Sequence region with truncation
class SeqRegionTrunc extends SeqRegion {
        boolean truncL, truncR;
        SeqRegionTrunc(int _begin, int _end, boolean _truncL, boolean _truncR) {
                super(_begin, _end);
                truncL = _truncL; truncR = _truncR;
        }
        SeqRegionTrunc (SeqRegion seqreg) {
                super(seqreg.begin, seqreg.end);
                truncL = truncR = false;
        }
        boolean isTruncL() {return truncL;}
        boolean isTruncR() {return truncR;}
}
*/
