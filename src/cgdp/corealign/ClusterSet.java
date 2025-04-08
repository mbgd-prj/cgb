package cgdp.corealign;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.corealign.GenomeData.GeneInfo;

class ClusterSet implements Serializable, Iterable<Cluster> {
	/**
	 * serialVersionUID。
	 */
	private static final long serialVersionUID = -1958789720681435778L;

/*
	String[] species;
	HashMap<String,Integer>  spHash;
*/
	SpeciesList species;

	HashMap<String,Cluster> clusterHash;
	ArrayList<Cluster> clusterList;
	SpIndex spIndex[];
	int specNum;
	static ClusterSet _clusterSet = null;

	public ClusterSet() {
		clusterList = new ArrayList<Cluster>();
		clusterHash = new HashMap<String, Cluster>();
	}
	public ClusterSet(ClusterSet origSet) {
		this();
		setSpecies(origSet.species);
	}
	static ClusterSet getInstance() {
		if (_clusterSet == null) {
			_clusterSet = new ClusterSet();
		}
		return _clusterSet;
	}
	public void setSpecies(SpeciesList _species) {
		species = _species;
		specNum = species.spNum();
	}
	public void setSpecies(String[] _species) {
		species = new SpeciesList(_species);
		setSpecies(species);
/*
		species = _species;
		specNum = _species.length;
		createSpHash();
*/
	}
	public void setSpecies(LinkedList<String> _species) {
		species = new SpeciesList(_species);
		specNum = species.spNum();
/*
		specNum = _species.size();
		species = new String[specNum];
		ListIterator<String> iter = _species.listIterator(0);
		int i = 0;
		while (iter.hasNext()) {
			 species[i++] = iter.next();
		}
		createSpHash();
*/
	}
/*
	void createSpHash() {
		spHash = new HashMap();
		for (int i = 0; i < species.length; i++) {
			spHash.put(species[i], i);
		}
	}
*/
	void add(Cluster cluster) {
		clusterList.add(cluster);
		clusterHash.put(cluster.id, cluster);
		cluster.setSpeciesList(species);
	}
	int specNum() {
		return(specNum);
	}
	int size() {
		return(clusterList.size());
	}
	Cluster getCluster(String clustid) {
/* DELETED 2020/4/21
		//------ removing #num from cid, is it correct?  Should be checked !!!
		int idx = clustid.indexOf('#');
		if (idx >= 0) {
			clustid = clustid.substring(0, idx);
		}
		//------
*/
		return(clusterHash.get(clustid));
	}
	LinkedList<DomCluster> getClusterData(String clustid, String sp) {
		return getClusterData(clustid, sp, false);
	}
	LinkedList<DomCluster> getClusterData_DupCheck(String clustid, String sp) {
		return getClusterData(clustid, sp, true);
	}
	LinkedList<DomCluster> getClusterData(String clustid, String sp, boolean dupFlag) {
		if (dupFlag) {
			DupCidCheck dupCheck = DupCidCheck.getInstance();
			if (dupCheck.isNewCID(clustid)) {
				clustid = dupCheck.getOrigID(clustid);
			}
		}
		Cluster cluster = clusterHash.get(clustid);
		if (cluster == null) {
			return null;
		} else if (sp == null) {
			return(cluster.members[0]);
		} else {
/*
			int specNo = spHash.get(sp);
*/
			int specNo = species.getIdx(sp);
			return(cluster.members[specNo]);
		}
	}
	DomCluster getClusterData1(String clustid, String sp) {
		return getClusterData1(clustid, sp, false);
	}
	DomCluster getClusterData1_DupCheck(String clustid, String sp) {
		return getClusterData1(clustid, sp, true);
	}
	DomCluster getClusterData1(String clustid, String sp, boolean dupFlag) {
		if (dupFlag) {
			DupCidCheck dupCheck = DupCidCheck.getInstance();
			if (dupCheck.isNewCID(clustid)) {
				clustid = dupCheck.getOrigID(clustid);
			}
		}
		LinkedList<DomCluster> list = getClusterData(clustid, sp);
		if (list != null && list.size() > 0 ) {
			return ((DomCluster) list.get(0));
		} else {
			return null;
		}
	}
	void setClusterNamesFromRefSp(String refsp) throws Exception {
/*
		if (! spHash.containsKey(refsp)) {
			throw new Exception();
		}
		int refspNo = spHash.get(refsp);
*/
		if (! species.exists(refsp)) {
			throw new Exception();
		}
		int refspNo = species.getIdx(refsp);
		for (Cluster clust: clusterList) {
			clust.setNameFromRefSp(refspNo);
		}
	}
	Set<DomClusterWithDist> getNbrClusterSet(String clustid, int spNo, DomCluster dcl_center, int win) {
		int order = dcl_center.order;
		int seqno = dcl_center.dom.gene.seqno;
		Set<DomClusterWithDist> nbrSet = new HashSet<DomClusterWithDist>();
		DomCluster dcl;
		int directions[] = {-1, 1};
		for (int dir: directions) {
			for (int w = 1; w <= win; w++) {
				int k = order + w * dir;
				dcl = spIndex[spNo].getByChrIdx(seqno, k);
				if (dcl != null) {
					if (dcl.dom.gene.seqno != seqno) {
						// should not come here
						System.out.println("Error: "+seqno+" != "+dcl.dom.gene.seqno);
						continue;
					}
					// cluster information with a directed distance
					DomClusterWithDist dclinfo = new DomClusterWithDist(dcl, w * dir);
					nbrSet.add(dclinfo);
				} else {
					break;
				}
			}
		}
		return nbrSet;
	}
	void setClusterData(String clustid, String sp, LinkedList<DomCluster> members) {
		Cluster cluster = clusterHash.get(clustid);
		if (cluster == null) {
			cluster = new Cluster(specNum, clustid);
			clusterHash.put(clustid, cluster);
		}
/*
		int specNo = spHash.get(sp);
*/
		int specNo = species.getIdx(sp);
//		cluster.members[specNo.intValue()] = members;
		cluster.setMembers(specNo, members);
	}
	void changeClustID(LinkedList<DomCluster> updList, String newid, String sp) {
		LinkedList<DomCluster> data = getClusterData(newid, sp);
		Iterator<DomCluster> iter;
		if (data != null) {
		// assign delete flag to all the original data
			for (iter = data.iterator(); iter.hasNext(); ) {
				DomCluster d = (DomCluster) iter.next();
				d.clustid = "deleted";
			}
		}
		// assign newid to the list for update
		for (iter = updList.iterator(); iter.hasNext(); ) {
			DomCluster d = (DomCluster) iter.next();
			d.clustid = newid;
		}
		// reassign cluster data
		setClusterData(newid, sp, updList);
	}
	void orderByAliPath(AlignmentPath aliPath) {
		ArrayList<Cluster> newClusterList = new ArrayList<Cluster>();
		for (Iterator<ClustAliPath> iter = aliPath.iterator(); iter.hasNext(); ) {
			ClustAliPath alip = (ClustAliPath)iter.next();
//System.out.println("alip="+alip);
			ArrayList<Object> aliArray = alip.toArrayList();
			for (int j = 0; j < aliArray.size(); j++) {
				String name = (String)aliArray.get(j);
				Cluster cluster = getCluster(name);
				if (cluster == null) {
					System.err.println("Not found; "+name);
				} else {
					newClusterList.add(cluster);
				}
			}
		}
		clusterList = newClusterList;
	}
	SpIndex getSpIndex(String spec) {
/*
		int spNo = spHash.get(spec);
*/
		int spNo = species.getIdx(spec);
		return(spIndex[spNo]);
	}
	void makeSpIndex(GenomeData gdata) {
		spIndex = new SpIndex[species.spNum()];
		for (int i = 0; i < species.spNum(); i++) {
			Genome genome = gdata.getGenome(species.get(i));
			spIndex[i] = new SpIndex(i, clusterList, genome);
		}
	}
	public Iterator<Cluster> iterator() {
		return(clusterList.iterator());
	}
}
class ConsClusterFilter {
	double minSpRatio;
	int minSpNum;
	GenomeData gdata;
	ConsClusterFilter(double _minSpRatio, GenomeData _gdata) {
		minSpRatio = _minSpRatio;
		gdata  = _gdata;
	}
	ConsClusterFilter(int _minSpNum, GenomeData _gdata) {
		minSpNum  = _minSpNum;
		gdata  = _gdata;
	}
	ClusterSet filter(ClusterSet clustSet) {
		if (minSpNum == 0 && minSpRatio > 0) {
			minSpNum = (int) Math.ceil(clustSet.specNum() * minSpRatio);
		}
		ClusterSet newClustSet = new ClusterSet(clustSet);
		for (Cluster clust: clustSet.clusterList) {
			if (clust.spnum() >= minSpNum) {
				newClustSet.add(clust);
			}
		}
		newClustSet.makeSpIndex(gdata);
		return(newClustSet);
	}
}

