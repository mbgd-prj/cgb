package cgdp.corealign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;

/**
 * Class for managing chromosomal arrangements of all genome information on the entier view space.
 * Chromosomal arrangement for each genome is stored in GenomeMapInfo.
 * Oreder of the genomes in the current view is store in currGinfoList, curr_spList, and curr_spHash;
 */
public class CompGenomeMap {

	/**
	 * Logger.
	 */
	private Logger logger = LogManager.getLogger(CompGenomeMap.class);

	/** number of species */
	int spnum;
	GenomeMapInfo[] ginfoList;
	/** ordered list of species in the original dataset */
	ArrayList<String> spList;
	/** species hash for the original dataset */
	HashMap<String, Integer> spHash;

	/** ordered list of species in the current view */
	ArrayList<GenomeMapInfo> currGinfoList;
	/** species list of the current view */
	@Getter
	private ArrayList<String> curr_spList;
	/** species hash for the current view */
	HashMap<String, Integer> curr_spHash;

	/** current view center (which may not be the actual midpoint of the view space) */
	int centerViewPos;

	int viewWidth;

	/** rule to determine the direction of each chromosome.
		FORWARD: all selected genes should be on the forward strand.
		REFSP: refsp genome should not be reversed. Other chromosome should be appropriately reversed so that the selected genes should have the same direction of each chromosome.
	 */
	enum ForceDirection {
		NONE,
		FORWARD,
		REFSP
	}
	ForceDirection forceCenterGeneDirection;
	String refsp;

	public Cluster selectedCluster;
	HashSet<Cluster> selectedClusters;

	/**
	 * 選択されたクラスタIDを取得する。
	 * @return 選択されたクラスタID。
	 */
	public String getSelectedClusterID() {
		return this.selectedCluster.id();
	}

	/**
	 * 選択されたクラスタ名称を取得します。
	 * @return 選択されたクラスタ名称。
	 */
	public String getSelectedCluesterName() {
		return this.selectedCluster.name;
	}

