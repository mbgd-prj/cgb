package cgdp.corealign;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgat.seq.Sequence;
import cgdp.corealign.CompareMapOpt.ClusterGroup;
import cgdp.corealign.CompareMapOpt.HitInfo;
import cgdp.corealign.CoreGenome.BlockInfo;
import cgdp.corealign.GenomeData.GeneInfo;
import cgdp.dialog.AddGroupDialog;
import cgdp.dialog.ClusterGroupEditDialog;
import cgdp.dialog.ClusterGroupListDialog;
import cgdp.dialog.SearchGenomeSequenceDialog;
import cgdp.dialog.SearchNameDialog;
import cgdp.dialog.SelectGeneSetDialog;
import cgdp.dialog.SelectInparalogDialog;
import cgdp.dialog.SelectSegmentDialog;
import cgdp.dialog.SpSequenceDialog;
import cgdp.dialog.TaxFileOpenDialog;
import cgdp.dialog.ViewOptionDialog;
import cgdp.dialog.VisibilityDialog;
import cgdp.util.UserConfUtil;
import lombok.Getter;
import net.arnx.jsonic.JSON;

/** GUI windows for comparative genome map with cotrol panel */
public class ComparativeMapViewer extends JFrame implements Printable {
	/**
	 *
	 */
	private static final long serialVersionUID = -650920398919882163L;

	/**
	 * Window title.
	 */
	private static final String WINDOW_TITLE = "Comparative Genome Browser";

	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(ComparativeMapViewer.class);

	private JMenuBar menuBar = null;
	// ファイルメニュー
	private JMenu fileMenu = null;
	// TAXファイルオープン
	private JMenuItem fileTaxOpen = null;
	// Statusファイルオープン。
	private JMenuItem fileOpen = null;
	// Statusファイル保存。
	private JMenuItem fileSave = null;
	// Statusファイル保存。
	private JMenuItem fileSaveAs = null;

	// 画像ファイル出力
	private JMenuItem fileWriteImage = null;
	// 印刷処理
	private JMenuItem filePrint = null;

	// GraphicalOutput起動出力
	private JMenuItem fileGraphicalOutput = null;


	// 終了
	private JMenuItem fileExit = null;

	// 編集メニュー
	private JMenu editMenu = null;
	// 選択範囲をグルーブに追加。
	private JMenuItem editAddSelectRange = null;
	// ブロックをグルーブに追加。
	private JMenuItem editAddBlock = null;
	// グループを編集。
	private JMenuItem editGroup = null;


	// 表示メニュー
	private JMenu viewMenu = null;
	// 表示順変更。
	private JMenuItem viewSequence = null;
	// 表示オプション。
	private JMenuItem viewOption = null;
	// 表示設定。
	private JMenuItem viewSetting = null;
	// 中心の選択。
	private JMenuItem selectInparalog = null;
	// 遺伝子集合の選択。
	private JMenuItem selectGeneSet = null;
	// 特徴領域の選択。
	private JMenuItem selectSegment = null;


	// 検索メニュー
	private JMenu searchMenu = null;
	// locusの正規表現検索。
	private JMenuItem searchName = null;
	// 塩基配列の検索。
	private JMenuItem searchSequence = null;



	@Getter
	private CompareMapOpt option = null;

	@Getter
	private ComparativeMapDrawer drawer;
//	private JPanel panel;
	@Getter
	private JTextField locusInput;

//	@Getter
//	private JComboBox<String> locusComboBox = null;

	@Getter
	private JScrollPane scrollPane;


	@Getter
	private JCheckBox CB_showLinks = null;
	@Getter
	private JCheckBox CB_skip = null;
	@Getter
	private JCheckBox CB_reftop = null;
	@Getter
	private JCheckBox CB_colorMode = null;


	private JButton centerSelectButton = null;


	ComparativeMapViewer(ComparativeMapDrawer _drawer, int width, int height, boolean islmode) {
		this.initMenuBar();
		drawer = _drawer;
		setSize(width,height);

		this.option = new CompareMapOpt();

		JButton leftButton = new JButton("<");
		JButton rightButton = new JButton(">");
		JButton plusButton = new JButton("+");
		JButton minusButton = new JButton("-");
		drawer.mapViewer = this;

		leftButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				drawer.moveLeft();
				repaint();
			}
		});
		rightButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				drawer.moveRight();
				repaint();
			}
		});
		plusButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				drawer.zoomIn();
				repaint();
			}
		});
		minusButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				drawer.zoomOut();
				repaint();
			}
		});

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JPanel p = new JPanel();
		JPanel p2 = new JPanel();

		p.add(leftButton); p.add(rightButton);
		p.add(plusButton); p.add(minusButton);

		ComparativeMapParams param = ComparativeMapParams.getInstance();

		this.CB_showLinks = new JCheckBox("Show links", param.drawLinks);
		this.CB_skip = new JCheckBox("Skip genomes lacking selected ortholog");
		this.CB_reftop = new JCheckBox("Move ref genome to top");
		this.CB_colorMode = new JCheckBox("Color islands");
		this.CB_colorMode.setSelected(false);

		this.CB_showLinks.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				param.drawLinks = ! param.drawLinks;
				repaint();
			}
		});
		p2.add(this.CB_showLinks);

		this.CB_skip.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				drawer.limitGenomesInSelectedCluster =
					! drawer.limitGenomesInSelectedCluster;
				repaint();
			}
		});
		p2.add(this.CB_skip);

		this.CB_reftop.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				drawer.moveRefGenomeToTop =
					! drawer.moveRefGenomeToTop;
				repaint();
			}
		});
		p2.add(this.CB_reftop);

