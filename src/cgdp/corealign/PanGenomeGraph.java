package cgdp.corealign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

class PanGenomeGraph {
	static String consoutFile = "aaa";
	static String linkoutFile = "lll";
	static String tripletoutFile = "ttt";
	static int MinClustCnt = 1;
	static String refsp = null;
	static boolean skip_findbest = false;
	static double orthoTolerance = 0.6;

	static final int LEFT = 0;
	static final int RIGHT = 1;

	ConsNbrPairForNonCore consPairNonCore;
	CoreGenome coreGenomeAlign;
	CoreGenome nonCoreGenomeAlign;
	ClusterSet clustSet;
	SlinkClust slinkClust;
	IslandCluster[] islClust;


	static PanGenomeGraph create(CoreGenome coreGenome, ClusterSet cSet, GenomeData gData, SpGroup spGroup) {
		PanGenomeGraph pang = new PanGenomeGraph();
		ClusterSet nonCore = new ClusterSet(cSet);
		Iterator iter = cSet.iterator();

/*
		while (iter.hasNext()) {
			Cluster clust = (Cluster) iter.next();
//			for (DomCluster dcl: clust.members[0]) {
//				System.out.println("dcl:>"+dcl);
//			}
			if (! clust.statusEquals("core")) {
				nonCore.add(clust);
			}
		}
*/
		boolean calcTriplet = false;

/*
		nonCore.makeSpIndex();
*/


		ClusterOutFile cout = null;
		try {
			cout = new ClusterOutFile();
		} catch (Exception e) {
			System.err.println("file open error");
		}

//		cout.writeClusterSet(nonCore);

		NbrTriplet nbrTrip = new NbrTriplet(cSet);
		ConsNbrPairForNonCore consPair= new ConsNbrPairForNonCore(cSet, nbrTrip, spGroup);
//		consPair.setNbrConsRatio(0.8);
//		consPair.unsetNbrConsNum2();
		consPair.checkNeighbor();
		if (consoutFile != null) {
			consPair.savePairLink(consoutFile);
		}

		ClusterDir gDir =new ClusterDir();
		LinkDir linkDir = new LinkDir(cSet, consPair.nbrGraph, gDir);
		linkDir.checkLinkDir();
		if (linkoutFile != null) {
			linkDir.saveLinks(linkoutFile);
		}

		Graph inGraph;

		if (calcTriplet) {
			TripletGraph tripGraphBuild = new TripletGraph(
					linkDir.newGraph, nbrTrip);
			Graph tripGraph = tripGraphBuild.makeTripletGraph();

			if (tripletoutFile != null) {
				tripGraph.print(tripletoutFile);
			}

			inGraph = tripGraph;
		} else {
			inGraph = linkDir.newGraph;
		}

		SlinkClust slink = new SlinkClust(inGraph);
		slink.clustering();

		FeedbackSet fsetBuild = new FeedbackSet(inGraph);
		Set fbkSet = fsetBuild.makeFset();
		fsetBuild.removeFset(inGraph);

		AlignmentPathBuilder aliBuild =
			new AlignmentPathBuilder(cSet, inGraph);
		aliBuild.set_MinClustCnt(MinClustCnt);
		AlignmentPath rawCorePath = aliBuild.findMaxPath();

		DupCidCheck dupCheck = DupCidCheck.getInstance();
		AlignmentPathConverter aliConv =
			new AlignmentPathConverter(rawCorePath, dupCheck, gDir);


		AlignmentPath newCorePath;
		if (calcTriplet) {
			newCorePath = aliConv.restoreTriplet();
		} else {
			newCorePath = rawCorePath;
		}

		if (refsp != null) {
			newCorePath.sortPath(refsp, cSet, gDir);
		}

                if (! skip_findbest) {
                        FindBestOrthologs fBest = new FindBestOrthologs(
                                                cSet, newCorePath, dupCheck);
                        fBest.setOrthoTolerance(orthoTolerance);
                        fBest.findBestAll();
                }


		CoreGenome genomeAli = CoreGenome.create(newCorePath, cSet, gDir);
		genomeAli.makeSpIndex(gData);
		genomeAli.setBlockType(BlockType.Island);
/*
		genomeAli.reorderBlocks();
*/


/*
		iter = nonCore.iterator();
		while (iter.hasNext()) {
			Cluster clst = (Cluster) iter.next();
			findMobile(clst);
		}
*/

		pang.consPairNonCore = consPair;
		pang.nonCoreGenomeAlign = genomeAli;
		pang.coreGenomeAlign = coreGenome;
		pang.clustSet = cSet;
		pang.slinkClust = slink;
		pang.islClust = pang.makeIslandCluster();
		return pang;
	}
	void outputNonCoreAlign() {
		CoreGenomeWriter core_out = null;
		try {
			core_out = new CoreGenomeWriter(nonCoreGenomeAlign);
		} catch (IOException e) {
			System.err.println("Can't output core genome data");
		}
//		core_out.setPosOrtho(flag_posortho);
		core_out.outputText();
	}
	void outputIsland() {
		CoreGenomeWriter core_out = null;

		for (int clid = 0; clid < islClust.length; clid++) {
			int blkid = 0;
			for (IslandBlock iblk: islClust[clid].blocks) {
				blkid++;
				String blkid_str = String.format("%d.%d", clid+1, blkid);
				System.out.println("#Block "+blkid_str+"\t"+iblk.mobInfo.mobility()+"\t"
					+iblk.mobInfo.getAdjacentRegionAsString() );
			}
		}

		try {
			core_out = new CoreGenomeWriter(nonCoreGenomeAlign);
		} catch (IOException e) {
			System.err.println("Can't output core genome data");
		}
		core_out.outputHeader();
		for (int clid = 0; clid < islClust.length; clid++) {
			int blkid = 0;
			for (IslandBlock iblk: islClust[clid].blocks) {
				blkid++;
				String blkid_str = String.format("%d.%d", clid+1, blkid);
				core_out.outputBlock(iblk, blkid_str);
			}
		}
		core_out.close();
	}
	IslandCluster[] makeIslandCluster() {
		Iterator iter = nonCoreGenomeAlign.blocks.iterator();
		IslandCluster islClust[] = new IslandCluster[slinkClust.clustNum()];
		int blkid = 0;
		while (iter.hasNext()) {
			CoreGenomeBlock cblk = (CoreGenomeBlock) iter.next();

			/* convert to islandBlock by adding mobInfo */
			MobilityInfo mobInfo = checkMobilityBlock(cblk);
			IslandBlock iblk = new IslandBlock(cblk, mobInfo);

			for (int i = 0; i < cblk.length(); i++) {
				CoreCluster cclust = cblk.get(i);
				String clid = cclust.id();
				int sclid = slinkClust.getClustID(clid);
				if (islClust[sclid] == null) {
					islClust[sclid] = new IslandCluster();
				}
				islClust[sclid].addBlock(iblk);
				break;
			}
			blkid++;
		}
		Arrays.sort(islClust, (a,b)-> a.size() - b.size());
		return(islClust);
	}