	CompGenomeMap(GenomeData genomeData) {
		LinkedList<String>specList  = genomeData.specList;
		spnum = specList.size();
		ginfoList = new GenomeMapInfo[spnum];

		/** ginfo list in current order */
		currGinfoList = new ArrayList<GenomeMapInfo>();
		/** sp list in original order */
		spList = new ArrayList<String>();
		/** sp index in original order */
		spHash = new HashMap<String,Integer>();
		/** sp index in current order */
		curr_spHash = new HashMap<String,Integer>();

		/** How to determine the direction of each chromsome */
		forceCenterGeneDirection = ForceDirection.FORWARD;
		int i = 0;
		for (String spec: specList) {
			Genome g = genomeData.getGenome(spec);
			// TODO:
			logger.info("*** g.chromosomes.size() = " + g.chromosomes.size());
			ginfoList[i] = new GenomeMapInfo(g, i);
			spHash.put(spec, i);
			spList.add(spec);
			currGinfoList.add(ginfoList[i]);
			curr_spHash.put(spec, i);
			i++;
		}
		setGenomeOrder(spList);

		selectedClusters = new HashSet<Cluster>();
	}
	void setGenomeOrder(ArrayList<String> spList)  {
		curr_spList = spList;
		currGinfoList.clear();
		int i = 0;
		for (String spec: spList) {
			int idx =  spHash.get(spec);
			currGinfoList.add(ginfoList[idx]);
			curr_spHash.put(spec, i);
			i++;
		}
	}
	GenomeMapInfo getGenomeMap(String spec) {
		int idx = spHash.get(spec);
		return(ginfoList[idx]);
	}
	GenomeMapInfo getGenomeMap(int idx) {
		return(ginfoList[idx]);
	}
	int getCurrGenomeOrder(String spec) {
		int idx = curr_spHash.get(spec);
		return(idx);
	}
	GenomeMapInfo getCurrGenomeMap(String spec) {
		int idx = curr_spHash.get(spec);
		return(currGinfoList.get(idx));
	}
	GenomeMapInfo getCurrGenomeMap(int idx) {
		return(currGinfoList.get(idx));
	}
	int getChromNum(int seqno) {
		return ginfoList[seqno].chrnum;
	}
	/**
	 * Get maximum length of concatenated chromosomes
	 */
	int getMaxLength() {
		int maxlen = 0;
		for (GenomeMapInfo ginfo: currGinfoList) {
			int len = ginfo.getTotalLength();
			if (maxlen < len) {
				maxlen = len;
			}
		}
		return(maxlen);
	}
	/**
	 * Get the width of the view space containing all concatenated chromosomes.
	 */
	int getViewWidth() {
		int maxwidth = 0;
		if (viewWidth > 0) {
			return viewWidth;
		}
		for (GenomeMapInfo ginfo: currGinfoList) {
			int genomeViewWidth = ginfo.getTotalLength() + ginfo.getViewShift();
			if (maxwidth < genomeViewWidth) {
				maxwidth = genomeViewWidth;
//System.out.println("genomeViewWidth="+ginfo.genome.getSpCode()+" "+ginfo.getTotalLength()+" "+ginfo.getViewShift()+" "+genomeViewWidth+" "+maxwidth);
			}
		}
		viewWidth = maxwidth;
		return(maxwidth);
	}
	int getOutSpNum() {
		return(currGinfoList.size());
	}
	ArrayList<GenomeMapInfo> getGenomeMapInfoList() {
		return(currGinfoList);
	}
	/** return the list of GenomeMapInfo containing only specified set of species */
	ArrayList<GenomeMapInfo> getGenomeMapInfoList(Set<Integer> specList) {
		ArrayList<GenomeMapInfo> newGinfoList = new ArrayList<GenomeMapInfo>();;
		for (GenomeMapInfo ginfo: currGinfoList) {
			if (specList.contains(ginfo.origOrder)) {
				newGinfoList.add(ginfo);
			}
		}
		return(newGinfoList);
	}
	void setSelectedCluster(Cluster cluster) {
		selectedCluster = cluster;
		selectedClusters.clear();
		selectedClusters.add(cluster);
	}
	void addSelectedCluster(Cluster cluster) {
		selectedClusters.add(cluster);
	}
	void toggleSelectedCluster(Cluster cluster) {
		if (selectedClusters.contains(cluster)) {
			selectedClusters.remove(cluster);
		} else {
			selectedClusters.add(cluster);
		}
	}
	Set<Cluster> getSelectedClusters() {
		return selectedClusters;
	}
	Cluster getSelectedCluster() {
		return selectedCluster;
	}
	void setCenterDirection(ForceDirection forcedir) {
		forceCenterGeneDirection = forcedir;
	}
	void setCenterDirectionRefSp(String _refsp) {
		setCenterDirection(ForceDirection.REFSP);
		refsp = _refsp;
	}
	void setZeroCenter() {
		for (String spec: spList) {
			GenomeMapInfo ginfo = getGenomeMap(spec);
			ginfo.setZeroCenter();
		}
		setCenterViewShift();
	}
	/** set the specified cluster at the center */
	void setCenterByCluster(Cluster cluster) {
		HashMap<String, DomCluster> domHash = new HashMap<String, DomCluster>();
		for (int i = 0; i < spnum; i++) {
			LinkedList<DomCluster> mem = cluster.members(i);
//System.out.println(">cen>"+i+" "+spList.get(i)+" "+mem.size());
			if (mem == null || mem.size() == 0) continue;
			DomCluster dcl = mem.get(0);
//System.out.println("center_gene:"+dcl.dom.gene);
			domHash.put(spList.get(i), dcl);
		}
		setCenter(domHash);
//		setSelectedCluster(cluster);
	}
	void setCenter(HashMap<String,DomCluster> domHash) {
		// forced direction for each selected gene (default: forward)
		int forcedir = 1;
//System.out.println("force="+forceCenterGeneDirection);

		// forced direction is same as that of refsp ortholog
		if (forceCenterGeneDirection == ForceDirection.REFSP) {
			DomCluster dcl = domHash.get(refsp);
System.out.println("REF="+refsp+" "+dcl+" "+forcedir);
			forcedir = dcl.dom.gene.dir;
		}
		for (String spec: domHash.keySet()) {
			GenomeMapInfo ginfo = getGenomeMap(spec);
			DomCluster dcl = domHash.get(spec);
			int seqno = dcl.dom.gene.getSeqNo_0base();
			int pos = (int) dcl.dom.gene.pos;
//System.out.println("PPP:"+pos);
			ginfo.setCenter(seqno, pos);
			if (dcl.dom.gene.dir * ginfo.getChromDir(seqno) != forcedir) {
//System.out.println("reverse: "+dcl.dom.gene);
				ginfo.reverse(seqno);
			}
		}
		setCenterViewShift();
	}
	/** post-processing: move the center positions to the center of view space */
	void setCenterViewShift() {
		int center = 0, max_center = 0;
		for (String spec: spList) {
			GenomeMapInfo ginfo = getGenomeMap(spec);
			center = ginfo.getCenterPos();
			if (center > max_center) {
				max_center = center;
			}
		}
		centerViewPos = max_center;
		for (String spec: spList) {
			GenomeMapInfo ginfo = getGenomeMap(spec);
			ginfo.setViewShift(max_center);
		}
	}
	/** get the center position to be aligned in the view space */
	int getCenterViewPos() {
		return(centerViewPos);
	}
	void setCenterViewPos(int centerPos) {
		centerViewPos = centerPos;
	}

