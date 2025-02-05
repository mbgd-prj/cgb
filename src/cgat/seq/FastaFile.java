package cgat.seq;
import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

public class FastaFile {
	static interface type {
		int UNKNOWN = 0, DNA = 1, PROTEIN = 2;
	};
	BufferedReader in;
	String linebuf;
	String titSepChar = " \t\n";
	File filename;
	int seqType = type.UNKNOWN;
/*
	FaIndex faIndex;
*/

	public FastaFile() throws IOException {
		try {
			setReader(new InputStreamReader(System.in));
		} catch (IOException e) {
			throw e;
		}
	}
	public FastaFile(String filename) throws IOException {
		try {
			Reader reader;
			if (filename.startsWith("http:") ||
			    filename.startsWith("https:") ||
				filename.startsWith("file:")) {
				URL url = new URL(filename);
				reader = new InputStreamReader(url.openStream());
			} else {
				reader = new FileReader(filename);
			}
			setReader(reader);
		} catch (IOException e) {
			throw e;
		} 
		this.filename = new File(filename);
	}
	protected void setReader(Reader r) throws IOException {
		setReader(new BufferedReader(r));
	}
	protected void setMark() throws IOException {
		try {
			in.mark(1024);
		} catch (IOException e) {
			throw e;
		}

	}
	protected void setReader(BufferedReader r) throws IOException {
		in = r;
		setMark(); // MARK file-pointer at head of this file.
	}
	/** Specify a set of characters that separate an entry name and
		an additional description on a title line */
	public void setTitSepChar(String sep){
		titSepChar = sep;
	}
	/** specify the sequences contained are DNA sequences */
	public void asDNA(){
		seqType = type.DNA;
	}
	/** specify the sequences contained are protein sequences */
	public void asProtein(){
		seqType = type.PROTEIN;
	}
	/** return true if the sequences contained are
			treated as DNA sequences */
	public boolean isDNA(){
		return (seqType == type.DNA);
	}
	/** return true if the sequences contained are
			treated as protein sequences */
	public boolean isProtein(){
		return (seqType == type.PROTEIN);
	}
	public RawSequence readSeq() throws IOException {
		return readSeq(null, null);
	}
	public Integer countSeqLen() throws IOException {
		boolean readflag = false;
		int readSize = 0;

		try {
			while ( (linebuf = in.readLine()) != null ) {
				if (linebuf.startsWith(">")) {
					if (readflag) {
						return new Integer(readSize);
					}
				} else {
					linebuf.trim();
					readflag = true;
					readSize += linebuf.length();
				}
			}
			if (readflag) {
				return new Integer(readSize);
			} else {
				return null;
			}
		} catch (IOException e) {
			if (readflag) {
				return new Integer(readSize);
			} else {
				throw e;
			}
		}
	}
	public RawSequence readSeq(int ofs, int len) throws IOException {
		return readSeq(new Integer(ofs), new Integer(len));
	}
	public RawSequence readSeq(Integer ofs, Integer len) throws IOException {
		boolean readflag = false;
		StringBuffer seqbuf = new StringBuffer();
		StringBuffer namebuf = new StringBuffer();
		int readSize = 0;

		if ((ofs != null) && (ofs.intValue() < 0)) {
			ofs = null;
		}
		if (((len != null) && len.intValue() <= 0)) {
			len = null;
		}

		if (linebuf != null && linebuf.startsWith(">")) {
			extract_name(linebuf, namebuf);
		}
		try {
			while ( (linebuf = in.readLine()) != null ) {
				if (linebuf.startsWith(">")) {
					if (readflag) {
						return(createSeq(namebuf, seqbuf));
					} else {
						extract_name(linebuf, namebuf);
					}
				} else {
					linebuf.trim();
					readflag = true;
					linebuf=linebuf.toUpperCase();
					if ((ofs == null) || (len == null)) {
						seqbuf.append(linebuf);
					}
					else if ((ofs.intValue() <= readSize + linebuf.length()) &&
					         (readSize + linebuf.length() < ofs.intValue() + len.intValue())) {
						int start = ofs.intValue() - readSize;
						if (start < 0) {
							start = 0;
						}
						seqbuf.append(linebuf.substring(start));
						if (len.intValue() <= seqbuf.length()) {
							seqbuf.setLength(len.intValue());
						}
					}
					readSize += linebuf.length();
				}
			}
			if (readflag) {
				return(createSeq(namebuf, seqbuf));
			} else {
				return null;
			}
		} catch (IOException e) {
			if (readflag) {
				return(createSeq(namebuf, seqbuf));
			} else {
				throw e;
			}
		}
	}
/***
	public boolean readFaiFile(){
		String faiFilename = filename.toString() + ".fai";
		if (new File(faiFilename).exists()) {
			readFaiFile(faiFilename);
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
*/
	private RawSequence createSeq(StringBuffer name, StringBuffer seq) {
		if (isDNA()) {
			return (RawSequence)
				new DNASequence(name.toString(), seq.toString());
		} else if (isProtein()) {
			return (RawSequence)
				new ProteinSequence(name.toString(), seq.toString());
		} else {
			return new RawSequence(name.toString(), seq.toString());
		}
	}
	protected void extract_name(String linebuf, StringBuffer namebuf) {
		int i;
		for (i = 1; Character.isWhitespace(linebuf.charAt(i)); i++) {
		}
		String [] str = Utils.split(linebuf.substring(i), titSepChar, 1);
		namebuf.append(str[0]);
	}
	public void close() throws IOException {
		in.close();
	}
	public static void main(String args[]) {
		String filename = args[0];
//		Integer ofs = new Integer(args[1]);
//		Integer len = new Integer(args[2]);
		FastaFile ff = null;
		try  {
			ff = new FastaFile(filename);
		} catch (Exception e) {
			System.err.println("Can't open file: " + filename);
			System.exit(1);
		}
/*
		boolean useFai = ff.readFaiFile();
*/

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
	}
}

/*
class FaIndex {
	ArrayList<FastaInfo> faInfoList;
	FaIndex() {
		faInfoList = new ArrayList<FastaInfo>();
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
				FastaInfo fInfo = new FastaInfo(
					name, length, offset, lbases, lwidth);
				faIdx.faInfoList.add(fInfo);
			}
		} catch (IOException e) {
			throw e;
		}
		return(faIdx);
	}
}
class FastaInfo {
	String name ;
	long length, offset;
	int linebase, linewidth;
	FastaInfo(String _name, long _len, long _offset, int _lbase, int _lwidth) {
		name = _name; length = _len; offset = _offset;
		linebase = _lbase; linewidth = _lwidth;
	}
}
*/
