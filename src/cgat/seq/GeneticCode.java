package cgat.seq;
import java.io.Serializable;
import java.util.HashMap;
public class GeneticCode implements Serializable {
	private static final long serialVersionUID = 7156475111489615770L;

	static String[] nt = {
		"TTT","TTC","TTA","TTG","TCT","TCC","TCA","TCG",
		"TAT","TAC","TAA","TAG","TGT","TGC","TGA","TGG",
		"CTT","CTC","CTA","CTG","CCT","CCC","CCA","CCG",
		"CAT","CAC","CAA","CAG","CGT","CGC","CGA","CGG",
		"ATT","ATC","ATA","ATG","ACT","ACC","ACA","ACG",
		"AAT","AAC","AAA","AAG","AGT","AGC","AGA","AGG",
		"GTT","GTC","GTA","GTG","GCT","GCC","GCA","GCG",
		"GAT","GAC","GAA","GAG","GGT","GGC","GGA","GGG",
			};

	static int tablenum = 17;
	static int[] id = { 1,2,3,4,5,6,9,10,11,12,13,14,15,16,21,22,23,};
	static String[] name = {
	  "Standard",
	  "Vertebrate Mitochondrial",
	  "Yeast Mitochondrial",
	  "Mold Mitochondrial; Protozoan Mitochondrial; Coelenterate Mitochondrial; Mycoplasma; Spiroplasma",
	  "Invertebrate Mitochondrial",
	  "Ciliate Nuclear; Dasycladacean Nuclear; Hexamita Nuclear",
	  "Echinoderm Mitochondrial; Flatworm Mitochondrial",
	  "Euplotid Nuclear",
	  "Bacterial and Plant Plastid",
	  "Alternative Yeast Nuclear",
	  "Ascidian Mitochondrial",
	  "Alternative Flatworm Mitochondrial",
	  "Blepharisma Macronuclear",
	  "Chlorophycean Mitochondrial",
	  "Trematode Mitochondrial",
	  "Scenedesmus obliquus mitochondrial",
	  "Thraustochytrium mitochondrial code",
	};
	static String[] aminostr = {
	  "FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSS**VVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CCWWTTTTPPPPHHQQRRRRIIMMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSSSSVVVVAAAADDEEGGGG",
	  "FFLLSSSSYYQQCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNNKSSSSVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CCCWLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CC*WLLLSPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSSGGVVVVAAAADDEEGGGG",
	  "FFLLSSSSYYY*CCWWLLLLPPPPHHQQRRRRIIIMTTTTNNNKSSSSVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY*QCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY*LCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNNKSSSSVVVVAAAADDEEGGGG",
	  "FFLLSS*SYY*LCC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	  "FF*LSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG",
	};
	static String[] startstr = {
	  "---M---------------M---------------M----------------------------",
	  "--------------------------------MMMM---------------M------------",
	  "----------------------------------MM----------------------------",
	  "--MM---------------M------------MMMM---------------M------------",
	  "---M----------------------------MMMM---------------M------------",
	  "-----------------------------------M----------------------------",
	  "-----------------------------------M---------------M------------",
	  "-----------------------------------M----------------------------",
	  "---M---------------M------------MMMM---------------M------------",
	  "-------------------M---------------M----------------------------",
	  "-----------------------------------M----------------------------",
	  "-----------------------------------M----------------------------",
	  "-----------------------------------M----------------------------",
	  "-----------------------------------M----------------------------",
	  "-----------------------------------M---------------M------------",
	  "-----------------------------------M----------------------------",
	  "--------------------------------M--M---------------M------------",
	};

	static GeneticCode[] generated = new GeneticCode[tablenum];
	HashMap amino, start;
	private GeneticCode(HashMap _amino, HashMap _start){
		amino = _amino;
		start = _start;
	}
	public static GeneticCode generate(int tabid){
		int i, j;
		if (generated[tabid] != null) {
			return generated[tabid];
		}
		for (i = 0; i < tablenum; i++) {
			if (id[i] == tabid) {
				HashMap _amino = new HashMap();
				HashMap _start = new HashMap();
				for (j = 0; j < 64; j++) {
					_amino.put(nt[j], new Character(
						aminostr[i].charAt(j)));
					_start.put(nt[j], new Character(
						startstr[i].charAt(j)));
				}
				generated[i] = new GeneticCode(_amino, _start);
				return generated[i];
			}
		}
		return null;
	}
	public char getAmino(String nt) {
		return (((Character)amino.get(nt)).charValue());
	}
	public char getStart(String nt) {
		return (((Character)start.get(nt)).charValue());
	}
	public boolean isStart(String nt) {
		return (getStart(nt) != '-');
	}
}
