package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;

class ConsNbrPair {
	NbrTriplet nbrTrip;
	ClusterSet clustSet;
	SpGroup spGroup;
	String delim = ";";
	double ReargPen = 0.25;
	double SubLinkPen = 0.25;
	HashMap nbrClustHash;

	/** cutoff distance of the neighboring gene pair */
	int GAPWIN = 20;
	/** cutoff of minimum distance between a neighboring OG pair; 0: ineffective */
	int MAX_MINDIST = 0;
	/** minimum number of genomes satisfying neighboring relation in a neighboring OG pair */
	int NBR_CONS_NUM;
	int NBR_CONS_NUM2 = 2;

	/** minimum ratio of genomes satisfying neighboring relation in a neighboring OG pair (denominator: smaller OG); 0: ineffective */
	double NBR_CONS_RATIO = 0.0;
	/** minimum ratio of genomes satisfying neighboring relation in a neighboring OG pair (denominator: larger OG); 0: ineffective */
	double NBR_CONS_RATIO2 = 0.0;
	UndirectedGraph nbrGraph;

	ConsNbrPair(ClusterSet clSet, NbrTriplet ntrp, SpGroup spgrp) {
		clustSet = clSet;
		nbrTrip = ntrp;
		spGroup = spgrp;
	}
	void setNbrConsNum(int nbrConsNum) {
		NBR_CONS_NUM = nbrConsNum;
	}
	void setNbrConsNum2(int nbrConsNum2) {
		NBR_CONS_NUM2 = nbrConsNum2;
		nbrTrip.setConsNum(NBR_CONS_NUM2);
	}
	void unsetNbrConsNum2() {
		setNbrConsNum2(0);
	}
	void setGapWin(int gapWin) {
		GAPWIN = gapWin;
	}
	void setMaxMinDist(int mindist) {
		MAX_MINDIST = mindist;
	}
	void setNbrConsNum_ByRatio(double ratio) {
		NBR_CONS_NUM = (int) Math.floor(ratio * clustSet.specNum);
	}
	void setNbrConsNum2_ByRatio(double ratio) {
		NBR_CONS_NUM2 = (int) Math.floor(ratio * clustSet.specNum);
		if (NBR_CONS_NUM2 < 2) {
			NBR_CONS_NUM2 = 2;
		}
		nbrTrip.setConsNum(NBR_CONS_NUM2);
	}
	void setNbrConsRatio(double ratio) {
		NBR_CONS_RATIO = ratio;
	}
	void setNbrConsRatio2(double ratio) {
		NBR_CONS_RATIO2 = ratio;
	}

	boolean check_NBR_CONS(NbrClustData nclust) {
		if (NBR_CONS_RATIO == 0) {
			return satisfy_NBR_CONS(nclust.maxcnt, NBR_CONS_NUM);
		} else if (NBR_CONS_RATIO > 0) {
			Cluster cl1 = clustSet.getCluster(nclust.clustid1);
			Cluster cl2 = clustSet.getCluster(nclust.clustid2);
			int min_spnum = Math.min(cl1.spnum(), cl2.spnum());
			int max_spnum = Math.max(cl1.spnum(), cl2.spnum());

/*
if (min_spnum > 1 && satisfy_NBR_CONS( nclust.maxcnt, min_spnum * NBR_CONS_RATIO )){
System.out.println("#"+nclust.clustid1+" "+nclust.clustid2+" "+nclust.maxcnt+" "+min_spnum+" "+NBR_CONS_RATIO);
}
*/
			if (min_spnum > 1 && satisfy_NBR_CONS( nclust.maxcnt, min_spnum * NBR_CONS_RATIO )) {
				if (NBR_CONS_RATIO2 > 0) {
					return (max_spnum > 1 && satisfy_NBR_CONS( nclust.maxcnt, max_spnum * NBR_CONS_RATIO2 ));
				} else {
					return true;
				}
			} else {
				return false;
			}
		}
		/* never come here */
		return false;
	}
	boolean satisfy_NBR_CONS(int cnt, double mincnt) {
		return (cnt >= mincnt);
	}
	/* older version: obsolete */
	boolean satisfy_NBR_CONS_GT(int cnt, double mincnt) {
		return (cnt > mincnt);
	}
	boolean satisfy_NBR_CONS2(int cnt) {
		/* false if NBR_CONS_NUM2 == 0 */
		return (NBR_CONS_NUM2 > 0 && cnt >= NBR_CONS_NUM2);
//		return (cnt >= NBR_CONS_NUM2);
	}
/*
	boolean satisfy_NBR_CONS(int cnt) {
		return (cnt > NBR_CONS_NUM);
	}
*/

