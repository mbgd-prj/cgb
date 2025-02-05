package cgdp.corealign;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgat.seq.DPAlign;
import cgat.seq.GappedSequence;
import cgat.seq.Sequence;
import cgat.seq.SequenceAlignment;
import cgdp.corealign.CompareMapOpt.ClusterGroup;
import cgdp.corealign.CompareMapOpt.HitInfo;
import cgdp.util.ColorUtil;
import lombok.Getter;
import lombok.Setter;
import net.arnx.jsonic.JSON;

/*
viewRegion
         |-------|
spname1 --------- --C-- -------
spname2   ------- --C-- ------
spname3        -----C---- ------
 <-----view space------->
        <---concatseq3-->

C: center(selected)pos
*/

/** class for drawing comparative genome map */
public class ComparativeMapDrawer implements Drawer {

	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(ComparativeMapDrawer.class);

	/**
	 * 塩基配列パターンの検索結果リスト。
	 */
	@Getter
	GenomeData genomeData;
	@Getter
	CoreGenome coreGenome;

	@Getter
	private CompGenomeMap compMap;
	Graphics2D g;
	ComparativeMapParams param = ComparativeMapParams.getInstance();
	SeqRegion viewRegion;
	GenomicLocus centerGenePos;
	private boolean colorIslandMode = false;
	boolean limitGenomesInSelectedCluster = false;
	boolean moveRefGenomeToTop = false;
	ComparativeMapViewer mapViewer;

	Rectangle drawGeneRegion;

	int drawWidth, drawHeight;

	// draw region on Y-axis
	Region drawRegSp;

	boolean _DEBUG_FLAG = false;
	boolean nameout = true;
	boolean showScaleBar = true;

	enum GeneDrawMode {
		Seq, Name, Name1, Arrow, Rect, Line
	};

	static int cutoff_seq_def = 1;
	static int cutoff_name_def = 10;
	static int cutoff_name1_def = 50;
	static int cutoff_arrow_def = 100;
	static int cutoff_rect_def = 500;

	Color colorTextSelSp = Color.red;
	Color colorSelGene = Color.cyan;
	Color colorSelGene2 = Color.red;
	Color colorIsland = Color.gray;

	double moveRatio = 0.1;
	double zoomFactor = 2.0;

	int cutoff_seq, cutoff_name, cutoff_name1, cutoff_arrow, cutoff_rect;
	GeneDrawMode geneDrawMode = GeneDrawMode.Line;

	enum GeneColorMode {
		RG, RGB
	};

	GeneColorMode geneColorMode = GeneColorMode.RGB;

	static enum MoveMode {
		WithinChrom, AcrossChrom
	};

	MoveMode moveMode = MoveMode.AcrossChrom;

	@Getter
	AlignmentCache alignCache;
	/**
	 * 印刷モードフラグ。
	 */
	@Setter
	private boolean printMode = false;

	ArrayList<GenomeMapInfo> currGinfoList = null;

	HashMap<String, GappedSequence> alignSeqHash = null;

	void setGeneDrawMode(SeqRegion region) {
		if (region == null) {
			/* display entire region */
			geneDrawMode = GeneDrawMode.Line;
		}
		int width = region.length();
		if (width <= cutoff_seq) {
			geneDrawMode = GeneDrawMode.Seq;
		} else if (width <= cutoff_name) {
			geneDrawMode = GeneDrawMode.Name;
		} else if (width <= cutoff_name1) {
			geneDrawMode = GeneDrawMode.Name1;
		} else if (width <= cutoff_arrow) {
			geneDrawMode = GeneDrawMode.Arrow;
		} else if (width <= cutoff_rect) {
			geneDrawMode = GeneDrawMode.Rect;
		} else {
			geneDrawMode = GeneDrawMode.Line;
		}
	}

	void setColorIslandMode(boolean mode) {
		colorIslandMode = mode;
	}

	void reverseColorIslandMode(boolean check) {
		colorIslandMode = check;
		CompareMapOpt opt = this.mapViewer.getOption();
		if (colorIslandMode) {
			opt.setCoreGradation(false);
			opt.setIslandGradation(true);
		} else {
			opt.setCoreGradation(true);
			opt.setIslandGradation(false);
		}
		opt.setColorIslandMode(colorIslandMode);
	}

	void setMoveMode(boolean withinChromMode) {
		if (withinChromMode) {
			moveMode = MoveMode.WithinChrom;
		}
	}

	void setGeneColorMode(String colmode) {
		if (colmode.equals("RG")) {
			geneColorMode = GeneColorMode.RG;
		}
	}

	Font fontGene, fontGenome;
	//FontMetrics fontMetricsGene, fontMetricsGenome;
	int geneFontSize = 12;
	int genomeFontSize = 15;


	/**
	 * コンストラクタ。
	 * @param cMap 比較マップ。
	 */
	ComparativeMapDrawer(CompareMap cMap) {
		this(cMap.coreGenome, cMap.genomeData, cMap.compMap);
	}

	/**
	 * Islandゲノムを取得する。
	 * <pre>
	 * TODO:後でIslandを配列化する。
	 * islandゲノムを配列化するために、Islandのメンバー変数を削除する。
	 * </pre>
	 * @return Islandゲノム
	 */
	public CoreGenome getIsland() {
		return this.mapViewer.getOption().getCmap().getIsland();
	}

	/**
	 * コンストラクタ。
	 * @param _coreGenome コアゲノム。
	 * @param _gdata ゲノムデータ。
	 * @param _compMap 比較マップ。
	 */
	ComparativeMapDrawer(CoreGenome _coreGenome, GenomeData _gdata, CompGenomeMap _compMap) {
		init(_coreGenome, _gdata, _compMap);
	}

	/**
	 * 初期化処理。
	 * @param _coreGenome コアゲノム。
	 * @param _gdata ゲノムデータ。
	 * @param _compMap 比較ゲノムマップ。
	 */
	private void init(CoreGenome _coreGenome, GenomeData _gdata, CompGenomeMap _compMap) {
		coreGenome = _coreGenome;
		genomeData = _gdata;
		compMap = _compMap;
		fontGene = new Font(Font.SANS_SERIF, Font.PLAIN, geneFontSize);
		fontGenome = new Font(Font.SANS_SERIF, Font.PLAIN, genomeFontSize);
		if (genomeData.seqAvail) {
			alignSeqHash = new HashMap<String, GappedSequence>();
		}
	}

	/**
	 * Viewerにオプション情報を設定する。
	 * @param opt コマンドラインオプション情報。
	 */
	public void setOpt(CompareMapOpt opt) {
		this.init(opt.getCoreGenome(), opt.getGdata(), opt.getCmap().compMap);

		GenomicLocus centerPos = null;
		GenomeData gdata = opt.getGdata();
		CompareMap cmap = opt.getCmap();

		logger.debug("cmap.compMap.viewWidth=" + cmap.compMap.viewWidth);

		this.setCutoff(gdata.isRealChrLen());
		this.setGeneColorMode(opt.getColorMode());
		this.setColorIslandMode(opt.isColorIslandMode());
		this.setMoveMode(opt.isMoveWithinChrom());

		String refsp = opt.getRefsp();
		String centerPosStr = opt.getCenterPosStr();
		if (refsp == null && centerPosStr != null) {
			String[] tmp_split = centerPosStr.split(":");
			refsp = tmp_split[0];
		}
		if (refsp == null) {
			if (opt.getCoreGenome() != null) {
				if (opt.getCoreGenome().species != null)
				refsp = opt.getCoreGenome().species.get(0);
			}
		}
		if (refsp != null) {
			this.setRefSp(refsp);
			opt.setCenterPosStr(refsp + ":1:1");
//			opt.setCenterPosStr(refsp + "::0");
//			opt.setCenterPosStr(refsp + "0");
		}
		/* create initial arrangement in centering mode */
		String centerGene = opt.getCenterGene();
		CoreCluster centerClust = null;
		if (centerGene != null) {
			centerClust = coreGenome.getClusterByGene(centerGene);
			centerPos = gdata.genes.getGene(centerGene).getLocus();
		} else if (centerPosStr != null) {
			logger.debug("***** centerPosStr=" + centerPosStr);

			centerPos = new GenomicLocus(centerPosStr);
//System.out.println("CENT>>"+centerPos+" "+refsp+"<<");
			if (refsp != null) {
				centerPos.fillMissingInfo(refsp, gdata);
				centerClust = coreGenome.getClusterByPos(centerPos, gdata);
			} else {
				// set center at the zero point for each genome
				cmap.compMap.setZeroCenter();
			}
		}
		/* find appropriate chromosome order based on core alignment */
		SetChromOrder setChrOrd = new SetChromOrder(coreGenome, cmap.compMap);
		if (!coreGenome.isBlank()) {
			setChrOrd.setOrder(centerClust);
		}

		if (centerClust != null) {
			cmap.compMap.setCenterByCluster(centerClust.cluster);
		}

		int viewWidth = opt.getViewWidth();
		this.setViewRegion();
//		logger.debug("bbb viewRegion=" + this.viewRegion.begin + "," + this.viewRegion.end);

		logger.debug("centerGene=" + centerGene);
		logger.debug("centerPos=" + centerPos);
		if(centerGene != null || centerPos != null) {
			this.setCenterGenePos(centerPos);
			int centerViewPos = cmap.compMap.getCenterViewPos();
			if (viewWidth > 0) {
				this.setViewRegion(centerViewPos, viewWidth);
			}
		}
		logger.debug("setOpt isBlank=" + opt.getCoreGenome().isBlank());
		if (mapViewer != null) {
			logger.debug("*** locus=" + opt.getLocus() + ", refsp=" + opt.getRefsp());
//			this.mapViewer.getLocusInput().setText(opt.getLocus());
			this.mapViewer.getCB_showLinks().setSelected(opt.isShowLinks());
			this.mapViewer.getCB_skip().setSelected(opt.isSkip());
			this.mapViewer.getCB_reftop().setSelected(opt.isReftop());
			this.mapViewer.getCB_colorMode().setSelected(opt.isColorIslandMode());
			this.mapViewer.getDrawer().reverseColorIslandMode(opt.isColorIslandMode());
//			this.mapViewer.getLocusInput().setText(refsp + ":1");
		}
		this.colorIslandMode = opt.isColorIslandMode();

/*		String centerPosStr = opt.getCenterPosStr();
		if (centerPosStr != null) {
			logger.debug("setCenterPosByStr:" + centerPosStr);
			this.setCenterPosByStr(centerPosStr, true);
		}
*/
	}

