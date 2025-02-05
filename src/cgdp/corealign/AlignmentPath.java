package cgdp.corealign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** construct alignment paths */
class AlignmentPathBuilder {
	Graph graph;
	HashSet visit;
	HashMap posClustID;
	private Graph pathTree;
	private AlignmentPath corePath;
	int MIN_CLUSTCNT;

	AlignmentPathBuilder(ClusterSet clustSet, Graph g) {
		graph = g;
		corePath = new AlignmentPath();
		posClustID = new HashMap();
		MIN_CLUSTCNT = 10;
	}
	void set_MinClustCnt(int cnt) {
		MIN_CLUSTCNT = cnt;
	}
/*
	void findPath() {
		makeCorePath();
//		duplicatedClusterCheck();
//		restoreTriplet();
	}
*/
	AlignmentPath findMaxPath() {
		pathTree = new Graph(); 	//reset pathTree
		// do longest path search
		longestPathSearch();
		Set nSet = pathTree.nodeSet();
		int nextID = 0;
		HashSet tmpVis = new HashSet();
		int nodenum = nSet.size();
		double end_thre = 0.1;

		while (nodenum > 0) {
			double maxscore, wt;
			Object n, maxn = null;
			Iterator iter = nSet.iterator();
			maxscore = 0.0;
			int cnt = 0;
			// find maxscore node
			while (iter.hasNext()) {
				n = iter.next();
				if (posClustID.containsKey(n) || tmpVis.contains(n)) {
					continue;
				}
				wt = pathTree.node_data(n).get_weight();
				if (maxscore < wt) {
					maxn = n;
					maxscore = wt;
				}
				cnt++;
			}
			n = maxn;
/*
System.out.println("maxscore="+maxscore+",n="+n+",cnt="+cnt);
*/

			if (maxscore <= MIN_CLUSTCNT * end_thre) break;

			// construct max path
			tmpVis.add(n);
			int clustSiz = 0;
			ArrayList path0 = new ArrayList();
			while (n != null) {
				if (posClustID.containsKey(n)) {
					break;
				}
//System.out.println("n="+n);
				path0.add(n);
				clustSiz++;
				Set out_nSet = pathTree.out(n);
				int outn = out_nSet.size();
				// traverse the tree
				// note that there is at most one out edge
				if (outn == 0) {
					n = null;
				} else if (outn == 1) {
					n = out_nSet.iterator().next();
					if (n != null) {
						if (tmpVis.contains(n)) {
							break;
						}
						tmpVis.add(n);
					}
				}
			}
//System.out.println("clustSiz="+clustSiz);
			if (clustSiz >= MIN_CLUSTCNT - 1) {
				++nextID;
				for (int i = 0; i < path0.size(); i++) {
					Object n0 = path0.get(i);
					posClustID.put(n0, Integer.valueOf(nextID));
				}
				corePath.newPath(path0);
			}
			nodenum -= path0.size();
			updatePath(path0);
			longestPathSearch();
		}
		return corePath;
	}
	void longestPathSearch() {
		Iterator<Object> iter = graph.nodeSet().iterator();
		visit = new HashSet<Object>();	// reset visit flag;
		while (iter.hasNext()) {
			Object node = iter.next();
			double weight = dpSearch(node);
		}
	}
	double dpSearch(Object node) {
		if (visit.contains(node)) {
			if (! pathTree.nodeExist(node)) {
				System.err.println("LOOP: " + node);
				return 0;
			}
			return (pathTree.node_data(node).get_weight());
		} else if (pathTree.nodeExist(node)) {
			return (pathTree.node_data(node).get_weight());
		}
		visit.add(node);

		Object node2;
		Object maxPath = null;
		Iterator iter = graph.out(node).iterator();
		double maxWeight = 0.0;
		while (iter.hasNext()) {
			node2 = iter.next();

//			double wt = node2.get_weight();
//			double weight = dpSearch(node2) + wt;
			double weight = dpSearch(node2);
			weight += graph.get_edgeWeight(node, node2);

			if (weight > maxWeight) {
				maxWeight = weight;
				maxPath = node2;
			}
		}
		if (maxPath != null) {
//System.out.println(maxPath+" "+maxWeight+" "+node);
			pathTree.addEdge(node, maxPath);
		}
		/* node has a weight -- TripletGraph */
		if (node instanceof WeightedData) {
			maxWeight += ((WeightedData) node).get_weight();
		}
		pathTree.addNode(node, maxWeight);
/*
System.out.println("ww="+node+" "+pathTree.node_data(node).get_weight());
*/
		return maxWeight;
	}
	void updatePath(ArrayList selectedPath) {
		HashSet selPathFlag = new HashSet();
		// weights of the nodes on the selected path are reset
		for (int i = 0; i < selectedPath.size(); i++) {
			Object n = selectedPath.get(i);
			pathTree.set_nodeWeight(n, Integer.MIN_VALUE);
			selPathFlag.add(n);
		}
		DFSearch dfs = new DFSearch(pathTree);
		dfs.search(-1, selectedPath);

		// list of the descendant nodes
		ArrayList outNList = dfs.outNodeList();
/*
		Set outNSet = dfs.outNodeSet();
		Object[] outNList = (Object[]) outNSet.toArray();
System.out.println("outNSet="+outNSet);
		Arrays.sort(outNList, new Comparator() {
			public int compare(Object o1, Object o2){
				return ((Comparable)o2).compareTo((Comparable)o1);
			}
		});
*/
		// the descendant nodes are deleted
		for (int i = 0; i < outNList.size(); i++) {
			if (! selPathFlag.contains(outNList.get(i))) {
				pathTree.deleteNode(outNList.get(i));
			} else {
			}
		}
	}
	void duplicatedClusterCheck() {
	}
}
/** convert from a triplet graph to its original graph*/
class AlignmentPathConverter {
	AlignmentPath rawPath;
	AlignmentPath newPath;
	DupCidCheck dupCheck;
	ClusterDir gDir;
	AlignmentPathConverter(AlignmentPath rawp, DupCidCheck dck, ClusterDir gdir) {
		rawPath = rawp;
		newPath = new AlignmentPath();
		dupCheck = dck;
		gDir = gdir;
	}
	AlignmentPath restoreTriplet() {
		Iterator iter = rawPath.iterator();
		while (iter.hasNext()) {
			ArrayList alip = ((ClustAliPath) iter.next()).toArrayList();
			ArrayList newp = new ArrayList();
			String prevName;
			for (int i = 0; i < alip.size(); i++) {
				String nid = alip.get(i).toString();
				if (TripletGraph.checkName(nid)) {
					String[] splitName =
						TripletGraph.splitName(nid);
					String cid1 = splitName[0];
					String cid2 = splitName[1];
					if (i == 0) {
						String newcid1 = dupCheck.checkDupCluster(cid1, 1);
						if (! newcid1.equals(cid1)) {
							gDir.copyGdir( cid1, newcid1 );
						}
						newp.add(newcid1);
					}
					String newcid2 = dupCheck.checkDupCluster(cid2,1);
					if (! newcid2.equals(cid2)) {
						gDir.copyGdir( cid2, newcid2 );
					}
					newp.add(newcid2);
				} else {
					newp.add(nid);
				}
			}
			newPath.newPath(newp);
		}
		iter = newPath.iterator();
		while (iter.hasNext()) {
			ClustAliPath alip = (ClustAliPath) iter.next();
			ArrayList alist = alip.toArrayList();
			for (int i =0; i < alist.size(); i++) {
				String cid = (String) alist.get(i);
				String newcid = dupCheck.checkDupCluster(cid,2);
				if (! newcid.equals(cid)) {
					alip.setData(i, newcid);
					gDir.copyGdir( cid, newcid );
				}
			}
		}

		return newPath;
	}
}
/** A set of alignment paths */
class AlignmentPath {
	ArrayList<ClustAliPath> aliPaths;
	AlignmentPath() {
		aliPaths = new ArrayList<ClustAliPath>();
	}
	void newPath() {
		aliPaths.add(new ClustAliPath());
	}
	void newPath(Collection c) {
		newPath();
		addData(c);
	}
	ClustAliPath getData(int idx) {
		return (ClustAliPath) aliPaths.get(idx);
	}
	void addData(Object o) {
		int pathNum = aliPaths.size();
		((ClustAliPath)aliPaths.get(pathNum-1)).addData(o);
	}
	void addData(Collection c) {
		int pathNum = aliPaths.size();
		((ClustAliPath)aliPaths.get(pathNum-1)).addData(c);
	}
	int size() {
		return aliPaths.size();
	}
	Iterator<ClustAliPath> iterator() {
		return aliPaths.iterator();
	}
	void sortPath(String refsp, ClusterSet clustSet, ClusterDir gDir) {
		for (int i = 0; i < aliPaths.size(); i++) {
			ClustAliPath alip = (ClustAliPath) aliPaths.get(i);
			alip.calcPosDir(refsp, clustSet, gDir);
		}
		Collections.sort(aliPaths, new Comparator<ClustAliPath>() {
			public int compare(ClustAliPath o1, ClustAliPath o2) {
				return Double.compare(o1.posMean, o2.posMean);
			}
		});
	}
	void print() {
		for (int i = 0; i < aliPaths.size(); i++) {
			ClustAliPath aliPath = (ClustAliPath) aliPaths.get(i);
			ArrayList alip = aliPath.toArrayList();
			for (int j = 0; j < alip.size(); j++) {
				Object a = alip.get(j);
				System.out.println(a);
			}
			System.out.println("***" + " "+aliPath.posMean);
		}
	}
}
/** An alignment path of cluster */
class ClustAliPath {
	ArrayList<Object> aliPath;
	double posMean;
	ClustAliPath() {
		aliPath = new ArrayList<Object>();
	}
	ClustAliPath(ArrayList<Object> alist) {
		aliPath = alist;
	}
	void addData(Object o) {
		aliPath.add(o);
	}
	void addData(Collection c) {
		aliPath.addAll(c);
	}
	int size() {
		return aliPath.size();
	}
	String getData(int idx) {
		return (String) aliPath.get(idx);
	}
	void setData(int idx, String data) {
		aliPath.set(idx, data);
	}
	ArrayList<Object> toArrayList() {
		return aliPath;
	}
	void calcPosDir(String refsp, ClusterSet clustSet, ClusterDir gDir) {
		float posSum = 0;
		int num = 0;
		float prevPos = (float) Integer.MIN_VALUE;
		int diffSum = 0;
		int DiffCut = 10000;
		for (int i = 0; i < aliPath.size(); i++) {
			DomCluster dom = clustSet.getClusterData1_DupCheck(
				(String) aliPath.get(i), refsp);
			if (dom != null) {
				float pos = dom.dom.gene.pos;
				posSum += pos;
				num++;
				if (prevPos > Integer.MIN_VALUE &&
					Math.abs(pos - prevPos) < DiffCut) {
					if (prevPos < pos) {
						diffSum++;
					} else {
						diffSum--;
					}
				}
				prevPos = pos;
			}
		}
		if (num == 0) {
			posMean = 0;
		} else {
			posMean = (double) posSum / num;
		}
		if (diffSum < 0) {
			// reverse direction
//System.out.println("orig:"+ aliPath);
			Collections.reverse(aliPath);
			for (int i = 0; i < aliPath.size(); i++) {
				String cid = (String) aliPath.get(i);
				gDir.inverseGdir(cid);
			}
//System.out.println("rev: "+ aliPath);
		}
	}
}
