package cgdp.corealign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgat.seq.DNASequence;
import cgat.seq.FastaFile;
import cgat.seq.IndexedFastaFile;
import cgat.seq.RawSequenceDB;
import cgat.seq.Sequence;

public class SeqData<SQ extends Sequence> {
	/**
	 * Logger.
	 */
	private Logger logger = LogManager.getLogger(SeqData.class);

	ArrayList<SQ> seqData;
	HashMap<String,SQ> seqHash;
	IndexedFastaFile idxFasta;
	boolean useFai;

	SeqData() {
		seqData = new ArrayList<SQ>();
		seqHash = new HashMap<String,SQ>();
	}

	public void dump() throws Exception {
		logger.debug("----- SeqData start -----");
		logger.debug("useFai=" + useFai);
		for (String k: this.seqHash.keySet()) {
			logger.debug("Seq:" + k);
		}
		if (this.idxFasta != null) {
			this.idxFasta.dump();
		}
		logger.debug("----- SeqData finish -----");
	}


	void addSequence(SQ seq) {
		seqHash.put(seq.getName(), seq);
		seqData.add(seq);
	}
	SQ getSequence(String spname, int seqno) {
		String chrname = spname + ":" + seqno;
		return getSequence(chrname);
	}
	SQ getSequence(String chrname) {
		return seqHash.get(chrname);
	}
	String getSubSeqString(String chrname, int begin, int end) {
		if (seqHash != null && seqHash.containsKey(chrname)) {
			SQ seq = null;
			if (seqHash.containsKey(chrname)) {
				seq = seqHash.get(chrname);
			}
			if (seq == null) {
				System.err.println("sequence not found: "+chrname);
			}
			/* 1-based */
			return seq.getSubSeqString(begin+1, end);
		} else if (useFai) {
			String seq = null;
			try {
				System.out.println("#getregion: "+idxFasta+" "+chrname+" "+begin+" "+end);
				RawSequenceDB seqdb = new RawSequenceDB(chrname, idxFasta);
				/* 1-based */
				seq = seqdb.getSubSeqString(begin+1, end);
/*
				seq = idxFasta.getRegionSeq(chrname, begin, end);
*/
			} catch (Exception e) {
				System.err.println("index search error");
			}
			return seq;
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	void readFromFasta(FastaFile seqFile) {
		SQ seq = null;
		try {
			while ((seq = (SQ) seqFile.readSeq()) != null) {
				addSequence(seq);
			}
		} catch (IOException e) {
			System.err.println("Sequenc read error");
			System.exit(1);
		}
	}
	@SuppressWarnings("unchecked")
	void readFromFastaIndex(IndexedFastaFile fastaFile) {
		setIndexedFasta(fastaFile);
		// IndexedFasta file returns all seqs as RawSequenceDB object (not sequence string)
		for (SQ seq: (ArrayList<SQ>) fastaFile.getAllSequences()) {
			addSequence(seq);
		}
	}
	void setIndexedFasta(IndexedFastaFile _idxFasta) {
		idxFasta = _idxFasta;
		useFai = idxFasta.readFaiFile();
	}
	boolean isIndexed() {
		return useFai;
	}
	@SuppressWarnings("deprecation")
	void writeSeqData(String seqFile) {
		for (Sequence seq: seqData) {
			byte bval = 0;
			int i = 0;
			@SuppressWarnings("unused")
			boolean err = false;
			ArrayList<Byte> byteSeq = new ArrayList<Byte>();
			for (i = 0; i < seq.length(); i++) {
				char c = seq.charAt(i);
				switch (Character.toUpperCase(c)) {
				case 'A':
					break;
				case 'C':
					bval += 1;
					break;
				case 'G':
					bval += 2;
					break;
				case 'T':
					bval += 3;
					break;
				default:
					err = true;
				}
				bval <<= 2;
				if (i % 4 == 3) {
//System.out.println(i+" "+bval);
					byteSeq.add(new Byte(bval));
					bval = 0;
				}
			}
			System.out.println(byteSeq);
		}
		System.out.println("OK");
	}
	public void setCircular(String name, boolean isCircular) {
		idxFasta.setCircular(name, isCircular);
	}
	public static void main(String args[]) {
		String seqFile = args[0];
		String chrName = args[1];
		SeqData<DNASequence> genomeSeq = new SeqData<DNASequence>();
		FastaFile fastaFile = null;
		try {
			fastaFile = new FastaFile(seqFile);
		} catch (IOException e){
		}
		genomeSeq.readFromFasta(fastaFile);
		genomeSeq.writeSeqData(seqFile);
		Sequence seq = genomeSeq.getSequence(chrName);
		System.out.println(seq.createSubSequence(1,1000));
	}
}