	/*
	public void setViewRegion(SeqRegion reg) {
	viewRegion = reg;
	setGeneDrawMode(viewRegion);
	}
	*/
	public void setViewRegion() {
		int viewWidth = getViewWidth();
		logger.debug("drawer.viewWidth=" + viewWidth);
		if (viewWidth > 0) {
			setViewRegion(viewWidth / 2, viewWidth);
		} else {
			this.viewRegion = null;
		}
	}

	/** Set view region by specifying center position (GenomicLocus object) and view width (in bases) */
	public void setViewRegion(GenomicLocus loc, int viewWidth) {
		int viewpos = compMap.getViewPosition(loc);
		setViewRegion(viewpos, viewWidth);
	}

	/** Set view region by specifying center position and view width (in bases) */
	public void setViewRegion(int center, int viewWidth) {
		// 0-base
		viewRegion = new SeqRegion(center - viewWidth / 2, center + viewWidth / 2);
		SeqRegion.zeroBased();
		setGeneDrawMode(viewRegion);
	}

	/** Set view center position (keeping view width unchanged) */
	public void setViewCenterPos(GenomicLocus loc) {
		int viewpos = compMap.getViewPosition(loc);
		if (this.viewRegion != null) {
			viewRegion.setCenterPos(viewpos);
		}
		//System.out.println("VIEWREG:"+viewRegion);
	}

	/** Set view width (keeping center position unchanged) */
	public void setViewWidth(int viewWidth) {
		setViewRegion(getViewCenterPos(), viewWidth);
	}

	public SeqRegion getViewRegion() {
		return (viewRegion);
	}

	/** get center position of the current view region */
	public int getViewCenterPos() {
		if (viewRegion != null) {
			/*
			System.out.println(viewRegion);
			System.out.println( "Cent1: "+ (viewRegion.begin + viewRegion.end) / 2 );
			System.out.println( "Cent2: "+ compMap.getCenterViewPos());
			*/
			return (viewRegion.begin + viewRegion.end) / 2;
		} else {
			return compMap.getCenterViewPos();
		}
	}

	public GenomicLocus getCenterGenePos() {
		return centerGenePos;
	}

	void setCenterGenePos(GenomicLocus loc) {
		centerGenePos = loc;
	}

	public int getViewWidth() {
		if (viewRegion != null) {
			return (viewRegion.length());
		} else {
			// width of entire region
			return (compMap.getViewWidth());
		}
	}

	/** move the center position to left */
	public void moveLeft() {
		moveRegion(-1);
	}

	/** move the center position to right */
	public void moveRight() {
		moveRegion(+1);
	}

	void moveRegion(int dir) {
		moveRegion(dir, moveRatio);
	}

	void moveRegion(int dir, double moveRatio) {
		String refsp = getRefSp();
		GenomeMapInfo ginfo = compMap.getGenomeMap(refsp);
		int viewWidth = getViewWidth();
		GenomicLocus centerPos = getCenterGenePos();
		logger.debug("centerPos=" + centerPos);
		centerPos.fillMissingInfo(refsp, genomeData);
		dir *= ginfo.getChromDir(centerPos.getSeqNo_0base());
		int moveWidth = (int) (viewWidth * moveRatio) * dir;
		if (moveMode == MoveMode.WithinChrom) {
			// move center pos
			centerPos.pos = centerPos.pos + moveWidth;
			// correct locus
			centerPos = genomeData.correctLocus(centerPos);
		} else {
			centerPos = ginfo.movePositionOnConcatSeq(centerPos, moveWidth);
		}
		setCenterPos(centerPos);
		setViewRegion(centerPos, viewWidth);
	}

	public void zoomIn() {
		zoomRegion(1.0 / zoomFactor);
	}

	public void zoomOut() {
		zoomRegion(zoomFactor);
	}

	void zoomRegion(double factor) {
		int viewWidth = 0;
		int entireViewWidth = compMap.getViewWidth();
		int minWidth = 40;
		if (viewRegion == null) {
			viewWidth = entireViewWidth;
		} else {
			viewWidth = viewRegion.length();
		}
		viewWidth *= factor;
		if (viewWidth >= entireViewWidth) {
			viewWidth = entireViewWidth;
			viewRegion = null;
		} else if (viewWidth <= minWidth) {
		} else {
			setViewWidth(viewWidth);
		}
	}

	public void setGraphics(Graphics2D _g) {
		g = _g;
	}

	public void setCutoff(boolean isRealChrLen) {
		int factor = 1;
		if (isRealChrLen) {
			factor = 1000;
		}
		cutoff_seq = cutoff_seq_def * factor;
		cutoff_name = cutoff_name_def * factor;
		cutoff_name1 = cutoff_name1_def * factor;
		cutoff_arrow = cutoff_arrow_def * factor;
		cutoff_rect = cutoff_rect_def * factor;
	}

	ArrayList<GenomeMapInfo> getCurrGinfoList() {
		ArrayList<GenomeMapInfo> currGinfoList = null;
		if (limitGenomesInSelectedCluster) {
			/*
			Cluster selCluster = compMap.getSelectedCluster();
			if (selCluster != null) {
				Set spSet = selCluster.spSet();
				return(compMap.getGenomeMapInfoList(spSet));
			}
			*/
			HashSet<Integer> selectedSpSet = new HashSet<Integer>();
			for (Cluster selClst : compMap.getSelectedClusters()) {
				selectedSpSet.addAll(selClst.spSet());
			}
			currGinfoList = compMap.getGenomeMapInfoList(selectedSpSet);
		} else {
			currGinfoList = compMap.getGenomeMapInfoList();
		}
		if (moveRefGenomeToTop) {
			String refsp = getRefSp();
			LinkedList<GenomeMapInfo> newCurrGinfoList = new LinkedList<GenomeMapInfo>();
			GenomeMapInfo ref_ginfo = null;
			for (GenomeMapInfo ginfo : currGinfoList) {
				if (ginfo.genome.getSpCode().equals(refsp)) {
					ref_ginfo = ginfo;
				} else {
					newCurrGinfoList.add(ginfo);
				}
			}
			newCurrGinfoList.addFirst(ref_ginfo);
			currGinfoList = new ArrayList<GenomeMapInfo>(newCurrGinfoList);
		}
		return (currGinfoList);
	}

