package cgdp.corealign;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgat.seq.DNASequence;
import cgat.seq.FastaFile;
import cgat.seq.IndexedFastaFile;
import cgdp.corealign.GenomeData.GeneInfo;
import cgdp.filereader.AnnotationFileReader;
import cgdp.filereader.SegmentFileReader;
import cgdp.filereader.SegmentFileReader.Segment;
import lombok.Data;
import net.arnx.jsonic.JSON;

/**
 * コマンドラインオプション情報。
 *
 */
@Data // getter/setterの自動生成。
public class CompareMapOpt {

	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(CompareMapOpt.class);

	/**
	 * Conf file。
	 */
	private boolean confFile = true;
	/**
	 * データパス。
	 */
	private String dataPath = null;

	/**
	 * coreファイル。
	 */
	private String corefile = null;
	/**
	 * geneファイル。
	 */
	private String genefile = null;

	/**
	 * Islandファイル名。
	 */
	private String islandFile = null;

	/**
	 * Otherファイル名リスト。
	 */
	private List<String> otherFileList = null;

	/**
	 * 表示順ファイル。
	 */
	private String dnaSeqFile = null;

	/**
	 * 遺伝子集合ファイル。
	 */
	private List<String> geneSetFileList = null;

	/**
	 * セグメントファイルリスト。
	 */
	private List<String> segmentFileList = null;


	/**
	 * セグメントリスト。
	 */
	private List<Segment> segmentList = null;

	/**
	 * セグメントの色コードのマップ。
	 */
	private Map<String, String> segmentColorMap = null;

	/**
	 * アノテーションファイル。
	 */
	private List<String> annotationFileList = null;

	// 各種オプション。

	/**
	 * -refsp=<spcode>で指定される生物種コード。
	 */
	private String refsp = null;
	/**
	 * -CenterGene=spcode:geneで指定される表示位置。
	 */
	private String centerGene = null;
	/**
	 * -center=[chr:]positionで指定される表示位置。
	 */
	private String centerPosStr  = null;
	/**
	 * 画像ファイル出力フラグ。
	 */
	private boolean outputImage  = false;
	/**
	 * -outfile=filename.{pdf,png,jpg}で指定された出力ファイル。
	 */
	private String outfile = null;
	/**
	 * -paper=<size>で指定された用紙サイズ。
	 */
	private String paper_size = null;
	private int paper_width = CompareMap.WIDTH;
	private int paper_height = CompareMap.HEIGHT;
	private int viewWidth = 0;
	private String colorMode = "RGB";
	private String orderfile = null;
	private String bpfile = null;
	private String altNameFile = null;
	private boolean colorIslandMode = false;
	private boolean islandAddMode = false;
	private double chromGapLenRatio = 0.05;
	private boolean moveWithinChrom = false;

	/**
	 * locusInputの値。
	 */
	private String locus = null;

	// Checkboxの値。
	private boolean showLinks = true;
	private boolean skip = false;
	private boolean reftop = false;
//	private boolean colorIsalnd = false;



	// Region
	private int regionBegin;
	private int regionEnd;
	private int regionOffset = 1;

	/**
	 * Helpメッセージ。
	 */
	private boolean help = false;

	/**
	 * 比較マップパラメータ。
	 */
	private ComparativeMapParams param = null;

	/**
	 * ゲノムデータ。
	 */
	private GenomeData gdata = null;

	/**
	 * コアゲノムデータ。
	 */
	private CoreGenome coreGenome = null;

	/**
	 * 比較マップ。
	 */
	private CompareMap cmap = null;

	/**
	 * 生物種とLeftovers Geneリストのマップ。
	 */
	private Map<String, List<Gene>> leftoversMap = null;

	private int alignmentAddwin = 400;

	/**
	 * アライメントキャッシュ。
	 */
	private AlignmentCache alignCache = null;

	private boolean leftoversView = false;
	private boolean leftoversGradation = false;
	private Color leftoversColor = Color.GRAY;


	/**
	 * 塩基配列の検索パターン。
	 */
	private String sequencePattern = null;
	/**
	 * 塩基配列検索タイプ。
	 */
	private int sequenceSearchType = 0;


	/**
	 * ヒット情報。
	 *
	 */
	@Data
	public static class HitInfo {
		private String chrName = null;
		private int seqNo = -1;
		private String sequence = null;
		private int start = 0;
		private int end = 0;
		private int length;
		private int dir = 1;

		/**
		 * コンストラクタ。
		 * @param chromosome 染色体。
		 * @param sequence 見つかったパターン。
		 * @param start 開始位置。
		 * @param end 終了位置。
		 */
		public HitInfo(String chrName, int seqNo, String sequence, int start, int end, int length, int dir) {
			this.chrName = chrName.replaceAll(":.*?$", "");
			this.seqNo = seqNo;
			this.start = start;
			this.end = end;
			this.length = length;
			this.sequence = sequence;
			this.dir = dir;
		}

		/**
		 * コンストラクタ。
		 * @param m マップ。
		 */
		public HitInfo(Map<String, Object> m) {
			this.chrName = (String) m.get("chrName");
			this.seqNo = ((BigDecimal) m.get("seqNo")).intValue();
			this.sequence = (String) m.get("sequence");
			this.start = ((BigDecimal) m.get("start")).intValue();
			this.end = ((BigDecimal) m.get("end")).intValue();
			this.length = ((BigDecimal) m.get("length")).intValue();
			this.dir = ((BigDecimal) m.get("dir")).intValue();
		}


		/**
		 * Locusを取得する。
		 * @return Locus。
		 */
		public String getLocusFrom() {
			String name = this.chrName;
			int seqno = this.seqNo;
			int start = this.getStart();
			String locus = name+ ":" + Integer.toString(seqno) +":" + (start);
			return locus;
		}

