package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.corealign.ComparativeMapViewer;

/**
 * 検索ダイアログ。
 *
 */
public class SearchNameDialog extends JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 7947341415520147101L;

	/**
	 * logger.
	 */
	private static Logger logger = LogManager.getLogger(SearchNameDialog.class);

	private final JPanel contentPanel = new JPanel();

	/**
	 * Viewer.
	 */
	private ComparativeMapViewer viewer = null;

	private JLabel lblTypeLabel = null;
	private JRadioButton rdbtnName = null;
	private JRadioButton rdbtnClustid = null;
	private JTextField textField = null;
	private JList<String> resultList = null;
	private DefaultListModel<String> listModel = null;
	private JButton okButton = null;

	/**
	 * Create the dialog.
	 */
	public SearchNameDialog(final ComparativeMapViewer viewer) {
		setTitle("Search gene");
		this.viewer = viewer;
		setBounds(100, 100, 435, 322);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		this.lblTypeLabel = new JLabel("Name : ");
		lblTypeLabel.setBounds(12, 33, 79, 13);
		contentPanel.add(lblTypeLabel);

		textField = new JTextField();
		textField.setBounds(103, 30, 201, 19);
		contentPanel.add(textField);
		textField.setColumns(10);
		{
			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setBounds(12, 77, 395, 165);
			contentPanel.add(scrollPane);
			{
				SearchNameDialog.this.listModel = new DefaultListModel<String>();
				SearchNameDialog.this.resultList = new JList<String>(SearchNameDialog.this.listModel);
				resultList.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getClickCount() == 2) {
							SearchNameDialog.this.onOk();
							SearchNameDialog.this.dispose();
						}
					}
				});
				resultList.addListSelectionListener((ListSelectionEvent e) -> {
					logger.debug("selChange");
					if (SearchNameDialog.this.resultList.getSelectedIndex() >= 0) {
						SearchNameDialog.this.okButton.setEnabled(true);
					} else {
						SearchNameDialog.this.okButton.setEnabled(false);
					}
				});
				scrollPane.setViewportView(resultList);
			}
		}
		{
			JLabel lblNewLabel_1 = new JLabel("Result : ");
			lblNewLabel_1.setBounds(12, 56, 50, 13);
			contentPanel.add(lblNewLabel_1);
		}

		JButton btnNewButton = new JButton("Search");
		btnNewButton.addActionListener((ActionEvent e) -> {
			SearchNameDialog.this.search();
		});
		btnNewButton.setBounds(316, 29, 91, 21);
		contentPanel.add(btnNewButton);

		this.rdbtnName = new JRadioButton("Name");
		this.rdbtnName.addActionListener((ActionEvent e) ->{
			SearchNameDialog.this.setButtonStatus();
		});
		this.rdbtnName.setBounds(103, 6, 154, 21);
		contentPanel.add(rdbtnName);

		this.rdbtnClustid = new JRadioButton("Cluster ID.");
		this.rdbtnClustid.addActionListener((ActionEvent e) ->{
			SearchNameDialog.this.setButtonStatus();
		});
		rdbtnClustid.setBounds(265, 6, 126, 21);
		contentPanel.add(rdbtnClustid);

		ButtonGroup buttonGroup = new ButtonGroup();
		rdbtnName.setSelected(true);
		buttonGroup.add(rdbtnName);
		buttonGroup.add(rdbtnClustid);

		JLabel lblNewLabel = new JLabel("Type:");
		lblNewLabel.setBounds(12, 10, 50, 13);
		contentPanel.add(lblNewLabel);

		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				SearchNameDialog.this.okButton = new JButton("OK");
				SearchNameDialog.this.okButton.addActionListener((ActionEvent e) -> {
					SearchNameDialog.this.onOk();
					SearchNameDialog.this.dispose();
				});
				SearchNameDialog.this.okButton.setActionCommand("OK");
				buttonPane.add(SearchNameDialog.this.okButton);
				getRootPane().setDefaultButton(SearchNameDialog.this.okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener((ActionEvent e) -> {
					SearchNameDialog.this.dispose();
				});
				buttonPane.add(cancelButton);
			}
		}
		this.okButton.setEnabled(false);
	}

	/**
	 * ボタンの状態を設定する。
	 */
	private void setButtonStatus() {
		logger.debug("type change:" + this.rdbtnName.isSelected());
		logger.debug("type change:" + this.rdbtnClustid.isSelected());
		if (this.rdbtnName.isSelected()) {
			logger.debug("name");
			this.lblTypeLabel.setText("Name:");
		} else {
			logger.debug("clust id.");
			this.lblTypeLabel.setText("Cluster ID.:");
		}
	}


	/**
	 * 名称候補検索。
	 */
	private void search() {
		this.listModel.clear();
		boolean clustid = this.rdbtnClustid.isSelected();
		String text = this.textField.getText();
		List<String> list = this.viewer.searchGeneName(text, clustid);
		for (String g: list) {
			this.listModel.addElement(g);
		}
	}

	/**
	 * 検索処理。
	 */
	private void onOk() {
		String name = this.resultList.getSelectedValue();
		name = name.replaceAll("\\(\\d+\\)", "");
		logger.debug("name=" + name);
		this.viewer.getLocusInput().setText(name);
		this.viewer.getDrawer().setCenterPosByStr(name, true);
		this.viewer.repaint();
	}
}