	/** The main function to draw data on the comparative mapping viewer */
	public void drawData() {
		if (coreGenome.isBlank()) {
			return;
		}

		/*
		ArrayList<GenomeMapInfo> currGinfoList = compMap.getGenomeMapInfoList();
		*/
		currGinfoList = getCurrGinfoList();
		int spNo = 0;
		String refsp = coreGenome.getRefSp();
//		int refspNo = coreGenome.getRefSpNo();

		// draw chromosome lines
		g.setFont(fontGenome);
//		int maxnamelen = 0, namelen;
		/*
		for (GenomeMapInfo ginfo: currGinfoList){
		Genome genome = ginfo.getGenome();
		namelen = g.getFontMetrics().stringWidth(genome.getName()+"/- ");
		if (maxnamelen < namelen) {
			maxnamelen = namelen;
		}
		}
		param.SPNAME_WIDTH = maxnamelen;
		*/

		drawGeneRegion = new Rectangle(
				param.LEFT_MARGIN + param.SPNAME_WIDTH, 0,
				drawWidth - param.LEFT_MARGIN * 2 - param.SPNAME_WIDTH,
				drawHeight);

		for (GenomeMapInfo ginfo : currGinfoList) {
			Genome genome = ginfo.getGenome();
			//int pos = 1;
			int ypos = get_ypos(spNo);

			if (refsp != null && refsp.equals(genome.getSpCode())) {
				g.setColor(colorTextSelSp);
			} else {
				g.setColor(Color.black);
			}

			String genomeName = genome.getName();

			if (ginfo.getChromDir() < 0) {
				genomeName = genomeName + "/-";
			}
			g.setFont(fontGenome);
			g.drawString(genomeName, param.LEFT_MARGIN, ypos);
			spNo++;
		}

		g.setClip(drawGeneRegion);

		if (showScaleBar) {
			drawScaleBar();
		}

		// 印刷時には全ての生物種を出力するように修正。
		if (this.printMode) {
			drawRegSp.begin = 0;
			drawRegSp.end = this.mapViewer.getOption().getGdata().specNum() - 1;
		} else {
			drawRegSp = getDrawRegion();
		}

		spNo = 0;
		for (GenomeMapInfo ginfo : currGinfoList) {
			int ypos = get_ypos(spNo);
			g.setColor(Color.black);
			SeqRegion[] chrViewPos = ginfo.getChromosomeViewPositions();
			//System.out.println("draw: "+ginfo.getGenome().getName());

			// draw chromosomes as lines
			if (geneDrawMode.ordinal() > GeneDrawMode.Seq.ordinal()) {
				for (int i = 0; i < chrViewPos.length; i++) {
					// chromosome segment within viewRegion
					SeqRegion dispChrReg = getDisplayRegion(chrViewPos[i], true);
					if (dispChrReg == null) {
						continue;
					}
					int xpos1 = get_xpos(dispChrReg.begin);
					int xpos2 = get_xpos(dispChrReg.end);

					/*
						SeqRegion dispChrRegNoClip = getDisplayRegion(chrViewPos[i], false);
						SeqRegionReal subRegRatio = dispChrRegNoClip.getSeqRegionRatio(dispChrReg);
						Chromosome chrom = ginfo.getChromosomeByViewOrder(i);
						SeqRegion subRegion = chrom.asSeqRegion().getSubSeqRegion(subRegRatio);
						System.out.println("##SUB: "+ginfo.getGenome().getName()+" "+subRegion);
					*/
					g.drawLine(xpos1, ypos, xpos2, ypos);
				}
				spNo++;
			}
		}
		if (shouldCalcAlign()) {
			// calculate sequence alignment and set gapped seq in alignedSeqList
			alignSequence();
		}
		/*
		CoreGenomeIterator iter = coreGenome.iterator();
		*/

/*		List<Gene> leftoversList = this.mapViewer.getOption().getLeftoversList();
		if (this.mapViewer.getOption().isLeftoversView() && leftoversList != null) {
			boolean othersOradation = this.mapViewer.getOption().isLeftoversGradation();
			Color othersColor = this.mapViewer.getOption().getLeftoversColor();
			this.drawLeftovers(leftoversList, othersOradation, othersColor);
		}
*/

		Map<String, List<Gene>> leftoversMap = this.mapViewer.getOption().getLeftoversMap();
		if (this.mapViewer.getOption().isLeftoversView() && leftoversMap != null) {
			boolean leftoversOradation = this.mapViewer.getOption().isLeftoversGradation();
			Color leftoversColor = this.mapViewer.getOption().getLeftoversColor();
			this.drawLeftovers(leftoversMap, leftoversOradation, leftoversColor);
		}

		// draw genes for each cluster
		if (this.mapViewer.getOption().isViewCore()) {
			this.drawGenome(this.coreGenome, this.coreGenome.isGradation(), this.coreGenome.getColor());
		}

		// Islandの描画
		if (this.mapViewer.getOption().isViewIsland() && this.getIsland() != null) {
			this.drawGenome(this.getIsland(), this.getIsland().isGradation(), this.getIsland().getColor());
		}
		// Otherゲノムの描画
		List<CoreGenome> otherList = this.mapViewer.getOption().getCmap().getOtherList();
		if (otherList != null) {
			for (CoreGenome other: otherList) {
				if (other.isVisible()) {
					this.drawGenome(other, other.isGradation(), other.getColor());
				}
			}
		}

		for (ClusterGroup cg: this.mapViewer.getOption().getClusterGroupList()) {
			this.drawClusterGroup(cg);
		}

		// draw selected Cluster
		/*
		Cluster selCluster = compMap.getSelectedCluster();
		if (selCluster != null) {
		g.setStroke(new BasicStroke(3));
		drawGenesInCluster(selCluster, colorSelGene, 1);
		}
		*/
		g.setStroke(new BasicStroke(3));
		for (Cluster selCluster : compMap.getSelectedClusters()) {
			drawGenesInCluster(selCluster, colorSelGene, 1);
		}
		// 検索結果の表示処理。
		this.drawHitResult();

		///draw sequence
		//System.out.println("mode=>"+geneDrawMode);
		if (shouldCalcAlign()) {
			drawSequence();
		}
	}

	/**
	 * 印刷中フラグ。
	 */
	private boolean printing = false;

	/**
	 * 印刷中の場合ページ内の生物種かどうかを判定する。
	 * @param spNo 生物種No。
	 * @return ページ内の生物種の場合true。
	 */
	private boolean inPage(int spNo) {
		if (this.printing) {
			return (this.spOffset <= spNo && spNo < this.spOffset + 24);
		} else {
			return true;
		}
	}

	/** The main function to draw data on the comparative mapping viewer */
	public void printData(int pageIndex) {
		this.printing = true;
		this.spOffset = (pageIndex * 24);
		try {
			currGinfoList = getCurrGinfoList();
			int spNo = 0;
			String refsp = coreGenome.getRefSp();
			g.setFont(fontGenome);

			drawGeneRegion = new Rectangle(
					param.LEFT_MARGIN + param.SPNAME_WIDTH, 0,
					drawWidth - param.LEFT_MARGIN * 2 - param.SPNAME_WIDTH,
					drawHeight);

			for (GenomeMapInfo ginfo : currGinfoList) {
				Genome genome = ginfo.getGenome();
				//int pos = 1;
				if (inPage(spNo)) {
					int ypos = get_ypos(spNo);

					if (refsp != null && refsp.equals(genome.getSpCode())) {
						g.setColor(colorTextSelSp);
					} else {
						g.setColor(Color.black);
					}

					String genomeName = genome.getName();

					if (ginfo.getChromDir() < 0) {
						genomeName = genomeName + "/-";
					}
					g.setFont(fontGenome);
					g.drawString(genomeName, param.LEFT_MARGIN, ypos);
				}
				spNo++;
			}

			g.setClip(drawGeneRegion);

			if (showScaleBar) {
				drawScaleBar();
			}

			int lastSp = this.spOffset + 23;
			if (lastSp >= currGinfoList.size()) {
				lastSp = currGinfoList.size() - 1;
			}
			this.drawRegSp = new Region(this.spOffset, lastSp);
			spNo = 0;
			for (GenomeMapInfo ginfo : currGinfoList) {
				if (inPage(spNo)) {
					int ypos = get_ypos(spNo);
					g.setColor(Color.black);
					SeqRegion[] chrViewPos = ginfo.getChromosomeViewPositions();
					if (geneDrawMode.ordinal() > GeneDrawMode.Seq.ordinal()) {
						for (int i = 0; i < chrViewPos.length; i++) {
							// chromosome segment within viewRegion
							SeqRegion dispChrReg = getDisplayRegion(chrViewPos[i], true);
							if (dispChrReg == null) {
								continue;
							}
							int xpos1 = get_xpos(dispChrReg.begin);
							int xpos2 = get_xpos(dispChrReg.end);

							g.drawLine(xpos1, ypos, xpos2, ypos);
						}
						spNo++;
					}
				}
			}
			if (shouldCalcAlign()) {
				// calculate sequence alignment and set gapped seq in alignedSeqList
				alignSequence();
			}

			Map<String, List<Gene>> leftoversMap = this.mapViewer.getOption().getLeftoversMap();
			if (this.mapViewer.getOption().isLeftoversView() && leftoversMap != null) {
				boolean leftoversOradation = this.mapViewer.getOption().isLeftoversGradation();
				Color leftoversColor = this.mapViewer.getOption().getLeftoversColor();
				this.drawLeftovers(leftoversMap, leftoversOradation, leftoversColor);
			}
			// draw genes for each cluster
			if (this.mapViewer.getOption().isViewCore()) {
				this.drawGenome(this.coreGenome, this.coreGenome.isGradation(), this.coreGenome.getColor());
			}

			if (this.mapViewer.getOption().isViewIsland() && this.getIsland() != null) {
				this.drawGenome(this.getIsland(), this.getIsland().isGradation(), this.getIsland().getColor());
			}

			// Otherゲノムの描画
			List<CoreGenome> otherList = this.mapViewer.getOption().getCmap().getOtherList();
			if (otherList != null) {
				for (CoreGenome other: otherList) {
					if (other.isVisible()) {
						this.drawGenome(other, other.isGradation(), other.getColor());
					}
				}
			}

			for (ClusterGroup cg: this.mapViewer.getOption().getClusterGroupList()) {
				this.drawClusterGroup(cg);
			}

			g.setStroke(new BasicStroke(3));
			for (Cluster selCluster : compMap.getSelectedClusters()) {
				drawGenesInCluster(selCluster, colorSelGene, 1);
			}
			// 検索結果の表示処理。
			this.drawHitResult();

			///draw sequence
			//System.out.println("mode=>"+geneDrawMode);
			if (shouldCalcAlign()) {
				drawSequence();
			}
		} finally {
			this.printing = false;
			this.spOffset = 0;
		}
	}



	private Gene getGeneFromHitInfo(HitInfo hit) {
		String sp = hit.getChrName();
		float pos = (float)(hit.getStart() + hit.getEnd()) / 2;
		int len = Math.abs(hit.getEnd() - hit.getStart()) / 3;
		Gene gene = new Gene(sp, sp, hit.getSeqNo(), pos, len, hit.getDir());
		return gene;
	}

