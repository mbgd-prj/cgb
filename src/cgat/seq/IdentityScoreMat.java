package cgat.seq;
import java.io.*;
import java.lang.*;
import java.text.*;

public class IdentityScoreMat extends ScoreMat {
	int MATCH_SCORE = 5;
	int MISMATCH_SCORE = -4;
	public IdentityScoreMat() {
		setScores(MATCH_SCORE, MISMATCH_SCORE);
	}
	public IdentityScoreMat(int match, int mismatch) {
		setScores(match, mismatch);
	}
	public void setScores(int match, int mismatch) {
		MATCH_SCORE = match;
		MISMATCH_SCORE = mismatch;
		maxScore = MATCH_SCORE;
		minScore = MISMATCH_SCORE;
		opengap = edgegap = MISMATCH_SCORE * 2;
		setGaps((int)(MISMATCH_SCORE*1.5),
			(int)(MISMATCH_SCORE*0.5),
			(int)(MISMATCH_SCORE*0.5));
	}
	public int score(char a, char b) {
		if (a == b) {
			return MATCH_SCORE;
		} else {
			return MISMATCH_SCORE;
		}
	}
	public String toString() {
		StringBuffer outstr = new StringBuffer();
		outstr.append("MATCH_SCORE: "+MATCH_SCORE+"; ");
		outstr.append("MISMATCH_SCORE: "+MISMATCH_SCORE);
		return outstr.toString();
	}
}
