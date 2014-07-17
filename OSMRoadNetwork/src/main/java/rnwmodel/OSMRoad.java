package rnwmodel;

import java.util.List;

import utils.EarthFunctions;

public class OSMRoad {

	private int roadId;
	private int lanes;
	private boolean oneWay;
	private String name;
	private List<OSMNode> roadNodes;
	private String roadType;
	private OSMNode beginNode;
	private OSMNode endNode;

	/**
	 * @param roadId
	 * @param roadNodes
	 */
	public OSMRoad(int roadId) {
		this.roadId = roadId;
	}

	/**
	 * @return the roadId
	 */
	public int getRoadId() {
		return roadId;
	}

	/**
	 * @param roadId
	 *            the roadId to set
	 */
	public void setRoadId(int roadId) {
		this.roadId = roadId;
	}

	/**
	 * @return the intermediateNodes
	 */
	public List<OSMNode> getRoadNodes() {
		return roadNodes;
	}

	/**
	 * @param intermediateNodes
	 *            the nodes to set
	 */
	public void setRoadNodes(List<OSMNode> intermediateNodes) {
		this.roadNodes = intermediateNodes;
	}

	/**
	 * @return the lanes
	 */
	public int getLanes() {
		return lanes;
	}

	/**
	 * @param lanes
	 *            the lanes to set
	 */
	public void setLanes(int lanes) {
		this.lanes = lanes;
	}

	/**
	 * @return the oneWay
	 */
	public boolean isOneWay() {
		return oneWay;
	}

	/**
	 * @param oneWay
	 *            the oneWay to set
	 */
	public void setOneWay(boolean oneWay) {
		this.oneWay = oneWay;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the roadType
	 */
	public String getRoadType() {
		return roadType;
	}

	/**
	 * @param roadType
	 *            the roadType to set
	 */
	public void setRoadType(String roadType) {
		this.roadType = roadType;
	}

	/**
	 * @return the beginNode
	 */
	public OSMNode getBeginNode() {
		return beginNode;
	}

	/**
	 * @param beginNode
	 *            the beginNode to set
	 */
	public void setBeginNode(OSMNode beginNode) {
		this.beginNode = beginNode;
	}

	/**
	 * @return the endNode
	 */
	public OSMNode getEndNode() {
		return endNode;
	}

	/**
	 * @param endNode
	 *            the endNode to set
	 */
	public void setEndNode(OSMNode endNode) {
		this.endNode = endNode;
	}

	/**
	 * Returns the length of the road.
	 * 
	 * @return
	 */
	public double getWeight() {

		double roadLength = 0.0;
		for (int i = 1; i < roadNodes.size(); i++) {
			roadLength += EarthFunctions.haversianDistance(roadNodes.get(i).getY(),
					roadNodes.get(i - 1).getY(), roadNodes.get(i).getX(), roadNodes.get(i - 1)
							.getX());
		}
		return roadLength;
	}

	@Override
	public int hashCode() {
		return roadId;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OSMRoad))
			return false;
		if (obj == this)
			return true;

		// Return true if the id of the begin and end nodes are the same.
		return (this.beginNode.getNodeId().equals(((OSMRoad) obj).beginNode.getNodeId()) && this.endNode
				.getNodeId().equals(((OSMRoad) obj).endNode.getNodeId()));

	}

	@Override
	public String toString() {
		return this.beginNode.getNodeId() + " to " + this.endNode.getNodeId();
	}

}
