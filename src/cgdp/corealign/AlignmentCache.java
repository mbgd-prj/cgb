package cgdp.corealign;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cgat.seq.DPAlign;
import cgat.seq.GappedSequence;
import cgat.seq.Sequence;
import cgat.seq.SequenceAlignment;

class AlignmentCache {
	static AlignmentCache instance;

	HashMap<String,GenomicRegion> seqRegHash;
	HashMap<String, SequenceAlignment> alignmentHash;
	int seqnum;
	int addwin = 400;	// flanking sequences added at both sides of the original sequence.
	int shift;	// relative alignment position to show on the saved alignment
	int reglen;	// alignment length to show, i.e., region is (shift, shift+reglen]
	int alilen;	// alignment length to calculate including flanking seq.
	GenomeData gdata;
	String[] seqNames;
	String refsp;
	Sequence refseq;
	DPAlign dp;
	int numThreads;
	int margin = 50;

	static ExecutorService executor;
	static int JobTimeOut = 20;

	AlignmentCache(GenomeData _gdata, int _addwin) {
		gdata = _gdata; addwin=_addwin;
		seqnum = gdata.specList.size();
System.out.println("CACHE_ADDWIN="+_addwin);

		alignmentHash = new HashMap<String,SequenceAlignment>();
		seqRegHash = new HashMap<String,GenomicRegion>();
		seqNames = new String[seqnum];
		dp = new DPAlign();
		instance = this;
	}
	AlignmentCache(GenomeData _gdata, int _addwin, int _numThreads) {
		this(_gdata, _addwin);
		numThreads = _numThreads;
		if (numThreads == 0) {
			int nproc = Runtime.getRuntime().availableProcessors();
			numThreads = (nproc > 2) ? nproc - 1 : nproc;
		}
		if (useThread()) {
			ExecDP.init(this);
		}
	}
	boolean setSeqRegions(ComparativeMapDrawer drawer) {
		boolean update_flag = false;
		int i = 0;

		String newRefsp = drawer.getRefSp();
		if (refsp == null || ! refsp.equals(newRefsp)) {
			refsp = newRefsp;
			update_flag = true;
		}


		for (GenomeMapInfo ginfo: drawer.currGinfoList) {
			String spec = ginfo.getGenome().getSpCode();
			GenomicRegion seqreg = drawer.getSeqReg_in_ViewRegion(ginfo);
			GenomicRegion stored_seqreg = seqRegHash.get(spec);
			reglen = seqreg.length();
/*
System.out.println("spec="+spec+"========");
System.out.println(stored_seqreg);
System.out.println(">>seqreg="+seqreg+" "+reglen);
*/
			if (stored_seqreg == null || ! stored_seqreg.includes(seqreg, margin)) {
				//region to display is not included in the aligned region
System.out.println("##UPDATE: "+spec);
				update_flag = true;
				break;
			} else {
				// seqreg is included in stored_seqreg
				if (seqreg.dir > 0) {
					shift = seqreg.regionDiffBegin(stored_seqreg);
/*
					seqreg.begin0() - stored_seqreg.begin0();
*/
				} else {
					shift = stored_seqreg.regionDiffEnd(seqreg);
/*
					shift = stored_seqreg.end0() - seqreg.end0();
*/
				}
//System.out.println("..shift="+shift);
			}
		}
System.out.println("###REGLEN="+reglen+"; SHIFT="+shift);

/*
		for (GenomeMapInfo ginfo: drawer.currGinfoList) {
			String spec = ginfo.getGenome().getSpCode();
			if (spec.equalserefsp)) {
				GenomicRegion seqreg = drawer.getSeqReg_in_ViewRegion(ginfo, addwin);
			}
		}
*/
//		update_flag = true;
		if (update_flag) {
			reset();
			i = 0;
System.out.println("refsp="+refsp);
			for (GenomeMapInfo ginfo: drawer.currGinfoList) {
				String spec = ginfo.getGenome().getSpCode();
				GenomicRegion seqreg = drawer.getSeqReg_in_ViewRegion(ginfo, addwin);
				seqRegHash.put(spec, seqreg);
				seqNames[i++] = spec;
				alilen = seqreg.length();
System.out.println("########seqreg:"+spec+" "+seqreg+" "+alilen);
				if (spec.equals(refsp)) {
//					refseq = seqreg.getSequence_addflank(gdata, addwin);
					refseq = seqreg.getSequence(gdata);
System.out.println("####REFSEQ="+refseq);
				}
			}
			shift = addwin;
		}
		return(update_flag);
	}
	static AlignmentCache getInstance() {
		if (instance == null) {
			System.err.println("AlignmentCache: No instance");
/*
			instance = new AlignmentCache();
*/
		}
		return(instance);
	}
	/** get reference sequence to show (without flanking sequences) */
	Sequence getRefSeq() {
		if (refseq==null) {
			return null;
		}
		int begin = shift;
		int end = begin + reglen;
//		end = end > alilen ? alilen : end;

		System.out.println("######PPPPP: "+refsp+" "+refseq.length()+", "+begin+"-"+end+" "+reglen+" "+alilen);
		System.out.println("refseq:circular: "+refseq.isCircular());
		// cgat.seq library is 1-based
		return(refseq.createSubSequence(begin+1, end));
	}
	GenomicRegion getRegion(String spec) {
		GenomicRegion seqreg = seqRegHash.get(spec);
		return(seqreg);
	}