	public void readPairs(String filename) throws IOException {
		BufferedReader reader;
		String buf = null;
		try {
			reader = new BufferedReader( new FileReader(filename) );
		} catch(IOException e) {
			throw(e);
		}
		while (	(buf = reader.readLine()) != null ) {
		}
	}
	public void checkNeighbor() {
		Iterator iter = clustSet.iterator();
		Cluster cluster;
		nbrGraph = new UndirectedGraph();
		while (iter.hasNext()) {
			cluster = (Cluster) iter.next();
			if (! checkClusterStatus(cluster)) {
				continue;
			}
			checkNeighbor0(cluster);
		}
		checkSubLink();
	}
	boolean checkClusterStatus(Cluster cluster) {
		/* do nothing: should be overridden in subclass */
		return true;
	}
	boolean checkClusterStatus(String clustid) {
		/* do nothing: should be overridden in subclass */
		return true;
	}
	void checkNeighbor0(Cluster cluster) {
		String clustid1 = cluster.id;
//		DomCluster dcl1, dcl2;
		DomCluster dcl1;
		TreeMap distHash = new TreeMap();
		class TmpClustData {
			double dist;
			int reldir;
			TmpClustData(double _dist, int _reldir) {
				dist = _dist; reldir = _reldir;
			}
		}
//System.out.println("cl1="+clustid1);
		for (int spNo = 0; spNo < clustSet.specNum; spNo++) {
			String spname = clustSet.species.get(spNo);
			String spgrp = (spGroup != null) ?
				spGroup.getSpGroup(spname) : spname;
			Iterator iter = cluster.members[spNo].iterator();
			int win = GAPWIN;

			while (iter.hasNext()) {
				dcl1 = (DomCluster) iter.next();
				int ord = dcl1.order;
				String name = dcl1.getName();
				int dir1 = dcl1.dir();
				Set<DomClusterWithDist> nbrClstSet = clustSet.getNbrClusterSet(clustid1, spNo, dcl1, win);

				for (DomClusterWithDist dclinfo: nbrClstSet) {
					DomCluster dcl2 = dclinfo.domClust;
					int dist = dclinfo.dist;
					int dir2 = dcl2.dir();

					if (dcl2.equals(dcl1)) {
						continue;
					}
					if (dcl2.clustid().equals(clustid1)) {
						continue;
					}
					if (! checkClusterStatus(dcl2.clustid)) {
						continue;
					}
					int side = (dir1 * dist > 0) ?
						Direction.RIGHT : Direction.LEFT;

					nbrTrip.add(name, side,
						dcl2.clustid(), Math.abs(dist));

/*
System.out.println("dist="+dist);
if (dist==0) {
	System.out.println(dcl1.clustid()+":"+dcl1.order+" "+dcl2.clustid()+":"+dcl2.order+";"+k);
}
*/
					String key = dcl2.clustid() + delim + spgrp;
					TmpClustData cld =
					    (TmpClustData) distHash.get(key);
/*
if (clustid1.equals("604")) {
System.out.println(name+" "+dcl1.clustid+" "+dcl2.clustid()+","+dcl2.getName()+" "+dist);
if (cld != null) System.out.println(cld.dist);
}
*/

					if (cld==null || cld.dist > Math.abs(dist)) {
					    int reldir = Direction.getRelDir(
						side, dir1 * dir2);
					    TmpClustData cldata =
						new TmpClustData(
						    Math.abs(dist), reldir);
					    distHash.put(key, cldata);
					}
				}
//				System.out.println(dcl1+" "+dir1);
			}
		}
		Set keyset = distHash.keySet();
		Iterator iter = keyset.iterator();
		String previd, clustid, spgrp;
		TmpClustData cldata;
		int curr_reldir;
		int count = 0;
		double dirweight[] = new double[4];
		int dircnt[] = new int[4];
		for (int i = 0; i < 4; i++) {
			dirweight[i] = 0;
			dircnt[i] = 0;
		}
		previd = null;
		clustid = null;
		nbrClustHash = new HashMap();
		double min_dist = -1;

		/* create nbrClustHash {nbr_clustid => nbrClustData} */
		while (iter.hasNext()) {
			String keystr = (String) iter.next();
			String[] keys = keystr.split(delim);
			clustid = keys[0];
			spgrp = keys[1];

			if (count > 0 && ! clustid.equals(previd)) {
				if (MAX_MINDIST == 0 || min_dist <= MAX_MINDIST) {
//System.out.println("add="+previd+" "+clustid1+"; dist="+min_dist);
					NbrClustData nclst = makeNbrData(clustid1, previd, count, dirweight, dircnt);
					nbrClustHash.put(previd, nclst);
				}
//System.out.println(">"+clustid1+" "+previd + " " + nclst.maxdir + " " + nclst.weight);
				for (int i = 0; i < 4; i++) {
					dirweight[i] = 0;
					dircnt[i] = 0;
				}
				count = 0;
				min_dist = -1;
			}

			cldata = (TmpClustData) distHash.get(keystr);
				
			if (min_dist < 0 || cldata.dist < min_dist) {
				min_dist = cldata.dist;
//System.out.println("min>"+min_dist+" "+keystr);
			}
			curr_reldir = cldata.reldir;
			dirweight[curr_reldir] += CalWeight.calc(
							(double) cldata.dist);
			dircnt[curr_reldir]++;

			count++;
			previd = clustid;
		}

//System.out.println("count="+count);
		if (count > 0) {
			if (MAX_MINDIST == 0 || min_dist <= MAX_MINDIST) {
				NbrClustData nclst = makeNbrData(clustid1, clustid, count, dirweight, dircnt);
				nbrClustHash.put(clustid, nclst);
//System.out.println(">"+clustid1+" "+clustid + " " + nclst.maxdir + " " + nclst.weight);
			}
		}
		HashMap countProxClust = new HashMap();
		HashMap countProxSubL = new HashMap();
		HashMap countProxSubR = new HashMap();
		HashSet nbrClustSel = new HashSet();
		for (int spNo = 0; spNo < clustSet.specNum; spNo++) {
			String spec = clustSet.species.get(spNo);
			LinkedList cdata = clustSet.getClusterData(clustid1, spec);
			iter = cdata.iterator();
			while (iter.hasNext()) {
				DomCluster dclst = (DomCluster) iter.next();
				String proxL, proxR, key;
				HashSet subProxSetL = new HashSet();
				HashSet subProxSetR = new HashSet();

				proxL = checkProximalNeighbor(dclst.getName(),
					Direction.LEFT, subProxSetL);
				if (proxL != null) {
					hashCntIncr(countProxClust,proxL);
/*
					nbrClustSel.add(proxL);
*/
					Iterator iter2 = subProxSetL.iterator();
					while (iter2.hasNext()) {
					    String clid = (String)iter2.next();
					    key = clid+":"+proxL;
					    hashCntIncr(countProxSubL, key);
					}
				}
				proxR = checkProximalNeighbor(dclst.getName(),
					Direction.RIGHT, subProxSetR);
				if (proxR != null) {
					hashCntIncr(countProxClust,proxR);
/*
					nbrClustSel.add(proxR);
*/
					Iterator iter2 = subProxSetR.iterator();
					while (iter2.hasNext()) {
					    String clid = (String)iter2.next();
					    key = clid+":"+proxR;
					    hashCntIncr(countProxSubR, key);
					}
				}
//System.out.println(dclst.getName()+" "+proxL+" "+dclst.clustid+" "+proxR);
			}
		}

		/* secondary condition */

		iter = countProxClust.keySet().iterator();
		while (iter.hasNext()) {
			String proxClust = (String) iter.next();

			Integer ii = (Integer) countProxClust.get(proxClust);
			if ( NBR_CONS_NUM2 == 0  /* true when NBR_CONS_NUM2==0 */
				|| satisfy_NBR_CONS2( ii.intValue() ) ) {
				nbrClustSel.add(proxClust);
			}
		}

		iter = countProxSubL.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			String keystr[] = key.split(":");

			Integer ii = (Integer) countProxSubL.get(key);
//			if (ii.intValue() >= NBR_CONS_NUM2) {
			if ( satisfy_NBR_CONS2( ii.intValue() ) ) {
//System.out.println("add("+clustid1+"): "+key+" "+ii.intValue());
				nbrClustSel.add(keystr[0]);
			}
		}
		iter = countProxSubR.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			String keystr[] = key.split(":");

