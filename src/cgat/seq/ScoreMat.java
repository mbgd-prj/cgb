package cgat.seq;
import java.io.*;
import java.lang.*;
import java.text.*;

public class ScoreMat {
	Alphabet alpha;
	int [][] scoreMat;
	int minScore, maxScore;

	public int edgegap, opengap, extgap;

	public ScoreMat() {
	}
	public ScoreMat(String filename) {
		read_NCBIMatrixFile(filename);
	}
	public void read_NCBIMatrixFile(String filename) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(
				new FileReader(filename));
		} catch (IOException e) {
			System.err.println("Can't open matrixfile");
			System.exit(1);
		}
		try {
			int ln = 0;
			String line = null;
			String [] strArray = null;
			String [] chars0;
			char [] chars;
			int i = 0, j = 0;

			while ( (line = in.readLine()) != null ) {
				if (line.charAt(0) == '#') {
					continue;
				}
				if (ln == 0 && line.charAt(0) == ' ') {
					chars0 = Utils.split(line, " ");
					chars = new char[chars0.length];
					for (int k = 0; k < chars0.length; k++){
						chars[k] = chars0[k].charAt(0);
					}
					alpha = new Alphabet(chars);
					scoreMat = new int[chars.length][chars.length];
					i = 0;
				} else {
					strArray = Utils.split(line, " ");
					for (j = 1; j < strArray.length; j++) {
						int ival =
					Integer.valueOf(strArray[j]).intValue();
						scoreMat[i][j-1] = ival;
						if (ival > maxScore) {
							maxScore = ival;
						}
						if (ival < minScore) {
							minScore = ival;
						}
					}
					i++;
				}
				ln++;
			}
		} catch (IOException e) {
		}
		edgegap = opengap = extgap = minScore;
	}
	public void setGaps(int _opengap, int _extgap, int _edgegap) {
		opengap = _opengap;
		extgap = _extgap;
		edgegap = _edgegap;
	}
	public int score(char a, char b) {
		int i = alpha.toIdx(a);
		int j = alpha.toIdx(b);
		return scoreMat[i][j];
	}
	public int seqScore(char a, char b, int gapstatus) {
		if (a == '-' || b == '-') {
			if (a == b) {
				return 0;
			} else if (gapstatus == 2) {
				gapstatus = 3;
				return edgegap;
			} else if (gapstatus == 3) {
				return Math.max(edgegap,extgap);
			} else if (gapstatus == 0) {
				gapstatus = 1;
				return opengap;
			} else if (gapstatus == 1) {
				return extgap;
			}
		} else {
			gapstatus = 0;
			return score(a, b);
		}
		return 0;
	}
	public String toString() {
		int i, j;
		StringBuffer outstr = new StringBuffer();
		NumberFormat nf = NumberFormat.getInstance();
		for (i = 0; i < alpha.charnum(); i++) {
			outstr.append(" "+alpha.get(i));
		}
		outstr.append("\n");
		for (i = 0; i < alpha.charnum(); i++) {
			outstr.append(alpha.get(i));
			for (j = 0; j < alpha.charnum(); j++) {
				outstr.append(" ");
				nf.format(scoreMat[i][j],outstr,
					new FieldPosition(nf.INTEGER_FIELD));
			}
			outstr.append("\n");
		}
		return outstr.toString();
	}
/*
	static public void main(String args[]) {
		String matfile;
		if (args.length > 0) {
			matfile= args[0];
		} else {
			matfile= "/bio/db/blast/matrix/aa/blosum62";
		}
		ScoreMat scoreMat = new ScoreMat(matfile);
		System.out.println(scoreMat);
	}
*/
}
