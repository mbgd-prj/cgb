package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CompareMapOpt;
import cgdp.util.ColorUtil;

/**
 * クラスタグルーフ編集ダイアログ。
 *
 */
public class ClusterGroupEditDialog extends JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 2445007279077047639L;
	private final JPanel contentPanel = new JPanel();
	private JTextField nameField;
	private JTextField colorField;
	private JCheckBox visibleCheckBox = null;
	private ComparativeMapViewer viewer = null;



	/**
	 * Create the dialog.
	 */
	public ClusterGroupEditDialog() {
		setTitle("Cluster group");
		setBounds(100, 100, 450, 146);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		{
			JLabel lblNewLabel = new JLabel("Name : ");
			lblNewLabel.setBounds(12, 10, 50, 13);
			contentPanel.add(lblNewLabel);
		}

		nameField = new JTextField();
		nameField.setBounds(63, 7, 359, 19);
		contentPanel.add(nameField);
		nameField.setColumns(10);

		this.visibleCheckBox = new JCheckBox("Visible");
		this.visibleCheckBox.setSelected(true);
		this.visibleCheckBox.setBounds(225, 37, 103, 21);
		contentPanel.add(this.visibleCheckBox);

		JLabel lblNewLabel_1 = new JLabel("Color : ");
		lblNewLabel_1.setBounds(12, 41, 50, 13);
		contentPanel.add(lblNewLabel_1);

		colorField = new JTextField();
		colorField.setText("#009999");
		colorField.setBounds(63, 38, 96, 19);
		contentPanel.add(colorField);
		colorField.setColumns(10);

		JButton colorButton = new JButton("...");
		colorButton.addActionListener((ActionEvent e) -> {
			ClusterGroupEditDialog.this.selectColor();
		});
		colorButton.setBounds(166, 37, 29, 21);
		contentPanel.add(colorButton);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener((ActionEvent e) -> {
					ClusterGroupEditDialog.this.saveClusterGroup();
					ClusterGroupEditDialog.this.dispose();
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener((ActionEvent e) -> {
					ClusterGroupEditDialog.this.dispose();
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	/**
	 * コンストラクタ。
	 * @param viewer viewer。
	 */
	public ClusterGroupEditDialog(ComparativeMapViewer viewer) {
		this();
		this.viewer = viewer;
		this.nameField.setText(this.viewer.getOption().getNewGroupName());
		this.colorField.setText(this.viewer.getOption().getNewColor());
	}

	/**
	 * 色を選択する。
	 */
	private void selectColor() {
		String code = (String) this.colorField.getText();
		Color c = ColorUtil.getColor(code);
		Color color = JColorChooser.showDialog(this, "Color", c);
		if (color != null) {
			String rgb = ColorUtil.getColorCode(color);
			this.colorField.setText(rgb);
		}
	}

	/**
	 * クラスタグループを登録する。
	 */
	private void saveClusterGroup() {
		String name = this.nameField.getText();
		boolean visible = this.visibleCheckBox.isSelected();
		String color = this.colorField.getText();
		CompareMapOpt.ClusterGroup g = new CompareMapOpt.ClusterGroup(name, visible, color);
		this.viewer.addClusterGroup(g);
	}
}