			Integer ii = (Integer) countProxSubR.get(key);
//			if (ii.intValue() >= NBR_CONS_NUM2) {
			if (satisfy_NBR_CONS2( ii.intValue() )) {
//System.out.println("add:("+clustid1+") "+key+" "+ii.intValue());
				nbrClustSel.add(keystr[0]);
			}
		}

		iter = nbrClustSel.iterator();
		while (iter.hasNext()) {
			String clustid2 = (String) iter.next();
			NbrClustData nbrdata = (NbrClustData)
						nbrClustHash.get(clustid2);
			makePairLink(clustid1, clustid2, nbrdata);
		}
	}
	void checkSubLink() {
		Set eSet= nbrGraph.edgeSet();
		Iterator iter, iter2;
		NbrClustDataForMST eData, eData2;
		for (iter = eSet.iterator(); iter.hasNext(); ) {
			Object[] nPair = (Object[]) iter.next();
			eData = (NbrClustDataForMST)
					nbrGraph.edge_data(nPair[0], nPair[1]);
//System.out.println("0>"+nPair[0]+" "+nPair[1]+" "+eData.maxcnt());
//			if (eData.maxcnt() < NBR_CONS_NUM) {
			if ( ! satisfy_NBR_CONS( eData.maxcnt(), NBR_CONS_NUM) ) {
				// If [$a,$b] is a weak link (<NBR_CONS_NUM),
				// examine whether there is a node $n s.t.
				// both [$a,$n] and [$b,$n] are conserved
				Set out1 = nbrGraph.out(nPair[0]);
				HashSet consLink = new HashSet();
				for (iter2 = out1.iterator(); iter2.hasNext(); ) {
					Object n = iter2.next();
					eData2 = (NbrClustDataForMST)
						nbrGraph.edge_data(nPair[0],n);
//System.out.println("chk0>"+nPair[0]+"<<"+nPair[1]+" "+n+" "+eData2.maxcnt()+" "+NBR_CONS_NUM);
//					if (eData2.maxcnt() >= NBR_CONS_NUM) {
					if ( satisfy_NBR_CONS( eData2.maxcnt(), NBR_CONS_NUM ) ) {
						// eData2 is a conserved link
//System.out.println("OK:"+eData2.nbrdata);
						consLink.add(n);
					}
				}
				Set out2 = nbrGraph.out(nPair[1]);
				boolean consFlag = false;
				for (iter2 = out2.iterator(); iter2.hasNext(); ) {
					Object n = iter2.next();
					eData2 = (NbrClustDataForMST)
						nbrGraph.edge_data(nPair[1],n);
					if (consLink.contains(n) &&
						// OK: this is a conserved link
//						eData2.maxcnt() >= NBR_CONS_NUM) {
						satisfy_NBR_CONS( eData2.maxcnt(), NBR_CONS_NUM ) ) {
//System.out.println("chk>"+nPair[0]+" "+nPair[1]+" "+n+" d="+eData2.nbrdata);
						consFlag = true;
						break;
					}
					
				}
				if (! consFlag) {
					// non-conserved sublink: delete
					nbrGraph.deleteEdge(nPair[0], nPair[1]);
				}
			}
		}
		
	}
	void hashCntIncr(HashMap hash, String string) {
		Integer ii = (Integer) hash.get(string);
		int i;
		if (ii == null) {
			hash.put(string, Integer.valueOf(1));
		} else {
			i = ii.intValue();
//System.out.println("i="+i);
			hash.put(string, Integer.valueOf(++i));
		}
	}
	void makePairLink(String clust1, String clust2, NbrClustData nbrdata) {
		if (nbrGraph.edge_data(clust1, clust2) != null) {
		} else {
			NbrClustDataForMST mst_node
				= new NbrClustDataForMST(nbrdata);
			nbrGraph.addEdge(clust1, clust2, mst_node);
		}
	}
	void savePairLink(String outfile) {
		PrintWriter bufout;
		try {
			bufout = new PrintWriter(
				new BufferedWriter(new FileWriter(outfile)));
		} catch (IOException e) {
			System.err.println("file write open failed");
			return;
		}
		savePairLink(bufout);
		bufout.close();
	}
	void savePairLink(PrintWriter out) {
		Set eSet = nbrGraph.edgeSet();
		Iterator iter = eSet.iterator();
		while (iter.hasNext()) {
			Object[] nPair = (Object[]) iter.next();
			NbrClustDataForMST eData = (NbrClustDataForMST)
					nbrGraph.edge_data(nPair[0], nPair[1]);
			out.println(nPair[0] +" "+ nPair[1] +" "+
				eData.nbrdata.weight +" "+
				eData.maxcnt()+" "+eData.maxdir());
		}
	}
	NbrClustData makeNbrData(String clustid1, String clustid2, int count, double dirweight[], int dircnt[]) {
		double maxweight = -1.0;
		int maxdir = -1;
		int maxcnt = 0;
		// find max
		for (int i = 0; i < 4; i++) {
//System.out.println("dirw: " + clustid2 + " " + i + " " + dirweight[i]+" "+dircnt[i]);
			if (dirweight[i] > maxweight ||
			   (dirweight[i]==maxweight && dircnt[i]>maxcnt) ) {
				maxweight = dirweight[i];
				maxcnt = dircnt[i];
				maxdir = i;
			}
		}
		// calc score
		double weight = 0;
		for (int i = 0; i < 4; i++) {
			if (i == maxdir) {
				weight += dirweight[i];
			} else {
				weight += dirweight[i] * ReargPen;;
			}
		}
//		if (maxcnt < NBR_CONS_NUM) {
		if (! satisfy_NBR_CONS(maxcnt, NBR_CONS_NUM) ) {
			weight *= SubLinkPen;
		}
//System.out.println("max: " + clustid2 + " " + maxdir+" " + maxcnt);
		return(new NbrClustData(clustid1, clustid2, count, maxdir, maxcnt, weight));
	}
	/** Find the most proximal cluster to the target cluster that satisfy the conservation condition.
		Clusters closer to the proximal cluster that satisfy only the second conservation condition are stored in subProximalClustSet **/
	String checkProximalNeighbor(String gene, int side,
				HashSet subProximalClustSet) {
		ArrayList clist = nbrTrip.getOrderedNbrList(gene, side);
		if (clist == null) {
			return null;
		}
		Iterator iter = clist.iterator();
		String proximalClust = null;
		subProximalClustSet.clear();
//System.out.println(gene+" "+side+" "+clist);

		while (iter.hasNext()) {
			String clustid = (String) iter.next();
			NbrClustData nclst =
				(NbrClustData) nbrClustHash.get(clustid);
			if (nclst == null) continue;
//System.out.println(Direction.reldir2dir(nclst.maxdir,1)+" "+side);
			if (Direction.reldir2dir(nclst.maxdir,1)!=side) {
				continue;
			}
//System.out.println("eval "+clustid+" "+nclst.maxcnt);
			if (proximalClust == null) {
//System.out.println("prox="+clustid+" "+nclst.maxcnt+" "+side+" "+gene);
//				if (nclst.maxcnt >= NBR_CONS_NUM) {
//				if ( satisfy_NBR_CONS(nclst.maxcnt, NBR_CONS_NUM) ) {
				if ( check_NBR_CONS(nclst) ) {
					proximalClust = clustid;
					break;
//				} else if (nclst.maxcnt >= NBR_CONS_NUM2) {
				} else if ( satisfy_NBR_CONS2(nclst.maxcnt) ) {
					subProximalClustSet.add(clustid);
				}
			}
		}
		return(proximalClust);
	}
}
/*
class NbrClustData extends EdgeData {
	String clustid;
	int count, maxdir, maxcnt;
	int dir, side1, side2;
	NbrClustData(String _clustid, int _count, int _maxdir, int _maxcnt, double _weight) {
		clustid = _clustid; count = _count; maxdir = _maxdir;
		maxcnt = _maxcnt; weight = _weight;
	}
	public String toString() {
		return(count+" "+maxdir+" "+maxcnt+" "+weight);
	}
}
*/

