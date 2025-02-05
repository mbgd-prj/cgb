package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;

public class CoreAlign {
	static String clustFile, genomesFile;
	static String refsp;
	static SpGroup spGroup;
	static String pairoutFile, linkoutFile, tripletoutFile, alignoutFile;
	static double ConsRatio = 0.5;
	static double NbrConsRatio = 0;
	/* secondary condition for CoreAlign */
//	static double NbrConsRatio2 = 0.1;
	static double NbrConsRatio2 = 0;	/* default(0) is same with NbrConsRatio */
	static int GapWin = 20;
	static int MinClustCnt = 20;
	static int flag_posortho = 1;
	static boolean skip_findbest = false;
	static boolean domclustIn = false;
	static boolean execFindIsland = false;
	static boolean outputAll = false;
	static double orthoTolerance = 0.6;
	static String version = "2.1.0";
	static void getArgs(String args[]) {
		int fn = 0;
		for (int i = 0; i < args.length; i++) {
			String ag = args[i];
			if (ag.charAt(0) == '-') {
				if (ag.startsWith("refsp=", 1)) {
					refsp = ag.substring(7);
				} else if (ag.startsWith("SpGrp=", 1)) {
					String spgrpSpec = ag.substring(7);
					spGroup = new SpGroup(spgrpSpec);
				} else if (ag.startsWith("GapWin=", 1)) {
					GapWin = Integer.parseInt(
							ag.substring(8));
				} else if (ag.startsWith("ConsRatio=", 1)) {
					ConsRatio = Double.parseDouble(
							ag.substring(11));
				} else if (ag.startsWith("NbrConsRatio=", 1)) {
					NbrConsRatio = Double.parseDouble(
							ag.substring(14));
				} else if (ag.startsWith("NbrConsRatio2=", 1)) {
					NbrConsRatio2 = Double.parseDouble(
							ag.substring(15));
				} else if (ag.startsWith("MinClustCnt=", 1)) {
					MinClustCnt = Integer.parseInt(
						ag.substring(13));
				} else if (ag.startsWith("pairout=", 1)) {
					pairoutFile = ag.substring(9);
				} else if (ag.startsWith("linkout=", 1)) {
					linkoutFile = ag.substring(9);
				} else if (ag.startsWith("tripletout=", 1)) {
					tripletoutFile = ag.substring(12);
				} else if (ag.startsWith("alignout=", 1)) {
					alignoutFile = ag.substring(10);
				} else if (ag.startsWith("posOrtho=", 1)) {
					flag_posortho = Integer.parseInt(
						ag.substring(10));
				} else if (ag.startsWith("orthoTolerance=", 1)) {
					orthoTolerance = Double.parseDouble(
						ag.substring(16));
				} else if (ag.startsWith("domclustIn", 1)) {
					domclustIn = true;
				} else if (ag.startsWith("execFindIsland", 1)) {
					execFindIsland = true;
				} else if (ag.startsWith("outputAll", 1)) {
					outputAll = true;
				} else if (ag.startsWith("version", 1)) {
					showVersion();
				} else if (ag.startsWith("help", 1)) {
					usage_out();
					System.exit(0);
				}
			} else {
				switch (fn++) {
				case 0:
					clustFile = args[i];
					break;
				case 1:
					genomesFile = args[i];
					break;
				}
			}
		}
		if (NbrConsRatio == 0) {
			NbrConsRatio = ConsRatio;
		} else if (NbrConsRatio > ConsRatio) {
			ConsRatio = NbrConsRatio;
		}
	}
	static void  showVersion() {
		System.err.println("CoreAligner ver. "+  version);
	}
	static void  usage_out() {
		showVersion();
		System.err.println("Usage: CoreAlign [options] clustfile genomefile");
		System.err.println(" -refsp,-NbrConsRatio,-GapWin,-alignout,-pairout,-linkout");
	}
	public static void main(String args[]) {
		getArgs(args);

		if (clustFile == null || genomesFile == null) {
			System.err.println("Usage: CoreAlign clustfile genomefile");
			System.exit(0);
		}

		GenomeData gdata = null;
		try {
			gdata = GenomeData.readFromFile(genomesFile, domclustIn);
		} catch (IOException e) {
			System.err.println("Can't read genome file: " + genomesFile);
			e.printStackTrace();
			System.exit(1);
		}
		ClusterSetReader cf = null;
		try {
			cf = new ClusterSetReader(clustFile, gdata);
		} catch (Exception e) {
			System.err.println("Can't open cluster file: " + clustFile);
			System.exit(1);
		}
		execute(gdata, cf);
	}
	public static void execute(GenomeData gdata, ClusterSetReader cf) {
//		cf.setMinSpRatio(ConsRatio);
		cf.setMinSpCnt(2);
		cf.setSpGroup(spGroup);
		// 1) read cluster table and extract conserved groups
		ClusterSet clustSetAll = null;
		ClusterSet clustSet = null;
		try {
			clustSetAll = cf.readClusterSet(domclustIn);
		} catch (Exception e) {
			System.err.println("Can't read cluster data: " + clustFile);
			e.printStackTrace();
			System.exit(1);
		}
		if (refsp != null) {
			try {
				clustSetAll.setClusterNamesFromRefSp(refsp);
			} catch (Exception e) {
				System.err.println("refsp "+refsp+" not exist: reset");
				refsp = null;
			}
		}
		// retain only conserved clusters
		clustSet = new ConsClusterFilter(ConsRatio, gdata).filter(clustSetAll);
//System.out.println("size="+ clustSetAll.size()+", "+clustSet.size());
/* FOR DEBUG 
ClusterOutFile cout=null;
try {
	cout = new ClusterOutFile();
} catch (IOException e) {
}
cout.writeClusterSet(clustSet);
*/
/*
System.out.println("cluster num:" + clustSet.size());
*/
		NbrTriplet nbrTrip = new NbrTriplet(clustSet);
		// 2) find conserved neighborhood pairs
		ConsNbrPair consPair = new ConsNbrPair(clustSet, nbrTrip, spGroup);
		if (GapWin > 0) {
			consPair.setGapWin(GapWin);
///			CoreCluster.setGapWin(GapWin);
			CoreGenome.setGapWin(GapWin);
		}
		if (NbrConsRatio > 0.0) {
			consPair.setNbrConsNum_ByRatio(NbrConsRatio);
		}
		if (NbrConsRatio2 > 0.0) {
			consPair.setNbrConsNum2_ByRatio(NbrConsRatio2);
		} else {
			/* same for ConsRatio */
			consPair.setNbrConsNum2_ByRatio(NbrConsRatio);
		}

		consPair.checkNeighbor();
		if (pairoutFile != null) {
			consPair.savePairLink(pairoutFile);
		}
		// 3) determine the direction of the nodes
		ClusterDir gDir =new ClusterDir();
		LinkDir linkDir = new LinkDir(clustSet, consPair.nbrGraph, gDir);
		linkDir.checkLinkDir();
		if (linkoutFile != null) {
			linkDir.saveLinks(linkoutFile);
		}
		// 4) converting the graph into a triplet graph
		TripletGraph tripGraphBuild = new TripletGraph(
					linkDir.newGraph, nbrTrip);
		Graph tripGraph = tripGraphBuild.makeTripletGraph();
		// 5) eliminating loops and making a DAG
		if (tripletoutFile != null) {
			tripGraph.print(tripletoutFile);
		}

		FeedbackSet fsetBuild = new FeedbackSet(tripGraph);
		Set fbkSet = fsetBuild.makeFset();
//System.out.println("fbkSet="+ fbkSet);
		fsetBuild.removeFset(tripGraph);
/*
DFSearch dfs = new DFSearch(tripGraph);
dfs.search();
*/
		// 6) finding the longest paths
		AlignmentPathBuilder aliBuild =
			new AlignmentPathBuilder(clustSet, tripGraph);
		aliBuild.set_MinClustCnt(MinClustCnt);
		AlignmentPath rawCorePath = aliBuild.findMaxPath();
/*
System.out.println("ALIPATH");
rawCorePath.print();
*/

		// 7) restoring the original graph
		DupCidCheck dupCheck = DupCidCheck.getInstance();
		AlignmentPathConverter aliConv =
			new AlignmentPathConverter(rawCorePath, dupCheck, gDir);
		AlignmentPath newCorePath = aliConv.restoreTriplet();
/*
System.out.println("ALIPATH2");
newCorePath.print();
*/

		newCorePath.sortPath(refsp, clustSet, gDir);
/*
System.out.println("ALIPATH2_SORT");
newCorePath.print();
*/

		if (! skip_findbest) {
			// 3) finding the best ortholog among inparalogs in terms of genomic context
			FindBestOrthologs fBest = new FindBestOrthologs(
						clustSet, newCorePath, dupCheck);
			fBest.setOrthoTolerance(orthoTolerance);
			fBest.findBestAll();
		}

/* no longer needed 
		clustSet.orderByAliPath(newCorePath);
*/

		CoreGenome coreGenome = CoreGenome.create(newCorePath, clustSet, gDir, ConsRatio );
		if (coreGenome.blockNum() == 0) {
			System.err.println("Empty core genome");
			System.exit(0);
		}

		/* reorder block */
		coreGenome.makeSpIndex(gdata);
//System.err.println("reorderBlocks");
		coreGenome.reorderBlocks();
//		coreGenome.redefineBlocks(NbrConsRatio, MinClustCnt);
		coreGenome.redefineBlocks(NbrConsRatio, 0);
//System.err.println("concatBlocks");
/*
		coreGenome.concatBlocks();
*/

		CoreGenomeWriter core_out = null;
		try {
			core_out = new CoreGenomeWriter(coreGenome);
		} catch (IOException e) {
			System.err.println("Can't output core genome data");
		}
		core_out.setPosOrtho(flag_posortho);
		core_out.outputText();

		if (execFindIsland) {
			clustSet = clustSetAll;
			coreGenome.assignCoreIdx();
			PanGenomeGraph panG = PanGenomeGraph.create(coreGenome, clustSet, gdata, spGroup);
			panG.outputIsland();
		}
		if (outputAll) {
			// output remaining ortholog groups
			CoreGenomeWriter clout = null;
			try {
				clout = new CoreGenomeWriter();
			} catch (IOException e) {
				System.err.println("Can't output core genome data");
			}
			for (Cluster clust: clustSetAll) {
				if (clust.status == "") {
					clout.outputOrigCluster(clust);
				}
			}
			clout.close();
		}

/*

		ClusterOutFile cout = null;
		try {
			cout = new ClusterOutFile(alignoutFile);
			cout.setPosOrtho(flag_posortho);
			cout.writeClusterSet(clustSet);
		} catch (IOException e) {
			System.err.println("Can't open clustout file");
		}
*/
	}
}
