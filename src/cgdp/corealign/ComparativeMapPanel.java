package cgdp.corealign;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cgdp.util.WebDBConfUtil;
import cgdp.util.WebDBConfUtil.WebDB;

/** Panel for drawing comparative map with mouse listener */
public class ComparativeMapPanel extends JPanel implements MouseListener, KeyListener{

	/**
	 *
	 */
	private static final long serialVersionUID = -8950241954176375014L;

	/**
	 * Logger。
	 */
	private Logger logger = LogManager.getLogger(ComparativeMapPanel.class);

	private ComparativeMapDrawer drawer;
	private boolean multiSelectionMode = false;
	private boolean contiguousSelectionMode = false;


	public ComparativeMapPanel(ComparativeMapDrawer _drawer) {
		super();
		drawer = _drawer;
		this.addMouseListener(this);
		this.addKeyListener(this);
	}
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		setBackground(Color.white);
		Dimension dim = getSize();
		Graphics2D g2 = (Graphics2D) g;
		drawer.setGraphics(g2);
		drawer.setParameters(dim);
		setPreferredSize(new Dimension(drawer.drawWidth, drawer.drawHeight));
		drawer.drawData();
		requestFocus();
	}

	/* keyboard listner methods follow */
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_CONTROL || e.getKeyCode() == KeyEvent.VK_META) {
			multiSelectionMode = true;
		} else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			contiguousSelectionMode = true;
// System.out.println("key pressed:"+e.getKeyCode()+" "+KeyEvent.VK_SHIFT);
		}
	}
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_CONTROL || e.getKeyCode() == KeyEvent.VK_META) {
			multiSelectionMode = false;
		} else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			contiguousSelectionMode = false;
		}
	}
	public void keyTyped(KeyEvent e) {
	}

	/* mouse listner methods follow */
	public void mouseClicked(MouseEvent e) {
		logger.debug("multiSelectionMode = " + this.multiSelectionMode);
		logger.debug("contiguousSelectionMode = " + this.contiguousSelectionMode);
		boolean addFlag = false;
		if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
			// Come here not only single click cases but also double click cases
			if (this.multiSelectionMode || this.contiguousSelectionMode) {
				addFlag = true;
			}
			drawer.selectClickedPos(e.getX(),  e.getY(), this.multiSelectionMode, this.contiguousSelectionMode);
			repaint();
		} else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
			// double click
			if (multiSelectionMode) {
				addFlag = true;
			}
			// double click to align center orthologs
			drawer.resetCenterPositions(e.getX(),  e.getY(),  addFlag);
			repaint();
		} else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON3) {
			// right click


//			int pos = drawer.get_viewPosition_from_x_clickedpos(e.getX());
//			String spName = drawer.get_spName_from_y_clickedpos(e.getY());
			GenomicLocus loc = drawer.getClickedLocus(e.getX(), e.getY());
			Gene g = loc.getGene(drawer.genomeData);
System.out.println("gene="+g);

			showGeneInfoPopupMenu(e, g);

/*
			String geneInfo = g.geneInfoString();
			CoreCluster cc = drawer.coreGenome.getClusterByGene(g.getSpName());
			if (cc != null) {
				String clusterInfo = cc.clusterInfoString();
				JOptionPane.showMessageDialog(fr, clusterInfo+geneInfo);
			}
*/
		}
/*
		int pos = drawer.get_viewPosition_from_x_clickedpos(e.getX());
		String spName = drawer.get_spName_from_y_clickedpos(e.getY());

		System.out.println(e.getX()+" -> "+pos+"; "
					+e.getY()+" "+spName);
*/
	}
	public void mousePressed(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	void showGeneInfoPopupMenu(MouseEvent e, Gene gene) {
		CompareMapOpt opt = this.drawer.mapViewer.getOption();

		JPopupMenu popup = new JPopupMenu();
		MouseEvent mouseEvent = e;
		logger.info("menu x=" + e.getX() + ", y=" + e.getY());
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (((JMenuItem)e.getSource()).getName().equals("Message")) {
					JFrame fr = new JFrame();
					String geneInfo = gene.geneInfoString();
					CoreCluster cc = drawer.coreGenome.getClusterByGene(gene.getSpName());
					if (cc != null) {
						String clusterInfo = cc.clusterInfoString();
						logger.debug("clusterId=" + cc.id());
						String ann = opt.getAnnotation(cc.id());
						if (ann != null) {
							geneInfo += "Annotation: " + ann + "\n";
						}
						JOptionPane.showMessageDialog(fr, clusterInfo + geneInfo);
					}
				} else if (((JMenuItem)e.getSource()).getName().equals("Center")) {
					ComparativeMapPanel.this.drawer.selectCenter(mouseEvent);
				}
			}
		};

		List<WebDB> list = WebDBConfUtil.getWebDBList();
		for (WebDB webdb : list) {
			JMenuItem webdbItem = new JMenuItem("Search on Web DB " + webdb.getName());
			logger.debug("Adding webdb menu: " + webdb.getName());
			webdbItem.setName("WebDB:" + webdb.getName());
			webdbItem.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					this.showWebDB(gene, webdb);
				}

				private void showWebDB(final Gene gene, final WebDB webdb) {
					String url = webdb.getUrl();
					logger.debug("URL template: " + url);
					if (gene == null) {
						logger.debug("gene is null");
						return;
					}
					logger.debug("gene: " + gene.toString());
					url = url.replace("${sp}", gene.sp);
					url = url.replace("${name}", gene.name);
					try {
						URI uri = new URI(url);
						Desktop desktop = Desktop.getDesktop();
						desktop.browse(uri);
					} catch (Exception ex) {
					}
				}
			});
			popup.add(webdbItem);
		}

		JMenuItem messageBox = new JMenuItem("Show info on a message box");
		JMenuItem selectCenter = new JMenuItem("Select inparalog");
		messageBox.setName("Message");
		selectCenter.setName("Center");
		messageBox.addActionListener(al);
		selectCenter.addActionListener(al);
		popup.add(messageBox);
		popup.add(selectCenter);
		popup.show(this, e.getX(), e.getY());
	}
}
