package cgdp.corealign;

/** parameters for drawing comparative genome map */
public class ComparativeMapParams {
	/* Top margin of the draw area */
	final int TOP_MARGIN = 30;
	/* Left margin of the draw area */
	final int LEFT_MARGIN = 30;

	/* Hight of the region to show the genome information */
	final int GENOME_HEIGHT_DEFAULT = 80;
	int GENOME_HEIGHT = GENOME_HEIGHT_DEFAULT;
	/* Hight of the gene rectangle */
	int GENE_HEIGHT = 5;
	/* Width of the region for species name */
	int SPNAME_WIDTH = 15;
	/* Hight of the scale bar drawn at the top */
	int SCALEBAR_HEIGHT = 0;
	int SCALEBAR_TICK_HEIGHT = 0;
	final double MIN_HEIGHT_RATIO = 0.4;
	double XSCALE = 0.5;
	boolean drawLinks = true;

	public double ConsRatio = 0.8;

	private static ComparativeMapParams instance;

	private ComparativeMapParams() {
	}

	public static ComparativeMapParams getInstance() {
		if (instance == null) {
			instance = new ComparativeMapParams();
		}
		return(instance);
	}

	public static void clear() {
		ComparativeMapParams.instance = null;
	}
}