	/** get postion of the specified locus in the current view space */
	int getViewPosition(GenomicLocus loc) {
		GenomeMapInfo ginfo = getGenomeMap(loc.spec);
		return ginfo.getViewPosition(loc);
	}

	/**
	 * 選択されたLocusのリストを取得する。
	 * @return 選択されたLocusのリスト。
	 */
	public Map<String, List<String>> getSpLocusListMap() {
		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		for (Cluster selCluster : this.getSelectedClusters()) {
			for (LinkedList<DomCluster> list: selCluster.members) {
				for (DomCluster dc: list) {
					String sp = dc.dom.spec;
					String name = dc.dom.name;
					List<String> glist = ret.get(sp);
					if (glist == null) {
						glist = new ArrayList<String>();
					}
					glist.add(name);
					ret.put(sp, glist);
				}
			}
		}
		return ret;
	}

}
/**
 * Class for managing chromosome arrangement of a genome on the view space.
 * All chromosomes (or contigs) are re-ordered and concatenated into one sequence with inserting fixed-length gaps.
 * In centering mode, the specified position on the specified chromosome is aligned to other genomes.
 * For this purpose, the location of the concatenated sequence is adjusted on the view space (based on chromViewShift).
 */
class GenomeMapInfo {
	Genome genome;

	/** original genome order */
	int origOrder;

	/** orig to view order */
	int[] chromOrder;
	/** view to orig order */
	int[] chromView;
	/** view direction (1/-1) of each chromosome */
	int[] chromDir;
	/** shifted position from the original for displaying each circular chromosome */
	int[] chromShift;
	/** shifted position from the original for saved new origin */
	int[] chromShift2;
	/** selected chromosome for centering */
	int selectedChrom;
	/** selected position */
	int selectedPos;

	/** offset to adjust view position after aligning the center genes */
	int chromViewShift;

