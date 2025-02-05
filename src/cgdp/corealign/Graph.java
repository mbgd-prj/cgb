package cgdp.corealign;
import java.lang.*;
import java.util.*;
import java.io.*;

class Graph {
	HashMap dirEdges;
	HashMap revEdges;
	HashMap nodes;
	static double defaultWeight = 0.0;
	Graph() {
		dirEdges = new HashMap(500);
		revEdges = new HashMap(500);
		nodes = new HashMap(100);
	}
	void addEdge(Object n1, Object n2) {
		addEdge(n1, n2, defaultWeight);
	}
	void addEdge(Object n1, Object n2, double value) {
		addEdge(n1, n2, new WeightedData(value));
	}
	void addEdge(Object n1, Object n2, WeightedData value) {
		addEdge_sub(n1, n2, value, dirEdges);
		addEdge_sub(n2, n1, value, revEdges);
	}
	void addEdge_sub(Object n1, Object n2, WeightedData value, HashMap hash) {
		HashMap hash2;
		if (hash.containsKey(n1)) {
			hash2 = (HashMap) hash.get(n1);
		} else {
			hash2 = new HashMap();
			hash.put(n1, hash2);
		}
		hash2.put(n2, value);
		WeightedData d = (WeightedData) hash2.get(n2);
		Set ss = hash2.keySet();
		if (! nodeExist(n1)) {
			addNode(n1);
		}
		if (! nodeExist(n2)) {
			addNode(n2);
		}
	}
	void addNode(WeightedData n) {
		addNode(n, n);
	}
	void addNode(Object n) {
		addNode(n, defaultWeight);
	}
	void addNode(Object n, double weight) {
		addNode(n, new WeightedData(weight));
	}
	void addNode(Object n, WeightedData value) {
		nodes.put(n, value);
	}
	WeightedData edge_data(Object n1, Object n2) {
		HashMap hash = dirEdges;
		HashMap hash2;
		if (hash.containsKey(n1)) {
			hash2 = (HashMap) hash.get(n1);
			return((WeightedData) hash2.get(n2));
		} else {
			return(null);
		}
	}
	double get_edgeWeight(Object n1, Object n2) {
		return(edge_data(n1, n2).get_weight());
	}
	void set_edgeWeight(Object n1, Object n2, double wt) {
		edge_data(n1, n2).set_weight(wt);
	}
	WeightedData node_data(Object n1) {
		return( (WeightedData) nodes.get(n1) );
	}
	double get_nodeWeight(Object n1) {
		return(node_data(n1).get_weight());
	}
	void set_nodeWeight(Object n1, double wt) {
		node_data(n1).set_weight(wt);
	}
	boolean nodeExist(Object n1) {
		return(nodes.containsKey(n1));
	}
	boolean edgeExist(Object n1, Object n2) {
		HashMap hash2;
		if (dirEdges.containsKey(n1)) {
			hash2 = (HashMap) dirEdges.get(n1);
			return hash2.containsKey(n2);
		} else {
			return false;
		}
	}
	void deleteNode(Object n) {
		// remove reverse edges
		// n->n2
		Iterator outIter = out(n).iterator();
		while (outIter.hasNext()) {
			Object n2 = outIter.next();
			HashMap hash2 = (HashMap) revEdges.get(n2);
			if (hash2 != null) {
				hash2.remove(n);
			}
		}
		// n2->n
		Iterator inIter = in(n).iterator();
		while (inIter.hasNext()) {
			Object n2 = inIter.next();
			HashMap hash2 = (HashMap) dirEdges.get(n2);
			if (hash2 != null) {
				hash2.remove(n);
			}
		}
		dirEdges.remove(n);
		revEdges.remove(n);
		nodes.remove(n);
	}
	void deleteNodeShrink(Object n) {
		Set inSet = in(n);
		Set outSet = out(n);
		Iterator inIter = inSet.iterator();
		while (inIter.hasNext()) {
			Object prevNode = (Object) inIter.next();
			Iterator outIter = outSet.iterator();
			while (outIter.hasNext()) {
				Object nextNode =
					(Object) outIter.next();
				addEdge(prevNode, nextNode);
			}
		}
		deleteNode(n);
	}
	void deleteEdge(Object n1, Object n2) {
		deleteEdge_sub(dirEdges, n1, n2);
		deleteEdge_sub(revEdges, n1, n2);
	}
	void deleteEdge_sub(HashMap hash, Object n1, Object n2) {
		HashMap hash2 = (HashMap) hash.get(n1);
		if (hash2 != null) {
			if (hash2.containsKey(n2)) {
				hash2.remove(n2);
			}
		}
	}
	/** return a set of all edges in the graph as a set of arrays of length 2 */
	Set edgeSet() {
		Set nSet = nodeSet();
		HashSet eSet = new HashSet();
		for (Iterator iter = nSet.iterator(); iter.hasNext(); ) {
			Object n1 = (Object) iter.next();
			Set nSet2 = out(n1);
			for (Iterator iter2 = nSet2.iterator(); iter2.hasNext(); ) {
				Object n2 = (Object) iter2.next();
				Object npair[] = { n1, n2 };
				eSet.add(npair);
			}
		}
		return(eSet);
	}
	/** return a set of all nodes in the graph */
	Set nodeSet() {
		return(nodes.keySet());
	}
	Set in(Object node) {
		HashMap hash2 = (HashMap) revEdges.get(node);
		if (hash2 == null) {
			// return an empty set
			return(new HashSet());
		} else {
			return(hash2.keySet());
		}
	}
	Set out(Object node) {
		HashMap hash2 = (HashMap) dirEdges.get(node);
		if (hash2 == null) {
			// return an empty set
			return(new HashSet());
		} else {
			return(hash2.keySet());
		}
	}
	Set nbrNodes(Object node) {
		Set nbr = new HashSet();
		nbr.addAll(in(node));
		nbr.addAll(out(node));
		return(nbr);
	}
	Set findSource() {
		Set set = new HashSet();
		for (Object n: nodeSet()) {
			if (in(n).size() == 0) {
				set.add(n);
			}
		}
		return set;
	}
	Set findSink() {
		Set set = new HashSet();
		for (Object n: nodeSet()) {
			if (out(n).size() == 0) {
				set.add(n);
			}
		}
		return set;
	}
	Graph dup() {
		Graph newGraph = new Graph();
		Set eSet = edgeSet();
		Iterator iter = eSet.iterator();
		while (iter.hasNext()) {
			Object[] npair = (Object[]) iter.next();
			newGraph.addNode(npair[0], get_nodeWeight(npair[0]));
			newGraph.addNode(npair[1], get_nodeWeight(npair[1]));
			newGraph.addEdge(npair[0], npair[1],
					edge_data(npair[0], npair[1]));
		}
		return newGraph;
	}
	static Graph readData() throws IOException {
		try {
			return readData(new BufferedReader(new InputStreamReader(System.in)));
		} catch (IOException e) {
			throw e;
		}
	}
	static Graph readData(BufferedReader in) throws IOException {
		String line;
		Graph g = new Graph();
		try {
			while ((line = in.readLine()) != null) {
				String data[] = line.split(" ");
				g.addEdge(data[0], data[1], Double.parseDouble(data[2]));
			}
		} catch (IOException e) {
			throw e;
		}
		in.close();
		return g;
	}
	static Graph readData(String file) throws IOException {
		BufferedReader in;
		Graph g = null;
		in = new BufferedReader(new FileReader(file)); 
		try {
			g = readData(in);
		} catch (IOException e) {
			throw e;
		}
		return(g);
	}
	void print() {
		print(System.out);
	}
	void print(PrintStream out) {	
		Iterator iter = edgeSet().iterator();
		while (iter.hasNext()){
			Object[] eNode = (Object[]) iter.next();
			out.println(eNode[0]+" "+eNode[1]+" "+
				get_edgeWeight(eNode[0],eNode[1]));
		}
		if (out != System.out) {
			out.close();
		}
	}
	void print(String file) {
		PrintStream out = null;
		try {
			out = new PrintStream(new BufferedOutputStream(
				new FileOutputStream(file)));
		} catch (IOException e) {
			return;
		}
		print(out);
	}
	public static void main(String[] args) {
		Graph g = new Graph();
		g.addEdge("1", "2");
		g.addEdge("3", "4");
		g.addEdge("5", "4");
		g.addEdge("6", "7");
		g.addEdge("1", "7");
		System.out.println(g.out("4"));
		System.out.println(g.in("4"));
	}
}
//////////////////////////////////////////////////
class UndirectedGraph extends Graph {
	// revEdge is not used 
	UndirectedGraph() {
		dirEdges = new HashMap(500);
		nodes = new HashMap(100);
	}
	void addEdge(Object n1, Object n2) {
		addEdge(n1, n2, new WeightedData(defaultWeight));
	}
	void addEdge(Object n1, Object n2, double value) {
		addEdge(n1, n2, new WeightedData(value));
	}
	void addEdge(Object n1, Object n2, WeightedData value) {
		addEdge_sub(n1, n2, value, dirEdges);
		addEdge_sub(n2, n1, value, dirEdges);
	}
	Set edgeSet() {
		Set nSet = nodeSet();
		HashSet eSet = new HashSet();
		HashMap foundHash = new HashMap();	// for reverse check
		for (Iterator iter = nSet.iterator(); iter.hasNext(); ) {
			Object n1 = (Object) iter.next();
			Set nSet2 = out(n1);
			HashSet foundHash2 = (HashSet) foundHash.get(n1);
			if (foundHash2 == null) {
				foundHash2 = new HashSet();
				foundHash.put(n1, foundHash2);
			}
			for (Iterator iter2 = nSet2.iterator(); iter2.hasNext(); ) {
				Object n2 = (Object) iter2.next();
				// reverse check: to avoid duplicated output
				if (foundHash.get(n2) == null ||
				   ! ((HashSet)foundHash.get(n2)).contains(n1)) {
					Object npair[] = { n1, n2 };
					eSet.add(npair);
				}
				foundHash2.add(n2);
			}
		}
		return(eSet);
	}
	void deleteEdge(Object n1, Object n2) {
		deleteEdge_sub(dirEdges, n1, n2);
		deleteEdge_sub(dirEdges, n2, n1);
	}
	Set in(Object node) {
		return out(node);
	}
	Set nbrNodes(Object node) {
		return(out(node));
	}
	public static void main(String[] args) {
		UndirectedGraph g = new UndirectedGraph();
		g.addEdge("1", "2", 1.0);
		g.addEdge("3", "4", 1.2);
		g.addEdge("5", "4", 1.1);
		g.addEdge("5", "3", 1.8);
		g.addEdge("6", "7", 2.1);
		g.addEdge("6", "2", 1.6);
		g.addEdge("1", "7", 2);
		MSTBuild mstCalc = new MSTBuild(g);
		UndirectedGraph mst = mstCalc.kruskal();
		mst.print();
		System.out.println("dist=" + mstCalc.getDist());
/*
		System.out.println(g.out("4"));
		System.out.println(g.in("4"));
		SlinkClust slink = new SlinkClust(g);
		slink.clustering();
		slink.resultout();
*/
	}
}
//////////////////////////////////////////////////
class WeightedData implements Comparable {
	double weight;
	WeightedData() {
	}
	WeightedData(double w) {
		weight = w;
	}
	public double get_weight() {
		return weight;
	}
	public void set_weight(double w) {
		weight = w;
	}
	public int compareTo(Object o) {
		//return in ascending order
		if (weight == ((WeightedData) o).weight) {
			return 0;
		} else if (weight < ((WeightedData) o).weight) {
			return 1;
		} else {
			return -1;
		}
	}
}
//////////////////////////////////////////////////
class FeedbackSet {
	Graph graph;
	boolean edgeFlag;
	HashSet fbkSet;

