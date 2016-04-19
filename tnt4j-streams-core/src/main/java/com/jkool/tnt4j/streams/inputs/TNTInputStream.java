/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.inputs;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.jkool.tnt4j.streams.configure.StreamProperties;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.StreamsThread;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.config.DefaultConfigFactory;
import com.nastel.jkool.tnt4j.config.TrackerConfig;
import com.nastel.jkool.tnt4j.config.TrackerConfigStore;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.source.DefaultSourceFactory;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.source.SourceType;
import com.nastel.jkool.tnt4j.tracker.Tracker;

/**
 * <p>
 * Base class that all activity streams must extend. It provides some base
 * functionality useful for all activity streams.
 * <p>
 * All activity streams should support the following properties:
 * <ul>
 * <li>DateTime - default date/time to associate with activities. (Optional)
 * </li>
 * <li>HaltIfNoParser - if set to {@code true}, stream will halt if none of the
 * parsers can parse activity object RAW data. If set to {@code false} - puts
 * log entry and continues. (Optional)</li>
 * <li>UseExecutors - identifies whether stream should use executor service to
 * process activities data items asynchronously or not. (Optional)</li>
 * <li>ExecutorThreadsQuantity - defines executor service thread pool size.
 * (Optional)</li>
 * <li>ExecutorRejectedTaskOfferTimeout - time to wait (in seconds) for a
 * executor service to terminate. (Optional)</li>
 * <li>ExecutorsBoundedModel - identifies whether executor service should use
 * bounded tasks queue model. (Optional)</li>
 * <li>ExecutorsTerminationTimeout - time to wait (in seconds) for a task to be
 * inserted into bounded queue if max. queue size is reached. (Optional, actual
 * only if {@code ExecutorsBoundedModel} is set to {@code true})</li>
 * </ul>
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 1 $
 *
 * @see ExecutorService
 */
public abstract class TNTInputStream<T> implements Runnable {

	private static final int DEFAULT_EXECUTOR_THREADS_QTY = 4;
	private static final int DEFAULT_EXECUTORS_TERMINATION_TIMEOUT = 20;
	private static final int DEFAULT_EXECUTOR_REJECTED_TASK_TIMEOUT = 20;

	private static final String DEFAULT_SOURCE_NAME = "com.jkool.tnt4j.streams"; // NON-NLS

	/**
	 * Stream logger.
	 */
	protected final EventSink logger;

	/**
	 * StreamThread running this stream.
	 */
	protected StreamThread ownerThread = null;

	/**
	 * Map of parsers being used by stream.
	 */
	protected final Map<String, List<ActivityParser>> parsersMap = new LinkedHashMap<String, List<ActivityParser>>();

	/**
	 * Used to deliver processed activity data to destination.
	 */
	protected final Map<String, Tracker> trackersMap = new HashMap<String, Tracker>();

	private String tnt4jCfgFilePath;
	private TrackerConfig streamConfig;
	private Source defaultSource;
	private Map<String, String> tnt4jProperties;

	private boolean haltIfNoParser = true;

	private AtomicInteger currActivityIndex = new AtomicInteger(0);
	private AtomicInteger skippedActivitiesCount = new AtomicInteger(0);
	private AtomicLong streamedBytesCount = new AtomicLong(0);
	private long startTime = -1;
	private long endTime = -1;

	private List<InputStreamListener> streamListeners;
	private List<StreamTasksListener> streamTasksListeners;

	/**
	 * Delay between retries to submit data package to jKool Cloud Service if
	 * some transmission failure occurs, in milliseconds.
	 */
	protected static final long CONN_RETRY_INTERVAL = TimeUnit.SECONDS.toMillis(15);

	private boolean useExecutorService = false;
	private ExecutorService streamExecutorService = null;

	// executor service related properties
	private boolean boundedExecutorModel = false;
	private int executorThreadsQty = DEFAULT_EXECUTOR_THREADS_QTY;
	private int executorsTerminationTimeout = DEFAULT_EXECUTORS_TERMINATION_TIMEOUT;
	private int executorRejectedTaskOfferTimeout = DEFAULT_EXECUTOR_REJECTED_TASK_TIMEOUT;

	private String name;

	/**
	 * Constructs a new TNTInputStream.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected TNTInputStream(EventSink logger) {
		this.logger = logger;
	}

	/**
	 * Get the thread owning this stream.
	 *
	 * @return owner thread
	 */
	public StreamThread getOwnerThread() {
		return ownerThread;
	}

	/**
	 * Set the thread owning this stream.
	 *
	 * @param ownerThread
	 *            thread owning this stream
	 */
	public void setOwnerThread(StreamThread ownerThread) {
		this.ownerThread = ownerThread;
	}

