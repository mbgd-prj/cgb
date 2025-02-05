package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.component.UserTable;
import cgdp.component.UserTableModel;
import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CompareMapOpt.ClusterGroup;
import cgdp.corealign.CoreGenome;
import cgdp.corealign.CoreGenome.BlockInfo;
import cgdp.corealign.GenomeData;
import cgdp.corealign.GenomeData.GeneInfo;

/**
 * ブロック追加ダイアログ。
 *
 */
public class AddGroupDialog extends JDialog {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 1L;

	private static Logger logger = LogManager.getLogger(AddGroupDialog.class);

	public interface GroupInfo {
		boolean isSelect();
	}

	/**
	 * ブロック情報テーブルモデル。
	 *
	 */
	private class BlockInfoTableModel extends UserTableModel<BlockInfo> {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public static final String SELECT = "Select";
		public static final String TYPE = "Type";
		public static final String BLOCK_NO = "Block No";
		public static final String CLUSTER_COUNT = "Cluster count";
		/**
		 * コンストラクタ。
		 * @param dataList データリスト。
		 */
		public BlockInfoTableModel(List<BlockInfo> dataList) {
			super(dataList);
			this.addColumnInfo(new ColumnInfo(SELECT, "select", true, boolean.class));
			this.addColumnInfo(new ColumnInfo(TYPE, "blockType", false, String.class));
			this.addColumnInfo(new ColumnInfo(BLOCK_NO, "blockNo", false, String.class));
			this.addColumnInfo(new ColumnInfo(CLUSTER_COUNT, "clusterCount", false, Integer.class));
		}
	}

	/**
	 * ブロック情報テーブルモデル。
	 *
	 */
	private class GeneInfoTableModel extends UserTableModel<GeneInfo> {


		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public static final String SELECT = "Select";
		public static final String SPECIES = "Species";
		public static final String NAME = "Name";
		public static final String POSITION = "Position";
		public static final String DIR = "Dir";
		public static final String LENGTH = "Length";

		/**
		 * コンストラクタ。
		 * @param dataList データリスト。
		 */
		public GeneInfoTableModel(List<GeneInfo> dataList) {
			super(dataList);
			this.addColumnInfo(new ColumnInfo(SELECT, "select", true, boolean.class));
			this.addColumnInfo(new ColumnInfo(SPECIES, "sp", false, String.class));
			this.addColumnInfo(new ColumnInfo(NAME, "name", false, String.class));
			this.addColumnInfo(new ColumnInfo(POSITION, "pos", false, Float.class));
			this.addColumnInfo(new ColumnInfo(DIR, "dir", false, Float.class));
			this.addColumnInfo(new ColumnInfo(LENGTH, "len", false, Float.class));
		}
	}

	public class BlockOrGeneInfoTable extends UserTable {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		public BlockOrGeneInfoTable() {
			this.setModel(new BlockInfoTableModel(new ArrayList<BlockInfo>()));
		}

		@Override
		public void setModel(TableModel dataModel) {
			super.setModel(dataModel);
			try {
				if (dataModel instanceof BlockInfoTableModel) {
						this.getColumn(BlockInfoTableModel.SELECT).setPreferredWidth(60);
						this.getColumn(BlockInfoTableModel.TYPE).setPreferredWidth(60);
						this.getColumn(BlockInfoTableModel.BLOCK_NO).setPreferredWidth(160);
						this.getColumn(BlockInfoTableModel.CLUSTER_COUNT).setPreferredWidth(160);
				} else {
					this.getColumn(GeneInfoTableModel.SELECT).setPreferredWidth(60);
					this.getColumn(GeneInfoTableModel.SPECIES).setPreferredWidth(80);
					this.getColumn(GeneInfoTableModel.NAME).setPreferredWidth(140);
					this.getColumn(GeneInfoTableModel.POSITION).setPreferredWidth(80);
					this.getColumn(GeneInfoTableModel.DIR).setPreferredWidth(60);
					this.getColumn(GeneInfoTableModel.LENGTH).setPreferredWidth(60);
				}
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
		}
	}

