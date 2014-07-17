package routing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.redisson.Redisson;

import rnwmodel.OSMNode;
import rnwmodel.OSMQuadTree;
import rnwmodel.OSMRoad;
import rnwmodel.OSMRoadNetworkModel;
import utils.EarthFunctions;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SingleThreadedClaimStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Generates the arc flags for the entire road network.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class ArcFlagsForOSMRNW {

	private static final double bbTopLeftLon = 103.618;
	private static final double bbTopLeftLat = 1.4697;
	private static final double bbBotRightLat = 1.23843;
	private static final double bbTopRightLon = 104.0263;
	private static final String REDIS_SERVER = "172.25.187.111";
	private static OSMRoadNetworkModel model;
	private static Set<OSMQuadTree> all = new HashSet<OSMQuadTree>();
	private static AtomicInteger finishedCount = new AtomicInteger(0);
	private OSMQuadTree quadTree;
	private ThreadPoolExecutor executor;
	private Redisson redisson;
	private Map<Integer, HashSet<Coordinate>> arcFlags;
	private Set<Integer> alreadyEvaluatedLink;
	private Disruptor<ArcFlagBean> disruptor;
	private static RingBuffer<ArcFlagBean> ringBuffer;
	private final static int RING_SIZE = 32768;
	private static EventHandler<ArcFlagBean> eventHandler;
	private static Queue<String> logs = new ConcurrentLinkedQueue<>();
	private static BufferedWriter bw;

	private static class ArcFlagBean {
		int linkId;
		Coordinate boundingBox;
		final static EventFactory<ArcFlagBean> EVENT_FACTORY = new EventFactory<ArcFlagBean>() {
			public ArcFlagBean newInstance() {
				return new ArcFlagBean();
			}
		};
	}

	/**
	 * Initialize the required variables.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ArcFlagsForOSMRNW() throws Exception {
		Properties connectionProperties = new Properties();
		connectionProperties.load(new FileInputStream(
				"/home/abhinav/MapMatchingStuff/arcflags/connection.properties"));
		model = OSMRoadNetworkModel.getOSMRoadNetworkInstance(connectionProperties);
		quadTree = new OSMQuadTree(0, new Envelope(bbTopLeftLon, bbTopRightLon, bbTopLeftLat,
				bbBotRightLat));
		bw = new BufferedWriter(new FileWriter(new File(
				"/home/abhinav/MapMatchingStuff/arcflags/logs_part1.txt")));

		for (OSMNode node : model.getBeginAndEndNodes()) {
			quadTree.insert(node);
		}

		org.redisson.Config redissonConfig = new org.redisson.Config();
		redissonConfig.addAddress(REDIS_SERVER + ":6379");
		redisson = Redisson.create(redissonConfig);

		arcFlags = redisson.getMap("ARC-FLAGS");
		alreadyEvaluatedLink = redisson.getSet("alreadyEvaluatedLink");

		// Initialize thread pool executor
		ThreadFactory threadFactory = Executors.defaultThreadFactory();
		RejectedExecutionHandler threadFactoryHandler = new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				logs.add("Task Rejected : " + (r));
			}
		};
		executor = new ThreadPoolExecutor(150, 500, 100000, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(3000), threadFactory, threadFactoryHandler);

		disruptor = new Disruptor<ArcFlagBean>(ArcFlagBean.EVENT_FACTORY, executor,
				new SingleThreadedClaimStrategy(RING_SIZE), new SleepingWaitStrategy());

		eventHandler = new EventHandler<ArcFlagBean>() {
			long count = 0;

			@Override
			public void onEvent(ArcFlagBean bean, long sequence, boolean endOfBatch)
					throws Exception {

				if (arcFlags.get(bean.linkId) == null) {
					HashSet<Coordinate> flag = new HashSet<Coordinate>();
					flag.add(bean.boundingBox);
					arcFlags.put(bean.linkId, flag);
				} else {

					HashSet<Coordinate> flag = arcFlags.get(bean.linkId);
					if (!flag.contains(bean.boundingBox)) {
						flag.add(bean.boundingBox);
						arcFlags.put(bean.linkId, flag);
						count++;

					}

				}

				if (count % 10000 == 0 && count > 0) {
					logs.add("Inserted " + count + " new records\n");
				}

			}
		};

		disruptor.handleEventsWith(eventHandler);
		ringBuffer = disruptor.start();

	}

	public static void main(String[] args) {
		try {

			final ArcFlagsForOSMRNW arc = new ArcFlagsForOSMRNW();

			arc.executor.submit(new Runnable() {

				@Override
				public void run() {
					try {
						while (true) {
							while (logs.isEmpty()) {
								Thread.sleep(100);
							}
							String str = logs.poll();
							bw.write(str);
							bw.flush();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			});

			Collection<Future<?>> futures = new LinkedList<Future<?>>();

			int count = 1;
			for (OSMQuadTree leaf : arc.quadTree.getAllLeaves()) {
				if (leaf.getNodeId() < 1450) {
					futures.add(arc.executor.submit(arc.new ComputeFlagsForLeaf(leaf, count)));
					all.add(leaf);
					count++;
				}

			}

			logs.add("total work:" + all.size() + "\n");

			arc.executor.shutdown();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static ArrayList<OSMRoad> getRoute(List<OSMNode> intermediateNodes) {
		ArrayList<OSMRoad> path = new ArrayList<OSMRoad>();
		Object[] nodesArr = intermediateNodes.toArray();
		for (int count = nodesArr.length - 1; count > 0; count--) {
			OSMRoad link = model.getConnectingRoad(intermediateNodes.get(count),
					intermediateNodes.get(count - 1));
			path.add(link);

		}
		return path;

	}

	private class ComputeFlagsForLeaf implements Runnable {
		private OSMQuadTree leaf;
		private int id;

		ComputeFlagsForLeaf(OSMQuadTree leaf, int id) {
			this.leaf = leaf;
			this.id = id;

		}

		private void addArcFlag(OSMRoad routeLink, OSMQuadTree neighbor) {
			Coordinate boundingBoxTopLeft = new Coordinate(neighbor.getBounds().getMinX(), neighbor
					.getBounds().getMaxY());
			long sequence = ringBuffer.next();
			ArcFlagBean bean = ringBuffer.get(sequence);
			bean.boundingBox = boundingBoxTopLeft;
			bean.linkId = routeLink.getRoadId();
			ringBuffer.publish(sequence);

		}

		@Override
		public void run() {
			logs.add(id + ">> " + "Starting partition" + leaf.getBounds() + "\n");

			// Retreive a list of all neighbours in the range of 1.1 km
			Set<OSMQuadTree> neighbours = quadTree.getAllNeighbours(leaf, 1100.0);
			System.out.println(id + ">> " + "Number of neighbours:" + neighbours.size());
			RoutingOSMDjikstra dji = new RoutingOSMDjikstra(model);

			int linkcount = 0;
			for (OSMRoad link : leaf.getAssociatedLinks()) {

				if (alreadyEvaluatedLink.contains(link.getRoadId())) {
					linkcount++;
					double percentage = linkcount * 100.0 / leaf.getAssociatedLinks().size();
					logs.add("Finished " + percentage + " of links\n");
					continue;
				}

				boolean allEvaluated = true;
				for (OSMRoad tnodeLink : link.getEndNode().getOutRoads()) {
					if (!alreadyEvaluatedLink.contains(tnodeLink.getRoadId())) {
						allEvaluated = false;
						break;
					}

				}

				if (allEvaluated) {
					alreadyEvaluatedLink.add(link.getRoadId());
					linkcount++;
					double percentage = linkcount * 100.0 / leaf.getAssociatedLinks().size();
					logs.add(id + ">> " + "Finished " + percentage + " of links\n");
				}

				for (OSMQuadTree neighbor : neighbours) {

					if (arcFlags.containsKey(link.getRoadId())) {
						if (arcFlags.get(link.getRoadId()).contains(
								new Coordinate(neighbor.getBounds().getMinX(), neighbor.getBounds()
										.getMaxY()))) {
							continue;
						}

					}

					// All links with in a partition also belong to that
					// partition.
					for (OSMRoad mlink : neighbor.getAssociatedLinks()) {
						addArcFlag(mlink, neighbor);
					}

					for (OSMNode node : neighbor.getAllBorderNodes()) {
						if (EarthFunctions.haversianDistance(
								new Coordinate(node.getX(), node.getY()), new Coordinate(link
										.getEndNode().getX(), link.getEndNode().getY())) > 1500) {
							continue;
						}

						List<OSMNode> intermediateNodes = dji.djikstra(link.getEndNode(), node);
						if (intermediateNodes != null) {
							ArrayList<OSMRoad> route = getRoute(intermediateNodes);
							for (OSMRoad routeLink : route) {
								addArcFlag(routeLink, neighbor);

							}

						}

					}

				}
				linkcount++;
				double percentage = linkcount * 100.0 / leaf.getAssociatedLinks().size();
				logs.add(id + ">> " + "Finished " + percentage + " of links\n");
				alreadyEvaluatedLink.add(link.getRoadId());

			}

			logs.add(new Timestamp(System.currentTimeMillis()) + " Finished envelope:"
					+ leaf.getBounds() + " " + finishedCount.incrementAndGet() * 100.0 / all.size()
					+ "% of all partitions\n");

		}
	}

}
