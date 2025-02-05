package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.Map.*;
import java.awt.*;

enum RearrType {
	SmallGap, SmallRearr, LargeRearr, SmallTransloc;
	boolean isRearrangement( ) {
		return( this != SmallGap );
	}
}

public class SyntenyAnalysis {
	CoreGenome coreGenome;
	GenomeData genomeData;
	int GapWin = 20;
	static boolean outputAllBp = true;
	static boolean outputGeneOrder = true;
	static boolean outputBitVectors = false;
	static String outFormat = "fasta";
	static String outBpFile = null;
	static String outBlkOrderFile = null;
	static String outSynBlockFile = null;
	static String outBitVectFile = null;
	static String outputName = null;

	static String suff_BlkOrderFile = ".blkorder";
	static String suff_SynBlockFile = ".sblk";
	static String suff_BpFile = ".bpall";
	static String suff_BitVectFile = ".bvect";

	static boolean reorderBlock = false;
	static double cutConsRatio = 0.0;
	static boolean includeSmallGap = false;
	static boolean includeSmallRearr = false;
	static int minBpOvlp;

	ArrayList<BreakPoint> bpList;
	OverlapCluster bpCluster;

	public SyntenyAnalysis(CoreGenome _coreGenome, GenomeData _genomeData) {
		coreGenome = _coreGenome;
		genomeData = _genomeData;
		BreakPoint.setCoreGenome(_coreGenome);
		bpList = new ArrayList<BreakPoint>();;

		if (reorderBlock) {
//System.err.println("makeSpIndex");
			coreGenome.makeSpIndex(genomeData);
//System.err.println("reorderBlocks");
			coreGenome.reorderBlocks();
//System.err.println("concatBlocks");
			coreGenome.concatBlocks();
		}

/*
System.err.println("makeOrder");
			coreGenome.makeOrderHash();
*/

/*** output reordered block
		CoreGenomeWriter writer = null;
		try {
			writer = new CoreGenomeWriter();
			writer.outputText(coreGenome);
		} catch (Exception e) {
		}
***/
	}
	public OverlapCluster findBpClusters() {
		coreGenome.setAllConnections(genomeData);
		SpeciesList species = coreGenome.getSpecies();
		int spnum = species.spNum();
		int[] delstart = new int[species.spNum()];
/*
		ArrayList<BreakPoint> bpList = new ArrayList<BreakPoint>();;
*/
		boolean debug_flag = false;
		RearrType type;
		for (int i = 0; i < delstart.length; i++) {
			delstart[i] = -1;
		}
		int total_corelen = coreGenome.totalLength();

		for (int spNo = 0; spNo < spnum; spNo++) {
			CoreCluster prev_cclust = null;
			int maxjump_idx = -1;	// index to the next jump link in forward direction 

/*
if (species[spNo].equals("irn001")) {
	debug_flag = true;
} else {
	debug_flag = false;
}
*/

			for (CoreCluster cclust: coreGenome) {
				int cnt_consecutive = 0, cnt_skip = 0, cnt_jump = 0, cnt_del = 0;
				int cnt_rev = 0, cnt_all = 0;
				int next_maxjump_idx = -999;
				boolean flag_jumplink_fwd = false, flag_jumplink_rev = false;
				ArrayList<Connection> connections = cclust.getConnections(spNo);
				LinkedList<DomCluster> mem = cclust.members(spNo);
				if (connections != null) {
					Connection max_prev_conn = null;
					for (Connection conn: connections) {
						int prev_idx = conn.prev_node.idx;
						if (total_corelen - prev_idx + cclust.idx < GapWin) {
							prev_idx -= total_corelen;
						}
						if (prev_idx < cclust.idx) {
/*
if (debug_flag) {
System.err.println("##>>"+cclust.id()+" "+conn.prev_node.idx+" "+cclust.idx+" >>"+conn.diff+" "+conn.clstdiff);
}
*/
							// connection to prev node
							// conn.diff: on each genome (abs); conn.clstdiff: on core genome
							//// if ( conn.diff == 1 &&
							if (conn.clstdiff == -1) {
								cnt_consecutive++;
							} else if (prev_idx == delstart[spNo] - 1) {
								// previous core genes are deleted
								cnt_skip++;
							} else {
								// jump link in reverse direction: previous core genes are inserted by rearrangement
								cnt_jump++;
								flag_jumplink_rev = true;
							}
						} else {
							if (conn.clstdiff > 1) {
								if (next_maxjump_idx < cclust.idx + conn.clstdiff) {
									next_maxjump_idx = cclust.idx + conn.clstdiff;
								}
							}
							cnt_rev++;
						}
						cnt_all++;
/*
						if (prev_idx < 0 && (max_prev_conn == null || prev_idx > max_prev_conn.prev_node.idx)) {
							max_prev_conn = conn;
						}
*/
					}
/*
					if (prev_cclust == null && max_prev_conn != null) {
						prev_cclust = max_prev_conn.prev_node;
					}
*/
				}
				if (cclust.idx < maxjump_idx) {
					// there is a jump link beyond this node
					flag_jumplink_fwd = true;
				}
				if (prev_cclust == null) {
					prev_cclust = cclust.prev_node_existing[spNo];
				}
/*
if (debug_flag) {
	System.err.println("l>>>"+ cclust.id()+";  "+mem.size()+" "+cnt_consecutive+" "+cnt_skip+" "+cnt_all+" "+cnt_jump);
}
*/
				if (mem.size() == 0) {
					// deleted ortholog
					cnt_del++;
					if (delstart[spNo] < 0) {
/*
						delstart[spNo] = cclust.orig_idx;
*/
						delstart[spNo] = cclust.idx;
					}
//					System.out.println("del: "+spNo+" "+species.get(spNo)+" "+cclust.orig_idx);
				} else {
					int mindist = cclust.getMinDist(prev_cclust, spNo);
					type = null;
					boolean flag_dupRearr = false;
					if (mem.size() > 1) {
//						System.out.println("dup: "+spNo+" "+species[spNo]+" "+cclust.orig_idx);
						// duplicate
						boolean findConnection = false;
						HashMap<DomCluster, Connection> dclHash = new HashMap<DomCluster, Connection>();
						for (Connection conn: connections) {
							Connection c = dclHash.get(conn.dcl);
							if (c != null) {
								if (c.clstdiff * conn.clstdiff < 0) {
									findConnection = true;
									break;
								}
							}
							dclHash.put(conn.dcl, conn);
						}
						if (! findConnection) {
							// not consecutive
							flag_dupRearr = true;
						}
					}
//					System.out.println("cnt:\t"+cnt_consecutive+" "+cnt_skip+" "+species[spNo]+"\t"+cclust.id());
					if (flag_dupRearr) {
						type = RearrType.LargeRearr;
					} else if (cnt_consecutive >= 1) {
						// consecutive
					} else if (cnt_skip > 0) {
						// small gap
						if (includeSmallGap) {
							type = RearrType.SmallGap;
						}
					} else if ( mindist <= GapWin ) {
						// a break point whose interval is within GapWin is classified as SmallRearr
						if (includeSmallRearr) {
							type = RearrType.SmallRearr;
						}
					} else if ( flag_jumplink_fwd || flag_jumplink_rev) {
						// a break point over which a jump link exists is classified as SmallTransloc
						if (includeSmallRearr) {
							type = RearrType.SmallTransloc;
						}
					} else {
						// break point of large rearrangement
						type = RearrType.LargeRearr;
					}
					BreakPoint bp = null;
					if (flag_dupRearr) {
						// duplicate
/*
						bp = new BreakPoint(cclust.orig_idx, cclust.orig_idx, spNo, type);
*/
						bp = new BreakPoint(cclust.idx, cclust.idx, spNo, type);
					} else if (type != null) {
						int start;
						if (delstart[spNo] == -1) {
							start = cclust.idx - 1;
/*
							start = cclust.orig_idx - 1;
*/
						} else {
							start = delstart[spNo] - 1;
						}
/*
						bp = new BreakPoint(start, cclust.orig_idx, spNo, type);
*/
						bp = new BreakPoint(start, cclust.idx, spNo, type);
					}
					if (bp != null) {
						bpList.add(bp);
					}

					if (debug_flag && type != null && prev_cclust != null) {
/*
						System.err.println("bp:"+spNo+":"+species.get(spNo)+":"+type+": "+prev_cclust.id()+" "+cclust.id()+" "+cclust.orig_idx+": "+mindist);
*/
						System.err.println("bp:"+spNo+":"+species.get(spNo)+":"+type+": "+prev_cclust.id()+" "+cclust.id()+" "+cclust.idx+": "+mindist);
					}

					delstart[spNo] = -1;
					prev_cclust = cclust;
				}
				maxjump_idx = next_maxjump_idx;

/*
				for (DomCluster dcl: cclust.members(spNo)) {
					System.out.println(cclust.idx+" "+dcl+">> "+dcl.order);
				}
*/
			}
		}

		bpCluster = OverlapCluster.createOverlapCluster(bpList, minBpOvlp, coreGenome);

//		SlinkClust slink = bpCluster.slink;

		bpCluster.createBpClustList();

		return(bpCluster);
	}