	private List<? extends GroupInfo> result = null;


	/**
	 * パネル。
	 */
	private final JPanel contentPanel = new JPanel();
	private JScrollPane scrollPane =null;
	private JTable table;
	private JRadioButton blockRadioButton;
	private JRadioButton geneRadioButton;
	private ButtonGroup typeButtonGroup = null;
	private ComparativeMapViewer viewer = null;
	private JTextField blockNoFromField;
	private JTextField geneNameField;
	private JTextField groupNameField;
	private JTextField colorField;
	private JTextField blockNoToField;
	private JCheckBox visibleCheckBox = null;
	private JButton okButton = null;


	/**
	 * Create the dialog.
	 */
	public AddGroupDialog(final ComparativeMapViewer viewer) {
		this.viewer = viewer;
		setTitle("Add block to group");
		setBounds(100, 100, 453, 442);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		this.scrollPane = new JScrollPane();
		this.scrollPane.setBounds(12, 83, 413, 208);
		contentPanel.add(scrollPane);

		this.table = new BlockOrGeneInfoTable();
		this.scrollPane.setViewportView(this.table);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1) {
					AddGroupDialog.this.changeStatus();
				}
			}
		});

		this.blockRadioButton = new JRadioButton("Block");
		blockRadioButton.addActionListener((ActionEvent e) -> {
			AddGroupDialog.this.changeStatus();
		});
		this.blockRadioButton.setBounds(120, 6, 88, 21);
		this.contentPanel.add(this.blockRadioButton);

		this.geneRadioButton = new JRadioButton("Gene");
		geneRadioButton.addActionListener((ActionEvent e) -> {
			AddGroupDialog.this.changeStatus();
		});
		this.geneRadioButton.setBounds(226, 6, 97, 21);
		contentPanel.add(this.geneRadioButton);

		this.typeButtonGroup = new ButtonGroup();
		this.typeButtonGroup.add(this.blockRadioButton);
		this.typeButtonGroup.add(this.geneRadioButton);
		this.blockRadioButton.setSelected(true);

		JLabel lblNewLabel_1 = new JLabel("Block no. :");
		lblNewLabel_1.setBounds(12, 31, 75, 13);
		contentPanel.add(lblNewLabel_1);

		JLabel lblNewLabel_2 = new JLabel("Gene name :");
		lblNewLabel_2.setBounds(12, 55, 75, 13);
		contentPanel.add(lblNewLabel_2);

		JButton btnNewButton = new JButton("Search");
		btnNewButton.addActionListener((ActionEvent e) -> {
			AddGroupDialog.this.search();
		});
		btnNewButton.setBounds(334, 54, 91, 21);
		contentPanel.add(btnNewButton);

		blockNoFromField = new JTextField();
		blockNoFromField.setHorizontalAlignment(SwingConstants.RIGHT);
		blockNoFromField.setBounds(120, 28, 82, 19);
		contentPanel.add(blockNoFromField);
		blockNoFromField.setColumns(10);

		geneNameField = new JTextField();
		geneNameField.setBounds(120, 54, 194, 19);
		contentPanel.add(geneNameField);
		geneNameField.setColumns(10);

		JLabel lblNewLabel_3 = new JLabel("Target : ");
		lblNewLabel_3.setBounds(12, 10, 50, 13);
		contentPanel.add(lblNewLabel_3);

		JLabel lblNewLabel = new JLabel("Group name : ");
		lblNewLabel.setBounds(12, 312, 88, 13);
		contentPanel.add(lblNewLabel);

		groupNameField = new JTextField();
		groupNameField.setBounds(120, 309, 300, 19);
		contentPanel.add(groupNameField);
		groupNameField.setColumns(10);

		JLabel lblNewLabel_4 = new JLabel("Color : ");
		lblNewLabel_4.setBounds(12, 338, 50, 13);
		contentPanel.add(lblNewLabel_4);

		colorField = new JTextField();
		colorField.setText("#009999");
		colorField.setColumns(10);
		colorField.setBounds(120, 335, 96, 19);
		contentPanel.add(colorField);

		this.visibleCheckBox = new JCheckBox("Visible");
		this.visibleCheckBox.setSelected(true);
		this.visibleCheckBox.setBounds(226, 334, 103, 21);
		contentPanel.add(this.visibleCheckBox);

		blockNoToField = new JTextField();
		blockNoToField.setHorizontalAlignment(SwingConstants.RIGHT);
		blockNoToField.setColumns(10);
		blockNoToField.setBounds(231, 28, 82, 19);
		contentPanel.add(blockNoToField);

		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				this.okButton = new JButton("Add to group");
				this.okButton.addActionListener((ActionEvent e) -> {
					AddGroupDialog.this.addToGroup();
					AddGroupDialog.this.dispose();
				});
				this.okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(this.okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener((ActionEvent e) -> {
					AddGroupDialog.this.dispose();
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}

		this.groupNameField.setText(this.viewer.getOption().getNewGroupName());
		this.colorField.setText(this.viewer.getOption().getNewColor());

		this.changeStatus();
	}

	/**
	 * 各コンポーネントの状態を設定する。
	 */
	private void changeStatus() {
		if (this.blockRadioButton.isSelected()) {
			this.blockNoFromField.setEnabled(true);
			this.blockNoToField.setEnabled(true);
			this.geneNameField.setEnabled(false);
		} else {
			this.blockNoFromField.setEnabled(false);
			this.blockNoToField.setEnabled(false);
			this.geneNameField.setEnabled(true);
		}
		boolean enable = false;
		if (this.result != null) {
			for (GroupInfo gi: this.result) {
				if (gi.isSelect()) {
					enable = true;
				}
			}
		}
		this.okButton.setEnabled(enable);
	}



	/**
	 * グループの追加を行う。
	 */
	private void addToGroup() {
		String name = this.groupNameField.getText();
		String color = this.colorField.getText();
		boolean visible = this.visibleCheckBox.isSelected();
		if (this.blockRadioButton.isSelected()) {
			@SuppressWarnings("unchecked")
			List<BlockInfo> blist = (List<BlockInfo>) this.result;
			ClusterGroup cg = new ClusterGroup(name, visible, color);
			this.viewer.addBlockToClusterGroup(cg, blist);
		} else {
			@SuppressWarnings("unchecked")
			List<GeneInfo> glist = (List<GeneInfo>) this.result;
			ClusterGroup cg = new ClusterGroup(name, visible, color);
			this.viewer.addGeneToClusterGroup(cg, glist);
		}
	}

	/**
	 * ブロック検索処理。
	 */
	private void search() {
		if (this.blockRadioButton.isSelected()) {
			String fromNo = this.blockNoFromField.getText();
			String toNo = this.blockNoToField.getText();
			CoreGenome coreGenome = this.viewer.getOption().getCoreGenome();
			List<BlockInfo> list = coreGenome.search("Core", fromNo, toNo);
			CoreGenome islandGenome = this.viewer.getOption().getCmap().getIsland();
			if (islandGenome != null) {
				list.addAll(islandGenome.search("Island", fromNo, toNo));
			}
			this.table.setModel(new  BlockInfoTableModel(list));
			this.result = list;
		} else {
			GenomeData genomeData = this.viewer.getOption().getGdata();
			String name = this.geneNameField.getText();
			List<GeneInfo> list = genomeData.searchGene(name);
			for (GeneInfo gi: list) {
				String text = gi.getSp() + ":" + gi.getName() + "," + gi.getPos() + "," + gi.getDir() + "," + gi.getCoreid1() + "," + gi.getCoreid2();
				logger.debug(text);
			}
			this.table.setModel(new GeneInfoTableModel(list));
			this.result = list;
		}
	}
}
