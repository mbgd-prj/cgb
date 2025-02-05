package cgat.seq;

public class Alphabet {
	private String charset;
	private int [] toIdx;
	private int [] complementArray;
	private String [] names;
	private int charnum;
	private static final int NOT_FOUND = -1;
	private boolean ignoreCase = true;

	static Alphabet aminoAcids;
	static Alphabet nucleotides;
	static final String AMINO_CHARS = "ARNDCQEGHILKMFPSTWYVBZX*";
	static final String [] AMINO_NAMES = {
		"Ala","Arg","Asn","Asp","Cys","Gln","Glu","Gly","His","Ile",
		"Leu","Lys","Met","Phe","Pro","Ser","Thr","Trp","Tyr","Val",
		"Asx","Glx","Xaa","TER"
	};
	static final String NUCLEOTIDE_CHARS = "ATGCSWRYKMBVHDN";
	static final String[] NUCLEOTIDE_OPT = {
		"complement=TACGSWYRMKVBDHN",
		"origCharNum=4",
	};

	static final int NUMCHAR = 128;

	public Alphabet() {
	}
	public Alphabet(char [] charset) {
		init(new String(charset), null);
	}
	public Alphabet(String charset) {
		init(charset, null);
	}
	public Alphabet(String charset, String[] optionArgs) {
		init(charset, optionArgs);
	}

	/** static method to generate amino acid alphabets */
	public static Alphabet getAminoAcids() {
		if (aminoAcids == null) {
			aminoAcids = new Alphabet(AMINO_CHARS);
			aminoAcids.setNames(AMINO_NAMES);
		}
		return aminoAcids;
	}
	/** static method to generate nucleotide alphabets */
	public static Alphabet getNucleotides() {
		if (nucleotides == null) {
			nucleotides = new Alphabet(NUCLEOTIDE_CHARS,
							NUCLEOTIDE_OPT);
		}
		return nucleotides;
	}

	public void init(String _charset, String[] optionArgs) {
		charset = _charset;
		charnum = charset.length();
		toIdx = new int[NUMCHAR];
		for (int i = 0; i < NUMCHAR; i++) {
			toIdx[i] = NOT_FOUND;
		}
		for (int i = 0; i < charnum; i++) {
			char c = charset.charAt(i);
			toIdx[c] = i;
			if (ignoreCase) {
			    if (Character.isUpperCase(c)) {
			        toIdx[Character.toLowerCase(c)] = i;
			    } else if (Character.isLowerCase(c)) {
			        toIdx[Character.toUpperCase(c)] = i;
			    }
			}
		}

		String complementString = null;
		if (optionArgs != null) {
			for (int i = 0; i < optionArgs.length; i++) {
				String[] str = Utils.split(optionArgs[i], "=", 2);
				if (str[0].equals("complement")) {
					complementString = str[1];
				}
			}
		}
		if (complementString != null) {
			complementArray = new int[NUMCHAR];
			if (charnum != charset.length()) {
			}
			for (int i = 0; i < NUMCHAR; i++) {
				complementArray[i] = NOT_FOUND;
			}
			for (int i = 0; i < charnum; i++) {
				complementArray[i] =
					toIdx[complementString.charAt(i)];
			}
		}
	}
	/** set an alternative names (3 letter code) of the characters */
	public void setNames(String[] names) {
		this.names = names;
	}
	/** return an alternative name (3 letter code) of character c */
	public String getName(char c) {
		return names[toIdx[c]];
	}
	/** return an alternative name of character index at idx */
	public String getName(int idx) {
		return names[idx];
	}
	/** return a character index at idx */
	public char get(int idx) {
		return charset.charAt(idx);
	}
	/** return an index of a complement character
			of the character index at idx */
	public int complement(int idx) throws ArrayIndexOutOfBoundsException {
		if (complementArray == null) {
			return idx;
		}
		try {
			return complementArray[idx];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw e;
		}
	}
	/** return a complement character of c */
	public char complement(char c) throws ArrayIndexOutOfBoundsException {
		try {
			return charset.charAt( complement(toIdx[c]) );
		} catch (ArrayIndexOutOfBoundsException e) {
			throw e;
		}
	}
	/** return whether or not the character c is contained in the alphabet */
	public boolean contains(char c) {
		if (toIdx[c] == NOT_FOUND) {
			return false;
		} else {
			return true;
		}
	}
	/** return an index number of the character c */
	public int toIdx(char c) {
		return toIdx[c];
	}
	/** return the number of characters */
	public int charnum() {
		return charnum;
	}

	/*
	public static void main(String[] args) {
		Alphabet a = new Alphabet("ABCDE");
		Alphabet dna = Alphabet.getNucleotides();
		Alphabet aa = Alphabet.getAminoAcids();
		System.out.println(a.get(3));
		System.out.println(a.toIdx('E'));
		System.out.println(dna.toIdx('C'));
		System.out.println(aa.toIdx('C'));
		System.out.println(dna.toIdx('c'));

		System.out.println("A" + "->" + dna.complement('A'));
		System.out.println("C" + "->" + dna.complement('C'));
		System.out.println("G" + "->" + dna.complement('G'));
		System.out.println("T" + "->" + dna.complement('T'));
		System.out.println("N" + "->" + dna.complement('N'));
	}
	*/
}
