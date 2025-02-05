package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.component.RefspComboBox;
import cgdp.component.UserTable;
import cgdp.component.UserTableModel;
import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CompareMapOpt.HitInfo;

/**
 * 塩基配列検索ダイアログ。
 *
 */
public class SearchGenomeSequenceDialog extends JDialog {
	/**
	 *
	 */
	private static final long serialVersionUID = 6215709180311150225L;

	private static final int MAX_HITS = 3000;

	private static Logger logger = LogManager.getLogger(SearchGenomeSequenceDialog.class);

	private final JPanel contentPanel = new JPanel();
	private JTextField patternField;
	private JScrollPane scrollPane = null;
	private JTable table;
	private JLabel countLabel = null;
	private JComboBox<String> typeComboBox = null;
	private RefspComboBox spComboBox = null;

	/**
	 * 検索結果テーブルモデル。
	 */
	private class HitResultTableModel extends UserTableModel<HitInfo> {
		/**
		 *
		 */
		private static final long serialVersionUID = -1571224894071275254L;

		/**
		 * コンストラクタ。
		 * @param dataList データリスト。
		 */
		public HitResultTableModel(List<HitInfo> dataList) {
			super(dataList);
			this.addColumnInfo(new ColumnInfo("Name", "chrName", false, String.class));
			this.addColumnInfo(new ColumnInfo("Seqno", "seqNo", false, Integer.class));
			this.addColumnInfo(new ColumnInfo("Position", "start", false, Integer.class));
			this.addColumnInfo(new ColumnInfo("Position", "end", false, Integer.class));
			this.addColumnInfo(new ColumnInfo("Length", "length", false, Integer.class));
			this.addColumnInfo(new ColumnInfo("Dir", "dir", false, Integer.class));
			this.addColumnInfo(new ColumnInfo("Sequence", "sequence", false, String.class));
		}
	}

	/**
	 * 検索結果テーブル。
	 *
	 */
	private class HitResultTable extends UserTable {
		/**
		 *
		 */
		private static final long serialVersionUID = -5964561052991459007L;

		/**
		 * コンストラクタ。
		 */
		public HitResultTable() {
			this.setModel(new HitResultTableModel(new ArrayList<HitInfo>()));
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
	 * Viewer。
	 */
	private ComparativeMapViewer viewer = null;
	private JButton clearButton;

	/**
	 * Create the dialog.
	 */
	public SearchGenomeSequenceDialog(ComparativeMapViewer viewer) {
		setTitle("Search genome sequence");
		this.viewer = viewer;
		setBounds(100, 100, 543, 383);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		JLabel lblNewLabel = new JLabel("Sequence : ");
		lblNewLabel.setBounds(12, 10, 67, 13);
		contentPanel.add(lblNewLabel);

		patternField = new JTextField();
		patternField.setBounds(91, 7, 241, 19);
		contentPanel.add(patternField);
		patternField.setColumns(10);

		this.scrollPane = new JScrollPane();
		this.scrollPane.setBounds(12, 86, 503, 217);
		contentPanel.add(this.scrollPane);

		table = new HitResultTable();
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					logger.debug("Double clicked");
					SearchGenomeSequenceDialog.this.updateViewer();
				}
			}
		});
		scrollPane.setViewportView(table);

		JButton searchButton = new JButton("Search");
		searchButton.addActionListener((ActionEvent e) -> {
			SearchGenomeSequenceDialog.this.searchGenomeSequence();
		});
		searchButton.setBounds(351, 6, 76, 21);
		contentPanel.add(searchButton);

		this.countLabel = new JLabel("");
		this.countLabel.setBounds(12, 60, 503, 13);
		contentPanel.add(this.countLabel);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton cancelButton = new JButton("Close");
				cancelButton.addActionListener((ActionEvent e) -> {
					SearchGenomeSequenceDialog.this.dispose();
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}

		JLabel lblNewLabel_1 = new JLabel("Range : ");
		lblNewLabel_1.setBounds(12, 37, 50, 13);
		contentPanel.add(lblNewLabel_1);

		this.typeComboBox = new JComboBox<String>();
		this.typeComboBox.addActionListener((ActionEvent e) -> {
			logger.debug("change");
			int idx = this.typeComboBox.getSelectedIndex();
			if (idx == 0) {
				this.spComboBox.setEnabled(true);
			} else {
				this.spComboBox.setEnabled(false);
			}
		});
		this.typeComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"Only refsp sequence", "Only display range", "All range"}));
		this.typeComboBox.setBounds(91, 33, 154, 21);
		contentPanel.add(this.typeComboBox);

		this.spComboBox = new RefspComboBox(this.viewer.getOption());
		this.spComboBox.setBounds(351, 33, 164, 21);
		contentPanel.add(this.spComboBox);
		this.spComboBox.addActionListener((ActionEvent e) -> {
			logger.debug("change refsp = " + this.spComboBox.getSelectedIndex());
			this.viewer.getDrawer().setRefSp(this.spComboBox.getValue());
			this.viewer.repaint();
		});

		JLabel lblNewLabel_2 = new JLabel("Species : ");
		lblNewLabel_2.setBounds(281, 37, 67, 13);
		contentPanel.add(lblNewLabel_2);

		clearButton = new JButton("Clear");
		clearButton.addActionListener((ActionEvent e) -> {
			SearchGenomeSequenceDialog.this.clearResult();
		});
		clearButton.setBounds(439, 6, 76, 21);
		contentPanel.add(clearButton);

		this.setAlwaysOnTop(true);
		this.patternField.setText(this.viewer.getOption().getSequencePattern());
		this.typeComboBox.setSelectedIndex(this.viewer.getOption().getSequenceSearchType());
		this.setResult(this.viewer.getOption().getHitList());
	}

	/**
	 * 検索結果をクリアする。
	 */
	private void clearResult() {
		HitResultTableModel model = new HitResultTableModel(new ArrayList<HitInfo>());
		this.table.setModel(model);
		this.table.updateUI();
		SearchGenomeSequenceDialog.this.viewer.getOption().setHitList(null);
		SearchGenomeSequenceDialog.this.viewer.repaint();
	}

	/**
	 * 相補パターンを作成する。
	 */
