package cgat.seq;

public class RawSequenceDB extends RawSequence {
	SequenceDB seqdb;

	/** default indexed file is used (set by SequenceDB.setDefaultFastaFile beforehand) */
	public RawSequenceDB(String name) {
		seqdb = new SequenceDB(name);
	}
	public RawSequenceDB(String name, IndexedFastaFile fastaFile) {
		seqdb = new SequenceDB(name, fastaFile);
	}
	public String getSubSeqString(int from, int to) {
System.out.println("#C1");
		return( super.getSubSeqString(from, to) );
	}
	/* Return substring of sequence; position is 0-based */
	public String getSeqString_substring(int from) {
		return(seqdb.getSeqString_substring(from, length()));
	}
	/* Return substring of sequence; position is 0-based */
	public String getSeqString_substring(int from, int to) {
System.out.println("#C2");
		/* cgat.seq is 1-based and idxfasta is 0-based; convert 0-based to 1-based */
		return(seqdb.getSeqString_substring(from, to));
	}
	public int length() {
		return(seqdb.length());
	}
	public boolean isCircular() {
		return(seqdb.isCircular());
	}
}