		/**
		 * Locusを取得する。
		 * @return Locus。
		 */
		public String getLocusTo() {
			String name = this.chrName;
			int seqno = this.seqNo;
			int end = this.getEnd();
			String locus = name+ ":" + Integer.toString(seqno) +":" + (end);
			return locus;
		}

		public void dump() {
			logger.debug("hitInfo:" + this.chrName + ":" + this.start + "→" + this.end + ":" + this.sequence + "," +this.dir);
		}

		/**
		 * GenomicRegionのインスタンスを取得する。
		 * @return GenomicRegionのインスタンス。
		 */
		public GenomicRegion getGenomicRegion() {
			double pos = (this.start + this.end) / 2;
			int len = Math.abs(this.end - this.start);
			int begin = ((int) Math.round(pos - (float)(len * 3 + 3) / 2 - 0.5));
			int end   = ((int) Math.round(pos + (float)(len * 3 + 3) / 2 - 0.5));
//			int begin = this.start;
//			int end = this.end;
//			GenomicRegion ret = new GenomicRegion(this.chrName, this.seqNo, this.start, this.end, this.dir);
			GenomicRegion ret = new GenomicRegion(this.chrName, this.seqNo, begin, end, this.dir);
			// ベースクラスのプロパティが設定されていないので設定する。
			ret.begin = begin;
			ret.end = end;
			return ret;
		}
	}

	/**
	 * 検索結果リスト。
	 */
	private List<HitInfo> hitList = null;



	/**
	 * クラスタグループ。
	 */
	@Data
	public static class ClusterGroup implements Cloneable, Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = -7780822215587599132L;
		/**
		 * グループ名。
		 */
		private String name = null;
		/**
		 * 表示フラグ。
		 */
		private boolean visible = true;
		/**
		 * カラーコード。
		 */
		private String colorCode = null;
		/**
		 * クラスタ集合。
		 */
		private ClusterSet clusterSet = null;

		/**
		 * コンストラクタ。
		 * @param name グループ名。
		 * @param visible 表示フラグ。
		 * @param colorCode 色コード。
		 */
		public ClusterGroup(final String name, final boolean visible, final String colorCode) {
			this.name = name;
			this.visible = visible;
			this.colorCode = colorCode;
			this.clusterSet = new ClusterSet();
		}

		/**
		 * コンストラクタ。
		 * @param map グループ名。
		 */
		public ClusterGroup(final Map<String, Object> map) {
			this.name = (String) map.get("name");
			this.visible = (Boolean) map.get("visible");
			this.colorCode = (String)  map.get("colorCode");;
			this.clusterSet = new ClusterSet();
		}


		/**
		 * クラスタを追加します。
		 * @param cluster クラスタ。
		 */
		public void addCluster(final Cluster cluster) {
			this.clusterSet.add(cluster);
		}

		/**
		 * 指定したblock中のクラスタを追加する。
		 * @param coreGenome core or island
		 * @param blockNo 追加するブロック番号。
		 */
		public void addBlock(final CoreGenome coreGenome, final String blockNo) {
			List<CoreGenomeBlock> blist = coreGenome.blocks;
			for (CoreGenomeBlock b: blist) {
				if (blockNo.equals(b.getBlockNo())) {
					for (CoreCluster cc: b.coreClusterList) {
						logger.debug("cc.cluster=" + cc.cluster.id + "," + cc.cluster.name);
						this.addCluster(cc.cluster);
					}
				}
			}
		}

		/**
		 * 指定したGeneを含むクラスタを追加します。
		 * @param coreGenome core or island
		 * @param gene Gene情報。
		 * @return 追加した場合。
		 */
		public boolean addClusterContainingGene(CoreGenome coreGenome, final GeneInfo gene) {
			boolean ret = false;
			if (coreGenome != null) {
				List<CoreGenomeBlock> blist = coreGenome.blocks;
				for (CoreGenomeBlock b: blist) {
					for (CoreCluster cc: b.coreClusterList) {
						Cluster c = cc.cluster;
						if (c.containing(gene)) {
							logger.debug("cc.cluster=" + cc.cluster.id + "," + cc.cluster.name);
							this.addCluster(c);
						}
					}
				}
			}
			return ret;
		}

