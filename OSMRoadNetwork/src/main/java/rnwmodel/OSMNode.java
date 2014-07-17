package rnwmodel;

import java.util.HashSet;
import java.util.Set;

public class OSMNode {

	private Integer nodeId;
	private double x;
	private double y;
	private Set<OSMRoad> outRoads = new HashSet<OSMRoad>();
	private Set<OSMRoad> inRoads = new HashSet<OSMRoad>();

	// For routing purposes alone
	public double minDistance = Double.POSITIVE_INFINITY;
	public OSMNode previous;

	/**
	 * @param nodeId
	 * @param point
	 */
	public OSMNode(int nodeId, double x, double y) {
		this.nodeId = nodeId;
		this.x = x;
		this.y = y;
	}

	/**
	 * @return the nodeId
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * @param nodeId
	 *            the nodeId to set
	 */
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return the x
	 */
	public double getX() {
		return x;
	}

	/**
	 * @param x
	 *            the x to set
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * @return the y
	 */
	public double getY() {
		return y;
	}

	/**
	 * @param y
	 *            the y to set
	 */
	public void setY(double y) {
		this.y = y;
	}

	/**
	 * @return the outRoads
	 */
	public Set<OSMRoad> getOutRoads() {
		return outRoads;
	}

	/**
	 * @param outRoads
	 *            the outRoads to set
	 */
	public void setOutRoads(Set<OSMRoad> outRoads) {
		this.outRoads = outRoads;
	}

	/**
	 * @return the inRoads
	 */
	public Set<OSMRoad> getInRoads() {
		return inRoads;
	}

	/**
	 * @param inRoads
	 *            the inRoads to set
	 */
	public void setInRoads(Set<OSMRoad> inRoads) {
		this.inRoads = inRoads;
	}

	@Override
	public int hashCode() {
		return nodeId;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OSMNode))
			return false;
		if (obj == this)
			return true;
		return this.nodeId.equals(((OSMNode) obj).nodeId);

	}

	@Override
	public String toString() {
		return nodeId + "";
	}

}
