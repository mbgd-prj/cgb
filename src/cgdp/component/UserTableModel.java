package cgdp.component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import lombok.Getter;

/**
 * 汎用テーブルモデルクラス。
 *
 * @param <T> 処理するデータ型。
 */
public class UserTableModel<T> extends AbstractTableModel {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(UserTableModel.class);

	/**
	 * カラム情報。
	 */
	@Data
	public class ColumnInfo {
		private String name = null;
		private String prop = null;
		private boolean editable = false;
		private Class<?> clazz = null;
		private DefaultTableCellRenderer renderer = null;
		private DefaultCellEditor editor = null;
		/**
		 * コンストラクタ。
		 * @param name カラム表示名。
		 * @param prop データクラスTのプロパティ。
		 * @param editable 編集フラグ。
		 * @param clazz プロパティ値のクラス。
		 * @param renderer セルレンダラー。
		 * @param editor セルエディター。
		 */
		public ColumnInfo(String name, String prop, boolean editable, Class<?> clazz, DefaultTableCellRenderer renderer, DefaultCellEditor editor) {
			this.name = name;
			this.prop = prop;
			this.editable = editable;
			this.clazz = clazz;
			this.renderer = renderer;
			this.editor = editor;

		}

		/**
		 * コンストラクタ。
		 * @param name カラム表示名。
		 * @param prop データクラスTのプロパティ。
		 * @param editable 編集フラグ。
		 * @param clazz プロパティ値のクラス。
		 */
		public ColumnInfo(String name, final String prop, boolean editable, Class<?> clazz) {
			this(name, prop, editable, clazz, null, null);
		}

		/**
		 * 先頭が大文字のプロパティを取得する。
		 * @return 先頭が大文字のプロパティ。
		 */
		private String getTopUpperProp() {
			if (this.prop != null) {
				String ret = this.prop.substring(0, 1).toUpperCase();
				if (this.prop.length() > 1) {
					ret = ret + this.prop.substring(1);
				}
				return ret;
			} else {
				return this.prop;
			}
		}

		/**
		 * Getterのメソッド名を取得する。
		 * @return Getterのメソッド名。
		 */
		public String getGetterName() {
			if (this.prop != null) {
				String ret = this.getTopUpperProp();
				if (this.clazz.equals(boolean.class)) {
					ret = "is" + ret;
				} else {
					ret = "get" + ret;
				}
				return ret;
			} else {
				return this.prop;
			}
		}

		/**
		 * Setterのメソッド名を取得する。
		 * @return Setterのメソッド名。
		 */
		public String getSetterName() {
			if (this.prop != null) {
				String ret = this.getTopUpperProp();
				ret = "set" + ret;
				return ret;
			} else {
				return this.prop;
			}
		}

	}

	/**
	 * カラムリスト。
	 */
	@Getter
	private List<ColumnInfo> columnList = null;

	@Getter
	private List<T> dataList = null;

	/**
	 * コンストラクタ。
	 */
	public UserTableModel(List<T> dataList) {
		this.columnList = new ArrayList<ColumnInfo>();
		this.dataList = dataList;
	}

	/**
	 * カラム情報の追加。
	 * @param ci カラム情報。
	 */
	public void addColumnInfo(ColumnInfo ci) {
		this.columnList.add(ci);
	}

	@Override
	public int getColumnCount() {
		return this.columnList.size();
	}

	/**
	 * カラム名の取得。
	 */
	@Override
	public String getColumnName(int column) {
		ColumnInfo c = this.columnList.get(column);
		return c.name;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		ColumnInfo c = this.columnList.get(columnIndex);
		if (c.clazz.equals(boolean.class)) {
			return Boolean.class;
		}
		return c.clazz;
	}

	/**
	 * 編集可能セルの設定。
	 */
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		ColumnInfo c = this.columnList.get(columnIndex);
		return c.editable;
	}


	@Override
	public int getRowCount() {
		return this.dataList.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object value = null;
		try {
			ColumnInfo c = this.columnList.get(columnIndex);
			T data = this.dataList.get(rowIndex);
			Class<?> cls = data.getClass();
			String getter = c.getGetterName();
			if (getter != null) {
				Method m = cls.getMethod(getter);
				value = m.invoke(data);
			} else {
				value = c.name;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return value;
	}

	/**
	 * 編集値の設定。
	 */
	public void setValueAt(Object val, int rowIndex, int columnIndex) {
		try {
			ColumnInfo c = this.columnList.get(columnIndex);
			T data = this.dataList.get(rowIndex);
			Class<?> cls = data.getClass();
			String setter = c.getSetterName();
			if (setter != null) {
				Method m = cls.getMethod(setter, c.clazz);
				m.invoke(data, val);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		fireTableCellUpdated(rowIndex, columnIndex);
	}
}
