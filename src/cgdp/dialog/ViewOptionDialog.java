package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.component.RefspComboBox;
import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CompareMap;
import cgdp.corealign.CompareMapOpt;

/**
 * ファイル読み込みダイアログ。
 *
 */
public class ViewOptionDialog extends JDialog {
	/**
	 *
	 */
	private static final long serialVersionUID = -2640261410416989378L;


	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(ViewOptionDialog.class);


	/**
	 * 内容パネル。
	 */
	private final JPanel contentPanel = new JPanel();
	/**
	 * 各種フィールド。
	 */
	private JTextField paperWidthField;
	private JTextField paperHeightField;
//	private DefaultComboBoxModel<String> refspComboModel;
	private RefspComboBox refspComboBox;
	private JTextField centerGeneField;
	private JTextField centerField;
	private JTextField paperSizeField;
	private JTextField consRateField;
	private JTextField viewWidthField;
	private JTextField colorModeField;
	private JTextField chromoGapLenField;
	private JTextField chromoGapLenRatioField;
	private JCheckBox colorIslandModeCheckBox;
	private JScrollPane scrollPane;
	private JTextArea messageArea;

	/**
	 * Viewer。
	 */
	private ComparativeMapViewer viewer= null;

	/**
	 * Create the dialog.
	 * @param viwer ダイアログタイプ。
	 */
	public ViewOptionDialog(final ComparativeMapViewer viewer) {
		setTitle("View option");
		this.viewer = viewer;

		setBounds(100, 100, 652, 290);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		JLabel lblNewLabel_1 = new JLabel("Paper:");
		lblNewLabel_1.setBounds(12, 61, 50, 13);
		contentPanel.add(lblNewLabel_1);

		JLabel lblNewLabel_2 = new JLabel("Width : ");
		lblNewLabel_2.setBounds(198, 61, 56, 13);
		contentPanel.add(lblNewLabel_2);

		JLabel lblNewLabel_3 = new JLabel("Height : ");
		lblNewLabel_3.setBounds(317, 61, 65, 13);
		contentPanel.add(lblNewLabel_3);

		paperWidthField = new JTextField();
		paperWidthField.setHorizontalAlignment(SwingConstants.RIGHT);
		paperWidthField.setBounds(255, 57, 50, 19);
		contentPanel.add(paperWidthField);
		paperWidthField.setColumns(10);

		paperHeightField = new JTextField();
		paperHeightField.setHorizontalAlignment(SwingConstants.RIGHT);
		paperHeightField.setBounds(394, 57, 50, 19);
		contentPanel.add(paperHeightField);
		paperHeightField.setColumns(10);



		this.paperWidthField.setText(Integer.toString(CompareMap.WIDTH));
		this.paperHeightField.setText(Integer.toString(CompareMap.HEIGHT));

		JLabel lblNewLabel_4 = new JLabel("refsp : ");
		lblNewLabel_4.setBounds(12, 14, 50, 13);
		contentPanel.add(lblNewLabel_4);

		JLabel lblNewLabel_5 = new JLabel("centerGene : ");
		lblNewLabel_5.setBounds(257, 14, 96, 13);
		contentPanel.add(lblNewLabel_5);

		centerGeneField = new JTextField();
		centerGeneField.setToolTipText("spcode:gene");
		centerGeneField.setBounds(348, 10, 96, 19);
		contentPanel.add(centerGeneField);
		centerGeneField.setColumns(10);

		JLabel lblNewLabel_6 = new JLabel("center : ");
		lblNewLabel_6.setBounds(456, 14, 56, 13);
		contentPanel.add(lblNewLabel_6);

		centerField = new JTextField();
		centerField.setToolTipText("[chr:]position");
		centerField.setBounds(524, 10, 96, 19);
		contentPanel.add(centerField);
		centerField.setColumns(10);

		JCheckBox nolinkCheckBox = new JCheckBox("nolink");
		nolinkCheckBox.setBounds(12, 33, 125, 21);
		contentPanel.add(nolinkCheckBox);

		paperSizeField = new JTextField();
		paperSizeField.setText("A4");
		paperSizeField.setBounds(136, 57, 50, 19);
		contentPanel.add(paperSizeField);
		paperSizeField.setColumns(10);

		JLabel lblNewLabel_7 = new JLabel("Cons Rate : ");
		lblNewLabel_7.setBounds(307, 36, 102, 13);
		contentPanel.add(lblNewLabel_7);

		consRateField = new JTextField();
		consRateField.setHorizontalAlignment(SwingConstants.RIGHT);
		consRateField.setText("0.8");
		consRateField.setBounds(394, 34, 50, 19);
		contentPanel.add(consRateField);
		consRateField.setColumns(10);

		JLabel lblNewLabel_8 = new JLabel("View width : ");
		lblNewLabel_8.setBounds(475, 61, 102, 13);
		contentPanel.add(lblNewLabel_8);

		viewWidthField = new JTextField();
		viewWidthField.setHorizontalAlignment(SwingConstants.RIGHT);
		viewWidthField.setText("0");
		viewWidthField.setBounds(564, 57, 56, 19);
		contentPanel.add(viewWidthField);
		viewWidthField.setColumns(10);

		JLabel lblNewLabel_9 = new JLabel("Color mode : ");
		lblNewLabel_9.setBounds(12, 87, 125, 13);
		contentPanel.add(lblNewLabel_9);

		colorModeField = new JTextField();
		colorModeField.setText("RGB");
		colorModeField.setBounds(147, 83, 39, 19);
		contentPanel.add(colorModeField);
		colorModeField.setColumns(10);

		this.colorIslandModeCheckBox = new JCheckBox("Color island mode");
		colorIslandModeCheckBox.setBounds(198, 83, 193, 21);
		contentPanel.add(colorIslandModeCheckBox);

		JLabel lblNewLabel_12 = new JLabel("Chromo Gap Len : ");
		lblNewLabel_12.setBounds(12, 110, 125, 13);
		contentPanel.add(lblNewLabel_12);

		chromoGapLenField = new JTextField();
		chromoGapLenField.setHorizontalAlignment(SwingConstants.RIGHT);
		chromoGapLenField.setText("10000");
		chromoGapLenField.setBounds(136, 106, 50, 19);
		contentPanel.add(chromoGapLenField);
		chromoGapLenField.setColumns(10);

		JLabel lblNewLabel_13 = new JLabel("Chromo Gap Len Ratio : ");
		lblNewLabel_13.setBounds(242, 110, 167, 13);
		contentPanel.add(lblNewLabel_13);

		chromoGapLenRatioField = new JTextField();
		chromoGapLenRatioField.setHorizontalAlignment(SwingConstants.RIGHT);
		chromoGapLenRatioField.setText("0.05");
		chromoGapLenRatioField.setBounds(405, 106, 39, 19);
		contentPanel.add(chromoGapLenRatioField);
		chromoGapLenRatioField.setColumns(10);

		JCheckBox moveWithinChromCheckBox = new JCheckBox("Move within chrom");
		moveWithinChromCheckBox.setBounds(455, 105, 165, 21);
		contentPanel.add(moveWithinChromCheckBox);

		this.scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 133, 608, 79);
		contentPanel.add(scrollPane);

