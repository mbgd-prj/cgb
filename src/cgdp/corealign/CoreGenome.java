package cgdp.corealign;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.dialog.AddGroupDialog.GroupInfo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

enum BlockType {Core, Island, Other};

public class CoreGenome implements Iterable<CoreCluster> {

	private static Logger logger = LogManager.getLogger(CoreGenome.class);

	ArrayList<CoreGenomeBlock> blocks;
/*
	String[] species;
	HashMap<String, Integer>spHash;
*/
	SpeciesList species;
	int specNum;
	SpIndex[] spIndex;
	String refsp;
	static CoreGenome _coreGenome = null;
	HashMap<String, CoreCluster> geneIndex = null;
	HashMap<String,Integer> orderHash;
	boolean make_spindex, make_connection;
//	int WinSize = 10;
	static int GapWin = 20;

	/**
	 * clustidとClusterの対応マップ。
	 */
	@Setter
	@Getter
	private Map<String, Cluster> clusterMap = null;

	/**
	 * クラスターIDからCluster情報を取得する。
	 * @param id クラスターID。
	 * @return Cluster情報。
	 */
	public Cluster getCluster(final String id) {
		return this.clusterMap.get(id);
	}

	/**
	 * マッチング関数インターフェース。
	 */
	@FunctionalInterface
	private interface Match {
		/**
		 * クラスタ内を検索する。
		 * @param kw 検索キーワード。
		 * @param cluster クラスタ情報。
		 * @return 検索結果。
		 */
		List<String[]> match(final String kw, final Cluster cluster);
	}

	/**
	 * 検索処理。
	 * @param kw キーワード。
	 * @param m マッチング処理。
	 * @return 検索結果。
	 */
	private List<String[]> search(final String kw, final Match m) {
		List<String[]> list = new ArrayList<String[]>();
		Set<String> cset = this.clusterMap.keySet();
		for (String cid: cset) {
			Cluster cl = this.clusterMap.get(cid);
			list.addAll(m.match(kw, cl));
		}
		return list;
	}

	/**
	 * Gene名称で検索する。
	 * @param name Gene名称。
	 * @return 検索結果。
	 */
	public List<String[]> searchGeneName(final String name) {
		List<String[]> list = this.search(name, (kw, cluster) -> {
			Pattern p = Pattern.compile(kw);
			List<String[]> ret = new ArrayList<String[]>();
			if (cluster.members.length > 0) {
				for (List<DomCluster> dclist: cluster.members) {
					for (DomCluster dc: dclist) {
						String nm = dc.dom.spec + ":" + dc.dom.name;
						Matcher m = p.matcher(nm);
						if (m.find()) {
							String[] rec = new String[2];
							rec[0] = dc.clustid;
							rec[1] = nm;
							ret.add(rec);
						}
					}
				}
			}
			return ret;
		});
		return list;
	}

	/**
	 * クラスタIDで検索します。
	 * @param clustid クラスタID。
	 * @return 検索結果。
	 */
	public List<String[]> searchClustid(final String clustid) {
		logger.debug("clustid=" + clustid);
		List<String[]> list = this.search(clustid, (kw, cluster) -> {
			List<String[]> ret = new ArrayList<String[]>();
			if (cluster.members.length > 0) {
				for (List<DomCluster> dclist: cluster.members) {
					for (DomCluster dc: dclist) {
						String nm = dc.dom.spec + ":" + dc.dom.name;
						String id = dc.clustid;
						logger.debug("id=" + id + "," + nm + "," + kw);
						if (id.equals(kw)) {
							String[] rec = new String[2];
							rec[0] = dc.clustid;
							rec[1] = nm;
							ret.add(rec);
						}
					}
				}
			}
			return ret;
		});
		return list;
	}

	/**
	 * 表示フラグ。
	 */
	@Getter
	@Setter
	private boolean visible = true;

	/**
	 * 名称。
	 */
	@Getter
	@Setter
	private String name = "";

	/**
	 * グラデーションフラグ。
	 */
	@Getter
	@Setter
	private boolean gradation = true;

	/**
	 * 表示色。
	 */
	@Getter
	@Setter
	private Color color = Color.LIGHT_GRAY;


	/**
	 * コンストラクタ。
	 */
	CoreGenome() {
		blocks = new ArrayList<CoreGenomeBlock>();
		refsp = null;	// not assigned
		make_spindex = make_connection = false;
	}


	/**
	 * ブロック情報。
	 *
	 */
	@Data
	public static class BlockInfo implements GroupInfo {
		private boolean select;
		private String blockType = null;
		private String blockNo = null;
		private int clusterCount = 0;
		public BlockInfo(final String type, final String blockNo, final int cnt) {
			this.blockType = type;
			this.blockNo = blockNo;
			this.clusterCount = cnt;
		}
	}

	/**
	 * 条件判定。
	 * @param no ブロック番号。
	 * @param name Geneの名称。
	 * @param block ブロック情報。
	 * @return 該当する場合true。
	 */
	private boolean match(final String fromNo, final String toNo, final CoreGenomeBlock block) {
		float from = Float.MIN_VALUE;
		float to = Float.MAX_VALUE;
		if (fromNo != null && fromNo.length() > 0) {
			from = Float.parseFloat(fromNo);
		}
		if (toNo != null && toNo.length() > 0) {
			to = Float.parseFloat(toNo);
		}
		float blockNo = Float.parseFloat(block.getBlockNo());
		if (from <= blockNo && blockNo <= to) {
			return true;
		}
		return false;
	}

	/**
	 * ブロックの検索を行う。
	 * @param type Core or Island。
	 * @param no ブロック番号。
	 * @param name Geneの名称。
	 * @return ブロックリスト。
	 */
	public List<BlockInfo> search(final String type, final String fromNo, final String toNo) {
		List<BlockInfo> ret = new ArrayList<BlockInfo>();
		for (CoreGenomeBlock block: this.blocks) {
			logger.debug("blockNo=" + block.getBlockNo() + ", " + block.coreClusterList.size());
			if (this.match(fromNo, toNo, block)) {
				BlockInfo bi = new BlockInfo(type, block.getBlockNo(), block.coreClusterList.size());
				ret.add(bi);
			}
		}
		return ret;
	}



	/**
	 * データのダンプ。
	 */
	public void dump() {
		logger.debug("--- CoreGenome start ---");
		for (CoreGenomeBlock block: this.blocks) {
			block.dump();
		}
		logger.debug("--- CoreGenome finish ---");
	}

	/**
	 * sp:name形式のSetを取得します。
	 * @return sp:name形式のSet。
	 */
	public Set<String> getSpNameSet() {
		Set<String> ret = new HashSet<String>();
		for (CoreGenomeBlock b: this.blocks) {
			for (CoreCluster cc: b.coreClusterList) {
				for (LinkedList<DomCluster> list: cc.cluster.members) {
					for (DomCluster dc: list) {
						String gene = dc.dom.spec + ":" + dc.dom.name;
						ret.add(gene);
					}
				}
			}
		}
		return ret;
	}


/*
	static CoreGenome getInstance() {
		if (_coreGenome == null) {
			_coreGenome = new CoreGenome();
		}
		return(_coreGenome);
	}
*/
	/**
	 * 空データ(読み込んでいないデータ)の判定。
	 * @return 空データの場合true。
	 */
	public boolean isBlank() {
		return this.species == null;
	}

