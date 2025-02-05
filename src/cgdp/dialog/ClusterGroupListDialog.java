package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.component.ClusterGroupTable;
import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CompareMapOpt.ClusterGroup;

/**
 * クラスタグループ一覧ダイアログ。
 *
 */
public class ClusterGroupListDialog extends JDialog {

	/**
	 *
	 */
	private static final long serialVersionUID = -7453887047467710418L;

	private static Logger logger = LogManager.getLogger(ClusterGroupListDialog.class);

	private final JPanel contentPanel = new JPanel();
	private JTable table;
	/**
	 * Viewer。
	 */
	private ComparativeMapViewer viewer = null;

	/**
	 * 編集対象リスト。
	 */
	private List<ClusterGroup> list = null;

	/**
	 * コンストラクタ。
	 * @param viewer Viewer。
	 */
	public ClusterGroupListDialog(final ComparativeMapViewer viewer) {
		this.viewer = viewer;
		this.list = new ArrayList<ClusterGroup>();
		setTitle("Cluster group list");
		setBounds(100, 100, 489, 279);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JScrollPane scrollPane = new JScrollPane();
			contentPanel.add(scrollPane, BorderLayout.CENTER);
			{
				try {
					for (ClusterGroup cg: this.viewer.getOption().getClusterGroupList()) {
						this.list.add(cg.clone());
					}
					table = new ClusterGroupTable (this.list);
					scrollPane.setViewportView(table);
				} catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener((ActionEvent e) -> {
					ClusterGroupListDialog.this.onOk();
					ClusterGroupListDialog.this.dispose();
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener((ActionEvent e) -> {
						ClusterGroupListDialog.this.dispose();
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	/**
	 * OKボタンの処理。
	 */
	private void onOk() {
		this.viewer.getOption().getClusterGroupList().clear();
		this.viewer.getOption().getClusterGroupList().addAll(this.list);
		this.viewer.repaint();
	}
}
