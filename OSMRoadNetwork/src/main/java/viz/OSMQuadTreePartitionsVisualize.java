package viz;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import rnwmodel.OSMQuadTree;
import rnwmodel.OSMRoad;
import rnwmodel.OSMRoadNetworkModel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * This module is responsible for splitting the entire road network into grids
 * with each grid cell being associated with an arbitrary number of link Ids.
 * 
 * @author abhinav.sunderrajan
 * 
 * 
 */
public class OSMQuadTreePartitionsVisualize implements ActionListener, KeyListener {

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

	private BufferedImage background = null;
	private int backgroundXOffset = -1;
	private int backgroundYOffset = -1;

	private final static double bbTopLeftLon = 103.618;
	private final static double bbTopLeftLat = 1.4697;

	private final static double bbBotleftLat = 1.23843;

	private final static double bbTopRightLon = 104.0263;

	private static Set<Long> partitionLinks = new HashSet<Long>();
	private static Set<Integer> partitionNodes = new HashSet<Integer>();
	private static final GeometryFactory gf = new GeometryFactory();
	private static List<Coordinate> gpsData = new ArrayList<Coordinate>();
	private static List<String> allCells = new ArrayList<String>();
	private static Set<Envelope> allPartitions = new HashSet<Envelope>();
	private static List<OSMQuadTree> coordinatePartitions = new ArrayList<>();
	private static final double STD_DEV = 11.0;
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * Initialize with the title and the {@link OSMRoadNetworkModel}
	 * 
	 * @param title
	 * @param model
	 * @throws Exception
	 */
	public OSMQuadTreePartitionsVisualize(String title, OSMRoadNetworkModel model) throws Exception {
		this.model = model;

		for (OSMNode node : model.getAllNodes().values()) {
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

	@Override
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		try {
			if (arg0.getSource() == cmdline) {
				String cmd = cmdline.getText();
				if (cmd.equalsIgnoreCase("SAVE")) {
					// int width = image.getWidth(), height = image.getHeight();
					// XSPDF.getInstance().setPageSize(500,
					// 240).setPageMargin(0).setImage(image, 0, 0, 500, 240, 0)
					// .createPdf("C:\\Users\\abhinav.sunderrajan\\Desktop\\damn.pdf");
					File outputfile = new File("C:\\Users\\abhinav.sunderrajan\\Desktop\\saved.png");
					ImageIO.write(image, "png", outputfile);

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * The panel for visualization
	 * 
	 */
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

	/**
	 * 
	 * Provides the concrete implementation for the abstract
	 * {@link MouseAdapter} class for detecting mouse events.
	 * 
	 */
	private class MouseBehavior extends MouseAdapter {
		private double[] prevOffset = null;
		private java.awt.Point startPoint = null;
		private OSMNode dragNode = null;

		@Override
		public void mouseDragged(MouseEvent e) {
			java.awt.Point currentPoint = e.getPoint();
			int dx = startPoint.x - currentPoint.x;
			int dy = startPoint.y - currentPoint.y;
			int xo = panel.getWidth() / 2;
			int yo = panel.getHeight() / 2;

			double lon = (currentPoint.x - xo) / zoom + offset[0];
			double lat = -((currentPoint.y - yo) / zoom + offset[1]);

			if (SwingUtilities.isLeftMouseButton(e)) {
				offset[0] = prevOffset[0] + dx / zoom;
				offset[1] = prevOffset[1] + dy / zoom;
				updateVisibleNodesAndLinks();
				if (e.getSource() instanceof JComponent) {
					((JComponent) e.getSource()).repaint();
				}
			} else if (SwingUtilities.isRightMouseButton(e) && dragNode != null) {
				dragNode.setX(lon);
				dragNode.setY(lat);

				// model.updateNodePosition(dragNode.nodeId, lon, lat);

				updateVisibleNodesAndLinks();
				if (e.getSource() instanceof JComponent) {
					((JComponent) e.getSource()).repaint();
				}
			}

		}

		@Override
		public void mousePressed(MouseEvent e) {
			panel.requestFocusInWindow();
			prevOffset = new double[] { offset[0], offset[1] };
			startPoint = e.getPoint();

			if (e.getSource() instanceof JComponent) {
				((JComponent) e.getSource()).repaint();
			}

		}

		@Override
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

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		public void mouseClicked(MouseEvent e) {

			java.awt.Point currentPoint = e.getPoint();
			int xo = panel.getWidth() / 2;
			int yo = panel.getHeight() / 2;

			double lon = (currentPoint.x - xo) / zoom + offset[0];
			double lat = -((currentPoint.y - yo) / zoom + offset[1]);

			System.out.println(lon + ", " + lat);

		}
	}

	/**
	 * Draw an arrow at the end of the link representing the link direction.
	 * 
	 * @param g
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @param a
	 */
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
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, pwidth, pheight);

			if (background != null) {
				g.drawImage(background, backgroundXOffset, backgroundYOffset, null);
			}

			Graphics2D g2 = (Graphics2D) g;

			for (Coordinate position : gpsData) {

				g2.setColor(Color.ORANGE);
				int x0 = xo + (int) (zoom * (position.x - offset[0]));
				int y0 = yo + (int) (zoom * (-position.y - offset[1]));
				Ellipse2D.Double circle = new Ellipse2D.Double(x0, y0, 15, 15);
				g2.fill(circle);

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

					this.drawArrowFromLine(g, x0, y0, x1, y1, 0.5 * Math.PI);
					this.drawArrowFromLine(g, x0, y0, x1, y1, 1.5 * Math.PI);

				}

			}

			for (OSMNode node : visibleNodes) {
				int x0 = xo + (int) (zoom * (node.getX() - offset[0]));
				int y0 = yo + (int) (zoom * (-node.getY() - offset[1]));
				Ellipse2D.Double circle = new Ellipse2D.Double(x0, y0, 5, 5);
				g2.setColor(Color.BLACK);
				g2.fill(circle);

			}

			// Draw bounding box

			g2.setColor(Color.GREEN);
			g2.setStroke(new BasicStroke(3));

			for (Envelope en : allPartitions) {
				int x1 = xo + (int) (zoom * (en.getMinX() - offset[0]));
				int y1 = yo + (int) (zoom * (-en.getMaxY() - offset[1]));
				int x2 = xo + (int) (zoom * (en.getMaxX() - offset[0]));
				int y2 = yo + (int) (zoom * (-en.getMinY() - offset[1]));
				g2.drawRect(x1, y1, Math.abs(x2 - x1), Math.abs(y2 - y1));

			}
			g2.setColor(Color.BLUE);
			for (int i = 0; i < coordinatePartitions.size(); i++) {
				int x1 = xo
						+ (int) (zoom * (coordinatePartitions.get(i).getBounds().getMinX() - offset[0]));
				int y1 = yo
						+ (int) (zoom * (-coordinatePartitions.get(i).getBounds().getMaxY() - offset[1]));
				int x2 = xo
						+ (int) (zoom * (coordinatePartitions.get(i).getBounds().getMaxX() - offset[0]));
				int y2 = yo
						+ (int) (zoom * (-coordinatePartitions.get(i).getBounds().getMinY() - offset[1]));
				g2.drawRect(x1, y1, Math.abs(x2 - x1), Math.abs(y2 - y1));
			}

		}

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

	public static void main(String[] args) {
		try {

			// Create the OSM road network model
			System.out.println("Creating the road-network from database..");

			Properties connectionProperties = new Properties();
			connectionProperties.load(new FileInputStream(
					"src/main/resources/connection.properties"));
			OSMRoadNetworkModel llmodel = OSMRoadNetworkModel
					.getOSMRoadNetworkInstance(connectionProperties);

			long[] allLinks = {};

			for (int i = 0; i < allLinks.length; i++) {
				partitionLinks.add(allLinks[i]);

			}

			System.out.println("Partitioning the road-network using QuadTree..");
			// Use Quadtree for network parttiotioning.
			OSMQuadTree quadTree = new OSMQuadTree(0, new Envelope(new Coordinate(bbTopLeftLon,
					bbTopLeftLat), new Coordinate(bbTopRightLon, bbBotleftLat)));

			for (OSMNode node : llmodel.getBeginAndEndNodes()) {
				quadTree.insert(node);
			}

			// Over laying the crowd-sourced GPS data.

			BufferedReader br = new BufferedReader(new FileReader(new File(
					"samples/crowd-sourced-gps.txt")));
			int loopcount = 0;
			int driveCount = 0;
			long t1 = 0;
			String prevDriveId = "";
			while (br.ready()) {
				if (loopcount == 0) {
					br.readLine();
				} else {
					String split[] = br.readLine().split(";");
					String driveId = split[4];
					Date date = formatter.parse(split[3]);
					Coordinate coordinate = new Coordinate(Double.parseDouble(split[1]),
							Double.parseDouble(split[0]));
					if (driveCount == 0) {
						gpsData.add(coordinate);
						t1 = date.getTime();
					}
					if (driveCount > 0 && prevDriveId.equalsIgnoreCase(driveId)) {
						long t2 = date.getTime();
						if (t2 - t1 > 9000) {
							gpsData.add(coordinate);
							t1 = t2;
						}

					} else {
						if (driveCount > 0) {
							gpsData.add(coordinate);
							t1 = date.getTime();
							driveCount = 0;
						}
					}
					prevDriveId = driveId;
					driveCount++;

				}
				loopcount++;
			}

			br.close();
			int count = 0;
			for (Coordinate coord : gpsData) {
				OSMQuadTree partition = quadTree.getLeafPartition(coord);
				coordinatePartitions.add(partition);
				Set<OSMQuadTree> neighbours = quadTree.getAllNeighbours(partition, 1100.0);
				for (OSMQuadTree tree : neighbours) {
					allPartitions.add(tree.getBounds());
				}
				count += neighbours.size();

			}

			// for (OSMQuadTree tree : quadTree.getAllLeaves()) {
			// allPartitions.add(tree.getBounds());
			//
			// }

			System.out.println("Average number of neighbours:" + (double) count / gpsData.size());
			System.out.println("Total number of leaf nodes:" + quadTree.getAllLeaves().size());

			@SuppressWarnings("unused")
			OSMQuadTreePartitionsVisualize editor = new OSMQuadTreePartitionsVisualize(
					"Road network and grid", llmodel);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
