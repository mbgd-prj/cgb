package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.component.ClusterGroupTable;
import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CompareMapOpt.ClusterGroup;
import lombok.Getter;
import net.arnx.jsonic.JSON;

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
	private ClusterGroupTable table;
	/**
	 * Viewer。
	 */
	private ComparativeMapViewer viewer = null;

	/**
	 * 編集対象リスト。
	 */
	@Getter
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
					table = new ClusterGroupTable(this);
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
				JButton saveAllGroupButton = new JButton("Save all group");
				saveAllGroupButton.addActionListener((ActionEvent e) -> {
					ClusterGroupListDialog.this.saveAllGroups();
				});
				saveAllGroupButton.setActionCommand("SaveAllGroup");
				buttonPane.add(saveAllGroupButton);
			}
			{
				JButton loadGroupButton = new JButton("Load group");
				loadGroupButton.addActionListener((ActionEvent e) -> {
					ClusterGroupListDialog.this.loadGroup();
				});
				loadGroupButton.setActionCommand("LoadGroup");
				buttonPane.add(loadGroupButton);
			}
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
		logger.debug("this.list=" + JSON.encode(this.list, true));
		this.viewer.getOption().getClusterGroupList().addAll(this.list);
		this.viewer.repaint();
	}

	/**
	 * グループ情報を保存します。
	 * @param idx グループインデックス。
	 * @throws Exception 例外。
	 */
	public void saveGroup(int idx) throws Exception {
		logger.debug("saveGroup idx=" + idx);
		String file = this.viewer.getOption().getDefaultGroupFile(this.list.get(idx).getName());
		logger.debug("basePath=" + file);
		JFileChooser dlg = new JFileChooser();
		dlg.addChoosableFileFilter(new FileNameExtensionFilter("Status File(*.grp)", "grp"));

		dlg.setAcceptAllFileFilterUsed(false);
		dlg.setSelectedFile(new File(file));
		int selected = dlg.showSaveDialog(this);
		if (selected  == JFileChooser.APPROVE_OPTION) {
			File f = dlg.getSelectedFile();
			logger.debug("f=" + f.getAbsolutePath());
			this.viewer.getOption().saveClusterGroup(idx, f.getAbsolutePath());
		}
	}

	/**
	 * 全グループ保存。
	 */
	public void saveAllGroups()  {
		try {
			int cnt = 0;
			if (JOptionPane.showConfirmDialog(this, "Do you want to save all groups?", null, JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				return;
			}
			for (int i = 0; i < this.list.size(); i++) {
				ClusterGroup cg = this.list.get(i);
				String f = this.viewer.getOption().getDefaultGroupFile(cg.getName());
				File file = new File(f);
				if (file.exists()) {
					String msg = "'" + file.getAbsolutePath() + "' is exists. Overwrite it ?";
					if (JOptionPane.showConfirmDialog(this, msg, null, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						this.viewer.getOption().saveClusterGroup(i, file.getAbsolutePath());
						cnt++;
					};
				} else {
					this.viewer.getOption().saveClusterGroup(i, file.getAbsolutePath());
					cnt++;
				}
			}
			String msg = cnt + " groups saved.";
			JOptionPane.showMessageDialog(this, msg);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			JOptionPane.showMessageDialog(this, ex.getMessage());
		}
	}

	/**
	 * グループ情報を読み込みます。
	 */
	public void loadGroup() {
		try {
			String basePath = this.viewer.getOption().getDefaultGroupDir();
			JFileChooser dlg = new JFileChooser();
			dlg.setMultiSelectionEnabled(true);
			dlg.setCurrentDirectory(new File(basePath));
			dlg.setAcceptAllFileFilterUsed(false);
			dlg.addChoosableFileFilter(new FileNameExtensionFilter("Status File(*.grp)", "grp"));
			int selected = dlg.showOpenDialog(this);
			if (selected  == JFileChooser.APPROVE_OPTION) {
				File[] filelist = dlg.getSelectedFiles();
				for (File f:filelist) {
					this.viewer.getOption().loadClusterGroup(f);
				}
				List<ClusterGroup> cflist = new ArrayList<ClusterGroup>();
				cflist.addAll(this.viewer.getOption().getClusterGroupList());
				this.list = cflist;
				this.table.setClusterGroupList(this.list);
				this.viewer.repaint();
				this.viewer.setCenterPos();
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			JOptionPane.showMessageDialog(this, ex.getMessage());
		}
	}
}
