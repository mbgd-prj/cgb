package cgat.seq;
import java.lang.*;
import java.io.*;
import java.util.*;

public class SequenceAlignment{
	static char GAPCHAR = '-';
	static char INSCHAR = '/';
	static char MATCH_CHAR = ':';
	static char POSITIVE_CHAR = '.';
	static char MISMATCH_CHAR = ' ';
	static int TITWIDTH = 14;

	char add_match = 0;
	Sequence[] seqSet;
	String[] alignSeq;
	GappedSequence[] gappedSeq;
	int seqnum;
	int score;
	int aliLen;
	int lineLen = 60;
	char add_consensus = 0;
	ScoreMat sMat;
	short[] aliposIdx;
	boolean modeAlignToRef = true;

	public SequenceAlignment(Sequence [] _seqSet, int _seqnum,
			LinkedList aliPath) {
		makeAlignment(_seqSet, _seqnum, aliPath);
	}
	public SequenceAlignment(Sequence [] _seqSet, int _seqnum,
			LinkedList aliPath, ScoreMat _sMat) {
		makeAlignment(_seqSet, _seqnum, aliPath);
		setScoreMat(_sMat);
	}
	public void makeAlignment(Sequence [] _seqSet, int _seqnum,
			LinkedList aliPath) {
		seqnum = _seqnum;
		seqSet = _seqSet;
		StringBuffer[] alignSeqBuf = new StringBuffer[_seqnum];

		int [] posSet, prevPosSet;
		Iterator iter = aliPath.iterator();
		for (int i = 0; i < seqnum; i++) {
			alignSeqBuf[i] = new StringBuffer();
		}
		prevPosSet = new int[seqnum];
		while (iter.hasNext()) {
			posSet = (int []) iter.next();
			int maxdiff = 0;
			int diff = 0;
			for (int i = 0; i < seqnum; i++) {
				diff = posSet[i] - prevPosSet[i] - 1;
				if (maxdiff < diff) {
					maxdiff = diff;
				}
			}
			for (int i = 0; i < seqnum; i++) {
				diff = posSet[i] - prevPosSet[i] - 1;
				for (int j = 0; j < maxdiff; j++) {
					if (j < diff) {
					    alignSeqBuf[i].append(
						seqSet[i].charAt(
						  prevPosSet[i]+j));
					} else {
					    alignSeqBuf[i].append(GAPCHAR);
					}
				}
				if (diff >= 0) {
					alignSeqBuf[i].append(seqSet[i].
						charAt(posSet[i]-1));
				} else {
					alignSeqBuf[i].append(GAPCHAR);
				}
			}
			prevPosSet = posSet;
		}
		alignSeq = new String[_seqnum];
		for (int i = 0; i < seqnum; i++) {
			alignSeq[i] = alignSeqBuf[i].toString();
		}
		aliLen = alignSeq[0].length();
		if (seqnum == 2) {
			add_match = 1;
			add_consensus = 0;
		} else {
			add_match = 0;
			add_consensus = 1;
		}
	}
	/** get aligned sequence (with gaps) of the specified seq w.r.t. the other seq as reference */
	public String getAlignedSeqToRef(int tgt_id) {
		StringBuffer aliseq = new StringBuffer();
		int ref_id = (tgt_id == 0 ? 1 : 0);
		boolean ins_stat = false;
		for (int i = 0; i < aliLen; i++) {
			char refc = alignSeq[ref_id].charAt(i);
			char tgtc = alignSeq[tgt_id].charAt(i);
			if (refc == GAPCHAR) {
//				if (! ins_stat) {
					aliseq.append(INSCHAR);
//				}
//				ins_stat = true;
			} else {
				aliseq.append(tgtc);
//				ins_stat = false;
			}
		}
		return(aliseq.toString());
	}
	public String toCIGAR() {
		return(toCIGAR(0));
	}
	public String toCIGAR(int refid) {
		if (refid > 1) {
			return null;
		}
		StringBuffer cigar = new StringBuffer();
		char stat, prevstat = '0';
		int count = 0;
		int tgtid = (refid == 0 ? 1 : 0);
		for (int i = 0; i < aliLen; i++) {
			char refc = alignSeq[refid].charAt(i);
			char tgtc = alignSeq[tgtid].charAt(i);
			if (refc == GAPCHAR) {
				stat = 'I';
			} else if (tgtc == GAPCHAR) {
				stat = 'D';
			} else {
				stat = 'M';
			}
			if (prevstat == stat) {
				count++;
			} else {
				if (count > 0 && prevstat != '0') {
					cigar.append(count);
					cigar.append(prevstat);
				}
				count = 1;
				prevstat = stat;
			}
		}
		cigar.append(count);
		cigar.append(prevstat);
		return(cigar.toString());
	}
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		for (int j = 0; j < aliLen; j += lineLen) {
			for (int i = 0; i < seqnum; i++) {
				int endj = Math.min(j+lineLen, aliLen);
				strbuf.append(Utils.setStringWidth(
					seqSet[i].getName(), TITWIDTH));
				strbuf.append(alignSeq[i].substring(j, endj));
				strbuf.append("\n");
				if (add_match == 1 && (i < seqnum - 1)) {
					strbuf.append(Utils.
						setStringWidth("", TITWIDTH));
					for (int k = j; k < endj; k++) {
					    strbuf.append(
						matchChar(alignSeq[i].charAt(k),
						  alignSeq[i+1].charAt(k)));
					}
					strbuf.append("\n");
				}
			}
			strbuf.append("\n");
		}
		strbuf.append("Score: "+score+"\n");
		return strbuf.toString();
	}
	static public boolean isGap(char ch) {
		return(ch == GAPCHAR);
	}
	static public boolean isIns(char ch) {
		return(ch == INSCHAR);
	}
	public void setScoreMat(ScoreMat _sMat) {
		sMat = _sMat;
	}
	static public void setMatchChars(char match, char positive, char mismatch) {
		MATCH_CHAR = match;
		POSITIVE_CHAR = positive;
		MISMATCH_CHAR = mismatch;
	}
	public void setScore(int _score) {
		score = _score;
	}
	public int getScore() {
		return(score);
	}
	public String getAlignedSeq(int idx) {
		return alignSeq[idx];
	}
	/** Gapped sequnece against the refseq (which has no gap) is saved */
	public void setAlignToRef() {
		modeAlignToRef = true;
	}
	/** Gapped sequnece for each aligned sequence is saved */
	public void unsetAlignToRef() {
		modeAlignToRef = false;
	}
	public GappedSequence getGappedSequence(int idx) {
		String name = seqSet[idx].getName();
		if (modeAlignToRef) {
			return( new GappedSequence(name, getAlignedSeqToRef(idx)) );
		} else {
			return( new GappedSequence(name, alignSeq[idx]) );
		}
	}
	public GappedSequence getGappedSequenceSave(int idx) {
		if (gappedSeq == null) {
			gappedSeq = new GappedSequence[seqnum];
		}
		gappedSeq[idx] = getGappedSequence(idx);
		return gappedSeq[idx];
	}
	private char matchChar(char c1, char c2) {
		if (c1 == c2) {
			return(MATCH_CHAR);
		} else if (sMat != null && sMat.seqScore(c1,c2,0) > 0) {
			return(POSITIVE_CHAR);
		} else {
			return(MISMATCH_CHAR);
		}
	}
}