	/**
	 * 検索結果の表示。
	 */
	private void drawHitResult() {
		List<HitInfo> hitList = this.mapViewer.getOption().getHitList();
		if (hitList != null) {

			this.g.setColor(Color.BLACK);
			int hh = 10;
			String pat = this.mapViewer.getOption().getSequencePattern();
			if (pat != null) {
				int len = this.mapViewer.getOption().getSequencePattern().length();
				for (int i = 0; i < hitList.size(); i++) {
					HitInfo hitInfo = hitList.get(i);

					GenomeMapInfo ginfo = compMap.getGenomeMap(hitInfo.getChrName());
					int dir = ginfo.getChromDir();

					GenomicLocus fromgl = new GenomicLocus(hitInfo.getLocusFrom());
					GenomicLocus togl = new GenomicLocus(hitInfo.getLocusTo());
					Point from = this.genomicLocus2Coordinate(fromgl);
					Point to = this.genomicLocus2Coordinate(togl);

					boolean draw = true;
					if (this.shouldCalcAlign()) {
						// 表示位置の補正処理
						Gene gene = this.getGeneFromHitInfo(hitInfo);
						SeqRegion r = ginfo.getGeneViewRegion(gene);
						SeqRegion seqreg = getDisplayRegion(new SeqRegion(r.getBegin() + 1, r.getEnd() - 1));
						SeqRegion dispReg = seqreg;
						// 表示範囲外の生物種のキャッシュはない?
						if (alignCache.alignmentHash.containsKey(hitInfo.getChrName()) && dispReg != null) {
							alignCache.getRegionOnAlignment(dispReg, gene.getRegion(), dir);
							from.x = dispReg.begin();
							to.x = dispReg.end();
						} else {
							draw = false;
						}
					}
					if (draw) {
						int x0 = this.get_xpos(from.x);
						int x1 = this.get_xpos(to.x);
						int y0 = this.get_ypos(from.y);

						if (y0 < param.TOP_MARGIN + param.SCALEBAR_HEIGHT) {
							continue;
						}

						int w = x1 - x0;
						int dx = (w / len) / 4;
						x0 -= dx;
						x1 -= dx * 2;
						BasicStroke bs = new BasicStroke(2);
						this.g.setStroke(bs);
						if (hitInfo.getDir() < 0) {
							int[] x = {	x0     , x0 - dx, x0     , x1     , x1     };
							int[] y = {	y0 - hh, y0     , y0 + hh, y0 + hh, y0 - hh};
							this.g.	drawPolygon(x, y, x.length);
						} else {
							int[] x = {	x0     ,  x0     , x1     , x1 + dx, x1     };
							int[] y = {	y0 - hh,  y0 + hh, y0 + hh, y0     , y0 - hh};
							this.g.	drawPolygon(x, y, x.length);
						}
					}
				}
			}
		}
	}

	/**
	 * CoreGenomeを描画する。
	 * @param genome CoreGenome。
	 * @param gradation グラデーションフラグ。
	 * @param color 色。
	 */
	private void drawGenome(final CoreGenome genome, final boolean gradation, final Color color) {
		String refsp = genome.getRefSp();
		int refspNo = genome.getRefSpNo();

		int clst_cnt = 1;
		Color lineCol = Color.black;
		for (CoreCluster cclust : genome) {
			if (geneDrawMode.ordinal() > GeneDrawMode.Rect.ordinal() &&
					(cclust.isCore() &&
							cclust.cluster.spConsRatio() < param.ConsRatio)) {
				clst_cnt++;
				continue;
			}
			if (!gradation) {
				lineCol = color;
			} /*else 	if ((!colorIslandMode && cclust.isIsland()) ||
					(colorIslandMode && cclust.isCore())) {
				lineCol = colorIsland;
			}*/ else if (refsp == null) {
				// refsp is not defined
				int totlen = coreGenome.totalLength();
				double clustpos = ((double) (clst_cnt - 1)) / (totlen - 1);
				//	g.setColor( getColorByPos(clustpos) );
				lineCol = getColorByPos(clustpos);
			} else {
				LinkedList<DomCluster> dlist = cclust.members(refspNo);
				if (dlist != null && dlist.size() > 0) {
					DomCluster dcl = dlist.getFirst();
					Gene gene = dcl.dom.gene;
					GenomeMapInfo ginfo_refsp = compMap.getGenomeMap(refsp);
					int totlen = ginfo_refsp.getTotalLength();

					double refpos = (double) ginfo_refsp.getOrigGenePosition(gene) / totlen;
					//		g.setColor( getColorByPos(refpos) );
					lineCol = getColorByPos(refpos);
				}
			}
			clst_cnt++;
			drawGenesInCluster(cclust.cluster, lineCol, 0);
		}
	}

	/**
	 * クラスターグループの描画。
	 * @param cg クラスターグループ。
	 */
	private void drawClusterGroup(final ClusterGroup cg) {
		if (!cg.isVisible()) {
			return;
		}
		Color lineCol = ColorUtil.getColor(cg.getColorCode());
		int viewWidth = getViewWidth();
		int extraWidth = (int) ((double) viewWidth * 0.2);
		int minWidth = 2000;
		LinkedList<Integer> prevPosList = null;
		for (int spNo = drawRegSp.begin; spNo <= drawRegSp.end; spNo++) {
			GenomeMapInfo ginfo = currGinfoList.get(spNo);
			String sp = ginfo.genome.spcode;
			ClusterSet cset = cg.getClusterSet();
			LinkedList<Integer> next_prevPosList = new LinkedList<Integer>();
			for (Cluster c: cset.clusterList) {
				for (LinkedList<DomCluster> list: c.members) {
					for (DomCluster dc: list) {
						Gene gene = dc.dom.gene;
						if (sp.equals(gene.sp)) {
							int pos = getDisplayPos(ginfo.getGeneViewPosition(gene));
							extraWidth = (extraWidth >= minWidth) ? extraWidth : minWidth;
							if (pos < -extraWidth || pos > viewWidth + extraWidth) {
								;
							} else {
								int xpos1 = get_xpos(pos);
								int ypos1 = get_ypos(spNo);
								drawGene(ginfo, gene, xpos1, ypos1, lineCol, 0);
								if (param.drawLinks && prevPosList != null) {
									for (int prev_pos : prevPosList) {
										int xpos2 = get_xpos(prev_pos);
										int ypos2 = get_ypos(spNo - 1);
										g.drawLine(xpos1, ypos1 - param.GENE_HEIGHT, xpos2, ypos2 + param.GENE_HEIGHT);
									}
								}
								next_prevPosList.add(pos);
							}
						}
					}
				}
			}
			prevPosList = next_prevPosList;
		}
	}

	/**
	 * Leftovers geneの表示処理。
	 * @param leftoversMap 残りのgeneのリスト。
	 * @param gradation グラデーションフラグ。
	 * @param geneCol 残りのgeneの色。
	 */
	private void drawLeftovers(final Map<String, List<Gene>> leftoversMap, final boolean gradation, final Color color) {
		long t0 = (new Date()).getTime();
		int viewWidth = getViewWidth();
		int extraWidth = (int) ((double) viewWidth * 0.2);
		int minWidth = 2000;
		for (int spNo = drawRegSp.begin; spNo <= drawRegSp.end; spNo++) {
			GenomeMapInfo ginfo = currGinfoList.get(spNo);
			String sp = ginfo.genome.spcode;
			List<Gene> leftoversList = leftoversMap.get(sp);
			if (leftoversList == null) {
				continue;
			}
			logger.info("sp=" + sp + ", leftoversList.size()=" + leftoversList.size());
			long tt0 = (new Date()).getTime();
			int clst_cnt = 1;
			for (Gene gene: leftoversList) {
				clst_cnt++;
				int p0 = ginfo.getGeneViewPosition(gene); // これが遅い
				int pos = getDisplayPos(p0);
				extraWidth = (extraWidth >= minWidth) ? extraWidth : minWidth;
				if (pos < -extraWidth || pos > viewWidth + extraWidth) {
					continue;
				}
				Color lineCol = color;
				if (gradation) {
					int totlen = coreGenome.totalLength();
					double clustpos = ((double) (clst_cnt - 1)) / (totlen - 1);
					//	g.setColor( getColorByPos(clustpos) );
					lineCol = getColorByPos(clustpos);
				}
				int xpos1 = get_xpos(pos);
				int ypos1 = get_ypos(spNo);
				drawGene(ginfo, gene, xpos1, ypos1, lineCol, 0);
			}
			long tt1 = (new Date()).getTime();
			logger.info("sptime=" + (tt1 - tt0));
		}
		long t1 = (new Date()).getTime();
		logger.info("time=" + (t1 - t0));
	}


	boolean shouldCalcAlign() {
		return (genomeData.seqAvail && geneDrawMode == GeneDrawMode.Seq);
	}

	/*
	double calcRefPos(Gene gene, refsp) {
	GenomeMapInfo ginfo_refsp = compMap.getGenomeMap(refsp);
	int totlen = ginfo_refsp.getTotalLength();
	double refpos = (double) ginfo_refsp.getOrigGenePosition(gene) / totlen;
	}
	*/
	public void setRefSp(String refsp) {
		coreGenome.setRefSp(refsp);
		compMap.setCenterDirectionRefSp(refsp);
		this.mapViewer.getOption().setRefsp(refsp);
	}

	public String getRefSp() {
		return coreGenome.getRefSp();
	}

