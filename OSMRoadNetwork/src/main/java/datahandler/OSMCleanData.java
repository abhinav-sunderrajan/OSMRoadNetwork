package datahandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import rnwmodel.OSMNode;
import rnwmodel.OSMRoad;
import utils.DatabaseAccess;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * This is a one time use class like {@link OSMRoadNetworkToDB} which makes and
 * breaks roads for better representation. Also the roads that do not belong to
 * the Singapore mainland are filtered out in the database. This reduces the
 * time taken for initializing the road-network.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class OSMCleanData {

	private Map<Integer, OSMNode> allNodes = new HashMap<Integer, OSMNode>();
	private Set<OSMRoad> singaporeRoads = new HashSet<OSMRoad>();
	private Set<OSMRoad> deleteRoads = new HashSet<OSMRoad>();
	private Set<OSMNode> beginAndEndNodes = new HashSet<OSMNode>();
	private static DatabaseAccess access;

	/**
	 * 
	 * @param dbConnectionProperties
	 */
	private OSMCleanData(Properties dbConnectionProperties) {
		access = new DatabaseAccess();
		access.openDBConnection(dbConnectionProperties);

	}

	private void loadNodesAndRoads() throws SQLException, FileNotFoundException, IOException,
			ParseException {

		ResultSet rs = access.retrieveQueryResult("SELECT nodeid,x,y FROM openstreetmap.nodes");
		while (rs.next()) {
			OSMNode node = new OSMNode(rs.getInt("nodeid"), rs.getDouble("x"), rs.getDouble("y"));
			allNodes.put(node.getNodeId(), node);

		}

		rs.close();

		// Define the polygon representing the main land Singapore for filtering
		// superfluous roads. Most time consuming part but cannot avoid as of
		// now.
		Properties config = new Properties();
		config.load(new FileInputStream("src/main/resources/config.properties"));
		GeometryFactory factory = new GeometryFactory();
		WKTReader reader = new WKTReader(factory);
		Geometry polygon = reader.read(config.getProperty("bounding.box.singapore"));

		rs = access
				.retrieveQueryResult("SELECT road_id,nodes,roadname,lanes,oneway,roadtype FROM openstreetmap.roads");
		while (rs.next()) {
			Integer roadId = rs.getInt("road_id");
			String nodeList = rs.getString("nodes");
			String[] split = nodeList.split(",");
			List<OSMNode> intermediateNodes = new ArrayList<>();

			OSMRoad road = new OSMRoad(roadId);
			road.setLanes(rs.getInt("lanes"));
			road.setRoadType(rs.getString("roadtype"));
			road.setOneWay(rs.getBoolean("oneway"));
			road.setName(rs.getString("roadname"));

			boolean inSingapore = false;

			for (int i = 0; i < split.length; i++) {
				int nodeId = Integer.parseInt(split[i]);
				OSMNode node = allNodes.get(nodeId);
				double lon = node.getX();
				double lat = node.getY();
				Point point = (Point) reader.read("POINT (" + lon + " " + lat + ")");
				if (i == 0) {
					if (point.within(polygon)) {
						beginAndEndNodes.add(node);
						road.setBeginNode(node);
						node.getOutRoads().add(road);
						if (!road.isOneWay()) {
							node.getInRoads().add(road);
						}
						inSingapore = true;
					}
				} else if ((i == split.length - 1) && (inSingapore || point.within(polygon))) {
					beginAndEndNodes.add(node);
					road.setEndNode(node);
					node.getInRoads().add(road);
					if (!road.isOneWay()) {
						node.getOutRoads().add(road);
					}

					inSingapore = true;
				} else {
					intermediateNodes.add(allNodes.get(nodeId));
				}

			}

			road.setRoadNodes(intermediateNodes);
			if (inSingapore)
				singaporeRoads.add(road);
			else
				deleteRoads.add(road);
		}
	}

	/**
	 * Fix the code here. This needs to be done recursively.
	 * 
	 * @return
	 */
	public Map<OSMRoad, OSMNode> splitRoadsAtIntersections() {
		Map<OSMRoad, OSMNode> roadsForSplitting = new HashMap<OSMRoad, OSMNode>();
		for (OSMRoad road : singaporeRoads) {
			for (OSMNode node : road.getRoadNodes()) {
				if (beginAndEndNodes.contains(node)) {
					roadsForSplitting.put(road, node);

				}

			}

		}
		return roadsForSplitting;

	}

	public static void main(String[] args) {
		try {
			Properties dbConnectionProperties = new Properties();
			dbConnectionProperties.load(new FileInputStream(
					"src/main/resources/connection.properties"));
			OSMCleanData clean = new OSMCleanData(dbConnectionProperties);
			clean.loadNodesAndRoads();
			System.out.println("Total number of roads in Singapore main land:"
					+ clean.singaporeRoads.size());

			System.out.println("Number of roads to be deleted:" + clean.deleteRoads.size());

			Map<OSMRoad, OSMNode> roadsToBeSplit = clean.splitRoadsAtIntersections();
			System.out.println("Number of roads to be split are:" + roadsToBeSplit.size());

			System.out.println("Deleting roads that do not belong to mainland...");

			for (OSMRoad road : clean.deleteRoads) {
				access.executeUpdate("DELETE FROM openstreetmap.roads WHERE road_id="
						+ road.getRoadId());
			}

			ResultSet rs = access
					.retrieveQueryResult("select max(road_id) AS maxRoadId from openstreetmap.roads");

			int maxRoadId = 0;
			while (rs.next()) {
				maxRoadId = rs.getInt("maxRoadId");
			}

			System.out.println("Splitting roads where the need arises...");

			for (Entry<OSMRoad, OSMNode> entry : roadsToBeSplit.entrySet()) {
				OSMNode splitNode = entry.getValue();
				OSMRoad splitRoad = entry.getKey();

				// This needs to be investigated as well.
				if (splitRoad == null || splitRoad.getBeginNode() == null) {
					System.out.println("should not happen.." + splitRoad.getRoadId());
					continue;

				}
				StringBuffer part1 = new StringBuffer(splitRoad.getBeginNode().getNodeId() + ",");
				StringBuffer part2 = null;
				for (OSMNode node : splitRoad.getRoadNodes()) {
					if (part2 == null) {
						if (node.getNodeId() == splitNode.getNodeId()) {
							part1.append(splitNode.getNodeId() + "");
							part2 = new StringBuffer(splitNode.getNodeId() + ",");
						} else {
							part1.append(node.getNodeId() + ",");
						}
					} else {
						part2.append(node.getNodeId() + ",");
					}

				}

				part2.append(splitRoad.getEndNode().getNodeId());

				access.executeUpdate("UPDATE openstreetmap.roads SET nodes='" + part1.toString()
						+ "' WHERE road_id = " + splitRoad.getRoadId());

				access.executeUpdate("INSERT INTO openstreetmap.roads (road_id,nodes,roadname,lanes,oneway,roadtype) VALUES("
						+ ++maxRoadId
						+ ",'"
						+ part2.toString()
						+ "','"
						+ splitRoad.getName()
						+ "',"
						+ splitRoad.getLanes()
						+ ","
						+ splitRoad.isOneWay()
						+ ",'"
						+ splitRoad.getRoadType() + "'" + ")");

			}

			access.closeConnection();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