	int chrnum;
	static int gap_cnt = 50;
	static int gap_len = 10000;
	static int gap = gap_cnt;
	int totalLength;
	GenomeMapInfo(Genome g, int ord) {
		genome = g;
		origOrder = ord;
		chrnum = genome.chromosomes.size();
		chromOrder = new int[chrnum];
		chromView = new int[chrnum];
		chromDir = new int[chrnum];
		chromShift = new int[chrnum];
		resetPositions();
		resetCenter();
		calcTotalLength();
	}
	Genome getGenome() {
		return genome;
	}
	static void setGapByChrLenMode(boolean isRealChrLen) {
		if (isRealChrLen) {
			gap = gap_len;
		} else {
			gap = gap_cnt;
		}
	}
	static void setChromosomeGapLen(int _gap_len) {
		gap_len = _gap_len;
	}
	void setGap(int _gap) {
		gap = _gap;
	}
	/**
	 * get chromosome data by specifying view order.
	 */
	Chromosome getChromosomeByViewOrder(int viewOrder) {
		return( genome.chromosomes.get( chromView[viewOrder] ) );
	}
	/**
	 * Create array of SeqRegion containing each chromosome location on the view space.
	 */
	SeqRegion[] getChromosomeViewPositions() {
		SeqRegion[] reg = new SeqRegion[chrnum];
		int pos = chromViewShift;
		int begin, end;
		for (int i = 0; i < chrnum; i++) {
			begin = pos;
			Chromosome c = getChromosomeByViewOrder(i);
//System.out.println(genome.spcode+": viewpos-"+i+"="+"seqno-"+c.getSeqNo_0base());
			pos += c.getLength();
			end = pos;
			reg[i] = new SeqRegion(begin, end);
			pos += gap;
		}
		return(reg);
	}
	/**
	 * Calculate the total length of the genome (including gap) on the view space.
	 */
	void calcTotalLength() {
		totalLength = 0;
		for (int i = 0; i < chrnum; i++) {
			Chromosome c = genome.chromosomes.get(i);
			if (i > 0) {
				totalLength += gap;
			}
			totalLength += c.getLength();
		}
	}
	int getTotalLength() {
		return(totalLength);
	}
	int getViewShift() {
		return(chromViewShift);
	}
	/**
	 * Get the region on the concatenated genome of the specified gene (assuming nuc length = aa length x 3).
	 */
	GenomicRegion getGeneRegion(Gene gene) {
		int seqno = gene.getSeqNo_0base();
		int pos_begin = getPosition(seqno, gene.getBegin0());
		int pos_end = getPosition(seqno, gene.getEnd());
if (gene.getName().equals("H779_YJM993P00474")){
System.out.println("SSSSS:"+pos_begin+" "+pos_end+" // "+gene.getBegin0()+" "+gene.getEnd());
}
		if (pos_begin > pos_end) {
			int tmp = pos_begin; pos_begin = pos_end; pos_end = tmp;
		}
		return(new GenomicRegion(gene.getSpec(), seqno, pos_begin, pos_end, gene.getDir()));
/*
		double pos =  (double) getGenePosition(gene);
		double nuclen = (double) gene.getLen() * 3;
		pos = pos - 0.5; // 0-base
		return  (new SeqRegion( (int) Math.round((pos - nuclen / 2)),
				(int) Math.round((pos + nuclen / 2)) ));
*/
	}
	/**
	 * Get the position on the concatenated genome of the specified gene.
	 */
	int getGenePosition(Gene gene) {
		return getGenePosition(gene, 0);
	}
	int getGenePosition(Gene gene, int mode) {
		int genepos = (int) gene.getPos();
		int seqno = gene.getSeqNo_0base();
		return( getPosition(seqno, genepos, mode) );
	}
	int getOrigGenePosition(Gene gene) {
		return getGenePosition(gene, 1);
	}
	/**
	 * Get the position on the concatenated genome from the sequnece position on the specified chromosome in the original order (without rotation in circular genome).
	 */
	int getOrigPosition(int seqno, int pos) {
		return getPosition(seqno, pos, 1);
	}
	int getOrigPosition(GenomicLocus loc) {
		return getOrigPosition(loc.getSeqNo_0base(), loc.pos);
	}
	/**
	 * Get the position on the concatenated genome from the sequnece position on the specified chromosome.
	 */
	int getPosition(int seqno, int pos) {
		return getPosition(seqno, pos, 0);
	}
	int getPosition(GenomicLocus loc) {
		return getPosition(loc.getSeqNo_0base(), loc.pos);
	}
	/**
	 * Get the position on the concatenated genome from the sequnece position on the specified chromosome.
	 */
	 // TODO:これが遅い
	int getPosition(int seqno, int pos, int mode) {
		int viewpos = 0;

		for (int i = 0; i < chromOrder[seqno]; i++) {
			Chromosome c = getChromosomeByViewOrder(i);
			viewpos += c.getLength();
			viewpos += gap;
		}
		Chromosome c = genome.chromosomes.get(seqno);
		int chrLen = c.getLength();
/*if (pos==905995) {
	System.out.println("AAA>"+seqno+";chrlen="+chrLen+" "+chromShift[seqno]);
}
if (chrLen == 0) {
System.out.println("sp="+genome.spcode+" "+"chrlen="+chrLen+" "+seqno+" "+pos);
}*/
		if (mode == 0) {
			pos = pos + chromShift[seqno];
		}
		if (c.isCircular()) {
			pos = pos % chrLen;
			pos = (pos > 0) ? pos : (pos + chrLen);
		}

		if (chromDir[seqno] < 0) {
			pos = (chrLen - pos) + 1;
		}
		viewpos += pos;
		return viewpos;
	}
	GenomicLocus movePositionOnConcatSeq(GenomicLocus loc, int moveLength) {
//		int newpos = getOrigPosition(loc) +  moveLength;
		int newpos = getPosition(loc) +  moveLength;
		GenomicLocus newloc = getLocus_from_concatSeqPos(newpos);
System.out.println("LOC>>"+loc+" "+moveLength+" ;newpos="+newpos+"; newloc>> "+newloc);
		return newloc;
	}

