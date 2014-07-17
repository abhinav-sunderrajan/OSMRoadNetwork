package datahandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import rnwmodel.OSMNode;
import rnwmodel.OSMRoad;
import utils.DatabaseAccess;

import com.vividsolutions.jts.geom.Coordinate;

public class OSMRoadNetworkToDB {
	private static final String FILE_LOCATION = "C:\\Users\\abhinav.sunderrajan\\Desktop\\Future\\OSM\\singapore_osm.json";
	private static Set<String> roadTypes = new HashSet<>();
	private static Set<OSMRoad> roads = new HashSet<>();
	private static Map<Coordinate, OSMNode> nodesMap = new HashMap<Coordinate, OSMNode>();
	private static int nodeId = 0;
	private static int roadId = 0;

	public static void main(String[] args) {

		try {

			String[] arr = { "secondary", "motorway_junction", "secondary_link", "tertiary_link",
					"mini_roundabout", "tertiary", "raceway", "primary", "traffic_signals",
					"turning_circle", "motorway_link", "motorway", "trunk_link", "primary_link",
					"trunk", "living_street", "residential" };
			for (int i = 0; i < arr.length; i++) {
				roadTypes.add(arr[i]);
			}

			File file = new File(FILE_LOCATION);
			JSONParser parser = new JSONParser();
			Object obj;
			obj = parser.parse(new FileReader(file));
			JSONObject jsonobj = (JSONObject) obj;
			JSONArray array = (JSONArray) jsonobj.get("features");
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> it = array.iterator();
			while (it.hasNext()) {
				JSONObject feature = it.next();
				JSONObject properties = (JSONObject) feature.get("properties");
				if (properties.containsKey("highway")) {

					String roadType = (String) properties.get("highway");
					if (!roadTypes.contains(roadType)) {
						continue;
					}

					JSONObject geometry = (JSONObject) feature.get("geometry");
					if (((String) geometry.get("type")).equalsIgnoreCase("LineString")) {
						JSONArray coordinates = (JSONArray) geometry.get("coordinates");
						List<OSMNode> roadNodes = new ArrayList<OSMNode>();
						for (int i = 0; i < coordinates.size(); i++) {
							JSONArray coordinate = (JSONArray) coordinates.get(i);
							double lon = (Double) coordinate.get(0);
							double lat = (Double) coordinate.get(1);

							Coordinate coord = new Coordinate(lon, lat);

							if (nodesMap.containsKey(coord)) {
								roadNodes.add(nodesMap.get(coord));
							} else {
								OSMNode node = new OSMNode(++nodeId, lon, lat);
								nodesMap.put(coord, node);
								roadNodes.add(node);
							}

						}

						OSMRoad road = new OSMRoad(++roadId);
						road.setRoadNodes(roadNodes);

						road.setRoadType(roadType);

						if (properties.containsKey("oneway")) {
							String oneWay = (String) properties.get("oneway");

							if (oneWay.equalsIgnoreCase("yes")) {
								road.setOneWay(true);
							} else {
								road.setOneWay(false);
							}
						}
						if (properties.containsKey("name")) {
							String name = (String) properties.get("name");
							name = name.replace("'", "");
							road.setName(name);

						} else {
							road.setName("");
						}

						if (properties.containsKey("lanes")) {
							road.setLanes(Integer.parseInt((String) properties.get("lanes")));
						} else {
							road.setLanes(-1);
						}

						roads.add(road);

					}

				}

			}

			System.out.println("Number of nodes:" + nodesMap.size());
			System.out.println("Number of roads:" + roads.size());

			Properties connectionProperties = new Properties();
			connectionProperties.load(new FileInputStream(
					"src/main/resources/connection.properties"));
			DatabaseAccess access = new DatabaseAccess();
			access.openDBConnection(connectionProperties);
			for (Entry<Coordinate, OSMNode> entry : nodesMap.entrySet()) {
				OSMNode node = entry.getValue();
				access.executeUpdate("INSERT INTO openstreetmap.nodes VALUES (" + node.getNodeId()
						+ "," + node.getX() + "," + node.getY() + ")");

			}

			for (OSMRoad road : roads) {
				StringBuffer buffer = new StringBuffer("");
				List<OSMNode> nodeIds = road.getRoadNodes();
				for (int i = 0; i < nodeIds.size(); i++) {
					if (i == nodeIds.size() - 1) {
						buffer.append(nodeIds.get(i).getNodeId());
					} else {
						buffer.append(nodeIds.get(i).getNodeId() + ",");
					}
				}

				access.executeUpdate("INSERT INTO openstreetmap.roads (road_id,nodes,roadname,lanes,oneway,roadtype) VALUES("
						+ road.getRoadId()
						+ ",'"
						+ buffer.toString()
						+ "','"
						+ road.getName()
						+ "',"
						+ road.getLanes()
						+ ","
						+ road.isOneWay()
						+ ",'"
						+ road.getRoadType() + "'" + ")");
			}

			access.closeConnection();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
