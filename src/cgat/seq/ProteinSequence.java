package cgat.seq;
public class ProteinSequence extends RawSequence {
	public ProteinSequence(String name, String seq) {
		super(name, seq);
		alpha = Alphabet.getAminoAcids();
	}
	int getType() {
		return(FastaFile.type.PROTEIN);
	}
/*
	public String get3LetterSeq() {
		String str = getSeqString();
		StringBuffer retstr = new StringBuffer();
		for (int i = 0; i < length(); i++) {
			retstr.append(alpha.getName(str.charAt(i)));
		}
		return retstr.toString();
	}
*/
}