	public void outputBitVectors(String outfile) {
		SpeciesList species = coreGenome.getSpecies();
		String[] bitVectors = bpCluster.createBitVect(species.spNum());
		TraitMatrix tmat = null;
		PrintWriter writer = null;
		try {
			tmat = new TraitMatrix(bitVectors, species);
		} catch (Exception e) {
			System.err.println("Error in trait matrix");
			System.exit(1);
		} 
		String outsuff;
		if (outFormat.equals("nexus")) {
			outsuff = ".nex";
		} else {
			outsuff = ".fas";
		}

		try {
			writer = new PrintWriter(outfile + outsuff);
		} catch (IOException e) {
		}

		if (outFormat.equals("nexus")) {
			tmat.outputNexus(writer);
		} else {
			tmat.outputFasta(writer);
		}
		writer.close();
	}
	public static void main(String Args[]) {
		int fn = 0;
		String corefile = null;
		String genefile = null;

		if (Args.length < 2) {
			System.err.println("Usage: SyntenyAnalysis corefile genefile");
			System.exit(1);
		}
	       for (int i = 0; i < Args.length; i++) {
			String ag = Args[i];
			if (ag.charAt(0) == '-') {
				if (ag.startsWith("outputAllBp", 1)) {
					outputAllBp = true;
					if (ag.length() > 13) {
						outBpFile = ag.substring(13);
					}
				} else if (ag.startsWith("outputGeneOrder=", 1)) {
					outBlkOrderFile = ag.substring(17);
					if (outBlkOrderFile == "0") {
						outputGeneOrder = false;
					}
				} else if (ag.startsWith("outputSynBlock", 1)) {
					outSynBlockFile = ag.substring(16);
				} else if (ag.startsWith("outputBitVect=", 1)) {
					if (ag.length() > 15) {
						outBitVectFile = ag.substring(15);
						outputBitVectors = true;
					}
				} else if (ag.startsWith("outputName=", 1)) {
					outputName = ag.substring(12);
				} else if (ag.startsWith("outFormat=", 1)) {
					outFormat = ag.substring(11);
				} else if (ag.startsWith("reorderBlock", 1)) {
					reorderBlock = true;
				} else if (ag.startsWith("consRatio=", 1)) {
					cutConsRatio = Double.valueOf(ag.substring(11));
				} else if (ag.startsWith("includeSmallRearr", 1)) {
					includeSmallRearr = true;
				} else if (ag.startsWith("includeSmallGap", 1)) {
					includeSmallGap = true;
				} else if (ag.startsWith("minBpOvlp=", 1)) {
					minBpOvlp = Integer.valueOf(ag.substring(11));
				}
			} else {
				switch (fn++) {
				case 0:
					corefile = Args[i];
					break;
				case 1:
					genefile = Args[i];
					break;
				}
			}
		}

		if (outputName == null) {
			outputName = "syn";
		}
		if (outBlkOrderFile == null) {
			outBlkOrderFile = outputName + suff_BlkOrderFile;
		}
		if (outBpFile == null) {
			outBpFile = outputName + suff_BpFile;
		}
		if (outSynBlockFile == null) {
			outSynBlockFile = outputName + suff_SynBlockFile;
		}
		outBitVectFile = outputName + suff_BitVectFile;

		GenomeData gdata = null;
		try {
			gdata = GenomeData.readFromDomClustGeneFile(genefile);
		} catch (IOException e) {
		}

		CoreGenome coreGenome = null;
		try {
			CoreGenomeReader reader = new CoreGenomeReader(corefile, gdata);	
			if (cutConsRatio > 0) {
				reader.setConsRatio(cutConsRatio);
			}
			coreGenome = reader.readCoreGenome();
		} catch (IOException e) {
		}
		SyntenyAnalysis synanal = new SyntenyAnalysis(coreGenome, gdata);
		OverlapCluster bpCluster = synanal.findBpClusters();


		if (outputAllBp) {
			bpCluster.printAllBreakPoints(outBpFile);
			if (outBpFile == null) {
				System.exit(0);
			}
		}

		SyntenyBlock synBlock = bpCluster.createSyntenyBlock();

		if (outputGeneOrder) {
			CoreGeneOrder gOrder = new CoreGeneOrder(coreGenome);
			gOrder.assignSynBlock(synBlock);
			gOrder.outputGeneOrder(outBlkOrderFile);
			if (outSynBlockFile != null) {
				gOrder.outputSynBlocks(outSynBlockFile);
			}
		}

		if (outputBitVectors) {
			synanal.outputBitVectors(outBitVectFile);
		}


/*
		CoreGenomeWriter writer = null;
		try {
			writer = new CoreGenomeWriter(coreGenome);
		} catch (IOException e) {
		}
		writer.outputText();
*/
	}
}
class BreakPoint extends SeqRegion implements Comparable<BreakPoint>
{
	static CoreGenome coreGenome;
	int spNo;
	RearrType type;
	DomCluster dcl1, dcl1_next, dcl2, dcl2_next;
/*
	BreakPoint(CoreCluster cclust1, CoreCluster cclust2, int _spNo, RearrType _type) {
		if (cclust1.orig_idx < cclust2.orig_idx) {
			this(cclust1.orig_idx, cclust2.orig_idx, _spNo, _type);
		} else {
			this(cclust2.orig_idx, cclust1.orig_idx, _spNo, _type);
		}
	}
*/
	BreakPoint(int _begin, int _end, int _spNo, RearrType _type) {
		super(_begin, _end);
		zeroBased();
		spNo = _spNo;
		type = _type;
	}
	static void setCoreGenome(CoreGenome _coreGenome) {
		coreGenome = _coreGenome;
	}
	public int compareTo(BreakPoint r2) {
		if (begin() == r2.begin()) {
			return(end() - r2.end());
		} else {
			return(begin() - r2.begin());
		}
	}
	public String toString() {
		return(begin()+"\t"+end()+"\t"+spNo+"\t"+type);
	}
	public String toStringDetail() {
		CoreCluster begin_cclust = coreGenome.getClusterByIdx(begin());
		CoreCluster end_cclust = coreGenome.getClusterByIdx(end());
		SpeciesList species = coreGenome.getSpecies();
		String rearrInfo = dcl1+"\t"+dcl1_next+"\t"+dcl2+"\t"+dcl2_next;
		

		return(begin_cclust.id()+"\t"+end_cclust.id()+"\t"+species.get(spNo)+"\t"+rearrInfo);
	}
	void findCounterpart() {
		CoreCluster cclust1 = coreGenome.getClusterByIdx( begin() );
		CoreCluster cclust2 = coreGenome.getClusterByIdx( end() );
//		System.out.println("BP:"+ begin()+" "+end());
		if (begin() == end()){
			findCounterpart_sub(cclust1, 2);
		} else {
			findCounterpart_sub(cclust1, 0);
			findCounterpart_sub(cclust2, 1);
		}
//		System.out.println("other: "+ dcl1+" "+dcl1_next+" "+dcl2+" "+dcl2_next);
	}
	private void findCounterpart_sub(CoreCluster cclust, int side) {
		SpIndex spIndex = coreGenome.spIndex[spNo];
//System.out.println(side+" "+cclust.members(spNo));
		for (DomCluster dcl: cclust.members(spNo)) {
			int idx = spIndex.binsearch(dcl);
			DomCluster dclR=null, dclL=null;
			DomCluster dcl0 = spIndex.getByIdx(idx);
			int i = idx;
			do {
				if (++i >= spIndex.length()) {
					i = 0;
				}
				dclR = spIndex.getByIdx(i);
			} while (dclR.clustid() == dcl.clustid());

			i = idx;
			do {
				if (--i < 0) {
					i = spIndex.length() - 1;
				}
				dclL = spIndex.getByIdx(i);
			} while (dclL.clustid() == dcl.clustid());

			int diffR = dclR.getCoreIdx() - dcl.getCoreIdx();
			int diffL = dclL.getCoreIdx() - dcl.getCoreIdx();
			char minside;
			int mindiff = 0;
			DomCluster dcl_otherSide;
			if (Math.abs(diffR) < Math.abs(diffL)) {
				minside = 'R';
				mindiff = diffR;
				dcl_otherSide = dclL;
			} else {
				minside = 'L';
				mindiff = diffL;
				dcl_otherSide = dclR;
			}
	System.out.println(spNo+"> "+dcl+"\t"+dclR+" "+dclL+"; "+dclR.getCoreIdx()+" "+dcl.getCoreIdx()+" "+dclL.getCoreIdx()+"; side="+side+"; mindiff="+mindiff);

			if (side == 0 || side == 2 ) {
				// left side bp on core
				// core  C1----C2 bk C3--
				// genm dclL--dclC--dclR
				//      (C1)  (C2)  (CX)  C1 < C2
				// bp1: (dclC(C2), dclR(CX))
				if (mindiff < 0) {
					dcl1 = dcl;
					dcl1_next = dcl_otherSide;
				} else {
System.err.println("##" +side+" "+mindiff);
				}
			}
			if (side == 1 || side == 2) {
				// right side bp on core
				// core   C2 bk C3----C4--
				// genm  dclL--dclC--dclR
				//       (CY)  (C3)  (C4)  C3 < C4
				// bp2: (dclL(CY), dclC(C3)
				if (mindiff > 0) {
					dcl2 = dcl;
					dcl2_next = dcl_otherSide;
				} else {
System.err.println("##" +side+" "+mindiff);
				}
			} 
		}
	}
	static ArrayList<BreakPoint> readBpFile(String bpfile) throws IOException {
		ArrayList<BreakPoint> bpList = new ArrayList<BreakPoint>();
		BufferedReader reader;
		try {
			reader = new BufferedReader( new FileReader(bpfile) );
		} catch (IOException e) {
			throw e;
		}
		String linebuf = null;
		while  ( (linebuf = reader.readLine()) != null) {
			if (linebuf.charAt(0) == '#') {
				continue;
			}
//System.out.println(linebuf);
			String[] cols = linebuf.split("[\t ]");
			int begin = Integer.valueOf(cols[0]);
			int end = Integer.valueOf(cols[1]);
			int spNo = Integer.valueOf(cols[2]);
			RearrType type = RearrType.valueOf(cols[3]);
			BreakPoint r = new BreakPoint(begin, end, spNo, type);
			bpList.add(r);
		}
		return(bpList);
	}
	/** orig_idx is used for output */
	int orig_begin() {
		return( get_core_orig_idx( begin() ) );
	}
	int orig_end() {
		return( get_core_orig_idx( end() ) );
	}
	int get_core_orig_idx(int cidx) {
		CoreCluster cclust = coreGenome.getClusterByIdx( cidx );
		return cclust.orig_idx;
	}
	public boolean equals(BreakPoint bp2) {
		return(this.toString().equals(bp2.toString()));
	}
	public int hashCode() {
		return(this.toString().hashCode());
	}
}
class BpCount {
/*
	int begin, end, length;
*/
	BreakPoint bp;
	int count;
	Set<Integer> spSet;
	BpCount(BreakPoint _bp) {
		bp = _bp;
		spSet = new HashSet<Integer>();
		count = 1;
	}
/*
	BpCount(int _begin, int _end, int _length){
		begin=_begin; end=_end; length=_length;
		spSet = new HashSet<Integer>();
		count = 1;
	}
*/
	void add(int spNo) {
		spSet.add(spNo);
		count++;
	}
	int spCount() {
		return(spSet.size());
	}
	int compareTo(BpCount bpc) {
		/* asc order of length and desc order of count
			to find the narrowest region with the highest count */
		return ((length() == bpc.length()) ?
			bpc.spCount() - spCount() : length() - bpc.length());
	}
	int begin() {
		return bp.begin();
	}
	int end() {
		return bp.end();
	}
	int orig_begin() {
		return bp.orig_begin();
	}
	int orig_end() {
		return bp.orig_end();
	}
	int length() {
		return bp.length();
	}
	public String toString() {
		return(begin()+" "+end()+" "+spCount()+" "+count);
	}
}
/**  A cluster of overlapped breakpoints */
class BpClust {
	ArrayList<BreakPoint> bpList;
	BpCount bestBp;
	int clustid;
	BpClust(ArrayList<BreakPoint> _bpList, BpCount _bestBp) {
		bpList = _bpList;
		bestBp = _bestBp;
	}
	int count() {
		return(bpList.size());
	}
	int begin() {
		return bestBp.begin();
	}
	int end() {
		return bestBp.end();
	}
	public String toString() {
		return bestBp.toString();
	}
}
/**  Clustering result of overlapped breakpoints */
class OverlapCluster {
	SlinkClust slink;
	CoreGenome coreGenome;
	ArrayList<BpClust> bpClustList;

