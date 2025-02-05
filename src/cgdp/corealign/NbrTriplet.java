package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;

class NbrTriplet {
	HashMap hash;
	ClusterSet clustSet;
	static final String delim = ";";
	static final int LEFT = Direction.LEFT, RIGHT = Direction.RIGHT;
	int NBR_CONS_NUM;

	NbrTriplet() {
		hash = new HashMap();
	}
	NbrTriplet(ClusterSet _clustSet) {
		hash = new HashMap();
		clustSet = _clustSet;
		NBR_CONS_NUM = 2;
	}
	void setConsNum(int consNum) {
		NBR_CONS_NUM = consNum;
	}
	void add(String name, int side, String clustid, int dist) {
		String key = name + delim + side;
		HashMap hash2;
		if (hash.containsKey(key)) {
			hash2 = (HashMap) hash.get(key);
		} else {
			hash2 = new HashMap();
			hash.put(key, hash2);
		}
		hash2.put(clustid, Integer.valueOf(dist));
	}
	boolean checkNbr(String name, int dir, String clustid) {
		String key = name + delim + dir;
		HashMap hash2;
		if (hash.containsKey(key)) {
			hash2 = (HashMap) hash.get(key);
			return(hash2.containsKey(clustid));
		}
		return(false);
	}
	ArrayList getOrderedNbrList(String name, int dir) {
		String key = name + delim + dir;
		HashMap hash2 = (HashMap) hash.get(key);
		if (hash2 == null) {
			return null;
		}
		Set clustSet = hash2.keySet();
		ArrayList alist = new ArrayList( clustSet );
		class CompareDist implements Comparator {
			HashMap hash2;
			CompareDist(HashMap _hash2) {
				hash2 = _hash2;
			}
			public int compare (Object cl1, Object cl2) {
				Integer dist1 = (Integer) hash2.get(cl1);
				Integer dist2 = (Integer) hash2.get(cl2);
				return(dist1.intValue() - dist2.intValue());
			}
		}
		Collections.sort(alist, new CompareDist(hash2) );
		return(alist);
	}
	boolean checkTriplet(String clid0, String clid1, String clid2) {
		Cluster clust1 = clustSet.getCluster(clid1);
		LinkedList memlist1;
		DomCluster dclst;

		if (clid0 == null) {
			// always come here in the first call
			return true;
		}
		int cnt = 0;
		for (int i = 0; i < clustSet.specNum; i++) {
			memlist1 = clust1.members(i);
			Iterator iter1 = memlist1.iterator();
			while (iter1.hasNext()) {
				dclst = (DomCluster) iter1.next();
				String name = dclst.dom.getName();
				if ((checkNbr(name, LEFT, clid0) &&
					checkNbr(name, RIGHT, clid2)) ||
				     checkNbr(name, RIGHT, clid0) &&
					checkNbr(name, LEFT, clid2)) {
					cnt++;
					break;
				}
			}
		}
		if (cnt >= NBR_CONS_NUM) {
			return true;
		} else {
			return false;
		}
	}
	public static void main(String[] args) {
		NbrTriplet ntrp = new NbrTriplet();
		ntrp.add("A", LEFT, "1", 2);
		ntrp.add("A", LEFT, "2", 1);
		ntrp.add("A", RIGHT, "3", 1);
		ntrp.add("A", RIGHT, "4", 2);
		boolean ret = ntrp.checkTriplet("1","2","3");
		System.out.println(ret);
	}
}