	void drawScaleBar() {
		//int centerViewPos = compMap.getCenterViewPos();
		int centerViewPos = getViewCenterPos();
		SeqRegion dispScaleBar = new SeqRegion(1, compMap.getViewWidth());
		dispScaleBar = getDisplayRegion(dispScaleBar, true);
		int xpos1 = get_xpos(dispScaleBar.begin);
		int xpos2 = get_xpos(dispScaleBar.end);
		int ypos = param.TOP_MARGIN + param.SCALEBAR_HEIGHT / 2;
		g.drawLine(xpos1, ypos, xpos2, ypos);
		//int centpos = get_xpos(centerViewPos);
		SeqRegion seqReg = getViewRegion();
		int viewWidth = getViewWidth();
		String refsp = getRefSp();
		GenomeMapInfo ginfo_refsp = compMap.getGenomeMap(refsp);

		double exp = Math.log(viewWidth) / Math.log(10);
		int ticksize = (int) Math.pow(10, Math.floor(exp));
		if ((double) viewWidth / ticksize < 1.2) {
			ticksize = ticksize / 10;
		} else if ((double) viewWidth / ticksize < 2.5) {
			ticksize = ticksize / 5;
		} else if ((double) viewWidth / ticksize < 5) {
			ticksize = ticksize / 2;
		}
		int xpos = get_xpos(centerViewPos);
		g.drawLine(xpos, ypos - param.SCALEBAR_TICK_HEIGHT, xpos, ypos + param.SCALEBAR_TICK_HEIGHT);

		final int[] directions = { 1, -1 };
		g.setColor(Color.black);
		class TickInfo {
			int tickPos;
			String tickStr;
		}
		ArrayList<TickInfo> tickList = new ArrayList<TickInfo>();

		int tickBasePos, tickBasePosCoord;
		boolean absoluteScaleMode = true;

		int centerPos = centerViewPos;

		/*
		if (seqReg != null) {
		centerPos = (int) ((seqReg.begin + seqReg.end) / 2);
		}
		*/
		if (absoluteScaleMode) {
			GenomicLocus loc = ginfo_refsp.getLocus_from_ViewPos(centerPos);
			tickBasePosCoord = loc.pos / ticksize * ticksize;
			tickBasePos = centerPos - loc.pos + tickBasePosCoord;
		} else {
			tickBasePos = centerPos;
			tickBasePosCoord = tickBasePos;
		}
		for (int dir : directions) {
//			int beginPos;
			int tickPos = tickBasePos;
//			int regBoundBegin, regBoundEnd;
			//for (int tick = 0; tickPos >= regBoundBegin
			//	    && tickPos <= regBoundEnd; tick += ticksize) {
			for (int tick = 0; tick < viewWidth; tick += ticksize) {
				tickPos = tickBasePos + tick * dir;
				//System.out.println("tick="+tick+" tickPos"+tickPos+" centerPos="+centerViewPos+" viewWidth="+viewWidth+" seqReg="+seqReg);
				if (seqReg != null) {
					if (tickPos < seqReg.begin || tickPos > seqReg.end) {
						break;
					}
				} else if (tickPos < 0 || tickPos > viewWidth) {
					break;
				}
				if (tick == 0 && dir < 0)
					continue;

				TickInfo tickInfo = new TickInfo();
				tickInfo.tickPos = tickPos;
				if (absoluteScaleMode) {
					tickInfo.tickStr = String.valueOf(tickBasePosCoord + tick * dir);
				} else {
					tickInfo.tickStr = String.valueOf(tick * dir);
				}
				tickList.add(tickInfo);
			}
		}

		for (TickInfo tickInfo : tickList) {
			xpos = get_xpos(getDisplayPos(tickInfo.tickPos - 1)); // TO BE CHECKED!!! (-1 seems to be required)
			g.drawLine(xpos, ypos - param.SCALEBAR_TICK_HEIGHT, xpos, ypos + param.SCALEBAR_TICK_HEIGHT);
			//String tickStr = String.valueOf(tick*dir);
			int tickStrLen = g.getFontMetrics().stringWidth(tickInfo.tickStr);
			g.drawString(tickInfo.tickStr, xpos - tickStrLen / 2, ypos - param.SCALEBAR_TICK_HEIGHT);
		}
	}

	/** draw all genes in the specified ortholog cluster */
	void drawGenesInCluster(Cluster cluster, Color geneCol, int flag) {
		/*
		ArrayList<GenomeMapInfo> currGinfoList = compMap.getGenomeMapInfoList();
		*/
		if (geneDrawMode == GeneDrawMode.Seq && genomeData.seqAvail) {
			geneCol = Color.lightGray;
		}
		if (currGinfoList == null) {
			currGinfoList = getCurrGinfoList();
		}
		LinkedList<Integer> prevPosList = null;
		int spNo = 0;
		int viewWidth = getViewWidth();
		int extraWidth = (int) ((double) viewWidth * 0.2);
		int minWidth = 2000;

		// 生物種のループ。
		for (spNo = drawRegSp.begin; spNo <= drawRegSp.end; spNo++) {
			GenomeMapInfo ginfo = currGinfoList.get(spNo);

			int orig_spNo = ginfo.origOrder;
			LinkedList<DomCluster> dlist = cluster.members[orig_spNo];
			LinkedList<Integer> next_prevPosList = new LinkedList<Integer>();
			for (DomCluster dcl : dlist) {
				Gene gene = dcl.dom.gene;


				//	SeqRegion seqreg = getDisplayRegion( ginfo.getGeneViewRegion(gene) );
				//	if (seqreg == null) return;

				int pos = getDisplayPos(ginfo.getGeneViewPosition(gene));
				extraWidth = (extraWidth >= minWidth) ? extraWidth : minWidth;
				if (pos < -extraWidth || pos > viewWidth + extraWidth)
					continue;

				if (_DEBUG_FLAG) {
					System.out.println(gene + "   " + pos);
				}
				int xpos1 = get_xpos(pos);
				int ypos1 = get_ypos(spNo);

				drawGene(ginfo, gene, xpos1, ypos1, geneCol, flag);

				if (param.drawLinks && prevPosList != null) {
					for (int prev_pos : prevPosList) {
						int xpos2 = get_xpos(prev_pos);
						int ypos2 = get_ypos(spNo - 1);
						g.drawLine(xpos1, ypos1 - param.GENE_HEIGHT, xpos2, ypos2 + param.GENE_HEIGHT);
					}
				}
				next_prevPosList.add(pos);
			}
			prevPosList = next_prevPosList;
		}
	}

	void drawGene(GenomeMapInfo ginfo, Gene gene, int spNo, Color geneCol, int flag) {
		SeqRegion seqreg = getDisplayRegion(ginfo.getGeneViewRegion(gene));
		if (seqreg == null)
			return;

		int xpos1 = get_xpos(seqreg.begin0());
//		int xpos2 = get_xpos(seqreg.end0());
		int ypos1 = get_ypos(spNo);

		drawGene(ginfo, gene, xpos1, ypos1, geneCol, flag);
	}

	/** draw individual gene */
	void drawGene(GenomeMapInfo ginfo, Gene gene, int xpos1, int ypos1, Color geneCol, int flag) {

		int geneDir = gene.getDir() * ginfo.getChromDir();

		if (nameout &&
				(geneDrawMode.ordinal() <= GeneDrawMode.Name.ordinal() ||
						(geneDrawMode == GeneDrawMode.Name1 && flag == 1))) {
			g.setFont(fontGene);
			g.setColor(Color.black);
			int namelen = g.getFontMetrics().stringWidth(gene.getName());
			g.drawString(gene.getName(),
					xpos1 - namelen / 2, ypos1 - param.GENE_HEIGHT);
		}
		g.setColor(geneCol);
		if (genomeData.isRealChrLen()) {
			SeqRegion seqreg = getDisplayRegion(ginfo.getGeneViewRegion(gene));

			//SeqRegion seqreg  = ginfo.getGeneViewRegion(gene);
			if (gene.getName().equals("H779_YJM993P00474")) {
				System.out.println("viewreg=" + ginfo.getGeneViewRegion(gene));
				System.out.println("####GGG>" + gene + " " + gene.getBegin0() + " " + gene.getEnd() + " " + seqreg);
			}
			if (seqreg == null)
				return;

			if (shouldCalcAlign()) {
				/* display aligned sequences */
				GenomicRegion reg = gene.getRegion();
				//System.out.println("##ALIGNSEQ: seqreg="+seqreg+" gene="+gene+" geneReg:"+reg);
				alignCache.getRegionOnAlignment(seqreg, reg, ginfo.getChromDir());

				/***
					int relpos;
					relpos = alignCache.getAlignmentPosition(reg.beginLocus(),ginfo.getChromDir(),true);
					if (relpos != 0) {
						begin_pos += relpos;
				//System.out.println("#relposB: "+begin_pos+"; "+relpos);
					}
					relpos = alignCache.getAlignmentPosition(reg.endLocus(),ginfo.getChromDir(),true);
					if (relpos != 0) {
						end_pos += relpos;
				//System.out.println("#relposE: "+end_pos+"; "+relpos);
					}
				*/
			}
			int xpos1L = get_xpos(seqreg.begin());
			int xpos1R = get_xpos(seqreg.end());

			int genelen = conv_xlen(seqreg.length());
			if (geneDrawMode.compareTo(GeneDrawMode.Arrow) <= 0) {
				int x[] = new int[5];
				int y[] = new int[5];
				int arrow_width = param.GENE_HEIGHT;
				if (geneDir > 0) {
					/* right arrow */
					x[0] = x[1] = xpos1L;
					x[2] = x[4] = xpos1R - arrow_width;
					x[3] = xpos1R;
					y[0] = y[4] = ypos1 + param.GENE_HEIGHT;
					y[1] = y[2] = ypos1 - param.GENE_HEIGHT;
					y[3] = ypos1;
				} else {
					/* left arrow */
					x[0] = x[1] = xpos1R;
					x[2] = x[4] = xpos1L + arrow_width;
					x[3] = xpos1L;
					y[0] = y[4] = ypos1 + param.GENE_HEIGHT;
					y[1] = y[2] = ypos1 - param.GENE_HEIGHT;
					y[3] = ypos1;
				}
				g.setColor(Color.black);
				g.drawPolygon(x, y, 5);
				g.setColor(geneCol);
				g.fillPolygon(x, y, 5);
			} else if (geneDrawMode.compareTo(
					GeneDrawMode.Rect) <= 0) {
				if (genelen < 1)
					genelen = 1;
				g.fillRect(xpos1L, ypos1 - param.GENE_HEIGHT,
						genelen, param.GENE_HEIGHT * 2);
			} else {
				g.drawLine(xpos1, ypos1 - param.GENE_HEIGHT, xpos1, ypos1 + param.GENE_HEIGHT);
			}
			/*
			if (geneDrawMode.compareTo(GeneDrawMode.Rect) <= 0) {
				Rectangle rect = new Rectangle(xpos1L, ypos1-param.GENE_HEIGHT,
					genelen, param.GENE_HEIGHT*2);
			}
			*/
		} else {
			g.drawLine(xpos1, ypos1 - param.GENE_HEIGHT, xpos1, ypos1 + param.GENE_HEIGHT);
		}
	}