/** List of species which can be accessed either by name or by index (spNo) */
class SpeciesList {
	ArrayList<String> species;
	HashMap<String,Integer>spHash;
	int spnum;
	SpeciesList() {
		spnum = 0;
		species = new ArrayList<String>();
		spHash = new HashMap<String,Integer>();
	}
	SpeciesList(String _species[]) {
		this();
		add(_species);
	}
	SpeciesList(List<String> _species) {
		this();
		add(_species);
	}
	void add(String spec) {
		if (! spHash.containsKey(spec)) {
			species.add(spec);
			spHash.put(spec, spnum);
			spnum++;
		}
	}
	void add(String _species[]) {
		for (String spec: _species) {
			add(spec);
		}
	}
	void add(List<String> _species) {
		for (String spec: _species) {
			add(spec);
		}
	}
	String[] toArray() {
		String[] array = new String[spnum];
		return(species.toArray(array));
	}
	/** Get species name by index */
	String get(int idx) {
//		System.out.println("sp="+idx+" "+species.size());
		return(species.get(idx));
	}
	boolean exists(String spec) {
		return(spHash.containsKey(spec));
	}
	/** Get index by species name */
	int getIdx(String spec) {
		Integer idx = spHash.get(spec);
		if (idx == null) {
			return -1;
		}
		return(idx);
	}
	int spNum() {
		return(spnum);
	}
}


