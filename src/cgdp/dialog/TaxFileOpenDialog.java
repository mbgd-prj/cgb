package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CompareMapOpt;
import cgdp.util.ConfFileUtil;
import cgdp.util.UserConfUtil;

/**
 * ファイル読み込みダイアログ。
 *
 */
public class TaxFileOpenDialog extends JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = -8442264909331210414L;

	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(TaxFileOpenDialog.class);

	/**
	 * ファイル選択ボタンの幅。
	 */
	private static final int SELECT_BUTTON_WIDTH = 50;
	/**
	 * ファイル選択ボタンの高さ。
	 */
	private static final int SELECT_BUTTON_HEIGHT = 20;
	/**
	 * 行の高さ。
	 */
	private static final int ROW_HEIGHT = 26;
	/**
	 * 内容パネル。
	 */
	private final JPanel contentPanel = new JPanel();
	/**
	 * データパスフィールド。
	 */
	private JRadioButton confFileRadioButton = null;
	private JRadioButton dataDirectoryRadioButton = null;
	private ButtonGroup typeButtonGroup = null;
	private JTextField dataPathField = null;
	private JTextField coreFileField = null;
	private JButton coreButton = null;
	private JTextField geneFileField;
	private JButton geneButton = null;
	private JTextField islandFileField;
	private JButton islandButton = null;
	private JTextField dnaSeqFileField;
	private JButton dnaSeqButton = null;
	private JTextField orderFileField;
	private JButton orderButton = null;
	private JTextField altnameFieldField;
	private JButton altnamesButton = null;
	private JButton writeConfFileButton = null;


	/**
	 * ファイルリストコンポーネント。
	 */
	private class FileListComponent {
		/**
		 * ラベル。
		 */
		private String label = null;
		/**
		 * ファイルの拡張子。
		 */
		private String ext = null;
		/**
		 * 追加ボタン。
		 */
		private JButton addButton = null;
		/**
		 * 削除ボタン。
		 */
		private JButton delButton = null;
		/**
		 * ファイルリストモデル。
		 */
		private DefaultListModel<String> fileListModel = null;
		/**
		 * ファイルリスト。
		 */
		private JList<String> fileList = null;

		/**
		 * コンストラクタ。
		 * @param label ラベル。
		 * @param ext ファイルの拡張子。
		 */
		public FileListComponent(String label, String ext) {
			this.label = label;
			this.ext = ext;
		}

		/**
		 * ファイルを選択する。
		 * @param list 選択したファイルを追加するリスト。
		 * @param fileType ファイルタイプリスト。
		 */
		private void selectFile(final DefaultListModel<String> list, final String... fileType) {
			String path = TaxFileOpenDialog.this.dataPathField.getText();
			JFileChooser dlg = new JFileChooser(path);
			String types = "";
			for (String t: fileType) {
				if (types.length() > 0) {
					types += ", ";
				}
				types += "*." + t;
			}
			dlg.addChoosableFileFilter(new FileNameExtensionFilter("Status File(" + types + ")", fileType));
			dlg.setAcceptAllFileFilterUsed(true);
			dlg.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int selected = dlg.showOpenDialog(TaxFileOpenDialog.this);
			if (selected  == JFileChooser.APPROVE_OPTION) {
				File f = dlg.getSelectedFile();
				String parentPath = f.getParentFile().getAbsolutePath();
				if (parentPath.equals(path)) {
					list.addElement(f.getName());
				} else {
					list.addElement(f.getAbsolutePath());
				}
			}
		}

		/**
		 * Otherファイルリストの操作ボタンの制御。
		 */
		private void setButtonStatus() {
			this.addButton.setEnabled(true);
			int[] sellist = this.fileList.getSelectedIndices();
			if (sellist.length > 0) {
				this.delButton.setEnabled(true);
			} else {
				this.delButton.setEnabled(false);
			}
		}

		/**
		 * Otherファイルリストを削除する。
		 */
		private void deleteIslandFile() {
			int[] sellist = this.fileList.getSelectedIndices();
			List<String> list = new ArrayList<String>();
			for (int idx: sellist) {
				logger.debug("selidx=" + idx);
				String name = this.fileListModel.get(idx);
				list.add(name);
			}
			for (String name: list) {
				this.fileListModel.removeElement(name);
			}
		}

		/**
		 * コンポーネントを配置する。
		 * @param ypos 配置するy座標。
		 * @return 次の配置に使用するy座標。
		 */
		public int addComponent(int ypos) {
			JLabel lblNewLabel = new JLabel(this.label);
			lblNewLabel.setBounds(12, ypos, 108, 13);
			TaxFileOpenDialog.this.contentPanel.add(lblNewLabel);
			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setBounds(117, ypos - 4, 377, 62);
			TaxFileOpenDialog.this.contentPanel.add(scrollPane);
			this.fileListModel = new DefaultListModel<String>();
			this.fileList = new JList<String>(this.fileListModel);
			// ファイルリストの選択イベント
			// ボタンのDisable/Enableを切り替える。
			this.fileList.addListSelectionListener((ListSelectionEvent e) -> {
				this.setButtonStatus();
			});
			scrollPane.setViewportView(this.fileList);
			this.addButton = new JButton("+");
			this.addButton.addActionListener((ActionEvent e) -> {
				logger.debug("addFileButton");
				this.selectFile(this.fileListModel, this.ext);
			});
			this.addButton.setBounds(506, ypos - 4, SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT);
			TaxFileOpenDialog.this.contentPanel.add(this.addButton);

			this.delButton = new JButton("-");
			this.delButton.addActionListener((ActionEvent e) -> {
				logger.debug("delFileButton");
				this.deleteIslandFile();
			});
			this.delButton.setBounds(506, ypos - 4 + ROW_HEIGHT, SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT);
			TaxFileOpenDialog.this.contentPanel.add(this.delButton);
			// 最初は削除ボタンは無効にする。
			this.setButtonStatus();

			return ypos + 68;
		}

		/**
		 * ファイルリストを設定する。
		 * @param fileList ファイルリスト。
		 */
		public void setFileList(final List<String> fileList) {
			this.fileListModel.clear();
			if (fileList != null) {
				for (String file: fileList) {
					this.fileListModel.addElement(file);
				}
			}
		}

		/**
		 * ファイルリストを取得する。
		 * @return ファイルリスト。
		 */
		public List<String> getFileList() {
			List<String> list = new ArrayList<String>();
			for (int i = 0; i < this.fileListModel.size(); i++) {
				String file = this.fileListModel.get(i);
				list.add(file);
			}
			return list;
		}
	}

	/**
	 * その他ファイルコンポーネント。
	 */
	private FileListComponent otherComponent = null;
	/**
	 * 遺伝子集合ファイルコンポーネント。
	 */
	private FileListComponent geneSetComponent = null;
	/**
	 * 特徴領域ファイルコンポーネント。
	 */
	private FileListComponent segmentComponent = null;
	/**
	 * アノテーションファイルコンポーネント。
	 */
	private FileListComponent annotationComponent = null;



	/**
	 * Viewer。
	 */
	private ComparativeMapViewer viewer= null;

	/**
	 * Create the dialog.
	 * @param viwer ダイアログタイプ。
	 */
	public TaxFileOpenDialog(final ComparativeMapViewer viewer) {
		setTitle("Open input files");
		this.viewer = viewer;

		this.setBounds(100, 100, 580, 568);
		this.getContentPane().setLayout(new BorderLayout());
		this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
		this.contentPanel.setLayout(null);
		int ypos = 10;
		{
			// Conf file / Data directoryのラジオボタン
			JLabel lblNewLabel_14 = new JLabel("Type : ");
			lblNewLabel_14.setBounds(12, ypos, 50, 13);
			contentPanel.add(lblNewLabel_14);
			this.typeButtonGroup = new ButtonGroup();
			this.confFileRadioButton = new JRadioButton("Conf file");
			this.confFileRadioButton.addActionListener((ActionEvent e) ->{
				TaxFileOpenDialog.this.setButtonStatus(false);
			});
			this.confFileRadioButton.setSelected(true);
			this.confFileRadioButton.setBounds(117, ypos - 4, 87, 21);
			this.contentPanel.add(this.confFileRadioButton);
			this.dataDirectoryRadioButton = new JRadioButton("Data directory");
			this.dataDirectoryRadioButton.addActionListener((ActionEvent e) ->{
				TaxFileOpenDialog.this.setButtonStatus(true);
			});
			this.dataDirectoryRadioButton.setBounds(300, ypos - 4, 141, 21);
			this.contentPanel.add(this.dataDirectoryRadioButton);
			this.typeButtonGroup.add(this.confFileRadioButton);
			this.typeButtonGroup.add(this.dataDirectoryRadioButton);
		}
		ypos += ROW_HEIGHT;
		{
			// 読み込み対象のファイル,ディレクトリの指定
			JLabel lblNewLabel = new JLabel("Path(*) :");
			lblNewLabel.setBounds(12, ypos, 63, 13);
			this.contentPanel.add(lblNewLabel);

			this.dataPathField = new JTextField();
			this.dataPathField.setBounds(117, ypos - 4, 377, 19);
			this.contentPanel.add(this.dataPathField);
			this.dataPathField.setColumns(10);

			JButton pathButton = new JButton("...");
			pathButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					TaxFileOpenDialog.this.selectDataPath();
				}
			});
			pathButton.setBounds(506, ypos - 4, SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT);
			this.contentPanel.add(pathButton);
		}
		ypos += ROW_HEIGHT;
		{
			// Coreファイル指定
			JLabel coreLabel = new JLabel("Core :");
			coreLabel.setBounds(12, ypos, 50, 13);
			this.contentPanel.add(coreLabel);
			this.coreFileField = new JTextField();
			// this.coreFileField.setEditable(false);
			this.coreFileField.setBounds(117, ypos - 4, 377, 19);
			this.contentPanel.add(this.coreFileField);
			this.coreFileField.setColumns(10);
			this.coreButton = new JButton("...");
			this.coreButton.addActionListener((ActionEvent e) -> {
				TaxFileOpenDialog.this.selectFile(this.coreFileField, "coaln");
			});
			this.coreButton.setEnabled(false);
			this.coreButton.setBounds(506, ypos - 4, SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT);
			this.contentPanel.add(this.coreButton);

		}

		ypos += ROW_HEIGHT;
		{
			JLabel islandLabel = new JLabel("Island : ");
			islandLabel.setBounds(12, ypos, 50, 13);
			this.contentPanel.add(islandLabel);
			this.islandFileField = new JTextField();
			// this.islandFileField.setEditable(false);
			this.islandFileField.setBounds(117, ypos - 4, 377, 19);
			this.contentPanel.add(this.islandFileField);
			this.islandFileField.setColumns(10);
			this.islandButton = new JButton("...");
			this.islandButton.setEnabled(false);
			this.islandButton.addActionListener((ActionEvent e) -> {
				TaxFileOpenDialog.this.selectFile(this.islandFileField, "isl");
			});
			this.islandButton.setBounds(506, ypos - 4, SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT);
			this.contentPanel.add(this.islandButton);
		}
		ypos += ROW_HEIGHT;
		{
			this.otherComponent = new FileListComponent("Other :", "other");
			ypos = this.otherComponent.addComponent(ypos);
		}
		{
			JLabel ganeLabel = new JLabel("Gene :");
			ganeLabel.setBounds(12, ypos, 50, 13);
			this.contentPanel.add(ganeLabel);
			this.geneFileField = new JTextField();
			// this.geneFileField.setEditable(false);
			this.geneFileField.setBounds(117, ypos - 4, 377, 19);
			this.contentPanel.add(this.geneFileField);
			this.geneFileField.setColumns(10);
			this.geneButton = new JButton("...");
			this.geneButton.setEnabled(false);
			this.geneButton.addActionListener((ActionEvent e) -> {
				TaxFileOpenDialog.this.selectFile(this.geneFileField, "genetab");
			});
			this.geneButton.setBounds(506, ypos - 4, SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT);
			this.contentPanel.add(this.geneButton);
		}
		ypos += ROW_HEIGHT;
		{
			JLabel dnaSeqLabel = new JLabel("DNA seq. :");
			dnaSeqLabel.setBounds(12, ypos, 76, 13);
			this.contentPanel.add(dnaSeqLabel);
			this.dnaSeqFileField = new JTextField();
			// this.dnaSeqFileField.setEditable(false);
			this.dnaSeqFileField.setBounds(117, ypos - 4, 377, 19);
			this.contentPanel.add(this.dnaSeqFileField);
			this.dnaSeqFileField.setColumns(10);
			this.dnaSeqButton = new JButton("...");
			this.dnaSeqButton.setEnabled(false);
			this.dnaSeqButton.addActionListener((ActionEvent e) -> {
				TaxFileOpenDialog.this.selectFile(this.dnaSeqFileField, "dnaseq", "fas");
			});
			this.dnaSeqButton.setBounds(506, ypos - 4, SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT);
			this.contentPanel.add(this.dnaSeqButton);
		}
		ypos += ROW_HEIGHT;
		{
			JLabel lblNewLabel_10 = new JLabel("Order file :");
			lblNewLabel_10.setBounds(12, ypos, 76, 13);
			this.contentPanel.add(lblNewLabel_10);
			this.orderFileField = new JTextField();
			// this.orderFileField.setEditable(false);
			this.orderFileField.setBounds(117, ypos - 4, 377, 19);
			this.contentPanel.add(orderFileField);
			this.orderFileField.setColumns(10);
			this.orderButton = new JButton("...");
			this.orderButton.addActionListener((ActionEvent e) -> {
				TaxFileOpenDialog.this.selectFile(this.orderFileField, "order");
			});
			this.orderButton.setBounds(506,  ypos - 4, SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT);
			this.orderButton.setEnabled(false);
			this.contentPanel.add(this.orderButton);
		}
		ypos += ROW_HEIGHT;
		{
			JLabel lblNewLabel_11 = new JLabel("Alt name file :");
			lblNewLabel_11.setBounds(12, ypos, 108, 13);
			this.contentPanel.add(lblNewLabel_11);
			this.altnameFieldField = new JTextField();
			// this.altnameFieldField.setEditable(false);
			this.altnameFieldField.setBounds(117, ypos - 4, 377, 19);
			this.contentPanel.add(this.altnameFieldField);
			this.altnameFieldField.setColumns(10);
			this.altnamesButton = new JButton("...");
			this.altnamesButton.addActionListener((ActionEvent e) -> {
				TaxFileOpenDialog.this.selectFile(this.altnameFieldField, "altnames");
			});
			this.altnamesButton.setBounds(506, ypos - 4, SELECT_BUTTON_WIDTH, SELECT_BUTTON_HEIGHT);
			this.altnamesButton.setEnabled(false);
			this.contentPanel.add(this.altnamesButton);
		}
		ypos += ROW_HEIGHT;
		{
			this.geneSetComponent = new FileListComponent("Gene set :", "genes");
			ypos = this.geneSetComponent.addComponent(ypos);
		}
		{
			this.segmentComponent = new FileListComponent("Segment :", "seg");
			ypos = this.segmentComponent.addComponent(ypos);
		}
		{
			this.annotationComponent = new FileListComponent("Annotation :", "ann");
			ypos = this.annotationComponent.addComponent(ypos);
		}
		this.addButtonPane();
		// this.setIslandListButtonStatus();
	}

	/**
	 * ファイルのパスを取得します。
	 * @param parentPath データディレクトリのパス。
	 * @param file ファイルのパス。
	 * @return	fileが絶対パスの場合そのまま返します。fileが相対パスの場合、parentPathとfileを結合したパスを返します。
	 */
	private String getFilePath(String parentPath, String file) {
		logger.debug("getFilePath: parentPath=" + parentPath + ", file=" + file);
		File f = new File(file);
		if (f.isAbsolute()) {
			return file;
		} else {
			File parentDir = new File(parentPath);
			return  parentDir.getName() + File.separator + file;
		}
	}

	/**
	 * conffileを作成します。
	 * @throws Exception 例外。
	 */
	private void writeConfFile() throws Exception {
		String path = this.dataPathField.getText();
		logger.debug("path=" + path);
		File dir = new File(path);
		String name = path; // dir.getName();
		File pdir = dir.getParentFile();
		String conffile = pdir.getAbsolutePath() + File.separator + "conffile." + dir.getName();
		logger.debug("conffile=" + conffile);
		String corefile = this.coreFileField.getText();
		String islandFile = this.islandFileField.getText();
		List<String> otherFileList = this.otherComponent.getFileList();
		String geneFile = this.geneFileField.getText();
		String dnaSeqFile = this.dnaSeqFileField.getText();
		String orderFile = this.orderFileField.getText();
		String altNameFile = this.altnameFieldField.getText();
		List<String> geneSetFileList = this.geneSetComponent.getFileList();
		List<String> segmentFileList = this.segmentComponent.getFileList();
		List<String> annotationFileList = this.annotationComponent.getFileList();
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotEmpty(corefile)) {
			sb.append(ConfFileUtil.COREFILE + "=" + this.getFilePath(name, corefile) + "\n");
		}
		if (StringUtils.isNotEmpty(geneFile)) {
			sb.append(ConfFileUtil.GENEFILE + "=" + this.getFilePath(name, geneFile) + "\n");
		}
		if (StringUtils.isNotEmpty(islandFile)) {
			sb.append(ConfFileUtil.ISLFILE + "=" + this.getFilePath(name, islandFile) + "\n");
		}
		for (String otherFile: otherFileList) {
			sb.append(ConfFileUtil.OTHERFILE + "=" + this.getFilePath(name, otherFile) + "\n");
		}
		if (StringUtils.isNotEmpty(dnaSeqFile)) {
			sb.append(ConfFileUtil.SEQFILE + "=" + this.getFilePath(name, dnaSeqFile) + "\n");
		}
		if (StringUtils.isNotEmpty(orderFile)) {
			sb.append(ConfFileUtil.ORDERFILE + "=" + this.getFilePath(name, orderFile) + "\n");
		}
		if (StringUtils.isNotEmpty(altNameFile)) {
			sb.append(ConfFileUtil.ALTNAMEFILE + "=" + this.getFilePath(name, altNameFile) + "\n");
		}
		for (String geneSetFile: geneSetFileList) {
			sb.append(ConfFileUtil.GENESETFILE +"=" + this.getFilePath(name, geneSetFile) + "\n");
		}
		for (String segmentFile: segmentFileList) {
			sb.append(ConfFileUtil.SEGMENTFILE + "=" + this.getFilePath(name, segmentFile) + "\n");
		}
		for (String annotationFile: annotationFileList) {
			sb.append(ConfFileUtil.ANNOTATIONFILE + "=" + this.getFilePath(name, annotationFile) + "\n");
		}
		logger.debug("conffile=\n" + sb.toString());

		File f = new File(conffile);
		try (java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(f))) {
			bw.write(sb.toString());
		}
	}


	/**
	 * OK/Cancelボタンを配置する。
	 */
	private void addButtonPane() {
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		{
			writeConfFileButton = new JButton("Write conf file");
			writeConfFileButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						TaxFileOpenDialog.this.writeConfFile();
					} catch (Exception ex) {
						logger.error(ex.getMessage(), ex);
					}
				}
			});
			writeConfFileButton.setActionCommand("WriteConfFile");
			buttonPane.add(writeConfFileButton);

		}
		{
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						if (TaxFileOpenDialog.this.openFile()) {
							TaxFileOpenDialog.this.dispose();
						}
					} catch (Exception ex) {
						logger.error(ex.getMessage(), ex);
					}
				}
			});
			okButton.setActionCommand("OK");
			buttonPane.add(okButton);
			getRootPane().setDefaultButton(okButton);
		}
		{
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					TaxFileOpenDialog.this.dispose();
				}
			});
			cancelButton.setActionCommand("Cancel");
			buttonPane.add(cancelButton);
		}
	}

	/**
	 * ボタンの状態を設定します。
	 * @param dirFlag データディレクトリを選択している場合true。
	 */
	private void setButtonStatus(boolean dirFlag) {
		logger.debug("setButtonStatus: dirFlag=" + dirFlag);
		this.coreButton.setEnabled(true);
		this.geneButton.setEnabled(true);
		this.islandButton.setEnabled(true);
		this.dnaSeqButton.setEnabled(true);
		this.orderButton.setEnabled(true);
		this.altnamesButton.setEnabled(true);
//		this.setIslandListButtonStatus();
		if (dirFlag) {
			try {
				String dataPath = UserConfUtil.get(UserConfUtil.DATA_PATH);
				logger.debug("read data directory: " + dataPath);
				this.dataPathField.setText(dataPath);
				this.readDataDirectory(dataPath);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			this.writeConfFileButton.setEnabled(true);
		} else {
			this.writeConfFileButton.setEnabled(false);
		}
	}

	/**
	 * ファイルを選択する。
	 * @param field 選択したファイルを設定するフィールド。
	 * @param fileType ファイルタイプリスト。
	 */
	private void selectFile(final JTextField field, final String... fileType) {
		String path = this.dataPathField.getText();
		JFileChooser dlg = new JFileChooser(path);
		String types = "";
		for (String t: fileType) {
			if (types.length() > 0) {
				types += ", ";
			}
			types += "*." + t;
		}
		dlg.addChoosableFileFilter(new FileNameExtensionFilter("Status File(" + types + ")", fileType));
		dlg.setAcceptAllFileFilterUsed(true);
		dlg.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int selected = dlg.showOpenDialog(this);
		if (selected  == JFileChooser.APPROVE_OPTION) {
			File f = dlg.getSelectedFile();
			field.setText(f.getAbsolutePath());
		}
	}

	/**
	 * オプションの値を設定する。
	 * <pre>
	 * 読み込んでいるファイル情報をダイアログに転記します。
	 * </pre>
	 * @throws Exception 例外。
	 */
	public void setOption() throws Exception {
		CompareMapOpt opt = this.viewer.getOption();
		logger.debug("getRefsp:" + opt.getRefsp());
		if (opt.isConfFile()) {
			this.confFileRadioButton.getModel().setSelected(true);
		} else {
			this.dataDirectoryRadioButton.getModel().setSelected(true);
		}
		this.dataPathField.setText(opt.getDataPath());
		this.coreFileField.setText(opt.getCorefile());
		this.geneFileField.setText(opt.getGenefile());
		this.islandFileField.setText(opt.getIslandFile());
		this.otherComponent.setFileList(opt.getOtherFileList());
		this.dnaSeqFileField.setText(opt.getDnaSeqFile());
		this.orderFileField.setText(opt.getOrderfile());
		this.altnameFieldField.setText(opt.getAltNameFile());

		this.geneSetComponent.setFileList(opt.getGeneSetFileList());
		this.segmentComponent.setFileList(opt.getSegmentFileList());
		this.annotationComponent.setFileList(opt.getAnnotationFileList());

		this.setButtonStatus(!opt.isConfFile());
	}

	/**
	 * フィールドのテキストを取得する。
	 * @param field フィールド。
	 * @return テキスト。
	 */
	private String getText(JTextField field) {
		if (field.getText().length() == 0) {
			return null;
		} else {
			return field.getText();
		}
	}

	/**
	 * optionの値を取得する。
	 * <pre>
	 * ダイアログの内容をoptionに転記します。
	 * </pre>
	 *
	 * @return option.
	 */
	private CompareMapOpt getOpt() {
		CompareMapOpt opt = this.viewer.getOption();
		if (this.confFileRadioButton.isSelected()) {
			opt.setConfFile(true);
		} else {
			opt.setConfFile(false);
		}
		opt.setDataPath(this.getText(this.dataPathField));
		opt.setCorefile(this.getText(this.coreFileField));
		opt.setGenefile(this.getText(this.geneFileField));
		opt.setIslandFile(this.getText(this.islandFileField));
		opt.setDnaSeqFile(this.getText(this.dnaSeqFileField));
		opt.setOrderfile(this.getText(this.orderFileField));
		opt.setAltNameFile(this.getText(this.altnameFieldField));
		opt.setOtherFileList(this.otherComponent.getFileList());
		opt.setGeneSetFileList(this.geneSetComponent.getFileList());
		opt.setSegmentFileList(this.segmentComponent.getFileList());
		opt.setAnnotationFileList(this.annotationComponent.getFileList());

		opt.dump();

		return opt;
	}

	public static final java.awt.Color ERROR_COLOR = java.awt.Color.PINK;


	/**
	 * 空白かどうかをチェックする。
	 * @param field フィールド。
	 * @return 空白の場合true。
	 */
	private boolean isBlank(JTextField field) {
		String text = field.getText();
		if (text.length() == 0) {
			return true;
		}
		return false;
	}


	/**
	 * データの確認。
	 * @return 問題なければtrue。
	 */
	private boolean validateData() {
		this.dataPathField.setBackground(java.awt.Color.WHITE);

		String message = "";
		boolean ret = true;
		if (this.isBlank(this.dataPathField)) {
			this.dataPathField.setBackground(ERROR_COLOR);
			message += "Path is required.\n";
			ret = false;
		}
		if (!ret) {
			JOptionPane.showMessageDialog(this, message);
		}
		return ret;
	}

	/**
	 * ファイルをオープンする。
	 */
	private boolean openFile() throws Exception {
		if (!this.validateData()) {
			return false;
		}
		try {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			CompareMapOpt opt = this.getOpt();
			if (opt != null) {
				logger.debug("openFile coreFile=" + opt.getCorefile());
				opt.readData();
				this.viewer.getDrawer().setOpt(opt);
				logger.debug("isBlank=" + opt.getCoreGenome().isBlank());
				this.viewer.repaint();
				this.viewer.setCenterPos();
			}
		} finally {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		return true;
	}

	/**
	 * データのパスを選択します。
	 */
	private void selectDataPath() {
		try {
			JFileChooser filechooser = new JFileChooser();
			String dataPath = UserConfUtil.get(UserConfUtil.DATA_PATH);
			if (dataPath != null) {
				filechooser.setCurrentDirectory(new File(dataPath));
			}
			if (this.confFileRadioButton.isSelected()) {
				filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			} else {
				filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}
			int selected = filechooser.showOpenDialog(this);
			if (selected  == JFileChooser.APPROVE_OPTION) {
				File file = filechooser.getSelectedFile();
				logger.debug("selected file=" + file.getAbsolutePath());
				String userhome = System.getProperty("user.home");
				logger.debug("userhome=" + userhome);
				this.dataPathField.setText(file.getAbsolutePath());
				if (this.confFileRadioButton.isSelected()) {
					UserConfUtil.set(UserConfUtil.DATA_PATH, file.getParent());
					this.readConfFile(file.getAbsolutePath());
				} else {
					UserConfUtil.set(UserConfUtil.DATA_PATH, file.getAbsolutePath());
					this.readDataDirectory(file.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * 各種ファイルの情報を表示する。
	 * @param conf 各種ファイルの情報マップ。
	 */
	private void setConfMap(Map<String, Object> conf) {
		this.coreFileField.setText((String) conf.get(ConfFileUtil.COREFILE));
		this.geneFileField.setText((String) conf.get(ConfFileUtil.GENEFILE));
		this.dnaSeqFileField.setText((String) conf.get(ConfFileUtil.SEQFILE));
		this.islandFileField.setText((String) conf.get(ConfFileUtil.ISLFILE));
		this.orderFileField.setText((String) conf.get(ConfFileUtil.ORDERFILE));
		this.altnameFieldField.setText((String) conf.get(ConfFileUtil.ALTNAMEFILE));
		{
			@SuppressWarnings("unchecked")
			List<String> otherList = (List<String>) conf.get(ConfFileUtil.OTHERFILE);
			this.otherComponent.setFileList(otherList);
		}
		{
			@SuppressWarnings("unchecked")
			List<String> geneSetList = (List<String>) conf.get(ConfFileUtil.GENESETFILE);
			this.geneSetComponent.setFileList(geneSetList);
		}
		{
			@SuppressWarnings("unchecked")
			List<String> segmentList = (List<String>) conf.get(ConfFileUtil.SEGMENTFILE);
			this.segmentComponent.setFileList(segmentList);
		}
		{
			@SuppressWarnings("unchecked")
			List<String> annotationList = (List<String>) conf.get(ConfFileUtil.ANNOTATIONFILE);
			this.annotationComponent.setFileList(annotationList);
		}
	}

	/**
	 * conffileを取得します。
	 * @param path 選択したパス。
	 * @return 先頭のconffile。
	 * @throws Exception 例外。
	 */
	private void readConfFile(final String conffile) throws Exception {
		Map<String, Object> conf = ConfFileUtil.readConfFile(conffile);
		this.setConfMap(conf);
	}

	/**
	 * ディレクトリ中のファイルを取得します。
	 * @param path ディレクトリのパス。
	 * @throws Exception 例外。
	 */
	private void readDataDirectory(final String path) throws Exception {
		Map<String, Object> conf = ConfFileUtil.readDataDirectory(path);
		logger.debug("conf=" + conf);
		this.setConfMap(conf);
	}
}
