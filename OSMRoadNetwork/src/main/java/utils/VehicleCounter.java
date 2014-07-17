package utils;

/**
 * A simple vehicle counter which maintains the count of vehicles along an out
 * road of a node.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class VehicleCounter {

	private String splitId;
	private int transitionCount = 0;
	private int destinationCount = 0;
	private long beginTime;
	private long endTime;

	/**
	 * @param splitId
	 *            An splitId Id is nodeId_outRoadId
	 * @param beginTime
	 * @param endTime
	 */
	public VehicleCounter(String splitId, long beginTime, long endTime) {
		this.splitId = splitId;
		this.beginTime = beginTime;
		this.endTime = endTime;
	}

	/**
	 * @return the splitId
	 */
	public String getSplitId() {
		return splitId;
	}

	/**
	 * @return the transitionCount
	 */
	public int getTransitionCount() {
		return transitionCount;
	}

	/**
	 * @return the destinationCount
	 */
	public int getDestinationCount() {
		return destinationCount;
	}

	/**
	 * @return the beginTime
	 */
	public long getBeginTime() {
		return beginTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * Increment the transition counter.
	 */
	public void incrementTransitionCount() {
		++transitionCount;
	}

	/**
	 * Increment destination counter.
	 */
	public void incrementDestinationCounter() {
		++destinationCount;
	}

}
