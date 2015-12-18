/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.inputs;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang.ArrayUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.StreamsThread;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.config.DefaultConfigFactory;
import com.nastel.jkool.tnt4j.config.TrackerConfig;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.Tracker;

/**
 * <p>
 * Base class that all activity streams must extend. It provides some base
 * functionality useful for all activity streams.
 * </p>
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
 * <li>ExecutorsTerminationTimeout -time to wait (in seconds) for a task to be
 * inserted into bounded queue if max. queue size is reached. (Optional, actual
 * only if ExecutorsBoundedModel is set to {@code true})</li>
 * </ul>
 *
 * @version $Revision: 8 $
 */
public abstract class TNTInputStream<T> implements Runnable {

	private static final int DEFAULT_EXECUTOR_THREADS_QTY = 4;
	private static final int DEFAULT_EXECUTORS_TERMINATION_TIMEOUT = 20;
	private static final int DEFAULT_EXECUTOR_REJECTED_TASK_TIMEOUT = 20;

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

	private TrackerConfig streamConfig;
	private Source defaultSource;

	private boolean haltIfNoParser = true;

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

	/**
	 * Initializes TNTInputStream.
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
	 * @throws Throwable
	 *             indicates error with properties
	 */
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_HALT_ON_PARSER.equalsIgnoreCase(name)) {
				haltIfNoParser = Boolean.parseBoolean(value);
			} else if (StreamsConfig.PROP_USE_EXECUTOR_SERVICE.equalsIgnoreCase(name)) {
				useExecutorService = Boolean.parseBoolean(value);
			} else if (StreamsConfig.PROP_EXECUTOR_THREADS_QTY.equalsIgnoreCase(name)) {
				executorThreadsQty = Integer.parseInt(value);
			} else if (StreamsConfig.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT.equalsIgnoreCase(name)) {
				executorRejectedTaskOfferTimeout = Integer.parseInt(value);
			} else if (StreamsConfig.PROP_EXECUTORS_TERMINATION_TIMEOUT.equalsIgnoreCase(name)) {
				executorsTerminationTimeout = Integer.parseInt(value);
			} else if (StreamsConfig.PROP_EXECUTORS_BOUNDED.equalsIgnoreCase(name)) {
				boundedExecutorModel = Boolean.parseBoolean(value);
			}
		}
	}

	/**
	 * Get value of specified property. If subclasses override
	 * {@link #setProperties(Collection)}, they should generally override this
	 * method as well to return the value of custom properties, and invoke the
	 * base class method to handle any built-in properties.
	 * <p>
	 * The default implementation handles the
	 * {@value com.jkool.tnt4j.streams.configure.StreamsConfig#PROP_DATETIME}
	 * property and returns the value of the {@link #getDate()} method.
	 *
	 * @param name
	 *            name of property whose value is to be retrieved
	 *
	 * @return value for property, or {@code null} if property does not exist
	 */
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_DATETIME.equals(name)) {
			return getDate();
		}
		if (StreamsConfig.PROP_HALT_ON_PARSER.equals(name)) {
			return haltIfNoParser;
		}
		if (StreamsConfig.PROP_USE_EXECUTOR_SERVICE.equals(name)) {
			return useExecutorService;
		}
		if (StreamsConfig.PROP_EXECUTOR_THREADS_QTY.equals(name)) {
			return executorThreadsQty;
		}
		if (StreamsConfig.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT.equals(name)) {
			return executorRejectedTaskOfferTimeout;
		}
		if (StreamsConfig.PROP_EXECUTORS_TERMINATION_TIMEOUT.equals(name)) {
			return executorsTerminationTimeout;
		}
		if (StreamsConfig.PROP_EXECUTORS_BOUNDED.equals(name)) {
			return boundedExecutorModel;
		}

		return null;
	}

	/**
	 * Initialize the stream.
	 * <p>
	 * This method is called by default {@code run} method to perform any
	 * necessary initializations before the stream starts processing, including
	 * verifying that all required properties are set. If subclasses override
	 * this method to perform any custom initializations, they must call the
	 * base class method. If subclass also overrides the {@code run} method, it
	 * must call this at start of {@code run} method before entering into
	 * processing loop.
	 *
	 * @throws Throwable
	 *             indicates that stream is not configured properly and cannot
	 *             continue.
	 */
	protected void initialize() throws Throwable {
		streamConfig = DefaultConfigFactory.getInstance().getConfig("com.jkool.tnt4j.streams"); // NON-NLS
		Tracker tracker = TrackingLogger.getInstance(streamConfig.build());
		defaultSource = streamConfig.getSource();
		trackersMap.put(defaultSource.getFQName(), tracker);
		logger.log(OpLevel.DEBUG,
				StreamsResources.getStringFormatted("TNTInputStream.default.tracker", defaultSource.getFQName()));

		if (useExecutorService) {
			streamExecutorService = boundedExecutorModel
					? getBoundedExecutorService(executorThreadsQty, executorRejectedTaskOfferTimeout)
					: getDefaultExecutorService(executorThreadsQty);
		}

		// t1 = System.currentTimeMillis();
	}

	// long t1;

	/**
	 * Creates default thread pool executor service for a given number of
	 * threads. Using this executor service tasks queue size is unbound. Thus
	 * memory use may be high to store all producer thread created tasks.
	 *
	 * @param threadsQty
	 *            the number of threads in the pool
	 *
	 * @return the newly created thread pool
	 *
	 * @see Executors#newFixedThreadPool(int)
	 */
	private static ExecutorService getDefaultExecutorService(int threadsQty) {
		return Executors.newFixedThreadPool(threadsQty, new StreamExecutorThreadFactory("StreamExecutorThread-"));
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
	 * @return the newly created thread pool
	 *
	 * @see ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit,
	 *      BlockingQueue, ThreadFactory)
	 */
	private ExecutorService getBoundedExecutorService(int threadsQty, final int offerTimeout) {
		ThreadPoolExecutor tpe = new ThreadPoolExecutor(threadsQty, threadsQty, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingDeque<Runnable>(threadsQty * 2),
				new StreamExecutorThreadFactory("StreamExecutorThread-"));

		tpe.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				try {
					boolean added = executor.getQueue().offer(r, offerTimeout, TimeUnit.SECONDS);
					if (!added) {
						logger.log(OpLevel.WARNING,
								StreamsResources.getStringFormatted("TNTInputStream.tasks.buffer.limit", offerTimeout));
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
	 */
	public void addParser(ActivityParser parser) {
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
	 * Get the position in the source activity data currently being processed.
	 * For line-based data sources, this is generally the line number.
	 * Subclasses should override this to provide meaningful information, if
	 * relevant. The default implementation just returns 0.
	 *
	 * @return current position in activity data source being processed
	 */
	public int getActivityPosition() {
		return 0;
	}

	/**
	 * Get the next raw activity data item to be processed. All subclasses must
	 * implement this.
	 *
	 * @return next raw activity data item, or {@code null} if there is no next
	 *         item
	 *
	 * @throws Throwable
	 *             if any errors occurred getting next item
	 */
	public abstract T getNextItem() throws Throwable;

	/**
	 * Makes activity information {@code ActivityInfo} object from raw activity
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
	 * @throws Throwable
	 *             if error occurs while parsing raw activity data item
	 */
	protected ActivityInfo makeActivityInfo(T data) throws Throwable {
		ActivityInfo ai = null;
		if (data != null) {
			try {
				ai = applyParsers(data);
			} catch (ParseException exc) {
				int position = getActivityPosition();
				ParseException pe = new ParseException(
						StreamsResources.getStringFormatted("TNTInputStream.failed.to.process", position), position);
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
		ownerThread.halt();
	}

	/**
	 * Indicates whether this stream has stopped.
	 *
	 * @return {@code true} if stream has stopped processing, {@code false}
	 *         otherwise
	 */
	public boolean isHalted() {
		return ownerThread.isStopRunning();
	}

	/**
	 * Cleanup the stream.
	 * <p>
	 * This method is called by default {@code run} method to perform any
	 * necessary cleanup before the stream stops processing, releasing any
	 * resources created by {@link #initialize()} method. If subclasses override
	 * this method to perform any custom cleanup, they must call the base class
	 * method. If subclass also overrides the {@code run} method, it must call
	 * this at end of {@code run} method before returning.
	 */
	protected void cleanup() {
		if (!trackersMap.isEmpty()) {
			for (Map.Entry<String, Tracker> te : trackersMap.entrySet()) {
				Tracker tracker = te.getValue();
				Utils.close(tracker);
			}

			trackersMap.clear();
		}

		// System.out.println("====================>>>>>>>>>>>>>> ELAPSED TIME:
		// " + (System.currentTimeMillis() - t1));
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
			streamExecutorService.shutdownNow();
		}
	}

	private Tracker getTracker(Source aiSource) {
		synchronized (trackersMap) {
			Tracker tracker = trackersMap.get(aiSource == null ? defaultSource.getFQName() : aiSource.getFQName());
			if (tracker == null) {
				aiSource.setSSN(defaultSource.getSSN());
				streamConfig.setSource(aiSource);
				tracker = TrackingLogger.getInstance(streamConfig.build());
				trackersMap.put(aiSource.getFQName(), tracker);
				logger.log(OpLevel.DEBUG,
						StreamsResources.getStringFormatted("TNTInputStream.build.new.tracker", aiSource.getFQName()));
			}

			return tracker;
		}
	}

	/**
	 * Starts input stream processing. Implementing {@code Runnable} interface
	 * makes it possible to process each stream in separate thread.
	 *
	 * @see Runnable#run()
	 */
	@Override
	public void run() {
		logger.log(OpLevel.INFO, StreamsResources.getString("TNTInputStream.starting"));
		if (ownerThread == null) {
			throw new IllegalStateException(StreamsResources.getString("TNTInputStream.no.owner.thread"));
		}
		try {
			initialize();
			while (!isHalted()) {
				try {
					T item = getNextItem();
					if (item == null) {
						logger.log(OpLevel.INFO, StreamsResources.getString("TNTInputStream.data.stream.ended"));
						shutdownExecutors();
						halt(); // no more data items to process
					} else {
						if (streamExecutorService == null) {
							processActivityItem(item);
						} else {
							streamExecutorService.submit(new ActivityItemProcessingTask(item));
						}
					}
				} catch (IllegalStateException ise) {
					logger.log(OpLevel.ERROR, StreamsResources.getStringFormatted(
							"TNTInputStream.failed.record.activity.at", getActivityPosition(), ise.getMessage()), ise);
					halt();
				} catch (Exception exc) {
					logger.log(OpLevel.ERROR, StreamsResources.getStringFormatted(
							"TNTInputStream.failed.record.activity.at", getActivityPosition(), exc.getMessage()), exc);
				}
			}
		} catch (Throwable t) {
			logger.log(OpLevel.ERROR,
					StreamsResources.getStringFormatted("TNTInputStream.failed.record.activity", t.getMessage()), t);
		} finally {
			cleanup();
			logger.log(OpLevel.INFO, StreamsResources.getStringFormatted("TNTInputStream.thread.ended",
					Thread.currentThread().getName()));
		}
	}

	private void processActivityItem(T item) throws Throwable {
		ActivityInfo ai = makeActivityInfo(item);
		if (ai == null) {
			logger.log(OpLevel.INFO, StreamsResources.getString("TNTInputStream.no.parser"));
			if (haltIfNoParser) {
				halt();
			}
		} else {
			if (!ai.isFiltered()) {
				Tracker tracker = getTracker(ai.getSource());

				while (!isHalted() && !tracker.isOpen()) {
					try {
						tracker.open();
					} catch (IOException ioe) {
						logger.log(OpLevel.ERROR,
								StreamsResources.getStringFormatted("TNTInputStream.failed.to.connect", tracker), ioe);
						Utils.close(tracker);
						logger.log(OpLevel.INFO, StreamsResources.getStringFormatted("TNTInputStream.will.retry",
								TimeUnit.MILLISECONDS.toSeconds(CONN_RETRY_INTERVAL)));
						if (!isHalted()) {
							StreamsThread.sleep(CONN_RETRY_INTERVAL);
						}
					}
				}

				ai.recordActivity(tracker, CONN_RETRY_INTERVAL);
			}
		}
	}

	private class ActivityItemProcessingTask implements Runnable {
		private T item;

		ActivityItemProcessingTask(T activityItem) {
			this.item = activityItem;
		}

		public void run() {
			try {
				processActivityItem(item);
			} catch (Throwable t) { // TODO: better handling
				logger.log(OpLevel.ERROR,
						StreamsResources.getStringFormatted("TNTInputStream.failed.record.activity", t.getMessage()),
						t);
			}
		}
	}

	private static class StreamExecutorThreadFactory implements ThreadFactory {
		private int count = 0;
		private String prefix;

		StreamExecutorThreadFactory(String pfix) {
			prefix = pfix;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread task = new Thread(r, prefix + count++);
			task.setDaemon(true);

			return task;
		}
	}

}