	void alignSequence() {
		int spNo = 0;
//		ArrayList<Sequence> seqList = new ArrayList<Sequence>();
		String refsp = getRefSp();

		boolean use_cache = true;
		alignCache = AlignmentCache.getInstance();
		int addwin = alignCache.getAddwin();
//		int addwin_size = 0;

		boolean use_thread = alignCache.useThread();

		DPAlign dp = new DPAlign();

		/* set refsp sequence */
		Sequence refseq = null;
//		int seqlen = 0;
//		String refseqstr = null;

		if (use_cache) {
			alignCache.setSeqRegions(this);
			/* reference sequence with additional ntseq in addwin */
			refseq = alignCache.getRefSeq();
			/* lenth of original sequence */
//			seqlen = refseq.length();
//			refseqstr = refseq.getSeqString();
			//System.out.println("VIEWREG="+viewRegion+" "+viewRegion.length()+" "+refseq.length()+" "+seqlen);
		} else {
			for (GenomeMapInfo ginfo : currGinfoList) {
				if (ginfo.genome.getSpCode().equals(refsp)) {
					refseq = getSequence_in_ViewRegion(ginfo, addwin);
					break;
				}
			}
//			seqlen = refseq.length() - addwin * 2;
//			refseqstr = refseq.getSeqString();
		}
		//System.out.println("####AAALIGN");
		alignSeqHash.clear();
		if (use_cache && use_thread) {
			alignCache.createExecutor();
			for (spNo = drawRegSp.begin; spNo <= drawRegSp.end; spNo++) {
				GenomeMapInfo ginfo = currGinfoList.get(spNo);
				String spec = ginfo.genome.getSpCode();
				try {
					alignCache.getAlignment(spec);
				} catch (InterruptedException e) {
				}
			}
			try {
				///				ExecDP.joinAll();
				alignCache.waitJobs();
			} catch (InterruptedException e) {
			}
		}
		for (spNo = drawRegSp.begin; spNo <= drawRegSp.end; spNo++) {
			GenomeMapInfo ginfo = currGinfoList.get(spNo);
			String spec = ginfo.genome.getSpCode();

//			int ypos = get_ypos(spNo);

			SequenceAlignment ali = null;
			String aliseq = null;

			//System.out.println("LENGTH="+refseq.length());
			try {
				if (use_cache) {
					aliseq = alignCache.getAlignmentSequence(spec);
					/*
					if (aliseq.length() < 150) {
					System.out.println("ali="+aliseq);
					}
					*/
//					addwin_size = 0;
				} else {
					Sequence seq = getSequence_in_ViewRegion(ginfo, addwin);
					ali = dp.align(refseq, seq);
					aliseq = ali.getAlignedSeqToRef(1);
//					addwin_size = addwin;
				}
				alignSeqHash.put(spec, new GappedSequence(spec, aliseq));

			} catch (InterruptedException e) {
				System.err.println("Interrupted");
			}
		}
		//debug
		/**
		if (shouldCalcAlign()) {
		alignCache.printAlignment("gm04234");
		}
		*/
	}


	/** draw aligned DNA sequence */
	void drawSequence() {
		int spNo = 0;
		String refsp = getRefSp();
//		DPAlign dp = new DPAlign();

		AlignmentCache alignCache = AlignmentCache.getInstance();
		int addwin = alignCache.getAddwin();
		int addwin_size = 0; // additional window that should be removed

		/* set refsp sequence */
		Sequence refseq = null;
		int seqlen = 0;
		String refseqstr = null;

		boolean use_cache = true;
		boolean use_thread = alignCache.useThread();

		/*
		alignCache.setSeqRegions(this);
		refseq = alignCache.getRefSeq();
		refseqstr = refseq.getSeqString();
		seqlen = refseq.length() - addwin_size * 2;
		*/

		if (use_cache) {
			alignCache.setSeqRegions(this);
			/* reference sequence with additional ntseq in addwin */
			refseq = alignCache.getRefSeq();
			/* lenth of original sequence */
			seqlen = refseq.length() - addwin_size * 2;
			refseqstr = refseq.getSeqString();
			System.out.println(
					"VIEWREG=" + viewRegion + " " + viewRegion.length() + " " + refseq.length() + " " + seqlen);
		} else {
			for (GenomeMapInfo ginfo : currGinfoList) {
				if (ginfo.genome.getSpCode().equals(refsp)) {
					refseq = getSequence_in_ViewRegion(ginfo, addwin);
					break;
				}
			}
			seqlen = refseq.length() - addwin * 2;
			refseqstr = refseq.getSeqString();
		}

		/*
		System.out.println("spreg="+drawRegSp.begin+" "+drawRegSp.end+" "+
		currGinfoList.get(drawRegSp.begin).genome.getSpCode()+" "+
		currGinfoList.get(drawRegSp.end).genome.getSpCode());
		*/
		if (use_cache && use_thread) {
			for (spNo = drawRegSp.begin; spNo <= drawRegSp.end; spNo++) {
				GenomeMapInfo ginfo = currGinfoList.get(spNo);
				String spec = ginfo.genome.getSpCode();
				try {
					alignCache.getAlignment(spec);
				} catch (InterruptedException e) {
				}
			}
		}

//		Font f = g.getFont();
//		Font bf = f.deriveFont(Font.BOLD | Font.ITALIC);
//		FontMetrics bfm = FontDesignMetrics.getMetrics(bf);

		// drawRegSp: draw region on Y-axis in the scrolled pane
		for (spNo = drawRegSp.begin; spNo <= drawRegSp.end; spNo++) {
			GenomeMapInfo ginfo = currGinfoList.get(spNo);
			String spec = ginfo.genome.getSpCode();

			int ypos = get_ypos(spNo);

//			SequenceAlignment ali = null;
			String aliseq = null;

/*			if (false) {
				try {
					if (use_cache) {
						aliseq = alignCache.getAlignmentSequence(spec);
						addwin_size = 0;
					} else {
						Sequence seq = getSequence_in_ViewRegion(ginfo, addwin);
						ali = dp.align(refseq, seq);
						aliseq = ali.getAlignedSeqToRef(1);
						addwin_size = addwin;
					}

					//	refseqstr = ali.getAlignedSeqToRef(0);

				} catch (InterruptedException e) {
					System.err.println("Interrupted");
				}
			}
*/			GappedSequence gappedSeq = alignSeqHash.get(spec);
			aliseq = gappedSeq.getSeqString();
//			logger.debug("aliseq={}:{}", spec, aliseq);
			SeqRegion dispChrReg = getDisplayRegion(viewRegion, true);

			//System.out.print(ginfo.genome.getSpCode()+" ");

			g.setColor(Color.black);
			int refpos = 0, ref_alipos = 0;
			boolean ins_flag = false;

//			HitRangeList hrlist = new HitRangeList(this.mapViewer.getOption().getSequencePattern(), aliseq);

			for (int i = 0; i < aliseq.length(); i++) {
				String ch = aliseq.substring(i, i + 1).toUpperCase();
				if (ch.charAt(0) == '/') {
					// insertion; skip
					if (ins_flag) {
					} else {
						ins_flag = true;
						double seqpos = (double) dispChrReg.begin()
								+ (double) (refpos + 0.5) * dispChrReg.length() / seqlen;
						int xpos = get_xpos_d(seqpos);
						g.setColor(Color.blue);
						g.drawString("/", xpos, ypos + param.GENE_HEIGHT);
					}
					continue;
				}
				ins_flag = false;
				if (ref_alipos < addwin_size) {
					// skip additional window
					ref_alipos++;
					continue;
				}
				refpos = ref_alipos - addwin_size;
				double seqpos = (double) dispChrReg.begin() + (double) refpos * dispChrReg.length() / seqlen;
				/*
					double seqpos = (double) dispChrReg.begin() + (double) i * dispChrReg.length() / seqlen;
				*/
				int xpos = get_xpos_d(seqpos);

				//	String ch = seq.getSeqString().substring(i,i+1).toUpperCase();
				//Sstem.out.println("xpos:"+xpos+",i="+i+" "+seqpos+" "+ch);
				//	setBaseColor(ch.charAt(0));

				boolean isGap = SequenceAlignment.isGap(ch.charAt(0));
				boolean isIdent = (ch.charAt(0) == Character.toUpperCase(refseqstr.charAt(ref_alipos)));
				if (isGap) {
					g.setColor(Color.green);
				} else if (isIdent) {
					g.setColor(Color.black);
				} else {
					g.setColor(Color.red);
				}
				if (getViewWidth() > 200) {
					if (!isGap) {
						if (isIdent) {
							ch = ":";
						} else {
							ch = "|";
						}
					}
				}
				g.drawString(ch, xpos, ypos + param.GENE_HEIGHT);
				ref_alipos++;
				if (ref_alipos >= refseqstr.length()) {
					break;
				}
			}
			/*
			spNo++;
			if (spNo > end_sp) {
				break;
			}
			*/
		}
	}

