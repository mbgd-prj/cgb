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
import cgdp.filereader.SegmentFileReader.Segment;
import net.arnx.jsonic.JSON;

/**
 * セグメント選択ダイアログ。
 */
public class SelectSegmentDialog extends JDialog {

	/**
	 * Logger.
	 */
	private static final Logger logger = LogManager.getLogger(SelectSegmentDialog.class);

	/**
	 * UID。
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 特徴領域メンバーテーブルモデル。
	 */
	private class SegmentTableModel extends UserTableModel<Segment> {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * コンストラクタ。
		 * @param dataList データリスト。
		 */
		public SegmentTableModel(List<Segment> dataList) {
			super(dataList);
			this.addColumnInfo(new ColumnInfo("Name", "species", false, String.class));
			this.addColumnInfo(new ColumnInfo("Seqno", "seqNo", false, Integer.class));
			this.addColumnInfo(new ColumnInfo("Position", "start", false, Integer.class));
			this.addColumnInfo(new ColumnInfo("Position", "end", false, Integer.class));
			// this.addColumnInfo(new ColumnInfo("Length", "length", false, Integer.class));
			this.addColumnInfo(new ColumnInfo("Dir", "dir", false, Integer.class));
			this.addColumnInfo(new ColumnInfo("Sequence", "pattern", false, String.class));
		}
	}

	/**
	 * 特徴領域テーブル。
	 *
	 */
	private class SegmentTable extends UserTable {
		/**
		 * UID。
		 */
		private static final long serialVersionUID = 1L;
			/**
		 * コンストラクタ。
		 */
		public SegmentTable() {
			this.setModel(new SegmentTableModel(new ArrayList<Segment>()));
			this.getColumn("Name").setPreferredWidth(100);
			this.getColumn("Seqno").setPreferredWidth(60);
			this.getColumn("Position").setPreferredWidth(80);
			this.getColumn("Dir").setPreferredWidth(40);
			this.getColumn("Sequence").setPreferredWidth(200);
		}

		@Override
		public void setModel(TableModel dataModel) {
			super.setModel(dataModel);
			try {
				this.getColumn("Name").setPreferredWidth(100);
				this.getColumn("Seqno").setPreferredWidth(60);
				this.getColumn("Position").setPreferredWidth(80);
				this.getColumn("Sequence").setPreferredWidth(200);
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
		}
	}

	/**
	 * 配色グループテーブル。
	 */
	private final ColorGroupTable groupTable = new ColorGroupTable();
	/**
	 * 特徴領域メンバーテーブル。
	 */
	private final SegmentTable memberTable = new SegmentTable();

	/**
	 * 親コンポーネント。
	 */
	private ComparativeMapViewer viewer = null;

	/**
	 * 配色グループリストを取得します。
	 * @return 配色グループリスト。
	 */
	private List<ColorGroup> getColorGroupList() {
		List<ColorGroup> list = this.viewer.getOption().getSegmentColorGroupList();
		List<ColorGroup> copyList = new ArrayList<ColorGroup>();
		for (ColorGroup group : list) {
			copyList.add(new ColorGroup(group));
		}
		return copyList;
	}

	/**
	 * 配色グループを選択したときに特徴領域テーブルを更新します。
	 * @param list 配色グループリスト。
	 */
	private void updateSegmentTable(final List<ColorGroup> list) {
		List<Segment> segList = this.viewer.getOption().getSegmentList(list);
		this.memberTable.setModel(new SegmentTableModel(segList));
	}

	/**
	 * Create the dialog.
	 * @param viewer 親コンポーネント。
	 */
	public SelectSegmentDialog(final ComparativeMapViewer viewer) {
		this.viewer = viewer;
		setTitle("Select segment");
		setBounds(100, 100, 791, 465);
		getContentPane().setLayout(new BorderLayout());
		// グループ選択パネル
		{
			JPanel groupPanel = new JPanel();
			groupPanel.setLayout(new BorderLayout(0, 0));
			groupPanel.setPreferredSize(new java.awt.Dimension(200, 0));
			getContentPane().add(groupPanel, BorderLayout.WEST);
			groupPanel.add(new JLabel("Group list"), BorderLayout.NORTH);
			JScrollPane groupScrollPane = new JScrollPane();
			groupScrollPane.setViewportView(this.groupTable);
			groupPanel.add(groupScrollPane);
			this.groupTable.setList(this.getColorGroupList());
			this.groupTable.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseClicked(java.awt.event.MouseEvent e) {
					int row = SelectSegmentDialog.this.groupTable.getSelectedRow();
					int col = SelectSegmentDialog.this.groupTable.getSelectedColumn();
					logger.debug("row=" + row + ", col=" + col);
					List<ColorGroup> list = SelectSegmentDialog.this.groupTable.getList();
					logger.debug("list=" + JSON.encode(list, true));
					if (col == 0) {
						SelectSegmentDialog.this.updateSegmentTable(list);
					}
				}
			});
		}
		// メンバーリストパネル
		{
			JPanel memberPanel = new JPanel();
			memberPanel.setLayout(new BorderLayout(0, 0));
			getContentPane().add(memberPanel, BorderLayout.CENTER);
			memberPanel.add(new JLabel("Segment list"), BorderLayout.NORTH);
			JScrollPane memberScrollPane = new JScrollPane();
			memberScrollPane.setViewportView(this.memberTable);
			memberPanel.add(memberScrollPane);
			this.memberTable.setModel(new SegmentTableModel(this.viewer.getOption().getSegmentList(this.groupTable.getList())));
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
					this.viewer.getOption().setSegmentColorGroupList(this.groupTable.getList());
					SelectSegmentDialog.this.dispose();
				});

			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
				cancelButton.addActionListener((ActionEvent e) -> {
					SelectSegmentDialog.this.dispose();
				});
			}
		}
	}

}
