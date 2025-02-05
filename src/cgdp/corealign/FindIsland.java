package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;

public class FindIsland {
	static String coreFile, clustFile, genomesFile;
	static SpGroup spGroup;
	static int MaxMinDist;
	static double NbrConsRatio;
	static double NbrConsRatio2;
	static boolean domclustIn = false;

	static void getArgs(String args[]) {
		int fn = 0;
		for (int i = 0; i < args.length; i++) {
			String ag = args[i];
			if (ag.charAt(0) == '-') {
				if (ag.startsWith("SpGrp=", 1)) {
					String spgrpSpec = ag.substring(7);
					spGroup = new SpGroup(spgrpSpec);
				} else if (ag.startsWith("MaxMinDist=", 1)) {
					MaxMinDist = Integer.parseInt(ag.substring(12));
				} else if (ag.startsWith("NbrConsRatio=", 1)) {
					NbrConsRatio = Double.parseDouble(ag.substring(14));
				} else if (ag.startsWith("NbrConsRatio2=", 1)) {
					NbrConsRatio2 = Double.parseDouble(ag.substring(15));
				} else if (ag.startsWith("domclustIn", 1)) {
					domclustIn = true;
				}
			} else {
				switch (fn++) {
				case 0:
					coreFile = args[i];
					break;
				case 1:
					clustFile = args[i];
					break;
				case 2:
					genomesFile = args[i];
					break;
				}

			}
		}

	}
	public static void main(String args[]) {
		CoreGenome coreGenome = null;
		GenomeData genomeData = null;
		ClusterSet clustSet = null;
		getArgs(args);
		if (coreFile == null || clustFile == null || genomesFile == null) {
			System.err.println("Usage: FindIsland coreFile clustFile genomesFile");
			System.exit(1);
		}

		try {
			genomeData = GenomeData.readFromFile(genomesFile, domclustIn);
		} catch (IOException e) {
		}
		try {
			ClusterSetReader clReader = new ClusterSetReader(clustFile, genomeData);
			clReader.setSpGroup(spGroup);
			clReader.setMinSpCnt(2);
			clustSet = clReader.readClusterSet(domclustIn);
		} catch (IOException e) {
		}

		try {
			CoreGenomeReader coreReader = new CoreGenomeReader(coreFile, clustSet, genomeData);
			coreGenome = coreReader.readCoreGenome();
		} catch (IOException e) {
			System.err.println("Warning: core genome file "+coreFile+" read filed (ignored)");
		}



/*
		try {
			CoreGenomeWriter cwriter = new CoreGenomeWriter(coreGenome);
			cwriter.outputText();
		} catch (IOException e) {
		}
*/

System.out.println("core="+coreGenome.totalLength()+",clustset="+clustSet.size());
		PanGenomeGraph panG = PanGenomeGraph.create(coreGenome, clustSet, genomeData, spGroup);
//		panG.checkMobility();
//		panG.outputNonCoreAlign();
		panG.outputIsland();
	} 
}

