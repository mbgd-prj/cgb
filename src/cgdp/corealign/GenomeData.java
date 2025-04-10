package cgdp.corealign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgat.seq.DNASequence;
import cgat.seq.Sequence;
import cgdp.dialog.AddGroupDialog.GroupInfo;
import lombok.Data;

public class GenomeData {
	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(GenomeData.class);

	GeneData genes;
	private HashMap<String, Genome> genomes;
	public LinkedList<String> specList;
	private static GenomeData _genomeData = null;

	enum ChrLenMode {GeneCnt, Length};
	private ChrLenMode chrLenEstim;

	String genomeDisplayName = "dispname";
	boolean seqAvail;
	GenomeSeq genomeSeq;

	GenomeData() {
		genomes = new HashMap<String, Genome>();
		genes = new GeneData();
		specList = new LinkedList<String>();
 		chrLenEstim = ChrLenMode.GeneCnt;
	}

	public void dump() {
		logger.debug("--- GenomeData start ---");
		this.genes.dump();
		logger.debug("--- GenomeData finish ---");
	}

	/**
	 * 別パッケージからGeneをアクセスできないためこのクラスを使用する。
	 */
	@Data
	public static class GeneInfo implements GroupInfo {
		private boolean select;
		private String sp;
		private String name;
		private float pos;
		private int dir;
		private int seqno;
		private int len;
		private int coreid1;
		private int coreid2;

		/**
		 * コンストラクタ。
		 * @param gene Geneのインスタンス。
		 */
		public GeneInfo(final Gene gene) {
			this.sp = gene.sp;
			this.name = gene.name;
			this.pos = gene.pos;
			this.dir = gene.dir;
			this.seqno = gene.seqno;
			this.len = gene.len;
			this.coreid1 = gene.coreid1;
			this.coreid2 = gene.coreid2;
		}
	}

	public List<GeneInfo> searchGene(final String pattern) {
		Pattern p = Pattern.compile(pattern);
		List<GeneInfo> ret = new ArrayList<GeneInfo>();
		List<Gene> list = this.genes.geneList;
		for (Gene g: list) {
			Matcher m = p.matcher(g.name);
			if (m.find()) {
				ret.add(new GeneInfo(g));
			}
		}
		return ret;
	}

	public static void clear() {
		GenomeData._genomeData = null;
	}

