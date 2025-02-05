package cgdp.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.corealign.ComparativeMapViewer;
import cgdp.corealign.CoreGenome;
import cgdp.corealign.GenomeData;

/**
 * 中心を選択するダイアログ。
 */
public class SelectInparalogDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(SelectInparalogDialog.class);

	private final JPanel contentPanel = new JPanel();

	/**
	 * Viewer.
	 */
	private ComparativeMapViewer viewer = null;

	/**
	 * 生物種リスト。
	 */
	private JList<String> speciesListBox = null;

	/**
	 * 名称リスト。
	 */
	private JList<String> nameListBox = null;

	/**
	 * OKボタン。
	 */
	private JButton okButton = null;

	/**
	 * 生物種と名称リストのマップ。
	 */
	private Map<String, List<String>> spLocusListMap = null;
	private JTextField clusterIDField;

	/**
	 * Create the dialog.
	 */
	public SelectInparalogDialog(ComparativeMapViewer viewer) {
		setTitle("Select Inparalog");
		setBounds(100, 100, 450, 328);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		{
			JLabel lblNewLabel_2 = new JLabel("Cluster ID : ");
			lblNewLabel_2.setBounds(12, 13, 76, 13);
			contentPanel.add(lblNewLabel_2);
			clusterIDField = new JTextField();
			clusterIDField.setEditable(false);
			clusterIDField.setHorizontalAlignment(SwingConstants.RIGHT);
			clusterIDField.setBounds(100, 10, 96, 19);
			contentPanel.add(clusterIDField);
			clusterIDField.setColumns(10);
		}
		{
			JLabel lblNewLabel = new JLabel("Species");
			lblNewLabel.setBounds(12, 36, 92, 13);
			contentPanel.add(lblNewLabel);
		}

		JLabel lblNewLabel_1 = new JLabel("Name");
		lblNewLabel_1.setBounds(189, 36, 131, 13);
		contentPanel.add(lblNewLabel_1);

		JScrollPane speciesScrollPane = new JScrollPane();
		speciesScrollPane.setBounds(12, 59, 165, 187);
		contentPanel.add(speciesScrollPane);

		this.speciesListBox = new JList<String>();
		speciesListBox.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				SelectInparalogDialog.this.onSelectSpecies();
			}
		});
		speciesScrollPane.setViewportView(this.speciesListBox);
		{
			JScrollPane nameScrollPane = new JScrollPane();
			nameScrollPane.setBounds(189, 59, 233, 189);
			contentPanel.add(nameScrollPane);
			{
				this.nameListBox = new JList<String>();
				nameListBox.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getClickCount() == 2) {
							SelectInparalogDialog.this.onOk();
						}
					}
				});
				nameListBox.addListSelectionListener(new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						SelectInparalogDialog.this.onSelectName();
					}
				});
				nameScrollPane.setViewportView(nameListBox);
			}
		}

		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				this.okButton = new JButton("OK");
				this.okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SelectInparalogDialog.this.onOk();
					}
				});
				this.okButton.setActionCommand("OK");
				buttonPane.add(this.okButton);
				getRootPane().setDefaultButton(this.okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						SelectInparalogDialog.this.onCancel();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		this.viewer = viewer;
		this.setData();
		this.setButtonStatus();
	}

	/**
	 * ボタンの状態設定。
	 */
	private void setButtonStatus() {
		int spidx = this.speciesListBox.getSelectedIndex();
		int nmidx = this.nameListBox.getSelectedIndex();
		if (spidx >= 0 && nmidx >= 0) {
			this.okButton.setEnabled(true);
		} else {
			this.okButton.setEnabled(false);
		}
	}

	/**
	 * 生物種リスト。
	 */
	private List<String> spList = null;
	/**
	 * 名称リスト。
	 */
	private List<String> nameList = null;

	/**
	 * データの設定。
	 */
	private void setData() {
		DefaultListModel<String> listModel = new DefaultListModel<String>();
		List<String> splist = this.viewer.getDrawer().getCompMap().getCurr_spList();
		if (splist != null) {
			this.spList = new ArrayList<String>();
			this.clusterIDField.setText(this.viewer.getDrawer().getCompMap().getSelectedClusterID());
			String name = this.viewer.getDrawer().getCompMap().getSelectedClusterID();
			logger.info("Selected cluster name=" + name);
			this.spLocusListMap = this.viewer.getDrawer().getCompMap().getSpLocusListMap();
			for (String sp: splist) {
				List<String> nameList = this.spLocusListMap.get(sp);
				if (nameList != null) {
					this.spList.add(sp);
					listModel.addElement(sp + "(" + nameList.size() + ")");
				}
			}
			this.speciesListBox.setModel(listModel);
		}
		this.selectLocus();
		this.setButtonStatus();
	}

	/**
	 * 中央のGeneを選択する。
	 */
	private void selectLocus() {
		String locus = this.viewer.getLocusInput().getText();
		GenomeData genomeData = this.viewer.getDrawer().getGenomeData();
		CoreGenome coreGenome = this.viewer.getDrawer().getCoreGenome();
		String ret = coreGenome.getSpName(locus, genomeData);
		logger.info("locus=" + locus + " -> " + ret);
		String [] sp = null;
		if (ret != null) {
			sp = ret.split(":");
			int idx = this.spList.indexOf(sp[0]);
			if (idx >= 0) {
				this.speciesListBox.setSelectedIndex(idx);
				this.onSelectSpecies();
				int idx1 = this.nameList.indexOf(sp[1]);
				this.nameListBox.setSelectedIndex(idx1);
			}
		}
	}

	/**
	 * 生物種選択時の動作。
	 */
	private void onSelectSpecies() {
		int idx = this.speciesListBox.getSelectedIndex();
		if (idx >= 0) {
			String sp = this.speciesListBox.getSelectedValue().replaceAll("\\(\\d+\\)$", "");
			logger.info("sp=" + sp);
			this.nameList = new ArrayList<String>();
			DefaultListModel<String> listModel = new DefaultListModel<String>();
			List<String> nameList = this.spLocusListMap.get(sp);
			if (nameList != null && nameList.size() > 0) {
				this.nameList.addAll(nameList);
				listModel.addAll(nameList);
			}
			this.nameListBox.setModel(listModel);
		}
		this.setButtonStatus();
	}

	private void onSelectName() {
		this.setButtonStatus();
	}


	/**
	 * OK塗ボタン押下時の処理。
	 */
	private void onOk() {
		int spidx = this.speciesListBox.getSelectedIndex();
		if (spidx >= 0) {
			String sp = this.speciesListBox.getSelectedValue().replaceAll("\\(\\d+\\)$", "");
			logger.info("sp=" + sp);
			int nmidx = this.nameListBox.getSelectedIndex();
			if (nmidx >= 0) {
				String locus = sp + ":" + this.nameListBox.getSelectedValue();
				this.viewer.getLocusInput().setText(locus);
				this.viewer.getDrawer().setCenterPosByStr(locus, true);
				this.viewer.repaint();
			}
		}

		this.dispose();
	}

	/**
	 * Cancelボタン押下時の処理。
	 */
	private void onCancel() {
		this.dispose();
	}
}
