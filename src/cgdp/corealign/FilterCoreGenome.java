package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;

class FilterCoreGenome {

	public static void main(String[] args) {
		String coreFile = null;
		String genomeFile = null;
		GenomeData genomeData = null;
		CoreGenome coreGenome = null;
		boolean domclustIn = true;
		double consRatio = 0.0;
		int fn = 0;

		for (int i = 0; i < args.length; i++) {
			String ag = args[i];
			if (ag.charAt(0) == '-') {
				if (ag.startsWith("ConsRatio=", 1)) {
					consRatio =
					  Double.parseDouble(ag.substring(11));
				}
			} else {
				switch (fn++) {
					case 0:
						coreFile = args[i];
						break;
					case 1:
						genomeFile = args[i];
						break;
				}

			}
		}

                if (coreFile == null || genomeFile == null) {
                        System.err.println("Usage: FilterCoreGenome coreFile genomeFile");
                        System.exit(1);
                }

                try {
                        genomeData = GenomeData.readFromFile(genomeFile, domclustIn);
                } catch (IOException e) {
                        System.err.println("Can't read genome data");
                }
                try {
                        CoreGenomeReader reader = new CoreGenomeReader(coreFile, genomeData);
			reader.setConsRatio(consRatio);
                        coreGenome = reader.readCoreGenome();
                } catch (IOException e) {
                }

		CoreGenomeWriter writer = null;
                try {
			writer = new CoreGenomeWriter(coreGenome);

                } catch (IOException e) {
		} 
		writer.outputText();
	}
}
