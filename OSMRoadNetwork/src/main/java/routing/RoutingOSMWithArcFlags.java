package routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.redisson.Redisson;

import rnwmodel.OSMNode;
import rnwmodel.OSMQuadTree;
import rnwmodel.OSMRoad;
import rnwmodel.OSMRoadNetworkModel;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Routing using arc flags the OSM road network.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class RoutingOSMWithArcFlags {

	private Set<OSMNode> seen = new HashSet<OSMNode>();
	private Set<OSMNode> unseen = new HashSet<OSMNode>();
	private Coordinate endBoundingBox;
	private OSMQuadTree leafEnd;
	private OSMQuadTree quadTree;
	private static Map<Long, HashSet<Coordinate>> arcFlags;
	private Set<OSMNode> all = new HashSet<OSMNode>();
	private Set<OSMNode> changed = new HashSet<OSMNode>();

	/**
	 * 
	 * @param quadTree
	 * @param redisson
	 * @param rnwModel
	 */
	public RoutingOSMWithArcFlags(OSMQuadTree quadTree, Redisson redisson,
			OSMRoadNetworkModel rnwModel) {
		this.quadTree = quadTree;
		arcFlags = redisson.getMap("ARC-FLAGS");
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
	 * @param beginNode
	 * @param endNode
	 */
	public List<OSMNode> djikstra(OSMNode beginNode, OSMNode endNode) {

		unseen.addAll(all);
		changed.add(beginNode);
		// Determine the partition the end node falls into
		leafEnd = quadTree.getLeafPartition(new Coordinate(endNode.getX(), endNode.getY()));
		endBoundingBox = new Coordinate(leafEnd.getBounds().getMinX(), leafEnd.getBounds()
				.getMaxY());

		for (OSMNode node : unseen) {
			if (node.getNodeId() == beginNode.getNodeId()) {
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
					HashSet<Coordinate> boundingBoxes = arcFlags.get(road.getRoadId());
					if (boundingBoxes == null) {
						continue;
					}

					if (seen.contains(road.getEndNode()) || !boundingBoxes.contains(endBoundingBox)) {
						continue;
					}

					if (road.getEndNode().minDistance == Double.POSITIVE_INFINITY) {
						road.getEndNode().minDistance = road.getWeight() + node.minDistance;
						road.getEndNode().previous = node;
						changed.add(road.getEndNode());

					} else {
						double temp = node.minDistance + road.getWeight();
						if (temp < road.getEndNode().minDistance) {
							road.getEndNode().minDistance = temp;
							road.getEndNode().previous = node;
						}
					}
				}
			}

			seen.add(node);
			unseen.remove(node);
			if (node.getNodeId() == endNode.getNodeId()) {
				break;
			}
		}

		for (OSMNode vertex : seen) {
			if (vertex.getNodeId() == endNode.getNodeId()) {
				OSMNode vp = vertex;

				while (true) {
					if (vp.getNodeId() == beginNode.getNodeId()) {
						// System.out.println(vp.name);
						nodePath.add(vp);
						break;
					} else {
						// System.out.print(vp.name + "<--");
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

}
