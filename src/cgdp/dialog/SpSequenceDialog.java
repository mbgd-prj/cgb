package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import cgdp.component.DnDList;
import cgdp.corealign.ComparativeMapViewer;

/**
 * 生物種順序変更ダイアログ。
 *
 */
public class SpSequenceDialog extends JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 2663403936299284848L;

	/**
	 * Logger.
	 */
//	private Logger logger = LogManager.getLogger(SpSequenceDialog.class);


	private final JPanel contentPanel = new JPanel();

	/**
	 * viewer。
	 */
	private ComparativeMapViewer viewer = null;

	/**
	 * 生物種リスト。
	 */
	private JList<String> specicsList = null;

	/**
	 * Listに表示するデータ。
	 */
	private DefaultListModel<String> listModel = null;

	/**
	 * Create the dialog.
	 */
	public SpSequenceDialog(ComparativeMapViewer viewer) {
		this.listModel = new DefaultListModel<String>();
		this.viewer = viewer;
		setTitle("Specics Sequence");
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		{
			JLabel lblNewLabel = new JLabel("Specics List");
			lblNewLabel.setBounds(12, 10, 132, 13);
			contentPanel.add(lblNewLabel);
		}
		{
			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setBounds(12, 34, 410, 165);
			contentPanel.add(scrollPane);
			{
				this.specicsList = new DnDList<String>(this.listModel);
				scrollPane.setViewportView(specicsList);
			}
		}
		{
			JLabel lblNewLabel_1 = new JLabel("Drag the list to change the order.");
			lblNewLabel_1.setBounds(194, 209, 228, 13);
			contentPanel.add(lblNewLabel_1);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener((ActionEvent e) -> {
					SpSequenceDialog.this.viewer.setSequence(this.listModel);
					SpSequenceDialog.this.dispose();
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener((ActionEvent e) -> {
					SpSequenceDialog.this.dispose();
				});
				buttonPane.add(cancelButton);
			}
		}
		this.setSpToList();
	}

	/**
	 * 生物種のリストを設定する。
	 */
	private void setSpToList() {
		List<String> list = this.viewer.getSpCodeList();
		for (String spcode : list) {
			this.listModel.addElement(spcode);
		}
	}

}