//		String islandfile = this.option.getIslandfile();
//		if (islandfile != null && islandfile.length() > 0) {
			this.CB_colorMode.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					drawer.reverseColorIslandMode(ComparativeMapViewer.this.CB_colorMode.isSelected());
					repaint();
				}
			});
			p2.add(this.CB_colorMode);
//		}

		JLabel lab = new JLabel("Locus:");
		locusInput = new JTextField(15);
		locusInput.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String centerPosStr = locusInput.getText();
				if (centerPosStr != null) {
					String loc = getSpOnlyLocation(centerPosStr);
					if (loc != null) {
						centerPosStr = loc;
					} else 	if (centerPosStr.indexOf("cluster:") == 0) {
						String clustId = centerPosStr.replace("cluster:", "");
						List<String[]> list = ComparativeMapViewer.this.searchClustId(clustId);
						if (list.size() > 0) {
							centerPosStr = list.get(0)[1];
						} else {
							JOptionPane.showMessageDialog(ComparativeMapViewer.this, "Cluster id " + clustId + " not found.");
							return;
						}
					}
				}
				drawer.setCenterPosByStr(centerPosStr, true);
				repaint();
			}
		});
		p.add(lab);
		p.add(locusInput);

		this.centerSelectButton = new JButton("▼");
		this.centerSelectButton.setPreferredSize(new Dimension(46, 24));
		this.centerSelectButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				logger.info("Select center gene.");
				ComparativeMapViewer.this.selectInparalog();
			}
		});
		p.add(this.centerSelectButton);

		panel.add(p);
		panel.add(p2);

		ComparativeMapPanel comp_panel = new ComparativeMapPanel(drawer);
		comp_panel.setPreferredSize(new Dimension(width, height));
		scrollPane = new JScrollPane(comp_panel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(25);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane , BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.setTitle(WINDOW_TITLE);
		this.setButtonStatus();
	}

	/**
	 * "<spname>:"形式の場合のロケーション取得。
	 * @param centerPosStr 場所指定文字列。
	 * @return 中心文字列。
	 */
	private String getSpOnlyLocation(String centerPosStr) {
		String ret = null;
		if (Pattern.matches(".+:$", centerPosStr)) {
			String sp = centerPosStr.replace(":", "");
			Map<String, List<String>> map = getDrawer().getCompMap().getSpLocusListMap();
			List<String> loclist = map.get(sp);
			ret = centerPosStr + loclist.get(0);
		}
		return ret;
	}

	private void setButtonStatus() {
		this.centerSelectButton.setEnabled(this.isSelectCenterEnabled());
	}

	/**
	 * 中心のLocusを選択する。
	 */
	public void selectInparalog() {
		SelectInparalogDialog dlg = new SelectInparalogDialog(this);
		dlg.setModal(true);
		dlg.setVisible(true);
	}

	/**
	 * ウインドウタイトルの表示。
	 */
	public void setWindowTitle() {
		String title = WINDOW_TITLE;
		String fname = CompareMap.getStatusFile();
		if (fname != null) {
			title += " - (" + fname + ")";
		}
		this.setTitle(title);
	}

	@FunctionalInterface
	private interface EnableListener {
		boolean isEnabled();
	}

	/**
	 * メニューの有効判定ロジックの設定。
	 * @param menuItem メニューアイテム。
	 * @param listener メニューの有効判定処理。
	 */
	private void setEnabledListener(JMenuItem menuItem, EnableListener listener) {
		menuItem.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorRemoved(AncestorEvent event) {
				menuItem.setEnabled(listener.isEnabled());
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {
				menuItem.setEnabled(listener.isEnabled());
			}

			@Override
			public void ancestorAdded(AncestorEvent event) {
				menuItem.setEnabled(listener.isEnabled());
			}
		});
	}

	/**
	 * ファイル出力系メニューのEnable制御を追加する。
	 * @param menuItem メニュー項目。
	 */
	private void setWriteFileEnabled(JMenuItem menuItem) {
		logger.debug("setWriteFileEnabled=" + menuItem.getText());
		this.setEnabledListener(menuItem, () -> {
			logger.debug("setWriteFileEnabled=" + menuItem.getText() + ":" + ComparativeMapViewer.this.option.getCoreGenome() != null);
			return (ComparativeMapViewer.this.option.getCoreGenome() != null);
		});
	}

	/**
	 * 中央選択機能が使える状態かどうかを判定。
	 * @return 中央選択機能が使える状態の場合true。
	 */
	private boolean isSelectCenterEnabled() {
		boolean ret = false;
		if (this.drawer.getCompMap() != null) {
			Map<String, List<String>> map = this.drawer.getCompMap().getSpLocusListMap();
			if (map.size() > 0) {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * ファイル出力系メニューのEnable制御を追加する。
	 * @param menuItem メニュー項目。
	 */
	private void setSelectCeterEnabled(JMenuItem menuItem) {
		logger.debug("setSelectCeterEnabled=" + menuItem.getText());
		if (this.centerSelectButton != null) {
			this.centerSelectButton.setEnabled(false);
		}
		this.setEnabledListener(menuItem, () -> {
			return this.isSelectCenterEnabled();
		});
	}


	/**
	 * メニューバーの設定を行う。
	 */
	private void initMenuBar() {
		this.menuBar = new JMenuBar();
		// ファイルメニュー。
		this.menuBar.add(this.fileMenu = new JMenu("File"));
		this.fileMenu.add(this.fileTaxOpen = new JMenuItem("Open input files ..."));
		this.fileMenu.add(this.fileOpen = new JMenuItem("Open ..."));
		this.fileMenu.add(this.fileSave = new JMenuItem("Save ..."));
		this.fileMenu.add(this.fileSaveAs = new JMenuItem("Save as ..."));
		this.fileMenu.add(this.fileWriteImage = new JMenuItem("Write image ..."));
		this.fileMenu.add(this.filePrint = new JMenuItem("Print ..."));
		this.fileMenu.add(this.fileGraphicalOutput = new JMenuItem("Core Genome Viewer ..."));

		this.setWriteFileEnabled(this.fileSave);
		this.setWriteFileEnabled(this.fileSaveAs);
		this.setWriteFileEnabled(this.fileWriteImage);
		this.setWriteFileEnabled(this.filePrint);
		this.setWriteFileEnabled(this.fileGraphicalOutput);

		this.fileMenu.add(this.fileExit = new JMenuItem("Exit"));

		// 編集メニュー
		this.menuBar.add(this.editMenu = new JMenu("Edit"));
		this.editMenu.add(this.editAddSelectRange = new JMenuItem("Add selected range to group ..."));
		this.editMenu.add(this.editAddBlock = new JMenuItem("Add block to group ..."));
		this.editMenu.add(this.editGroup= new JMenuItem("Edit cluster group ..."));
		this.setWriteFileEnabled(this.editAddBlock);

		// 表示メニュー
		this.menuBar.add(this.viewMenu = new JMenu("View"));
		this.viewMenu.add(this.viewSequence = new JMenuItem("Change species sequence ..."));
		this.viewMenu.add(this.viewOption = new JMenuItem("View option ..."));
		this.viewMenu.add(this.viewSetting = new JMenuItem("Visible genome selection ..."));
		this.viewMenu.add(this.selectInparalog = new JMenuItem("Select inparalog ..."));
		this.viewMenu.add(this.selectGeneSet = new JMenuItem("Select gene set ..."));
		this.viewMenu.add(this.selectSegment = new JMenuItem("Select segment ..."));

		this.setWriteFileEnabled(this.viewSequence);
		this.setWriteFileEnabled(this.viewOption);
		this.setWriteFileEnabled(this.viewSetting);
		this.setSelectCeterEnabled(this.selectInparalog);
		this.setWriteFileEnabled(this.viewSetting);
		this.setWriteFileEnabled(this.viewSetting);


		// 検索メニュー
		this.menuBar.add(this.searchMenu = new JMenu("Search"));
		this.searchMenu.add(this.searchName = new JMenuItem("Search gene"));
		this.searchMenu.add(this.searchSequence = new JMenuItem("Search genome sequence"));
		this.setWriteFileEnabled(this.selectGeneSet);
		this.setWriteFileEnabled(this.selectSegment);

		this.setJMenuBar(this.menuBar);

		// File/Open tax.
		this.fileTaxOpen.addActionListener((ActionEvent e) ->  {
			try {
				ComparativeMapViewer.this.openTaxFile();
				this.setButtonStatus();
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
				JOptionPane.showMessageDialog(this, ex.getMessage());
			}
		});

		// File/Open
		this.fileOpen.addActionListener((ActionEvent e) ->  {
			ComparativeMapViewer.this.openFile();
			ComparativeMapViewer.this.setWindowTitle();
			this.setButtonStatus();
		});

		// File/Save
		this.fileSave.addActionListener((ActionEvent e) ->  {
			ComparativeMapViewer.this.saveFile(false);
			ComparativeMapViewer.this.setWindowTitle();
		});

		// File/SaveAs
		this.fileSaveAs.addActionListener((ActionEvent e) ->  {
			ComparativeMapViewer.this.saveFile(true);
			ComparativeMapViewer.this.setWindowTitle();
		});

		// File/Write Image
		this.fileWriteImage.addActionListener((ActionEvent e) -> {
			ComparativeMapViewer.this.writeImage();
		});

		// File/Print
		this.filePrint.addActionListener((ActionEvent e) -> {
			ComparativeMapViewer.this.print();
		});

		// File/Write Image
		this.fileGraphicalOutput.addActionListener((ActionEvent e) -> {
			ComparativeMapViewer.this.graphicalOutput();
		});


		// File/Exit
		this.fileExit.addActionListener((ActionEvent e) -> {
			System.exit(0);
		});

		// グループの追加はクラスタが選択された状態で有効。
		this.setEnabledListener(this.editAddSelectRange, () -> {
			int selcnt = ComparativeMapViewer.this.drawer.getCompMap().selectedClusters.size();
			return (selcnt > 0);
		});
		// グループの編集はグループが存在する時のみ有効。
		this.setEnabledListener(this.editGroup, () -> {
			/*List<ClusterGroup> list = ComparativeMapViewer.this.option.getClusterGroupList();
			if (list != null && list.size() > 0) {
				return true;
			}
			return false;*/
			return true;
		});



		this.editAddSelectRange.addActionListener((ActionEvent e) -> {
			this.editNewGroup();
		});

		this.editAddBlock.addActionListener((ActionEvent e) -> {
			this.addBlock();
		});

		this.editGroup.addActionListener((ActionEvent e) -> {
			this.editGroupList();
		});


		this.viewSequence.addActionListener((ActionEvent e) -> {
			logger.debug("Change species sequence");
			this.changeSpeciesSequence();
		});
		this.viewOption.addActionListener((ActionEvent e) -> {
			logger.debug("View option");
			this.viewOption();
		});
		this.viewSetting.addActionListener((ActionEvent e) -> {
			logger.debug("View setting");
			this.viewSetting();
		});
		this.selectInparalog.addActionListener((ActionEvent e) -> {
			logger.debug("selectCenter");
			this.selectInparalog();
		});
/*		this.selectGeneSet.addActionListener((ActionEvent e) -> {
			logger.debug("selectGeneSet");
//			this.selectInparalog();
		});*/
		this.selectGeneSet.addActionListener((ActionEvent e) -> {
			this.selectGeneSet();
		});
		this.selectSegment.addActionListener((ActionEvent e) -> {
			this.selectSegment();
		});


		this.searchName.addActionListener((ActionEvent e) -> {
			logger.debug("Search name");
			this.searchName();
		});
		this.searchSequence.addActionListener((ActionEvent e) -> {
			logger.debug("Search genome sequence");
			this.searchGenomeSequence();
		});
	}


	/**
	 * 	表示する特徴領域の選択。
	 */
	private void selectGeneSet() {
		logger.debug("selectGeneSet");
		SelectGeneSetDialog dlg = new SelectGeneSetDialog(this);
		dlg.setModal(false);
		dlg.setVisible(true);
	}


	/**
	 * 	表示する特徴領域の選択。
	 */
	private void selectSegment() {
		logger.debug("selectSegment");
		SelectSegmentDialog dlg = new SelectSegmentDialog(this);
		dlg.setModal(false);
		dlg.setVisible(true);
	}

	/**
	 * クラスタグループの編集。
	 */
	/**
	 * GraphicalOutputの起動。
	 */
	private void graphicalOutput() {
		String cp = System.getProperty("java.class.path");
		logger.info("cp=" + cp);
		Runtime r = Runtime.getRuntime();
		CompareMapOpt opt = ComparativeMapViewer.this.getOption();
		String genefile = opt.getFilePath(opt.getGenefile());
		String corefile = opt.getFilePath(opt.getCorefile());
		String orderfile = opt.getOrderfile();
		String bpfile = opt.getBpfile();
		try {
			String command = "java -cp " + cp + " cgdp.corealign.GraphicalOutput -GUI " + corefile + " " + genefile;
			if (orderfile != null) {
				orderfile = opt.getFilePath(orderfile);
				command = command + " -orderfile="+orderfile;
			}
			if (bpfile != null) {
				bpfile = opt.getFilePath(bpfile);
				command = command + " -bpfile="+bpfile;
			}
			r.exec(command);
			logger.info("cmd=" + command);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * ファイルをオーブンする。
	 */
	private void openTaxFile() throws Exception {
		TaxFileOpenDialog dlg = new TaxFileOpenDialog(this);
		dlg.setOption();
		dlg.setModal(true);
		dlg.setVisible(true);
	}

	/**
	 * ステータスファイルをオープンする。
	 */
	private void openFile() {
		try {
			JFileChooser dlg = new JFileChooser();
			String datapath = UserConfUtil.get(UserConfUtil.DATA_PATH);
			dlg.setCurrentDirectory(new File(datapath));
			dlg.addChoosableFileFilter(new FileNameExtensionFilter("Status File(*.status)", "status"));
			dlg.setAcceptAllFileFilterUsed(false);
			int selected = dlg.showOpenDialog(this);
			if (selected  == JFileChooser.APPROVE_OPTION) {
				File f = dlg.getSelectedFile();
				try {
					this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					this.readStatusFile(f);
				} finally {
					this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}

	/**
	 * Statusファイルの出力。
	 * @param file ファイル。
	 */
	private void readStatusFile(final File file) {
		try {
			try (FileInputStream is = new FileInputStream(file)) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = JSON.decode(is, HashMap.class);
				this.option.setStatusMap(map);
				this.option.readData();
				this.getDrawer().setOpt(this.option);

/*				String centerPosStr = locusInput.getText();
				if (centerPosStr != null && centerPosStr.length() > 0) {
					drawer.setCenterPosByStr(centerPosStr, true);
					repaint();
				}
*/
				this.drawer.param.drawLinks = this.option.isShowLinks();
				this.drawer.limitGenomesInSelectedCluster = this.option.isSkip();
				this.drawer.moveRefGenomeToTop = this.option.isReftop();

				this.drawer.viewRegion.begin = this.option.getRegionBegin();
				this.drawer.viewRegion.end = this.option.getRegionEnd();
				SeqRegion.offset = this.option.getRegionOffset();
				this.drawer.setGeneDrawMode(this.drawer.viewRegion);
				this.repaint();
				CompareMap.setStatusFile(file.getAbsolutePath());

				this.option.setClusterGroupList(this.option.getCGList(map));

				this.repaint();
				//
				this.setCenterPos();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}

	public void setCenterPos() {
		try {
			String centerPosStr = ComparativeMapViewer.this.option.getCenterPosStr();
			logger.debug("setCenterPosByStr:" + centerPosStr);
			if (centerPosStr != null) {
				ComparativeMapViewer.this.locusInput.setText(centerPosStr);
				ComparativeMapViewer.this.drawer.setCenterPosByStr(centerPosStr, true);
				ComparativeMapViewer.this.repaint();
			}
		} catch (Error e) {
			// エラーしたら最長の染色体の先頭に移動
			logger.error(e.getMessage());
			String newpos = this.option.getMaxChromosome(this.option.getRefsp());
			ComparativeMapViewer.this.locusInput.setText(newpos);
			ComparativeMapViewer.this.drawer.setCenterPosByStr(newpos, true);
			ComparativeMapViewer.this.repaint();
		}
	}

	/**
	 * Statusファイルの出力。
	 * @param file ファイル。
	 */
	private void writeStatusFile(final File file) {
		try {
//			String center = this.locusInput.getText();
			GenomicLocus center = this.drawer.getCenterGenePos();
			if (center != null) {
				String pos = center.spec + ":" + center.seqno + ":" + center.pos;
				this.option.setCenterPosStr(pos);
			} else {
				this.option.setCenterPosStr(null);
			}
			this.option.setShowLinks(this.CB_showLinks.isSelected());
			this.option.setSkip(this.CB_skip.isSelected());
			this.option.setReftop(this.CB_reftop.isSelected());
//			this.option.setColorIsalnd(this.CB_colorMode.isSelected());
			if (this.drawer.viewRegion != null) {
				this.option.setRegionBegin(this.drawer.viewRegion.begin);
				this.option.setRegionEnd(this.drawer.viewRegion.end);
				this.option.setViewWidth(this.drawer.viewRegion.end - this.drawer.viewRegion.begin + 1);
			}
			this.option.setRegionOffset(SeqRegion.offset);
			// TODO:
//			this.drawer.viewRegion.
			String json = this.option.getStatusJson();
			try (PrintWriter w = new PrintWriter(file)) {
				w.print(json);
			}
			CompareMap.setStatusFile(file.getAbsolutePath());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}

	/**
	 * ファイルを保存する。
	 * @param saveAs SaveAsフラグ。
	 */
	private void saveFile(final boolean saveAs) {
		String fname = saveAs ? null: CompareMap.getStatusFile();
		if (fname == null) {
			String statusfile = this.option.getDefaultStatusFile();
			logger.info("statusfile=" + statusfile);

			JFileChooser dlg = new JFileChooser() {
				private static final long serialVersionUID = 1L;
				@Override
				public void approveSelection() {
					File f = getSelectedFile();
					if (f.exists() && getDialogType() == SAVE_DIALOG) {
						String m = String.format(
								"<html>%s already exists.<br>Do you want to replace it?",
								f.getAbsolutePath());
						int rv = JOptionPane.showConfirmDialog(
								this, m, "Save As", JOptionPane.YES_NO_OPTION);
						if (rv != JOptionPane.YES_OPTION) {
							return;
						}
					}
					super.approveSelection();
				}
			};
			dlg.addChoosableFileFilter(new FileNameExtensionFilter("Status File(*.status)", "status"));
			dlg.setAcceptAllFileFilterUsed(false);
			dlg.setSelectedFile(new File(statusfile));
			int selected = dlg.showSaveDialog(this);
			if (selected  == JFileChooser.APPROVE_OPTION) {
				fname = dlg.getSelectedFile().getAbsolutePath();
			}
		}
		if (fname != null) {
			File f = new File(fname);
			if (Pattern.matches(".+\\.status$", f.getName())) {
				logger.debug("write image file = " + f.getAbsolutePath());
				this.writeStatusFile(f);
			} else {
				String msg = "Specify 'status' as the extension.";
				JOptionPane.showMessageDialog(this, msg);
			}
		}
	}


	/**
	 * 印刷処理。
	 */
	private void writeImage() {
		logger.debug("writeImage");
		JFileChooser dlg = new JFileChooser();
		String imagefile = this.option.getDefaultImageFile();
		logger.info("image file=" + imagefile);
		dlg.addChoosableFileFilter(new FileNameExtensionFilter("Image file(*.pdf, *.png, *.jpg)", "pdf","png","jpg"));
		dlg.setAcceptAllFileFilterUsed(false);
		dlg.setSelectedFile(new File(imagefile));
		int selected = dlg.showSaveDialog(this);
		if (selected  == JFileChooser.APPROVE_OPTION) {
			File f = dlg.getSelectedFile();
			if (Pattern.matches(".+\\.((pdf)|(png)|(jpg))$", f.getName())) {
				logger.debug("write image file = " + f.getAbsolutePath());
				this.writeFile(f.getAbsolutePath());
			} else {
				String msg = "Specify 'pdf', 'png' or 'jpg' as the extension.";
				JOptionPane.showMessageDialog(this, msg);
			}
		}
	}

	/**
	 * 画像ファイル出力。
	 * @param filename ファイル名。
	 */
	private void writeFile(final String filename) {
		try {
			CompareMap.getOption().setOutfile(filename);
			this.drawer.setPrintMode(true);
			CompareMap.outoutFile(this.drawer);
			this.drawer.setPrintMode(false);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			JOptionPane.showMessageDialog(this, ex.getMessage());
		}
	}

	/**
	 * 正規表現検索。
	 */
	private void searchName() {
		SearchNameDialog dlg = new SearchNameDialog(this);
		dlg.setModal(true);
		dlg.setVisible(true);
	}



	/**
	 * 正規表現検索。
	 */
	private void searchGenomeSequence() {
		SearchGenomeSequenceDialog dlg = new SearchGenomeSequenceDialog(this);
		dlg.setModal(false);
		dlg.setVisible(true);
	}

	/**
	 * 表示順の変更。
	 */
	private void changeSpeciesSequence() {
		SpSequenceDialog dlg = new SpSequenceDialog(this);
		dlg.setModal(true);
		dlg.setVisible(true);
	}

	/**
	 * 表示オプションの変更。
	 */
	private void viewOption() {
		try {
			ViewOptionDialog dlg = new ViewOptionDialog(this);
			dlg.setOption();
			dlg.setModal(true);
			dlg.setVisible(true);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			JOptionPane.showMessageDialog(this, ex.getMessage());
		}
	}

	/**
	 * 表示の変更。
	 */
	private void viewSetting() {
		VisibilityDialog dlg = new VisibilityDialog(this);
		dlg.setModal(true);
		dlg.setVisible(true);
	}

	/**
	 * 新規クラスターグループ。
	 */
	private void editNewGroup() {
		ClusterGroupEditDialog dlg = new ClusterGroupEditDialog(this);
		dlg.setModal(true);
		dlg.setVisible(true);
	}

	/**
	 * ブロック追加ダイアログ。
	 */
	private void addBlock() {
		AddGroupDialog dlg = new AddGroupDialog(this);
		dlg.setModal(true);
		dlg.setVisible(true);
	}

	/**
	 * グループリストの編集。
	 */
	private void editGroupList() {
		ClusterGroupListDialog dlg = new ClusterGroupListDialog(this);
		dlg.setModal(true);
		dlg.setVisible(true);
	}

	/**
	 * クラスターグループを追加する。
	 * @param g クラスターグループ。
	 */
	public void addClusterGroup(final ClusterGroup g) {
		for (Cluster cluster: this.drawer.getCompMap().selectedClusters) {
			g.addCluster(cluster);
			cluster.dump();
		}
		this.option.addClusterGroup(g);
		this.drawer.getCompMap().selectedClusters.clear();
		this.repaint();
	}

	/**
	 * 選択したブロックをクラスタグループに追加する。
	 * @param cg クラスタグループ。
	 * @param blist ブロックリスト。
	 */
	public void addBlockToClusterGroup(final ClusterGroup cg, final List<BlockInfo> blist) {
		CoreGenome core = this.getOption().getCoreGenome();
		CoreGenome island = this.getOption().getCmap().getIsland();
		for (BlockInfo bi: blist) {
			if (bi.isSelect()) {
				logger.debug(bi.getBlockType() + ":" + bi.getBlockNo());
				if ("Core".equals(bi.getBlockType())) {
					cg.addBlock(core, bi.getBlockNo());
				} else {
					cg.addBlock(island, bi.getBlockNo());
				}
			}
		}
		this.option.addClusterGroup(cg);
		this.repaint();
	}

	/**
	 * 選択したGeneを含むクラスタグループを作成します。
	 * @param cg クラスタグループ。
	 * @param glist gene選択リスト。
	 */
	public void addGeneToClusterGroup(final ClusterGroup cg, final List<GeneInfo> glist) {
		CoreGenome core = this.getOption().getCoreGenome();
		CoreGenome island = this.getOption().getCmap().getIsland();
		for (GeneInfo g: glist) {
			if (g.isSelect()) {
				if (!cg.addClusterContainingGene(core, g)) {
					cg.addClusterContainingGene(island, g);
				}
			}
		}
		this.option.addClusterGroup(cg);
		this.repaint();
	}

	/**
	 * 塩基配列パターン検索。
	 *
	 */
	private class PatternMatcher {
		@Getter
		private List<HitInfo> hitResult = null;

		@Getter
		private String pattern = null;

		/**
		 * 正規表現パターン。
		 */
		private Pattern regpat = null;
		/**
		 *コンストラクタ。
		 * @param pattern 塩基配列パターン。
		 */
		public PatternMatcher(final String pattern) {
			this.hitResult = new ArrayList<HitInfo>();
			this.pattern = pattern;
			this.regpat = Pattern.compile(pattern);
		}

		/**
		 * 順方向検索。
		 * @param chrName 染色体名。
		 * @param seqno 染色体順序。
		 * @param sequence シーケンス。
		 * @param pattern 塩基配列パターン。
		 * @param from 検索範囲開始。
		 * @param to 検索範囲終了。
		 * @param length 染色体の長さ。
		 */
		private void searchForword(final String chrName, final int seqno, final Sequence sequence, final String pattern, final int from, final int length) {
			String seq = sequence.getSeqString();
System.out.println("###SEQ=>> "+sequence.getName()+" "+seq.length());
			Matcher m = regpat.matcher(seq);
			while (m.find()) {
				String group = m.group();
				int start = m.start();
				int end = m.end();
				this.hitResult.add(new HitInfo(chrName, seqno, group, from + start, from + end, length, 1));
			}
		}

		/**
		 * 逆パターン検索。
		 * @param chrName 染色体名。
		 * @param seqno 染色体順序。
		 * @param sequence シーケンス。
		 * @param pattern 塩基配列パターン。
		 * @param from 検索範囲開始。
		 * @param to 検索範囲終了。
		 * @param length 染色体の長さ。
		 */
		private void searchReverse(final String chrName, final int seqno, final Sequence sequence, final String pattern, final int to, final int length) {
			String orgseq = sequence.getSeqString();
			int olen = orgseq.length();
			Sequence rs = sequence.getReverse();
			String seq = rs.getSeqString();
			Matcher m = regpat.matcher(seq);
			while (m.find()) {
				int start = to - m.end();
				int end = to - m.start();
				String mtext = orgseq.substring(olen - m.end(), olen - m.start());
				this.hitResult.add(new HitInfo(chrName, seqno, mtext, start, end, length, -1));
			}
		}

		/**
		 * 塩基配列パータンの検索。
		 * @param chrName 染色体名。
		 * @param seqno 染色体順序。
		 * @param sequence シーケンス。
		 * @param pattern 塩基配列パターン。
		 * @param from 検索範囲開始。
		 * @param to 検索範囲終了。
		 * @param length 染色体の長さ。
		 */
		public void search(final String chrName, final int seqno, final Sequence sequence, final String pattern, final int from, final int to, final int length) {
			this.searchForword(chrName, seqno, sequence, pattern, from, length);
			this.searchReverse(chrName, seqno, sequence, pattern, to, length);
		}
	}


	/**
	 * refspのパターンのみを検索。
	 * @param pat パターン。
	 * @param gdata ゲノムデータ。
	 * @return 検索結果。
	 */
	private List<HitInfo> searchRefSpPattern(String pat, GenomeData gdata) {
		List<HitInfo> hitResult = new ArrayList<HitInfo>();
		String refsp = this.getDrawer().coreGenome.getRefSp();
		if (refsp != null && refsp.length() > 0) {
			PatternMatcher matcher = new PatternMatcher(pat);
			logger.debug("refsp=" + refsp);
			Genome g = gdata.getGenome(refsp);
			for (Chromosome c : g.chromosomes) {
				String chrName = c.getChrName();
				logger.debug("chrName=" + chrName + ", seqno=" + c.seqno + ",c.length=" + c.getLength());
				if (chrName != null) {
					logger.debug("gdata.genomeSeq=" + gdata.genomeSeq);
					Sequence s = null;
					if (gdata.genomeSeq != null) {
						s = gdata.genomeSeq.getSubSequence(chrName, 0, c.getLength(), 1);
					} else {
						s = c.getSequence();
					}
					matcher.search(chrName, c.seqno, s, pat, 0, c.getLength(), c.getLength());
				}
				hitResult = matcher.getHitResult();
			}
		}
		return hitResult;
	}


	/**
	 * 全領域の検索。
	 * @param pat バターン。
	 * @param gdata ゲノムデータ。
	 * @return 検索結果。
	 */
	private List<HitInfo> searchDisplayRange(String pat, GenomeData gdata) {
		ArrayList<GenomeMapInfo> currGinfoList = this.getDrawer().getCurrGinfoList();
//		SeqRegion sr = this.getDrawer().viewRegion;
//		logger.debug("sr=" + JSON.encode(sr));
//		Region drawRegSp = this.getDrawer().drawRegSp;

		AlignmentCache ac = this.getDrawer().getAlignCache();
		PatternMatcher matcher = new PatternMatcher(pat);
		int cnt = this.getOption().getGdata().specNum();

		for (int spNo = 0; spNo < cnt; spNo++) {
			GenomeMapInfo ginfo = currGinfoList.get(spNo);
			String spec = ginfo.genome.getSpCode();
			Sequence sequence = ac.getSequence(spec);
			GenomicRegion gc = ac.getRegion(spec);
//			logger.debug("spec=" + spec + ",range=" + gc.begin() + ", " + gc.end());
			Genome g = gdata.getGenome(spec);
			Chromosome c = g.getChromosome(gc.seqno);
			int seqlen = sequence.length();
			int begin = gc.begin();
			int end = gc.end();
			int tlen = c.getLength();
			logger.debug("seqlen=" + seqlen + ",tlen=" + tlen + ",begin=" + begin + ",end=" + end + ",end-begin=" + (end - begin));
			int dir = ginfo.getChromDir();
			if (dir > 0) {
				matcher.search(spec, gc.seqno, sequence, pat, gc.begin(), gc.end(), c.getLength());
			} else {
				matcher.search(spec, gc.seqno, sequence.getReverse(), pat, gc.begin(), gc.end(), c.getLength());
			}
		}
		List<HitInfo> hitResult = matcher.getHitResult();
		return hitResult;
	}

	/**
	 * 全領域の検索。
	 * @param pat バターン。
	 * @param gdata ゲノムデータ。
	 * @return 検索結果。
	 */
	private List<HitInfo> searchAllRange(String pat, GenomeData gdata) {
		PatternMatcher matcher = new PatternMatcher(pat);
		for (String sp: gdata.specList) {
			Genome g = gdata.getGenome(sp);
			for (Chromosome c : g.chromosomes) {
				String chrName = c.getChrName();
				logger.debug("chrName=" + chrName + ", seqno=" + c.seqno + ",c.length=" + c.getLength());
				if (chrName != null) {
					logger.debug("gdata=" + gdata);
					logger.debug("gdata.genomeSeq=" + gdata.genomeSeq);
					Sequence s = null;
					if (gdata.genomeSeq != null) {
						s = gdata.genomeSeq.getSubSequence(chrName, 0, c.getLength(), 1);
					} else {
						s = c.getSequence();
					}
					matcher.search(chrName, c.seqno, s, pat, 0, c.getLength(), c.getLength());
				}
			}
		}
		List<HitInfo> hitResult = matcher.getHitResult();
		return hitResult;
	}

	/**
	 * 塩基配列パターンを検索する。
	 * @param type 検索タイプ。
	 * @param pat 塩基配列パターン。
	 * @return 検索結果。
	 */
	public List<HitInfo> searchPattern(int type, String pat) {
		GenomeData gdata = this.getOption().getGdata();
		List<HitInfo> hitResult = new ArrayList<HitInfo>();
		if (type == 0) {
			hitResult = this.searchRefSpPattern(pat, gdata);
		} else if (type == 1) {
			hitResult = this.searchDisplayRange(pat, gdata);
		} else if (type == 2) {
			hitResult = this.searchAllRange(pat, gdata);
		}
		return hitResult;
	}

	/**
	 * Geneの名称を検索する。
	 * @param name 名称。
	 * @return 検索結果。
	 */
	public List<String> searchGeneName(final String name, final boolean clustidMode) {
		if (clustidMode) {
			List<String[]> list =  this.searchClustId(name);
			List<String> ret = new ArrayList<String>();
			for (String[] r: list) {
				logger.debug("hit=" + r[0] + ", " + r[1]);
				ret.add(r[1] + "(" + r[0] + ")");
			}
			return ret;
		} else {
			List<String> ret = new ArrayList<String>();
			Pattern p = Pattern.compile(name);
			Map<String, Gene> map = this.getDrawer().genomeData.genes.nameHash;
			for (String k: map.keySet()) {
				Matcher m = p.matcher(k);
				if (m.find()) {
					logger.debug("k=" + k);
					Gene g = map.get(k);
					ret.add(g.sp + ":" + g.name);
				}
			}
			return ret;
		}
	}

	/**
	 * クラスタIDで検索します。
	 * @param name クラスタID。
	 * @return 検索結果。
	 */
	private List<String[]> searchClustId(final String name) {
		CoreGenome coreGenome = this.getDrawer().coreGenome;
		List<String[]> list = coreGenome.searchClustid(name);
		list.addAll(this.getDrawer().getIsland().searchClustid(name));
		return list;
	}

	/**
	 * 生物種コードのリストを取得する。
	 * @return 生物種コードのリスト。
	 */
	public List<String> getSpCodeList() {
		List<String> ret = new ArrayList<String>();
		for (GenomeMapInfo ginfo : this.getDrawer().currGinfoList) {
			logger.debug("sp=" + ginfo.genome.spcode);
			ret.add(ginfo.genome.spcode);
		}
		return ret;
	}


	/**
	 * GenimeMapInfoを取得する。
	 * @param name 名称。
	 * @param list リスト。
	 * @return GenimeMapInfo。
	 */
	private GenomeMapInfo getGenomeMapInfo(final String name, final ArrayList<GenomeMapInfo> list) {
		GenomeMapInfo ret = null;
		for (GenomeMapInfo ginfo : list) {
			if (name.equals(ginfo.genome.spcode)) {
				ret = ginfo;
				break;
			}
		}
		return ret;
	}


	/**
	 * 表示順を変更する。
	 */
	public void setSequence( DefaultListModel<String> listModel) {
		ArrayList<GenomeMapInfo> newList = new ArrayList<GenomeMapInfo>();
		for (int i = 0; i < listModel.size(); i++) {
			String name = listModel.get(i);
			GenomeMapInfo ret = this.getGenomeMapInfo(name, this.getDrawer().currGinfoList);
			if (ret != null) {
				newList.add(ret);
			}
		}
		this.getDrawer().currGinfoList.clear();
		for (GenomeMapInfo gminfo: newList) {
			this.getDrawer().currGinfoList.add(gminfo);
		}

		for (GenomeMapInfo ginfo : this.getDrawer().currGinfoList) {
			logger.debug("updated sp=" + ginfo.genome.spcode);
		}
		this.getDrawer().drawData();
		this.repaint();
	}

	@Override
	public int print(Graphics g0, PageFormat pf, int page) throws PrinterException {
		logger.info("print page=" + page);
		int cnt = this.drawer.currGinfoList.size();
		int pages = cnt / 24;
		if ((cnt % 24) > 0) {
			pages++;
		}
		logger.info("cnt=" + cnt + ", page=" + page + ", pages=" + pages);
		if (page >= pages) {
			return Printable.NO_SUCH_PAGE;
		}
		Graphics2D g = (Graphics2D) g0;
		double w = pf.getImageableWidth();
		double h = pf.getImageableHeight();
		logger.info("print w=" + w + ", h=" + h);
/*		int idx = 0;
		for (GenomeMapInfo ginf: this.drawer.currGinfoList) {
			logger.info("idx=" + idx + ", sp=" + ginf.getGenome().spcode);
			idx++;
		}
*/
		drawer.setGraphics(g);
		drawer.setParametersByPaperSize((int) w, (int) h);
		drawer.printData(page);

		return Printable.PAGE_EXISTS;
	}


	private void print() {
		PrinterJob pj = PrinterJob.getPrinterJob();
		pj.setPrintable(this);
		if (pj.printDialog()) {
			try {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				pj.print();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}

}