	OverlapCluster(CoreGenome _coreGenome) {
		coreGenome = _coreGenome;
	}

	static OverlapCluster createOverlapCluster(ArrayList<BreakPoint> seqregList, int minBpOvlp, CoreGenome coreGenome) {
		OverlapCluster oclust = new OverlapCluster(coreGenome);
		Graph ovlpGraph = new Graph();
		Collections.sort(seqregList);
		BreakPoint prev_seqreg = null;

		for (BreakPoint seqreg: seqregList) {
//			ovlpGraph.addNode(seqreg);

//System.out.println(seqreg+" -- "+prev_seqreg+"  : "+seqreg.overlap(prev_seqreg)+"  "+seqreg.offset);
			// overlap length is set to zero
			if (seqreg.overlap(prev_seqreg, -minBpOvlp)) {
//System.out.println("OVLP");
				ovlpGraph.addEdge(seqreg, prev_seqreg);
			} else {
				// for singleton
				ovlpGraph.addNode(seqreg);
			}

			prev_seqreg = seqreg;
		}

		oclust.slink = new SlinkClust(ovlpGraph);
		oclust.slink.clustering();

		return oclust;
	}
	void createBpClustList() {
		bpClustList = new ArrayList<BpClust>();;
//int clustid = 0;
		for (ArrayList bpList: slink.clusterList) {
			HashMap<String,BpCount> BpHash = new HashMap<String,BpCount>();
			HashSet<Integer> spSetClust = new HashSet<Integer>();
//clustid++;
			for (BreakPoint r: (ArrayList<BreakPoint>)bpList) {
				String regStr = r.begin+":"+r.end;
				BpCount bp;
				if (BpHash.containsKey(regStr)) {
					bp = BpHash.get(regStr);
					bp.add(r.spNo);
				} else {
/*
					bp = new BpCount(r.begin,r.end,r.length());
*/
					bp = new BpCount(r);
					BpHash.put(regStr, bp);
				}
				spSetClust.add(r.spNo);
			}
/*
			if (BpHash.size() == 0) {
				// only small rearrangement
				continue;
			}
*/
		/* sort BpHash in descending order */
			ArrayList<Entry<String,BpCount>> countRegList =
				new ArrayList<Entry<String,BpCount>>(BpHash.entrySet());
			Collections.sort(countRegList, new Comparator<Entry<String,BpCount>>() {
				public int compare(Entry<String,BpCount>o1, Entry<String,BpCount>o2) {
					return(o1.getValue().compareTo(o2.getValue()));
				}
			});
			/* most frequent posiition */
			BpCount bestBp = countRegList.get(0).getValue();
			int spcount = 0;
/*
			for (Entry<String,BpCount> bpent: countRegList) {
				spcount += bpent.getValue().spCount();
			}
*/
			bestBp.count = spSetClust.size();
			BpClust bpClust = new BpClust(bpList, bestBp);
			bpClustList.add(bpClust);
		}
		bpClustList.sort( (a,b) -> (a.bestBp.begin() - b.bestBp.begin()) );
		int clustid = 1;
		for (BpClust bpc: bpClustList) {
			bpc.clustid = clustid++;
		}
	}
	void addBpCounterpart() {
/*
		for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
			coreGenome.spIndex[spNo]
		}
		for (BpClust bpClust: bpClustList) {
			bpClust.spNo
			bpClust.begin
			bpClust.end
		}
*/
	}
	void printAllBreakPoints(String outBpFile) {
//		int clustid = 0;
		PrintStream outf = System.out;
		if (outBpFile != null) {
			try {
				outf = new PrintStream(outBpFile);
			} catch (Exception e) {
				System.err.println("Can't open output file: " + outBpFile);
			}
		}
		for (BpClust bpClust: bpClustList) {
			Collections.sort(bpClust.bpList, (a,b) -> (a.spNo - b.spNo) );
			ArrayList<BreakPoint>bpList = bpClust.bpList;
			BpCount bestBp = bpClust.bestBp;
//System.out.println("CLUST:"+clustid+" "+bpList.size());
			for (BreakPoint bp: bpList) {
				bp.findCounterpart();
				Object[] output = { bestBp.orig_begin(), bestBp.orig_end(), bp.spNo, bp.type, bpClust.clustid, bpClust.count(),
					 bp.orig_begin(), bp.orig_end(), "# "+bp.toStringDetail() };
				StringJoiner sj = new StringJoiner("\t");
				Arrays.asList(output).forEach( a ->  sj.add(a.toString()));
				outf.println( sj.toString() );
			}
//			clustid++;
		}
	}
	SyntenyBlock createSyntenyBlock() {
		SyntenyBlock synBlock = new SyntenyBlock(bpClustList, coreGenome);
//		System.out.println(synBlock);
		return(synBlock);
	}
	String[] createBitVect(int specnum) {
		int bitvect[][] = new int[ specnum ][ slink.clustNum ];
		for (int i = 0; i < specnum; i++) {
			for (int j = 0; j < slink.clustNum; j++) {
				bitvect[i][j] = 0;
			}
		}
		int clustid = 0;
		for (ArrayList bpList: slink.clusterList) {
//System.out.println(clustid+" "+vec.size());
			for (BreakPoint r: (ArrayList<BreakPoint>)bpList) {
				bitvect[r.spNo][clustid] = 1;
			}
			clustid++;
		}
		String bitvectString[] = new String[ specnum ];
		for (int i = 0; i < specnum; i++) {
			bitvectString[i] = "";
			for (int j = 0; j < slink.clustNum; j++) {
				bitvectString[i] += Integer.toString(bitvect[i][j]);
			}
		}
		return(bitvectString);
	}
	void resultout() {
		int clustid = 0;
		for (ArrayList bpList: slink.clusterList) {
			clustid++;
			for (BreakPoint r: (ArrayList<BreakPoint>)bpList) {
				System.out.println(clustid+" "+r);
			}
		}
	}
}
class CoreGeneOrder {
	class CoreClusterInfo {
		CoreCluster cclust;
		int origIdx, coreIdx;
		int synBlockId;
		CoreClusterInfo(CoreCluster _cclust, int _orig_idx, int _coreIdx) {
			cclust = _cclust; origIdx = _orig_idx; coreIdx = _coreIdx;
			synBlockId = -1;
		}
		public String toString() {
			return(origIdx+","+coreIdx+"("+synBlockId+") ");
		}
	}
	class ClusterGene {
		int spNo;
		CoreCluster cclust;
		DomCluster dcl;
		ClusterGene(int _spNo, CoreCluster _cclust, DomCluster _dcl) {
			spNo = _spNo;
			cclust = _cclust;
			dcl = _dcl;
		}
		public String toString() {
			return("["+dcl+"]");
		}
	}