/*	private void updateComplement() {
		StringBuilder sb = new StringBuilder();
		String text = this.patternField.getText();
		Alphabet dna = Alphabet.getNucleotides();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			try {
				sb.append(dna.complement(c));
			} catch (Exception e) {
				sb.append(c);
			}
		}
		this.complementField.setText(sb.toString());
	}
*/
	/**
	 * 正規表現検索。
	 */
	private void searchGenomeSequence() {
		String pat = this.patternField.getText().toUpperCase();
//		RawSequence seq = new RawSequence("name", pat);
//		Sequence rseq = seq.getReverse();
		if (pat.length() >= 3) {
			try {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				int type = this.typeComboBox.getSelectedIndex();
				logger.debug("searchType=" + type);
//				String cpat = this.complementField.getText().toUpperCase();
				List<HitInfo> hitResult = this.viewer.searchPattern(type, pat);
				this.setResult(hitResult);
				this.viewer.getOption().setHitList(hitResult);
				this.viewer.getOption().setSequenceSearchType(type);
				this.viewer.getOption().setSequencePattern(pat);
				this.viewer.repaint();
			} finally {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		} else if (pat.length() == 0){
			JOptionPane.showMessageDialog(this, "Please enter the sequence.");
		} else {
			JOptionPane.showMessageDialog(this, "Please specify at least 3 characters.");
		}
//		logger.debug("revpat=" + rseq.getSeqString());
	}

	/**
	 * 検索結果をテーブルに表示する。
	 * @param hitResult 検索結果。
	 */
	private void setResult(List<HitInfo> hitResult) {
		if (hitResult == null) {
			return;
		}
		List<HitInfo> tableList = hitResult;
		if (hitResult.size() == 0) {
			String msg = "";
			msg = String.format("Not found.");
			this.countLabel.setText(msg);
		} else if (hitResult.size() < 0) {
			String msg = "";
			msg = String.format("Nucleotide sequence is not displayed.");
			this.countLabel.setText(msg);
		} else if (hitResult.size() <= MAX_HITS){
			String count = "";
			if (hitResult.size() > 0) {
				if (hitResult.size() > 0) {
					count = String.format("Matched %d cases.Double-clicking the found position updates the Viewer.", hitResult.size());
				} else {
					count = String.format("Matched %d cases. Matching patterns are highlighted.", hitResult.size());
				}
			}
			this.countLabel.setText(count);
		} else {
			String msg = String.format("Matched %d cases.Showing the top %d items.", hitResult.size(), MAX_HITS);
			this.countLabel.setText(msg);
			ArrayList<HitInfo> list = new ArrayList<HitInfo>();
			for (int i = 0; i < MAX_HITS; i++) {
				list.add(hitResult.get(i));
			}
			tableList = list;
		}
		HitResultTableModel model = new HitResultTableModel(tableList);
		this.table.setModel(model);
		this.table.updateUI();
	}

	/**
	 * Viewerの更新。
	 */
	private void updateViewer() {
		int row = table.getSelectedRow();
		logger.debug("row=" + row);
		if (row >= 0) {
			HitResultTableModel model = (HitResultTableModel) table.getModel();
			List<HitInfo> list = model.getDataList();
			HitInfo hit = list.get(row);
			String locus = hit.getLocusFrom();
			try {
				this.viewer.getLocusInput().setText(locus);
				this.viewer.getDrawer().setCenterPosByStr(locus, true);
				this.viewer.repaint();
			} catch (Error e) {
				JOptionPane.showMessageDialog(this, e.getMessage());
			}
		}
	}
}
