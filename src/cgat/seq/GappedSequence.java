package cgat.seq;

public class GappedSequence extends RawSequence {
	char GAP_SYMBOL = SequenceAlignment.GAPCHAR;
	char INS_SYMBOL = SequenceAlignment.INSCHAR;
	int [] r2t, t2r;
	public GappedSequence(String name, String seq) {
		setName(name);
		setSeqString(seq);
		makeGapCntTab();
	}
	public void setGapSymbol(char gap) {
		GAP_SYMBOL = gap;
	}
	/* 
	       //AB--C/D--E
	  ref: --ABooC-DooD
	       --01234-5678
 	  tgt: ooAB--CoD--E
	       0123--456--7
	  r2t  2,2,1,0,0,1,0,-1,-1
	  t2r  -1,-2,-2,-2,0,-1,-1,1
	*/
	public void makeGapCntTab() {
		int i;
		int gapcnt = 0;
		int inscnt = 0;

		int length = length();
		r2t = new int[length];
		t2r = new int[length];

		int rp = 0,tp = 0;
		for (i = 0; i < length; i++) {
			if (charAt(i)==GAP_SYMBOL) {
				++gapcnt;
			} else if (charAt(i)==INS_SYMBOL) {
				++inscnt;
			}
			if (charAt(i) != GAP_SYMBOL) {
				t2r[tp++] = (gapcnt - inscnt);
			}
			if (charAt(i) != INS_SYMBOL) {
				r2t[rp++] = (inscnt - gapcnt);
			}
		}
	}
	/** get orignal (tgt) position from alignment (ref) position */
	public int getOrigPos(int i) {
		return (i + r2t[i]);
	}
	/** get alignment (ref) position from original (tgt) position */
	public int getAliPos(int i) {
		return (i + t2r[i]);
	}
}
