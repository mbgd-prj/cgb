package cgdp.corealign;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.util.WebDBConfUtil;
import lombok.Getter;
import lombok.Setter;

public class CompareMap {

	private static Logger logger = LogManager.getLogger(CompareMap.class);

	/**
	 * オープンしているStatusファイル。
	 */
	@Getter
	@Setter
	private static String statusFile = null;

	GenomeData genomeData;
	CoreGenome coreGenome;

	/**
	 * Islandゲノムのリスト。
	 */
	@Getter
	private CoreGenome island = null;

	/**
	 * Otherゲノムリスト。
	 */
	@Getter
	private List<CoreGenome> otherList = null;


	CompGenomeMap compMap;
	boolean islandMode;
	/**
	 * 用紙のサイズ(幅)。
	 */
	public static final int WIDTH = 1200;
	/**
	 * 用紙のサイズ(高さ)。
	 */
	public static final int  HEIGHT = 1600;

	public CompareMap(CoreGenome core, GenomeData gdata) {
		genomeData = gdata;
		coreGenome = core;
		/*int spnum =*/ genomeData.specNum();
		compMap = new CompGenomeMap(gdata);
		this.otherList = new ArrayList<CoreGenome>();
	}


	/**
	 * クリック位置から、Core,Island,Otherのクラスタを取得します。
	 * @param loc クリック位置。
	 * @param gdata ゲノムデータ。
	 * @return クラスタ。
	 */
	public CoreCluster getClusterByPos(GenomicLocus loc, GenomeData gdata) {
		GeneIdx geneIdx = gdata.getGeneIdxByPos(loc);
		geneIdx.dump();
		List<CoreCluster> clist = new ArrayList<CoreCluster>();
		{
			logger.debug("------- core -------");
			CoreCluster cc = this.coreGenome.getClusterByPos(loc, genomeData);
			if (cc != null) {
				clist.add(cc);
			}
		}
		{
			logger.debug("------- island -------");
			CoreCluster cc = this.island.getClusterByPos(loc, genomeData);
			if (cc != null) {
				clist.add(cc);
			}
		}
		{
			logger.debug("------- other -------");
			for (CoreGenome cg: this.otherList) {
				CoreCluster cc = cg.getClusterByPos(loc, genomeData);
				clist.add(cc);
			}
		}
		CoreCluster ret = null;
		double dist = Double.MAX_VALUE;
		for (CoreCluster cc: clist) {
			logger.debug("------- cc.dump() -------");
			cc.dump();
			logger.debug("------- getDistance() -------");
			double d = cc.getDistance(loc);
			logger.debug("d=" + d + ",dist=" + dist);
			if (d < dist) {
				dist = d;
				ret = cc;
				logger.debug("select");
			}
		}
		return ret;
	}


	/**
	 * アイランドゲノムを追加します。
	 * @param _island アイランドゲノム。
	 */
	public void setIsland(CoreGenome _island) {
		this.island = _island;
	}

	/**
	 * Otherゲノムを追加する。
	 * @param other Otherゲノム。
	 */
	public void addOther(CoreGenome other) {
		this.otherList.add(other);
	}

	public void outputText() {
		ArrayList<GenomeMapInfo> currGinfoList = compMap.getGenomeMapInfoList();
		for (CoreCluster cclust: coreGenome) {
			System.out.println("Cluster "+cclust.name());
			if (cclust.cluster.spConsRatio() != 1.0) {
				continue;
			}
			int spNo = 0;
			for (GenomeMapInfo ginfo: currGinfoList) {
				LinkedList<DomCluster> dlist = cclust.cluster.members[spNo];
				for (DomCluster dcl: dlist) {
					Gene g = dcl.dom.gene;
//					Chromosome chr = ginfo.genome.getChromosome(g.seqno);
					int pos = ginfo.getGeneViewPosition(g);
					System.out.println(">> "+spNo+" "+pos);
				}
				spNo++;
			}
		}
	}
	static String getExtension(String filename) {
		int extpos = filename.lastIndexOf('.');
		if (extpos >= 0) {
			return(filename.substring(extpos+1));
		} else {
			return null;
		}
	}
	static String addExtension(String filename, String extension) {
		String ext = getExtension(filename);
		if (ext != null && ext.equals(extension)) {
			return filename;
		} else {
			return filename + "." + extension;
		}
	}


	/**
	 * オプションパラメータ。
	 */
	@Getter
	private static CompareMapOpt option = null;
	/**
	 * Viewer。
	 */
	@Getter
	private static ComparativeMapViewer viewer = null;