	FeedbackSet(Graph g) {
		graph = g.dup();	// copy graph to save the original one
		fbkSet = new HashSet();
	}
	void set_edgeFlag(boolean flag) {
		edgeFlag = flag;
	}
	Set makeFset() {
		contraction();
		return fbkSet;
	}
	void contraction() {
		int nnum = 0;
//int inum = 0;
//System.out.println("init="+graph.nodeSet().size());
		do {

			while (contractionCheck()) {
				// loop
			}
			Set nSet = graph.nodeSet();
			nnum = nSet.size();
//System.out.println("nnum="+nnum);
			if (nnum > 0) {
				Iterator iter = nSet.iterator();
				Object minNode = null;
				double minWeight = Double.MAX_VALUE;
				while (iter.hasNext()) {
//					WeightedData n = (WeightedData) iter.next();
					Object o = iter.next();
					WeightedData n = graph.node_data(o);
					if (minWeight > n.get_weight()) {
						minWeight = n.get_weight();
						minNode = o;
					}
				}
				// delete the node of minimum weight
				if (minNode != null) {
					fbkSet.add(minNode);
					graph.deleteNode(minNode);
					nnum--;
				}
			}
		} while (nnum > 0);
	}
	boolean contractionCheck() {
		Set nSet  = graph.nodeSet();
		Object[] nodes = (Object[])
				nSet.toArray(new Object[nSet.size()]);
		// descending order
		Arrays.sort(nodes, new Comparator() {
			public int compare(Object o1, Object o2) {
				double w1 = graph.get_nodeWeight(o1);
				double w2 = graph.get_nodeWeight(o2);
				return(Double.compare(w2, w1));
			}
/*
			public int compare(Object o1, Object o2) {
				return(o2.toString().compareTo(o1.toString()));
			}
*/
		});
		int modcnt = 0;
		for (int i = 0; i < nodes.length; i++) {
			Object node = nodes[i];
			Set inSet = graph.in(node);
			Set outSet = graph.out(node);
			if (graph.edgeExist(node,node)) {
				REM("loop: "+node);
				graph.deleteNode(node);
				fbkSet.add(node);
				modcnt++;
			} else if (inSet.size() == 0 || outSet.size() == 0) {
				REM("in0out0: "+node);
				graph.deleteNode(node);
				modcnt++;
			} else if (inSet.size() == 1) {
				REM("in1: "+node);
				graph.deleteNodeShrink(node);
				modcnt++;
			} else if (outSet.size() == 1) {
				REM("out1: "+node);
				graph.deleteNodeShrink(node);
				modcnt++;
			}
			if (modcnt > 0) {
				break;
			}
		}
		return(modcnt > 0);
	}
	void REM(String msg) {
//		System.out.println(msg);
	}
	Set getFset() {
		return fbkSet;
	}
	void removeFset(Graph g) {
		Iterator iter = fbkSet.iterator();
		while (iter.hasNext()) {
			Object n = (Object) iter.next();
//System.out.println(">del="+n+" "+((WeightedData)n).get_weight());
			g.deleteNode(n);
		}
	}
	public static void main(String[] args) {
		String file = args[0];
		Graph g = null;
		try {
			g = Graph.readData(file);
		} catch (IOException e) {
			System.err.println("Can't read file:" + file);
			System.exit(1);
		}
		FeedbackSet fsetBuild = new FeedbackSet(g);
		Set fset = fsetBuild.makeFset();
		System.out.println(fset);
		fsetBuild.removeFset(g);
		g.print();
		DFSearch dfs = new DFSearch(g);
		dfs.search();
	}
/*
	public static void main(String[] args) {
		Graph g = new Graph();
		g.addNode("1", 1.0);
		g.addNode("2", 2.0);
		g.addNode("3", 3.0);
		g.addNode("4", 4.0);
		g.addEdge("1","2");
		g.addEdge("2","3");
		g.addEdge("3","1");
		g.addEdge("4","3");
		g.addEdge("4","5");
		g.addEdge("2","4");
		g.addEdge("5","2");
		FeedbackSet fsetBuild = new FeedbackSet(g);
		Set fset = fsetBuild.makeFset();
		System.out.println(fset);
	}
*/
}
//////////////////////////////////////////////////
class DFSearch {
	Graph graph;
	HashMap visitIn, visitOut;
	int countIn, countOut;
	int searchDir;
	DFSearch(Graph g) {
		graph = g;
		visitIn = new HashMap();
		visitOut = new HashMap();
	}
	void search() {
		search(1);
	}
	void search(int dir) {
		search(dir, graph.nodeSet());
	}
	void search(int dir, Collection nodeSet) {
		searchDir = dir;
		Iterator iter = nodeSet.iterator(); 
		while (iter.hasNext()) {
			Object n = iter.next();
			search_sub(n);
		}
	}
	void search_sub(Object n1) {
		if (visitIn.containsKey(n1)) {
			if (! visitOut.containsKey(n1)) {
				System.err.println("LOOP:dfs:"+n1);
			}
			return;
		}
		visitIn.put(n1, Integer.valueOf(++countIn));
		Set nextNode = (searchDir > 0) ? graph.out(n1) : graph.in(n1);
		Iterator iter = nextNode.iterator();
		while (iter.hasNext()) {
			Object n2 = iter.next();
			search_sub(n2);
		}
		visitOut.put(n1, Integer.valueOf(++countOut));
	}
	Set inNodeSet() {
		return visitIn.keySet();
	}
	ArrayList inNodeList() {
		return(hash2list(visitIn));
	}
	Set outNodeSet() {
		return visitOut.keySet();
	}
	ArrayList outNodeList() {
		return(hash2list(visitOut));
	}
	ArrayList hash2list(final HashMap hash) {
		ArrayList alist = new ArrayList(hash.keySet());
		Collections.sort(alist, new Comparator() {
			public int compare(Object o1, Object o2) {
				int order1 = ((Integer)hash.get(o1)).intValue();
				int order2 = ((Integer)hash.get(o2)).intValue();
				return (order1 - order2);
			}
		});
		return(alist);
	}
}
//////////////////////////////////////////////////
class MSTBuild {
	Graph graph;
	double dist;
	UndirectedGraph tree;
	MSTBuild(Graph g) {
		graph = g;
 		tree = new UndirectedGraph();
	}
	UndirectedGraph kruskal() {
		DisjointSet cluster = new DisjointSet();
		Iterator iter = graph.nodeSet().iterator();
		int Nnum = 0;
		while (iter.hasNext()) {
			Object n = iter.next();
			cluster.add(n);
			Nnum++;
		}
		iter = graph.edgeSet().iterator();
		class WData extends WeightedData {
			Object node1, node2;
			double weight;
			WData(Object n1, Object n2, double w) {
				node1 = n1; node2 = n2; weight = w;
			}
			public String toString() {
				return("{"+node1+","+node2+","+weight+"}");
			}
		}
		ArrayList alist = new ArrayList();
		while (iter.hasNext()) {
			Object[] node = (Object[]) iter.next();
			WeightedData eData = (WeightedData) graph.edge_data(node[0],node[1]);
			double weight = eData.get_weight();
			alist.add( new WData(node[0], node[1], weight) );
		}
		Collections.sort(alist, new Comparator() {
			public int compare(Object e1, Object e2) {
				double w1 = ((WData)e1).weight;
				double w2 = ((WData)e2).weight;
				if (w1 == w2) {
					return 0;
				} else if (w1 < w2) {
					return -1;
				} else {
					return 1;
				}
			}
		});
//System.out.println(alist);
		iter = alist.iterator();
		while (iter.hasNext()) {
			WData e = (WData) iter.next();
			if (cluster.findSet(e.node1)
					== cluster.findSet(e.node2)) {
				continue;
			}
			tree.addEdge(e.node1, e.node2, e.weight);
			cluster.union(e.node1, e.node2);
//System.out.println("MST "+e.node1+" "+e.node2+" "+e.weight);
			dist += e.weight;
		}
		return(tree);
	}
	double getDist() {
		return dist;
	}
}
//////////////////////////////////////////////////
class DisjointSet {
	HashMap elemData;