	/** get the draw region on Y-axis (by spNo) in the scrolled pane */
	Region getDrawRegion() {
		Rectangle paneViewport = mapViewer.getScrollPane().getViewport().getViewRect();
		int begin_sp = get_sp_from_ypos(paneViewport.getY());
		int end_sp = get_sp_from_ypos(paneViewport.getY() + paneViewport.getHeight());
		begin_sp = (begin_sp < 0) ? 0 : begin_sp;
		end_sp = (end_sp >= currGinfoList.size()) ? currGinfoList.size() - 1 : end_sp;
		return new Region(begin_sp, end_sp);
	}

	GenomicRegion getSeqReg_in_ViewRegion(GenomeMapInfo ginfo) {
		return (getSeqReg_in_ViewRegion(ginfo, 0));
	}

	GenomicRegion getSeqReg_in_ViewRegion(GenomeMapInfo ginfo, int addwin) {
		if (viewRegion.begin() > viewRegion.end()) {
			System.out.println("#####******??????");
			System.out.println("VIEWREGION=" + viewRegion);
		}
		GenomicLocus loc1 = ginfo.getLocus_from_ViewPos(viewRegion.begin());
		GenomicLocus loc2 = ginfo.getLocus_from_ViewPos(viewRegion.end());
		GenomicLocus locC = ginfo.getLocus_from_ViewPos((viewRegion.begin() + viewRegion.end()) / 2);
		int chrDir = ginfo.getChromDir(locC);

		Chromosome c = ginfo.genome.getChromosome(locC.seqno);
		int posL, posR;
		if (loc1.seqno == locC.seqno) {
			posL = loc1.pos;
		} else {
			posL = c.getLength();
		}
		if (loc2.seqno == locC.seqno) {
			posR = loc2.pos;
		} else {
			posR = c.getLength();
		}
		if (posL > posR && (! c.isCircular() || posL - posR < c.getLength() / 2)) {
			/* Do not come here if the region is across the origin of the circular chromosome */
			//System.out.println("REV:"+ginfo.genome.getSpCode()+" "+posL+" "+posR+" "+chrDir);
			/* convert the position on the reverse strand */
			int tmp = posL;
			posL = posR;
			posR = tmp;
			/*
			chrDir = chrDir * -1;
			*/
			/*
			posL = c.getLength() - posL;
			posR = c.getLength() - posR;
			chrDir = chrDir * -1;
			*/
			//System.out.println("REV:after:"+ginfo.genome.getSpCode()+" "+posL+" "+posR+" "+chrDir);
		}
		posL = posL - addwin;
		posR = posR + addwin;
		//System.out.println("pos="+posL+" "+posR+" "+loc1+" "+loc2);
		return (new GenomicRegion(ginfo.getGenome().getSpCode(), locC.seqno, posL, posR, chrDir, genomeData));
	}

	Sequence getSequence_in_ViewRegion(GenomeMapInfo ginfo) {
		return (getSequence_in_ViewRegion(ginfo, 0));
	}

	Sequence getSequence_in_ViewRegion(GenomeMapInfo ginfo, int addwin) {
		logger.debug("addwin=" + addwin);
		GenomicRegion reg = getSeqReg_in_ViewRegion(ginfo);
		return (reg.getSequence_addflank(genomeData, addwin));
	}

	void setBaseColor(char c) {
		Color col = Color.black;
		switch (c) {
		case 'A':
			col = Color.red;
			break;
		case 'C':
			col = Color.yellow;
			break;
		case 'G':
			col = Color.green;
			break;
		case 'T':
			col = Color.magenta;
			break;
		}
		g.setColor(col);
	}

	/** Convert genomic locus (spec, seqno, pos) to the coordinate in the displayed window */
	public Point genomicLocus2Coordinate(GenomicLocus locus) {
		int spNo = compMap.getCurrGenomeOrder(locus.spec);
		GenomeMapInfo ginfo = compMap.getGenomeMap(spNo);
		int xpos = getDisplayPos(ginfo.getViewPosition(locus) - 1);
		int ypos = spNo;
		return new Point(xpos, ypos);
	}


	/** locate the region relative to current view window without clipping */
	SeqRegion getDisplayRegion(SeqRegion reg) {
		return getDisplayRegion(reg, false);
	}

	/** locate the region relative to current view window */
	SeqRegion getDisplayRegion(SeqRegion reg, boolean clipping) {
		int newreg_begin, newreg_end;
		if (viewRegion == null) {
			return reg;
		} else {
			if (reg.end < viewRegion.begin) {
				return null;
			} else if (clipping && reg.begin < viewRegion.begin) {
				/* clip region only when clipping option is specified */
				newreg_begin = 0;
			} else {
				newreg_begin = reg.begin - viewRegion.begin;
			}
			if (viewRegion.end < reg.begin) {
				return null;
			} else if (clipping && viewRegion.end < reg.end) {
				/* clip region only when clipping option is specified */
				newreg_end = viewRegion.length();
			} else {
				newreg_end = reg.end - viewRegion.begin;
			}
		}
		SeqRegion newreg = new SeqRegion(newreg_begin, newreg_end);
		SeqRegion.zeroBased();
		return (newreg);
	}

	/** locate the position relative to current view window */
	int getDisplayPos(int pos) {
		if (viewRegion == null) {
			return pos;
		} else {
			return pos - viewRegion.begin + 1;
			/* clipping is now done by setClip
			if (viewRegion.begin <= pos && pos <= viewRegion.end) {
				return pos - viewRegion.begin + 1;
			} else {
				return -1;
			}
			*/
		}
	}

	SeqRegion getDisplayReg(SeqRegion reg) {
		int begin = getDisplayPos(reg.begin);
		int end = getDisplayPos(reg.end);
		return (new SeqRegion(begin, end));
	}

	/** convert x-position from by base to by pixel */
	int get_xpos(int position) {
		return (param.LEFT_MARGIN + param.SPNAME_WIDTH + (int) ((double) (position - 1) * param.XSCALE));
	}

	/** convert x-position in double from by base to by pixel */
	int get_xpos_d(double position) {
		return (param.LEFT_MARGIN + param.SPNAME_WIDTH + (int) ((double) (position - 1) * param.XSCALE));
	}

	int conv_xlen(int len) {
		return (int) ((double) len * param.XSCALE);
	}

	/**
	 * 生物種オフセット。
	 */
	private int spOffset = 0;

	int get_ypos(int spNo) {
		return (param.TOP_MARGIN + param.SCALEBAR_HEIGHT +
				(int) (((double) (spNo - spOffset) + 0.5) * param.GENOME_HEIGHT));
	}

	int get_sp_from_ypos(double ypos) {
		return ((int) Math.round((double) (ypos - param.TOP_MARGIN - param.SCALEBAR_HEIGHT) / param.GENOME_HEIGHT
				- 0.5));
	}

	boolean isSpecNameArea(int xpos) {
		if (xpos < param.LEFT_MARGIN + param.SPNAME_WIDTH) {
			return (true);
		} else {
			return (false);
		}
	}

	/** reset center positions based on the clicked point (xpos,ypos) */
	void resetCenterPositions(int xpos, int ypos) {
		resetCenterPositions(xpos, ypos, false);
	}

	void resetCenterPositions(int xpos, int ypos, boolean addFlag) {
		GenomicLocus loc = getClickedLocus(xpos, ypos);
		String refsp = loc.spec;
		setRefSp(refsp);
		if (isSpecNameArea(xpos)) {
			return;
		}
		setCenterPos(loc, addFlag);
		int viewWidth = getViewWidth();
		setViewRegion(loc, viewWidth);
	}

	void selectClickedPos(int xpos, int ypos) {
		selectClickedPos(xpos, ypos, false, false);
	}

	void selectClickedPos(int xpos, int ypos, boolean addFlag, boolean contiguousSelection) {
		GenomicLocus loc = getClickedLocus(xpos, ypos);
		if (isSpecNameArea(xpos)) {
			setRefSp(loc.spec);
			return;
		}
		CoreCluster selCoreClust = coreGenome.getClusterByPos(loc, genomeData);
		Cluster selCluster = selCoreClust.cluster;
		if (addFlag) {
			// toggle between select and de-select status
			compMap.toggleSelectedCluster(selCluster);
		} else if (contiguousSelection) {
			this.selectRange(loc, selCluster);
		} else {
			compMap.setSelectedCluster(selCluster);
		}
		g.setStroke(new BasicStroke(3));
		drawGenesInCluster(selCluster, colorSelGene, 0);
	}

	/**
	 * 範囲選択を行う。
	 * @param loc クリックしたLocus。
	 * @param selCluster クリックしたクラスタ。
	 */
	private void selectRange(GenomicLocus loc, Cluster selCluster) {
		List<Gene> glist = new ArrayList<Gene>();
		// 既に選択されているクラスタと追加でクリックしたクラスタのGeneのリストを作成する。
		for (Cluster c: this.compMap.selectedClusters) {
			Gene gene = c.getGene(loc.spec);
			glist.add(gene);
		}
		Gene gene = selCluster.getGene(loc.spec);
		gene.dump();
		glist.add(gene);
		// クリックした生物種の位置の範囲を求める。
		double from = Double.MAX_VALUE;
		double to = 0;
		for (Gene g: glist) {
			if (g.pos < from) {
				from = g.pos;
			}
			if (g.pos >= to) {
				to = g.pos;
			}
		}
		// クリックした生物種の範囲内にあるクラスタを選択する。
		logger.debug("from = " + from + ", to = " + to);
		compMap.selectedClusters.clear();
		for (CoreCluster cc: this.coreGenome) {
			if (cc.cluster != null) {
				Gene g = cc.cluster.getGene(loc.spec);
				if (g != null) {
					if (from <= g.pos && g.pos <= to) {
						compMap.selectedClusters.add(cc.cluster);
					}
				}
			}
		}
	}