	static GenomeData getInstance() {
		if (_genomeData == null) {
			_genomeData = new GenomeData();
		}
		return _genomeData;
	}
	Genome getGenome(String sp) {
		Genome genome = genomes.get(sp);
		if (genome == null) {
			genome = new Genome(sp, "", 0);
			genomes.put(sp, genome);
		}
		return genome;
	}
	Chromosome addAndGetChromosome(String sp, int seqno) {
		Genome genome = getGenome(sp);
		return(genome.addAndGetChromosome(seqno));
	}
	void setGenomeDisplayName(String name) {
		genomeDisplayName = name;
	}
	String getGenomeDisplayName() {
		return(genomeDisplayName);
	}
	static GenomeData readFromFile(String filename, boolean domclustIn) throws IOException {
		GenomeData gdata;
		if (domclustIn) {
			gdata = readFromDomClustGeneFile(filename);
		} else {
			gdata = readFromFile(filename);
		}
		return(gdata);
	}
	static GenomeData readFromFile(String filename) throws IOException {
		GenomeData gdata = GenomeData.getInstance();

		File filepath = new File(filename);
		String dirname = filepath.getParent();
		if (dirname == null) {
			dirname = ".";
		}

		try (BufferedReader reader = new BufferedReader( new FileReader(filename) )) {
			String linebuf;
			while ( (linebuf = reader.readLine()) != null ) {
				if (linebuf.charAt(0) == '#') {
					continue;
				}
				String[] fields = linebuf.split("[ \t]");
				String spcode = fields[0];
				String spname = fields[1];
				int len = Integer.valueOf(fields[2]).intValue();
				Genome genome = new Genome(spcode,spname,len);
				/* read and add gene info */
				gdata.genes.readSpFromFile(dirname, spcode, gdata);
				gdata.specList.add(spcode);
				gdata.genomes.put(spcode, genome);
			}
		} catch (IOException e) {
			throw e;
		}
		gdata.setChromosomeLengthFromGeneData_All();
		return(gdata);
	}
	static GenomeData readFromDomClustGeneFile(String filename) throws IOException {
		GenomeData gdata = GenomeData.getInstance();
		try {
			gdata.genes.readFromDomClustGeneFile(filename, gdata);
		} catch (IOException e) {
			throw e;
		}
		gdata.setChromosomeLengthFromGeneData_All();
		return(gdata);
	}
	void readAltGeneName(String filename) throws IOException {
		try {
			genes.readAltGeneName(filename);
		} catch (IOException e) {
			throw e;
		}
	}
	int getMeanChromosomeLength() {
		int tot_chrlen = 0, chrnum = 0;
		for (String spec: specList) {
			Genome g = genomes.get(spec);
			for (Chromosome c: g.chromosomes) {
				int clen = c.getLength();
				tot_chrlen += clen;
				chrnum++;
			}
		}
		return( (int) tot_chrlen / chrnum );
	}
	void setChromosomeLengthFromGeneData_All() {
		for (String spec: specList) {
			Genome g = genomes.get(spec);
			setChromosomeLengthFromGeneData(g);
		}
	}
	void setChromosomeLengthFromGeneData(Genome genome) {
		logger.info("*** genome.chromosomes = " + genome.chromosomes.size());
		for (Chromosome c: genome.chromosomes) {
			float chrMaxPos = 0;
			int chrMaxPosLen = 0;
			for (Gene g: c.genes) {
				if (chrMaxPos < g.pos) {
					chrMaxPos  = g.pos;
					chrMaxPosLen  = g.len;
				}
			}
			float maxpos = chrMaxPos;
			int maxposlen = chrMaxPosLen;
			if (maxpos < c.genes.size()*5) {
				int chrlen =  0;
				for (Gene g: c.genes) {
					g.pos = chrlen + g.len *  3 / 2;
					chrlen +=  g.len * 3;
				}
//				chrLenEstim = ChrLenMode.GeneCnt;
				chrLenEstim = ChrLenMode.Length;
				c.setLength(chrlen);
			} else {
				chrLenEstim = ChrLenMode.Length;
				if (c.getLength() == 0) {
					c.setLength(Math.round(maxpos + maxposlen*3/2));
				}
			}
		}
	}
	void setSequences(SeqData<DNASequence> genomeSeqData) {
		if (genomeSeqData.useFai) {
			/* indexed seqdata */
			genomeSeq = new GenomeSeq(genomeSeqData);
//			System.err.println("Use FAI index file");
			for (String spec: specList) {
				Genome g = genomes.get(spec);
				for (Chromosome c: g.chromosomes) {
					genomeSeqData.setCircular(c.getChrName(), c.isCircular());
				}
			}
		} else {
			/* no-indexed seqdata; assign chromosome all seqdata read in memroy  */
		    for (String spec: specList) {
			Genome g = genomes.get(spec);
			for (Chromosome c: g.chromosomes) {
				String chrName = spec + ":" + c.name;
				DNASequence seq = genomeSeqData.getSequence(chrName);
				if (seq == null) {
//					System.out.println("Sequence not found: "+ chrName);
					// skip
				} else {
					c.setSequence(seq);
				}
			}
		    }
		}
		seqAvail = true;
	}
	boolean isRealChrLen() {
		return (chrLenEstim == ChrLenMode.Length);
	}
	int specNum() {
		return specList.size();
	}
	public GenomicLocus correctLocus(GenomicLocus loc) {
//		System.out.println("loc="+loc);
		Genome genome = getGenome(loc.spec);
		Chromosome chrom = genome.getChromosome(loc.seqno);
		int pos = 0;
		System.out.println(loc);
/*
		if (loc.seqno > genome.chromosomes.size()) {
			loc.seqno = genome.chromosomes.size();
		} else if (loc.seqno <= 0) {
			loc.seqno = 1;
			loc.pos = 1;
		}
*/
		if (chrom.shape == ChrShape.circular) {
			pos = loc.pos % chrom.getLength();
			if (pos < 0) {
				pos += chrom.getLength();
			}
		} else {
			pos = loc.pos;
			if (pos <= 0) {
				pos = 1;
			} else if (pos > chrom.getLength()) {
				pos = chrom.getLength();
			}
		}
		return new GenomicLocus(loc.spec,loc.seqno,pos);
	}

	/** get chromosome info from locus */
	Chromosome getChromosomeByPos(GenomicLocus locus) {
		String refsp = locus.spec;
		int seqno = locus.seqno;

		Genome refGenome = getGenome(refsp);
		Chromosome chrom;
		if (seqno > 0) {
			chrom = refGenome.getChromosome(seqno);
		} else {
			chrom = refGenome.getMaxChromosome();
		}
		return(chrom);
	}
	/** get gene info from locus */
	Gene getGeneByPos(GenomicLocus locus) {
		GeneIdx geneIdx = getGeneIdxByPos(locus);
		Gene hitGene = geneIdx.getGene();
		return(hitGene);
	}
	/** get gene index on the chromosome from locus */
	public GeneIdx getGeneIdxByPos(GenomicLocus locus) {
//		String refsp = locus.spec;
		int seqno = locus.seqno;
		int pos = locus.pos;
		Chromosome chrom = getChromosomeByPos(locus);

		int idx = Collections.binarySearch(chrom.genes,
			new Gene(seqno,pos),
			new Comparator<Gene>() {
				public int compare(Gene g1, Gene g2) {
					return ( (g1.seqno ==  g2.seqno) ?
						(int)(g1.pos - g2.pos) :
						(g1.seqno - g2.seqno) );
				}
			}
		);
		if (idx < 0) {
			idx = -idx - 1;
		}
		if (idx >= chrom.genes.size()) {
			idx = chrom.genes.size() - 1;
		}
		Gene hitGene = chrom.genes.get(idx);
		if (pos < hitGene.getBegin() && idx > 0) {
			idx--;
		} else if (pos > hitGene.getEnd() && idx < chrom.genes.size()-1) {
			idx++;
		}
		GeneIdx geneIdx = new GeneIdx(chrom, idx);
		return(geneIdx);
	}


}