		@Override
		public ClusterGroup clone() throws CloneNotSupportedException {
			return (ClusterGroup) super.clone();
		}
	}

	/**
	 * クラスタグループリスト。
	 */
	private List<ClusterGroup> clusterGroupList = null;

	/**
	 * CoreGenomeの表示フラグを取得する。
	 * @return CoreGenomeの表示フラグ
	 */
	public boolean isViewCore() {
		if (this.coreGenome == null) {
			return false;
		} else {
			return this.coreGenome.isVisible();
		}
	}

	/**
	 * CoreGenomeの表示フラグを設定する。
	 * @param visible 表示フラグ。
	 */
	public void setViewCore(boolean visible) {
		if (this.coreGenome != null) {
			this.coreGenome.setVisible(visible);
		}
	}

	/**
	 * Islandの表示フラグを取得する。
	 * @return Islandの表示フラグ
	 */
	public boolean isViewIsland() {
		if (this.cmap == null) {
			return false;
		} else {
			if (this.cmap.getIsland() == null) {
				return false;
			} else {
				return this.cmap.getIsland().isVisible();
			}
		}
	}

	/**
	 * Islandの表示フラグを設定する。
	 * @param visible 表示フラグ。
	 */
	public void setViewIsland(boolean visible) {
		if (this.cmap != null) {
			if (this.cmap.getIsland() != null) {
				this.cmap.getIsland().setVisible(visible);
			}
		}
	}

	/**
	 * CoreGenomeの色を取得する。
	 * @return CoreGenomeの色。
	 */
	public Color getCoreColor() {
		if (this.coreGenome == null) {
			return Color.WHITE;
		} else {
			return this.coreGenome.getColor();
		}
	}


	/**
	 * CoreGenomeの表示色を設定する。
	 * @param color 表示色。
	 */
	public void setCoreColor(Color color) {
		if (this.coreGenome != null) {
			this.coreGenome.setColor(color);
		}
	}


	/**
	 * Islandの表示色を取得する。
	 * @return Islandの表示色。
	 */
	public Color getIslandColor() {
		if (this.cmap == null) {
			return Color.WHITE;
		} else {
			if (this.cmap.getIsland() == null) {
				return Color.WHITE;
			} else {
				return this.cmap.getIsland().getColor();
			}
		}
	}

	/**
	 * Islandの表示色を設定する。
	 * @param color 表示色。
	 */
	public void setIslandColor(Color color) {
		if (this.cmap != null) {
			if (this.cmap.getIsland() != null) {
				this.cmap.getIsland().setColor(color);
			}
		}
	}

	/**
	 * CoreGenomeのグラデーションフラグを取得する。
	 * @return CoreGenomeのグラデーションフラグ。
	 */
	public boolean isCoreGradation() {
		if (this.coreGenome == null) {
			return false;
		} else {
			return this.coreGenome.isGradation();
		}
	}


	/**
	 * CoreGenomeの表示フラグを設定する。
	 * @param gradation 表示フラグ。
	 */
	public void setCoreGradation(boolean gradation) {
		if (this.coreGenome != null) {
			this.coreGenome.setGradation(gradation);
		}
	}


	/**
	 * Islandのグラデーションフラグを取得する。
	 * @return Islandのグラデーションフラグ
	 */
	public boolean isIslandGradation() {
		if (this.cmap == null) {
			return false;
		} else {
			if (this.cmap.getIsland() == null) {
				return false;
			} else {
				return this.cmap.getIsland().isGradation();
			}
		}
	}

	/**
	 * Islandのグラデーションフラグフラグを設定する。
	 * @param gradation グラデーションフラグフラグ。
	 */
	public void setIslandGradation(boolean gradation) {
		if (this.cmap != null) {
			if (this.cmap.getIsland() != null) {
				this.cmap.getIsland().setGradation(gradation);
			}
		}
	}

	/**
	 * コンストラクタ。
	 * @param args コマンドライン引数。
	 */
	public CompareMapOpt() {
		try {
			this.otherFileList = new ArrayList<String>();
			this.clusterGroupList = new ArrayList<ClusterGroup>();
			this.param = ComparativeMapParams.getInstance();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * クラスタグループリストをクリアします。
	 */
	public void resetClusterGroupList() {
		this.clusterGroupList = new ArrayList<ClusterGroup>();
	}

	/**
	 * Otherファイル名を追加する。
	 * @param otherFile Otherファイル名。
	 */
	public void addOtherFileName(final String otherFile) {
		this.otherFileList.add(otherFile);
	}

	/**
	 * ステータス情報をMap形式で取得する。
	 * @return ステータス情報をMap形式。
	 */
	private Map<String, Object> getStatusMap() throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("confFile", this.confFile);
		ret.put("dataPath", this.dataPath);
		ret.put("corefile", this.corefile);
		ret.put("genefile", this.genefile);
		ret.put("islandfile", this.islandFile);
		ret.put("otherFileList", this.otherFileList);
		ret.put("dnaSeqFile", this.dnaSeqFile);
		ret.put("refsp", this.refsp);
		ret.put("centerGene", this.centerGene);
		ret.put("centerPosStr", this.centerPosStr);
		ret.put("paper_size", this.paper_size);
		ret.put("paper_width", this.paper_width);
		ret.put("paper_height", this.paper_height);
		ret.put("viewWidth", this.viewWidth);
		ret.put("colorMode", this.colorMode);
		ret.put("orderfile", this.orderfile);
		ret.put("bpfile", this.bpfile);
		ret.put("altNameFile", this.altNameFile);
		ret.put("colorIslandMode", this.colorIslandMode);
		ret.put("islandAddMode", this.islandAddMode);
		ret.put("chromGapLenRatio", this.chromGapLenRatio);
		ret.put("moveWithinChrom", this.moveWithinChrom);

		// viwer settings
		ret.put("locus", this.locus);
		ret.put("showLinks", this.showLinks);
		ret.put("skip", this.skip);
		ret.put("reftop", this.reftop);
//		ret.put("colorIsalnd", this.colorIsalnd);

		// Region
		ret.put("regionBegin", this.regionBegin);
		ret.put("regionEnd", this.regionEnd);
		ret.put("regionOffset", this.regionOffset);

		// 塩基配列の検索結果関連
		ret.put("sequenceSearchType", this.sequenceSearchType);
		ret.put("sequencePattern", this.sequencePattern);
		ret.put("hitList", this.hitList);
		//
		ret.put("clusterGroupList", this.getCGList());
		return ret;
	}


	/**
	 * クラスタグループの保存形式リストを取得する。
	 * @return クラスタグループの保存形式リスト。
	 */
	public List<Map<String, Object>> getCGList() {
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		if (this.clusterGroupList != null) {
			for (ClusterGroup cg: this.clusterGroupList) {
				Map<String, Object> cgm = new HashMap<String, Object>();
				cgm.put("name", cg.getName());
				cgm.put("visible", cg.isVisible());
				cgm.put("colorCode", cg.getColorCode());
				List<String> cidlist = new ArrayList<String>();
				for (Cluster c: cg.getClusterSet().clusterList) {
					cidlist.add(c.id);
				}
				cgm.put("clusterIdList", cidlist);
				ret.add(cgm);
			}
		}
		return ret;
	}

	/**
	 * クラスタグループの保存処理。
	 * @param path ファイルのパス。
	 * @throws Exception 例外。
	 */
	public void saveClusterGroup(final String path) throws Exception {
		List<Map<String, Object>> cglist = this.getCGList();
		String json = JSON.encode(cglist, true);
		logger.debug("json=" + json);
		Charset charset = Charset.defaultCharset();
		Path p = Paths.get(path);
		try (BufferedWriter writer = Files.newBufferedWriter(p, charset)) {
			writer.write(json);
		}
	}

	/**
	 * クラスタグループの読み込み処理。
	 * @param grpfile ファイルのパス。
	 * @throws Exception 例外。
	 */
	public void loadClusterGroup(final File grpfile) throws Exception {
		try (FileInputStream is = new FileInputStream(grpfile)) {
			this.resetClusterGroupList();
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = JSON.decode(is, ArrayList.class);
			for (Map<String, Object> m: list)  {
				if (m.get("colorCode") == null) {
					m.put("colorCode", this.getNewColor());
				}
				this.addClusterGroup(m);
			}
		}
	}

	/**
	 * Map形式のステータス情報を設定する。。
	 * @param map Map形式のステータス情報。
	 */
	@SuppressWarnings("unchecked")
	public void setStatusMap(final Map<String, Object> map) throws Exception {
		this.confFile = (Boolean) map.get("confFile");
		this.dataPath = (String) map.get("dataPath");
		this.corefile = (String) map.get("corefile");
		this.genefile = (String) map.get("genefile");
		this.islandFile = (String) map.get("islandfile");
		this.otherFileList = (List<String>) map.get("otherfilelist");
		this.dnaSeqFile = (String) map.get("dnaSeqFile");
		this.refsp = (String) map.get("refsp");
		this.centerGene = (String) map.get("centerGene");
		this.centerPosStr = (String) map.get("centerPosStr");
		this.paper_size = (String) map.get("paper_size");
		this.paper_width = ((BigDecimal) map.get("paper_width")).intValue();
		this.paper_height = ((BigDecimal) map.get("paper_height")).intValue();
		this.viewWidth = ((BigDecimal) map.get("viewWidth")).intValue();
		this.colorMode = (String) map.get("colorMode");
		this.orderfile = (String) map.get("orderfile");
		this.bpfile = (String) map.get("bpfile");
		this.altNameFile = (String) map.get("altNameFile");
		this.colorIslandMode = (Boolean) map.get("colorIslandMode");
		this.islandAddMode = (Boolean) map.get("islandAddMode");
		this.chromGapLenRatio = ((BigDecimal) map.get("chromGapLenRatio")).doubleValue();
		this.moveWithinChrom = (Boolean) map.get("moveWithinChrom");

		// viwer settings
		this.locus = (String) map.get("locus");
		this.showLinks = (Boolean) map.get("showLinks");
		this.skip = (Boolean) map.get("skip");
		this.reftop = (Boolean) map.get("reftop");
//		this.colorIsalnd = (Boolean) map.get("colorIsalnd");

		// Region
		this.regionBegin = ((BigDecimal) map.get("regionBegin")).intValue();
		this.regionEnd = ((BigDecimal) map.get("regionEnd")).intValue();
		this.regionOffset = ((BigDecimal) map.get("regionOffset")).intValue();

		// 塩基配列の検索結果関連
		this.sequenceSearchType = ((BigDecimal) map.get("sequenceSearchType")).intValue();
		this.sequencePattern = (String) map.get("sequencePattern");
		this.hitList = new ArrayList<HitInfo>();
		List<?> hlist = (List<?>) map.get("hitList");
		if (hlist != null && hlist.size() > 0) {
			logger.debug("hlist=" + hlist.getClass());
			for (int i = 0; i < hlist.size(); i++) {
				Map<String, Object> m = (Map<String, Object>) hlist.get(i);
				HitInfo hi = new HitInfo(m);
				this.hitList.add(hi);
			}
		}
	}

	/**
	 * statusファイルからクラスタグループリストを取得する。
	 * @param map statusファイルのパース結果。
	 * @return クラスタグループリスト。
	 */
	public List<ClusterGroup> getCGList(Map<String, Object> map) {
		List<ClusterGroup> ret = new ArrayList<ClusterGroup>();
		List<?> list = (List<?>) map.get("clusterGroupList");
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) list.get(i);
				ClusterGroup cg = new ClusterGroup(m);
				@SuppressWarnings("unchecked")
				List<String> cidList = (List<String>) m.get("clusterIdList");
				for (String cid: cidList) {
					Cluster cluster = this.coreGenome.getCluster(cid);
					cg.addCluster(cluster);
				}
				ret.add(cg);
			}
		}
		return ret;
	}


	/**
	 * statusファイルからクラスタグループリストを取得する。
	 * @param map statusファイルのパース結果。
	 * @return クラスタグループリスト。
	 */
	public void addClusterGroup(Map<String, Object> map) {
		logger.debug("addClusterGroup: map=" + JSON.encode(map, true));
		ClusterGroup cg = new ClusterGroup(map);
		@SuppressWarnings("unchecked")
		List<String> cidList = (List<String>) map.get("clusterIdList");
		for (String cid: cidList) {
			Cluster cluster = this.coreGenome.getCluster(cid);
			if (cluster == null) {
				cluster = this.cmap.getIsland().getCluster(cid);
				if (cluster == null) {
					List<CoreGenome> list = this.cmap.getOtherList();
					for (CoreGenome g: list) {
						cluster = g.getCluster(cid);
						if (cluster != null) {
							break;
						}
					}
				}
			}
			logger.debug("cluster.id=" + cluster.id);
			cg.addCluster(cluster);
		}
		this.clusterGroupList.add(cg);
	}


	/**
	 * ステータス情報を取得します。
	 * @return ステータス情報。
	 */
	public String getStatusJson() throws Exception {
		Map<String, Object> map = this.getStatusMap();
		String json = JSON.encode(map, true);
		return json;
	}


	/**
	 * 基底のパスを取得する。
	 * @return 基底のパス。
	 */
	public String getBasePath() {
		String basePath = this.getDataPath();
		if (basePath == null) {
			return "";
		} else {
			if (this.confFile) {
				File dir = new File(basePath);
				basePath = dir.getParent();
			}
			return basePath + File.separator;
		}
	}

	/**
	 * アノテーションファイルを読み込みます。。
	 * @param fname アノテーションファイル名。
	 */
	private void readAnnotationFile(String fname) throws Exception {

		AnnotationFileReader reader = new AnnotationFileReader();
		this.anntationMap = reader.readAnnotationFile(fname);
	}

	/**
	 * データ読み込み処理。
	 * @throws Exception 例外。
	 */
	public void readData() throws Exception {
		ComparativeMapParams.clear();
		GenomeData.clear();
		this.setRefsp(null);
		this.setCenterPosStr(null);
		logger.debug("--- readData start ---");
		String genefile = this.getGenefile();
		logger.debug("readData genefile=" + genefile);
		if (genefile != null) {
			String fname = this.getFilePath(genefile);
			this.gdata = GenomeData.readFromDomClustGeneFile(fname);
			logger.info("*** genefile=" + fname + " readed");
//			this.gdata.dump();
			String coreFile = this.getFilePath(this.getCorefile());
			CoreGenomeReader reader = new CoreGenomeReader(coreFile, gdata);
			logger.debug("readData corefile=" + coreFile);
			List<String> typeList = reader.getFileTypeList();
			logger.info("fileTypeList=" + JSON.encode(typeList, true));
			if (typeList.size() == 0) {
				// coreのみのファイルの場合
				this.coreGenome = reader.readCoreGenome();
				this.coreGenome.setName("Core");
				this.coreGenome.setColor(Color.LIGHT_GRAY);
	//			this.coreGenome.dump();
				logger.debug(this.getCorefile() + " readed");
				this.setGapByChrLenMode();
				this.cmap = new CompareMap(coreGenome, gdata);
				int numProc = 0;
				this.alignCache = new AlignmentCache(gdata, alignmentAddwin, numProc);
				this.readIslandFile();
				this.readOtherFiles();
				this.readOrderFile();
				this.readDnaSeqFile();
				this.readAltGeneName();
				this.leftoversMap = this.createLeftoversMap();
			} else {
				// 1ファイルにcore,island,otherをまとめたファイルの場合。
				for (String type: typeList) {
					if ("core".equals(type)) {
						this.coreGenome = reader.readCoreGenome(type);
						this.coreGenome.setName("Core");
						this.coreGenome.setColor(Color.LIGHT_GRAY);
			//			this.coreGenome.dump();
						logger.debug(this.getCorefile() + " readed");
						this.setGapByChrLenMode();
						this.cmap = new CompareMap(coreGenome, gdata);
						int numProc = 0;
						this.alignCache = new AlignmentCache(gdata, alignmentAddwin, numProc);
					} else {
						CoreGenome cg = reader.readCoreGenome(type);
						if ("island".equals(type)) {
							cg.setName("Island");
							cg.setGradation(false);
							cg.setBlockType(BlockType.Island);
							boolean islandAddMode = this.isIslandAddMode();
							if (islandAddMode) {
								this.cmap.coreGenome.concatCore(cg);
							} else {
								this.cmap.setIsland(cg);
							}
						} else {
							cg.setName(type);
							cg.setGradation(false);
							cg.setColor(Color.LIGHT_GRAY);
							cg.setBlockType(BlockType.Other);
							this.cmap.addOther(cg);
						}
					}
				}

				//this.readIslandFile();
				//this.readOtherFiles();

				this.readOrderFile();
				this.readDnaSeqFile();
				this.readAltGeneName();
				this.leftoversMap = this.createLeftoversMap();

			}
		} else {
			this.coreGenome = new CoreGenome();
			this.gdata = GenomeData.getInstance();
			this.cmap = new CompareMap(coreGenome, gdata);
		}
		// アノテーションファイルの読み込み
		if (this.annotationFileList != null) {
			for (String ann: this.annotationFileList) {
				String fname = this.getFilePath(ann);
				this.readAnnotationFile(fname);
			}
		}
		// セグメントファイルの読み込み
		this.segmentList = new ArrayList<Segment>();
		this.segmentColorMap = new HashMap<>();
		if (this.segmentFileList != null) {
			SegmentFileReader reader = new SegmentFileReader();
			for (String seg: this.segmentFileList) {
				String fname = this.getFilePath(seg);
				List<Segment> seglist = reader.readSegmentFile(fname);
				this.segmentList.addAll(seglist);
				this.segmentColorMap.putAll(reader.getColorMap());
			}
		}
		logger.debug("--- readData finish. ---");
	}

	/**
	 * genesetに存在しないGeneを抽出したリストを作成する。
	 * @param genelist Geneのリスト。
	 * @param geneset 表示対象Genomeの集合。
	 * @return 抽出したリスト。
	 */
	private List<Gene> getLeftoversList(final List<Gene> genelist, final Set<String> geneset) {
		List<Gene> ret = new ArrayList<Gene>();
		for (Gene g: genelist) {
			String spname = g.sp + ":" + g.name;
			if (!geneset.contains(spname)) {
				ret.add(g);
			}
		}
		return ret;
	}


	/**
	 * その他ゲノムリストを作成する。
	 * @returns その他ゲノム。
	 */
	private List<Gene> createLeftoversList() {
		logger.debug("***** this.coreGenome.blocks.size()=" + this.coreGenome.blocks.size());
		Set<String> coreSet = this.coreGenome.getSpNameSet();
		Set<String> islandSet = new HashSet<String>();
		if (this.cmap.getIsland() != null) {
			logger.debug("***** this.cmap.island.blocks.size()=" + this.cmap.getIsland().blocks.size());
			islandSet = this.cmap.getIsland().getSpNameSet();
		}
		LinkedList<Gene> geneList = this.gdata.genes.geneList;
		List<Gene> othersList = this.getLeftoversList(geneList, coreSet);
		othersList = this.getLeftoversList(othersList, islandSet);
		logger.debug("othersgenes/allgenes=" + othersList.size() + "/" + this.gdata.genes.geneList.size());
		return othersList;
	}


	/**
	 * その他ゲノムリストを作成する。
	 * @returns その他ゲノム。
	 */
	private Map<String, List<Gene>> createLeftoversMap() {
		Map<String, List<Gene>> ret = new HashMap<String, List<Gene>>();
		List<Gene> leftoversList = this.createLeftoversList();
		for (Gene g: leftoversList) {
			String sp = g.sp;
			List<Gene> list = ret.get(sp);
			if (list == null) {
				list = new ArrayList<Gene>();
			}
			list.add(g);
			ret.put(sp, list);
		}
		return ret;
	}


	private void setGapByChrLenMode() {
		/** set the length of gap (in bp) between chromosomes */
		double chromGapLenRatio = this.getChromGapLenRatio();
		if (chromGapLenRatio > 0.0) {
			int mean_chrlen = gdata.getMeanChromosomeLength();
			if (mean_chrlen > 10000) {
				int gap = (int) ((double)mean_chrlen * chromGapLenRatio) / 100 * 100;
				GenomeMapInfo.setChromosomeGapLen(gap);
			}
		}
		GenomeMapInfo.setGapByChrLenMode(this.gdata.isRealChrLen());
	}


	/**
	 * Island ファイルを読み込む。
	 * @throws Exception 例外。
	 */
	private void readIslandFile() throws Exception {
		String islandfile = this.getIslandFile();
		logger.debug("readIslandFile " + islandfile);
		boolean islandAddMode = this.isIslandAddMode();
		if (islandfile != null) {
			try {
				CoreGenomeReader reader = new CoreGenomeReader(this.getFilePath(islandfile), gdata);
				CoreGenome island = reader.readCoreGenome();
				island.setName("Island");
				island.setGradation(false);
				island.setColor(Color.LIGHT_GRAY);
//					island.dump();
				island.setBlockType(BlockType.Island);
				if (islandAddMode) {
					this.cmap.coreGenome.concatCore(island);
				} else {
					this.cmap.setIsland(island);
				}
//				this.coreGenome.concatCore(island);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
			this.cmap.islandMode = true;
		}
		logger.debug("cmap.island=" + this.cmap.getIsland());
	}

	/**
	 * Otherファイルのリストを読み込みます。
	 * @throws Exception 例外。
	 */
	private void readOtherFiles() throws Exception {
		if (this.otherFileList != null) {
			for (String otherfile: this.otherFileList) {
				try {
					String name = new File(this.getFilePath(otherfile)).getName();
					logger.debug("other name=" + name);
					CoreGenomeReader reader = new CoreGenomeReader(this.getFilePath(otherfile), gdata);
					CoreGenome other = reader.readCoreGenome();
					other.setName(name);
					other.setGradation(false);
					other.setColor(Color.LIGHT_GRAY);
					other.setBlockType(BlockType.Other);
					this.cmap.addOther(other);
//					this.coreGenome.concatCore(other);
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
			}
		}
	}


	public String getFilePath(final String file) {
		logger.debug("getFilePath=" + file);
		File f = new File(file);
		if (f.exists() && f.isFile()) {
			return file;
		} else {
			return this.getBasePath() + file;
		}
	}


	/**
	 * Orderファイルを読み込む。
	 * @throws Exception 例外。
	 */
	private void readOrderFile() throws Exception {
		String orderfile = this.getOrderfile();
		if (orderfile != null) {
			logger.debug("readOrderFile " + orderfile);
			ArrayList<String> spOrder = null;
			try {
				spOrder = this.readOrderFile(this.getFilePath(orderfile));
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
			cmap.compMap.setGenomeOrder(spOrder);
		}
	}

	/**
	 * DNAシーケンスファイルの読み込む。
	 * @throws Exception 例外。
	 */
	private void readDnaSeqFile() throws Exception {
		String dnaSeqFile = this.getDnaSeqFile();
		logger.debug("readDnaSeqFile " + dnaSeqFile);
		if (dnaSeqFile != null) {
			IndexedFastaFile idxFastaFile = null;
			FastaFile fastaFile = null;
			try {
				idxFastaFile = new IndexedFastaFile(this.getFilePath(dnaSeqFile));
				idxFastaFile.asDNA();
			} catch (IOException e) {
				try {
					fastaFile = new FastaFile(this.getBasePath() + dnaSeqFile);
					fastaFile.asDNA();
				} catch (IOException e2) {
					logger.error("Can't open file", e2);
					System.exit(1);
				}
			}
			SeqData<DNASequence> genomeSeq = new SeqData<DNASequence>();
			if (idxFastaFile != null) {
				// read sequence index
				logger.debug("read sequence index");
				genomeSeq.readFromFastaIndex(idxFastaFile);
			} else {
				// read sequence string
				logger.debug("read sequence string");
				genomeSeq.readFromFasta(fastaFile);
			}
			gdata.setSequences(genomeSeq);
			// genomeSeq.dump();
		}
	}

	/**
	 * AltGeneNameを読み込む。
	 * @throws Exception 例外。
	 */
	private void readAltGeneName() throws Exception {
		String altNameFile = this.getAltNameFile();
		if (altNameFile != null) {
			try {
				gdata.readAltGeneName(this.getFilePath(altNameFile));
			} catch (IOException e) {
				logger.error("file open error: "+ this.getBasePath() + altNameFile, e);
				System.exit(1);
			}
		}
	}


	/**
	 * 表示順ファイルの読み込み。
	 * @param orderFile 表示順ファイル。
	 * @return 生物順リスト。
	 * @throws IOException 例外。
	 */
	private ArrayList<String> readOrderFile(String orderFile) throws IOException{
		File ordfile = new File(orderFile);
//		BufferedReader reader = null;
		ArrayList<String> spOrder = new ArrayList<String>();
		if (! ordfile.exists()) {
			logger.error("file not found: " + orderFile);
			System.exit(1);
		}
		try (BufferedReader reader = new BufferedReader( new FileReader(ordfile) )) {
			String linebuf = null;
			while  ( (linebuf = reader.readLine()) != null) {
				String[] fields  = linebuf.replaceFirst("\n$","").split("[\t ]");
				String spname = fields[0];
				spOrder.add(spname);
			}
		}
		return(spOrder);
	}

	/**
	 * コマンドライン引数を解析し、各種オプションをパースします。
	 * @param args コマンドライン引数。
	 */
	public void parseArgs(final String args[]) throws Exception {
		int fn = 0;
		for (int i = 0; i < args.length; i++) {
			String ag = args[i];
			if (ag.charAt(0) == '-') {
				this.setOption(ag, param);
			} else {
				String file = args[i];
				this.setFile(fn, file);
				fn++;
			}
		}
		if (fn == 1) {
			this.readPropertiesFile();
		}
	}

	/**
	 * ファイルの指定が1個の場合propertiesファイルとして読み込む。
	 */
	private void readPropertiesFile() throws Exception {
		CompareMapOpt opt = this;
		// all parameters will be incorporated from the config file
		String propfile = opt.getCorefile(); // corefile;
		logger.debug("propfile=" + propfile);
		Map<String,String> propMap = this.readProperty(propfile);
		for (String key: propMap.keySet()) {
			if (key.equals("corefile")) {
				String corefile = propMap.get(key);
				opt.setCorefile(corefile);
			} else if (key.equals("genefile")) {
				String genefile = propMap.get(key);
				opt.setGenefile(genefile);
			} else if (key.equals("islfile")) {
				String islandfile = propMap.get(key);
				opt.setIslandFile(islandfile);
				boolean islandAddMode = true;
				opt.setIslandAddMode(islandAddMode);
			} else if (key.equals("seqfile")) {
				String dnaSeqFile = propMap.get(key);
				opt.setDnaSeqFile(dnaSeqFile);
			} else if (key.equals("orderfile")) {
				String orderfile = propMap.get(key);
				opt.setOrderfile(orderfile);
			} else if (key.equals("altnamefile")) {
				String altNameFile = propMap.get(key);
				opt.setAltNameFile(altNameFile);
			} else if (key.equals("chromGapLenRatio")) {
				double chromGapLenRatio = Double.parseDouble( propMap.get(key) );
				opt.setChromGapLenRatio(chromGapLenRatio);
			}
		}
	}

	/**
	 * ファイルの設定。
	 * @param fn 引数のインデックス。
	 * @param file ファイル名。
	 */
	private void setFile(final int fn, final String file) {
		CompareMapOpt opt = this;
		switch (fn) {
		case 0:
			String corefile = file;
			opt.setCorefile(corefile);
			break;
		case 1:
			String genefile = file;
			opt.setGenefile(genefile);
			break;
		case 2:
			String islandfile = file;
			opt.setIslandFile(islandfile);
			boolean islandAddMode = true;
			opt.setIslandAddMode(islandAddMode);
			break;
		case 3:
			String dnaSeqFile = file;
			opt.setDnaSeqFile(dnaSeqFile);
			break;
		}
	}

	/**
	 * オプションの登録。
	 * @param ag コマンドライン引数。
	 * @param param パラメータ。
	 */
	private void setOption(String ag, ComparativeMapParams param) {
		CompareMapOpt opt = this;
		if (ag.startsWith("refsp=", 1)) {
			String refsp=ag.substring(7);
			opt.setRefsp(refsp);
		} else if (ag.startsWith("centerGene=", 1)) {
			String centerGene=ag.substring(12);
			opt.setCenterGene(centerGene);
		} else if (ag.startsWith("center=", 1)) {
			String centerPosStr=ag.substring(8);
			opt.setCenterPosStr(centerPosStr);
		} else if (ag.startsWith("center", 1)) {
			// TODO:この条件が必要か確認する。
			String centerPosStr = "1";
			opt.setCenterGene(centerPosStr);
		} else if (ag.startsWith("nolink", 1)) {
			param.drawLinks = false;
		} else if (ag.startsWith("GUI", 1)) {
			boolean outputImage = false;
			opt.setOutputImage(outputImage);
		} else if (ag.startsWith("outfile=", 1)) {
			String outfile=ag.substring(9);
			opt.setOutfile(outfile);
			opt.setOutputImage(true);
		} else if (ag.startsWith("consRatio=", 1)) {
			param.ConsRatio= Double.valueOf(ag.substring(11));
		} else if (ag.startsWith("paper=", 1)) {
			String paper_size=ag.substring(7);
			opt.setPaper_size(paper_size);
		} else if (ag.startsWith("width=", 1)) {
			int paper_width=Integer.parseInt( ag.substring(7) );
			opt.setPaper_width(paper_width);
		} else if (ag.startsWith("height=", 1)) {
			int paper_height=Integer.parseInt( ag.substring(8) );
			opt.setPaper_height(paper_height);
		} else if (ag.startsWith("viewWidth=", 1)) {
			int viewWidth=Integer.parseInt( ag.substring(11) );
			opt.setViewWidth(viewWidth);
		} else if (ag.startsWith("colorMode=", 1)) {
			String colorMode= ag.substring(11);
			opt.setColorMode(colorMode);
		} else if (ag.startsWith("orderFile=", 1)) {
			String orderfile= ag.substring(11);
			opt.setOrderfile(orderfile);
		} else if (ag.startsWith("altNameFile=", 1)) {
			String altNameFile= ag.substring(13);
			opt.setAltNameFile(altNameFile);
		} else if (ag.startsWith("shadow=", 1)) {
			// TODO: この設定は必要か ?
			String islandfile= ag.substring(8);
			opt.setIslandFile(islandfile);
		} else if (ag.startsWith("colorIslandMode", 1)) {
			boolean colorIslandMode = true;
			opt.setColorIslandMode(colorIslandMode);
		} else if (ag.startsWith("chromoGapLen=", 1)) {
			int gapLen = Integer.parseInt( ag.substring(14) );
			GenomeMapInfo.setChromosomeGapLen(gapLen);
		} else if (ag.startsWith("chromoGapLenRatio=", 1)) {
			double chromGapLenRatio = Double.parseDouble( ag.substring(19) );
			opt.setChromGapLenRatio(chromGapLenRatio);
		} else if (ag.startsWith("moveWithinChrom", 1)) {
			boolean moveWithinChrom = true;
			opt.setMoveWithinChrom(moveWithinChrom);
		} else if (ag.startsWith("help", 1)) {
			opt.setHelp(true);
		}
	}

	/**
	 * プロパティファイルを読み込む。
	 * @param file プロパティファイル。
	 * @return 読み込まれたマップ。
	 */
	private Map<String,String> readProperty(final String file) throws Exception {
		Properties prop = new Properties();
		try (InputStream istream = new FileInputStream(file)) {
			prop.load(istream);
		}
		Map<String,String> pMap = new HashMap<>();
		for (Map.Entry<Object,Object>e: prop.entrySet()) {
			logger.debug("key:" + e.getKey().toString() + ", " + e.getValue().toString());
			pMap.put(e.getKey().toString(), e.getValue().toString());
		}
		return(pMap);
	}


	/**
	 * クラスタグループを追加する。
	 * @param cg クラスタグループ。
	 */
	public void addClusterGroup(final ClusterGroup cg) {
		this.clusterGroupList.add(cg);
	}

	/**
	 * 新規クラスタグループ名称を取得する。
	 * @return 新規クラスタグループ名称。
	 */
	public String getNewGroupName() {
		int no = this.clusterGroupList.size() + 1;
		return "Cluster group " + no;
	}

	private static final String[] COLOR_TABLE = {
		"#696969"
		, "#4169e1"
		, "#00008b"
		, "#008080"
		, "#2f4f4f"
		, "#556b2f"
		, "#808000"
		, "#800000"
		, "#f08080"
		, "#c71585"

	};


	/**
	 * 新規クラスタグループ色を取得する。
	 * @return 新規クラスタグループ色。
	 */
	public String getNewColor() {
		int idx = this.clusterGroupList.size() % 10;
		return COLOR_TABLE[idx];
	}


	public int getGapLen() {
		return GenomeMapInfo.gap_len;
	}

	public void setGapLen(int gaplen) {
		GenomeMapInfo.gap_len = gaplen;
	}


	/**
	 * 設定内容をログ出力。
	 */
	public void dump() {
		logger.info("--- Compare map options  ---");
		logger.info("refsp=" + this.getRefsp());
		logger.info("centerGene=" + this.getCenterGene());
		logger.info("center=" + this.getCenterPosStr());
		logger.info("nolink=" + this.getParam().drawLinks);
		logger.info("ConsRatio=" + this.getParam().ConsRatio);
		logger.info("paper=" + this.getPaper_size());
		logger.info("paper_width=" + this.getPaper_width());
		logger.info("paper_height=" + this.getPaper_height());
		logger.info("view_width=" + this.getViewWidth());
		logger.info("colroMode=" + this.getColorMode());
		logger.info("shadow=" + this.getIslandFile());
		logger.info("colorIslandMode=" + this.isColorIslandMode());
		logger.info("chromosomeGapLen=" + GenomeMapInfo.gap_len);
		logger.info("chromosomeGapLenRatio=" + this.getChromGapLenRatio());
		logger.info("moveWithinChrom=" + this.isMoveWithinChrom());
		logger.info("orderFile=" + this.getOrderfile());
		logger.info("altNameFile=" + this.getAltNameFile());
		logger.info("----------------------------");
	}

	/**
	 * 最大長の染色体の先頭位置を取得する。
	 * @param spec 生物種。
	 * @return 最大長の染色体の先頭位置。
	 */
	public String getMaxChromosome(String spec) {
		logger.info("spec=" + spec);
		Genome genome = gdata.getGenome(spec);
		Chromosome ret = genome.chromosomes.get(0);
		for (Chromosome c: genome.chromosomes) {
			String n = spec + ":" + c.getSeqNo();
			logger.info("Chromosome=" + n + ", len=" + c.length);
			if (ret.length < c.length) {
				ret = c;
			}
		}
		logger.info("spec=" + spec + ", maxChromosome=" + spec + ":" + ret.getSeqNo());
		return spec + ":" + ret.getSeqNo() + ":1";
	}

	/**
	 * デフォルト*.statusファイル名を取得する。
	 * @return デフォルト*.statusファイル名。
	 */
	public String getDefaultStatusFile() {
		String fname = this.getFilePath(this.getCorefile()) + ".status";
		return fname;
	}

	/**
	 * デフォルト画像ファイル名を取得する。
	 * @return デフォルト画像ファイル名。
	 */
	public String getDefaultImageFile() {
		String fname = this.getFilePath(this.getCorefile()) + ".pdf";
		return fname;
	}


	/**
	 * デフォルト*.grpファイル名を取得する。
	 * @return デフォルト*.grpファイル名。
	 */
	public String getDefaultGroupFile(final String grpname) {
		String fname = this.getFilePath(this.getCorefile()) + ".grp";
		return fname;
	}

	/**
	 * デフォルト*.grpファイル名を取得する。
	 * @return デフォルト*.grpファイル名。
	 */
	public String getDefaultGroupDir() {
		String fname = this.getFilePath(this.getCorefile());
		return fname;
	}

	/**
	 * アノテーションのマップ。
	 */
	private Map<String, String> anntationMap = new HashMap<String, String>();

	/**
	 * アノテーションを取得する。
	 * @param spname 生物種名。
	 * @return アノテーション。
	 */
	public String getAnnotation(final String clustid) {
		return this.anntationMap.get(clustid);
	}


}