class SpIndex implements Iterable<DomCluster> {
	int specNo;
	ArrayList<DomCluster> clustList;	// ordered list of clusters according to chromosomal positions
	ArrayList<Integer> seqnoIdx;		// storing the begining position (index (order) in clustList) of each chromosome.
	ArrayList<ClusterI> origClustList;	// list of original cluster (Cluster) instead of cluster assinment info (DomCluster)
	Genome genome;
	SpIndex(int _specNo, ArrayList<?> clusterList, Genome _genome) {
		this(_specNo, clusterList, _genome, false);
	}
	SpIndex(int _specNo, ArrayList<?> clusterList, Genome _genome, boolean flagCreateOrigClusterList) {
		specNo = _specNo;
		genome = _genome;
		@SuppressWarnings("unchecked")
		Iterator<ClusterI> iter = (Iterator<ClusterI>) clusterList.iterator();
		clustList = new ArrayList<DomCluster>(200);
		seqnoIdx = new ArrayList<Integer>(5);
		if (flagCreateOrigClusterList) {
			origClustList = new ArrayList<ClusterI>(200);
		}
		while (iter.hasNext()) {
			ClusterI cluster = (ClusterI) iter.next();
//			Cluster cluster = clusterI.getCluster();
//			@SuppressWarnings("unchecked")
			Iterator<DomCluster> iter2 = cluster.members(specNo).iterator();
			while (iter2.hasNext()) {
				DomCluster domClust = (DomCluster) iter2.next();
if (domClust.dom.gene==null) {
System.out.println(domClust+"//"+domClust.dom+"//"+domClust.dom.gene);
}
				clustList.add(domClust);
				if (flagCreateOrigClusterList) {
					origClustList.add(cluster);
				}
			}
		}
		class ComparePosition implements Comparator<DomCluster> {
			public int compare(DomCluster obj1, DomCluster obj2) {
				DomInfo dom1 = obj1.dom;
				DomInfo dom2 = obj2.dom;
				if (dom1.gene.seqno != dom2.gene.seqno) {
					return(dom1.gene.seqno - dom2.gene.seqno);
				}
				if (dom1.gene == dom2.gene) {
					return(dom1.gene.dir * dom1.domNo -
							dom2.gene.dir * dom2.domNo);
				} else {
					return((int)(dom1.gene.pos - dom2.gene.pos));
				}
			}
		}
		Collections.sort( clustList, new ComparePosition() );
		Iterator<DomCluster>iter3 = clustList.iterator();
		int ord = 0;
		int prev_seqno = -1;
		int idx = 0;
		while (iter3.hasNext()) {
			DomCluster domCluster = iter3.next();
			domCluster.order = ord++;
			if (domCluster.dom.gene.seqno != prev_seqno) {
				seqnoIdx.add(idx);
				prev_seqno = domCluster.dom.gene.seqno;
				while (seqnoIdx.size() < prev_seqno) {
					seqnoIdx.add(idx);
				}
			}
			idx++;
		}
		seqnoIdx.add(idx);
	}
	DomCluster getByChrIdx(int seqno, int idx) {
		DomCluster domClust=null;
		ChrShape shape = Chromosome.defaultShape();
		if (genome.chromosomes.size() > 0) {
			Chromosome chrom = genome.chromosomes.get(seqno-1);
			shape = chrom.shape;
		}
		int seqlen = seqnoIdx.get(seqno) - seqnoIdx.get(seqno-1);
		int idxBegin = seqnoIdx.get(seqno-1);
		int idxOrig = idx;

		if (shape == ChrShape.circular) {
			int modulo_pos = (idx - idxBegin) % seqlen;
			if (modulo_pos < 0) {
				modulo_pos += seqlen;
			}
			idx = idxBegin + modulo_pos;
		}
		if (idx >= 0 && idx < clustList.size()) {
			domClust = (DomCluster) clustList.get(idx);
			if (domClust.dom.gene.seqno != seqno) {
				if (shape == ChrShape.circular) {
					System.out.println("chrom No mismatch in circular chromosome position: "+seqno+" "+domClust.dom.gene.seqno+": "+shape+"// "+idxOrig+">>"+idx+": "+idxBegin);
				}
				domClust = null;
			}
		} else {
//System.out.println("idx="+idx+" "+domClust);
		}
		return domClust;
	}
	int getSeqLen(int seqno) {
		return (seqnoIdx.get(seqno) - seqnoIdx.get(seqno-1));
	}
	DomCluster getByIdx(int idx) {
		DomCluster domClust=null;
		idx %= clustList.size();
		if (idx < 0) {
			idx += clustList.size();
		}
		domClust = (DomCluster) clustList.get(idx);
		return(domClust);
	}
	public Iterator<DomCluster> iterator() {
		return(clustList.iterator());
	}
	int binsearch(DomCluster dcl) {
		return (Collections.binarySearch(clustList, dcl, (a,b) -> (a.order - b.order) ));
	}
	void print() {
		Iterator<DomCluster> iter = clustList.iterator();
		while (iter.hasNext()) {
			DomCluster domCluster = (DomCluster) iter.next();
			System.out.println(domCluster);
		}
	}
	void setCoreIndexInterval(int specNo) {
		Iterator<DomCluster> iter = clustList.iterator();
		@SuppressWarnings("unused")
		int curr_coreid1;
		LinkedList<DomCluster> list = new LinkedList<DomCluster>();
		while (iter.hasNext()) {
			DomCluster dclust = (DomCluster) iter.next();
			if (dclust.isCore()) {
 				int coreid = dclust.getCoreIdx();
				list = null;
				curr_coreid1 = coreid;
			} else {
				list.push(dclust);
			}
		}
	}
	int length() {
		return(clustList.size());
	}
}
// for getNbrClusterSet
class DomClusterWithDist {
	DomCluster domClust;
	int dist;
	DomClusterWithDist(DomCluster _domClust, int _dist) {
		domClust = _domClust;
		dist = _dist;
	}
}