class Genome {
	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(Genome.class);
	String spcode;
	String name;
	int length;
	int numseq;
	ArrayList<Chromosome> chromosomes;
	HashMap<String, Chromosome> chrHash;
	GeneData genes;
	Genome(String _spcode, String _name, int _length) {
		this(_spcode, _name, _length, 1);
	}
	Genome(String _spcode, String _name, int _length, int _numseq) {
		spcode= _spcode; name = _name; length = _length; numseq = _numseq;
		chromosomes = new ArrayList<Chromosome>();
		chrHash = new HashMap<String,Chromosome>();
	}
	String getSpCode(){
		return(spcode);
	}
	String getName(){
		if (name != "") {
			return(name);
		} else {
			return(getSpCode());
		}
	}
	void setName(String _name) {
		name = _name;
	}
/**
	ChrShape getChromShape(int seqno) {
		if (chromosomes.size() > 0) {
			Chromosome chrom = chromosomes.get(seqno-1);
			return(chrom.shape);
		} else {
			return(Chromosome.defaultShape());
		}
	}
**/
	Chromosome getChromosome(int seqno) {
		return(chromosomes.get(seqno-1));
	}
	Chromosome getMaxChromosome() {
		int maxlen = -1;
		Chromosome maxChrom = null;
		for (Chromosome chrom: chromosomes) {
			int len = chrom.getLength();
			if (len > maxlen) {
				maxlen = len;
				maxChrom = chrom;
			}
		}
		return(maxChrom);
	}
	Chromosome addAndGetChromosome(int seqno) {
		Chromosome chr;
		String seqname = Integer.toString(seqno);
		if (chrHash.containsKey(seqname)) {
			chr = chrHash.get(seqname);
		} else {
			chr = new Chromosome();
//			chr.setChrName(spcode+":"+seqname);
			chr.setName(seqname);
			chr.setSeqNo(seqno);
//System.out.println("chr="+chr.getName());
			while (chromosomes.size() +1 < seqno) {
				Chromosome chr0 = new Chromosome();
				int sqn = chromosomes.size();
				chr0.setName(spcode+"-"+sqn);
				chromosomes.add(chr0);
			}
			chromosomes.add(chr);
			chrHash.put(seqname, chr);
		}
		return(chr);
	}
	void sortGenes() {
		for (Chromosome chrom: chromosomes) {
			chrom.sortGenes();
		}
	}
/*
	void addChromosome(Chromosome chr) {
		int previdx = chromosomes.size();
		String chrname = spcode+"-"+Integer.toString(previdx+1);
		chr.setName(chrname);
		chromosomes.add(chr);
	}
*/
}
class GenomeSeq {
	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(GenomeSeq.class);

	SeqData<DNASequence> genomeSeqData;
	GenomeSeq(SeqData<DNASequence> _genomeSeqData) {
		genomeSeqData = _genomeSeqData;
	}
	Sequence getSubSequence(String chrName, int from, int to, int dir) {
		logger.debug("*** from=" + from + ",to=" + to);
		String seqstr = genomeSeqData.getSubSeqString(chrName, from, to);
System.out.println("*** from=" + from + ",to=" + to);
		Sequence seq = new DNASequence(chrName, seqstr);
		if (dir < 0) {
			seq = seq.getReverse();
		}
		return(seq);
	}
	boolean isIndexed() {
		return(genomeSeqData.isIndexed());
	}
}
enum ChrShape {linear, circular};
class Chromosome {

	private Logger logger = LogManager.getLogger(Chromosome.class);

