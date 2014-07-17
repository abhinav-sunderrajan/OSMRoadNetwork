package datahandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ParseSingaporeMultiPolygon {
	private static final String FILE_LOCATION = "C:\\Users\\abhinav.sunderrajan\\Desktop\\singa.geojson";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		try {

			File file = new File(FILE_LOCATION);
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(new FileReader(file));
			JSONObject jsonobj = (JSONObject) obj;
			JSONArray array = (JSONArray) jsonobj.get("features");
			Iterator<JSONObject> it = array.iterator();

			while (it.hasNext()) {
				JSONObject singapore = it.next();
				JSONObject geometry = (JSONObject) singapore.get("geometry");
				if (((String) geometry.get("type")).equalsIgnoreCase("MultiPolygon")) {
					JSONArray polygons = (JSONArray) geometry.get("coordinates");
					Iterator<JSONArray> it1 = polygons.iterator();
					StringBuffer buffer = new StringBuffer("POLYGON (");

					while (it1.hasNext()) {
						JSONArray polygon = it1.next();
						Iterator<JSONArray> it2 = polygon.iterator();
						while (it2.hasNext()) {

							JSONArray arr = it2.next();
							if (arr.size() == 3826) {
								for (int i = 0; i < arr.size(); i++) {
									JSONArray coordinate = (JSONArray) arr.get(i);

									if (i == arr.size() - 1) {
										buffer.append(coordinate.get(0) + " " + coordinate.get(1));
									} else {
										buffer.append(coordinate.get(0) + " " + coordinate.get(1)
												+ ", ");
									}

								}
							}

						}

					}

					buffer.append(")");
					System.out.println(buffer.toString());

				}

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}
}