	String outfile;
	CoreGenome coreGenome;
	/** list containing the order of orthologous genes */
	ArrayList<ArrayList<ClusterGene>> clustList;
	ArrayList<CoreClusterInfo> coreClusterList;

	/** list of signed geneid in each genome for output (e.g. -3 -2 -1 4 5)  */
	ArrayList< ArrayList<Integer> > orderListAll;

	HashMap<String, CoreClusterInfo> coreClusterHash;
	boolean flag_synBlock;
	SyntenyBlock synBlocks;
	/** synBlock ID after renumbering to retain only coserved blocks */
	int[] convId, convId_rev;

	CoreGeneOrder(CoreGenome _coreGenome) {
		coreGenome = _coreGenome;
		clustList = new ArrayList<ArrayList<ClusterGene>>();
		flag_synBlock = false;
		createGeneOrder();
	}
/*
	CoreGeneOrder(CoreGenome _coreGenome, String _outfile) {
		outfile = _outfile;
		coreGenome = _coreGenome;
		try {
			writer = new PrintWriter(outfile);
		} catch (IOException e) {
		}
	}
*/
	/** create the order of orthologous genes for each genome */
	void createGeneOrder() {
		for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
			clustList.add( new ArrayList<ClusterGene>() );
		}
		coreClusterList = new ArrayList<CoreClusterInfo>();
		coreClusterHash = new HashMap<String, CoreClusterInfo>();
		int origIdx = 0, coreIdx = 0;
		for (CoreGenomeBlock blk: coreGenome.blocks) {
			for (CoreCluster cclust: blk) {
/*
				boolean skip = false;
				for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
					String sp = coreGenome.species.get(spNo);
					LinkedList<DomCluster>mem = cclust.members(spNo);
					if (mem.size() == 1) {
					} else if (mem.size() == 2) {
						if (Math.abs(mem.get(0).order - mem.get(1).order) <= 1){
						} else {
							skip = true;
							break;
						}
					} else {
						skip = true;
						break;
					}
				}
				if (flag_synBlock || ! skip) {
*/
					ArrayList<ClusterGene> cList = new ArrayList<ClusterGene>(coreGenome.specNum());
					for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
						String sp = coreGenome.species.get(spNo);
						LinkedList<DomCluster>mem = cclust.members(spNo);
						if (mem.size() == 0) continue;
						DomCluster dcl = mem.get(0);
						ClusterGene clGene = new ClusterGene(spNo, cclust, dcl);
						clustList.get(spNo).add(clGene);
					}
					CoreClusterInfo ccInfo = new CoreClusterInfo(cclust, origIdx, ++coreIdx);
					coreClusterList.add(ccInfo);
					coreClusterHash.put(cclust.id(), ccInfo);
/*
				}
*/
				origIdx++;
			}
		}
		for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
			clustList.get(spNo).sort( (a,b) -> (a.dcl.order - b.dcl.order ) );
		}
	}
	/* Assign each core ortholog group to a synteny block */
	void assignSynBlock(SyntenyBlock _synBlocks) {
		class SynBlockCount {
			int synBlockId;	// begining from 1
			int dir;
			int count;
			ArrayList<CoreClusterInfo> ccList;

			/* synteny block with direction and block length */
			SynBlockCount(int _blkid, int _dir, int _cnt, ArrayList<CoreClusterInfo>_ccList) {
				synBlockId = _blkid; dir = _dir; count = _cnt; ccList = _ccList;
			}
			public String toString() {
				return dir * synBlockId + ", " + count;
			}
			int dirId() {
				if (synBlockId < 0) {
					return 0;
				}
				return synBlockId * dir;
			}
			void merge(SynBlockCount blk) {
				if (synBlockId != blk.synBlockId || dir != blk.dir) {
					System.err.println("Can't merge: "+ this+" ; "+blk);
					return;
				}
				count += blk.count;
				ccList.addAll(blk.ccList);
			}
			void printList() {
				for (CoreClusterInfo ccInfo: ccList) {
					System.err.print(" "+ccInfo);
				}
			}
		}

		int synBlockId = 0;
		synBlocks = _synBlocks;

/*
		GeneBlock blk = synBlocks.synBlock.get(synBlockId);
*/
		GeneBlock blk = null;
		flag_synBlock = true;
		for (CoreClusterInfo ccInfo: coreClusterList) {
			if (blk == null || ccInfo.origIdx > blk.end) {
				do {
					/* find next synteny block to be assiged to the current cclust */
					++synBlockId;
					if (synBlockId <= synBlocks.synBlock.size()) {
						blk = synBlocks.synBlock.get(synBlockId-1);
					} else {
						blk = null;
						break;
					}
				} while (ccInfo.origIdx > blk.end);
				if (blk == null) {
					break;
				}
			}
			if (ccInfo.origIdx >= blk.begin && ccInfo.origIdx <= blk.end) {
				ccInfo.synBlockId = synBlockId;
			}
//			System.out.println("ID:"+ccInfo.origIdx+" "+ccInfo.synBlockId+" "+synBlockId+" "+blk.begin+" "+blk.end);
		}

		int numBlocks;
		int coreIdx;

		orderListAll = new ArrayList<ArrayList<Integer>>();

		if (flag_synBlock) {
			numBlocks = synBlocks.size();
		} else {
			numBlocks = coreGenome.totalLength();
		}
		int[] spCount_OrthoBlk = new int[ numBlocks ];

		for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
			ArrayList<Integer> orderList = new ArrayList<Integer>();
			ArrayList<SynBlockCount> blkOrderList = new ArrayList<SynBlockCount>();
			int fwd_cnt = 0, rev_cnt = 0;
			CoreClusterInfo prev_ccInfo = null;
			ArrayList<CoreClusterInfo> ccList = new ArrayList<CoreClusterInfo>();
			for (ClusterGene clGene: clustList.get(spNo)) {
				int sign = 1;
				CoreClusterInfo ccInfo = coreClusterHash.get(clGene.cclust.id());
//System.out.println("##"+spNo+" "+ccInfo.synBlockId+" "+ccInfo.coreIdx+" "+clGene.cclust.id());
				if (flag_synBlock) {
					if (ccInfo.synBlockId < 0) {
						continue;
					}
					if (prev_ccInfo != null) {
						if (prev_ccInfo.synBlockId != ccInfo.synBlockId && prev_ccInfo.synBlockId >= 0) {
							sign =  (fwd_cnt < rev_cnt) ? -1 : 1;
//							orderList.add(prev_ccInfo.synBlockId * sign);
							SynBlockCount blkCnt = new SynBlockCount(prev_ccInfo.synBlockId, sign, fwd_cnt + rev_cnt, ccList);
							blkOrderList.add(blkCnt);
							
							fwd_cnt = rev_cnt = 0;
							ccList = new ArrayList<CoreClusterInfo>();
						}
						if (prev_ccInfo.coreIdx < ccInfo.coreIdx) {
							fwd_cnt++;
						} else {
							rev_cnt++;
						}
					}
					prev_ccInfo = ccInfo;
				} else {
					if (clGene.dcl.dir() < 0) {
						sign = -1;
					}
					orderList.add(ccInfo.coreIdx * sign);
				}
			}
			if (flag_synBlock) {
				int sign =  (fwd_cnt < rev_cnt) ? -1 : 1;
//				orderList.add(prev_ccInfo.synBlockId * sign);
				SynBlockCount blkCnt = new SynBlockCount(prev_ccInfo.synBlockId, sign, fwd_cnt + rev_cnt, ccList);
				blkOrderList.add(blkCnt);
/*
				for (SynBlockCount blkCount: blkOrderList) {
					System.out.println(blkCount);
				}
*/
				HashMap<Integer, SynBlockCount> blkOrderHash = new HashMap<Integer, SynBlockCount>();
				ArrayList<SynBlockCount> remBlkList = new ArrayList<SynBlockCount>();
				/* Keep the longest occurrence if the same synblock separately appears in different positions */
				for (SynBlockCount blkCount: blkOrderList) {
					int gid = blkCount.synBlockId;
					if (blkOrderHash.containsKey(gid)) {
						SynBlockCount prevHit_blkC = blkOrderHash.get(gid);
						if (prevHit_blkC.count > 1 &&  blkCount.count > 1) {
/*
							System.err.println("Duplicated key 1: "+spNo+" "+gid+"; "+prevHit_blkC+" "+blkCount);
							prevHit_blkC.printList();
							blkCount.printList();
*/
						}
						if (prevHit_blkC.count < blkCount.count) {
							remBlkList.add(prevHit_blkC);
							blkOrderHash.put(gid, blkCount);
						} else {
							remBlkList.add(blkCount);
						}
					} else {
						blkOrderHash.put(gid, blkCount);
					}
				}
				blkOrderList.removeAll(remBlkList);
/*
				remBlkList.clear();
				SynBlockCount prev_blkC = null;
				for (SynBlockCount blkCount: blkOrderList) {
					if (prev_blkC != null && prev_blkC.synBlockId == blkCount.synBlockId) {
						// merge with the previous synteny block
						prev_blkC.merge(blkCount);
						remBlkList.add(blkCount);
					}
					prev_blkC = blkCount;
				}
				blkOrderList.removeAll(remBlkList);
*/
				for (SynBlockCount blkCount: blkOrderList) {
//System.out.println(blkCount.synBlockId+" "+blkCount.dirId()+" "+ synBlocks.synBlock.get(blkCount.synBlockId-1));
					orderList.add(blkCount.dirId());
				}
			}
			/* count species for each orthologs to check conservation */
			for (int gid: orderList) {
				int absId = Math.abs(gid);
				spCount_OrthoBlk[absId-1]++;
			}

			/* for check */
			HashSet<Integer> orderHash = new HashSet<Integer>();
			for (int gid: orderList) {
				if (orderHash.contains(Math.abs(gid))) {
					System.err.println("Duplicated key 2: "+spNo+" "+gid);
				}
				orderHash.add(Math.abs(gid));
			}
			orderListAll.add(orderList);
		}
		/** eliminate non-conserved orthologs and renumbering */
		convId = new int[ numBlocks ];
		int new_i = 0;
		for (int i = 0; i < spCount_OrthoBlk.length; i++) {
//			System.out.println(">>"+i+" "+spCount_OrthoBlk[i]);
			if (spCount_OrthoBlk[i] == coreGenome.specNum()) {
				convId[i] = new_i;
				new_i++;
			} else {
				/** skip non-conserved block */
				convId[i] = -1;
			}
		}
		convId_rev = new int[ new_i ];
		for (int i = 0; i < spCount_OrthoBlk.length; i++) {
			if (convId[i] >= 0) {
				convId_rev[ convId[i] ] = i;
			}
		}
		/** update orderListAll with renumbered Id */
		for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
			ArrayList<Integer>orderList = orderListAll.get(spNo);
			ArrayList<Integer>newOrderList = new ArrayList<Integer>();
			for (int gid: orderList) {
				int newId = convId[ Math.abs(gid) - 1 ] + 1;
				int sign = (gid > 0) ? 1 : -1;
				if (newId > 0) {
					newOrderList.add(newId * sign);
				}
			}
			orderListAll.set(spNo, newOrderList);
		}

	}

	/* output the order of genes or synteny blocks */
	void outputGeneOrder(String outfile) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outfile);
		} catch (IOException e) {
		}

		for (int spNo = 0; spNo < coreGenome.specNum(); spNo++) {
			ArrayList<Integer>orderList = orderListAll.get(spNo);

//			writer.println(">"+coreGenome.species.get(spNo)+" "+orderList.size());
			writer.println(">"+coreGenome.species.get(spNo));
			for (int gid: orderList) {
				writer.print(" ");
				writer.print(gid);
			}
			writer.println(" $");
		}
		writer.close();
	}
	void outputSynBlocks(String outfile) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outfile);
		} catch (IOException e) {
		}
		int blkn = 0;
		for (int i = 0; i <  synBlocks.synBlock.size(); i++) {
			int blkId = convId[i] >= 0 ? convId[i] + 1 : -1;
			GeneBlock geneBlk = synBlocks.synBlock.get(i);
			writer.print(blkId + "\t" + geneBlk);
			writer.print("\t"+geneBlk.bpClust.clustid);
			writer.print("\t"+geneBlk.bpClust.count());
			ArrayList<CoreCluster> cclustList = synBlocks.getCoreClusters(i);
			writer.print("\t");
			int ln = 0;
			for (CoreCluster cclust: cclustList) {
				if (ln++ > 0) {
					writer.print(",");
				}
				writer.print(cclust.id());
			}
			writer.println();
		}
		writer.close();
	}

}
class TraitMatrix {
	String matrix[];
	SpeciesList species;
	int nchar, ntax;
	TraitMatrix(String[] data, SpeciesList species) throws Exception {
		setData(data);
		setSpecies(species);
	}
	void setData(String[] data) throws Exception {
		matrix = data;
		for (String str: matrix) {
			if (nchar == 0) {
				nchar = str.length();
			} else if (nchar != str.length()) {
				throw new Exception();
			}
		}
		ntax = matrix.length;
	}
	void setSpecies(SpeciesList _species) {
		species = _species;
	}
	void outputFasta(PrintWriter writer) {
		for (int i = 0; i < ntax; i++) {
			String str = matrix[i];
			str = str.replace('0', 'A');
			str = str.replace('1', 'G');
			writer.println(">"+species.get(i));
//			writer.println(matrix[i]);
			writer.println(str);
		}
	}
	void outputNexus(PrintWriter writer) {
		writer.println("#NEXUS");
		writer.println("Begin taxa;");
		writer.println("  Dimensions ntax=" + ntax + ";");
		writer.println("  TaxLabels " + String.join(" ", species.toArray()));
		writer.println(";");
		writer.println("End;");

		writer.println("Begin data;");
		writer.println("  Dimensions nchar=" + nchar + ";");
		writer.println("Matrix");
		for (int i = 0; i < ntax; i++) {
			String bvect = matrix[i];
			writer.println(species.get(i)+"    "+bvect);
		}
		writer.println(";");
		writer.println("End;");
	}
	int ntax() {
		return(ntax);
	}
	int nchar() {
		return(nchar);
	}
}

