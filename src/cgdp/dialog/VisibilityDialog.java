package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.component.UserTableModel;
import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CompareMapOpt;
import cgdp.corealign.CoreGenome;
import cgdp.util.ColorUtil;
import lombok.Data;

/**
 * 表示設定ダイアログ。
 *
 */
public class VisibilityDialog extends JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 8071496064936055056L;

	/**
	 * Logger。
	 */
	private static Logger logger = LogManager.getLogger(VisibilityDialog.class);


	/**
	 * 残りの名称。
	 */
	private static final String LEFTOVERS = "Leftovers";

	/**
	 * 設定値のバックアップ。
	 */
	private List<Visibility> backupList = null;


	/**
	 * 内容パネル。
	 */
	private final JPanel contentPanel = new JPanel();
	/**
	 * 設定情報テーブル。
	 */
	private JTable table;

	/**
	 * 表示設定情報。
	 *
	 */
	@Data
	public static class Visibility {
		/**
		 * 表示フラグ
		 */
		private boolean visible;
		/**
		 * 名称。
		 */
		private String name;
		/**
		 * グラデーション。
		 */
		private boolean gradation;
		/**
		 * 色。
		 */
		private String color;

		/**
		 * コンストラクタ。
		 * @param visible 表示フラグ。
		 * @param name 名称。
		 * @param gradation グラデーションフラグ。
		 * @param color 色。
		 */
		public Visibility(final boolean visible, final String name, final boolean gradation, final Color color) {
			this.visible = visible;
			this.name = name;
			this.gradation = gradation;
			this.color = ColorUtil.getColorCode(color);
		}

		/**
		 * コンストラクタ。
		 * @param cg CoreGenome。
		 */
		public Visibility(final CoreGenome cg) {
			this.visible = cg.isVisible();
			this.name = cg.getName();
			this.gradation = cg.isGradation();
			this.color = ColorUtil.getColorCode(cg.getColor());
		}
	}

	/**
	 * テーブルモデル。
	 *
	 */
	private class VisibilityTableModel extends UserTableModel<Visibility> {
		/**
		 *
		 */
		private static final long serialVersionUID = 3604311390542962484L;

		/**
		 * コンストラクタ。
		 * @param dataList データリスト。
		 */
		public VisibilityTableModel(List<Visibility> dataList) {
			super(dataList);
			this.addColumnInfo(new ColumnInfo("Visible", "visible", true, boolean.class));
			this.addColumnInfo(new ColumnInfo("Name", "name", true, String.class));
			this.addColumnInfo(new ColumnInfo("Gradation", "gradation", true, boolean.class));
			this.addColumnInfo(new ColumnInfo("Color", "color", false, String.class));
		}
	}

	/**
	 * Viewer。
	 */
	private ComparativeMapViewer viewer = null;

	/**
	 * コンストラクタ.
	 * @param viewer Viewer。
	 */
	public VisibilityDialog(ComparativeMapViewer viewer) {
		setTitle("Visible genome selection");
		this.viewer = viewer;
		this.backupList = this.getVisibilityList(this.viewer.getOption());
		List<Visibility> list = this.getVisibilityList(this.viewer.getOption());
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JScrollPane scrollPane = new JScrollPane();
			contentPanel.add(scrollPane, BorderLayout.CENTER);
			{
				table = new JTable();
				table.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						int col = table.getSelectedColumn();
						int row = table.getSelectedRow();
						logger.debug("row=" + row + ", col=" + col);
						if (col == 0 || col == 2) {
							VisibilityDialog.this.updateView();
						}
						if (col == 3) {
							VisibilityDialog.this.selectColor(row);
						}
					}
				});

				table.setModel(new VisibilityTableModel(list));
				scrollPane.setViewportView(table);
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener((ActionEvent e) -> {
					VisibilityDialog.this.updateView();
					VisibilityDialog.this.dispose();
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener((ActionEvent e) -> {
					CompareMapOpt opt = VisibilityDialog.this.viewer.getOption();
					VisibilityDialog.this.setVisivilityInfo(opt, VisibilityDialog.this.backupList);
					VisibilityDialog.this.viewer.repaint();
					VisibilityDialog.this.dispose();
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	/**
	 * 表示対象リストを取得します。
	 * @param opt オプション情報。
	 * @return 表示対象リスト。
	 */
	private List<Visibility> getVisibilityList(final CompareMapOpt opt) {
		List<Visibility> list = new ArrayList<Visibility>();
		list.add(new Visibility(opt.getCoreGenome()));
		if (opt.getCmap().getIsland() != null) {
			list.add(new Visibility(opt.getCmap().getIsland()));
		}
		List<CoreGenome> olist = opt.getCmap().getOtherList();
		for (CoreGenome cg: olist) {
			list.add(new Visibility(cg.isVisible(), cg.getName(), cg.isGradation(), cg.getColor()));
		}
		list.add(new Visibility(opt.isLeftoversView(), LEFTOVERS, opt.isLeftoversGradation(), opt.getLeftoversColor()));
		return list;
	}

	/**
	 * 色設定ダイアログの設定。
	 * @param row 設定する行。
	 */
	private void selectColor(int row) {
		String code = (String) this.table.getModel().getValueAt(row, 3);
		Color c = ColorUtil.getColor(code);
		Color color = JColorChooser.showDialog(this, "Color", c);
		if (color != null) {
			String rgb = ColorUtil.getColorCode(color);
			this.table.getModel().setValueAt(rgb, row, 3);
			VisibilityDialog.this.updateView();
		}
	}

	/**
	 * Viewを更新する。
	 */
	private void updateView() {
		CompareMapOpt opt = this.viewer.getOption();
		VisibilityTableModel m = (VisibilityTableModel) this.table.getModel();
		List<Visibility> list = m.getDataList();
		this.setVisivilityInfo(opt, list);
		this.viewer.repaint();
	}

	/**
	 * 表示設定の値を保存する。
	 * @param opt オプション。
	 * @param list 表示設定リスト。
	 */
	private void setVisivilityInfo(final CompareMapOpt opt, final List<Visibility> list) {
		int idx = 0;
		for (Visibility v: list) {
			if ("Core".equals(v.getName())) {
				opt.getCoreGenome().setVisible(v.isVisible());
				opt.getCoreGenome().setGradation(v.isGradation());
				opt.getCoreGenome().setColor(ColorUtil.getColor(v.getColor()));
			} else if ("Island".equals(v.getName())) {
				opt.getCmap().getIsland().setVisible(v.isVisible());
				opt.getCmap().getIsland().setGradation(v.isGradation());
				opt.getCmap().getIsland().setColor(ColorUtil.getColor(v.getColor()));
			} else if (LEFTOVERS.equals(v.getName())) {
				opt.setLeftoversView(v.isVisible());
				opt.setLeftoversGradation(v.isGradation());
				opt.setLeftoversColor(ColorUtil.getColor(v.getColor()));
			} else {
				List<CoreGenome> cglist = opt.getCmap().getOtherList();
				cglist.get(idx).setVisible(v.isVisible());
				cglist.get(idx).setGradation(v.isGradation());
				cglist.get(idx).setColor(ColorUtil.getColor(v.getColor()));
				idx++;
			}
		}
	}
}