	void findMobile(Cluster clst) {

	}
/*
	boolean checkMobility() {
		Iterator iter = nonCoreGenomeAlign.blocks.iterator();
		int blkid = 0;
		while (iter.hasNext()) {
			blkid++;
			CoreGenomeBlock cblk = (CoreGenomeBlock) iter.next();
			MobilityInfo mobInfo = checkMobilityBlock(cblk);
			IslandBlock iblk = new IslandBlock(cblk, mobInfo);

			System.out.println("#Block "+blkid+"\t"+mobInfo.mobility()+"\t"
				+mobInfo.getAdjacentRegionAsString() );
		}
		return true;
	}
*/
	MobilityInfo checkMobilityBlock(CoreGenomeBlock cblk) {

		Set<String> internal_clustids = new HashSet<String>(); // cluster ids included in this block
		Iterator iter = cblk.coreClusterList.iterator();
		while (iter.hasNext()) {
			CoreCluster cc = (CoreCluster) iter.next();
			internal_clustids.add(cc.id());
		}

		double MIN_CNT_RATIO = 0.4;
		double MIN_CNT = cblk.length() * MIN_CNT_RATIO;
		if (MIN_CNT < 2) MIN_CNT = 2;

		Map<Integer,Integer>count_L = new HashMap<Integer,Integer>();
		Map<Integer,Integer>count_R = new HashMap<Integer,Integer>();

		Set<Integer> found_nbrcore_L = new HashSet<Integer>();
		Set<Integer> found_nbrcore_R = new HashSet<Integer>();

		DomCluster dcl1, dcl2;
		for (int spNo = 0; spNo < cblk.specNum(); spNo++) {

			/* skip this species if it does not contain sufficient number of genes in this block */
			Iterator iter_cc = cblk.coreClusterList.iterator();
			int cnt_cc = 0;
			while (iter_cc.hasNext()) {
				CoreCluster cc = (CoreCluster) iter_cc.next();
				if (cc.members(spNo).size() > 0) {
					cnt_cc++;
				}
			}
			if (cnt_cc < MIN_CNT) {
				continue;
			}

			found_nbrcore_L.clear();
			found_nbrcore_R.clear();

			findAdjacentCore(cblk, spNo, 1, internal_clustids, found_nbrcore_L);
			findAdjacentCore(cblk, spNo, -1, internal_clustids, found_nbrcore_R);

//System.out.println("L:"+found_nbrcore_L);
//System.out.println("R:"+found_nbrcore_R);
			for (Integer coreidx: found_nbrcore_L) {
				int cnt = (count_L.containsKey(coreidx) ? count_L.get(coreidx).intValue() : 0);
				count_L.put( coreidx, cnt + 1 );
			}
			for (Integer coreidx: found_nbrcore_R) {
				int cnt = (count_R.containsKey(coreidx) ? count_R.get(coreidx).intValue() : 0);
				count_R.put( coreidx, cnt + 1 );
			}
		}
		MobilityInfo mobinfo = new MobilityInfo(count_L, count_R);

		return mobinfo;
	}
	private void findAdjacentCore(CoreGenomeBlock cblk, int spNo, int srch_dir, Set<String> internal_clustids, Set<Integer> found_nbrcore) {
		boolean found_nbrcore_flag = false;
		DomCluster dcl1, dcl2;
		int index = (srch_dir > 0) ? 0 : cblk.coreClusterList.size();
		ListIterator iter_cc = cblk.coreClusterList.listIterator(index);
//System.out.println("DIR="+srch_dir+" "+index);
		while (srch_dir > 0 ?  iter_cc.hasNext() : iter_cc.hasPrevious()) {
			CoreCluster cc = (CoreCluster) ((srch_dir > 0) ? iter_cc.next() : iter_cc.previous());
			Iterator iter_mem = cc.members(spNo).iterator();
//System.out.println("ccid:"+cc.id());
			while (iter_mem.hasNext()) {
				dcl1 = (DomCluster) iter_mem.next();
//System.out.println("clust1:"+dcl1.clustid);
				int rel_dir = cc.dir() * dcl1.dir(); // relative direction against the consenseus direction
				int ord = dcl1.order;
				int[] dirs = {-1, 1};
				boolean found_sameblk_flag = false;
				for (int dir: dirs) {
				    for (int kk = 1; ; kk++) {
					int k = ord + kk * dir * rel_dir;
					dcl2 = clustSet.spIndex[spNo].getByIdx(k);
					String clustid = dcl2.clustid();
//System.out.println("clust2:"+dcl2.clustid+" "+kk);
					if (internal_clustids.contains(clustid)) {
						found_sameblk_flag = true;
//System.out.println("found_sameblk");
						break;
					}
					int coreidx = dcl2.getCoreIdx();
//System.out.println("coreidx="+coreidx);
					if (coreidx >= 0) {
						// record adjacent core cluster
//System.out.println("Core: "+spNo+" "+coreidx+" "+dcl1.clustid+" "+dcl2.clustid+" "+dir);
						found_nbrcore.add(Integer.valueOf(coreidx));
						found_nbrcore_flag = true;
						break;
					}
				    }
				    if (found_nbrcore_flag) {
					break;
				    }
				}
			}
			if (found_nbrcore_flag) {
				return;
			}
		}
	}
}