class GeneBlock extends SeqRegion {
	BpClust bpClust; // break point cluster located at the end of this block
	GeneBlock(int begin, int end, BpClust _bpClust) {
		super(begin, end);
		bpClust = _bpClust;
	}
	public String toString() {
		return "("+begin+" "+end+")";
	}
}
class SyntenyBlock {
	ArrayList<BpClust> bpClustList;
	ArrayList<GeneBlock> synBlock;
	CoreGenome coreGenome;
	SyntenyBlock(ArrayList<BpClust> _bpClustList) {
		this(_bpClustList, null);
	}
	SyntenyBlock(ArrayList<BpClust> _bpClustList, CoreGenome _coreGenome) {
		bpClustList = _bpClustList;
		coreGenome = _coreGenome;
		createSynBlock();
/*
		filterCount();
		sort();
*/
	}
	void createSynBlock() {
		synBlock = new ArrayList<GeneBlock>();
		int begin = -1, end = -1;
		int save_end = 0;
		BpClust save_bpClust = null;
		for (BpClust bpClust: bpClustList) {
			end = bpClust.begin();
			if (begin < 0) {
				save_end = end;
				save_bpClust = bpClust;
			} else {
				synBlock.add(new GeneBlock(begin, end, bpClust));
			}
			begin = bpClust.end();
		}
		if (save_end < 0) {
			end = coreGenome.totalLength() + save_end;
		} else {
			end = save_end;
		}
		synBlock.add(new GeneBlock(begin, end, save_bpClust));
	}
	void filterCount() {
		int minCount = 2;
		ArrayList<BpClust> newClustList = new ArrayList<BpClust>();
		for (BpClust bpClust: bpClustList) {
			if (bpClust.bestBp.spCount() >= minCount) {
				newClustList.add(bpClust);
			}
		}
		bpClustList = newClustList;
	}
	int size() {
		return(synBlock.size());
	}
	void sort() {
		bpClustList.sort( (a,b) -> (a.bestBp.begin() - b.bestBp.begin()) );
	}
	ArrayList<CoreCluster> getCoreClusters(int blk_i) {
		ArrayList<CoreCluster> cclustList = new ArrayList<CoreCluster>();
		GeneBlock blk = synBlock.get(blk_i);
		for (int i = blk.begin; i <= blk.end; i++) {
			CoreCluster cclust = coreGenome.getClusterByIdx(i);
			cclustList.add(cclust);
		}
		return(cclustList);
	}
	public String toString() {
		return(synBlock.toString());
/*
		return("bpClustList="+bpClustList);
*/
	}
}

