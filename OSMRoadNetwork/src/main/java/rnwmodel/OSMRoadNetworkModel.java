package rnwmodel;

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
import java.util.Properties;
import java.util.Set;

import utils.DatabaseAccess;

import com.vividsolutions.jts.io.ParseException;

/**
 * Road network model for the OSM road network. The nodes and links are read
 * from a database.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class OSMRoadNetworkModel {

	private Map<Integer, OSMNode> allNodes = new HashMap<Integer, OSMNode>();
	private Set<OSMRoad> allRoads = new HashSet<OSMRoad>();
	private Set<OSMNode> beginAndEndNodes = new HashSet<OSMNode>();
	private static OSMRoadNetworkModel model;
	private static DatabaseAccess access;

	/**
	 * 
	 * @param dbConnectionProperties
	 */
	private OSMRoadNetworkModel(Properties dbConnectionProperties) {
		access = new DatabaseAccess();
		access.openDBConnection(dbConnectionProperties);

	}

	public static OSMRoadNetworkModel getOSMRoadNetworkInstance(Properties dbConnectionProperties) {

		if (model == null) {
			model = new OSMRoadNetworkModel(dbConnectionProperties);
			try {
				loadNodesAndRoads();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return model;

	}

	private static void loadNodesAndRoads() throws SQLException, FileNotFoundException,
			IOException, ParseException {

		ResultSet rs = access.retrieveQueryResult("SELECT nodeid,x,y FROM openstreetmap.nodes");
		while (rs.next()) {
			OSMNode node = new OSMNode(rs.getInt("nodeid"), rs.getDouble("x"), rs.getDouble("y"));
			model.allNodes.put(node.getNodeId(), node);

		}

		rs.close();

		Properties config = new Properties();
		config.load(new FileInputStream("src/main/resources/config.properties"));

		rs = access
				.retrieveQueryResult("SELECT road_id,nodes,roadname,lanes,oneway,roadtype FROM openstreetmap.roads");
		while (rs.next()) {
			Integer roadId = rs.getInt("road_id");
			String nodeList = rs.getString("nodes");
			String[] split = nodeList.split(",");
			List<OSMNode> roadNodes = new ArrayList<>();

			OSMRoad road = new OSMRoad(roadId);
			road.setLanes(rs.getInt("lanes"));
			road.setRoadType(rs.getString("roadtype"));
			road.setOneWay(rs.getBoolean("oneway"));
			road.setName(rs.getString("roadname"));

			boolean inSingapore = false;

			for (int i = 0; i < split.length; i++) {
				int nodeId = Integer.parseInt(split[i]);
				OSMNode node = model.allNodes.get(nodeId);
				if (i == 0) {
					model.beginAndEndNodes.add(node);
					road.setBeginNode(node);
					node.getOutRoads().add(road);
					if (!road.isOneWay()) {
						node.getInRoads().add(road);
					}
					inSingapore = true;
				} else if (i == split.length - 1) {
					model.beginAndEndNodes.add(node);
					road.setEndNode(node);
					node.getInRoads().add(road);
					if (!road.isOneWay()) {
						node.getOutRoads().add(road);
					}

					inSingapore = true;
				}
				roadNodes.add(model.allNodes.get(nodeId));

			}

			road.setRoadNodes(roadNodes);
			if (inSingapore)
				model.allRoads.add(road);
		}

	}

	/**
	 * @return the allNodes
	 */
	public Map<Integer, OSMNode> getAllNodes() {
		return allNodes;
	}

	/**
	 * @return the allRoads
	 */
	public Set<OSMRoad> getAllRoads() {
		return allRoads;
	}

	/**
	 * @return the beginAndEndNodes
	 */
	public Set<OSMNode> getBeginAndEndNodes() {
		return beginAndEndNodes;
	}

}