	/** Return the genomic locus of the clicked position */
	GenomicLocus getClickedLocus(int xpos, int ypos) {
		//System.out.println("clicked:"+xpos+" "+ypos);
		String refsp = get_spName_from_y_clickedpos(ypos);
		//System.out.println("Refsp="+refsp);
		GenomeMapInfo ginfo_refsp = compMap.getGenomeMap(refsp);
		int viewpos = get_viewPosition_from_x_clickedpos(xpos);
		//System.out.println("viewpos1:"+viewpos);
		if (viewRegion != null) {
			viewpos = viewRegion.begin + viewpos;
		}
		//System.out.println("viewpos2:"+viewpos);
		GenomicLocus locus = ginfo_refsp.getLocus_from_ViewPos(viewpos);
		locus.fillMissingInfo(refsp, genomeData);
		return (locus);
	}

	/**
	* Select genes near the specified locus and locate its orthologs at center of view region
	* @param loc locus to be centered
	*/
	void setCenterPos(GenomicLocus loc) {
		setCenterPos(loc, false);
	}

	void setCenterPos(GenomicLocus loc, boolean addFlag) {
		//String refsp = getRefSp();
		System.out.println("centerPos=" + loc);
		setCenterGenePos(loc);
		CoreCluster centerClust = coreGenome.getClusterByPos(loc, genomeData);
		System.out.println("CLUST:" + centerClust.id());
		if (centerClust != null) {
			System.out.println("add=" + addFlag);
			compMap.setCenterByCluster(centerClust.cluster);
			if (addFlag) {
				compMap.addSelectedCluster(centerClust.cluster);
			} else {
				compMap.setSelectedCluster(centerClust.cluster);
			}
		}
		// ToDo: this.compMap
		Map<String, List<String>> map = this.compMap.getSpLocusListMap();
		String json = JSON.encode(map, true);
		String spList = JSON.encode(this.compMap.getCurr_spList(), true);
		logger.info("map json=" + json);
		logger.info("spList=" + spList);

		mapViewer.getLocusInput().setText(loc.toString());
		setViewCenterPos(loc);
	}

	public void selectCenter(MouseEvent e) {
		logger.info("Select center");
		logger.info("clicked x=" + e.getX() + ", y=" + e.getY());
		this.selectClickedPos(e.getX(),  e.getY(), false, false);
		this.mapViewer.selectInparalog();
	}

	/**
	* Input string may be species:locus_tag or species:chrnum:position
	*/
	void setCenterPosByStr(String centerPosStr) {
		setCenterPosByStr(centerPosStr, false);
	}

	public void setCenterPosByStr(String centerPosStr, boolean flag_setRefSp) {
		logger.debug("centerPosStr=" + centerPosStr);
		GenomicLocus centerPos;
		Gene g = genomeData.genes.getGene(centerPosStr);
		if (g != null) {
			centerPos = g.getLocus();
		} else {
			centerPos = new GenomicLocus(centerPosStr);
			centerPos.assignMaxChrom(genomeData);
			//System.out.println("CC="+centerPos);
		}
		if (flag_setRefSp) {
			String refsp = centerPos.getSpecies();
			//System.out.println("##refsp="+refsp);
			if (refsp != null) {
				setRefSp(refsp);
			}
		}
		setCenterPos(centerPos);
	}

	/* Get view position at the clicked xpos */
	int get_viewPosition_from_x_clickedpos(int xpos) {
		return (int) ((double) (xpos - param.LEFT_MARGIN - param.SPNAME_WIDTH) / param.XSCALE) + 1;
	}

	/* Get species number at the clicked ypos */
	int get_spNo_from_y_clickedpos(int ypos) {
		return (int) Math
				.round(((double) (ypos - param.TOP_MARGIN - param.SCALEBAR_HEIGHT) / param.GENOME_HEIGHT) - 0.5);
	}

	/* Get species name at the clicked ypos */
	String get_spName_from_y_clickedpos(int ypos) {
		int spNo = get_spNo_from_y_clickedpos(ypos);
		/*
		String spName = compMap.getCurrGenomeMap(spNo).getGenome().getSpCode();
		*/
		//System.out.println("spNo="+spNo);
		String spName = currGinfoList.get(spNo).getGenome().getSpCode();
		return spName;
	}

	/** setParameters to determine width and height */
	void setParameters(Dimension dim) {
		setParameters(dim.width, dim.height);
	}

	/** setParameters to determine width and height */
	void setParameters(int width, int height) {
		setParametersHeight(height);
		setParametersWidth(width);
	}

	/** setParameters to determine height: Should be called before fixing the canvas size. */
	public int setParametersHeight(int height) {
		drawHeight = height;
		double genomeHeightRatio = 1.0;
		int spnum = compMap.getOutSpNum();
		int min_spnum = 20, max_spnum = 200;
		if (spnum > min_spnum && spnum <= max_spnum) {
			genomeHeightRatio = (spnum - min_spnum) / (max_spnum - min_spnum) * (1.0 - param.MIN_HEIGHT_RATIO)
					+ param.MIN_HEIGHT_RATIO;
		} else if (spnum > max_spnum) {
			genomeHeightRatio = param.MIN_HEIGHT_RATIO;
		}
		param.GENOME_HEIGHT = (int) ((double) param.GENOME_HEIGHT_DEFAULT * genomeHeightRatio);
		if (param.GENOME_HEIGHT < param.GENE_HEIGHT * 5) {
			param.GENE_HEIGHT = (int) (param.GENOME_HEIGHT * 0.2);
		}

		if (showScaleBar) {
			param.SCALEBAR_HEIGHT = param.GENOME_HEIGHT;
			param.SCALEBAR_TICK_HEIGHT = param.SCALEBAR_HEIGHT / 8;
		}
		int TotalHeight = compMap.getOutSpNum() * param.GENOME_HEIGHT;

		drawHeight = TotalHeight + param.TOP_MARGIN * 2 + param.SCALEBAR_HEIGHT;
		return (drawHeight);
	}

	/** setParameters to determine width. Should be called after Graphics is assigned (due to the need of font metrics) */
	void setParametersWidth(int _drawWidth) {
		drawWidth = _drawWidth;

		/*
		boolean auto_scale = true;
		if (auto_scale) {
			double scale = (double) (drawHeight - param.TOP_MARGIN * 2 - param.SCALEBAR_HEIGHT) / TotalHeight;
			if (scale > 1.0) {
				scale = 1.0;
			}
		}
		*/
		double scale = 1.0;

		/* determine param.SP_NAME_WIDTH */
		setNameWidth();

		int maxlen = (viewRegion == null) ? compMap.getViewWidth() : viewRegion.length();
		param.XSCALE = ((double) (drawWidth - param.LEFT_MARGIN * 2 - param.SPNAME_WIDTH) / scale) / maxlen;
		g.scale(scale, scale);
	}

	public void setupDrawParameters(Graphics2D g, int width, int height) {
		setParametersHeight(height);
		setGraphics(g);
		setParametersWidth(width);
		/*
		setParametersByPaperSize(width, height);
		*/
	}

	public void setParametersByPaperSize(int width, int height) {
		setParameters(width, height);
	}

	Color getColorByPos(double pos) {
		if (geneColorMode == GeneColorMode.RGB) {
			return getColorByPos_RGB(pos);
		} else {
			return getColorByPos_RG2(pos);
		}
	}

	Color getColorByPos_RG1(double pos) {
		float R, G, B;
		if (pos <= 0.5) {
			R = 1;
			G = (float) pos * 2;
			B = 0;
		} else {
			R = (float) (1.0 - pos) * 2;
			G = 1;
			B = 0;
		}
		return new Color(R, G, B);
	}

	Color getColorByPos_RG2(double pos) {
		float R, G, B;
		if (pos <= 0.5) {
			R = (float) (0.5 - pos) * 2;
			G = 1;
			B = 0;
		} else {
			R = 1;
			G = (float) (pos - 0.5) * 2;
			B = 0;
		}
		return new Color(R, G, B);
	}

	Color getColorByPos_RGB(double pos) {
//		double sixth = (double) 1 / 6;
		float R = 1, G = 0, B = 0;
		if (pos <= (double) 1 / 6) {
			R = 1;
			B = 0;
			G = (float) pos * 6;
		} else if (pos <= (double) 1 / 3) {
			R = (float) ((double) 1 / 3 - pos) * 6;
			G = 1;
			B = 0;
		} else if (pos <= (double) 1 / 2) {
			R = 0;
			G = 1;
			B = (float) (pos - (double) 1 / 3) * 6;
		} else if (pos <= (double) 2 / 3) {
			R = 0;
			B = 1;
			G = (float) ((double) 2 / 3 - pos) * 6;
		} else if (pos <= (double) 5 / 6) {
			G = 0;
			B = 1;
			R = (float) (pos - (double) 2 / 3) * 6;
		} else {
			G = 0;
			R = 1;
			B = (float) (1 - pos) * 6;
		}
		if (R < 0) {
			R = 0;
		}
		if (G < 0) {
			G = 0;
		}
		if (B < 0) {
			B = 0;
		}
		return new Color(R, G, B);
	}

	void setNameWidth() {
		int maxNameWidth = 0;
		for (String spec : genomeData.specList) {
			Genome genome = genomeData.getGenome(spec);
			int width = g.getFontMetrics().stringWidth(genome.getName() + "/-");
			if (width > maxNameWidth) {
				maxNameWidth = width;
			}
		}
		param.SPNAME_WIDTH = (int) ((double) maxNameWidth * 1.1);
	}
}