	/**
	 * Set properties for activity stream. This method is invoked by the
	 * configuration loader in response to the {@code property} configuration
	 * elements. It is invoked once per stream definition, with all property
	 * names and values specified for this stream. Subclasses should generally
	 * override this method to process custom properties, and invoke the base
	 * class method to handle any built-in properties.
	 *
	 * @param props
	 *            properties to set
	 *
	 * @throws Exception
	 *             indicates error with properties
	 */
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamProperties.PROP_HALT_ON_PARSER.equalsIgnoreCase(name)) {
				haltIfNoParser = Boolean.parseBoolean(value);
			} else if (StreamProperties.PROP_USE_EXECUTOR_SERVICE.equalsIgnoreCase(name)) {
				useExecutorService = Boolean.parseBoolean(value);
			} else if (StreamProperties.PROP_EXECUTOR_THREADS_QTY.equalsIgnoreCase(name)) {
				executorThreadsQty = Integer.parseInt(value);
			} else if (StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT.equalsIgnoreCase(name)) {
				executorRejectedTaskOfferTimeout = Integer.parseInt(value);
			} else if (StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT.equalsIgnoreCase(name)) {
				executorsTerminationTimeout = Integer.parseInt(value);
			} else if (StreamProperties.PROP_EXECUTORS_BOUNDED.equalsIgnoreCase(name)) {
				boundedExecutorModel = Boolean.parseBoolean(value);
			}
		}
	}

	/**
	 * Get value of specified property. If subclasses override
	 * {@link #setProperties(Collection)}, they should generally override this
	 * method as well to return the value of custom properties, and invoke the
	 * base class method to handle any built-in properties.
	 * 
	 * @param name
	 *            name of property whose value is to be retrieved
	 *
	 * @return value for property, or {@code null} if property does not exist
	 */
	public Object getProperty(String name) {
		if (StreamProperties.PROP_DATETIME.equals(name)) {
			return getDate();
		}
		if (StreamProperties.PROP_HALT_ON_PARSER.equals(name)) {
			return haltIfNoParser;
		}
		if (StreamProperties.PROP_USE_EXECUTOR_SERVICE.equals(name)) {
			return useExecutorService;
		}
		if (StreamProperties.PROP_EXECUTOR_THREADS_QTY.equals(name)) {
			return executorThreadsQty;
		}
		if (StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT.equals(name)) {
			return executorRejectedTaskOfferTimeout;
		}
		if (StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT.equals(name)) {
			return executorsTerminationTimeout;
		}
		if (StreamProperties.PROP_EXECUTORS_BOUNDED.equals(name)) {
			return boundedExecutorModel;
		}

		return null;
	}

	/**
	 * Initialize the stream.
	 * <p>
	 * This method is called by default {@link #run()} method to perform any
	 * necessary initializations before the stream starts processing, including
	 * verifying that all required properties are set. If subclasses override
	 * this method to perform any custom initializations, they must call the
	 * base class method. If subclass also overrides the {@link #run()} method,
	 * it must call this at start of {@link #run()} method before entering into
	 * processing loop.
	 *
	 * @throws Exception
	 *             indicates that stream is not configured properly and cannot
	 *             continue.
	 */
	protected void initialize() throws Exception {
		streamConfig = DefaultConfigFactory.getInstance().getConfig(DEFAULT_SOURCE_NAME, SourceType.APPL,
				tnt4jCfgFilePath);
		if (MapUtils.isNotEmpty(tnt4jProperties)) {
			for (Map.Entry<String, String> tnt4jProp : tnt4jProperties.entrySet()) {
				streamConfig.setProperty(tnt4jProp.getKey(), tnt4jProp.getValue());
			}

			((TrackerConfigStore) streamConfig).applyProperties();
		}

		// NOTE: removing APPL=streams "layer" from default source and copy
		// SSN=streams value from config
		streamConfig = streamConfig.build();
		defaultSource = streamConfig.getSource().getSource();
		defaultSource.setSSN(streamConfig.getSource().getSSN());

		if (useExecutorService) {
			streamExecutorService = boundedExecutorModel
					? getBoundedExecutorService(executorThreadsQty, executorRejectedTaskOfferTimeout)
					: getDefaultExecutorService(executorThreadsQty);
		} else {
			initNewDefaultTracker(ownerThread == null ? Thread.currentThread() : ownerThread);
		}
	}

	private void initNewDefaultTracker(Thread t) {
		Tracker tracker = TrackingLogger.getInstance(streamConfig.build());
		trackersMap.put(getTrackersMapKey(t, defaultSource.getFQName()), tracker);
		logger.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "TNTInputStream.default.tracker"),
				(t == null ? "null" : t.getName()), defaultSource.getFQName());
	}

	/**
	 * Gets path string of TNT4J configuration file.
	 *
	 * @return returns path of TNT4J configuration file
	 */
	public String getTnt4jCfgFilePath() {
		return tnt4jCfgFilePath;
	}

	/**
	 * Sets path string of TNT4J configuration file.
	 *
	 * @param tnt4jCfgFilePath
	 *            path of TNT4J configuration file
	 */
	public void setTnt4jCfgFilePath(String tnt4jCfgFilePath) {
		this.tnt4jCfgFilePath = tnt4jCfgFilePath;
	}

	/**
	 * Adds TNT4J configuration property specific for this stream.
	 *
	 * @param key
	 *            property key
	 * @param value
	 *            property value
	 *
	 * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
	 *         if there was no mapping for <tt>key</tt>.
	 */
	public String addTNT4JProperty(String key, String value) {
		if (tnt4jProperties == null) {
			tnt4jProperties = new HashMap<String, String>();
		}

		return tnt4jProperties.put(key, value);
	}

	/**
	 * Creates default thread pool executor service for a given number of
	 * threads. Using this executor service tasks queue size is unbound. Thus
	 * memory use may be high to store all producer thread created tasks.
	 *
	 * @param threadsQty
	 *            the number of threads in the pool
	 *
	 * @return the newly created thread pool executor
	 *
	 * @see ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit,
	 *      BlockingQueue, ThreadFactory)
	 */
	private ExecutorService getDefaultExecutorService(int threadsQty) {
		StreamsThreadFactory stf = new StreamsThreadFactory("StreamDefaultExecutorThread-"); // NON-NLS
		stf.addThreadFactoryListener(new StreamsThreadFactory.StreamsThreadFactoryListener() {
			@Override
			public void newThreadCreated(Thread t) {
				initNewDefaultTracker(t);
			}
		});

		ThreadPoolExecutor tpe = new ThreadPoolExecutor(threadsQty, threadsQty, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(), stf);

		return tpe;
	}

	/**
	 * Creates thread pool executor service for a given number of threads with
	 * bounded tasks queue - queue size is 2x{@code threadsQty}. When queue size
	 * is reached, new tasks are offered to queue using defined offer timeout.
	 * If task can't be put into queue over this time, task is skipped with
	 * making warning log entry. Thus memory use does not grow drastically if
	 * consumers can't keep up the pace of producers filling in the queue,
	 * making producers synchronize with consumers.
	 * 
	 * @param threadsQty
	 *            the number of threads in the pool
	 * @param offerTimeout
	 *            how long to wait before giving up on offering task to queue
	 *
	 * @return the newly created thread pool executor
	 *
	 * @see ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit,
	 *      BlockingQueue, ThreadFactory)
	 */
	private ExecutorService getBoundedExecutorService(int threadsQty, final int offerTimeout) {
		StreamsThreadFactory stf = new StreamsThreadFactory("StreamBoundedExecutorThread-"); // NON-NLS
		stf.addThreadFactoryListener(new StreamsThreadFactory.StreamsThreadFactoryListener() {
			@Override
			public void newThreadCreated(Thread t) {
				initNewDefaultTracker(t);
			}
		});

		ThreadPoolExecutor tpe = new ThreadPoolExecutor(threadsQty, threadsQty, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(threadsQty * 2), stf);

		tpe.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				try {
					boolean added = executor.getQueue().offer(r, offerTimeout, TimeUnit.SECONDS);
					if (!added) {
						logger.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
								"TNTInputStream.tasks.buffer.limit"), offerTimeout);
						notifyStreamTaskRejected(r);
					}
				} catch (InterruptedException exc) {
					halt();
				}
			}
		});

		return tpe;
	}

	/**
	 * Adds the specified parser to the list of parsers being used by this
	 * stream.
	 *
	 * @param parser
	 *            parser to add
	 *
	 * @throws IllegalStateException
	 *             if parser can't be added to stream
	 */
	public void addParser(ActivityParser parser) throws IllegalStateException {
		String[] tags = parser.getTags();

		if (ArrayUtils.isNotEmpty(tags)) {
			for (String tag : tags) {
				addTaggedParser(tag, parser);
			}
		} else {
			addTaggedParser(parser.getName(), parser);
		}
	}

	private void addTaggedParser(String tag, ActivityParser parser) {
		List<ActivityParser> tpl = parsersMap.get(tag);

		if (tpl == null) {
			tpl = new ArrayList<ActivityParser>();
			parsersMap.put(tag, tpl);
		}

		tpl.add(parser);
	}

	/**
	 * Adds reference to specified entity object being used by this stream.
	 *
	 * @param refObject
	 *            entity object to reference
	 *
	 * @throws IllegalStateException
	 *             if referenced object can't be linked to stream
	 */
	public void addReference(Object refObject) throws IllegalStateException {
		if (refObject instanceof ActivityParser) {
			ActivityParser ap = (ActivityParser) refObject;
			addParser(ap);
		}
	}

	/**
	 * <p>
	 * Get the position in the source activity data currently being processed.
	 * For line-based data sources, this is generally the line number of
	 * currently processed file or other text source. If activity items source
	 * (i.e. file) changes - activity position gets reset.
	 * <p>
	 * Subclasses should override this to provide meaningful information, if
	 * relevant. The default implementation just returns 0.
	 *
	 * @return current position in activity data source being processed, or
	 *         {@code 0} if activity position can't be determined
	 *
	 * @see #getCurrentActivity()
	 */
	public int getActivityPosition() {
		return 0;
	}

	/**
	 * Returns currently streamed activity item index. Index is constantly
	 * incremented when streaming begins and activity items gets available to
	 * stream.
	 * <p>
	 * It does not matter if activity item source changes (i.e. file). To get
	 * actual source dependent position see {@link #getActivityPosition()}.
	 * 
	 * @return currently processed activity item index
	 *
	 * @see #getActivityPosition()
	 * @see #getTotalActivities()
	 */
	public int getCurrentActivity() {
		return currActivityIndex.get();
	}

	/**
	 * Returns total number of activity items to be streamed.
	 * 
	 * @return total number of activities available to stream, or {@code -1} if
	 *         total number of activities is undetermined
	 *
	 * @see #getCurrentActivity()
	 */
	public int getTotalActivities() {
		return -1;
	}

	/**
	 * Returns size in bytes of activity data items available to stream. If
	 * total size can't be determined, then {@code 0} is returned.
	 * 
	 * @return total size in bytes of activity data items
	 */
	public long getTotalBytes() {
		return 0;
	}

	/**
	 * Returns size in bytes if streamed activity data items.
	 * 
	 * @return streamed activity data items size in bytes
	 */
	public long getStreamedBytesCount() {
		return streamedBytesCount.get();
	}

	/**
	 * Returns number of activity data items skipped from streaming. Item may be
	 * skipped if it can't be parsed or some non-critical exception occurs.
	 * 
	 * @return number of skipped activities
	 */
	public int getSkippedActivitiesCount() {
		return skippedActivitiesCount.get();
	}

	/**
	 * Adds number of bytes to streamed bytes counter.
	 *
	 * @param bytesCount
	 *            number of bytes to add
	 */
	protected void addStreamedBytesCount(long bytesCount) {
		streamedBytesCount.addAndGet(bytesCount);
	}

	/**
	 * Returns duration of streaming process.
	 * 
	 * @return duration of steaming process
	 */
	public long getElapsedTime() {
		long et = endTime < 0 ? System.currentTimeMillis() : endTime;

		return startTime < 0 ? -1 : et - startTime;
	}

	/**
	 * Creates snapshot of instant stream statistics.
	 * 
	 * @return snapshot of instant stream statistics
	 */
	public StreamStats getStreamStatistics() {
		StreamStats stats = new StreamStats(this);

		return stats;
	}

	/**
	 * Get the next raw activity data item to be processed. All subclasses must
	 * implement this.
	 *
	 * @return next raw activity data item, or {@code null} if there is no next
	 *         item
	 *
	 * @throws Exception
	 *             if any errors occurred getting next item
	 */
	public abstract T getNextItem() throws Exception;

	/**
	 * Makes activity information {@link ActivityInfo} object from raw activity
	 * data item.
	 * <p>
	 * Default implementation simply calls {@link #applyParsers(Object)} to
	 * process raw activity data item.
	 *
	 * @param data
	 *            raw activity data item.
	 *
	 * @return activity information object
	 *
	 * @throws Exception
	 *             if error occurs while parsing raw activity data item
	 */
	protected ActivityInfo makeActivityInfo(T data) throws Exception {
		ActivityInfo ai = null;
		if (data != null) {
			try {
				ai = applyParsers(data);
			} catch (ParseException exc) {
				int position = getActivityPosition();
				ParseException pe = new ParseException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_CORE, "TNTInputStream.failed.to.process", position), position);
				pe.initCause(exc);
				throw pe;
			}
		}
		return ai;
	}

	/**
	 * Applies all defined parsers for this stream that support the format that
	 * the raw activity data is in the order added until one successfully
	 * matches the specified activity data item.
	 *
	 * @param data
	 *            activity data item to process
	 *
	 * @return processed activity data item, or {@code null} if activity data
	 *         item does not match rules for any parsers
	 *
	 * @throws IllegalStateException
	 *             if parser fails to run
	 * @throws ParseException
	 *             if any parser encounters an error parsing the activity data
	 */
	protected ActivityInfo applyParsers(Object data) throws IllegalStateException, ParseException {
		return applyParsers(null, data);
	}

	/**
	 * Applies all defined parsers for this stream that support the format that
	 * the raw activity data is in the order added until one successfully
	 * matches the specified activity data item.
	 *
	 * @param tags
	 *            array of tag strings to map activity data with parsers. Can be
	 *            {@code null}.
	 * @param data
	 *            activity data item to process
	 *
	 * @return processed activity data item, or {@code null} if activity data
	 *         item does not match rules for any parsers
	 *
	 * @throws IllegalStateException
	 *             if parser fails to run
	 * @throws ParseException
	 *             if any parser encounters an error parsing the activity data
	 */
	protected ActivityInfo applyParsers(String[] tags, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		Set<ActivityParser> parsers = getParsersFor(tags);
		for (ActivityParser parser : parsers) {
			if (parser.isDataClassSupported(data)) {
				ActivityInfo ai = parser.parse(this, data);
				if (ai != null) {
					// NOTE: TNT4J API fails if operation name is null
					if (StringUtils.isEmpty(ai.getEventName())) {
						ai.setEventName(name == null ? parser.getName() : name);
					}

					return ai;
				}
			}
		}
		return null;
	}

	private Set<ActivityParser> getParsersFor(String[] tags) {
		Set<ActivityParser> parsersSet = new LinkedHashSet<ActivityParser>();

		if (ArrayUtils.isNotEmpty(tags)) {
			for (String tag : tags) {
				List<ActivityParser> tpl = parsersMap.get(tag);

				if (tpl != null) {
					parsersSet.addAll(tpl);
				}
			}
		}

		if (parsersSet.isEmpty()) {
			Collection<List<ActivityParser>> allParsers = parsersMap.values();

			for (List<ActivityParser> tpl : allParsers) {
				if (tpl != null) {
					parsersSet.addAll(tpl);
				}
			}
		}

		return parsersSet;
	}

	/**
	 * Gets the default date/time to use for activity entries that do not
	 * contain a date. Default implementation returns the current date.
	 *
	 * @return default date/time to use for activity entries
	 */
	public Date getDate() {
		return new Date();
	}

	/**
	 * Signals that this stream should stop processing so that controlling
	 * thread will terminate.
	 */
	public void halt() {
		shutdownExecutors();

		ownerThread.halt();
	}

	/**
	 * Indicates whether this stream has stopped.
	 *
	 * @return {@code true} if stream has stopped processing, {@code false} -
	 *         otherwise
	 */
	public boolean isHalted() {
		return ownerThread.isStopRunning();
	}

	/**
	 * Cleanup the stream.
	 * <p>
	 * This method is called by default {@link #run()} method to perform any
	 * necessary cleanup before the stream stops processing, releasing any
	 * resources created by {@link #initialize()} method. If subclasses override
	 * this method to perform any custom cleanup, they must call the base class
	 * method. If subclass also overrides the {@link #run()} method, it must
	 * call this at end of {@link #run()} method before returning.
	 */
	protected void cleanup() {
		if (!trackersMap.isEmpty()) {
			for (Map.Entry<String, Tracker> te : trackersMap.entrySet()) {
				Tracker tracker = te.getValue();
				Utils.close(tracker);
			}

			trackersMap.clear();
		}

		if (CollectionUtils.isNotEmpty(streamListeners)) {
			streamListeners.clear();
		}

		if (CollectionUtils.isNotEmpty(streamTasksListeners)) {
			streamTasksListeners.clear();
		}
	}

	private synchronized void shutdownExecutors() {
		if (streamExecutorService == null || streamExecutorService.isShutdown()) {
			return;
		}

		streamExecutorService.shutdown();
		try {
			streamExecutorService.awaitTermination(executorsTerminationTimeout, TimeUnit.SECONDS);
		} catch (InterruptedException exc) {
			halt();
		} finally {
			List<Runnable> droppedTasks = streamExecutorService.shutdownNow();

			if (CollectionUtils.isNotEmpty(droppedTasks)) {
				notifyStreamTasksDropOff(droppedTasks);
			}
		}
	}

	private Tracker getTracker(String aiSourceFQN, Thread t) {
		synchronized (trackersMap) {
			Tracker tracker = trackersMap
					.get(getTrackersMapKey(t, aiSourceFQN == null ? defaultSource.getFQName() : aiSourceFQN));
			if (tracker == null) {
				Source aiSource = DefaultSourceFactory.getInstance().newFromFQN(aiSourceFQN);
				aiSource.setSSN(defaultSource.getSSN());
				streamConfig.setSource(aiSource);
				tracker = TrackingLogger.getInstance(streamConfig.build());
				trackersMap.put(getTrackersMapKey(t, aiSource.getFQName()), tracker);
				logger.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"TNTInputStream.build.new.tracker"), aiSource.getFQName());
			}

			return tracker;
		}
	}

	private static String getTrackersMapKey(Thread t, String sourceFQN) {
		return (t == null ? "null" : t.getId()) + ":?:" + sourceFQN; // NON-NLS
	}

	/**
	 * Starts input stream processing. Implementing {@link Runnable} interface
	 * makes it possible to process each stream in separate thread.
	 */
	@Override
	public void run() {
		notifyStatusChange(StreamStatus.NEW);

		logger.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "TNTInputStream.starting"), name);
		if (ownerThread == null) {
			IllegalStateException e = new IllegalStateException(StreamsResources
					.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "TNTInputStream.no.owner.thread"));
			notifyFailed(null, e, null);

			throw e;
		}

		AtomicBoolean failureFlag = new AtomicBoolean(false);
		try {
			initialize();
			startTime = System.currentTimeMillis();
			notifyStatusChange(StreamStatus.STARTED);
			while (!isHalted()) {
				try {
					T item = getNextItem();
					if (item == null) {
						logger.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
								"TNTInputStream.data.stream.ended"), name);
						halt(); // no more data items to process
					} else {
						if (streamExecutorService == null) {
							processActivityItem(item, failureFlag);
						} else {
							streamExecutorService.submit(new ActivityItemProcessingTask(item, failureFlag));
						}
					}
				} catch (IllegalStateException ise) {
					logger.log(OpLevel.ERROR,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
									"TNTInputStream.failed.record.activity.at"),
							getActivityPosition(), ise.getLocalizedMessage(), ise);
					failureFlag.set(true);
					notifyFailed(null, ise, null);
					halt();
				} catch (Exception exc) {
					logger.log(OpLevel.ERROR,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
									"TNTInputStream.failed.record.activity.at"),
							getActivityPosition(), exc.getLocalizedMessage(), exc);
				}
			}
		} catch (Exception e) {
			logger.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"TNTInputStream.failed.record.activity"), e.getLocalizedMessage(), e);
			failureFlag.set(true);
			notifyFailed(null, e, null);
		} finally {
			endTime = System.currentTimeMillis();
			if (!failureFlag.get()) {
				notifyStreamSuccess();
			}
			notifyFinished();

			cleanup();

			logger.log(OpLevel.INFO,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "TNTInputStream.thread.ended"),
					Thread.currentThread().getName());
			logger.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"TNTInputStream.stream.statistics"), name, getStreamStatistics());
		}
	}

	private void processActivityItem(T item, AtomicBoolean failureFlag) throws Exception {
		notifyProgressUpdate(currActivityIndex.incrementAndGet(), getTotalActivities());

		ActivityInfo ai = makeActivityInfo(item);
		if (ai == null) {
			logger.log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "TNTInputStream.no.parser"),
					item);
			skippedActivitiesCount.incrementAndGet();
			if (haltIfNoParser) {
				failureFlag.set(true);
				notifyFailed(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
						"TNTInputStream.no.parser", item), null, null);
				halt();
			} else {
				notifyStreamEvent(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"TNTInputStream.could.not.parse.activity"), item);
			}
		} else {
			if (!ai.isFiltered()) {
				Tracker tracker = getTracker(ai.getSourceFQN(), Thread.currentThread());

				while (!isHalted() && !tracker.isOpen()) {
					try {
						tracker.open();
					} catch (IOException ioe) {
						logger.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
								"TNTInputStream.failed.to.connect"), tracker, ioe);
						Utils.close(tracker);
						logger.log(OpLevel.INFO,
								StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
										"TNTInputStream.will.retry"),
								TimeUnit.MILLISECONDS.toSeconds(CONN_RETRY_INTERVAL));
						if (!isHalted()) {
							StreamsThread.sleep(CONN_RETRY_INTERVAL);
						}
					}
				}

				ai.recordActivity(tracker, CONN_RETRY_INTERVAL);
			}
		}
	}

	// /**
	// * Signals that streaming process has to be canceled and invokes status
	// * change event.
	// */
	// public void cancel() { // TODO
	// halt();
	//
	// // ownerThread.join();
	//
	// notifyStatusChange(StreamStatus.CANCEL);
	// // notifyFinished();
	//
	// // shutdownExecutors();
	// // cleanup();
	// }

	/**
	 * Returns stream name value
	 *
	 * @return stream name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets stream name value
	 *
	 * @param name
	 *            stream name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Adds defined {@code InputStreamListener} to stream listeners list.
	 *
	 * @param l
	 *            the {@code InputStreamListener} to be added
	 */
	public void addStreamListener(InputStreamListener l) {
		if (l == null) {
			return;
		}

		if (streamListeners == null) {
			streamListeners = new ArrayList<InputStreamListener>();
		}

		streamListeners.add(l);
	}

	/**
	 * Removes defined {@code InputStreamListener} from stream listeners list.
	 *
	 * @param l
	 *            the {@code InputStreamListener} to be removed
	 */
	public void removeStreamListener(InputStreamListener l) {
		if (l != null && streamListeners != null) {
			streamListeners.remove(l);
		}
	}

	/**
	 * Notifies that activity items streaming process progress has updated.
	 *
	 * @param curr
	 *            index of currently streamed activity item
	 * @param total
	 *            total number of activity items to stream
	 */
	protected void notifyProgressUpdate(int curr, int total) {
		if (streamListeners != null) {
			for (InputStreamListener l : streamListeners) {
				l.onProgressUpdate(this, curr, total);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process has completed
	 * successfully.
	 */
	@SuppressWarnings("unchecked")
	public void notifyStreamSuccess() {
		notifyStatusChange(StreamStatus.SUCCESS);
		if (streamListeners != null) {
			for (InputStreamListener l : streamListeners) {
				l.onSuccess(this);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process has failed.
	 *
	 * @param msg
	 *            failure message
	 * @param exc
	 *            failure exception
	 * @param code
	 *            failure code
	 */
	protected void notifyFailed(String msg, Throwable exc, String code) {
		notifyStatusChange(StreamStatus.FAILURE);
		if (streamListeners != null) {
			for (InputStreamListener l : streamListeners) {
				l.onFailure(this, msg, exc, code);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process status has changed.
	 *
	 * @param newStatus
	 *            new stream status value
	 */
	protected void notifyStatusChange(StreamStatus newStatus) {
		if (streamListeners != null) {
			for (InputStreamListener l : streamListeners) {
				l.onStatusChange(this, newStatus);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process has finished independent
	 * of completion state.
	 */
	protected void notifyFinished() {
		if (streamListeners != null) {
			StreamStats stats = getStreamStatistics();
			for (InputStreamListener l : streamListeners) {
				l.onFinish(this, stats);
			}
		}
	}

	/**
	 * Notifies that activity items streaming process detects some notable
	 * event.
	 * 
	 * @param level
	 *            event severity level
	 * @param message
	 *            event related message
	 * @param source
	 *            event source
	 */
	protected void notifyStreamEvent(OpLevel level, String message, Object source) {
		if (streamListeners != null) {
			for (InputStreamListener l : streamListeners) {
				l.onStreamEvent(this, level, message, source);
			}
		}
	}

	/**
	 * Adds defined {@code StreamTasksListener} to stream tasks listeners list.
	 *
	 * @param l
	 *            the {@code StreamTasksListener} to be added
	 */
	public void addStreamTasksListener(StreamTasksListener l) {
		if (l == null) {
			return;
		}

		if (streamTasksListeners == null) {
			streamTasksListeners = new ArrayList<StreamTasksListener>();
		}

		streamTasksListeners.add(l);
	}

	/**
	 * Removes defined {@code StreamTasksListener} from stream tasks listeners
	 * list.
	 *
	 * @param l
	 *            the {@code StreamTasksListener} to be removed
	 */
	public void removeStreamTasksListener(StreamTasksListener l) {
		if (l != null && streamTasksListeners != null) {
			streamTasksListeners.remove(l);
		}
	}

	/**
	 * Notifies that stream executor service has rejected offered activity items
	 * streaming task to queue.
	 * 
	 * @param task
	 *            executor rejected task
	 */
	protected void notifyStreamTaskRejected(Runnable task) {
		if (streamTasksListeners != null) {
			for (StreamTasksListener l : streamTasksListeners) {
				l.onReject(this, task);
			}
		}
	}

	/**
	 * Notifies that stream executor service has been shot down and some of
	 * unprocessed activity items streaming tasks has been dropped of the queue.
	 * 
	 * @param tasks
	 *            list of executor dropped of tasks
	 */
	protected void notifyStreamTasksDropOff(List<Runnable> tasks) {
		if (streamTasksListeners != null) {
			for (StreamTasksListener l : streamTasksListeners) {
				l.onDropOff(this, tasks);
			}
		}
	}

	private class ActivityItemProcessingTask implements Runnable {
		private T item;
		private AtomicBoolean failureFlag;

		/**
		 * Constructs a new ActivityItemProcessingTask.
		 *
		 * @param activityItem
		 *            raw activity data item to process asynchronously
		 */
		ActivityItemProcessingTask(T activityItem, AtomicBoolean failureFlag) {
			this.item = activityItem;
			this.failureFlag = failureFlag;
		}

		@Override
		public void run() {
			try {
				processActivityItem(item, failureFlag);
			} catch (Exception e) { // TODO: better handling
				logger.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"TNTInputStream.failed.record.activity"), e.getLocalizedMessage(), e);
				failureFlag.set(true);
				notifyFailed(null, e, null);
			}
		}

		/**
		 * Return string representing class name of task object and wrapped
		 * activity item data.
		 *
		 * @return a string representing activity item processing task
		 */
		@Override
		public String toString() {
			return "ActivityItemProcessingTask{" + "item=" + item + '}'; // NON-NLS
		}
	}

	/**
	 * TNT4J-Streams thread factory.
	 *
	 * @version $Revision: 1 $
	 */
	public static class StreamsThreadFactory implements ThreadFactory {
		private AtomicInteger count = new AtomicInteger(1);
		private String prefix;
		private List<StreamsThreadFactoryListener> listeners = null;

		/**
		 * Constructs a new StreamsThreadFactory.
		 *
		 * @param prefix
		 *            thread name prefix
		 */
		public StreamsThreadFactory(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public Thread newThread(Runnable r) {
			StreamsThread task = new StreamsThread(r, prefix + count.getAndIncrement());
			task.setDaemon(true);

			notifyNewThreadCreated(task);

			return task;
		}

		private void notifyNewThreadCreated(Thread t) {
			if (listeners != null) {
				for (StreamsThreadFactoryListener l : listeners) {
					l.newThreadCreated(t);
				}
			}
		}

		public void addThreadFactoryListener(StreamsThreadFactoryListener l) {
			if (l == null) {
				return;
			}

			if (listeners == null) {
				listeners = new ArrayList<StreamsThreadFactoryListener>();
			}

			listeners.add(l);
		}

		public void removeStreamListener(StreamsThreadFactoryListener l) {
			if (l != null && listeners != null) {
				listeners.remove(l);
			}
		}

		public interface StreamsThreadFactoryListener {
			void newThreadCreated(Thread t);
		}
	}

	/**
	 * Class representing snapshot of instant stream statistics.
	 */
	public static class StreamStats {
		private int activitiesTotal;
		private int currActivity;

		private long totalBytes;
		private long bytesStreamed;

		private long skippedActivities;
		private long streamedBytes;

		private long elapsedTime;

		StreamStats(TNTInputStream stream) {
			this.activitiesTotal = stream.getTotalActivities();
			this.currActivity = stream.getCurrentActivity();
			this.totalBytes = stream.getTotalBytes();
			this.bytesStreamed = stream.getStreamedBytesCount();
			this.skippedActivities = stream.getSkippedActivitiesCount();
			this.elapsedTime = stream.getElapsedTime();
		}

		/**
		 * Returns total number of activities available to stream.
		 * 
		 * @return total number of available activities
		 */
		public int getActivitiesTotal() {
			return activitiesTotal;
		}

		/**
		 * Returns number of currently streamed activity data item.
		 *
		 * @return activity data item number.
		 */
		public int getCurrActivity() {
			return currActivity;
		}

		/**
		 * Returns total size (in bytes) of activity items available to stream.
		 *
		 * @return number of total bytes
		 */
		public long getTotalBytes() {
			return totalBytes;
		}

		/**
		 * Returns size (in bytes) of streamed activity data items.
		 *
		 * @return number of streamed bytes
		 */
		public long getBytesStreamed() {
			return bytesStreamed;
		}

		/**
		 * Returns duration of streaming process.
		 *
		 * @return streaming process duration
		 */
		public long getElapsedTime() {
			return elapsedTime;
		}

		/**
		 * Returns number of activities skipped by stream.
		 * 
		 * @return number of skipped activities.
		 */
		public long getSkippedActivities() {
			return skippedActivities;
		}

		/**
		 * Returns stream statistics as text string.
		 * 
		 * @return stream statistics text string
		 */
		@Override
		public String toString() {
			return "StreamStats{" + "activities total=" + activitiesTotal + ", current activity=" + currActivity // NON-NLS
					+ ", total bytes=" + totalBytes + ", bytes streamed=" + bytesStreamed + ", skipped activities=" // NON-NLS
					+ skippedActivities + ", elapsed time=" + DurationFormatUtils.formatDurationHMS(elapsedTime) + '}'; // NON-NLS
		}
	}
}