	static CoreGenome create(AlignmentPath aliPath, ClusterSet cSet, ClusterDir gDir) {
		return create(aliPath, cSet, gDir, 0.0);
	}
	static CoreGenome create(AlignmentPath aliPath, ClusterSet cSet, ClusterDir gDir, double consRatio) {
/*
		CoreGenome coreGenome = CoreGenome.getInstance();
*/
		CoreGenome coreGenome = new CoreGenome();
		for (Iterator<ClustAliPath> iter = aliPath.iterator(); iter.hasNext(); ) {
			ClustAliPath alip = iter.next();
			CoreGenomeBlock cblk = CoreGenomeBlock.create(alip, cSet, gDir, consRatio);
//System.out.println("blocklen: "+cblk.length());
			coreGenome.addBlock(cblk);
		}
		coreGenome.setSpecies(cSet.species);
		return(coreGenome);
	}
	void setSpecies(SpeciesList _species) {
		species = _species;
		specNum = species.spNum();
	}
	void setSpecies(String[] _species) {
		species = new SpeciesList(_species);
		specNum = species.spNum();
/*
		species = _species;
		specNum = _species.length;
		createSpHash();
*/
	}
	void setBlockType(BlockType type) {
		for (CoreGenomeBlock blk: blocks) {
			blk.setBlockType(type);
		}
	}
/*
	String[]
*/
	SpeciesList getSpecies() {
		return species;
	}
	static void setGapWin(int gapWin) {
		GapWin = gapWin;
	}
/*
	void createSpHash() {
		spHash = new HashMap();
		for (int i = 0; i < species.spNum(); i++) {
			spHash.put(species.get(i), i);
		}
	}
*/
	void sortByReference() {
		if (species.spHash == null) {
			return;
		}
		logger.info("refsp=" + refsp);
		int refspNo = species.getIdx(refsp);
		if (refspNo < 0) {
			return;
		}
		for (CoreGenomeBlock blk: blocks) {
			blk.calcRefPos(refspNo);
			blk.setDirectionByRefSp(refspNo);
		}
		Collections.sort(blocks, new Comparator<CoreGenomeBlock>() {
			public int compare(CoreGenomeBlock blk1, CoreGenomeBlock blk2) {
				return((int) Math.signum(blk1.refpos - blk2.refpos));
			}
		});
		make_spindex = make_connection = false;
	}
	public CoreGenomeIterator iterator() {
		if (blocks.size() == 0) {
			return null;
		} else {
			return new CoreGenomeIterator(this);
		}
	}
	void addBlock(CoreGenomeBlock cblk) {
		cblk.coreGenome = this;
		blocks.add(cblk);
		cblk.setCoreGenome(this);
	}
	void concatCore(CoreGenome coreGenome) {
		for (CoreGenomeBlock cblk: coreGenome.blocks) {
			addBlock(cblk);
		}
	}
	int specNum() {
		return specNum;
	}
	void setRefSp(String _refsp) {
		setRefSp(_refsp,  true);
	}
	void setRefSp(String _refsp, boolean doSort) {
		refsp = _refsp;
		if (doSort == true) {
			sortByReference();
		}
	}
	void makeSpIndex(GenomeData gdata) {
		if (make_spindex) {
			return;
		}
		CoreGenomeIterator iter = iterator();
		ArrayList<ClusterI> clusterList = new ArrayList<ClusterI>();
		while (iter.hasNext()) {
			CoreCluster cclust = iter.next();
//			clusterList.add(cclust.cluster);
			clusterList.add(cclust);
		}
		spIndex = new SpIndex[species.spNum()];
		for (int i = 0; i < species.spNum(); i++) {
			Genome genome = gdata.getGenome(species.get(i));
			/* create also a list of CoreCluster */
			spIndex[i] = new SpIndex(i, clusterList, genome, true);
		}
		make_spindex = true;
	}
	SpIndex getSpIndex(String spec) {
		int spNo = species.getIdx(spec);
		return(spIndex[spNo]);
	}
	void makeGeneIndex() {
System.out.println("###makeGeneIndex: "+specNum());
		if (geneIndex == null) {
			geneIndex = new HashMap<String,CoreCluster>();
		}
		for (CoreCluster cclust: this) {
			for (int i = 0; i < specNum; i++) {
				LinkedList<DomCluster> dcl_list = cclust.members(i);
				if (dcl_list == null)
					continue;
				for (DomCluster dcl: dcl_list) {
					String genename = dcl.dom.gene.sp+":"+dcl.dom.gene.name;
					geneIndex.put(genename, cclust);
if (genename.equals("adh:CK627_RS12845")) {
System.out.println("INDEX:"+geneIndex.hashCode());
System.out.println("hit:"+cclust.id()+" "+geneIndex.get(genename));
}
				}
			}
		}
System.out.println("hit001:"+geneIndex.get("adh:CK627_RS12845"));
System.out.println("INDEX2:"+geneIndex.hashCode());
	}
	void makeOrderHash() {
		orderHash = new HashMap<String,Integer>();
		for (CoreGenomeBlock blk: blocks) {
			blk.makeOrderHash(orderHash);
		}
	}
	CoreCluster getClusterByIdx(int coreidx) {
		if (coreidx < 0) {
			coreidx = totalLength() - 1;
		}
		CoreGenomeBlock targetBlk = null;
		for (CoreGenomeBlock blk: blocks) {
			if (coreidx < blk.length()) {
				targetBlk = blk;
				break;
			}
			coreidx -= blk.length();
		}
		if (targetBlk == null) {
			return null;
		}
		return(targetBlk.get(coreidx));
	}
	CoreCluster getClusterByGene(String genename) {
		if (geneIndex == null) {
			makeGeneIndex();
		}
if (genename.equals("adh:CK627_RS12845")) {
System.out.println("hit002:"+geneIndex.get("adh:CK627_RS12845"));
System.out.println("INDEX:"+geneIndex.hashCode());
System.out.println("GGGG:"+genename+" "+geneIndex.get(genename));
}
		CoreCluster cclust = geneIndex.get(genename);
		return(cclust);
	}
	CoreCluster getClusterByPos(GenomicLocus loc, GenomeData gdata) {
		return getConsClusterByPos(loc, gdata, 0.0);
	}
/*
	CoreCluster getConsClusterByPos(String refsp, int pos, GenomeData gdata) {
		return getConsClusterByPos(refsp, pos, gdata, 1.0);
	}
	CoreCluster getConsClusterByPos(String loc, GenomeData gdata) {
		return getConsClusterByPos(loc, gdata, 1.0);
	}
	CoreCluster getConsClusterByPos(GenomicLocus loc, GenomeData gdata) {
		return getConsClusterByPos(loc, gdata, 1.0);
	}
	CoreCluster getConsClusterByPos(String loc, GenomeData gdata, double consRatioCut) {
		GenomicLocus locus = new GenomicLocus(loc);
		return getConsClusterByPos(locus, gdata, consRatioCut);
	}
	CoreCluster getConsClusterByPos(String refsp, int pos, GenomeData gdata, double consRatioCut) {
		GenomicLocus locus = new GenomicLocus(refsp, -1, pos);
		return getConsClusterByPos(locus, gdata, consRatioCut);
	}
*/

/*
	Chromosome getChromosomeByPos(GenomicLocus locus, GenomeData gdata) {
		String refsp = locus.spec;
		int seqno = locus.seqno;
		int pos = locus.pos;
		Genome refGenome = gdata.getGenome(refsp);
		Chromosome chrom;
		if (seqno > 0) {
			chrom = refGenome.getChromosome(seqno);
		} else {
			chrom = refGenome.getMaxChromosome();
		}
		return(chrom);
	}
	Gene getGeneByPos(GenomicLocus locus, GenomeData gdata) {
		Chromosome chrom = getChromosomeByPos(locus, gdata);
		int idx = getGeneIdxByPos(locus, gdata);
		Gene hitGene = chrom.genes.get(idx);
		return(hitGene);
	}
	int getGeneIdxByPos(GenomicLocus locus, GenomeData gdata) {
		String refsp = locus.spec;
		int seqno = locus.seqno;
		int pos = locus.pos;
		Chromosome chrom = getChromosomeByPos(locus, gdata);

		int idx = Collections.binarySearch(chrom.genes,
			new Gene(seqno,pos),
			new Comparator<Gene>() {
				public int compare(Gene g1, Gene g2) {
					return ( (g1.seqno ==  g2.seqno) ?
						(g1.pos - g2.pos) :
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
		return(idx);
	}
*/

