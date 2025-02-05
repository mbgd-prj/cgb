package cgat.seq;
import java.io.*;

public class SequenceDB {
	static IndexedFastaFile default_idxFasta;
	IndexedFastaFile idxFasta;
	String seqName;

	static void setDefaultFastaFile(IndexedFastaFile idxFasta) {
		default_idxFasta = idxFasta;
	}
	public SequenceDB (String seqName) {
		this(seqName, default_idxFasta);
		if (default_idxFasta == null) {
			System.err.println("SequenceDB: Set indexed fasta file before using it.");
		}
	}
	public SequenceDB (String _seqName, IndexedFastaFile _idxFasta) {
		seqName = _seqName;
		idxFasta = _idxFasta;
	}
	/** Simple function for taking substring. Use indexed fasta file instead of on momory data */
	public String getSeqString_substring(int from, int to) {
		return(idxFasta.getRegionSeq(seqName, from, to));
	}
	public int length() {
		return((int) idxFasta.length(seqName));
	}
	public boolean isCircular() {
		return(idxFasta.isCircular(seqName));
	}
	public static void main(String[] args) {
		IndexedFastaFile idxFasta = null;
		String filename = args[0];
		String entname = args[1];
		int from = Integer.parseInt(args[2]);
		int to = Integer.parseInt(args[3]);

		try {
			idxFasta = new IndexedFastaFile(filename);
		} catch (IOException e) {
		}
		
		SequenceDB.setDefaultFastaFile(idxFasta);
		SequenceDB seqdb = new SequenceDB(entname);
		String seqstr = seqdb.getSeqString_substring(from, to);
		System.out.println(seqstr);
	}
}