class ClusterDir {
	HashMap<String, Integer> GdirHash;
	ClusterDir() {
		GdirHash = new HashMap<String, Integer>();
	}
	int getGdir(String clustid) {
		Integer dir = (Integer) GdirHash.get(clustid);
		if (dir == null) {
			return 0;
		}
		return dir.intValue();
	}
	void setGdir(String clustid, int dir) {
		GdirHash.put(clustid, Integer.valueOf(dir));
	}
	/** copy dir value from clustid1 to clustid2 */
	void copyGdir(String clustid1, String clustid2) {
		setGdir( clustid2, getGdir(clustid1)  );
	}
	void inverseGdir(String clustid) {
		setGdir( clustid, getGdir(clustid) * -1 );
	}
}
interface ClusterI {
	String id();
	int specNum();
	int spnum();
	public double spConsRatio();
	LinkedList<DomCluster> members(int i);
	Cluster getCluster();
	void setSpeciesList(SpeciesList species);
	void prioritizeGene(Gene gene);
}
class Cluster implements ClusterI, Serializable {
	private static final long serialVersionUID = 3057346666983417057L;

	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(Cluster.class);

	SpeciesList species;
	LinkedList<DomCluster> members[];
	String id;
	String name;
	int size, spnum;
	int specNum;
	static SpGroupCounter spGrpCounter;
	String status;
	final static int LARGE_VALUE = 99999999;