	/**
	 * locusからsp:nameを取得する。
	 * @param locus locus。
	 * @param gdata GenomeData。
	 * @return sp:name。
	 */
	public String getSpName(String locus, GenomeData gdata) {
		String ret = null;
		try {
			GeneIdx geneIdx = gdata.getGeneIdxByPos(new GenomicLocus(locus));
			Chromosome chrom = geneIdx.chrom;
			int idx = geneIdx.geneIdx;
			if (idx < chrom.genes.size())  {
				System.out.println("GeneHit=>"+idx+" "+chrom.genes.get(idx));
				Gene g = chrom.genes.get(idx);
				ret = g.sp + ":" + g.name;
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			ret = locus;
		}
		return ret;
	}

	CoreCluster getConsClusterByPos(GenomicLocus locus,
				GenomeData gdata, double consRatioCut) {
		CoreCluster cclust = null;
/*
		Chromosome chrom = getChromosomeByPos(locus, gdata);
*/
		GeneIdx geneIdx = gdata.getGeneIdxByPos(locus);
		Gene hitGene = null;
		int idx = geneIdx.geneIdx;
		Chromosome chrom = geneIdx.chrom;
		String refsp = locus.spec;

		if (idx < chrom.genes.size())  {
			System.out.println("GeneHit=>"+idx+" "+chrom.genes.get(idx));
		}
		for (int k = 0; idx+k < chrom.genes.size() || idx-k>=0; k++) {
			if (idx+k < chrom.genes.size()) {
				hitGene = chrom.genes.get(idx+k);
//System.out.println(hitGene);
				cclust = getClusterByGene(refsp+":"+hitGene.name);
				if (cclust != null && cclust.spConsRatio() >= consRatioCut) {
					break;
				}
			}
			if (k > 0 && idx-k >= 0) {
				hitGene = chrom.genes.get(idx-k);
				cclust = getClusterByGene(refsp+":"+hitGene.name);
				if (cclust != null && cclust.spConsRatio() >= consRatioCut) {
					break;
				}
			}
		}
		logger.debug("HitClust="+cclust);

		if (cclust != null) {
			logger.debug("HitGene="+ hitGene);
			logger.debug(cclust.specNum()+" "+gdata.specNum());
		} else {
			throw new Error("The specified position does not exist within the displayed cluster.");
		}
		cclust.prioritizeGene(hitGene);
		return(cclust);
	}
	String getRefSp(){
		if (refsp==null) {
			refsp = species.get(0);
		}
		return(refsp);
	}
	int getRefSpNo(){
		if (refsp != null) {
			return (species.getIdx(refsp));
		} else {
			return(-1);
		}
	}
	void setClusterNamesFromRefSp() {
		int refspNo = species.getIdx(refsp);
		CoreGenomeIterator coreGenomeIter = new CoreGenomeIterator(this);
		while ( coreGenomeIter.hasNext() ) {
			CoreCluster cclust = coreGenomeIter.next();
			cclust.cluster.setNameFromRefSp(refspNo);
		}
	}
	void setAllConnections(GenomeData gdata) {
		if (! make_spindex) {
			makeSpIndex(gdata);
		} else if (make_connection) {
			return;
		}
		for (CoreGenomeBlock blk: blocks) {
			blk.setConnections(GapWin);
		}
		make_connection = true;
	}
	int totalLength() {
		int totlen = 0;
		for (CoreGenomeBlock blk: blocks) {
			totlen += blk.length();
		}
		return(totlen);
	}
	int blockNum() {
		return(blocks.size());
	}
	int total_colnum() {
		return (totalLength() + (blockNum() - 1));
	}
	void redefineBlocks(double nbrConsRatio, int minClustCnt) {
		LinkedList<CoreCluster> clustIDList = new LinkedList<CoreCluster>();
		concatBlocks();
		CoreGenomeBlock block = blocks.get(0);
		CoreGenomeBlock newblock = new CoreGenomeBlock();;
		blocks.clear();
		for (CoreCluster cclust: block) {
			Cluster clust1 = cclust.getCluster();
			if (clustIDList.size() == 0) {
				clustIDList.addFirst(cclust);
				newblock.addCluster(cclust);
				continue;
			}
			if (clustIDList.size() > GapWin) {
				clustIDList.removeLast();
			}
			boolean nbr_flag = false;
			for (CoreCluster cclust2: clustIDList) {
				Cluster clust2 = cclust2.getCluster();
				int[] alldist = clust1.getAllDistances(clust2, spIndex);
				int nbr_spcnt = 0;
				for (int spn = 0; spn < specNum; spn++) {
					if (Math.abs(alldist[spn]) < GapWin) {
						nbr_spcnt++;
//System.out.println("dist="+alldist[spn]);
					}
				}
				if (nbr_spcnt >= specNum * nbrConsRatio) {
					nbr_flag = true;
					break;
				}
			}
			if (! nbr_flag) {
//System.err.println("blklen="+newblock.length());
				if (newblock.length() >= minClustCnt) {
					addBlock(newblock);
				} else {
					System.err.println("too short block");
				}
				newblock = new CoreGenomeBlock();;
				clustIDList.clear();
			}
			newblock.addCluster(cclust);
			clustIDList.addFirst(cclust);
		}
//System.err.println("blklen="+newblock.length());
		if (newblock.length() >= minClustCnt) {
			blocks.add(newblock);
		}
//		assignCoreIdx();
	}
	void assignCoreIdx() {
		int coreidx = 0;
System.out.println("ASSIGN_CORE_IDX");
		for (CoreGenomeBlock blk: blocks) {
			for (CoreCluster cclust: blk) {
				cclust.setCoreIdx(coreidx);
//System.out.println(cclust+" "+" "+cclust.idx+" "+cclust.isCore());
				coreidx++;
			}
		}
	}
	void concatBlocks() {
		int blkid = 0;
		CoreGenomeBlock newblk = null;
		for (CoreGenomeBlock blk: blocks) {
			if (blkid == 0) {
				newblk = blk;
//		System.out.println("size="+newblk.coreClusterList.size());
			} else {
				newblk.coreClusterList.addAll(blk.coreClusterList);
			}
			blkid++;
		}
		blocks.clear();
		blocks.add( newblk );
	}
	/** determine the best order of blocks that reflecting the order in many organisms
	*/
	void reorderBlocks() {
		BlockConnectAll blkConn = new BlockConnectAll(blocks.size());
		for (int spid = 0; spid < specNum; spid++) {
			ArrayList<BorderInfo> borderInfo = new ArrayList<BorderInfo>();
			int blkid = 0;
			for (CoreGenomeBlock blk: blocks) {
				BlockEnd blkend = BlockEnd.Left;
				for (CoreCluster cclust: blk.coreClusterList) {
					LinkedList<DomCluster> mem = cclust.members(spid);
					if (mem.size() > 0) {
						for (DomCluster dcl: mem) {
							BorderInfo binfo = new BorderInfo(blkid, blkend, spid, dcl.order);
							borderInfo.add(binfo);
						}
						break;
					}
				}
				blkend = BlockEnd.Right;
				CoreCluster cclust = null;
				for (ListIterator<CoreCluster> it=blk.coreClusterList.listIterator(blk.coreClusterList.size());
						it.hasPrevious();  ) {
					cclust = it.previous();
					LinkedList<DomCluster> mem = cclust.members(spid);
					if (mem.size() > 0) {
						for (DomCluster dcl: mem) {
							BorderInfo binfo = new BorderInfo(blkid, blkend, spid, dcl.order);
							borderInfo.add(binfo);
						}
						break;
					}
				}
				blkid++;
			}
			borderInfo.sort( (a, b) -> a.order - b.order );
			BorderInfo prev_binfo = null;
			for (BorderInfo binfo: borderInfo) {
				if (prev_binfo != null && prev_binfo.blkid != binfo.blkid
						&& Math.abs(prev_binfo.order - binfo.order) < GapWin) {
					blkConn.add(prev_binfo, binfo);
				}
				prev_binfo = binfo;
			}
		}
		int[] blkOrder = blkConn.getOrder();

/*
		for (int blkn: blkOrder) {
System.out.print(blkn+" ");
		}
System.out.println();
*/

		ArrayList<CoreGenomeBlock> newBlocks = new ArrayList<CoreGenomeBlock>();

//System.err.println("OK:"+blocks.size()+" "+blkOrder.length);
//System.err.println("blkOrder: "+Arrays.toString(blkOrder));
		for (int blkn: blkOrder) {
			if (blkn == 0) {
				break;
			}
			CoreGenomeBlock cblk = blocks.get(Math.abs(blkn)-1);
			if (blkn < 0) {
				cblk.reverse();
			}
			newBlocks.add(cblk);
		}
		blocks = newBlocks;
	}
}
enum BlockEnd {Left, Right};
class BorderInfo {
	int blkid, spid, order;
	BlockEnd blkend;
	BorderInfo(int _blkid, BlockEnd _blkend, int _spid, int _order) {
		blkid = _blkid; blkend = _blkend; spid=_spid; order=_order;
	}
	public String toString() {
		return (blkid + " " + blkend + " " + spid + " " + order);
	}
}

/**
 * connections in all organisms between a pair of block borders, which are used to determine the order of blocks
 * */
class BlockConnectAll {
	/* connection between block borders */
	class BlockConnect {
		BorderInfo binfo1;
		BorderInfo binfo2;
		int count;
		BlockConnect(BorderInfo _binfo1, BorderInfo _binfo2) {
			if (_binfo1.blkid < _binfo2.blkid) {
				binfo1 = _binfo1; binfo2 = _binfo2;
			} else {
				binfo1 = _binfo2; binfo2 = _binfo1;
			}
			count = 1;
		}
		public String toString() {
			return(binfo1.blkid+"("+binfo1.blkend+") "+binfo2.blkid+"("+binfo2.blkend+") "+count+"<<");
		}
	}
	BlockConnect countConnect[][];
	ArrayList<BlockConnect> listConnect;
	int blknum;
	BlockConnectAll(int blknum) {
		countConnect = new BlockConnect[blknum*2][blknum*2];
		listConnect = new ArrayList<BlockConnect>(blknum*2);
		for ( int i = 0; i < blknum*2; i++ ) {
			countConnect[i] = new BlockConnect[blknum*2];
		}
		this.blknum = blknum;
	}
	void add(BorderInfo binfo1, BorderInfo binfo2) {
		if (binfo2.blkid < binfo1.blkid) {
			/* swap binfo1 and binfo2 */
			BorderInfo tmp_binfo = binfo1;
			binfo1 = binfo2;
			binfo2 = tmp_binfo;
		}
		int val1 = binfo1.blkid * 2 + binfo1.blkend.ordinal();
		int val2 = binfo2.blkid * 2 + binfo2.blkend.ordinal();
//System.out.println(val1+" "+val2);
		if (countConnect[val1][val2] == null) {
			BlockConnect bconn = new BlockConnect(binfo1, binfo2);
			listConnect.add(bconn);
			countConnect[val1][val2] = bconn;
		} else {
			countConnect[val1][val2].count++;
		}
	}
	int[] getOrder() {
		class BestConnect {
			// blkid should begin with 1 for distinguishing direction by sign
			int bestConn[][];
			BestConnect() {
				bestConn  = new int[ blknum ][2];
			}
			void put(BorderInfo binfo1, BorderInfo binfo2) {
				// convert 0-based to 1-based
				put(binfo1.blkid+1, binfo1.blkend, binfo2.blkid+1, binfo2.blkend);
			}
			void put(int blkid1, BlockEnd blkend1, int blkid2, BlockEnd blkend2) {
				// relative block direction
				// dir=1: L=>R L=>R or R<=L R<=L; dir=-1: L=R R<=L or R<=L L=>R
				int reldir = (blkend1 != blkend2) ? 1 : -1;
				if ( bestConn[blkid1-1][blkend1.ordinal()] == 0 &&
					bestConn[blkid2-1][blkend2.ordinal()] == 0) {
						bestConn[blkid1-1][blkend1.ordinal()] = (blkid2) * reldir;
						bestConn[blkid2-1][blkend2.ordinal()] = (blkid1) * reldir;
				}
			}
			int get(int blkid, BlockEnd blkend) {
				return(get(blkid, blkend.ordinal()));
			}
			int get(int blkid, int blkend) {
				return(bestConn[blkid-1][blkend]);
			}

			/** for debug: check consisntecy in neighborhood relation in bestConnection */
/*			boolean check() {
				for (int i = 0; i < blknum; i++) {
					System.err.println("##"+i+" "+bestConn[i][0]+" "+bestConn[i][1]);
					for (int blkend = 0; blkend <= 1; blkend++) {
						int blkid = i+1;
						int next = bestConn[i][blkend];
						if (next == 0) continue;
						int next_blkid = Math.abs(next);
						int reldir = (int) Math.signum(next);
						int next_blkend = reldir > 0 ? (1-blkend) : blkend;
						int next_prev = bestConn[next_blkid-1][next_blkend];
						if (Math.abs(next_prev) == blkid) {
						} else {
							System.err.println("ERR:"+blkid+" "+blkend+" "+next_blkid+" "+next_blkend);
							System.err.println(Arrays.toString(bestConn[i]));
							System.err.println(Arrays.toString(bestConn[next_blkid-1]));
							System.exit(1);
							return(false);
						}

					}
				}
				return(true);
			}*/
		}

		BestConnect bestConnect = new BestConnect();
		int blkOrder[] = new int[blknum];
		listConnect.sort(  (a,b) -> b.count - a.count );
		for (BlockConnect bconn: listConnect) {
			bestConnect.put(bconn.binfo1, bconn.binfo2);
//			System.err.println("B:"+bconn);
		}
/* for debug
		for (int i = 0; i < blknum; i++) {
			for (int j = 0; j < 2; j++) {
				System.out.println(i+" "+j+" "+bestConnect.get(i+1, j));
			}
		}
*/
/* for debug
		bestConnect.check();
*/

		int order = 0;
		int blkid = 1;
		BlockEnd blkend;
//		BlockEnd blkendR = BlockEnd.Right;
		boolean flag[] = new boolean[blknum];
		for (int i = 0;  i < blknum; i++) {
			flag[i] = false;
		}
		int next_blkid;
		int numhit = 0;

		while (numhit < blknum) {
			// find unordered block
			for (blkid = 1; blkid <= blknum; blkid++) {
				if (! flag[blkid-1]) {
					break;
				}
			}
//System.out.println("open"+" "+blkid+" "+order+" "+blknum);
			// find leftmost block first
			boolean flag2[] = new boolean[blknum];
			blkend = BlockEnd.Left;
			flag2[blkid-1] = true;
			while ( (next_blkid = bestConnect.get(blkid,blkend)) != 0) {
				blkid = Math.abs(next_blkid);
				// next direction: change if next block has the opposite direction
				if (next_blkid < 0) {
					blkend = (blkend == BlockEnd.Left ? BlockEnd.Right : BlockEnd.Left);
				}
//System.err.println("srchleft"+" "+blkid+" "+order+" "+blkend+" "+blknum);

				if (flag2[blkid-1]) {
					// loop detected
					break;
				}
				flag2[blkid-1] = true;
			}
//System.out.println("left"+" "+blkid+" "+order+" "+blknum);
			if (flag[blkid-1]) {
//System.out.println("break"+" "+order+" "+blknum);
				break;
			}
//System.out.println("1>>"+(blkid-1)+" "+next_blkid+" "+order);
			blkOrder[order++] = blkid;
			flag[blkid-1] = true;
			numhit++;
			// move right: take opposite direction
			blkend = (blkend == BlockEnd.Left ? BlockEnd.Right : BlockEnd.Left);
			while ( (next_blkid = bestConnect.get(blkid, blkend)) != 0) {
				blkid = Math.abs(next_blkid);
				if (next_blkid < 0) {
					blkend = (blkend == BlockEnd.Left ? BlockEnd.Right : BlockEnd.Left);
				}
				if (flag[blkid-1]) {
//System.out.println("break2"+" "+order+" "+blknum);
					break;
				}
				int sign = (blkend == BlockEnd.Right ? 1 : -1);
//System.err.println("srchright"+" "+blkid+" "+order+" "+blkend+" "+blknum+" "+next_blkid);
//System.out.println("2>>"+(blkid-1)+" "+next_blkid+" "+order);
//				blkOrder[order++] = next_blkid;
				blkOrder[order++] = blkid * sign;
				flag[blkid-1] = true;
				numhit++;
			}
		}
		return(blkOrder);
	}
}

class CoreGenomeIterator implements Iterator<CoreCluster> {
	Iterator<CoreGenomeBlock> iter1;
	Iterator<CoreCluster>  iter2;
	CoreGenomeIterator(CoreGenome coreGenome){
		iter1 = coreGenome.blocks.iterator();
		if (! iter1.hasNext()) {
			/* error */
		}
		CoreGenomeBlock cblk = iter1.next();
		iter2 = cblk.coreClusterList.iterator();
	}
	public boolean hasNext() {
		if (iter2.hasNext()) {
			return true;
		} else if (iter1.hasNext()) {
			CoreGenomeBlock cblk = iter1.next();
			iter2 = cblk.coreClusterList.iterator();
			return iter2.hasNext();
		} else {
			return false;
		}
	}
	public CoreCluster next() {
		if (! iter2.hasNext()) {
			CoreGenomeBlock cblk = iter1.next();
			iter2 = cblk.coreClusterList.iterator();
		}
		return iter2.next();
	}
	public void remove () {
		throw new UnsupportedOperationException();
	}
}
class CoreGenomeBlock implements Iterable<CoreCluster> {