	public Sequence getSequence(String spec) {
		GenomicRegion seqreg = seqRegHash.get(spec);
		Sequence seq = seqreg.getSequence(gdata);
		return(seq);
	}
	void reset() {
		seqRegHash.clear();
		alignmentHash.clear();
	}
	SequenceAlignment getAlignment(String spec) throws InterruptedException {
		if ( alignmentHash.containsKey(spec) ) {
			return(alignmentHash.get(spec));
		} else {
//			String spec = seqNames[seqNo];
			GenomicRegion seqreg = seqRegHash.get(spec);
System.out.println("####ALICACHE: "+seqreg+" "+addwin);
//			Sequence seq = seqreg.getSequence_addflank(gdata, addwin);
			Sequence seq = seqreg.getSequence(gdata);
			SequenceAlignment ali = null;
			try {
//System.out.println("##****ALIGN**"+spec);
				if (useThread()) {
					ExecDP execDP = new ExecDP(spec, refseq, seq);
//System.out.println("##****ALIGN_THREAD**"+spec);
					executor.submit(execDP);
//					execDP.start();
//System.out.println("##****OK**"+spec);
				} else {
					ali = dp.align(refseq, seq);
					alignmentHash.put(spec, ali);
					ali.setAlignToRef();
				}
			} catch (InterruptedException e) {
				throw e;
			}
			return(ali);
		}
	}
	boolean useThread() {
		return(numThreads > 0);
	}
	void createExecutor() {
//System.out.println("############################################ NUMTHREADS="+numThreads);
		if (numThreads > 0){
			executor = Executors.newFixedThreadPool(numThreads);
		}
	}
	void waitJobs() throws InterruptedException {
		if (executor != null) {
			executor.shutdown();
			executor.awaitTermination(JobTimeOut, TimeUnit.SECONDS);
		}
	}
	SequenceAlignment getAlignmentFromHash(String spec) {
		if ( alignmentHash.containsKey(spec) ) {
			return(alignmentHash.get(spec));
		} else {
			return(null);
		}
	}
	String getAlignmentSequence(String spec) throws InterruptedException {
		return(getAlignmentSequence(spec, 1));
	}
	/** get Alignment sequence (with gaps) of aliseqno as target */
	String getAlignmentSequence(String spec, int tgt_seqno) throws InterruptedException {
		String aliseq;
		SequenceAlignment ali;
		try {
			ali = getAlignment(spec);
		} catch (InterruptedException e) {
			throw e;
		}
//		aliseq = ali.getAlignedSeqToRef(tgt_seqno);
		GappedSequence gappedSeq = ali.getGappedSequenceSave(tgt_seqno);
		aliseq = gappedSeq.getSeqString();

//System.out.println("AA:"+aliseq);
		int refpos = 0, ref_alipos = 0;
//		int shift_pos = shift + addwin;
		int shift_pos = shift;

		/* create aligned sequence without flanking seq */
		StringBuffer retseq = new StringBuffer();
//System.out.println("SSP0: "+shift_pos+" "+reglen+" "+aliseq.length());
		for (int i = 0; i < aliseq.length(); i++) {
			String ch = aliseq.substring(i,i+1).toUpperCase();
			if (ch.charAt(0) == '/') {
				// insertion; add inschar but skip counting
				retseq.append(ch);
				continue;
			}
			if (ref_alipos < shift_pos) {
				ref_alipos++;
				continue;
			} else if (ref_alipos > shift_pos + reglen) {
				break;
			}
			retseq.append(ch);
			ref_alipos++;
		}
		return( retseq.toString() );
	}
	/** get the bondary positions of a give region on the gapped aligned sequence */
	void getRegionOnAlignment(SeqRegion dispReg, GenomicRegion genomeReg, int chromDir) {
		GenomicLocus beginLoc, endLoc;
		int relpos;
		if(chromDir > 0) {
			beginLoc = genomeReg.beginLocus();
			endLoc = genomeReg.endLocus();
		} else {
			beginLoc = genomeReg.endLocus();
			endLoc = genomeReg.beginLocus();
		}
// System.out.println("REG="+dispReg+"; "+genomeReg+"; "+beginLoc+" "+endLoc);
		relpos = getAlignmentPosition(beginLoc, chromDir, true);
		dispReg.begin += relpos;
		relpos = getAlignmentPosition(endLoc, chromDir, true);
		dispReg.end += relpos;
	}
	int getAlignmentPosition(GenomicLocus loc, int chromDir, boolean relative) {
		String spec = loc.getSpecies();
		/* aligned region */
		GenomicRegion seqreg = seqRegHash.get(spec);
//System.out.println("getAlignmentPos:"+ seqreg+"; "+loc);
		if (! seqreg.contains(loc)) {
			// no adjustment
			return 0;
		}
//		System.out.println("spec=" + spec);
		SequenceAlignment ali = getAlignmentFromHash(spec);
		int tgt_pos;
		if (chromDir > 0) {
			tgt_pos = loc.pos - seqreg.begin0();
		} else {
			tgt_pos = seqreg.end() - loc.pos;
		}
		GappedSequence gapped_seq = ali.getGappedSequenceSave(1);
		int alipos = gapped_seq.getAliPos(tgt_pos);
//System.out.println("####>>alipos="+alipos+" tgt_pos="+tgt_pos);
//System.out.println(ali.getAlignedSeq(1).substring(alipos-10,alipos+10));
//System.out.println(gapped_seq.getSeqString());

		if (relative) {
			alipos -= tgt_pos;
		}
		return(alipos);
	}
	/* for debug */
	void printAlignment(String spec) {
		SequenceAlignment ali = getAlignmentFromHash(spec);
		GappedSequence gapped_seq = ali.getGappedSequenceSave(1);
		GenomicRegion seqreg = seqRegHash.get(spec);
		System.out.println(seqreg.begin());
		int dir = seqreg.dir;
		int tgt_pos = (dir > 0) ? seqreg.begin() : seqreg.end();
		int ref_pos = 0;
		for (int i = 0; i < gapped_seq.length(); i++) {
			char c = gapped_seq.charAt(i);
			System.out.print(c);
			if (! SequenceAlignment.isGap(c)) {
				tgt_pos+=dir;
			}
			if (! SequenceAlignment.isIns(c)) {
				ref_pos++;
			}
			if (i % 10 == 0) {
				System.out.println(" "+ref_pos+" "+tgt_pos);
			}
		}
		System.out.println();
	}
	int getAddwin() {
		return addwin;
	}
}