	@SuppressWarnings("unchecked")
	Cluster(int spn, String clustid) {
		members = new LinkedList[spn];
		for (int i = 0; i < spn; i++) {
			members[i] = new LinkedList<DomCluster>();
		}
		if (spGrpCounter != null) {
			spGrpCounter.init();
		}
		id = clustid;
		specNum = spn;
		status = "";
	}

	public void dump() {
		logger.info("Cluster id=" + id + ", name=" + name);
		if (this.species != null) {
			for (String sp: this.species.species) {
				logger.debug("sp=" + sp);
			}
		}
		for (LinkedList<DomCluster> list: this.members) {
			for (DomCluster dc: list) {
				dc.dump();
			}
		}
	}

	/**
	 * 指定したGeneを含むかどうかを判定します。
	 * @param gi Geneの情報。
	 * @return 含む場合true。
	 */
	public boolean containing(final GeneInfo gi) {
		boolean ret = false;
		for (LinkedList<DomCluster> list: this.members) {
			for (DomCluster dc: list) {
				if (gi.getSp().equals(dc.dom.spec) && gi.getName().equals(dc.dom.name)) {
					ret = true;
					break;
				}
			}
		}
		return ret;
	}

	/**
	 * 指定した生物種のGeneを取得する。
	 * @param sp 生物種。
	 * @return Gene。
	 */
	public Gene getGene(final String sp) {
		Gene gene = null;
		for (LinkedList<DomCluster> list: this.members) {
			for (DomCluster dc: list) {
//				dc.dump();
				if (sp.equals(dc.dom.spec)) {
					gene = dc.dom.gene;
					break;
				}
			}
		}
		return gene;
	}