		this.messageArea = new JTextArea();
		this.messageArea.setEditable(false);
		this.messageArea.setForeground(java.awt.Color.RED);
		scrollPane.setViewportView(messageArea);

		this.refspComboBox = new RefspComboBox(this.viewer.getOption());
		this.refspComboBox.setBounds(87, 10, 146, 21);
		contentPanel.add(this.refspComboBox);


		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							ViewOptionDialog.this.getOption();
							if (ViewOptionDialog.this.openFile()) {
								ViewOptionDialog.this.dispose();
							}
						} catch (Exception ex) {
							logger.error(ex.getMessage(), ex);
						}
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						ViewOptionDialog.this.dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}

	}

	/**
	 * オプションの値を設定する。
	 */
	public void setOption() throws Exception {
		CompareMapOpt opt = this.viewer.getOption();
		logger.debug("getRefsp:" + opt.getRefsp());

		this.paperWidthField.setText(Integer.toString(opt.getPaper_width()));
		this.paperHeightField.setText(Integer.toString(opt.getPaper_height()));
		this.centerGeneField.setText(opt.getCenterGene());
		this.centerField.setText(opt.getCenterPosStr());
		this.paperSizeField.setText(opt.getPaper_size());
		this.consRateField.setText(Double.toString(opt.getParam().ConsRatio));
		this.viewWidthField.setText(Integer.toString(opt.getViewWidth()));
		this.colorModeField.setText(opt.getColorMode());
		this.chromoGapLenField.setText(Integer.toString(opt.getGapLen()));
		this.chromoGapLenRatioField.setText(Double.toString(opt.getChromGapLenRatio()));
		this.colorIslandModeCheckBox.setSelected(opt.isColorIslandMode());
	}

	/**
	 * フィールドのテキストを取得する。
	 * @param field フィールド。
	 * @return テキスト。
	 */
	private String getText(JTextField field) {
		if (field.getText().length() == 0) {
			return null;
		} else {
			return field.getText();
		}
	}

	/**
	 * optionの値を取得する。
	 * @return option.
	 */
	private CompareMapOpt getOption() {
		CompareMapOpt opt = this.viewer.getOption();
		opt.setPaper_width(Integer.parseInt(this.paperWidthField.getText()));
		opt.setPaper_height(Integer.parseInt(this.paperHeightField.getText()));
		opt.setRefsp((String) this.refspComboBox.getSelectedItem());
		opt.setCenterGene(this.getText(this.centerGeneField));
		opt.setCenterPosStr(this.getText(this.centerField));
		opt.setPaper_size(this.getText(this.paperSizeField));
		opt.getParam().ConsRatio = Double.parseDouble(this.consRateField.getText());
		opt.setViewWidth(Integer.parseInt(this.viewWidthField.getText()));
		opt.setColorMode(this.getText(this.colorModeField));
		opt.setColorIslandMode(this.colorIslandModeCheckBox.isSelected());
		opt.setGapLen(Integer.parseInt(this.chromoGapLenField.getText()));
		opt.setChromGapLenRatio(Double.parseDouble(this.chromoGapLenRatioField.getText()));

		opt.dump();

		return opt;
	}

	public static final java.awt.Color ERROR_COLOR = java.awt.Color.PINK;


	/**
	 * 空白かどうかをチェックする。
	 * @param field フィールド。
	 * @return 空白の場合true。
	 */
	private boolean isBlank(JTextField field) {
		String text = field.getText();
		if (text.length() == 0) {
			return true;
		}
		return false;
	}

	/**
	 * 整数チェックを行う、
	 * @param field フィールド。
	 * @return 整数の場合true。
	 */
	private boolean isInteger(JTextField field) {
		boolean ret = true;
		try {
			int v = Integer.parseInt(field.getText());
			logger.debug("int value=" + v);
		} catch (NumberFormatException e) {
			ret = false;
			;
		}
		return ret;
	}


	/**
	 * 整数チェックを行う、
	 * @param field フィールド。
	 * @return 整数の場合true。
	 */
	private boolean isDouble(JTextField field) {
		boolean ret = true;
		try {
			double v = Double.parseDouble(field.getText());
			logger.debug("double value=" + v);
		} catch (NumberFormatException e) {
			ret = false;
			;
		}
		return ret;
	}

	/**
	 * データの確認。
	 * @return 問題なければtrue。
	 */
	private boolean validateData() {
		this.consRateField.setBackground(java.awt.Color.WHITE);
		this.paperWidthField.setBackground(java.awt.Color.WHITE);
		this.paperHeightField.setBackground(java.awt.Color.WHITE);
		this.viewWidthField.setBackground(java.awt.Color.WHITE);
		this.chromoGapLenField.setBackground(java.awt.Color.WHITE);
		this.chromoGapLenRatioField.setBackground(java.awt.Color.WHITE);

		String message = "";
		this.messageArea.setText(message);
		boolean ret = true;
		if (this.isBlank(this.consRateField)) {
			this.consRateField.setBackground(ERROR_COLOR);
			message += "Cons Rate is required.\n";
			ret = false;
		}
		if (this.isBlank(this.paperWidthField)) {
			this.paperWidthField.setBackground(ERROR_COLOR);
			message += "Width is required.\n";
			ret = false;
		}
		if (this.isBlank(this.paperHeightField)) {
			this.paperHeightField.setBackground(ERROR_COLOR);
			message += "Height is required.\n";
			ret = false;
		}
		if (this.isBlank(this.viewWidthField)) {
			this.viewWidthField.setBackground(ERROR_COLOR);
			message += "View width is required.\n";
			ret = false;
		}
		if (this.isBlank(this.chromoGapLenField)) {
			this.chromoGapLenField.setBackground(ERROR_COLOR);
			message += "Chromo Gap Len is required.\n";
			ret = false;
		}
		if (this.isBlank(this.chromoGapLenRatioField)) {
			this.chromoGapLenRatioField.setBackground(ERROR_COLOR);
			message += "Chromo Gap Len Ratio is required.\n";
			ret = false;
		}

		if (ret) {
			if (!this.isDouble(this.consRateField)) {
				this.consRateField.setBackground(ERROR_COLOR);
				message += "Cons Rate must be an double.\n";
				ret = false;
			}
			if (!this.isInteger(this.paperWidthField)) {
				this.paperWidthField.setBackground(ERROR_COLOR);
				message += "Width must be an integer.\n";
				ret = false;
			}
			if (!this.isInteger(this.paperHeightField)) {
				this.paperHeightField.setBackground(ERROR_COLOR);
				message += "Height must be an integer.\n";
				ret = false;
			}
			if (!this.isInteger(this.viewWidthField)) {
				this.viewWidthField.setBackground(ERROR_COLOR);
				message += "View Width must be an integer.\n";
				ret = false;
			}
			if (!this.isInteger(this.chromoGapLenField)) {
				this.chromoGapLenField.setBackground(ERROR_COLOR);
				message += "Chromo Gap Len must be an integer.\n";
				ret = false;
			}
			if (!this.isDouble(this.chromoGapLenRatioField)) {
				this.chromoGapLenRatioField.setBackground(ERROR_COLOR);
				message += "Chromo Gap Len Ratio must be an double.\n";
				ret = false;
			}
		}
		if (!ret) {
			this.messageArea.setText(message);
		}
		return ret;
	}

	/**
	 * ファイルをオープンする。
	 */
	private boolean openFile() throws Exception {
		if (!this.validateData()) {
			return false;
		}
		CompareMapOpt opt = this.getOption();
		if (opt != null) {
			logger.debug("openFile coreFile=" + opt.getCorefile());
			opt.readData();
			this.viewer.getDrawer().setOpt(opt);
			logger.debug("isBlank=" + opt.getCoreGenome().isBlank());
			this.viewer.repaint();
		}
		return true;
	}

}
