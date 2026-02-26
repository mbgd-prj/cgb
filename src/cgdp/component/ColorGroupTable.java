package cgdp.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JTable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.corealign.CompareMapOpt.ColorGroup;
import cgdp.util.ColorUtil;
import net.arnx.jsonic.JSON;

/**
 * 特徴領域,遺伝子情報配色グループテーブル。
 */
public abstract class ColorGroupTable extends JTable {
	/**
	 * UID。
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(ColorGroupTable.class);

	/**
	 * 特徴領域グループテーブルモデル。
	 */
	private class GroupTableModel extends UserTableModel<ColorGroup> {
		/**
		 * UID
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * コンストラクタ。
		 * @param dataList データリスト。
		 */
		public GroupTableModel(List<ColorGroup> dataList) {
			super(dataList);
			this.addColumnInfo(new ColumnInfo("Visible", "visible", true, Boolean.class));
			this.addColumnInfo(new ColumnInfo("Name", "name", false, String.class));
			this.addColumnInfo(new ColumnInfo("Color", "color", false, String.class));
		}
	}

	/**
	 * 特徴領域、遺伝子集合リストを更新する。
	 * @param list 特徴領域、遺伝子集合グループのリスト。
	 */
	protected abstract void updateMemberList(List<ColorGroup> list);

	/**
	 * 色を設定する。
	 * @param row 行番号。
	 * @param col 列番号。
	 */
	private void setColor(int row, int col) {
		List<ColorGroup> list = this.getList();
		ColorGroup g = list.get(row);
		Color c = ColorUtil.getColor(g.getColor());
		Color color = JColorChooser.showDialog(this, "Color", c);
		if (color != null) {
			String rgb = ColorUtil.getColorCode(color);
			logger.debug("color=" + color + ", rgb=" + rgb);
			g.setColor(rgb);
			this.setList(list);
			this.updateMemberList(list);
		}
	}

	/**
	 * コンストラクタ。
	 */
	public ColorGroupTable() {
		this.setModel(new GroupTableModel(new ArrayList<ColorGroup>()));
		this.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				int row = ColorGroupTable.this.getSelectedRow();
				int col = ColorGroupTable.this.getSelectedColumn();
				logger.debug("row=" + row + ", col=" + col);
				List<ColorGroup> list = ColorGroupTable.this.getList();
				logger.debug("list=" + JSON.encode(list, true));
				if (col == 0) {
					ColorGroupTable.this.updateMemberList(list);
				} else if (col == 2) {
					ColorGroupTable.this.setColor(row, col);
				}
			}
		});

	}

	/**
	 * データを設定します。
	 * @param groupList 特徴領域、遺伝子集合グループリスト。
	 */
	public void setList(List<ColorGroup> groupList) {
		this.setModel(new GroupTableModel(groupList));
	}

	/**
	 * データを取得します。
	 * @return 特徴領域、遺伝子集合グループリスト。
	 */
	public List<ColorGroup> getList() {
		GroupTableModel model = (GroupTableModel)this.getModel();
		return model.getDataList();
	}

}