	public void setSpeciesList(SpeciesList spec) {
		species = spec;
	}
	public Cluster getCluster() {
		/* return self object */
		return this;
	}
	static void setSpGroupCounter(SpGroupCounter cntr) {
		spGrpCounter = cntr;
	}
	public LinkedList<DomCluster> members(int i) {
		return(members[i]);
	}
	int addMember(String str, int specNo, GeneData genes) {
		DomInfo dom;
		DomCluster dclust;
		if (spGrpCounter != null) {
			//count spgroup
			spGrpCounter.found(specNo);
			spnum = spGrpCounter.count();
		} else if (members[specNo].size()==0) {
			// new species
			spnum++;
		}
		dom = new DomInfo(str, genes);
		if (dom.gene == null) {
			return -1;
		}
		dclust = new DomCluster(dom, id);
		members[specNo].add(dclust);
		size++;
		return 0;
	}
	void setMembers(int specNo, LinkedList<DomCluster> list) {
		LinkedList<DomCluster> oldlist = members[specNo];
		members[specNo] = list;
		spnum += ( (list.size() > 0 ? 1 : 0) - (oldlist.size() > 0 ? 1 : 0) );
		size += (list.size() - oldlist.size());
	}
	void setName(String _name) {
		name = _name;
	}
	void setNameFromRefSp(int refspNo) {
		LinkedList<DomCluster> list = members[refspNo];
		if (list != null && ! list.isEmpty()) {
			DomCluster dcl = list.getFirst();
			if (dcl != null) {
				name=dcl.dom.gene.name;
			}
		}
	}
	void setStatus(String stat) {
		status = stat;
	}
	String getStatus() {
		return(status);
	}
	boolean statusEquals(String stat) {
		return (status.equals(stat));
	}
	public String id() {
		return(id);
	}
	String name() {
		return(name);
	}
	public int size() {
		return(size);
	}
	public int size_posOrtho() {
		int i;
		int count = 0;
		for (i = 0; i < members.length; i++) {
			Iterator<DomCluster> iter = members[i].iterator();
			while (iter.hasNext()) {
				DomCluster gene = (DomCluster) iter.next();
				if (gene.isPosOrtho()) {
					count++;
				}
			}
		}
		return(count);
	}
	/** total number of species */
	public int specNum() {
		return(specNum);
	}
	/** the number of species included in this cluster */
	public int spnum() {
		return(spnum);
	}
	public HashSet<Integer> spSet() {
		HashSet<Integer> spSet = new HashSet<Integer>();;
		for (int i = 0; i < members.length; i++) {
			if (members[i].size()>0) {
				spSet.add(i);
			}
		}
		return(spSet);
	}
	public double spConsRatio() {
		return((double) spnum / specNum);
	}
	/** set core index to each member gene for findMobile */
	void setCoreIdx(int coreidx) {
		for (int i = 0; i < specNum; i++) {
			Iterator<DomCluster> iter = members[i].iterator();
			while (iter.hasNext()) {
				DomCluster dc = (DomCluster) iter.next();

				dc.setCoreIdx(coreidx);
			}
		}
	}
	int[] getAllDistances(Cluster clust2, SpIndex spIndex[]) {
		int[] mindist = new int[specNum];
		for (int spn = 0; spn < specNum; spn++) {
			mindist[spn] = LARGE_VALUE;
			for (DomCluster dcl1: members[spn]) {
				for (DomCluster dcl2: clust2.members[spn]) {
					int dist = dcl1.getDistance(dcl2, spIndex[spn]);
					if (Math.abs(dist) < Math.abs(mindist[spn])) {
						mindist[spn] = dist;
					}
				}
			}
		}
		return(mindist);
	}
	/** Move the specified gene at first in the member list of that species (inparalogs), which will be located center when centering */
	public void prioritizeGene(Gene gene) {
		System.err.println("species=" + species);
		System.err.println("gene=" + gene);
		if (this.species != null) {
			int spi = species.getIdx(gene.sp);
			LinkedList<DomCluster> memb = members[spi];
			if (memb != null) {
				for (DomCluster dcl: memb) {
					if (gene.equals(dcl.dom.gene)) {
						System.out.println("Move first>>>>"+dcl.dom.gene);
						memb.remove(dcl);
						memb.addFirst(dcl);

						String text = dcl.dom.gene.sp + ":" + dcl.dom.gene.name;
						CompareMap.getViewer().getLocusInput().setText(text);
						break;
					}
				}
			}
		}
	}
}

class DomCluster implements Serializable{

	private static final long serialVersionUID = 8804370808291641268L;

	private static Logger logger = LogManager.getLogger(DomCluster.class);

