package cgdp.component;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;

import cgdp.corealign.CompareMapOpt.ColorGroup;

/**
 * 特徴領域,遺伝子情報配色グループテーブル。
 */
public class ColorGroupTable extends JTable {

	/**
	 * UID。
	 */
	private static final long serialVersionUID = 1L;

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
	 * コンストラクタ。
	 */
	public ColorGroupTable() {
		this.setModel(new GroupTableModel(new ArrayList<ColorGroup>()));
	}

	/**
	 * データを設定します。
	 * @param groupList 特徴領域、遺伝子集合グループリスト。
	 */
	public void setList(List<ColorGroup> groupList) {
		this.setModel(new GroupTableModel(groupList));
	}
}
