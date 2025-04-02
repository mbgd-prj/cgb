package cgdp.component;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JColorChooser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.corealign.CompareMapOpt.ClusterGroup;
import cgdp.dialog.ClusterGroupListDialog;
import cgdp.util.ColorUtil;

/**
 * クラスタグループテーブル。
 *
 */
public class ClusterGroupTable extends UserTable {
	/**
	 *
	 */
	private static final long serialVersionUID = -3359356035264696989L;
	private static final String VISIBLE = "Visible";
	private static final String NAME = "Name";
	private static final String COLOR = "Color";
	private static final String DELETE = "Delete";
	private static Logger logger = LogManager.getLogger(ClusterGroupTable.class);

	/**
	 * テーブルモデル。
	 */
	private class ClusterGroupTableModel extends UserTableModel<ClusterGroup> {
		/**
		 *
		 */
		private static final long serialVersionUID = 150686873485311575L;

		/**
		 * コンストラクタ。
		 * @param list データリスト。
		 */
		public ClusterGroupTableModel(List<ClusterGroup> list) {
			super(list);
			this.addColumnInfo(new ColumnInfo(VISIBLE, "visible", true, boolean.class));
			this.addColumnInfo(new ColumnInfo(NAME, "name", true, String.class));
			this.addColumnInfo(new ColumnInfo(COLOR, "colorCode", false, String.class));
			this.addColumnInfo(new ColumnInfo(DELETE, null, true, String.class, new ButtonCellRenderer(), new ButtonCellEditor() {
				/**
				 *
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public void action(ActionEvent e) {
					int idx = ClusterGroupTable.this.getSelectedRow();
					logger.debug("delete row=" + idx);
					// ClusterGroupTable.this.deleteClusterGroup(idx);
				}
			}));
		}
	}

	private ClusterGroupListDialog dialog = null;

	private List<ClusterGroup> list = null;


	/**
	 * コンストラクタ。
	 * @param list クラスタグループリスト。
	 */
	public ClusterGroupTable(ClusterGroupListDialog dialog) {
		this.dialog = dialog;
		this.list = dialog.getList();
		this.setModel(new ClusterGroupTableModel(this.list));
		this.getColumn(VISIBLE).setPreferredWidth(60);
		this.getColumn(NAME).setPreferredWidth(200);
		this.getColumn(COLOR).setPreferredWidth(60);

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				int col = ClusterGroupTable.this.getSelectedColumn();
				int row = ClusterGroupTable.this.getSelectedRow();
				logger.debug("row=" + row + ", col=" + col);
				if (col == 2) {
					ClusterGroupTable.this.selectColor(row);
				} else 	if (col == 3) {
					ClusterGroupTable.this.deleteClusterGroup(row);
				}
			}
		});
	}

	public void setClusterGroupList(List<ClusterGroup> list) {
		this.list = list;
		this.setModel(new ClusterGroupTableModel(list));
	}

	/**
	 * クラスタグループを削除します。
	 * @param idx 削除するクラスタグループのインデックス。
	 */
	protected void deleteClusterGroup(int idx) {
		this.list.remove(idx);
		this.updateUI();
	}

	/**
	 * クラスタグループを保存します。
	 * @param idx 保存するクラスタグルーブのインデックス。
	 * @throws Exception 例外。
	 */
//	protected void saveClusterGroup(int idx) throws Exception {
//		logger.debug("idx=" + idx);
//		this.dialog.saveGroup(idx);
//	}

	/**
	 * 色の選択を行います。
	 * @param idx 色を設定するグループのインデックス。
	 */
	protected void selectColor(int idx) {
		String code = (String) this.getModel().getValueAt(idx, 2);
		Color c = ColorUtil.getColor(code);
		Color color = JColorChooser.showDialog(this, "Color", c);
		if (color != null) {
			String rgb = ColorUtil.getColorCode(color);
			this.getModel().setValueAt(rgb, idx, 2);
		}
	}
}