	ChrShape shape;
	static ChrShape _defaultShape = ChrShape.circular;
	int length;
	int seqno;
	String name;
	String chrName;
	ArrayList<Gene> genes;
	DNASequence seq;
	Chromosome() {
		this(defaultShape());
	}
	Chromosome(ChrShape _shape) {
		shape = _shape;
		if (shape == null){
			shape = defaultShape();
		}
		genes = new ArrayList<Gene>();
	}
	static ChrShape defaultShape() {
		return _defaultShape;
	}
	void setShape(ChrShape _shape) {
		shape = _shape;
	}
	ChrShape getShape() {
		return shape;
	}
	boolean isCircular() {
		return shape==ChrShape.circular;
	}
	static void setDefaultShape(ChrShape shape) {
		_defaultShape = shape;
	}
	void setName(String _name) {
		name = _name;
	}
	void setChrName(String _chrName) {
		chrName = _chrName;
	}
	String getName() {
		return(name);
	}
	String getChrName() {
		return(chrName);
	}
	void setSeqNo(int  _seqno) {
		seqno = _seqno;
	}
	int getSeqNo() {
		return(seqno);
	}
	int getSeqNo_0base() {
		return(seqno-1);
	}
	void setLength(int _length) {
		length = _length;
	}
	int getLength() {
		return(length);
	}
	void setSequence(DNASequence _seq) {
		seq = _seq;
//System.out.println(seq.getName()+" "+seq.length());
		seq.setCircular( shape == ChrShape.circular );
	}
	Sequence getSequence() {
		return(seq);
	}
	Sequence getSequence(int from, int to) {
		if (from > to && from - to < seq.length() / 2) {
			// taking reverse strand
			// if from - to > seqlen / 2, the segment is considered to contain origin
			return( getSequence(to, from, -1) );
		} else {
			return( getSequence(from, to, 1) );
		}
	}
	/** get sequence of the specified region in this chromosome */
	Sequence getSequence(int from, int to, int dir) {
		return(getSequence_addflank(from, to, dir, 0));
	}
	/** get sequence of the specified region that is extended up to the fixed size specified by flankwin argument */
	Sequence getSequence_addflank(int from, int to, int dir, int flankwin) {
		int centerPos = 0;
		if (from < to) {
			centerPos = (from + to) / 2;
		} else {
			centerPos = (from + to + getLength()) / 2;
		}
		int addwin = flankwin - (centerPos-from);
		if (addwin < 0) {
			addwin = 0;
		}
		return(getSequence_addwin(from, to, dir, addwin));
	}
	Sequence getSequence_addwin(int from, int to, int dir, int win) {
		if (seq == null) {
			return(null);
		}

		logger.debug("from=" + from + ", to=" + to);

		Sequence origseq = seq;
/*
		if (dir < 0) {
			origseq = seq.getReverse();
		}
*/
System.out.println("##OOOOO:"+from+" "+to+" "+dir+" "+win+" "+seq.isCircular());
if (win > 100000) {
	Exception e = new Exception();
	e.printStackTrace();
	System.exit(1);
}
		if ( seq.isCircular() ) {
			if ( (from > to && from - to < origseq.length() / 2) ||
				(from < to && to - from > origseq.length() / 2) ) {
				int tmp = from; from = to; to = tmp;
				dir = dir * -1;
			}
		}
		from = from - win;
		to = to + win;
System.out.println("##OOOOO2:"+from+" "+to+" "+dir+" "+win+" "+seq.isCircular());
		if ( ! seq.isCircular() ) {
			from = (from >= 0) ? from : 0;
			to = (to < seq.length()) ? to : seq.length();
		}

		// convert from 0-base to 1-base
		Sequence newseq = origseq.createSubSequence(from+1, to);
System.out.println("OOOOOOO:"+from+" "+to+" "+dir);
/*
		return( origseq.createSubSequence(from, to) );
*/
		if (dir < 0) {
			newseq = newseq.getReverse();
		}
		return( newseq );
	}
	void addGene(Gene g) {
		genes.add(g);
	}
	void sortGenes() {
		Collections.sort(genes,
			new Comparator<Gene>() {
				public int compare(Gene a, Gene b) {
					return (int)(a.pos - b.pos);
				}
			}
		);
	}
	Gene findGene(int pos) {
		for (Gene g: genes) {
			if (g.getBegin() <= pos && pos <= g.getEnd()) {
				return(g);
			}
		}
		return(null);
	}
	SeqRegion asSeqRegion() {
		return( new SeqRegion(1, length) );

	}
	public String toString() {
		return(name+" "+shape+" "+length);
	}
}
class GeneIdx {

	private static Logger logger = LogManager.getLogger(GeneIdx.class);

	Chromosome chrom;
	int geneIdx;
	GeneIdx(Chromosome _chrom, int _geneIdx) {
		chrom = _chrom; geneIdx = _geneIdx;
	}
	Gene getGene() {
		return(chrom.genes.get(geneIdx));
	}

	public void dump() {
		logger.debug("GeneIdx:" + this.chrom.chrName + "," + this.geneIdx);
	}
}