	private static Logger logger = LogManager.getLogger(CoreGenomeBlock.class);

	CoreGenome coreGenome;
	ArrayList<CoreCluster> coreClusterList;
/*
	String[]
*/
	SpeciesList species;
	int specNum;
	double refpos;
	int direction;
	BlockType type;
	@Getter
	@Setter
	private String blockNo;

	CoreGenomeBlock() {
		coreClusterList = new ArrayList<CoreCluster>();
	}
	static CoreGenomeBlock create(ClustAliPath alip, ClusterSet cSet, ClusterDir gDir) {
		return create(alip, cSet, gDir, 0.0);
	}

	public void dump() {
		logger.debug("coreClusterList");
		for (CoreCluster cc: this.coreClusterList) {
			cc.dump();
		}
	}

	static CoreGenomeBlock create(ClustAliPath alip, ClusterSet cSet, ClusterDir gDir, double consRatio) {
		CoreGenomeBlock cBlock = new CoreGenomeBlock();
		cBlock.setSpecies(cSet.species);
		ArrayList<Object> aliArray = alip.toArrayList();
		for (int i = 0; i < aliArray.size(); i++) {
			String clustid = (String)aliArray.get(i);
			int dir = gDir.getGdir(clustid);
			Cluster cluster = cSet.getCluster(clustid);
			if (cluster == null) {
				System.err.println("Not found; " + clustid);
			} else if (consRatio > 0.0 && cluster.spConsRatio() < consRatio){
				/* come here when the cluster has been divided or some members have been removed from the original cluster */
				System.err.println("Skip low conserved cluster; " + clustid + " "+cluster.spConsRatio());
			} else {
				CoreCluster cclust = new CoreCluster(cluster, i, dir);
/*
System.out.println("c:"+clustid+" "+cclust.cluster.members[0]);
*/
				cBlock.addCluster(cclust);
			}
		}
		return(cBlock);
	}
	void setBlockType(BlockType _type) {
		type = _type;
		for (CoreCluster cclust: coreClusterList) {
			cclust.setBlockType(_type);
		}
	}
	CoreGenomeBlock getPrevBlock() {
		CoreGenomeBlock prev_blk = coreGenome.blocks.get( coreGenome.blocks.size()-1 );
		for (CoreGenomeBlock blk: coreGenome.blocks) {
			if (blk == this) {
				return prev_blk;
			}
			prev_blk = blk;
		}
		System.err.println("Undefeind CoreGenomeBlock is found");
		return null;
	}
	CoreGenome getCoreGenome() {
		return coreGenome;
	}
	void setCoreGenome(CoreGenome c) {
		coreGenome = c;
	}
	void addCluster(CoreCluster cclust) {
		coreClusterList.add(cclust);
	}
	void setSpecies(String _species[]) {
		species = new SpeciesList(_species);
		specNum = species.spNum();
	}
	void setSpecies(SpeciesList _species) {
		species = _species;
		specNum = species.spNum();
	}
	void setConnections(int gapWin) {
		int blkpos = 0;
		for (CoreCluster cclust: coreClusterList) {
			cclust.setConnection(this, blkpos, gapWin);
			blkpos++;
		}
	}
	/** return median postion in the reference genome */
	void calcRefPos(int refspNo) {
//		int sumpos = 0, count = 0;
		ArrayList<DomCluster> clustList = new ArrayList<DomCluster>();
		for (CoreCluster cclust: coreClusterList) {
			LinkedList<DomCluster> dcl_list = cclust.cluster.members[refspNo];
			if (dcl_list == null || dcl_list.size() == 0)
				continue;
//System.out.println("dcl"+dcl_list);
			DomCluster dcl = dcl_list.get(0);
			clustList.add(dcl);
		}
		Collections.sort(clustList, new Comparator<DomCluster>() {
			public int compare(DomCluster dcl1, DomCluster dcl2) {
				return((int)(dcl1.dom.gene.pos - dcl2.dom.gene.pos));
			}
		});
		if (clustList.size() > 0) {
			refpos = clustList.get( clustList.size() / 2 ).dom.gene.pos;
		} else {
			refpos = 0;
		}
/*
		for (DomCluster dcl: clustList) {
			sumpos += (dcl.dom.gene.pos);
			count++;
		}
		refpos = sumpos / count;
*/
	}
	/** set block direction according to the order in reference genome */
	void setDirectionByRefSp(int refspNo) {
		DomCluster prev_dcl = null;
		int[] sig_count = new int[2];
		for (CoreCluster cclust: coreClusterList) {
			LinkedList<DomCluster> dcl_list = cclust.cluster.members[refspNo];
			if (dcl_list == null || dcl_list.size() == 0)
				continue;
			DomCluster dcl = dcl_list.get(0);
			if (prev_dcl != null) {
				if (dcl.dom.gene.pos > prev_dcl.dom.gene.pos) {
					// forward direction
					sig_count[1]++;
				} else {
					// reverse direction
					sig_count[0]++;
				}
			}
			prev_dcl = dcl;
		}
		if (sig_count[0] > sig_count[1]) {
			reverse();
		}
	}
	void reverse() {
		Collections.reverse( coreClusterList );
		/* reverse each gene direction */
		for (CoreCluster cclust: coreClusterList) {
			cclust.dir *= -1;
		}
		re_index();
	}
	void re_index() {
		int i = 0;
		for (CoreCluster cclust: coreClusterList) {
			cclust.idx = i++;
		}
	}
	void makeOrderHash(HashMap<String,Integer> orderHash) {
		for (CoreCluster cclust: coreClusterList) {
			String id = cclust.cluster.id();
			int idx = cclust.idx;
			orderHash.put(id, idx);
		}
	}
	CoreCluster get(int i) {
		i = i % coreClusterList.size();
		if (i < 0) {
			i += coreClusterList.size();
		}
		return coreClusterList.get(i);
	}
	int length() {
		return coreClusterList.size();
	}
	int specNum() {
		return specNum;
	}
	public Iterator<CoreCluster> iterator() {
		return(coreClusterList.iterator());
	}
	ListIterator<CoreCluster> listIterator() {
		return(coreClusterList.listIterator());
	}
}
class CoreCluster implements ClusterI {
	private static Logger logger = LogManager.getLogger(CoreCluster.class);
	int dir;
	int idx;
	int orig_idx; 	// original idx before filtering
	Cluster cluster;
	int colnum; 	// column position in the graphical output
	ArrayList<Connection>[] connections;	// genes adjacent to this gene(s) on the genome
	CoreCluster[] prev_node_existing;	// previous cluster on the alignment containing a gene of this genome (regardless of adjacency on the genome)
	static int GapWin = 20;