	DomInfo dom;
	String clustid;
	int order;
	int coreidx1, coreidx2;
	boolean pos_ortho;
	DomCluster(DomInfo _dom, String _clustid) {
		dom = _dom; clustid = _clustid;
		coreidx2 = -2;
		pos_ortho = false;
	}

	public void dump() {
		logger.info("domNo=" + this.dom.domNo + ", name=" + this.dom.name + ", spec=" + this.dom.spec);
		logger.info("clustid=" + this.clustid + ", order=" + this.order + ", coreidx1=" + this.coreidx1 + ", coreidx2=" + this.coreidx2);
	}

	int dir() {
		return dom.gene.dir;
	}
	String clustid() {
		return clustid;
	}
	void setPosOrtho() {
		pos_ortho = true;
	}
	void unsetPosOrtho() {
		pos_ortho = false;
	}
	boolean isPosOrtho() {
		return(pos_ortho);
	}
	public String getName() {
		return(dom.getName());
	}
	public String toString(){
		return(dom + " " + clustid + " " + order);
	}
	/* methods for findMobile */
	/** set core index for findMobile */
	void setCoreIdx(int _coreidx) {
		coreidx1 = coreidx2 = _coreidx;
	}
	void setCoreIdxInterval(int _coreidx1, int _coreidx2) {
		coreidx1 = _coreidx1;
		coreidx2 = _coreidx2;
	}
        boolean isCore() {
                return(coreidx1 == coreidx2);
        }
	int getCoreIdx() {
		if (isCore()) {
			return coreidx1;
		} else {
			return -1;
		}
	}
	int getDistance(DomCluster dcl2, SpIndex spIndex) {
		int seqno = dom.gene.seqno;
		if ( dcl2.dom.gene.seqno != seqno ) {
			return(99999);
		}
		int dist = order - dcl2.order;
		int length = spIndex.getSeqLen(seqno);
		Chromosome chrom = spIndex.genome.getChromosome(seqno);
		ChrShape shape = (chrom == null) ? Chromosome.defaultShape() : chrom.getShape();
		if (shape == ChrShape.circular) {
			if (dist > length / 2) {
				dist = length - dist;
			} else if (dist < - length / 2) {
				dist = - length - dist;
			}
		}
		return(dist);
	}
}
class DomInfo implements Serializable {
	private static final long serialVersionUID = 3483110726340882599L;
	String spec, name;
	int domNo;
	Gene gene;
	static Pattern pat_order, pat_dom;

	DomInfo(String _spec, String _name, int _domNo, GeneData genes) {
		spec = _spec; name = _name; domNo = _domNo;
		gene = genes.getGene(spec, name);
	}
	DomInfo(String str, GeneData genes) {
		String[] tmpnames = str.split(":");
		if (tmpnames.length == 0) {
		} else if (tmpnames.length >= 2) {
			spec = tmpnames[0];
			name = tmpnames[1];
		} else {
			name = tmpnames[0];
		}

//		Pattern pat;
		Matcher mat;

		// remove [order] info added to the gene name
		if (pat_order == null) {
			pat_order = Pattern.compile("\\[(\\d+)\\]$");
		}
		mat = pat_order.matcher(name);
		if (mat.find()) {
			name = mat.replaceFirst("");
			@SuppressWarnings("unused")
			String orderS = mat.group(1);
		}

		// get domain info if exists
		if (pat_dom == null) {
			pat_dom = Pattern.compile("\\((\\d+)\\)$");
		}
		mat = pat_dom.matcher(name);
		if (mat.find()) {
			name = mat.replaceFirst("");
			String domNoS = mat.group(1);
			if (domNoS != null) {
				domNo = Integer.valueOf(domNoS).intValue();
			}
		}
		gene = genes.getGene(spec, name);
	}
	public String getName() {
		return(spec + ":" + name);
	}
	public String toString() {
		String ret = spec + ":" + name;
//ret += gene + " ";
		if (domNo > 0) {
			 ret += ("(" + domNo + ")");
		}
		return(ret);
	}
}
