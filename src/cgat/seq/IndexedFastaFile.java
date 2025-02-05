package cgat.seq;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IndexedFastaFile extends FastaFile {

	private Logger logger = LogManager.getLogger(IndexedFastaFile.class);

	static interface type {
		int UNKNOWN = 0, DNA = 1, PROTEIN = 2;
	};
	private RandomAccessFile in;

	String linebuf;
	String titSepChar = " \t\n";
	File filename;
	int seqType = type.UNKNOWN;
	FaIndex faIndex;
	boolean createIndex = false;

	public IndexedFastaFile(String _filename) throws IOException {
		filename = new File(_filename);
		in = new RandomAccessFile(filename, "r");
		if (! readFaiFile()) {
			throw new IOException();
		}
	}

	public void dump() {
		for (String key: this.faIndex.faInfoHash.keySet()) {
			logger.debug("sp=" + key);
		}
	}


	public boolean readFaiFile(){
		String faiFilename = filename.toString() + ".fai";
		if (new File(faiFilename).exists() == false) {
			if (createIndex) {
				createIndexFile();
			} else {
				return false;
			}
		}
		if (readFaiFile(faiFilename)) {
			return true;
		}
		return false;
	}
	public boolean readFaiFile(String filename) {
		try {
			faIndex = FaIndex.readFaiFile(filename);
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	/* position is 0-based */
	public String getRegionSeq(String name, int begin, int end) {
System.err.println("##GetRegionSeq:"+name+" "+begin+" "+end);
System.err.println("#FF:"+faIndex+" "+name+" "+begin);
		long beg_pos = faIndex.getFilePosition(name, begin);
System.err.println("#begpos="+beg_pos);
		long end_pos = faIndex.getFilePosition(name, end);
		int readSize = (int) (end_pos - beg_pos);
System.err.println("readSize="+readSize);
		byte[] bdata = new byte[ readSize ];
		StringBuffer seq = new StringBuffer();
		try {
			in.seek(beg_pos);
			int read_len = in.read(bdata);
			for (byte b: bdata) {
				char c = (char) b;
				if (Character.isUpperCase(c) || Character.isLowerCase(c)) {
					seq.append(Character.toUpperCase(c));
				}
			}
		} catch (Exception e) {
		}
///		System.out.println(seq);
		return(seq.toString());
	}
	public long length(String name) {
		return(faIndex.getLength(name));
	}
	public boolean isCircular(String name) {
		return(faIndex.isCircular(name));
	}
	public void setCircular(String name, boolean isCircular) {
		faIndex.setCircular(name, isCircular);
	}
	public ArrayList<RawSequenceDB> getAllSequences() {
		ArrayList<RawSequenceDB> seqList = new ArrayList<RawSequenceDB>();
		SequenceDB.setDefaultFastaFile(this);
//		SequenceDB sss = new SequenceDB("");
		for (String name: faIndex.faInfoHash.keySet()) {
			RawSequenceDB seq = new RawSequenceDB(name);
			seqList.add(seq);
		}
		return(seqList);
	}
	public void createIndexFile() {
		StringBuffer namebuf = new StringBuffer();
		int seqlen = 0, linebases = 0, linewidth = 0;
		long offset = 0, pos = 0, prevpos = 0;

		try {
			in.seek(0);
			while ( (linebuf = in.readLine()) != null ) {
				if (linebuf.startsWith(">")) {
					if (seqlen > 0 && offset > 0) {
						System.out.println(namebuf+"\t"+seqlen+"\t"+offset+"\t"+linebases+"\t"+linewidth);
						namebuf.setLength(0);
					}
					super.extract_name(linebuf, namebuf);
					offset = prevpos = in.getFilePointer();
					seqlen = linebases = linewidth = 0;
				} else {
					int linebases0 = linebuf.length();
					seqlen += linebases0;
					if (linebases == 0) {
						linebases = linebases0;
					} else if (linebases != linebases0) {
					}
					pos = in.getFilePointer();
					int linewidth0 = (int) (pos - prevpos);
					if (linewidth == 0) {
						linewidth = linewidth0;
					} else if (linewidth != linewidth0) {
					}
					prevpos = pos;
				}
			}
			if (seqlen > 0 && offset > 0) {
				System.out.println(namebuf+" "+seqlen+" "+offset+" "+linebases+" "+linewidth);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String args[]) {
		String seqname = args[0];
		int begin_pos = Integer.parseInt(args[1]);
		int end_pos = Integer.parseInt(args[2]);
		String filename = args[3];

//		Integer ofs = new Integer(args[1]);
//		Integer len = new Integer(args[2]);
		IndexedFastaFile ff = null;
		try  {
			ff = new IndexedFastaFile(filename);
		} catch (Exception e) {
			System.err.println("Can't open file: " + filename);
			System.exit(1);
		}
		boolean useFai = ff.readFaiFile();
		String seq = ff.getRegionSeq(seqname, begin_pos, end_pos);
		System.out.println(seq);

/*
		try  {
//			Sequence seq = null;
//			while ( (seq = ff.readSeq(ofs, len)) != null) {
			Integer seqLen = null;
			while ( (seqLen = ff.countSeqLen()) != null) {
				System.out.println(seqLen);
			}
		} catch (Exception e) {
			System.err.println("Can't read file: " + filename);
			e.printStackTrace();
			System.exit(1);
		}
*/
	}
}

class FaIndex {
	HashMap<String,FastaInfo> faInfoHash;
	FaIndex() {
		faInfoHash = new HashMap<String,FastaInfo>();
	}
	static FaIndex readFaiFile(String file) throws IOException {
		FaIndex faIndex;
		try {
			faIndex = readFaiFile(new File(file));
		} catch (IOException e) {
			throw e;
		}
		return(faIndex);
	}
	static FaIndex readFaiFile(File file) throws IOException {
		FaIndex faIdx = new FaIndex();
		Reader reader;
		try {
			reader = new FileReader(file);
		} catch (IOException e) {
			throw e;
		}
		BufferedReader infile = new BufferedReader(reader);;
		String linebuf;
		try {
			while ( (linebuf = infile.readLine()) != null ) {
				String[] ldata = linebuf.split("\t");
				String name = ldata[0];
				long length = Integer.parseInt(ldata[1]);
				long offset = Long.parseLong(ldata[2]);
				int lbases = Integer.parseInt(ldata[3]);
				int lwidth = Integer.parseInt(ldata[4]);
				int circular = 0;
				if (ldata.length >= 6) {
					circular = Integer.parseInt(ldata[5]);
				}
				FastaInfo fInfo = new FastaInfo(
					name, length, offset, lbases, lwidth, circular);
				faIdx.faInfoHash.put(name, fInfo);
			}
		} catch (IOException e) {
			throw e;
		}
		return(faIdx);
	}
	/* position is 0-based */
	long getFilePosition(String seqname, int pos) {
		FastaInfo finfo = faInfoHash.get(seqname);
		int lines = pos / finfo.linebase;
		int addbases = pos % finfo.linebase;
		long fpos = pos + lines * (finfo.linewidth - finfo.linebase);
		return(finfo.offset + fpos);
	}
	long getLength(String seqname) {
		FastaInfo finfo = faInfoHash.get(seqname);
		return(finfo.length);
	}
	boolean isCircular(String seqname) {
		FastaInfo finfo = faInfoHash.get(seqname);
		return(finfo.circular);
	}
	void setCircular(String seqname, boolean isCircular) {
		FastaInfo finfo = faInfoHash.get(seqname);
		if (finfo != null) {
			finfo.circular = isCircular;
		} else {
			System.err.println("finfo is null: "+seqname);
		}
	}
}
class FastaInfo {
	String name ;
	long length, offset;
	int linebase, linewidth;
	boolean circular;
	FastaInfo(String _name, long _len, long _offset, int _lbase, int _lwidth, int _circular) {
		name = _name; length = _len; offset = _offset;
		linebase = _lbase; linewidth = _lwidth;
		circular = (_circular > 0);
	}
}
