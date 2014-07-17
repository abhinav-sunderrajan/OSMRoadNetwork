package routing;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import rnwmodel.OSMNode;
import rnwmodel.OSMRoad;
import rnwmodel.OSMRoadNetworkModel;

/**
 * Routing using arc flags the OSM road network.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class RoutingOSMDjikstra {

	private Set<OSMNode> seen = new HashSet<OSMNode>();
	private Set<OSMNode> unseen = new HashSet<OSMNode>();
	private Set<OSMNode> all = new HashSet<OSMNode>();
	private Set<OSMNode> changed = new HashSet<OSMNode>();

	/**
	 * Initialize with the road network model for normal routing using Dijkstra.
	 * 
	 * @param rnwModel
	 */
	public RoutingOSMDjikstra(OSMRoadNetworkModel rnwModel) {
		all.addAll(rnwModel.getBeginAndEndNodes());

	}

	/**
	 * @return the unseen
	 */
	public Set<OSMNode> getUnseen() {
		return unseen;
	}

	/**
	 * Call to reset after every invocation of Dijkstra.
	 */
	private void reset() {

		if (changed.size() > 0) {
			for (OSMNode node : changed) {
				node.minDistance = Double.POSITIVE_INFINITY;
				node.previous = null;

			}

		}
		unseen.clear();
		seen.clear();
		changed.clear();

	}

	/**
	 * Return the shortest path from beginNode to the endNode.
	 * 
	 * @param srcNode
	 * @param destNode
	 */
	public List<OSMNode> djikstra(OSMNode srcNode, OSMNode destNode) {

		unseen.addAll(all);
		changed.add(srcNode);

		for (OSMNode node : unseen) {
			if (node.getNodeId() == srcNode.getNodeId()) {
				node.minDistance = 0;
				break;
			}
		}
		int graphSize = unseen.size();
		List<OSMNode> nodePath = new ArrayList<OSMNode>();
		while (seen.size() != graphSize) {
			OSMNode node = null;
			double minDistance = Double.POSITIVE_INFINITY;

			// Choose the vertex with the minimum distance
			for (OSMNode v : unseen) {
				if (v.minDistance < minDistance) {
					node = v;
					minDistance = v.minDistance;
				}
			}
			if (node == null) {
				return null;
			}

			if (node.getOutRoads() != null) {

				for (OSMRoad road : node.getOutRoads()) {

					OSMNode endNode = null;
					if (road.isOneWay()) {
						endNode = road.getEndNode();
					} else {
						if (road.getEndNode().getNodeId() == node.getNodeId()) {
							endNode = road.getBeginNode();
						} else {
							endNode = road.getEndNode();
						}
					}

					if (seen.contains(endNode)) {
						continue;
					}

					if (endNode.minDistance == Double.POSITIVE_INFINITY) {
						endNode.minDistance = road.getWeight() + node.minDistance;
						endNode.previous = node;
						changed.add(endNode);

					} else {
						double temp = node.minDistance + road.getWeight();
						if (temp < endNode.minDistance) {
							endNode.minDistance = temp;
							endNode.previous = node;
						}
					}
				}
			}

			seen.add(node);
			unseen.remove(node);
			if (node.getNodeId() == destNode.getNodeId()) {
				break;
			}
		}

		for (OSMNode vertex : seen) {
			if (vertex.getNodeId() == destNode.getNodeId()) {
				OSMNode vp = vertex;

				while (true) {
					if (vp.getNodeId() == srcNode.getNodeId()) {
						System.out.println(vp.getNodeId());
						nodePath.add(vp);
						break;
					} else {
						System.out.print(vp.getNodeId() + "<--");
						nodePath.add(vp);
						vp = vp.previous;
						if (vp == null) {
							return null;
						}
					}
				}
				break;
			}
		}
		reset();
		return nodePath;

	}

	public static void main(String[] args) {
		try {
			Class.forName("org.postgresql.Driver");

			Properties dbConnectionProperties = new Properties();
			dbConnectionProperties.load(new FileInputStream(
					"src/main/resources/connection.properties"));

			OSMRoadNetworkModel rnwModel = OSMRoadNetworkModel
					.getOSMRoadNetworkInstance(dbConnectionProperties);

			RoutingOSMDjikstra dji = new RoutingOSMDjikstra(rnwModel);
			List<OSMNode> nodes = dji.djikstra(rnwModel.getAllNodes().get(17013), rnwModel
					.getAllNodes().get(9674));

			System.out.println(nodes);
			nodes = dji.djikstra(rnwModel.getAllNodes().get(39476),
					rnwModel.getAllNodes().get(22127));
			System.out.println(nodes);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