	DisjointSet() {
		elemData = new HashMap();
	}
	void add(Object elem) {
		ElemData eData = new ElemData();
		eData.parent = elem;
		elemData.put(elem, eData);
	}
	void union(Object elem1, Object elem2) {
		Object root1 = findSet(elem1);
		Object root2 = findSet(elem2);
		link(root1, root2);
	}
	void link(Object elem1, Object elem2) {
		ElemData eData1 = (ElemData) elemData.get(elem1);
		ElemData eData2 = (ElemData) elemData.get(elem2);
		if (eData1.rank >= eData2.rank) {
			eData2.parent = eData1.parent;
			if (eData1.rank == eData2.rank) {
				eData2.rank++;
			}
		} else {
			eData1.parent = eData2.parent;
		}
	}
	Object findSet(Object elem) {
		ElemData eData = (ElemData) elemData.get(elem);
		if (eData.parent == elem) {
			return elem;
		}
		Object root = findSet(eData.parent);
		eData.parent = root;
		return root;
	}
}
class ElemData {
	int rank;
	Object parent;
}
//////////////////////////////////////////////////
class SlinkClust {
	Graph graph;
	int clustNum;
	/** Hash storing clustid for each node */
	HashMap<Object, Integer> clustRes;
	/** Array of clusters */
	ArrayList[] clusterList;
	SlinkClust(Graph g) {
		graph = g;
		clustRes = new HashMap<Object, Integer>();
	}
	void clustering() {
		Set set = graph.nodeSet();
		Iterator iter = set.iterator();
		int clustid = 0;
		while (iter.hasNext()) {
			Object node = iter.next();
			int count = traverse(node, clustid);
//			if (traverse(node, clustid) > 0) {
			if (count > 0) {
				clustid++;
			}
		}
		clustNum = clustid;
		makeCluster();
		addSingleton();
	}
	void makeCluster() {
		Iterator iter = clustRes.keySet().iterator();
		clusterList = new ArrayList[clustNum];
		while (iter.hasNext()) {
			Object node = iter.next();
			int clustid = ((Integer) clustRes.get(node)).intValue();
			if (clusterList[clustid] == null) {
				clusterList[clustid] = new ArrayList();
			}
			clusterList[clustid].add(node);
		}
//		System.out.println("OK:"+clustNum);
		// sort by cluster size
		Arrays.sort(clusterList, new Comparator<ArrayList>() {
			public int compare(ArrayList lis1, ArrayList lis2) {
				return Integer.compare(lis2.size(), lis1.size());
			}
		});
		clustRes.clear();
		for (int clustid = 0; clustid < clustNum; clustid++) {
			for (Object node: clusterList[clustid]) {
				clustRes.put(node, clustid);
			}
		}
/*
		for (int i = 0; i < clustNum; i++) {
			System.out.println((i)+" "+clusterList[i]);
		}
*/
	}
	void addSingleton() {
		int clustid = clustNum;
		for (Object node: graph.nodeSet()) {
			if (! clustRes.containsKey(node)) {
				clustRes.put(node, clustid);
				clustid++;
			}
		}
	}
	void resultout() {
		Iterator iter = clustRes.keySet().iterator();
		while (iter.hasNext()) {
			Object node = iter.next();
			Object clustid = clustRes.get(node);
//			System.out.println("clustres>"+node+" "+clustid);
		}
	}
	int getClustID(Object node) {
		return clustRes.get(node);
	}
	int clustNum() {
		return clustNum;
	}
	int traverse(Object node, int clustid) {
		int count = 0;
		if (clustRes.containsKey(node)) {
			return(0);
		}
		clustRes.put(node, Integer.valueOf(clustid));
		count++;
		Set nset = graph.nbrNodes(node);
		Iterator iter = nset.iterator();
		while(iter.hasNext()) {
			Object nextNode = iter.next();
			count += traverse(nextNode, clustid);
		}
		return(count);
	}
}