	/**
	 * メイン処理。
	 * @param Args コマンドライン引数。
	 */
	public static void main(String Args[]) {
//		GenomicLocus centerPos = null;
//		ComparativeMapParams param = ComparativeMapParams.getInstance();
		WebDBConfUtil.createDefaultWebDBConfFile();

		CompareMap.option = new CompareMapOpt();
		try {
			CompareMap.option.parseArgs(Args);
			CompareMap.option.readData();
			logger.debug("arg isBlank=" + CompareMap.option.getCoreGenome().isBlank());


		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		if (CompareMap.option.isHelp()) {
			System.err.println("Usage: CompareMap <conffile | corefile genefile islandfile dnafile> [options]");
			System.err.println("  -width=width -height=height");
			System.err.println("  -refsp=spcode");
			System.err.println("  -center=[chr:]position | -CenterGene=spcode:gene");
			System.err.println("  -GUI | -outfile=filename.{pdf,png,jpg}");
			System.err.println("  -nolink");
			System.err.println("  -help");
			System.exit(1);
		}

		CompareMap.option.dump();

//		GenomeData gdata = CompareMap.option.getGdata();
//		CoreGenome coreGenome = CompareMap.option.getCoreGenome();
		CompareMap cmap = CompareMap.option.getCmap();
//		AlignmentCache alignCache = opt.getAlignCache();

		ComparativeMapDrawer drawer = new ComparativeMapDrawer(cmap);
		drawer.setOpt(CompareMap.option);
		logger.debug("viewWidth=" + CompareMap.option.getViewWidth());
		CompareMap.viewer = new ComparativeMapViewer(drawer, CompareMap.option.getPaper_width(), CompareMap.option.getPaper_height(), cmap.islandMode);
		boolean outputImage = CompareMap.option.isOutputImage();
		logger.debug("outputImage=" + outputImage);
		if (outputImage) {
			CompareMap.viewer.setVisible(true);
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
			CompareMap.outoutFile(drawer);
			CompareMap.viewer.setVisible(false);
			System.exit(0);
		} else {
			CompareMap.viewer.setVisible(true);
		}
	}

	/**
	 * 画像ファイルの出力処理。
	 * @param drawer Drawer。
	 */
	public static void outoutFile(ComparativeMapDrawer drawer) {
		GenomeData gdata = CompareMap.option.getGdata();
		CoreGenome coreGenome = CompareMap.option.getCoreGenome();
		String default_outfile = "compmap.pdf";
		String imageFormat = null;
		int paper_width = CompareMap.option.getPaper_width();
		int paper_height = CompareMap.option.getPaper_height();
		String outfile = CompareMap.option.getOutfile();
		if (outfile == null) {
			outfile = default_outfile;
		}
		imageFormat = getImageFormat(outfile);
		String paper_size = CompareMap.option.getPaper_size();
		if (imageFormat.equals("pdf")) {
			outputPDF(outfile, paper_size, paper_width, paper_height, gdata, coreGenome, drawer);
		} else {
			outputImage(outfile, imageFormat, paper_width, paper_height, drawer);
		}
	}


	/**
	 * 画像ファイルを出力する。
	 * @param outfile ファイル名。
	 * @param imageFormat ファイルフォーマット。
	 * @param paper_width 幅。
	 * @param paper_height 高さ。
	 * @param drawer Drawer。
	 */
	private static void outputImage(String outfile, String imageFormat, int paper_width, int paper_height,
			ComparativeMapDrawer drawer) {
		int imageType = BufferedImage.TYPE_INT_ARGB;
		if (imageFormat.equals("jpg")) {
			imageType = BufferedImage.TYPE_INT_RGB;
		}

		paper_height = drawer.setParametersHeight(paper_height);

		BufferedImage img = new BufferedImage(
			paper_width, paper_height, imageType);
		Graphics2D g = img.createGraphics();

		drawer.setGraphics(g);
		drawer.setParameters(new Dimension(paper_width,paper_height));
		paper_height = drawer.drawHeight;

		g.setColor(Color.white);
		g.fillRect(0,0,paper_width,paper_height);

		drawer.drawData();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		g.dispose();
		File fout = new File(outfile);
		try {
			ImageIO.write(img, imageFormat, fout);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * PDFの出力処理。
	 * @param outfile 出力ファイル。
	 * @param paper_size 用紙サイズ。
	 * @param paper_width 用紙幅。
	 * @param paper_height 用紙高さ。
	 * @param gdata ゲノムデータ。
	 * @param coreGenome コアゲノム。
	 * @param drawer Drawer。
	 */
	private static void outputPDF(String outfile, String paper_size, int paper_width, int paper_height,
			GenomeData gdata, CoreGenome coreGenome, ComparativeMapDrawer drawer) {
		GraphicalOutputPDF gout = new GraphicalOutputPDF(coreGenome, gdata, drawer);
		if (paper_width > 0 && paper_height > 0) {
			gout.setPageSize((float) paper_width, (float) paper_height);
		} else if (paper_size != null) {
			gout.setPageSize(paper_size);
		}
//			outfile = addExtension(outfile, "pdf");
		gout.createPDF(outfile);
	}

	/**
	 * 画像ファイルフォーマットを取得。
	 * @param outfile 出力ファイル名。
	 * @return フォーマット。
	 */
	private static String getImageFormat(final String outfile) {
		String imageFormat = null;
		String extension=getExtension(outfile);
		if (extension.equals("pdf")) {
			imageFormat = "pdf";
		} else if (extension.equals("png")) {
			imageFormat = "png";
		} else if (extension.equals("jpeg")  ||
				extension.equals("jpg")) {
			imageFormat = "jpg";
		}
		return imageFormat;
	}
}