/**
 * ConsNbrPair class for non-core genome alignment where a relaxed parameter set is used.
 */
class ConsNbrPairForNonCore extends ConsNbrPair {
	double maxConsRatio = 0.0;
        ConsNbrPairForNonCore(ClusterSet clSet, NbrTriplet ntrp, SpGroup spgrp) {
                super(clSet, ntrp, spgrp);
		/* When c=nbr(a,b) and |a|<=|b|, NbrConsRatio=|c|/|a| and NbrConsRatio2=|c|/|b| */
                setNbrConsRatio(0.65);
                setNbrConsRatio2(0.4);
                setMaxMinDist(4);
                unsetNbrConsNum2();
		maxConsRatio = 0.6;
        }
	/* skip if the cluster is belonging to core or well conserved */
        boolean checkClusterStatus(Cluster cluster) {
                if (cluster.status.equals("core")) {
                        return false;
                } else if (maxConsRatio > 0 && cluster.spConsRatio() > maxConsRatio) {
			return false;
                } else {
                        return true;
                }
        }
        boolean checkClusterStatus(String clustid) {
                Cluster cluster = clustSet.getCluster(clustid);
		if (cluster==null) {
			// assuming a core cluster assigned a modified name
			return false;
		}
                return checkClusterStatus(cluster);
        }
}