	/**
	 * Get the sequence position from the position on the view space (clicked by user).
	 */
	GenomicLocus getLocus_from_ViewPos(int pos) {
//System.out.println("chromViewShift="+chromViewShift);
		pos -= chromViewShift;
		return getLocus_from_concatSeqPos(pos);
	}
	GenomicLocus getLocus_from_concatSeqPos(int pos) {
		int seqno;
		Chromosome c = null;
		for (seqno = 0; seqno < chrnum; seqno++) {
			c = getChromosomeByViewOrder(seqno);

			if (pos <= c.getLength()) {
				break;
			} else {
				pos -= c.getLength();
				pos -=  gap;
			}
		}
		if (seqno == chrnum) {
			seqno--;
		}

		seqno = chromView[seqno]; // convert from view to orig order

//		Chromosome c = genome.chromosomes.get(seqno);
		int chrLen = c.getLength();
//System.out.println("POS: dir:"+chromDir[seqno]+" seqno="+seqno+" pos="+pos+" "+chrLen+" "+c.shape);
		if (chromDir[seqno] < 0) {
			pos = (chrLen - pos) + 1;
//System.out.println(" revpos="+pos+": "+chrLen);
		}
//System.out.println("chromShift:"+chromShift[seqno]);
		if (c.shape == ChrShape.circular) {
			pos = (pos - chromShift[seqno]) % chrLen;
			pos = (pos > 0) ? pos : (pos + chrLen);
//System.out.println("convpos00:"+pos);
		} else {
			// linear
			if (pos <= 0) {
				pos = 1;
			} else if (pos > c.getLength()) {
				pos = c.getLength();
			}
		}
//System.out.println("convpos2:"+pos);
		// seqno in GenomicLocus should be 1-based coordinate
		return new GenomicLocus(genome.getSpCode(), seqno+1, pos);
	}
	int getChromDir() {
		if (selectedChrom < 0) {
			return 1;
		} else {
			return getChromDir(selectedChrom);
		}
	}
	int getChromDir(GenomicLocus loc) {
		return ( getChromDir(loc.getSeqNo_0base()) );
	}
	int getChromDir(int seqno) {
		return chromDir[seqno];
	}
	SeqRegion getGeneViewRegion(Gene gene) {
		SeqRegion seqreg =  getGeneRegion(gene).getSeqRegion();
		seqreg.begin += chromViewShift;
		seqreg.end += chromViewShift;
		return(seqreg);
	}
	int getGeneViewPosition(Gene gene) {
		return ( getGenePosition(gene) + chromViewShift );
	}
	int getViewPosition(int seqno, int pos) {
		return ( getPosition(seqno, pos) + chromViewShift );
	}
	SeqRegion getViewRegion(GenomicRegion reg) {
		int begpos = getViewPosition(reg.beginLocus());
		int endpos = getViewPosition(reg.endLocus());
		return new SeqRegion(begpos, endpos);
	}
	int getViewPosition(GenomicLocus loc) {
		return ( getPosition(loc) + chromViewShift );
	}
	int getChrnum() {
		return(chrnum);
	}
	/**
	 * Locate zero position of the longest chromosome at center.
	 */
	void setZeroCenter() {
		int len, maxlen = 0;
		int maxchrom = 0;
		for (int i = 0; i < chrnum; i++) {
			Chromosome c = genome.chromosomes.get(i);
			len = c.getLength();
			if (len > maxlen) {
				maxlen = len;
				maxchrom = i;
			}
		}
		setCenter(maxchrom, 0);
	}
	/**
	 * Set the specified position as selected, and locate it at center by rotating the chromosome (for circular chromosome).
	 */
	void setCenter(int seqno, int pos) {
		selectedChrom = seqno;
		Chromosome c = genome.chromosomes.get(seqno);
		if (c.shape == ChrShape.circular) {
			selectedPos = c.getLength() / 2;
			chromShift[seqno] = selectedPos - pos;
System.out.println(">>center:"+genome.getSpCode()+" "+seqno+" "+chromOrder[seqno]+" "+pos+" "+selectedPos+" "+chromDir[seqno]+" "+chromShift[seqno]);
		} else {
			// do nothing for linear genome
			selectedPos = pos;
System.out.println(">skip>center:"+genome.getSpCode()+" "+seqno+" "+pos+" "+selectedPos);
		}
	}
	/**
	 * Return view position of the center postion on the selected chromosome
	 */
	int getCenterPos() {
		int viewpos = 0;
		Chromosome c;
		if (selectedChrom < 0) {
			return(-1);
		}
		for (int i = 0; i < chromOrder[selectedChrom]; i++) {
			c = getChromosomeByViewOrder(i);
			viewpos += c.getLength();
			viewpos += gap;
		}
		// add selected (center) postion of the current selected chromosome
		if (chromDir[selectedChrom] < 0) {
			c = genome.chromosomes.get(selectedChrom);
			viewpos += (c.getLength() - selectedPos + 1);
		} else {
			viewpos += selectedPos;
		}
		return(viewpos);
	}
	/**
	 * Calculate the beginning position (viewShift) of each genome when the selected postion is aligned at the center of the view space.
	 * @param viewSize the size of the view space
	 */
	void setViewShift( int center_in_view ) {
		int viewpos = getCenterPos();
		chromViewShift = (center_in_view - viewpos);
//System.out.println("VIEWSHIFT:"+chromViewShift);
	}
	/**
	 * Reverse the selected chromosome.
	 */
	void reverse(int seqno) {
		chromDir[seqno] *= -1;
	}
	/**
	 * Initinalize the positional information for each chromosome.
	 */
	void resetPositions() {
		for (int i = 0; i < chrnum; i++) {
			chromOrder[i] = i;
			chromView[i] = i;
			chromDir[i] = 1;
			chromShift[i] = 0;
		}
	}
	/**
	 * Initinalize the selected position to be centered.
	 */
	void resetCenter() {
		selectedChrom = -1;
		chromViewShift = 0;
	}
	boolean isCentering() {
		return (selectedChrom >= 0);
	}
	/**
	 * Sort chromosomes according to the values assigned to each chromosome.
	 * The resulting order will be stored in class variables chromOrder and chromView,
	 * where chromOrder is used to obtain original order to new order
	 * and chromView is used to obtain new order to original order.
	 * @param values the values assiged to each chromosome by which chromosomes will be sorted.
	 */
	void setOrder(int[] values) {
		class TmpClass {
			int idx, val;
			TmpClass(int i, int v) {
				idx = i; val = v;
			}
		}
		TmpClass[] tmpList = new TmpClass[chrnum];
		for (int i = 0; i < chrnum; i++) {
			tmpList[i] = new TmpClass(i, values[i]);
		}
		Arrays.sort(tmpList, new Comparator<TmpClass>() {
			public int compare(TmpClass c1, TmpClass c2) {
				return( c1.val - c2.val );
			}
		});
		for (int ord = 0; ord < chrnum; ord++) {
			int origIdx = tmpList[ord].idx;
//System.out.println("setOrder:"+ genome.getSpCode()+": "+ord+" "+tmpList[ord].val+" "+origIdx);
			chromOrder[origIdx] = ord;	// origIdx to newOrder
			chromView[ord] = origIdx;	// newOrder to origIdx
		}
	}
}

