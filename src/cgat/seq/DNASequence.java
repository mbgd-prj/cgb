package cgat.seq;
public class DNASequence extends RawSequence {
	public DNASequence(String name, String seq) {
		super(name, seq);
		alpha = Alphabet.getNucleotides();
	}
	public DNASequence(String name, String seq, boolean isCircular) {
		super(name, seq);
		alpha = Alphabet.getNucleotides();
		setCircular(isCircular);
	}

	public static String reverseComplement(String seq){
		DNASequence dna = new DNASequence("rev", seq);
		return dna.getReverse().getSeqString();
	}
	public static ProteinSequence translate(Sequence seq){
		GeneticCode tab = GeneticCode.generate(1);
		return translate(seq, tab);
	}
	public static ProteinSequence translate(Sequence seq, GeneticCode tab) {
		return translate(seq, tab, 1);
	}
	public static ProteinSequence translate(Sequence seq, GeneticCode tab, int start) {
		String ntseq = seq.getSeqString();
		StringBuffer aaseq = new StringBuffer();
		for (int i = start-1; i < ntseq.length(); i+=3) {
			String codon = ntseq.substring(i, i+3);
			char aa = tab.getAmino(codon);
			aaseq.append(aa);
		}
		return new ProteinSequence(seq.getName(), aaseq.toString());
	}
	int getType() {
		return(FastaFile.type.DNA);
	}
/*
	public static void main(String[] args) {
		DNASequence dna = new DNASequence(
			"name", "AGTAGACAGAGGAAG", true);
		System.out.println(dna);
		Sequence dsub = new SubSequence(dna, 10, 12);
		System.out.println(dsub);
	}
*/
}