/*
class ConsNbrPairForNonCore extends ConsNbrPair {
	ConsNbrPairForNonCore(ClusterSet clSet, NbrTriplet ntrp, SpGroup spgrp) {
		super(clSet, ntrp, spgrp);
		setNbrConsRatio(0.65);
		setNbrConsRatio2(0.4);
		setMaxMinDist(4);
		unsetNbrConsNum2();
	}
	boolean checkClusterStatus(Cluster cluster) {
		if (cluster.status.equals("core")) {
			return false;
		} else {
			return true;
		}
	}
	boolean checkClusterStatus(String clustid) {
		Cluster cluster = clustSet.getCluster(clustid);
		return checkClusterStatus(cluster);
	}
}
*/

/** A wrapper class of NbrClustData for MST calculation, where
	a weight (distance) is re-set to 1 / weight.
*/
class NbrClustDataForMST extends WeightedData {
	NbrClustData nbrdata;
	NbrClustDataForMST(NbrClustData _nbrdata) {
		nbrdata = _nbrdata;
		weight = 1 / nbrdata.weight;
	}
	int maxcnt() {
		return nbrdata.maxcnt;
	}
	int maxdir() {
		return nbrdata.maxdir;
	}
	public int compareTo(Object o) {
		if (weight == ((NbrClustData) o).weight) {
			return 0;
		} else if (weight < ((NbrClustData) o).weight) {
			return -1;
		} else {
			return 1;
		}
	}
}