class SpGroup {
	HashMap<String, String> spGrpHash;
	SpGroup(String groupSpec) {
		spGrpHash = new HashMap<String, String>();
		setSpGroup(groupSpec);
	}
	void setSpGroup(String groupSpec) {
		String[] gspec_all = groupSpec.split(",");
		for (int i = 0; i < gspec_all.length; i++) {
			String[] gspec = gspec_all[i].split(":");
			for (int j = 0; j < gspec.length; j++) {
				spGrpHash.put(gspec[j], gspec[0]);
			}
		}
	}
	String getSpGroup(String spname) {
		String spgrpName;
		if (spGrpHash.containsKey(spname)) {
			spgrpName = (String) spGrpHash.get(spname);
		} else {
			spgrpName = spname;
		}
		return(spgrpName);
	}
}
class SpGroupCounter {
	SpGroup spGroup;
	HashSet<String> spGrpFound;
	SpeciesList species;
/*
	String[] species;
*/
	SpGroupCounter(SpGroup spGrp, String[] spec) {
		spGroup = spGrp;
		species = new SpeciesList(spec);
/*
		species = spec;
*/
		init();
	}
	SpGroupCounter(SpGroup spGrp, SpeciesList spec) {
		spGroup = spGrp;
		species = spec;
	}
	void init() {
		spGrpFound = new HashSet<String>();
	}
	void found(int spNum) {
		if (species == null) {
			System.err.println("fatal error: spgroupCounter");
		} else {
			found(species.get(spNum));
		}
	}
	void found(String spname) {
		String spgrp = spGroup.getSpGroup(spname);
		spGrpFound.add(spgrp);
	}
	int count() {
		return(spGrpFound.size());
	}
}

/* class for storing a locus with three tuples {spec, seqno, position} */
class GenomicLocus {
	String spec = null;
	int seqno = -1;
	int pos = -1;
	GenomicLocus(String _spec, int _seqno, int _pos) {
		spec = _spec;
		seqno = _seqno;
		pos = _pos;
	}
	GenomicLocus (String locstr) {	/* seqno:pos */
		String[] loc = locstr.split(":");
		if (loc.length == 3) {
			spec = loc[0];
			seqno = Integer.parseInt(loc[1]);
			pos = Integer.parseInt(loc[2]);
		} else if (loc.length == 2) {
			try {
				seqno = Integer.parseInt(loc[0]);
			} catch (Exception e) {
				spec = loc[0];
				seqno = 1;
			}
			pos = Integer.parseInt(loc[1]);
		} else {
			// seqno information is missing
			seqno = -1;
			pos = Integer.parseInt(loc[0]);
		}
	}
	void fillMissingInfo(String refsp, GenomeData gdata) {
		if (spec == null) {
			spec = refsp;
		}
		assignMaxChrom(gdata);
	}
	void assignMaxChrom(GenomeData gdata) {
		if (seqno <= 0)  {
			Genome genome = gdata.getGenome(spec);
			Chromosome chrom = genome.getMaxChromosome();
			seqno = chrom.seqno;
		}
	}
	Gene getGene(GenomeData gdata) {
		Genome genome = gdata.getGenome(spec);
		Chromosome chrom = genome.getChromosome(seqno);
		return chrom.findGene(pos);

	}
	String getSpecies() {
		return(spec);
	}
	int getSeqNo_0base(){
		return seqno - 1;
	}
/**
	int getPos0(){
		return(pos - 1);
	}
**/
	public String toString() {
		return (spec+":"+seqno+":"+pos);
	}
}

class GenomicRegion extends SeqRegion {
	/**
	 * Logger.
	 */
	private Logger logger = LogManager.getLogger(GenomicRegion.class);

