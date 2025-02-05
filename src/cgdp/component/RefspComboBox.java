package cgdp.component;

import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import cgdp.corealign.CompareMapOpt;

/**
 * 生物種の一覧を選択するComboBox。
 *
 */
public class RefspComboBox extends JComboBox<String> {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * spのcompoboxModel.
	 */
	private class Model extends DefaultComboBoxModel<String> {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		/**
		 * 生物種のリスト。
		 */
		private List<String> spList = null;

		/**
		 * コンストラクタ。
		 * @param opt オプション。
		 */
		public Model(CompareMapOpt opt) {
			this.setRefspList(opt);
		}
		/**
		 * Refspの選択肢を設定する。
		 * @param opt Option情報。
		 * @throws Exception 例外。
		 */
		public void setRefspList(CompareMapOpt opt) {
			this.removeAllElements();
			List<String> list = opt.getGdata().specList;
			for (String sp: list) {
				this.addElement(sp);
			}
			this.spList = list;
		}
	}

	/**
	 * コンストラクタ。
	 * @param opt オプション。
	 */
	public RefspComboBox(CompareMapOpt opt) {
		this.setOption(opt);
	}

	/**
	 * オプションの情報を設定する。
	 * @param opt オプション。
	 */
	public void setOption(CompareMapOpt opt) {
		this.setModel(new Model(opt));
		String refsp = opt.getRefsp();
		List<String> list = opt.getGdata().specList;
		int idx = list.indexOf(refsp);
		if (idx >= 0) {
			this.setSelectedIndex(idx);
		}
	}

	/**
	 * 選択値を取得する。
	 * @return 選択値。
	 */
	public String getValue() {
		Model m = (Model) this.getModel();
		int idx = this.getSelectedIndex();
		return m.spList.get(idx);
	}
}