	BlockType type = BlockType.Core;

	@SuppressWarnings("unchecked")
	CoreCluster( Cluster _clust, int _idx, int _dir) {
		cluster = _clust;
		idx = _idx;
		dir = _dir;
		connections = new ArrayList [this.specNum()];
		prev_node_existing = new CoreCluster[this.specNum()];
		setClusterStatus();
	}

	public void dump() {
		logger.debug("CoreCluster");
		this.cluster.dump();
	}

	void setClusterStatus() {
		if (type == BlockType.Core) {
			cluster.status = "core";
		} else if (type == BlockType.Island) {
			cluster.status = "island";
		}
	}
	void setBlockType(BlockType _type) {
		type = _type;
		setClusterStatus();
	}
	boolean isCore() {
		return type == BlockType.Core;
	}
	boolean isIsland() {
		return type == BlockType.Island;
	}
/*
	static void setGapWin(int gapwin) {
		GapWin = gapwin;
	}
*/
	public void setSpeciesList(SpeciesList spec) {
		cluster.setSpeciesList(spec);
	}
	public String id() {
		return(cluster.id);
	}
	public int idx() {
		return(idx);
	}
	String name() {
		return( (cluster.name != null && ! cluster.name.isEmpty()) ? cluster.name : cluster.id);
	}
	public Cluster getCluster() {
		return(cluster);
	}
	public LinkedList<DomCluster> members(int spidx) {
		return(cluster.members(spidx));
	}
	public int specNum() {
		return(cluster.specNum());
	}
	public int spnum() {
		return(cluster.spnum());
	}
	public double spConsRatio() {
		return(cluster.spConsRatio());
	}
	int dir() {
		return(dir);
	}
	void setCoreIdx(int coreidx) {
		idx = coreidx;
		cluster.setCoreIdx(coreidx);
	}
	String clusterInfoString() {
		String infoString = "Cluster: "+id()+"\n";
		infoString += "block type: "+type+"\n";
		return(infoString);
	}