	String spec = null;
	int seqno = -1;
	SeqRegion reg;
	int dir = 1;
	GenomeData gdata;
	Chromosome chrom;
	GenomicRegion() {
		SeqRegion.zeroBased();
	}
	GenomicRegion(String _spec, int _seqno, int begin, int end, int _dir) {
		this(_spec, _seqno, begin, end, _dir, null);
	}
	GenomicRegion(String _spec, int _seqno, int begin, int end, int _dir, GenomeData _gdata) {
		spec = _spec;
		seqno = _seqno;
		dir = _dir;
		reg = new SeqRegion(begin, end);
		if (_gdata != null) {
			gdata = _gdata;
			setGenomeData(_gdata);
		}
		SeqRegion.zeroBased();
	}
	void setGenomeData(GenomeData gdata) {
		Genome genome = gdata.getGenome(spec);
		chrom = genome.getChromosome(seqno);
	}
	Sequence getSequence(GenomeData gdata) {
		return(getSequence_addflank(gdata, 0));
	}
	Sequence getSequence_addflank(GenomeData _gdata, int addflank) {
		setGenomeData(_gdata);
		return(getSequence_addflank(addflank));
	}
	Sequence getSequence() {
		logger.debug("getSequence=");
		return(getSequence_addflank(0));
	}
	Sequence getSequence_addflank(int addflank) {
		logger.debug("addflank=" + addflank +", spec=" + spec + ", seqno=" + seqno);

/*
		Genome genome = gdata.getGenome(spec);
		Chromosome c = genome.getChromosome(seqno);
*/

		if (gdata.genomeSeq != null && gdata.genomeSeq.isIndexed()) {
			// use indexed fasta file
			logger.debug("###GETSEQ_REG="+spec+" "+chrom.getChrName()+" "+reg+" "+gdata.genomeSeq);
			String chrName = chrom.getChrName();
			return(gdata.genomeSeq.getSubSequence(chrName, reg.begin0(), reg.end0(), dir));
		} else {
			return( chrom.getSequence_addflank(reg.begin0(), reg.end0(), dir, addflank) );
		}
	}
	boolean contains(GenomicLocus loc) {
		if (spec.equals(loc.spec) && seqno == loc.seqno &&
			begin0() <= loc.pos && loc.pos < end0()) {
			return(true);
		} else {
			return(false);
		}
	}
	int getSeqNo_0base(){
		return seqno - 1;
	}
	int begin() {
		return reg.begin();
	}
	int end() {
		return reg.end();
	}
	int begin0() {
		return reg.begin0();
	}
	int end0() {
		return reg.end0();
	}
	int length() {
		if (gdata == null) {
			return Math.abs(reg.length());
		} else {
			int length =  reg.length();
			if (length < 0) {
				Genome genome = gdata.getGenome(spec);
				Chromosome c = genome.getChromosome(seqno);
				if (c.isCircular() && Math.abs(length) > c.getLength() / 2) {
					length = c.getLength() + length;
				} else {
					length = -length;
				}
			}
			return(length);
		}
	}
	GenomicLocus beginLocus() {
		return new GenomicLocus(spec, seqno, begin0());
	}
	GenomicLocus endLocus() {
		return new GenomicLocus(spec, seqno, end());
	}
	SeqRegion getSeqRegion() {
		return reg;
	}
	int regionDiffBegin(GenomicRegion reg) {
		return( regionDiff_sub(begin(), reg.begin()) );
	}
	int regionDiffEnd(GenomicRegion reg) {
		return( regionDiff_sub(end(), reg.end()) );
	}
	private int regionDiff_sub(int pos1, int pos2) {
		int diff = pos1 - pos2;
		if ( chrom != null && diff > chrom.getLength() ) {
			diff = diff % chrom.getLength();
		}
		return(diff);
	}
	public String toString() {
		return (spec+":"+seqno+":"+reg);
	}
}

class GeneData {

	private Logger logger = LogManager.getLogger(GeneData.class);

	LinkedList<Gene> geneList;
	HashMap<String, Gene> nameHash;
	static final String dirname = "gene";
	static final String fileSuffix = ".txt";
	static final String fileSuffix2 = ".dat";
	GeneData() {
		geneList = new LinkedList<Gene>();
		nameHash = new HashMap<String, Gene>();
	}

	public void dump() {
		for (Gene g: this.geneList) {
			logger.debug("sp=" + g.sp + ", name=" + g.name + ", pos=" + g.pos + ", dir=" + g.dir + ", seqno=" + g.seqno + ", len=" + g.len + ", coreid1=" + g.coreid1 + ", coreid2=" + g.coreid2);
		}
	}

	void printAll() {
		Iterator<Gene> iter = geneList.iterator();
		while (iter.hasNext()) {
			Gene gene = iter.next();
			System.out.println(gene);
		}
	}
	void readSpFromFile(String basedir, String spname, GenomeData gdata)
			throws IOException, RuntimeException {
		String filename = basedir + "/" + dirname + "/" + spname + fileSuffix;
		File file = new File(filename);
		if (! file.exists()) {
			filename = basedir + "/" + dirname + "/" + spname + fileSuffix2;
		}
		readFromFile(filename, gdata);
	}