/**
 * Obsolete: Sequence region: move to a specific source file SeqRegion.java
class SeqRegion {
	int begin, end;
	SeqRegion(int _begin, int _end) {
		begin = _begin;
		end = _end;
	}
	int length() {
		return(end - begin + 1);
	}
	public String toString() {
		return begin+" "+end;
	}
}

 *
 * Obsolete: Sequence region with truncation
class SeqRegionTrunc extends SeqRegion {
	boolean truncL, truncR;
	SeqRegionTrunc(int _begin, int _end, boolean _truncL, boolean _truncR) {
		super(_begin, _end);
		truncL = _truncL; truncR = _truncR;
	}
	SeqRegionTrunc (SeqRegion seqreg) {
		super(seqreg.begin, seqreg.end);
		truncL = truncR = false;
	}
	boolean isTruncL() {return truncL;}
	boolean isTruncR() {return truncR;}
}
*/

/**
 * class for setting the chromosome (contig) order that maximally fits the order in the core genome
 */
class SetChromOrder {
	CoreGenome coreGenome;
	CompGenomeMap compMap;
	int spnum;
	ChromosomeStat[][] chromStats;
	SetChromOrder(CoreGenome _coreGenome, CompGenomeMap _compMap) {
		coreGenome = _coreGenome;
		compMap = _compMap;
		spnum = coreGenome.specNum();
		chromStats = new ChromosomeStat[spnum][];
		for (int i = 0; i < spnum; i++) {
			int chrnum = compMap.getChromNum(i);
			chromStats[i] = new ChromosomeStat[ chrnum ];
//System.out.println("chrnum: "+i+" "+chrnum);
			for (int j = 0; j < chrnum; j++) {
//System.out.println("chrom: "+i+" "+j);
				chromStats[i][j] = new ChromosomeStat(j);
			}
		}
	}
	void setOrder(CoreCluster centerClust) {
		int corePos = 0;
		float[] prevpos = new float[spnum];
		int centerPos = 0;
		int coreSize = coreGenome.totalLength();
		if (centerClust != null) {
			for (CoreCluster cclust: coreGenome) {
				if (cclust== centerClust) {
					break;
				}
				centerPos++;
			}
		}
		for (CoreCluster cclust: coreGenome) {
			int spNo = 0;
//System.out.println("clust:"+corePos+" "+cclust.cluster.id());
			for (LinkedList<DomCluster> dcl_list: cclust.cluster.members) {
				if (dcl_list != null && dcl_list.size() > 0) {
					DomCluster dcl = dcl_list.get(0);
					Gene gene = dcl.dom.gene;
					int seqno = gene.seqno - 1;
					float pos = gene.pos;
					int coreViewPos = corePos;
					if (centerClust != null) {
						coreViewPos = (int) (corePos + (coreSize / 2 - centerPos) + coreSize) % coreSize;
					}
//System.out.println("chrom: "+spNo+" "+seqno+" "+gene+" "+gene.seqno);
					chromStats[spNo][seqno].order.add(coreViewPos);
					if (prevpos[spNo]>0 && Math.abs(pos - prevpos[spNo]) < 20) {
						if (prevpos[spNo] < pos) {
							chromStats[spNo][seqno].dir_count++;
						} else {
							chromStats[spNo][seqno].dir_count--;
						}
					}
					prevpos[spNo] = pos;
				}
				spNo++;
			}
			corePos++;
		}
		for (int i = 0; i < spnum; i++) {
			int chrnum = compMap.getChromNum(i);
			int[] values = new int[chrnum];
			for (int j = 0; j < chrnum; j++) {
				values[j] = chromStats[i][j].calcMidOrder();
			}
//System.out.println("OrderValues:"+Arrays.toString(values));
			GenomeMapInfo gMap = compMap.getGenomeMap(i);
//System.out.println(gMap.genome.getName()+" "+"chrnum: "+chrnum);
			gMap.setOrder(values);
			for (int j = 0; j < chrnum; j++) {
				if (chromStats[i][j].dir_count < 0) {
					gMap.reverse(j);
				}
			}
		}
	}
}
class ChromosomeStat {
	int idx;
	ArrayList<Integer> order;
	int midOrder;
	int dir_count;
	ChromosomeStat(int i) {
		idx = i;
		order = new ArrayList<Integer>();
	}
	int calcMidOrder() {
		if (order.size() == 0) {
			return 99999999;
		} else {
			return( order.get( (int) (order.size() / 2) ) );
		}
	}
}
