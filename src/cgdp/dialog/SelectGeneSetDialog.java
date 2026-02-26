package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.component.ColorGroupTable;
import cgdp.component.UserTable;
import cgdp.component.UserTableModel;
import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CompareMapOpt.ColorGroup;
import cgdp.filereader.GeneSetFileReader.GeneSet;

/**
 * 遺伝子セット選択ダイアログ。
 */
public class SelectGeneSetDialog extends JDialog {
	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(SelectGeneSetDialog.class);


	/**
	 * 遺伝集合テーブルモデル。
	 */
	private class GeneSetTableModel extends UserTableModel<GeneSet> {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * コンストラクタ。
		 * @param dataList データリスト。
		 */
		public GeneSetTableModel(List<GeneSet> dataList) {
			super(dataList);
			this.addColumnInfo(new ColumnInfo("Species", "species", false, String.class));
			this.addColumnInfo(new ColumnInfo("Locus", "locus", false, Integer.class));
		}
	}

	/**
	 * 特徴領域テーブル。
	 *
	 */
	private class GeneSetTable extends UserTable {
		/**
		 * UID。
		 */
		private static final long serialVersionUID = 1L;
			/**
		 * コンストラクタ。
		 */
		public GeneSetTable() {
			this.setModel(new GeneSetTableModel(new ArrayList<GeneSet>()));
			this.getColumn("Species").setPreferredWidth(100);
			this.getColumn("Locus").setPreferredWidth(60);
		}

		@Override
		public void setModel(TableModel dataModel) {
			super.setModel(dataModel);
			try {
				this.getColumn("Species").setPreferredWidth(100);
				this.getColumn("Locus").setPreferredWidth(100);
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
		}
	}

	/**
	 * 親コンポーネント。
	 */
	private final ComparativeMapViewer viewer;

	/**
	 * 配色グループテーブル。
	 */
	private final ColorGroupTable groupTable = new ColorGroupTable() {
		@Override
		protected void updateMemberList(List<ColorGroup> list) {
			SelectGeneSetDialog.this.updateGeneSetTable(list);
		}
	};

	/**
	 * 遺伝子集合テーブル。
	 */
	private final GeneSetTable geneSetTable = new GeneSetTable();


	/**
	 * 配色グループリストを取得します。
	 * @return 配色グループリスト。
	 */
	private List<ColorGroup> getColorGroupList() {
		List<ColorGroup> list = this.viewer.getOption().getGeneSetColorGroupList();
		List<ColorGroup> copyList = new ArrayList<ColorGroup>();
		for (ColorGroup group : list) {
			copyList.add(new ColorGroup(group));
		}
		return copyList;
	}

	/**
	 * 配色グループを選択したときに遺伝子氏集合テーブルを更新します。
	 * @param list 配色グループリスト。
	 */
	private void updateGeneSetTable(final List<ColorGroup> list) {
		List<GeneSet> glist = this.viewer.getOption().getGeneSetList(list);
		this.geneSetTable.setModel(new GeneSetTableModel(glist));
	}

	/**
	 * Create the dialog.
	 * @param viewer 親コンポーネント。
	 *
	 */
	public SelectGeneSetDialog(final ComparativeMapViewer viewer) {
		this.viewer = viewer;
		setTitle("Select gene set");
		setBounds(100, 100, 697, 461);
		getContentPane().setLayout(new BorderLayout());
		// グループ選択パネル
		{
			JPanel groupPanel = new JPanel();
			groupPanel.setLayout(new BorderLayout(0, 0));
			groupPanel.setPreferredSize(new java.awt.Dimension(400, 0));
			getContentPane().add(groupPanel, BorderLayout.WEST);
			groupPanel.add(new JLabel("Group list"), BorderLayout.NORTH);
			JScrollPane groupScrollPane = new JScrollPane();
			groupScrollPane.setViewportView(this.groupTable);
			groupPanel.add(groupScrollPane);
			this.groupTable.setList(this.getColorGroupList());
		}
		// メンバーリストパネル
		{
			JPanel memberPanel = new JPanel();
			memberPanel.setLayout(new BorderLayout(0, 0));
			getContentPane().add(memberPanel, BorderLayout.CENTER);
			memberPanel.add(new JLabel("Gene list"), BorderLayout.NORTH);
			JScrollPane memberScrollPane = new JScrollPane();
			memberScrollPane.setViewportView(this.geneSetTable);
			memberPanel.add(memberScrollPane);
			this.geneSetTable.setModel(new GeneSetTableModel(this.viewer.getOption().getGeneSetList(this.groupTable.getList())));

		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
				okButton.addActionListener((ActionEvent e) -> {
					this.viewer.getOption().setGeneSetColorGroupList(this.groupTable.getList());
					SelectGeneSetDialog.this.dispose();
				});
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
				SelectGeneSetDialog.this.dispose();
				cancelButton.addActionListener((ActionEvent e) -> {
					SelectGeneSetDialog.this.dispose();
				});
			}
		}
	}

}