	void readFromFile(String filename, GenomeData gdata)
			throws IOException, RuntimeException {
		try (BufferedReader reader = new BufferedReader( new FileReader(filename) )) {
			String linebuf;
			int linenum = 0;
			String field[];
			int spIdx = -1, nameIdx = -1, seqIdx = -1, typeIdx = -1,
				chridIdx = -1, fromIdx = -1, toIdx = -1, dirIdx = -1;
//			HashMap hash = new HashMap();
			int /*chrid,*/ from, to, dir, seqno = 1, prev_seqno = 0;
			String sp, name, type;
			Chromosome chr = null;
			while ( (linebuf = reader.readLine()) != null ) {
				linebuf.trim();
				field = linebuf.split("\t");
				if(++linenum==1) {
					for (int i=0; i<field.length; i++){
						if (field[i].equals("sp")){
							spIdx = i;
						} else if (field[i].equals("name")){
							nameIdx = i;
						} else if (field[i].equals("seqno")){
							seqIdx = i;
						} else if (field[i].equals("chrid")){
							chridIdx = i;
						} else if (field[i].equals("from1")){
							fromIdx = i;
						} else if (field[i].equals("to1")){
							toIdx = i;
						} else if (field[i].equals("dir")){
							dirIdx = i;
						} else if (field[i].equals("type")){
							typeIdx = i;
						}
					}
					if (nameIdx<0||fromIdx<0 ||toIdx < 0) {
						throw new RuntimeException("field not defined");
					}
					continue;
				}
				from = Integer.valueOf(field[fromIdx]);
				to = Integer.valueOf(field[toIdx]);
				sp = field[spIdx];
				name = field[nameIdx];
				dir = Integer.valueOf(field[dirIdx]);
				if (typeIdx >= 0) {
					type = field[typeIdx];
					if (! type.equals("CDS")) {
						continue;
					}
				}
				if (seqIdx >= 0) {
					seqno = Integer.valueOf(field[seqIdx]);
				} else if (chridIdx >= 0) {
					seqno = Integer.valueOf(field[chridIdx]);
				}
				int pos = (from + to) / 2;
				int len = (to - from + 1);
				Gene gene = new Gene(sp, name, seqno, pos, len, dir);
				/* add gene data */
				geneList.add(gene);
				nameHash.put(sp+":"+name, gene);
				if (chr == null || seqno != prev_seqno) {
//					addChromosome(gdata.getGenome(sp), null);
					if (chr != null) {
						chr.sortGenes();
					}
					chr = gdata.addAndGetChromosome(sp, seqno);
					prev_seqno = seqno;
				}
				chr.addGene(gene);
			}
			if (chr != null) {
				chr.sortGenes();
			}
		} catch (IOException e) {
			throw e;
		}
	}
	void readFromDomClustGeneFile(String filename, GenomeData gdata)
			throws IOException, RuntimeException {
		HashMap<String, Integer> seqnoHash = new HashMap<String, Integer>();
		try (BufferedReader reader = new BufferedReader( new FileReader(filename) )) {
			String linebuf;
			String sp = null, prevsp = null, name;
			int len, dir;
			float pos;
			int seqno = 1;
			String field[];
			String chrName = null;
			int chrLen = 0;
//			int spno = 0;
			boolean chrom_defined = false;
			Chromosome chr = null;
			while ( (linebuf = reader.readLine()) != null ) {
				if (linebuf.charAt(0) == '#') {
					if (linebuf.charAt(1) == '#') {
						if (linebuf.startsWith("##Genome")) {
							HashMap<String,String> genomeInfo = readInfo(linebuf);
							String spcode = genomeInfo.get("sp");
							Genome genome = gdata.getGenome(spcode);
							logger.info("... genome.chromosomes.size() = " + genome.spcode + ", " + genome.chromosomes.size());

							String dispName = gdata.getGenomeDisplayName();
							if (dispName!=null && genomeInfo.get(dispName)!=null){
								genome.setName(genomeInfo.get(dispName));
							}
							chrName = null;
						} else if (linebuf.startsWith("##Chromosome")) {
							HashMap<String,String> chromoInfo = readInfo(linebuf);
							chrName = chromoInfo.get("name");
							chrName = chrName.replaceAll(" ","_");
							if (chromoInfo.containsKey("seq_length")) {
								chrLen = Integer.valueOf( chromoInfo.get("seq_length") );
							}
						}
					} else if (sp != null) {
						// chromosome shape (1: linear, 2: circular) is added to chromlist in genome
						ChrShape shape = (linebuf.charAt(1) == '2') ? ChrShape.circular : ChrShape.linear;
//						addChromosome(gdata.getGenome(sp), shape);
						chr.setShape(shape);
						chrom_defined = true;

						seqnoHash.put(sp, ++seqno);
					}
					continue;
				} else if (linebuf.charAt(0) == '/') {
					continue;
				}
				field = linebuf.split("\\s");
				if (field.length < 2) {
					continue;
				}
				try {
					sp = field[0];
					name = field[1];
					len = Integer.valueOf(field[2]);
					pos = Float.valueOf(field[3]);
					dir = Integer.valueOf(field[4]);
				} catch (Exception e) {
					System.err.println("read error: "+ linebuf);
					throw(e);
				}
				if (prevsp == null || prevsp.compareTo(sp) != 0) {
					// no chromosome boundary mark '#'
					if (! seqnoHash.containsKey(sp)) {
						seqno = 1;
						seqnoHash.put(sp, seqno);
						gdata.specList.add(sp);
					} else {
						seqno = seqnoHash.get(sp);
					}
					if (prevsp != null && ! chrom_defined) {
//						addChromosome(gdata.getGenome(prevsp), Chromosome.defaultShape());
					}
				}
				prevsp = sp;
				chrom_defined = false;
				Gene gene = new Gene(sp, name, seqno, pos, len, dir);
				/* add gene data */
				geneList.add(gene);
				nameHash.put(sp+":"+name, gene);
				chr = gdata.addAndGetChromosome(sp, seqno);
				if (chrName != null) {
					chr.setName(chrName);
					chr.setChrName(sp+":"+chrName);
				}
				if (chrLen > 0) {
					chr.setLength(chrLen);
					chrLen = 0;
				}
				chr.addGene(gene);
			}
		} catch (IOException e) {
			throw e;
		}
		for (String sp: seqnoHash.keySet()) {
			int numseq = seqnoHash.get(sp);
			Genome genome = gdata.getGenome(sp);
			genome.numseq = numseq;
			genome.sortGenes();
			logger.info("+++ genome.chromosomes.size() = " + genome.spcode + ", " + genome.chromosomes.size());
		}
	}
	private HashMap<String,String> readInfo(String line) {
		HashMap<String,String> hash = new HashMap<String,String>();
		String[] data = line.split("\t");
		hash.put("header", data[0]);
		for (int i = 1; i < data.length; i++) {
			String[] keyval = data[i].split(":", 2);
			hash.put(keyval[0], keyval[1]);
		}
		return hash;
	}
	void readAltGeneName(String filename)
			throws IOException, RuntimeException {
//		HashMap<String,String> hashMap = new HashMap<String,String>();
		try (BufferedReader reader = new BufferedReader( new FileReader(filename) )) {
			String linebuf;
//			int linenum = 0;
			String field[];
			while ( (linebuf = reader.readLine()) != null ) {
				field = linebuf.split("\\s");
				if (field.length<1) {
					continue;
				}
				Gene g = (Gene) nameHash.get(field[0]);
				if (g != null) {
					for (int i = 1; i < field.length; i++) {
						String altname = g.sp+":"+field[i];
						nameHash.put(altname, g);
					}
				}
			}
		} catch (IOException e) {
			throw e;
		}
	}
/*
	private void addChromosome(Genome genome, ChrShape shape) {
		Chromosome chrom = new Chromosome(shape);
		genome.addChromosome(chrom);
	}
*/

