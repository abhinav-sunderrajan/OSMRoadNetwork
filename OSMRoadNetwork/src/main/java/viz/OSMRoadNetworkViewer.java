package viz;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import rnwmodel.OSMNode;
import rnwmodel.OSMRoad;
import rnwmodel.OSMRoadNetworkModel;

public class OSMRoadNetworkViewer implements ActionListener, KeyListener {

	// Defines the bounding box for Singapore.

	private class MouseBehavior extends MouseAdapter {
		private double[] prevOffset = null;
		private Point startPoint = null;
		private OSMNode dragNode = null;

		public void mousePressed(MouseEvent e) {
			panel.requestFocusInWindow();

			prevOffset = new double[] { offset[0], offset[1] };
			startPoint = e.getPoint();

			if (e.getSource() instanceof JComponent) {
				((JComponent) e.getSource()).repaint();
			}

			if (selectedNode != null) {
				dragNode = selectedNode;
			}

		}

		public void mouseDragged(MouseEvent e) {
			Point currentPoint = e.getPoint();
			int dx = startPoint.x - currentPoint.x;
			int dy = startPoint.y - currentPoint.y;

			if (SwingUtilities.isLeftMouseButton(e)) {
				offset[0] = prevOffset[0] + dx / zoom;
				offset[1] = prevOffset[1] + dy / zoom;

				updateVisibleNodesAndLinks();
				if (e.getSource() instanceof JComponent) {
					((JComponent) e.getSource()).repaint();
				}
			} else if (SwingUtilities.isRightMouseButton(e) && dragNode != null) {
				int pwidth = panel.getWidth();
				int pheight = panel.getHeight();

				int xo = pwidth / 2;
				int yo = pheight / 2;

				double lon = (currentPoint.x - xo) / zoom + offset[0];
				double lat = -((currentPoint.y - yo) / zoom + offset[1]);

				dragNode.setX(lon);
				dragNode.setY(lat);

				// model.updateNodePosition(dragNode.nodeId, lon, lat);

				updateVisibleNodesAndLinks();
				if (e.getSource() instanceof JComponent) {
					((JComponent) e.getSource()).repaint();
				}
			}

			// pressed = false;
		}

