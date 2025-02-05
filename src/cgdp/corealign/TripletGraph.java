package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;

class TripletGraph {
	Graph newGraph;
	Graph origGraph;
	HashMap foundHash;
	NbrTriplet nbrTriplet;
	static final String Delim = ":";
	TripletGraph(Graph oGraph, NbrTriplet nbrTrip) {
		newGraph = new Graph();
		origGraph = oGraph;
		nbrTriplet = nbrTrip;
		foundHash = new HashMap();
	}
	Graph makeTripletGraph() {
		Iterator iter = origGraph.nodeSet().iterator();
		while (iter.hasNext()) {
			String n = (String) iter.next();
			makeTripletGraph_sub(n, null);
		}
/*
System.out.println("###");
newGraph.print();
*/
		return(newGraph);
	}
	void makeTripletGraph_sub(String n1, String n0) {
		String key = n1 + Delim + n0;
		if (n0 != null) {
			Integer ii = (Integer) foundHash.get(key);
			if (ii != null) {
				if (ii.intValue() == 1) {
					//System.out.println("Loop");
				}
				return;
			}
			foundHash.put(key, Integer.valueOf(1));
		}
		Iterator iter = origGraph.out(n1).iterator();
		while (iter.hasNext()) {
			String n2 = (String) iter.next();
			if (nbrTriplet.checkTriplet(n0,n1,n2)) {
				if (n0 != null) {
					WeightedData edata1 = 
						origGraph.edge_data(n0, n1);
					WeightedData edata2 = 
						origGraph.edge_data(n1, n2);
					checkDir((NbrClustData)edata1, n0, n1);
					checkDir((NbrClustData)edata2, n1, n2);
/*
if ( ! ((NbrClustData)edata1).clustid1.equals(n0) ||
	 ! ((NbrClustData)edata1).clustid2.equals(n1) ) {
System.out.println("EE1="+((NbrClustData)edata1).clustid1+" "+
	((NbrClustData)edata1).clustid2+"; "+n0+" "+n1);
}
*/

					/* A node of the new graph
					   is an edge of the orig graph */
					newGraph.addEdge(edata1, edata2);
				}
				makeTripletGraph_sub(n2, n1);
			}
		}
		foundHash.put(key, Integer.valueOf(2));
	}
	private void checkDir(NbrClustData edata, String n1, String n2) {
		if ( edata.clustid1.equals(n2) && edata.clustid2.equals(n1) ) {
			edata.clustid1 = n1; edata.clustid2 = n2;
		} else if ( edata.clustid1.equals(n1) && edata.clustid2.equals(n2) ) {
			// OK
		} else {
			// should not come here
			System.err.println("ERROR in NbrClustData: "+n1+":"+n2);
		}
	}
	static String joinName(String n1, String n2) {
		return(n1+Delim+n2);
	}
	static String[] splitName(String name) {
		String[] ret = name.split(Delim);
		return(ret);
	}
	static boolean checkName(String n1) {
		return (n1.indexOf(Delim) >= 0);
	}
}
