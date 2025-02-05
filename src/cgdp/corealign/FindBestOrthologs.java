package cgdp.corealign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/** Find best "positional" ortholog among inparalogs */
class FindBestOrthologs {
	ClusterSet clstSet;
	AlignmentPath aliPath;
	int maxGap = 20;
	ContextWeightHash conHash;
	DupCidCheck dupCheck;
	double OrthoTolerance = 0.4;
	double Min_MaxWeight = 1.0;

	FindBestOrthologs(ClusterSet clset, AlignmentPath alpath,
					DupCidCheck dupchk) {
		clstSet = clset;
		aliPath = alpath;
		conHash = new ContextWeightHash();
		dupCheck = dupchk;
	}
	void setOrthoTolerance(double tol) {
		OrthoTolerance = tol;
	}
	void findBestAll() {
		Iterator iter = aliPath.iterator();
		Iterator iter2;
		while (iter.hasNext()) {
			ClustAliPath alip = (ClustAliPath) iter.next();
			ArrayList alist = alip.toArrayList();
			for (int idx = 0; idx < alist.size(); idx++) {
				String cid = (String) alist.get(idx);
				for (int j = 0; j < clstSet.specNum; j++) {
					String sp = clstSet.species.get(j);
					findBest(cid, sp, alip, idx);
				}
			}
		}
		Set domSet = conHash.keySet();
		HashMap maxData = new HashMap();
		for (iter = domSet.iterator(); iter.hasNext(); ) {
			DomCluster dom = (DomCluster) iter.next();
			LinkedList wdataList = (LinkedList) conHash.getData(dom);
			double maxWeight = 0.0;
			String maxCid = null;
			for (iter2 = wdataList.iterator(); iter2.hasNext(); ) {
				ContextWeight wdata =
					(ContextWeight) iter2.next();
				if (wdata.weight > maxWeight) {
					maxWeight = wdata.weight;
					maxCid = wdata.clustid;
				}
			}
			String spec = dom.dom.spec;
			HashMap spcldata = (HashMap) maxData.get(maxCid);
			LinkedList cldata = null;
			if (spcldata == null) {
				spcldata = new HashMap();
				maxData.put(maxCid, spcldata);
			} else {
				cldata = (LinkedList) spcldata.get(spec);
			}
			if (cldata == null) {
				cldata = new LinkedList();
				spcldata.put(spec, cldata);
			}
			cldata.add(dom);
		}
		Set newAssign = maxData.keySet();
		for (iter = newAssign.iterator(); iter.hasNext(); ) {
			String newCid = (String) iter.next();
			HashMap spcldata = (HashMap) maxData.get(newCid);
			Set species = spcldata.keySet();
			for (iter2 = species.iterator(); iter2.hasNext(); ) {
				String spec = (String) iter2.next();
				LinkedList cldata =
					(LinkedList) spcldata.get(spec);
				if (cldata != null) {
					clstSet.changeClustID(
						cldata, newCid, spec);
				}
			}
		}
	}
	void findBest(String cid, String sp, ClustAliPath alip, int idx) {
		LinkedList spClustData = clstSet.getClusterData_DupCheck(cid,sp);
		if (spClustData == null) return;
		ArrayList wdataList = null;
		double maxWeight = 0.0;

		boolean dupClustFlag = dupCheck.isNewCID(cid) ? true : false;

//		if (! dupClustFlag) {
//		}
		wdataList = new ArrayList();

		Iterator iter = spClustData.iterator();
		while (iter.hasNext()) {
			double weight = 0.0;
			DomCluster dom = (DomCluster) iter.next();
			for (int j = -maxGap; j <= maxGap; j++) {
				if (j == 0) continue;
				int idxj = idx + j;
				if (idxj < 0) continue;
				if (idxj >= alip.size()) break;
				String previd = alip.getData(idxj);
				LinkedList prevData =
				      clstSet.getClusterData_DupCheck(previd,sp);
				if (prevData == null) continue;
				int clustDiff = Math.abs(j);
				int minDiff = Integer.MAX_VALUE;
				DomCluster mindom = null;
				Iterator iter2 = prevData.iterator();
				while(iter2.hasNext()) {
					DomCluster pdom = (DomCluster)iter2.next();
					if (dom == pdom) {
						continue;
					}
					int diff = Math.abs(dom.order - pdom.order);
					if (diff < minDiff) {
						minDiff = diff;
						mindom = pdom;
					}
				}
				if (minDiff != Integer.MAX_VALUE) {
					double wt1 = CalWeight.calc(minDiff);
					double wt2 = CalWeight.calc(clustDiff);
					weight += Math.sqrt(wt1 * wt2);
// for debug
//System.out.println("findBest: "+dom+" "+cid+" "+minDiff+" "+clustDiff+" w="+wt1+" "+wt2+"; weight="+weight+"; mindom="+mindom+" "+previd);
				}
			}
			if (weight == 0) {
				continue;
			}
			if (idx==0 || idx==alip.size()-1) {
				weight *= 1.5;
			}
			if (dupClustFlag) {
//System.out.println("FindBest: "+dom+" "+cid+" "+weight);
				//assign weight to choose appropriate cluster
				conHash.putData(dom, cid, weight);
			}
//			} else {
				//assign weight to eliminate paralogs
				ContextWeight wdata = new ContextWeight(
					dom, cid, weight);
				wdataList.add(wdata);
				if (weight > maxWeight) {
					maxWeight = weight;
				}
//			}
		}
//		if (! dupClustFlag) {
//			LinkedList maxSpData = new LinkedList();
			for(int i  = 0; i < wdataList.size(); i++) {
				ContextWeight wtd = (ContextWeight)
						wdataList.get(i);
//For debug
//System.out.println( "w="+wtd.weight+"; maxw="+maxWeight+"; dom="+wtd.dom );
				if (maxWeight < Min_MaxWeight || wtd.weight >= maxWeight * OrthoTolerance) {
//					maxSpData.add(wtd.dom);
					wtd.dom.setPosOrtho();
				}
			}
/*
			if (maxSpData.size() > 0 && spClustData.size() > 1) {
//System.out.println("maxsp>"+maxSpData+" ");
				clstSet.changeClustID(maxSpData, cid, sp);
			}
*/
//		}
	}
}
class ContextWeightHash {
	HashMap hash;
	ContextWeightHash() {
		hash = new HashMap();
	}
	LinkedList getData(DomCluster d) {
		return (LinkedList) hash.get(d);
	}
	Set keySet() {
		return hash.keySet();
	}
	void putData(DomCluster dom, String cid, double weight) {
		LinkedList list = (LinkedList) hash.get(dom);
		if (list == null) {
			list = new LinkedList();
			hash.put(dom, list);
		}
		ContextWeight wdata = new ContextWeight(dom, cid, weight);
		list.add(wdata);
	}
}
class ContextWeight {
	String clustid;
	double weight;
	DomCluster dom;
	ContextWeight(DomCluster d, String id, double w) {
		clustid = id;
		weight = w;
		dom = d;
	}
}
