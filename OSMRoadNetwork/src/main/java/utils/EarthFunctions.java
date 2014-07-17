package utils;

import java.util.Map;

import rnwmodel.OSMRoad;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

public class EarthFunctions {
	private static final double EARTH_RADIUS = 6378137; // in meters

	/**
	 * Returns the distance between two coordinates.
	 * 
	 * @param lat1
	 * @param lat2
	 * @param lon1
	 * @param lon2
	 * @return distance between the coordinates in meters.
	 */
	public static double haversianDistance(double lat1, double lat2, double lon1, double lon2) {
		double dLat = Math.toRadians(lat1 - lat2);
		double dLng = Math.toRadians(lon1 - lon2);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat2))
				* Math.cos(Math.toRadians(lat1)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = EARTH_RADIUS * c;

		return dist;
	}

	/**
	 * Returns the distance between two coordinates.
	 * 
	 * @param coord1
	 * @param coord2
	 * @return distance between the coordinates in meters.
	 */
	public static double haversianDistance(Coordinate coord1, Coordinate coord2) {
		double dLat = Math.toRadians(coord1.y - coord2.y);
		double dLng = Math.toRadians(coord1.x - coord2.x);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(coord2.y))
				* Math.cos(Math.toRadians(coord1.y)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = EARTH_RADIUS * c;

		return dist;
	}

	/**
	 * Returns the perpendicular distance between the link and the the point
	 * represented by (lonPoint, latPoint)
	 * 
	 * @param link
	 * @param lonPoint
	 * @param latPoint
	 * @param coordinateOnLink1
	 * @return
	 */
	public static double calculateHaversianDistanceToLink(OSMRoad link, double lonPoint,
			double latPoint, Map<Integer, Coordinate> coordinateOnLink1) {
		LineSegment ls = new LineSegment(link.getBeginNode().getX(), link.getBeginNode().getY(),
				link.getEndNode().getX(), link.getEndNode().getY());
		Coordinate point = new Coordinate(lonPoint, latPoint);
		double t = ls.projectionFactor(point);

		// t indicates whether the projection of the point to the link lies
		// outside or inside.

		if (t < 0) {
			coordinateOnLink1.put(link.getRoadId(), new Coordinate(link.getBeginNode().getX(), link
					.getBeginNode().getY()));
			return haversianDistance(link.getBeginNode().getY(), latPoint, link.getBeginNode()
					.getX(), lonPoint);
		} else if (t > 1) {
			coordinateOnLink1.put(link.getRoadId(), new Coordinate(link.getEndNode().getX(), link
					.getEndNode().getY()));
			return haversianDistance(link.getEndNode().getY(), latPoint, link.getEndNode().getX(),
					lonPoint);
		} else {
			Coordinate coord = ls.project(point);
			coordinateOnLink1.put(link.getRoadId(), coord);
			double d = haversianDistance(latPoint, coord.y, lonPoint, coord.x);
			return d;

		}

	}

	/**
	 * Returns a {@link Coordinate} at a given distance and bearing from the one
	 * passed as a parameter.
	 * 
	 * @param init
	 *            The initial coordinate.
	 * @param distance
	 *            Distance in meters.
	 * @param bearing
	 *            the bearing angle in radians.
	 * @return
	 */
	public static Coordinate getPointAtDistanceAndBearing(Coordinate init, double distance,
			double bearing) {

		double lat1 = Math.toRadians(init.y);
		double lon1 = Math.toRadians(init.x);

		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / EARTH_RADIUS) + Math.cos(lat1)
				* Math.sin(distance / EARTH_RADIUS) * Math.cos(bearing));
		double lon2 = lon1
				+ Math.atan2(
						Math.sin(bearing) * Math.sin(distance / EARTH_RADIUS) * Math.cos(lat1),
						Math.cos(distance / EARTH_RADIUS) - Math.sin(lat1) * Math.sin(lat2));
		return new Coordinate(Math.toDegrees(lon2), Math.toDegrees(lat2));

	}
}