		public void mouseReleased(MouseEvent e) {
			try {
				if (SwingUtilities.isRightMouseButton(e) && dragNode != null) {
				}

				dragNode = null;

				updateVisibleNodesAndLinks();
				if (e.getSource() instanceof JComponent) {
					((JComponent) e.getSource()).repaint();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		public void mouseWheelMoved(MouseWheelEvent e) {
			if (image != null) {
				int units = -e.getUnitsToScroll();

				if (units >= 0) {
					zoom *= 1.5;
				} else {
					zoom /= 1.5;
				}

				updateVisibleNodesAndLinks();
				if (e.getSource() instanceof JComponent) {
					((JComponent) e.getSource()).repaint();
				}
			}
		}

		public void mouseMoved(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			OSMRoadNetworkViewer.this.setMousePosition(x, y);

			if (e.getSource() instanceof JComponent) {
				((JComponent) e.getSource()).repaint();
			}
		}
	}

	private class Panel extends JPanel {
		private static final long serialVersionUID = 1L;

		public Dimension getMinimumSize() {
			return new Dimension(750, 500);
		}

		public Dimension getPreferredSize() {
			return new Dimension(750, 500);
		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (model.getAllNodes().size() > 2) {
				updateView();

				synchronized (lock) {
					if (image != null) {
						g.drawImage(image, 0, 0, null);
					}
				}
			}
		}
	}

	private OSMRoadNetworkModel model = null;
	private double width = -1;
	private double height = -1;
	private double[] offset = null;

	private Panel panel = null;
	private JFrame frame = null;
	private JTextField cmdline = new JTextField();

	private Object lock = new Object();
	private BufferedImage image = null;
	private double zoom = 1000.0;

	private double mnx = Integer.MAX_VALUE;
	private double mxx = Integer.MIN_VALUE;
	private double mny = Integer.MAX_VALUE;
	private double mxy = Integer.MIN_VALUE;

	private Set<OSMNode> visibleNodes = new HashSet<OSMNode>();
	private Set<OSMRoad> visibleLinks = new HashSet<OSMRoad>();

	private int prevMX = 0;
	private int prevMY = 0;

	private BufferedImage background = null;
	private int backgroundXOffset = -1;
	private int backgroundYOffset = -1;

	private boolean[] showFunctionalClass = new boolean[] { true, true, true, true, true };
	private boolean showDirectionArrows = true;

	private OSMNode selectedNode = null;

	public OSMRoadNetworkViewer(String title, OSMRoadNetworkModel model) throws Exception {
		this.model = model;

		for (OSMNode node : model.getBeginAndEndNodes()) {
			if (node.getX() < mnx)
				mnx = node.getX();
			if (node.getX() > mxx)
				mxx = node.getX();
			if (-node.getY() < mny)
				mny = -node.getY();
			if (-node.getY() > mxy)
				mxy = -node.getY();

			width = mxx - mnx;
			height = mxy - mny;
			offset = new double[] { mnx + width / 2, mny + height / 2 };
		}

		MouseBehavior behavior = new MouseBehavior();

		panel = new Panel();
		panel.addMouseMotionListener(behavior);
		panel.addMouseListener(behavior);
		panel.addMouseWheelListener(behavior);
		panel.setFocusable(true);
		panel.requestFocusInWindow();
		panel.addKeyListener(this);

		cmdline.addActionListener(this);

		frame = new JFrame(title);
		frame.setLocation(10, 10);
		frame.setDefaultCloseOperation(3);
		frame.setLayout(new BorderLayout());
		frame.add(panel, BorderLayout.CENTER);
		frame.add(cmdline, BorderLayout.SOUTH);
		frame.pack();

		frame.setVisible(true);
		frame.repaint();
	}

	private void setMousePosition(int x, int y) {

	}

	private void updateVisibleNodesAndLinks() {
		int pwidth = panel.getWidth();
		int pheight = panel.getHeight();

		double vwidth = pwidth / zoom;
		double vheight = pheight / zoom;

		double mnx = offset[0] - 0.5 * vwidth;
		double mxx = offset[0] + 0.5 * vwidth;
		double mny = offset[1] - 0.5 * vheight;
		double mxy = offset[1] + 0.5 * vheight;

		// select all nodes that are within the range
		visibleNodes = new HashSet<OSMNode>();

		for (OSMNode node : model.getBeginAndEndNodes()) {
			if (node.getX() >= mnx && node.getX() <= mxx && -node.getY() >= mny
					&& -node.getY() <= mxy) {
				visibleNodes.add(node);
			}
		}

		// get all visible links
		visibleLinks = new HashSet<OSMRoad>();
		for (OSMRoad road : model.getAllRoads()) {

			boolean isRoadVisible = false;
			for (OSMNode node : road.getRoadNodes()) {
				if (visibleNodes.contains(node)) {
					isRoadVisible = true;
					break;
				}

			}

			if (isRoadVisible) {
				visibleLinks.add(road);
			}

		}
	}

	/**
	 * Updates the view on pan and zoom.
	 */
	private void updateView() {
		if (model.getAllNodes().isEmpty())
			return;

		synchronized (lock) {
			int pwidth = panel.getWidth();
			int pheight = panel.getHeight();

			// draw the nodes and links
			int xo = pwidth / 2;
			int yo = pheight / 2;

			image = new BufferedImage(pwidth, pheight, BufferedImage.TYPE_INT_RGB);

			Graphics g = image.getGraphics();
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, pwidth, pheight);

			if (background != null) {
				g2.drawImage(background, backgroundXOffset, backgroundYOffset, null);
			}

			for (OSMRoad link : visibleLinks) {

				String roadType = link.getRoadType();
				if (roadType.contains("trunk")) {
					g2.setColor(Color.decode("#089675"));
					g2.setStroke(new BasicStroke(2));
				} else if (roadType.contains("primary")) {
					g2.setColor(Color.decode("#7EA4E6"));
					g2.setStroke(new BasicStroke(2));
				} else if (roadType.contains("secondary")) {
					g2.setColor(Color.decode("#DB9A2A"));
					g2.setStroke(new BasicStroke(2));
				} else if (roadType.contains("motorway")) {
					g2.setColor(Color.decode("#C41D44"));
					g2.setStroke(new BasicStroke(4));
				} else if (roadType.contains("tertiary")) {
					g2.setColor(Color.decode("#BF3DA7"));
					g2.setStroke(new BasicStroke(2));
				} else {
					g2.setColor(Color.GRAY);
					g2.setStroke(new BasicStroke(2));
				}

				List<OSMNode> nodeIds = link.getRoadNodes();
				for (int i = 0; i < nodeIds.size() - 1; i++) {

					OSMNode node1 = nodeIds.get(i);
					OSMNode node2 = nodeIds.get(i + 1);

					int x0 = xo + (int) (zoom * (node1.getX() - offset[0]));
					int y0 = yo + (int) (zoom * (-node1.getY() - offset[1]));
					int x1 = xo + (int) (zoom * (node2.getX() - offset[0]));
					int y1 = yo + (int) (zoom * (-node2.getY() - offset[1]));
					g.drawLine(x0, y0, x1, y1);

					if (showDirectionArrows) {

						if (link.isOneWay()) {
							this.drawArrowFromLine(g, x0, y0, x1, y1, 0.5 * Math.PI);
							this.drawArrowFromLine(g, x0, y0, x1, y1, 1.5 * Math.PI);
						} else {
							this.drawArrowFromLine(g, x0, y0, x1, y1, 0.5 * Math.PI);
							this.drawArrowFromLine(g, x0, y0, x1, y1, 1.5 * Math.PI);
							this.drawArrowFromLine(g, x1, y1, x0, y0, 0.5 * Math.PI);
							this.drawArrowFromLine(g, x1, y1, x0, y0, 1.5 * Math.PI);
						}
					}

				}

			}

			for (OSMNode node : visibleNodes) {
				int x0 = xo + (int) (zoom * (node.getX() - offset[0]));
				int y0 = yo + (int) (zoom * (-node.getY() - offset[1]));
				Ellipse2D.Double circle = new Ellipse2D.Double(x0, y0, 8, 8);
				g2.setColor(Color.BLACK);
				g2.drawString(node.getNodeId() + "", x0, y0);
				g2.setColor(Color.YELLOW);
				g2.fill(circle);

			}

			if (selectedNode != null) {
				int x = xo + (int) (zoom * (selectedNode.getX() - offset[0]));
				int y = yo + (int) (zoom * (-selectedNode.getY() - offset[1]));

				g.setColor(Color.pink);
				g.fillOval(x - 5, y - 5, 10, 10);
			}

		}
	}

	private void drawArrowFromLine(Graphics g, int x0, int y0, int x1, int y1, double a) {
		double xm = (x0 + x1) / 2;
		double ym = (y0 + y1) / 2;

		double xx1 = xm - x0;
		double yy1 = ym - y0;

		double xx3 = xx1 * Math.cos(a) - yy1 * Math.sin(a) + x0;
		double yy3 = xx1 * Math.sin(a) + yy1 * Math.cos(a) + y0;

		double dx = xx3 - xm;
		double dy = yy3 - ym;
		double d = Math.hypot(dx, dy);

		double f = (zoom / 20000.0) / d;
		if (Double.isInfinite(f))
			return;

		dx *= f;
		dy *= f;

		double xx4 = xm + dx;
		double yy4 = ym + dy;

		g.drawLine((int) xm, (int) ym, (int) xx4, (int) yy4);
	}

	public void actionPerformed(ActionEvent event) {
		try {
			if (event.getSource() == cmdline) {
				String cmd = cmdline.getText();
				String[] tmp = cmd.split(" ");
				if (tmp[0].equals("show")) {
					if (tmp[1].equals("fclass")) {
						Set<Integer> indices = new HashSet<Integer>();
						String[] tmp2 = tmp[2].split(",");
						for (String idx : tmp2)
							indices.add(Integer.parseInt(idx));

						boolean flag = Boolean.parseBoolean(tmp[3]);

						for (int idx : indices) {
							showFunctionalClass[idx - 1] = flag;
						}
					} else if (tmp[1].equals("background")) {
						String filename = tmp[2];
						int xoffset = Integer.parseInt(tmp[3]);
						int yoffset = Integer.parseInt(tmp[4]);
						double xscale = Double.parseDouble(tmp[5]);
						double yscale = Double.parseDouble(tmp[6]);

						try {
							BufferedImage image = ImageIO.read(new File(filename));

							int width = (int) (image.getWidth() * xscale);
							int height = (int) (image.getHeight() * yscale);

							background = new BufferedImage(width, height,
									BufferedImage.TYPE_INT_ARGB);
							Graphics g = background.getGraphics();
							g.drawImage(image, 0, 0, width, height, null);

							backgroundXOffset = xoffset;
							backgroundYOffset = yoffset;

						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					panel.repaint();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void keyPressed(KeyEvent event) {
	}

	public void keyReleased(KeyEvent event) {
	}

	public void keyTyped(KeyEvent event) {
	}

	public static void main(String[] args) {
		try {
			Class.forName("org.postgresql.Driver");

			Properties dbConnectionProperties = new Properties();
			dbConnectionProperties.load(new FileInputStream(
					"src/main/resources/connection.properties"));

			System.out.println("Loading the OSM road network from database..");
			OSMRoadNetworkModel rnwModel = OSMRoadNetworkModel
					.getOSMRoadNetworkInstance(dbConnectionProperties);

			OSMRoadNetworkViewer editor = new OSMRoadNetworkViewer("link_model", rnwModel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