	enum Side {
		LEFT, RIGHT;
	}
	int getMinDist(CoreCluster cclust2, int spno) {
		int mindist = 999999;
		if (cclust2 == null) {
			return (mindist);
		}
		LinkedList<DomCluster> mem = members(spno);
		LinkedList<DomCluster> mem2 = cclust2.members(spno);
		for (DomCluster dcl: mem) {
			for (DomCluster dcl2: mem2) {
				int dist = Math.abs(dcl.order - dcl2.order);
				if (dist < mindist) {
					mindist = dist;
				}
			}
		}
		return(mindist);
	}
	ArrayList<Connection> getConnections(int spno) {
		return( connections[spno]);
	}
	void setConnection(CoreGenomeBlock blk, int blkpos, int GapWin) {
		@SuppressWarnings("unused")
		int LargeValue = 99999;
/*
		CoreGenome coreGenome = CoreGenome.getInstance();
		CoreGenome coreGenome = blk.coreGenome;
*/
//		CoreGenome coreGenome = new CoreGenome();
		CoreGenome coreGenome = blk.getCoreGenome();
		for (int spno = 0; spno < specNum(); spno++) {
			LinkedList<DomCluster> mem = cluster.members(spno);
			if (mem == null) continue;
			for (DomCluster dcl: mem) {
				Connection[] min_conn = new Connection[2];
				int side_check = 2;	// to check if the nearest node is located outside of the gap window.
				for (int j = -GapWin - side_check, idxj = blkpos + j;
						j <= GapWin + side_check && idxj < blk.length(); j++, idxj++) {
					if (j == 0) {
						 continue;
					}
					CoreCluster prev_clust;
					if (idxj < 0) {
						CoreGenomeBlock prev_blk = blk.getPrevBlock();
						if (prev_blk != null) {
							/* last core gene in the previous block */
							prev_clust = prev_blk.get(idxj);
						} else {
							/* only one block: last core gene in this block */
							prev_clust = blk.get(idxj);
/*
System.out.println("idxj="+idxj+" "+blkpos+" "+blk+" "+prev_blk+" :prev_clust="+prev_clust.id()+" :sp="+spno);
*/
						}
					} else {
						prev_clust = blk.get(idxj);
					}
					LinkedList<DomCluster> prev_mem = prev_clust.members(spno);
					if (prev_mem == null) continue;

					if (j < 0 && prev_mem.size() > 0) {
						prev_node_existing[spno] = prev_clust;
					}

					// diff: distance along each genome
					// clstdiff: distance along the alignment
					for (DomCluster prev_dcl: prev_mem) {
						if (dcl.dom.gene.seqno != prev_dcl.dom.gene.seqno)
							continue; // on different sequences
/*
						int dir = (dcl.order > prev_dcl.order) ? 1 : -1;
						int diff = Math.abs(dcl.order - prev_dcl.order);
*/
						int diff = dcl.getDistance(prev_dcl, coreGenome.spIndex[spno]);
						int dir = (diff > 0) ? 1 : -1;
						diff = Math.abs(diff);

						if (diff > GapWin) continue;

						int diridx = (dir+1)/2;		// {-1,1} => {0,1}

						boolean dirinv = false; //check inversion
						if (dcl.dom.gene.dir * prev_dcl.dom.gene.dir != this.dir * prev_clust.dir) {
							dirinv = true;
						}
						boolean fusion = false; //check fusion
						if (dcl.dom.gene == prev_dcl.dom.gene) {
							fusion = true;
						}
/*
if (idxj < 0) {
	System.err.println("idxj="+idxj+" "+dir+" "+diff+" "+j+" "+prev_clust.id());
}
*/
						if (min_conn[diridx] == null) {
							min_conn[diridx] = new Connection(dir, diff, j, dirinv, fusion, prev_clust, dcl, prev_dcl);
						} else if (min_conn[diridx].diff > diff ||
							  (min_conn[diridx].diff == diff && Math.abs(min_conn[diridx].clstdiff) > Math.abs(j)) ) {
							min_conn[diridx].setData(dir, diff, j, dirinv, fusion, prev_clust, dcl, prev_dcl);
						}
					}
				}
				for (Connection conn: min_conn) {
					if (connections[spno] == null) {
						connections[spno] = new ArrayList<Connection>();
					}

					if (conn != null) {
						if (Math.abs(conn.clstdiff) <= GapWin) {
							connections[spno].add(conn);
						}
					}
				}
			}
		}
	}
	LinkedList<DomCluster> getNeighbors(int spNo) {
		LinkedList<DomCluster> mem = cluster.members(spNo);
		for (DomCluster dcl: mem) {
			System.out.println("dcl=" + dcl.clustid);
			for (int j = -GapWin; j < 0; j++) {
			}
		}
		return mem;
	}
	public void prioritizeGene(Gene gene) {
		cluster.prioritizeGene(gene);
	}
}

class Connection {
	int diff, clstdiff, dir;
	CoreCluster prev_node;
	boolean dirinv, fusion;
	DomCluster dcl, prev_dcl;
	Connection(int dir, int diff, int clstdiff, boolean dirinv, boolean fusion, CoreCluster prev, DomCluster dcl, DomCluster prev_dcl) {
		setData(dir, diff, clstdiff, dirinv, fusion, prev, dcl, prev_dcl);
	}
	void setData(int _dir, int _diff, int _clstdiff, boolean _dirinv, boolean _fusion, CoreCluster _prev, DomCluster _dcl, DomCluster _prev_dcl) {
		dir = _dir; diff = _diff; clstdiff = _clstdiff; fusion = _fusion; dirinv = _dirinv;
		prev_node = _prev;
		dcl = _dcl; prev_dcl = _prev_dcl;
	}
	@Override
	public String toString() {
		String ret = "dir=" + dir + ", diff=" + diff + ", clstdiff=" + clstdiff;
		return(ret);
	}
}

class CoreGenomeReader extends ClusterSetReader {

