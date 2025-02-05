package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

class GraphicalOutput extends JFrame {
	public GraphicalOutput() {
	}
	public GraphicalOutput(CoreGenome coreGenome, GenomeData genomeData, CoreGenomeDrawer drawer) {
		GPanel gpanel = new GPanel(drawer);
		gpanel.setPreferredSize(new Dimension(1800,1800));
		JScrollPane pane = new JScrollPane( gpanel);
		pane.getVerticalScrollBar().setUnitIncrement(25);
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		drawer.setPane(pane);
		setSize(1200,1000);
		add(pane , BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
/*
*/
	}
	static String[] readOrderFile(String file, int spnum) throws IOException {
		String[] spList = new String[spnum];
		int i = 0;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch(IOException e) {
			throw(e);
		}
		try {
			String linebuf;
			while ( (linebuf = reader.readLine()) != null ) {
				if (linebuf.charAt(0) == '#') {
					continue;
				}
				String[] fields = linebuf.split("[\\s]");
				spList[i++] = fields[0];
			}
		} catch (IOException e) {
			throw(e);
		}
		return(spList);
	}
	public static void main (String args[]) {

		String coreFile = null;
		String genomeFile = null;
		String bpfile = null;
		boolean domclustIn = true;
		GenomeData genomeData = null;
		CoreGenomeReader reader = null;
		CoreGenome coreGenome = null;
		boolean outputPDF = true;
		String outfile = "drawcore.pdf";
		String orderFile = null;
		String paper_size = null;
		String refsp = null;
		int gapWin = 20;
		int paper_width = 0, paper_height = 0;
		int maxDrawSpNum = 0;
		boolean simpleMode = false;
		boolean onelineMode = false;

//		CoreCluster.setGapWin(gapWin);
		CoreGenome.setGapWin(gapWin);

		int fn = 0;
		for (int i = 0; i < args.length; i++) {
			String ag = args[i];
			if (ag.charAt(0) == '-') {
				if (ag.startsWith("GUI", 1)) {
					outputPDF = false;
					onelineMode = true;
				} else if (ag.startsWith("outfile=", 1)) {
					outfile=ag.substring(9);
				} else if (ag.startsWith("paper=", 1)) {
					paper_size=ag.substring(7);
				} else if (ag.startsWith("refsp=", 1)) {
					refsp=ag.substring(7);
				} else if (ag.startsWith("width=", 1)) {
					paper_width=Integer.parseInt( ag.substring(7) );
				} else if (ag.startsWith("height=", 1)) {
					paper_height=Integer.parseInt( ag.substring(8) );
				} else if (ag.startsWith("bpfile=", 1)) {
					bpfile=ag.substring(8);
				} else if (ag.startsWith("maxDrawSpNum=", 1)) {
					maxDrawSpNum=Integer.parseInt( ag.substring(14) );
				} else if (ag.startsWith("orderfile=", 1)) {
					orderFile = ag.substring(11);
				} else if (ag.startsWith("simple", 1)) {
					simpleMode = true;
				} else if (ag.startsWith("oneline", 1)) {
					onelineMode = true;
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
			System.err.println("Usage: GraphicalOutput coreFile genomeFile");
			System.exit(1);
		}


		try {
			genomeData = GenomeData.readFromFile(genomeFile, domclustIn);
		} catch (IOException e) {
			System.err.println("Can't read genome data");
		}
		try {
			reader = new CoreGenomeReader(coreFile, genomeData);
			coreGenome = reader.readCoreGenome();
		} catch (IOException e) {
		}
		if (refsp != null) {
			coreGenome.setRefSp(refsp);
			coreGenome.setClusterNamesFromRefSp();
		}
		coreGenome.setAllConnections(genomeData);
		HashMap<String,BreakPoint> bpMap = null;
		ArrayList<BreakPoint> bpList = null;
		if (bpfile != null) {
			bpMap = new HashMap<String,BreakPoint>();
			try {
				bpList = BreakPoint.readBpFile(bpfile);
			} catch (IOException e) {
			}
			for (BreakPoint bp: bpList) {
				String key = bp.end + ":" + bp.spNo;
				bpMap.put(key, bp);
			}
		}

		CoreGenomeDrawer drawer = new CoreGenomeDrawer(coreGenome, genomeData);

		if (orderFile != null) {
			String[] spList = null;
			try {
				spList = readOrderFile(orderFile, coreGenome.specNum);
			} catch (IOException e) {
				System.err.println("Order file open error: "+orderFile);
				System.exit(1);
			}
			drawer.setSpOrder(spList);
		}
		
		if (bpMap != null) {
			drawer.addBpSet(bpMap);
		}
		if (maxDrawSpNum > 0) {
			drawer.setMaxDrawSpNum(maxDrawSpNum);
		}
		if (simpleMode) {
			drawer.setSimpleMode(simpleMode);
		}
		if (onelineMode) {
			RectArea rect = drawer.setOneline();
/*
			paper_width = rect.width;
			paper_height = rect.height/2;
*/
		}
		if (outputPDF) {
			GraphicalOutputPDF gout = new GraphicalOutputPDF(coreGenome, genomeData, drawer);
			if (paper_width > 0 && paper_height > 0) {
				gout.setPageSize((float) paper_width, (float) paper_height);
			} else if (paper_size != null) {
				gout.setPageSize(paper_size);
			}
			drawer.setPrintMode(true);
			gout.createPDF(outfile);
		} else {
			GraphicalOutput gout = new GraphicalOutput(coreGenome, genomeData, drawer);

			if (paper_width > 0 && paper_height > 0) {
				gout.setSize(paper_width, paper_height);
			}
			gout.setVisible(true);
		}
	}
}

class GraphicParameters {
	int COLNUM = 160;

//	int ROWSIZ = 6;
	int COLSIZ = 10;

	int LEFT_MARGIN = 20;
	int TOP_MARGIN = 20;

	int DOTSIZ = 4;
	int LINE_HEIGHT = 10;
	int LINE_WIDTH = 1;
	int SPNAME_WIDTH = 10;
	int USHIFT_SKIP = 1;
	int UNIT_HEIGHT = 0;
	int GNAME_HEIGHT = (int) (LINE_HEIGHT * 2.5);
	double LINE_WIDTH_THICK = 2.8;
	double LINE_WIDTH_THIN = 0.3;
	BasicStroke StrokeConnect, StrokeConnectThin, StrokeConnectThick;
	Color COLOR_INV = Color.red;
	Color COLOR_GAP = Color.green;
	Color COLOR_SMALL_BP = Color.cyan;
	Color COLOR_TRANSLOC = Color.yellow;
	Color COLOR_LARGE_BP = Color.black;
	Color COLOR_NORMAL = Color.black;

	String DEFAULT_FONT = "Arial";
	int FONT_SIZE = LINE_HEIGHT;
	int FONT_SIZE_GENE = (int) (LINE_HEIGHT*0.7);
	Font DefaultFont, FontGene;
	boolean skipOrdinaryGene = false;

	void setup(int spnum) {
		StrokeConnect = new BasicStroke( (float) LINE_WIDTH );
		StrokeConnectThin = new BasicStroke( (float) LINE_WIDTH_THIN );
		StrokeConnectThick = new BasicStroke( (float) LINE_WIDTH_THICK );
		DefaultFont = new Font(DEFAULT_FONT, Font.PLAIN, FONT_SIZE);
		FontGene = new Font(DEFAULT_FONT, Font.PLAIN, FONT_SIZE_GENE);
		UNIT_HEIGHT = LINE_HEIGHT * (spnum + 2) + GNAME_HEIGHT;
	}
	void skipOrdinaryGene() {
		skipOrdinaryGene(true);
	}
	void skipOrdinaryGene(boolean bool) {
		skipOrdinaryGene = bool;
	}
	RectArea setColnum(int colnum) {
		COLNUM = colnum;
		return(calcPageSize(colnum));
	}
	double setParametersByPaperSize(int total_colnum, int spnum, int paper_width, int paper_height) {
		int total_width = total_colnum * COLSIZ;
		// find the level l with which the paper size becomes ((tot_w / l) x (uni_h * l)), which should fit with (pap_w x pap_h).
		double estimated_levels =  Math.sqrt( (double) total_width / UNIT_HEIGHT * paper_height  / paper_width );
		COLNUM = (int) Math.ceil(total_colnum / estimated_levels);
//		double scale1 = (double) paper_width / (total_width / estimated_levels + LEFT_MARGIN * 2 + SPNAME_WIDTH);
		double scale1 = (double) paper_width / (COLNUM * COLSIZ + LEFT_MARGIN * 2 + SPNAME_WIDTH);
		double scale2 = (double) paper_height / (UNIT_HEIGHT * Math.ceil(estimated_levels) + TOP_MARGIN * 2);

		double scale = (scale1 > scale2) ? scale2 : scale1;
//System.out.println(scale1+" "+scale2+" "+scale);

		int colnum_max = (int) ((paper_width / scale - LEFT_MARGIN * 2 - SPNAME_WIDTH) / COLSIZ);
//		COLNUM += (int) ((colnum_max - COLNUM) / 2);

//		double scale1 = (double) paper_width / (total_width / estimated_levels + LEFT_MARGIN * 2 + SPNAME_WIDTH);
//System.out.println(paper_height+" "+TOP_MARGIN);
//System.out.println(UNIT_HEIGHT * Math.ceil(estimated_levels));

		return(scale);
	}
	RectArea calcPageSize(int total_colnum) {
		int width = COLNUM * COLSIZ + SPNAME_WIDTH + LEFT_MARGIN * 2;
		int levels = (int) Math.ceil(total_colnum / COLNUM);
		int height = levels * UNIT_HEIGHT + TOP_MARGIN * 2;
		RectArea area = new RectArea(width, height);
//System.out.println("size="+width+","+height);
		return(area);
	}
}
class RectArea {
	int width, height;
	RectArea(int _width, int _height) {
		width = _width; height = _height;
	}
}

class GPanel extends JPanel {
	CoreGenomeDrawer drawer;
	public GPanel(CoreGenomeDrawer _drawer) {
		super();
		drawer = _drawer;
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		setBackground(Color.white);
		Graphics2D g2 = (Graphics2D) g;
		int w = this.getWidth();
		int h = this.getHeight();

		drawer.setGraphics(g2);
		changeScale(1.6);
//		g2.setFont(param.DefaultFont);
		drawer.drawData();
	}
	void changeScale(double scale) {
		drawer.changeScale(scale);
		int[] dim = drawer.calcSize();
		setPreferredSize( new Dimension((int) (dim[0] * scale), (int) (dim[1] * scale)) );
	}
}

class CoreGenomeDrawer implements Drawer {
	CoreGenome coreGenome;
	GenomeData genomeData;

	JScrollPane pane;

	Graphics2D g;
	GraphicParameters param;
	int curr_lev;
	CalcDepth[] calcDepth;
	int[] spOrder;

	HashMap<String, BreakPoint> bpSet;

	int gapwin;

	int maxDrawSpNum;

	boolean printMode;

	double scale = 1;

	CoreGenomeDrawer(CoreGenome coreGenome, GenomeData genomeData) {
		GraphicParameters param = new GraphicParameters();
		param.setup(coreGenome.specNum());
		init(coreGenome, genomeData, param);
	}
	CoreGenomeDrawer(CoreGenome coreGenome, GenomeData genomeData, GraphicParameters param) {
		init(coreGenome, genomeData, param);
	}
	void init(CoreGenome _coreGenome, GenomeData _genomeData, GraphicParameters _param) {
		coreGenome = _coreGenome;
		genomeData = _genomeData;
		param = _param;
		curr_lev = -1;
		printMode = false;
		spOrder = new int[coreGenome.specNum];
		resetSpOrder();
	}
	JScrollPane getPane() {
		return(pane);
	}
	void setPane(JScrollPane _pane) {
		pane = _pane;
	}
	void setPrintMode(boolean _printMode) {
		printMode = _printMode;
	}
	void resetSpOrder() {
		for (int i = 0; i < coreGenome.specNum; i++) {
			spOrder[i] = i;
		}
	}
	void setSpOrder(int[] _order) {
		spOrder = _order;
	}
	void setSpOrder(String[] spList) {
		int i = 0;
		for (String spec: spList) {
			if (coreGenome.species.exists(spec)) {
				int spidx = coreGenome.species.getIdx(spec);
				spOrder[i] = spidx;
				i++;
			} else {
				System.err.println("species name not found: "+spec);
			}
		}
		while (i < spOrder.length) {
			spOrder[i++] = -1;
		}
	}
	public void setGraphics(Graphics2D _g) {
		g = _g;
	}
	void setMaxDrawSpNum(int spnum) {
		maxDrawSpNum = spnum;
	}
	void setNameWidth(Graphics g) {
		int maxNameWidth = 0;
		for (String spec: genomeData.specList) {
			int width = g.getFontMetrics().stringWidth(spec);
			if (width > maxNameWidth) {
				maxNameWidth = width;
			}
		}
		param.SPNAME_WIDTH = maxNameWidth + param.DOTSIZ;
	}
	RectArea setOneline() {
		return(param.setColnum(coreGenome.total_colnum()));
	}
	public void setParametersByPaperSize(int paperWidth, int paperHeight) {
		setNameWidth(g);
		double scale = param.setParametersByPaperSize(coreGenome.total_colnum(), coreGenome.specNum(), paperWidth, paperHeight);
//System.err.println("Scale="+scale);
		changeScale(scale);
	}
	public int setParametersHeight(int height){
		return 0;
	}
	void changeScale(double _scale) {
		scale = _scale;
		g.scale(scale, scale);
	}
	void addBpSet(HashMap<String, BreakPoint> _bpSet) {
		bpSet = _bpSet;
	}
	void setSimpleMode(boolean simpleMode) {
		if (simpleMode) {
			param.skipOrdinaryGene(true);
		}
	}
	public void drawData() {
		int blockid = 0;
		int coln = 0;
		int winsize = 40;
		int specnum = coreGenome.specNum();
		coreGenome.setAllConnections(genomeData);

		calcDepth = new CalcDepth[specnum];
		for (int spno = 0; spno < specnum; spno++) {
			calcDepth[spno] = new CalcDepth(winsize);
		}

		setNameWidth(g);
		curr_lev = -1;
		for (Iterator iter = coreGenome.blocks.iterator(); iter.hasNext(); ) {
			CoreGenomeBlock cblock = (CoreGenomeBlock) iter.next();
			blockid++;
			for (Iterator<CoreCluster> iter2 = cblock.coreClusterList.iterator(); iter2.hasNext(); ) {
				CoreCluster cclust = iter2.next();
				cclust.colnum = coln++;
				drawCluster(cclust);
			}
			coln++;
		}
	}
	void drawCluster(CoreCluster cclust) {
		int[] tabpos, xypos;
		tabpos = get_tabpos(cclust.colnum);
		xypos = get_xypos(tabpos);
		int xpos = xypos[0], ypos_base = xypos[1];

		g.setFont(param.DefaultFont);
if (tabpos[1] > 0){
System.out.println("####>>>>"+tabpos[1]);
}
		if (tabpos[1] != curr_lev) {
			// first column in each level
			curr_lev = tabpos[1];
//System.out.println("drawString: "+ curr_lev);
//			for (int spno = 0; spno < cclust.specNum(); spno++) {
			for (int spi = 0; spi < cclust.specNum(); spi++) {
				int spno = spOrder[spi];
				if (spno < 0) {
					continue;
				}
				if (maxDrawSpNum > 0) {
					if (spno > maxDrawSpNum) {
						break;
					}
				}

				String spec = coreGenome.species.get(spno);
/*
				spec = genomeData.getGenome(spec).getName();
*/
//				g.drawString(spec, xpos - param.SPNAME_WIDTH, ypos_base+spno*param.LINE_HEIGHT + param.DOTSIZ/2);
				g.drawString(spec, xpos - param.SPNAME_WIDTH, ypos_base + spi * param.LINE_HEIGHT + param.DOTSIZ/2);
				
			}
		}
//System.out.println("cluster:"+cclust.name());
		String name = cclust.id();
		if (cclust.name() != null && ! cclust.name().equals("") && ! cclust.name().equals(cclust.id())) {
			name = name + " ("+cclust.name()+")";
		}

		drawStringRotate(name, xpos, ypos_base - param.LINE_HEIGHT-param.DOTSIZ/2, -45);
		drawGene(xpos, ypos_base - param.LINE_HEIGHT+ param.DOTSIZ/2, cclust.dir);

		int begin_sp = 0, end_sp = cclust.specNum();
		if (! printMode) {
			Region drawRegSp = getDrawRegion();
			begin_sp = drawRegSp.begin;
			end_sp = drawRegSp.end;
		}
//System.out.println("drawReg:"+ begin_sp+" "+end_sp);
//		for (int spno = 0; spno < cclust.specNum(); spno++) {
//		for (int spi = 0; spi < cclust.specNum(); spi++) {
		for (int spi = begin_sp; spi < end_sp; spi++) {
			int spno = spOrder[spi];
			if (spno < 0) {
				continue;
			}
			if (maxDrawSpNum > 0) {
				if (spno > maxDrawSpNum) {
					break;
				}
			}

			ArrayList<Connection>connections = cclust.getConnections(spno);
//			calcDepth[spno].moveSkipDepth();
			calcDepth[spi].moveSkipDepth();
//			int ypos = ypos_base + spno * param.LINE_HEIGHT;
			int ypos = ypos_base + spi * param.LINE_HEIGHT;

			if (bpSet != null) {
				/* draw a virtical bar at break point*/
				String key = cclust.idx + ":" + spno;
				if (bpSet.containsKey(key)) {
					BreakPoint bp = bpSet.get(key);
//System.err.println(bp.type+" "+bp.type.isRearrangement());
//					if (bp.type == RearrType.LargeRearr || bp.type == RearrType.SmallRearr) {
					if (bp.type.isRearrangement()) {
						Color currColor = g.getColor();
						if (bp.type == RearrType.LargeRearr) {
							g.setColor(param.COLOR_LARGE_BP);
						} else if (bp.type == RearrType.SmallRearr) {
							g.setColor(param.COLOR_SMALL_BP);
						} else {
							g.setColor(param.COLOR_TRANSLOC);
						}
						int prev_xpos = xpos - param.COLSIZ;
						int xpos_mid = (prev_xpos+xpos)/2;
						g.drawLine(xpos_mid, ypos-param.DOTSIZ, xpos_mid,ypos+param.DOTSIZ);
						g.setColor(currColor);
					}
				}
			}
/*
if (cclust.id().equals("4929") && spi < 5) {
	System.out.println("cc:"+spi+" "+connections);
}
*/

			if (connections == null)
				continue;

			for (Connection conn: connections) {
				boolean skip_flag = false;
				if (bpSet != null) {
					/* draw a virtical bar at break point*/
					String key = cclust.idx + ":" + spno;
					if (bpSet.containsKey(key)) {
						BreakPoint bp = bpSet.get(key);
						if (bp.type == RearrType.LargeRearr) {
							int prev_xpos = xpos - param.COLSIZ;
//							g.setColor(param.COLOR_INV);
//System.out.println("kk!<<"+key);
							int xpos_mid = (prev_xpos+xpos)/2;
							g.drawLine(xpos_mid, ypos-param.DOTSIZ, xpos_mid,ypos+param.DOTSIZ);
						}
					}
				}
/*
if (cclust.id().equals("4929") && spi < 5) {
	System.out.println("cc2:"+spi+":"+spno+" "+cclust.id()+" "+conn.prev_node.id()+" "+conn+"; "+conn.prev_node.idx+" "+cclust.idx+"; "+cclust.prev_node_existing[spno]);
}
*/
				if (conn.prev_node.idx >= cclust.idx) {
					continue;
				}
				if (cclust.prev_node_existing[spno] == null) {
					continue;
				}
				if (conn.prev_node.idx < cclust.prev_node_existing[spno].idx) {
					skip_flag = true;
				}
				int[] prev_tabpos = get_tabpos(conn.prev_node.colnum);
				int[] prev_xypos = get_xypos(prev_tabpos);
//				int prev_xpos = prev_xypos[0], prev_ypos = prev_xypos[1] + spno * param.LINE_HEIGHT;
				int prev_xpos = prev_xypos[0], prev_ypos = prev_xypos[1] + spi * param.LINE_HEIGHT;
				if (conn.dirinv) {
					g.setColor(param.COLOR_INV);
				} else if (conn.diff > 1) {
					g.setColor(param.COLOR_GAP);
				}
				if (conn.fusion) {
					
				}
				if (prev_ypos == ypos) {
					if (skip_flag) {
/*
System.out.println(spno+" "+coreGenome.specNum()+" "+calcDepth.length);
System.out.println("diff:" + (cclust.idx - conn.prev_node.idx));
*/
/*
if (spno == coreGenome.getRefSpNo()) {
System.out.println("##before");
calcDepth[spi].print();
calcDepth[spi].setDebug(true);
} else {
calcDepth[spi].setDebug(false);
}
*/
//						int depth = calcDepth[spno].calc(cclust.idx, conn.prev_node.idx);
						int depth = calcDepth[spi].calc(cclust.idx, conn.prev_node.idx);
/*
if (spno == coreGenome.getRefSpNo()) {
System.out.println("##after");
System.out.println("depth="+depth+","+cclust.name()+" "+cclust.idx+" "+conn.prev_node.name()+" "+conn.prev_node.idx);
calcDepth[spno].print();
}
*/
						int yshift = param.DOTSIZ/2 + param.USHIFT_SKIP * depth;
						g.setStroke( param.StrokeConnectThin );
						g.drawLine(prev_xpos+1, prev_ypos, prev_xpos+1, ypos-yshift);
						g.drawLine(xpos-1, prev_ypos, xpos-1, ypos-yshift);
						g.drawLine(prev_xpos+1, prev_ypos-yshift, xpos-1, ypos-yshift);
						g.setStroke( param.StrokeConnect );
					} else {
						if (conn.fusion) {
							g.setStroke( param.StrokeConnectThick );
						} else {
							g.setStroke( param.StrokeConnect );
						}
						g.drawLine(prev_xpos, prev_ypos, xpos, ypos);
						g.setStroke( param.StrokeConnect );
					}
				}
				g.setColor(param.COLOR_NORMAL);
			}
			if (cclust.cluster.members[spno].size() > 1) {
				/* duplicate or split gene */
				g.fillRect(xpos-param.DOTSIZ/2, ypos-param.DOTSIZ/2, param.DOTSIZ, param.DOTSIZ);
			} else {
				if (param.skipOrdinaryGene == true) {
					g.drawLine(xpos, ypos-param.DOTSIZ/4, xpos, ypos+param.DOTSIZ/4);
				} else {
					g.fillOval(xpos-param.DOTSIZ/2, ypos-param.DOTSIZ/2, param.DOTSIZ, param.DOTSIZ);
				}
			}
		}
	}
	int[] get_tabpos(int coln) {
		int[] tabpos = new int[2];
		tabpos[0] = coln % param.COLNUM;
		tabpos[1] = coln / param.COLNUM;
		return(tabpos);
	}
	int[] get_xypos(int[] tabpos) {
		int[] xypos = new int[2];
		xypos[0] = param.LEFT_MARGIN + param.SPNAME_WIDTH + tabpos[0] * param.COLSIZ;
		xypos[1] = param.TOP_MARGIN + param.GNAME_HEIGHT + tabpos[1] * param.UNIT_HEIGHT;
		return(xypos);
	}
	int[] calcSize() {
		int[] ret = new int[2];
		int xsize = param.LEFT_MARGIN + param.SPNAME_WIDTH + param.COLNUM * param.COLSIZ;
		int ysize = param.TOP_MARGIN + param.UNIT_HEIGHT
				* ( (coreGenome.totalLength()+coreGenome.blockNum()) / param.COLNUM);
		ret[0] = xsize;
		ret[1] = ysize;
		return(ret);
	}
	void drawGene(int xpos, int ypos, int dir) {
		int y0 = ypos;
		int y1 = ypos - param.DOTSIZ/2;
		int y2 = ypos + param.DOTSIZ/2;
		int x1 = xpos - param.COLSIZ/2;
		int x2 = xpos + param.COLSIZ/2;
		g.setStroke( param.StrokeConnectThin );
		if (dir > 0) {
			int[] xposList = {x1, x1, x2};
			int[] yposList =  {y1, y2, y0};
			g.drawPolygon(xposList, yposList, 3);
		} else {
			int[] xposList = {x2, x2, x1};
			int[] yposList =  {y1, y2, y0};
			g.drawPolygon(xposList, yposList, 3);
		}
		g.setStroke( param.StrokeConnect );
	}
	void drawStringRotate(String string, int x, int y, int degree) {
		AffineTransform orig = g.getTransform();
		g.rotate(Math.toRadians((double) degree), (double) x, (double) y);
		g.setFont(param.FontGene);
		g.drawString(string, x, y);
		g.setTransform(orig);
	}
	/** get the draw region on Y-axis (by spNo) in the scrolled pane */
	Region getDrawRegion() {
		Rectangle paneViewport = pane.getViewport().getViewRect();
//System.out.println("VIEW:"+paneViewport.getY()+" "+paneViewport.getHeight());
		int begin_sp = get_sp_from_ypos(paneViewport.getY());
		int end_sp = get_sp_from_ypos(paneViewport.getY() + paneViewport.getHeight());
		begin_sp = (begin_sp -1 < 0) ? 0 : begin_sp -1;
		end_sp = (end_sp >= coreGenome.specNum()) ? coreGenome.specNum(): end_sp;
		return new Region(begin_sp, end_sp);
	}
	int get_sp_from_ypos(double ypos) {
/*
System.out.println("ypos:"+ypos+" "+param.TOP_MARGIN+" "+param.GNAME_HEIGHT+" "+param.LINE_HEIGHT+" "+scale);
System.out.println("conv:"+(int) Math.round((double) (ypos - param.TOP_MARGIN - param.GNAME_HEIGHT) / param.LINE_HEIGHT / scale -0.5));
*/
		return ((int) Math.round((double) (ypos - param.TOP_MARGIN - param.GNAME_HEIGHT) / param.LINE_HEIGHT / scale
				- 0.5));
	}
}
/**
 * Calculate the amont of shift to draw a line without overlap when connecting to the previous node 
 */
class CalcDepth {
	int winsize;
	long[] skipDepth;
	int MAXHEIGHT = 5;
	boolean DebugFlag = false;
	CalcDepth(int _winsize) {
		winsize = _winsize;
		skipDepth = new long[winsize+1];
	}
	void setDebug(boolean flag) {
		DebugFlag = flag;
	}
	void print() {
		for (int i = 0; i < winsize; i++) {
			System.out.println(i+" "+skipDepth[i]);
		}
	}
	void moveSkipDepth() {
		for (int i = winsize - 1; i >= 1; i--) {
			skipDepth[i] = skipDepth[i-1];
		}
		skipDepth[0] = 0;
	}
	int calc(int curr, int prev) {
		long foundDepth = 0;
		int retDepth = 0;
if (DebugFlag)  {
  System.out.println("curr: "+curr);
}
		if (curr - prev > winsize) {
			System.err.println("CalcDepth: index overflows");
		}
		for (int i = curr - 1; i > prev; i--) {
			int idx = curr - i;
			// j-th bit of skipDepth[idx] represents j-th line at i-th node
			// idx begins at 1 (when i = curr-1)
			foundDepth |= skipDepth[idx];
if (DebugFlag)  {
    System.out.println(i+" "+idx+" "+skipDepth[idx]+" "+foundDepth);
}
		}
		for (int i = 1; i < MAXHEIGHT; i++) {
			if ( ((foundDepth  >> i) & 1) ==  0 ) {
				retDepth = i;
				break;
			}
		}
		for (int i = prev; i <= curr; i++) {
			// update skipDepth
			int idx = curr - i;
			skipDepth[idx] |= (1 << retDepth);
if (DebugFlag) {
    System.out.println("2>"+i+" "+idx+" "+skipDepth[idx]+" "+foundDepth);
}
		}
		return(retDepth);
	}
}
