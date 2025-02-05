package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;

/** converted directed graph */
class LinkDir {
	ClusterSet clstSet;
	ClusterDir cldir;
	LinkDir(ClusterSet _clstSet, UndirectedGraph _graph, ClusterDir gdir) {
		clstSet = _clstSet;
		nbrGraph = _graph;
		cldir = gdir;
	}
//	HashMap GdirHash;
	Graph newGraph;
	UndirectedGraph nbrGraph;
	UndirectedGraph mst;

	public void checkLinkDir() {
		newGraph = new Graph();
		MSTBuild mstBuild = new MSTBuild(nbrGraph);
		mst = mstBuild.kruskal();
//		mst.print();
		Set nSet = mst.nodeSet();
		Object[] a = nSet.toArray();
		String[] clustIDs = (String[])
			nSet.toArray(new String[nSet.size()]);
		Arrays.sort(clustIDs, new Comparator() {
			// descending order of spnum
			public int compare(Object e1, Object e2) {
				return (clstSet.getCluster((String)e2).spnum() -
					clstSet.getCluster((String)e1).spnum());
				
			}
		} );
//		GdirHash = new HashMap();
		for (int i = 0; i < clustIDs.length; i++) {
			if (cldir.getGdir(clustIDs[i]) == 0) {
				checkLink_sub(clustIDs[i], 1);
			}
		}
		int dir, nextdir, nextdir2;
		for (int i = 0; i < clustIDs.length; i++) {
			String n1 = clustIDs[i];
			if (cldir.getGdir(n1) > 0) {
				nextdir = Direction.RIGHT;
			} else {
				nextdir = Direction.LEFT;
			}
			Iterator iter = nbrGraph.out(n1).iterator();
			while (iter.hasNext()) {
				String n2 = (String) iter.next();
				dir = (cldir.getGdir(n1)==cldir.getGdir(n2)) ? 1: -1;

				NbrClustDataForMST mst_data =
					(NbrClustDataForMST)
						nbrGraph.edge_data(n1, n2);
				NbrClustData nbrdata = mst_data.nbrdata;

				if (nbrdata.clustid1.equals(n1)) {
					nextdir2 =Direction.reldir2dir(
							nbrdata.maxdir,1);
				} else if (nbrdata.clustid2.equals(n1)) {
					nextdir2 =Direction.reldir2dir(
							nbrdata.maxdir,2);
				} else {
					System.out.println("ERROR: nbrdata"+
						nbrdata.clustid1+" "+
						nbrdata.clustid2+" "+
						n1+" "+n2);
					continue;
				}
				if (nextdir != nextdir2) {
					// incorrect direction
					continue;
				}

				if (nbrdata.dir != dir) {
					// eliminate inconsistent edge
					continue;
				}
				// add an edge {n1->n2} to the new graph
/*
				if (nbrdata.clustid1.equals(n2) && 
						nbrdata.clustid2.equals(n1)) {
					nbrdata.clustid1 = n1;
					nbrdata.clustid2 = n2;
System.out.println("swap="+n1+" "+n2);
				} else if (nbrdata.clustid1.equals(n1) && 
						nbrdata.clustid2.equals(n2)) {
				} else {
					System.out.println("ERROR???"+
						nbrdata.clustid1+" "+
						nbrdata.clustid2+" "+
						n1+" "+n2);
				}
*/
				newGraph.addEdge(n1,n2,nbrdata);
			}
		}
	}
	void checkLink_sub(String n1, int gdir) {
		int gdir_n1 = cldir.getGdir(n1);
		if (gdir_n1 != 0) {
			if (gdir_n1 != gdir) {
				System.err.println("Error: direction "+n1+": "+gdir+":"+gdir_n1);
			}
			return;
		}
//System.out.println("set cluster " + n1 + " =" + gdir);
		cldir.setGdir(n1, gdir);

		Set out_nSet = mst.out(n1);
		Iterator iter = out_nSet.iterator();
		int new_gdir;
		while (iter.hasNext()) {
			// next node
			String n2 = (String) iter.next();
			if (n1 == n2) {
				continue;
			}

			// next direction
			NbrClustDataForMST mst_data =
				(NbrClustDataForMST) nbrGraph.edge_data(n1, n2);
			NbrClustData nbrdata = mst_data.nbrdata;

//System.out.println("nbr_dir>>"+nbrdata.dir);
			if (nbrdata.dir < 0) {
				new_gdir = gdir * -1;
			} else {
				new_gdir = gdir;
			}
//System.out.println("dir: "+n1+" "+n2+" "+gdir+" "+new_gdir);
			checkLink_sub(n2, new_gdir);
		}
	}
	void saveLinks(String outfile) {
		PrintWriter bufout;
		try {
			bufout = new PrintWriter(
				new BufferedWriter(new FileWriter(outfile)));
		} catch (IOException e) {
			System.err.println("file write open failed");
			return;
		}
		saveLinks(bufout);
		bufout.close();
	}
	void saveLinks(PrintWriter linkOut) {
		Iterator iter, iter2;
		String n1, n2;
		NbrClustData nbrdata;

		iter = newGraph.nodeSet().iterator();
		while (iter.hasNext()) {
			n1 = (String) iter.next();
			iter2 = newGraph.out(n1).iterator();
			while (iter2.hasNext()) {
				n2 = (String) iter2.next();
				nbrdata = (NbrClustData)
					newGraph.edge_data(n1,n2);
				linkOut.println(n1+" "+n2+" "+
					nbrdata.weight+" "+nbrdata.dir+" "+
					nbrdata.count+" "+nbrdata.maxcnt);
			}
		}
	}
/*
	int getGdir(String clustid) {
		Integer dir = (Integer) GdirHash.get(clustid);
		if (dir == null) {
			return 0;
		}
		return(dir.intValue());
	}
	void setGdir(String clustid, int dir) {
		GdirHash.put(clustid, new Integer(dir));
	}
*/
}