	private static Logger logger = LogManager.getLogger(CoreGenomeReader.class);

//	BufferedReader reader;
	private String filename = null;
	CoreGenome coreGenome;
	private ClusterSet baseClusterSet;
	GenomeData genomeData;
	double cutConsRatio;

	CoreGenomeReader(String filename, GenomeData _genomeData) throws IOException {
		this(filename, null, _genomeData);
		this.filename = filename;
	}
	CoreGenomeReader(String filename, ClusterSet clustSet, GenomeData _genomeData) throws IOException {
		super(filename, _genomeData);
		baseClusterSet = clustSet;
		genomeData = _genomeData;
		this.filename = filename;
	}
	public CoreGenome readCoreGenome() throws IOException {
		return readCoreGenome(false);
	}
	void setConsRatio(double consRatio) {
		cutConsRatio = consRatio;
	}

	/**
	 * 1ファイルにIslandとOtherファイルが複数入っている場合、そのタイプの一覧を取得する。
	 * @return ファイルタイプの一覧。
	 * @throws Exception 例外。
	 */
	public List<String> getFileTypeList() throws Exception {
		List<String> list = new ArrayList<String>();
		Pattern p = Pattern.compile("### (core)|(island)|(other)");
		try (BufferedReader reader = this.newBufferdReader(this.filename)){
			String linebuf = null;
			while ((linebuf = reader.readLine()) != null) {
				if (linebuf.length() == 0) {
					continue;
				}
				if (linebuf.charAt(0) == '#') {
					Matcher m = p.matcher(linebuf);
					if (m.find()) {
						String[] sp = linebuf.split(" ");
						list.add(sp[1]);
					}
				}
			}
		}
		return list;
	}

	public CoreGenome readCoreGenome(boolean ignore_block) throws IOException {
		CoreGenomeBlock cblk = null;
		String linebuf = null;
		String strarray[];
		String cid, name;
		int dir, blkid;
		int curr_blkid = 0;
		int coreidx = 0;
		int orig_coreidx = 0;

		Map<String, Cluster> clusterMap = new HashMap<String, Cluster>();
		logger.debug("readCoreGenome " + this.filename);
		coreGenome = new CoreGenome();
		try (BufferedReader reader = this.newBufferdReader(this.filename)){
			while ((linebuf = reader.readLine()) != null) {
				if (linebuf.length() == 0) {
					continue;
				}
				if (linebuf.charAt(0) == '#') {
					readHeader(linebuf);
					continue;
				}
				strarray = linebuf.split("\t");
				cid = strarray[0];
				name = strarray[1];
				dir = Integer.parseInt(strarray[2]);
				if (strarray[3].indexOf('.') >= 0) {
					String[] blkids = strarray[3].split("\\.");
					blkid = Integer.parseInt(blkids[0]) * 100 + Integer.parseInt(blkids[1]);
				} else {
					blkid = Integer.parseInt(strarray[3]);
				}
				if ( (! ignore_block && blkid != curr_blkid) || cblk == null) {
 					cblk = new CoreGenomeBlock();
					curr_blkid = blkid;
					cblk.setBlockNo(strarray[3]);
					coreGenome.addBlock(cblk);
				}
				Cluster cluster = null;
				if (baseClusterSet != null) {
					cluster = baseClusterSet.getCluster(cid);
				}
				if (cluster == null) {
					cluster = new Cluster(coreGenome.specNum(), cid);
					readClusterMembers(cluster, strarray, 4);
				}
				if (cluster.spConsRatio() < cutConsRatio) {
					// skip reading
					orig_coreidx++;
					continue;
				}

				cluster.setName(name);
				cluster.setSpeciesList(coreGenome.species);
				clusterMap.put(cluster.id, cluster);
				CoreCluster ccluster = new CoreCluster(cluster, coreidx, dir);
/*
				if (cutConsRatio > 0) {
				}
*/
				ccluster.orig_idx = orig_coreidx;

				ccluster.setCoreIdx(coreidx);
				coreidx++;
				orig_coreidx++;

				cblk.addCluster(ccluster);
			}
		} catch (IOException e) {
			throw e;
		}
/*
		coreGenome.makeSpIndex(genomeData);
		coreGenome.setAllConnections(genomeData);
*/
		coreGenome.setClusterMap(clusterMap);
		return coreGenome;
	}

	/**
	 * データを読み飛ばす。
	 * @param type "core" or "island" or "otherXX"を指定する。
	 * @throws IOException 例外。
	 */
	private void skipData(final String type, final BufferedReader reader) throws IOException {
		logger.info("skipData type=" + type);
		Pattern p = Pattern.compile("### (core)|(island)|(other)");
		String linebuf = null;
		while ((linebuf = reader.readLine()) != null) {
			if (linebuf.length() == 0) {
				continue;
			}
			if (linebuf.charAt(0) == '#') {
				logger.info("linebuf=" + linebuf);
				Matcher m = p.matcher(linebuf);
				if (m.find()) {
					if (linebuf.indexOf(type) >= 0) {
						break;
					}
				} else {
					this.readHeader(linebuf);
				}
			}
		}
	}


	/**
	 * 指定されたタイプのデータを読み込む。
	 * @param type "core" or "island" or "otherXX"を指定する。
	 * @return ゲノムデータ。
	 * @throws IOException IO例外。
	 */
	public CoreGenome readCoreGenome(final String type) throws IOException {
		boolean ignore_block = false;
		CoreGenomeBlock cblk = null;
		String linebuf = null;
		String strarray[];
		String cid, name;
		int dir, blkid;
		int curr_blkid = 0;
		int coreidx = 0;
		int orig_coreidx = 0;

		Map<String, Cluster> clusterMap = new HashMap<String, Cluster>();
		logger.debug("readCoreGenome " + this.filename);
		coreGenome = new CoreGenome();
		try (BufferedReader reader = this.newBufferdReader(this.filename)){
			this.skipData(type, reader);
			while ((linebuf = reader.readLine()) != null) {
				if (linebuf.length() == 0) {
					continue;
				}
				if (linebuf.indexOf("###") >= 0) {
					break;
				}
				if (linebuf.charAt(0) == '#') {
					continue;
				}
				strarray = linebuf.split("\t");
				cid = strarray[0];
				name = strarray[1];
				dir = Integer.parseInt(strarray[2]);
				if (strarray[3].indexOf('.') >= 0) {
					String[] blkids = strarray[3].split("\\.");
					blkid = Integer.parseInt(blkids[0]) * 100 + Integer.parseInt(blkids[1]);
				} else {
					blkid = Integer.parseInt(strarray[3]);
				}
				if ( (! ignore_block && blkid != curr_blkid) || cblk == null) {
 					cblk = new CoreGenomeBlock();
					curr_blkid = blkid;
					cblk.setBlockNo(strarray[3]);
					coreGenome.addBlock(cblk);
				}
				Cluster cluster = null;
				if (baseClusterSet != null) {
					cluster = baseClusterSet.getCluster(cid);
				}
				if (cluster == null) {
					cluster = new Cluster(coreGenome.specNum(), cid);
					readClusterMembers(cluster, strarray, 4);
				}
				if (cluster.spConsRatio() < cutConsRatio) {
					// skip reading
					orig_coreidx++;
					continue;
				}

				cluster.setName(name);
				cluster.setSpeciesList(coreGenome.species);
				clusterMap.put(cluster.id, cluster);
				CoreCluster ccluster = new CoreCluster(cluster, coreidx, dir);
/*
				if (cutConsRatio > 0) {
				}
*/
				ccluster.orig_idx = orig_coreidx;

				ccluster.setCoreIdx(coreidx);
				coreidx++;
				orig_coreidx++;

				cblk.addCluster(ccluster);
			}
		} catch (IOException e) {
			throw e;
		}
/*
		coreGenome.makeSpIndex(genomeData);
		coreGenome.setAllConnections(genomeData);
*/
		coreGenome.setClusterMap(clusterMap);
		return coreGenome;
	}






