package cgdp.component;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import cgdp.component.UserTableModel.ColumnInfo;

/**
 * ユーザ定義テーブルクラス。
 *
 */
public class UserTable extends JTable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * コンストラクタ。
	 */
	public UserTable() {

	}

	/**
	 * コンストラクタ。
	 * @param model テーブルモデル。
	 */
	public UserTable(UserTableModel<?> model) {
		super(model);
	}

	@Override
	public void setModel(TableModel dataModel) {
		super.setModel(dataModel);
		if (dataModel instanceof UserTableModel) {
			UserTableModel<?> m = (UserTableModel<?>) dataModel;
			for (int i = 0; i < m.getColumnList().size(); i++) {
				@SuppressWarnings("rawtypes")
				ColumnInfo ci = m.getColumnList().get(i);
				if (ci.getRenderer() != null) {
					UserTable.this.getColumnModel().getColumn(i).setCellRenderer(ci.getRenderer());
				}
				if (ci.getEditor() != null) {
					UserTable.this.getColumnModel().getColumn(i).setCellEditor(ci.getEditor());
				}
			}
		}
	}
}