	Gene getGene(String sp, String name) {
		return getGene(sp+":"+name);
	}
	Gene getGene(String spname) {
		return (Gene) nameHash.get(spname);
	}
}
class Gene implements Serializable {
	private static final long serialVersionUID = 2241854671050386513L;

	/**
	 * logger.
	 */
	private Logger logger = LogManager.getLogger(Gene.class);

	String sp;
	String name;
	float pos;
	int dir, seqno, len;
	int coreid1, coreid2;;
	Gene prev, next;

	/** constructor for creating a dummy gene for binary search */
	Gene( int _seqno, float _pos ){
		seqno = _seqno; pos = _pos;
	}

	/** general constructor */
	Gene(String _sp, String _name, int _seqno, float _pos, int _len, int _dir) {
		sp = _sp; name = _name; seqno = _seqno; pos = _pos; len = _len; dir = _dir;
/*
if (name.equals("C730_RS00005")) {
System.out.println(">>>>"+sp+" "+name+" "+seqno);
}
*/
	}

	public void dump() {
		logger.info(sp + ", seqno=" + this.seqno + ", pos=" + this.getPos() +  ", name=" + name + ", (" + this.getRegion0().begin + ", " + this.getRegion0().end + "), dir=" + dir);
	}

	public void setNeighbor(Gene _prev, Gene _next) {
		prev = _prev; next = _next;
	}

	String getSpec() {
		return sp;
	}
	int getSeqNo() {
		return seqno;
	}
	int getSeqNo_0base() {
		return seqno-1;
	}
	GenomicLocus getLocus() {
		return(new GenomicLocus(sp, seqno, (int)pos));
	}
	GenomicRegion getRegion() {
		return(new GenomicRegion(sp, seqno, getBegin0(), getEnd(), getDir()));
	}
	/**
	 * ベースクラスのbegin,endプロパティも更新する。
	 * @return GenericRegion。
	 */
	GenomicRegion getRegion0() {
		GenomicRegion r = this.getRegion();
		r.begin = r.reg.begin;
		r.end = r.reg.end;
		return r;
	}

	float getPos() {
		return(pos);
	}
	String getName() {
		return name;
	}
	int getDir() {
		return(dir);
	}
	int getLen() {
		return(len);
	}
	int getNtLen() {
		return(len * 3);
	}
	int getBegin() {
		return getBegin0();
	}
	int getBegin1() {
		// 1-base
		return((int) Math.round(pos - (float)(getNtLen()+3)/2 + 0.5));
	}
	int getBegin0() {
		// 0-base
		return((int) Math.round(pos - (float)(getNtLen()+3)/2 - 0.5));
	}
	int getEnd() {
		return((int) Math.round(pos + (float)(getNtLen()+3)/2 - 0.5));
	}
	String getSpName() {
		return sp+":"+name;
	}
	String geneInfoString() {
		String info = "name: "+getSpName()+"\n"+
		"seqno: "+getSeqNo()+"\n"+
		"begin: "+getBegin1()+"\n"+
		"end: "+getEnd()+"\n"+
		"dir: "+getDir()+"\n";
		return(info);
	}
	public String toString() {
		return(sp + " " + name + " " + seqno + " " + pos + " " + dir);
	}
}