	void readHeader(String linebuf) {
		String headerInfo[] = linebuf.substring(1).split("[ \t]+");
		String species[] = null;
		if (headerInfo.length == 2) {
			if (headerInfo[0].equals("Genomes")) {
				species = headerInfo[1].split(",");
			} else if (headerInfo[0].equals("Block")) {
			}
		} else if (headerInfo.length == 1) {
			species = linebuf.substring(1).split(",");
		} else {
		}
		if (species != null) {
			coreGenome.setSpecies(species);
		}
	}
	/** override */
	int specNum() {
		return coreGenome.specNum();
	}
	String getSpecies(int i) {
		return coreGenome.species.get(i);
	}
}
class CoreGenomeWriter {
	ClusterOutFile cout;
	PrintWriter writer;
	CoreGenome coreGenome;
	boolean original_format;
	boolean useStdOut = false;
	int formatVersion;

	CoreGenomeWriter() throws IOException {
		initialize(null, null);
	}
	CoreGenomeWriter(CoreGenome _coreGenome) throws IOException {
		initialize(_coreGenome, null);
	}
	CoreGenomeWriter(CoreGenome _coreGenome, String filename) throws IOException {
		initialize(_coreGenome, filename);
	}
	void initialize(CoreGenome _coreGenome, String filename) throws IOException{
		createWriter(filename);
		coreGenome = _coreGenome;
		cout = new ClusterOutFile(writer);
		original_format = false;
		formatVersion = 1;
	}
	void setPosOrtho(int flag_posortho) {
		cout.setPosOrtho(flag_posortho);
	}

	void createWriter(String filename) throws IOException{
//System.err.println("FILE="+filename);
		if (filename == null) {
			/* read from standard input */
			writer = new PrintWriter(System.out);
			useStdOut = true;
		} else {
			File outfile = new File(filename);
			try {
				writer = new PrintWriter(new BufferedWriter(
					new FileWriter(outfile) ) );
			} catch (IOException e) {
				throw e;
			}
		}
	}

	void outputHeader() {
		writer.print("#Genomes	");
		for (int i = 0; i < coreGenome.species.spNum(); i++) {
			if (i > 0) writer.print(",");
			writer.print(coreGenome.species.get(i));
		}
		writer.println();
	}
	void outputText() {
		int blockid = 0;
		outputHeader();
		for (Iterator<CoreGenomeBlock> iter = coreGenome.blocks.iterator(); iter.hasNext(); ) {
			CoreGenomeBlock cblock = iter.next();
			blockid++;
			outputBlock(cblock, blockid);
		}
		writer.flush();
	}
	void outputText(CoreGenome _coreGenome) {
		coreGenome = _coreGenome;
		outputText();
	}
	void outputBlock(CoreGenomeBlock cblock, int blockid) {
		outputBlock(cblock, String.valueOf(blockid));
	}
	void outputBlock(CoreGenomeBlock cblock, String blockid) {
		for (Iterator<CoreCluster> iter2 = cblock.iterator(); iter2.hasNext(); ) {
			CoreCluster cclust = iter2.next();
			outputCluster(cclust, blockid);
		}
	}
	void outputCluster(CoreCluster cclust, int blockid) {
		outputCluster(cclust, String.valueOf(blockid));
	}
	void outputCluster(CoreCluster cclust, String blockid) {
		writer.print(cclust.id() + "\t" +
				cclust.name() + "\t" +
					cclust.dir() + "\t");
		if (original_format) {
			for (int i = 0; i < cclust.specNum(); i++) {
				LinkedList<DomCluster> mem = cclust.members(i);
				if (mem.size() > 0){
					writer.print("*");
				}
				writer.print("\t");
			}
			writer.print(blockid);
		} else {
			if (formatVersion > 1) {
				// newversion
				writer.print(cclust.cluster.getStatus());
				writer.print("\t");
			}
			writer.print(blockid);
			cout.writeClusterMembers(cclust.cluster);
		}
		writer.println();
	}
	void outputOrigCluster(Cluster clust) {
		String clname = clust.name() != null ? clust.name() : clust.id();
		writer.print(clust.id() + "\t" + clname + "\t" +
			// no information of dir, status and blockid
			0 + "\t" + "" + "\t" + "");
		cout.writeClusterMembers(clust);
		writer.println();
	}
	void close() {
		if (useStdOut) {
			writer.flush();
		} else {
			writer.close();
		}
	}
}
/* should be removed */
class CoreGeneOrder_OBS {
	class ClusterInfo {
		int spNo;
		CoreCluster cclust;
		DomCluster dcl;
		ClusterInfo(int _spNo, CoreCluster _cclust, DomCluster _dcl) {
			spNo = _spNo;
			cclust = _cclust;
			dcl = _dcl;
		}
		public String toString() {
			return("["+dcl+"]");
		}
	}
	CoreGenome coreGenome;
	ArrayList<ArrayList<ClusterInfo>> clustList;
	HashMap<String, Integer> coreIdxHash;

	CoreGeneOrder_OBS(CoreGenome _coreGenome) {
		coreGenome = _coreGenome;
		clustList = new ArrayList<ArrayList<ClusterInfo>>();
		coreIdxHash = new HashMap<String, Integer>();
		create();
	}
	void create() {
		for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
			clustList.add( new ArrayList<ClusterInfo>() );
		}
		int coreIdx = 1;
		for (CoreGenomeBlock blk: coreGenome.blocks) {
			for (CoreCluster cclust: blk) {
				boolean skip = false;
				for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
					String sp = coreGenome.species.get(spNo);
					LinkedList<DomCluster>mem = cclust.members(spNo);
					if (mem.size() == 1) {
					} else if (mem.size() == 2) {
						if (Math.abs(mem.get(0).order - mem.get(0).order) <= 1){
						} else {
							skip = true;
							break;
						}
					} else {
						skip = true;
						break;
					}
				}
				if (! skip) {
					ArrayList<ClusterInfo> cList = new ArrayList<ClusterInfo>(coreGenome.specNum());
					for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
						String sp = coreGenome.species.get(spNo);
						LinkedList<DomCluster>mem = cclust.members(spNo);
						DomCluster dcl = mem.get(0);
						ClusterInfo clInfo = new ClusterInfo(spNo, cclust, dcl);
						clustList.get(spNo).add(clInfo);
					}
					coreIdxHash.put(cclust.id(), coreIdx++);
				}
			}
		}
		for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
			clustList.get(spNo).sort( (a,b) -> (a.dcl.order - b.dcl.order ) );
		}
	}
	void output() {
		PrintWriter writer = new PrintWriter(System.out);
		for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
			writer.println(">"+coreGenome.species.get(spNo));
			for (ClusterInfo clInfo: clustList.get(spNo)) {
				writer.print(" ");
				if (clInfo.dcl.dir() < 0) {
					writer.print("-");
				}
				int coreIdx = coreIdxHash.get(clInfo.cclust.id());
				writer.print(coreIdx);
			}
			writer.println(" $");
		}

/*
		for (String sp: coreGenome.species) {
		}
*/
	}

}
