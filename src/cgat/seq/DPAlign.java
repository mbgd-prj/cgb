package cgat.seq;
import java.lang.*;
import java.io.*;
import java.util.*;

public class DPAlign {
	Sequence seq1, seq2;
	ScoreMat sMat;
	char path[][];
	int gpath[][];
	int aliScore;
	boolean allocFlag = false;

	public DPAlign() {
		// default score matrix
		sMat = new IdentityScoreMat(5, -4);
	}

	public void setScoreMat(ScoreMat _scoreMat) {
		sMat = _scoreMat;
	}

	public void setSequences(String _seq1, String _seq2) {
		setSequence(_seq1, 0); setSequence(_seq2, 1);
	}
	public void setSequences(Sequence _seq1, Sequence _seq2) {
		setSequence(_seq1, 0); setSequence(_seq2, 1);
	}

	public void setSequence(String seq, int seqn) {
		setSequence(new RawSequence("Seq"+seqn, seq), seqn);
	}
	public void setSequence(Sequence seq, int seqn) {
		if (seqn == 0) {
			if (seq1 == null || seq.length() > seq1.length()) {
				allocFlag = true;
			}
			seq1 = seq;
		} else {
			if (seq2 == null || seq.length() > seq2.length()) {
				allocFlag = true;
			}
			seq2 = seq;
		}
	}

	private void allocateMatrix() {
		if (allocFlag) {
			path = new char[seq1.length() + 1][seq2.length() + 1];
			gpath = new int[seq1.length() + 1][seq2.length() + 1];
		}
		allocFlag = false;
	}
	public SequenceAlignment align(String _seq1, String _seq2) throws InterruptedException {
		setSequences(_seq1, _seq2);
		return align();
	}
	public SequenceAlignment align(Sequence _seq1, Sequence _seq2) throws InterruptedException {
		setSequences(_seq1, _seq2);
		return align();
	}
	public SequenceAlignment align() throws InterruptedException {
		SequenceAlignment ali;
		int score;
		score = calcMaxScore();
		ali = makeAlignment();
		ali.setScore(score);
		return(ali);
	}
	public int calcMaxScore() {
		int i, j;
		int len1 = seq1.length();
		int len2 = seq2.length();
		int s1, s2, s3, s10, s11, s30, s31;
		int max;
		char maxpath;
		char c1, c2;
		int score_0[], score_1[];
		int gscore1_0[], gscore1_1[], gscore3[];
		int gpath1[], gpath3;
		int opengap1, extgap1, opengap3, extgap3;
//System.out.println("start"+sMat.edgegap+" "+sMat.extgap);
		int edge_extgap = Math.max(sMat.edgegap, sMat.extgap);

		allocateMatrix();

		score_0 = new int[seq2.length() + 1];
		score_1 = new int[seq2.length() + 1];
		gscore1_0 = new int[seq2.length() + 1];
		gscore1_1 = new int[seq2.length() + 1];
		gscore3 = new int[seq2.length() + 1];
		gpath1 = new int[seq2.length() + 1];

		score_0[0] = gscore1_0[0] = gpath1[0] = 0;
//System.out.print(" "+score_0[0]);
		for (j = 1; j <= len2; j++) {
			score_0[j] = sMat.edgegap + edge_extgap * (j-1);
			gscore1_0[j] = score_0[j];
			gpath1[j] = 0;
//System.out.print(" "+score_0[j]);
		}
//System.out.println();
		for (i = 1; i <= len1; i++) {
			c1 = seq1.symbolAt(i);
			gscore3[0] = gscore1_0[0] = score_1[0] =
				sMat.edgegap + edge_extgap * (i-1);
			gpath3 = 0;
			opengap3 = (i<len1 ? sMat.opengap : sMat.edgegap);
			extgap3 = (i<len1 ? sMat.extgap : edge_extgap);
			for (j = 1; j <= len2; j++) {
				c2 = seq2.symbolAt(j);
				opengap1 = (j<len2 ?
					sMat.opengap : sMat.edgegap);
				extgap1 = (j<len2 ? sMat.extgap : edge_extgap);

				s10 = score_0[j] + opengap1;
				s11 = gscore1_0[j] + extgap1;
				if (s10 > s11) {
					s1 = gscore1_1[j] = s10;
					gpath1[j] = 1;
				} else {
					s1 = gscore1_1[j] = s11;
					gpath1[j]++;
				}

				s2 = score_0[j-1] + sMat.score(c1,c2);

				s30 = score_1[j-1] + opengap3;
				s31 = gscore3[j-1] + extgap3;
				if (s30 > s31) {
					s3 = gscore3[j] = s30;
					gpath3 = 1;
				} else {
					s3 = gscore3[j] = s31;
					gpath3++;
				}

				if (s1 > s2) {
					if (s1 >= s3) {
						max = s1;
						maxpath = 1;
					} else {
						max = s3;
						maxpath = 3;
					}
				} else {
					if (s2 >= s3) {
						max = s2;
						maxpath = 2;
					} else {
						max = s3;
						maxpath = 3;
					}
				}
				score_1[j] = max;
				path[i][j] = maxpath;
				if (maxpath == 1){
					gpath[i][j] = gpath1[j];
				} else if (maxpath == 3) {
					gpath[i][j] = gpath3;
				} else {
					gpath[i][j] = 0;
				}
			}
			for (j = 0; j <= len2; j++) {
				gscore1_0[j] = gscore1_1[j];
				score_0[j] = score_1[j];
//System.out.print(" "+score_0[j]);
			}
//System.out.println();
		}
		aliScore = score_1[len2];
		return aliScore;
	}
	public SequenceAlignment makeAlignment() throws InterruptedException {
		int i = seq1.length();
		int j = seq2.length();
		LinkedList aliPath = new LinkedList();
		Thread ct = Thread.currentThread();
		int [] posPair;
		while (path[i][j] != 0) {
			if (ct.isInterrupted()) {
				throw new InterruptedException("makeAlignment path["+i+"]["+j+"]");
			}
			posPair = new int[2];
			posPair[0] = i; posPair[1] = j;
			aliPath.addFirst(posPair);
			if (path[i][j] == 1) {
				i -= gpath[i][j];
			} else if (path[i][j] == 3) {
				j -= gpath[i][j];
			} else {
				i--; j--;
			}
		}
		Sequence [] seqset = new Sequence[2];
		seqset[0] = seq1; seqset[1] = seq2;
		return new SequenceAlignment(seqset, 2, aliPath, sMat);
	}
	public String getAlignedSeq(int idx) {
		// dummy
		return new String();
	}
	public static void main(String args[]) {
		DPAlign dp = new DPAlign();
		SequenceAlignment ali;
		RawSequence seq1=null, seq2=null;
		FastaFile ff = null;
		ScoreMat smat;

		if (args.length < 1) {
			System.err.println("Usage: DPAlign fastafile");
			System.exit(1);
		}

//		smat = new IdentityScoreMat(5, -4);
//		smat.setGaps(-8, -2, -2);

		smat = new ScoreMat("blosum62");
		smat.setGaps(-12, -2, -10);
		dp.setScoreMat(smat);

		try {
			ff = new FastaFile(args[0]);
		} catch (IOException e) {
			System.err.println("Can't open file\n");
			System.exit(1);
		}
		try {
			seq1 = ff.readSeq();
			while ( (seq2 = ff.readSeq()) != null) {
				ali = dp.align(seq1, seq2);
				System.out.print(ali);
			}
		} catch (InterruptedException ie) {
			System.err.println("Interrupted.\n");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Can't read sequences\n");
			System.exit(1);
		}
	}
}

