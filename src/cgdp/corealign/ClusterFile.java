package cgdp.corealign;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class ClusterSetReader {

	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(ClusterSetReader.class);

	String filename;
	private BufferedReader reader;
/*
	String species[];
*/
	SpeciesList species;
	int specNum;
	int minSpCnt;
	double minSpRatio;
	SpGroup spGroup;
	GenomeData genomeData;
	Pattern pat = Pattern.compile("\\(\\d+\\)");

	public ClusterSetReader(String _filename, GenomeData gdata) throws IOException {
		filename = _filename;
		this.reader = this.newBufferdReader(filename);
		genomeData = gdata;
	}

	public BufferedReader newBufferdReader(final String filename) throws IOException {
		BufferedReader ret = null;
		if (filename == null) {
			/* read from standard input */
			ret = new BufferedReader( new InputStreamReader(System.in) );
		} else if (filename.startsWith("http://")) {
			URL url = new URL(filename);
			ret = new BufferedReader( new InputStreamReader(url.openStream()) );
		} else {
			File infile = new File(filename);
			if (! infile.exists()) {
				System.err.println("file not found: " + filename);
			}
			ret = new BufferedReader( new FileReader(infile) );
		}
		return ret;
	}

	public void setMinSpRatio(double _minSpRatio) {
		minSpRatio = _minSpRatio;
	}
	public void setMinSpCnt(int _minSpCnt) {
		minSpCnt = _minSpCnt;
	}
	public void setSpecies(SpeciesList _species) {
		species = _species;
		specNum = species.spNum();
		if (minSpRatio > 0.0 && minSpCnt==0) {
			minSpCnt = (int) Math.ceil(minSpRatio * specNum);
		}
	}
	public void setSpecies(String _species[]) {
/*
		species = _species;
		specNum = _species.length;
*/
		setSpecies( new SpeciesList(_species) );
	}
	public void setSpGroup(SpGroup spGrp) {
		spGroup = spGrp;
	}
	public ClusterSet readClusterSet(boolean domclustIn) throws IOException {
		if (domclustIn) {
			return readClusterSetFromDomClustOut();
		} else {
			return readClusterSet();
		}
	}
	public ClusterSet readClusterSet() throws IOException {
		logger.debug("readClusterSet");
//		ClusterSet clustSet = ClusterSet.getInstance();
		ClusterSet clustSet = new ClusterSet();
		String linebuf = null;
		String strarray[];
		LinkedList<String> species_list = new LinkedList<String>();;
		int preCol = -1, postCol = 0;
		int nameCol = -1;
		boolean read_spec_flag = false;
		try {
			while ( (linebuf = reader.readLine()) != null) {
				strarray = null;
				if (linebuf.length() == 0) {
					continue;
				} else if (linebuf.charAt(0) == '#') {
					int pos = linebuf.indexOf("SPEC=");
					int bpos = pos + 5;
					int epos;

					String spstr;
					if (pos < 0) {
						pos = linebuf.indexOf("INGROUP=");
						bpos = pos + 8;
					}
					if (preCol < 0) {
						preCol = 3; postCol = 2;
					}
					if ( pos >= 0 ) {
						epos = linebuf.indexOf(' ', bpos);
						if (epos >= 0) {
							spstr = linebuf.substring(bpos, epos);
						} else {
							spstr = linebuf.substring(bpos);
						}

						strarray = spstr.split(",");
						for (int i = 0; i < strarray.length; i++) {
							species_list.add(strarray[i]);
						}
						read_spec_flag = true;
					} else if (species_list.size() == 0 && linebuf.charAt(1) == '\t') {
						strarray = linebuf.split("\t");
						preCol = 1;
						postCol = 0;
						for (int i = preCol; i < strarray.length - postCol; i++) {
							species_list.add(strarray[i]);
						}
						read_spec_flag = true;
					} else {
						pos = linebuf.indexOf("COLSKIP=");
						if (pos >= 0) {
//							int epos = linebuf.indexOf(" ", pos+8);
							preCol = Integer.parseInt( linebuf.substring(pos+8) );
						}
					}
					if (read_spec_flag) {
//						specNum = species.size();
						clustSet.setSpecies(species_list);
						setSpecies(clustSet.species);
						if (spGroup != null) {
						   Cluster.setSpGroupCounter(
						      new SpGroupCounter(
						      spGroup, clustSet.species));
						}
					}

					continue;
				} else if (linebuf.substring(0,7).contentEquals("Cluster")) {
					if (species != null) {
						continue;
					}
					if (preCol < 0) {
						preCol = 1;
					}
					postCol = 0;
					strarray = linebuf.split("\t");
//					for (int i = 2; i < strarray.length - 5; i++) {
					for (int i = preCol; i < strarray.length - postCol; i++) {
						species_list.add(strarray[i]);
					}
					clustSet.setSpecies(species);
					setSpecies(clustSet.species);
					if (spGroup != null) {
						Cluster.setSpGroupCounter(
						    new SpGroupCounter(
						    spGroup, clustSet.species));
					}
					continue;
				}
				linebuf.trim();
//System.out.println("linebuf:"+linebuf.length()+":"+linebuf);
				strarray = linebuf.split("\t");

				String clustid = strarray[0];
				Cluster cluster = new Cluster(specNum, clustid);
				cluster.setSpeciesList(species);

				boolean errflag = readClusterMembers(cluster, strarray, preCol);
//System.out.println(clustid+" "+errflag+" "+cluster.spnum());

				if (errflag) {
				} else if (cluster.spnum() >= minSpCnt) {
					if (nameCol >= 0) {
						cluster.setName(strarray[nameCol]);
					} else if (preCol+specNum < strarray.length) {
						cluster.setName(strarray[preCol+specNum]);
					}
					clustSet.add(cluster);
				} else {
				}


			}
		} catch( IOException e ) {
			throw e;
		}
		clustSet.makeSpIndex(genomeData);
		return clustSet;
	}
	int specNum() {
		return specNum;
	}
	String getSpecies(int i) {
		return species.get(i);
	}
	boolean readClusterMembers(Cluster cluster, String strarray[], int preCol) {
		boolean errflag = false;
		for (int i = 0; i < specNum(); i++) {
			int idx = i + preCol;
			if (idx >= strarray.length) {
				break;
			}
			if (strarray[idx].length()==0) {
				continue;
			}
			String strarray2[] = strarray[idx].split(" ");
			for (int j = 0; j < strarray2.length; j++) {
				String gname = strarray2[j];
				if (gname.indexOf(":") < 0) {
					gname = getSpecies(i)+":"+gname;
				}
//				gname.replaceAll("\\[\\d+\\]", "");
				if (cluster.addMember(
//					    strarray2[j], i,
					    gname, i,
					    genomeData.genes) < 0) {
					errflag = true;
					break;
				}
			}
			if (errflag) break;
		}
		return errflag;
	}
	public ClusterSet readClusterSetFromDomClustOut() throws IOException {
		String linebuf = null;
		String strarray[];
//		ClusterSet clustSet = ClusterSet.getInstance();
		ClusterSet clustSet = new ClusterSet();
		int spidx, spno = 0;
		Cluster cluster = null;
		clustSet.setSpecies(genomeData.specList);
		setSpecies(clustSet.species);
		try {
			while ( (linebuf = reader.readLine()) != null) {
				strarray = linebuf.split("\\s+");
				if (strarray.length == 0) {
				} else if (strarray[0].equals("Cluster")) {
					if (cluster != null) {
						if (cluster.spnum() >= minSpCnt) {
							clustSet.add(cluster);
						} else {
						}
						cluster = null;
					}
					String clustid = strarray[1];
					cluster = new Cluster(specNum, clustid);
				} else if (strarray[0].equals("HomCluster")) {
				} else if ((spidx = strarray[0].indexOf(':')) >= 0)  {
					String gname = strarray[0];
					String spname = gname.substring(0, spidx);
					if (clustSet.species.exists(spname)) {
						spno = clustSet.species.getIdx(spname);
					}
					if (cluster == null) {
						// error !!
					} else if (cluster.addMember( gname, spno, genomeData.genes) < 0) {
						// TODO: spno should be set from spname
						// error !!
//						errflag = true;
					}
				}
			}
		} catch( IOException e ) {
			System.err.println("read error: "+ linebuf);
			throw e;
		}
		if (cluster != null) {
			if (cluster.spnum() >= minSpCnt) {
				clustSet.add(cluster);
			}
			cluster = null;
		}
		clustSet.makeSpIndex(genomeData);
		return clustSet;
	}
	public ClusterSet readClusterSetFromDCSLTFile() throws IOException {
		String linebuf = null;
//		String strarray[];
//		ClusterSet clustSet = ClusterSet.getInstance();
		ClusterSet clustSet = new ClusterSet();
		clustSet.setSpecies(genomeData.specList);
		setSpecies(clustSet.species);
		try {
			while ( (linebuf = reader.readLine()) != null) {
				if (linebuf.charAt(0) == '#') {
					System.out.println(linebuf);
				} else if (linebuf.substring(7).equals("Cluster")) {
				} else if (linebuf.substring(7).equals("HomCluster")) {
				}
			}
		} catch( IOException e ) {
			System.err.println("read error: "+ linebuf);
			throw e;
		}
		return(null);
	}
	void reset() throws IOException {
		reader = newBufferdReader(filename);
	}
}
class ClusterOutFile {
	PrintWriter writer;
	int flag_posortho;
	public ClusterOutFile() throws IOException {
		// output to System.out
		createWriter(null);
	}
	public ClusterOutFile(String filename) throws IOException {
		flag_posortho = 0;
		createWriter(filename);
	}
	public ClusterOutFile(PrintWriter _writer) {
		writer = _writer;
	}
	void createWriter(String filename) throws IOException{
		if (filename == null) {
			/* read from standard input */
			writer = new PrintWriter(System.out);
		} else {
			File outfile = new File(filename);
			try {
				writer = new PrintWriter(new BufferedWriter(
					new FileWriter(outfile) ) );
			} catch (IOException e) {
				throw e;
			}
		}
	}
	public void setPosOrtho(int _flag_posortho) {
		flag_posortho = _flag_posortho;
	}
	public void writeClusterSet(ClusterSet clustSet) {
		Iterator<Cluster> iter = clustSet.iterator();
		Cluster cluster;
		writer.print("ClusterID\tSize");
		for (int i = 0; i < clustSet.specNum(); i++) {
			writer.print("\t"+clustSet.species.get(i));
		}
		writer.println();
		while (iter.hasNext()) {
			cluster = (Cluster) iter.next();
			writeCluster(cluster);
		}
		writer.flush();
	}
	void writeCluster(Cluster cluster) {
		int cluster_size = cluster.size;
		if (flag_posortho == 1) {
			cluster_size = cluster.size_posOrtho();
		}
		writer.print(cluster.id+"\t"+ cluster_size);
		writeClusterMembers(cluster);
		writer.println();
	}
	void writeClusterMembers(Cluster cluster) {
		for (int i = 0; i < cluster.specNum(); i++) {
			LinkedList<DomCluster> mem = cluster.members(i);
			Iterator<DomCluster> iter2 = mem.iterator();
			boolean flag = false;
			writer.print("\t");
			while (iter2.hasNext()) {
				DomCluster gene = (DomCluster) iter2.next();
				if (flag_posortho != 1 || gene.isPosOrtho()) {
					if (flag) {
						writer.print(" ");
					}
					writer.print(gene.dom);
					writer.print("["+gene.order+"]");
					flag = true;
				}
				if (flag_posortho == 2 && ! gene.isPosOrtho() ) {
					writer.print("#");
				}
			}
		}
	}
	void writeBitVector(Cluster cluster) {
		StringBuffer[] bitVect = new StringBuffer[cluster.specNum()];
		for (int i = 0; i < cluster.specNum(); i++) {
			LinkedList<DomCluster> mem = cluster.members(i);
			if (mem.size() > 0) {
				bitVect[i].append("1");
			} else {
				bitVect[i].append("0");
			}
		}
	}
}