enum Mobility {
	STABLE, SEMI_STABLE, MOBILE
}

class MobilityInfo {
	int minL, maxL, minR, maxR;
	int maxcntL, maxcntR, freqL, freqR;
	Mobility mobility;
	final int MAX_RANGE = 20;
	MobilityInfo ( Map<Integer,Integer> count_L, Map<Integer,Integer> count_R) {
		calcAdjacentCore(count_L, count_R);
	}
	void calcAdjacentCore( Map<Integer,Integer> count_L, Map<Integer,Integer> count_R) {
		minL = Integer.MAX_VALUE;
		maxL = -Integer.MAX_VALUE;
		minR = Integer.MAX_VALUE;
		maxR = -Integer.MAX_VALUE;
		maxcntL = 0;
		maxcntR = 0;
		freqL = -1;
		freqR = -1;

		for (Integer coreidx: count_L.keySet()) {
			int cnt = count_L.get(coreidx);
//System.out.println("Count_L: "+coreidx+" "+cnt);
			if (minL > coreidx) minL = coreidx;
			if (maxL < coreidx) maxL = coreidx;
			if (maxcntL < cnt) {
				maxcntL = cnt;;
				freqL = coreidx;
			}
		}
		for (Integer coreidx: count_R.keySet()) {
			int cnt = count_R.get(coreidx);
//System.out.println("Count_R: "+coreidx+" "+cnt);
			if (minR > coreidx) minR = coreidx;
			if (maxR < coreidx) maxR = coreidx;
			if (maxcntR < cnt) {
				maxcntR = cnt;;
				freqR = coreidx;
			}
		}
	}
	void calcMobility() {
		boolean rangeL = maxL - minL + 1 < MAX_RANGE;
		boolean rangeR = maxR - minR + 1 < MAX_RANGE;
		boolean rangeInt = false;
		if (maxR < minL) {
			rangeInt = minL - maxR + 1 < MAX_RANGE;
		} else if (maxL < minR) {
			rangeInt = minR - maxL + 1 < MAX_RANGE;
		} else {
			rangeInt = false;
			// regions are overlapped
		}
//System.out.println("L:"+minL+":"+maxL+",R:"+minR+":"+maxR);
//System.out.println("L:"+rangeL+",R:"+rangeR+",I:"+rangeInt);

		if (rangeL && rangeR && rangeInt) {
			mobility = Mobility.STABLE;
		} else if (rangeL || rangeR) {
			mobility = Mobility.SEMI_STABLE;
		} else {
			mobility = Mobility.MOBILE;;
		}
	}
	String getAdjacentRegionAsString() {
		return("L:"+minL+"-"+maxL+";R:"+minR+"-"+maxR);
	}
	Mobility mobility() {
		if (mobility == null) {
			calcMobility();
		}
		return mobility;
	}
}

class IslandBlock extends CoreGenomeBlock {
	CoreGenomeBlock cblk;
	MobilityInfo mobInfo;
	IslandBlock(CoreGenomeBlock _cblk, MobilityInfo _mobInfo) {
		cblk = _cblk;
		mobInfo = _mobInfo;
	}
	public Iterator iterator() {
		return cblk.iterator();
	}
}
class IslandCluster extends CoreGenome {
	int clustNum;
	int size;
	ArrayList<IslandBlock> blocks;
	ArrayList<CoreCluster> clusters;
	IslandCluster() {
		blocks = new ArrayList<IslandBlock>();
		size = 0;
	}
	void addBlock(IslandBlock iblk) {
		blocks.add(iblk);
		size += iblk.length();
	}
	int size() {
		return size;
	}
/*
	void outputText() {
		for (int i = 0; i < blocks.length(); i++) {
			CoreGenomeBlock cblk = blocks.get(i);
			CoreGenomeWriter.outputCluster(cblk);
		}
	}
*/
}