class ThreadSet {
	CopyOnWriteArrayList<Thread> threads;
	ThreadSet() {
		threads = new CopyOnWriteArrayList<Thread>();
	}
	void add(Thread t) {
		threads.add(t);
	}
	void joinAll() throws InterruptedException {
		if (threads != null) {
			try {
				for (Thread t: threads) {
					t.join();
				}
			} catch (InterruptedException e) {
				throw e;
			}
		}
	}
	void clear() {
		threads.clear();
	}
}
class ExecDP extends Thread {
	DPAlign dp;
	String spec;
	Sequence refseq, seq;
	static AlignmentCache aliCache;
	static ThreadSet threadSet;

	static void init(AlignmentCache _aliCache) {
		aliCache = _aliCache;
/*
		threadSet = new ThreadSet();
*/
	}
	ExecDP(String _spec, Sequence _refseq, Sequence _seq) {
		dp = new DPAlign();
		spec = _spec;
		refseq = _refseq;
		seq = _seq;
	}
	public void run() {
		SequenceAlignment ali = null;
/*
		threadSet.add(this);
*/
		try {
			ali = dp.align(refseq, seq);
		} catch (InterruptedException e) {
			return;
		}
		dp = null;
		aliCache.alignmentHash.put(spec, ali);
		ali.setAlignToRef();
	}
	static void joinAll() throws InterruptedException {
		try {
			threadSet.joinAll();
		} catch (InterruptedException e) {
			throw e;
		}
		threadSet.clear();
	}
}
