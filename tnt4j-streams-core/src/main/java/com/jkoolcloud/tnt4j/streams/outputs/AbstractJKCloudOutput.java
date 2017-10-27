/*
 * Copyright 2014-2017 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.outputs;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.config.TrackerConfigStore;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.sink.impl.BufferedEventSink;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigManager;
import com.jkoolcloud.tnt4j.streams.inputs.InputStreamListener;
import com.jkoolcloud.tnt4j.streams.inputs.StreamStatus;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsThread;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * Base class for TNT4J-Streams output handler. Handles {@link Tracker} initialization, configuration and caching. Picks
 * tracker to use according streamed data source FQN and stream running {@link Thread}.
 * <p>
 * This output supports the following configuration properties:
 * <ul>
 * <li>TNT4JConfigFile - path of 'tnt4j.properties' file. May be used to override default TNT4J system property
 * 'tnt4j.config' defined value. Default value - '{@code null}'. (Optional)</li>
 * <li>TNT4JProperty - defines specific TNT4J configuration properties to be used by stream output. (Optional)</li>
 * <li>TNT4JConfigZKNode - defines ZooKeeper path where stream configuration is located. Default value - ''.
 * (Optional)</li>
 * <li>RetryStateCheck - flag indicating whether tracker state check should be perform repeatedly. If {@code false},
 * then streaming process exits with {@link java.lang.IllegalStateException}. Default value - {@code false}.
 * (Optional)</li>
 * <li>SendStreamStates - flag indicating whether to send stream status change messages (`startup`/`shutdown`) to output
 * endpoint e.g. 'JKoolCloud'. Default value - {@code true}. (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of incoming activity data from stream
 * @param <O>
 *            the type of outgoing activity data package to be sent to JKool Cloud
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractJKCloudOutput<T, O> implements TNTStreamOutput<T> {

	private static final String DEFAULT_SOURCE_NAME = "com.jkoolcloud.tnt4j.streams"; // NON-NLS
	/**
	 * Delay between retries to submit data package to JKool Cloud if some transmission failure occurs, in milliseconds.
	 */
	protected static final long CONN_RETRY_INTERVAL = TimeUnit.SECONDS.toMillis(10);

	private static final String FILE_PREFIX = "file://"; // NON-NLS
	private static final String ZK_PREFIX = "zk://"; // NON-NLS

	/**
	 * Used to deliver processed activity data to destination.
	 */
	private final Map<String, Tracker> trackersMap = new HashMap<>();

	private TrackerConfig trackerConfig;
	private Source defaultSource;
	private String tnt4jCfgPath;
	private Map<String, String> tnt4jProperties;

	private TNTInputStream<?, ?> stream;
	private boolean retryStateCheck = false;

	private boolean closed = false;
	private boolean sendStreamStates = true;
	private JKoolNotificationListener jKoolNotificationListener = new JKoolNotificationListener();

	/**
	 * Constructs a new AbstractJKCloudOutput.
	 */
	protected AbstractJKCloudOutput() {
	}

	/**
	 * Returns logger used by this stream output handler.
	 *
	 * @return parser logger
	 */
	protected abstract EventSink logger();

	@Override
	public void setStream(TNTInputStream<?, ?> inputStream) {
		this.stream = inputStream;

		if (sendStreamStates) {
			stream.addStreamListener(jKoolNotificationListener);
		}
	}

	@Override
	public TNTInputStream<?, ?> getStream() {
		return stream;
	}

	/**
	 * Gets path string of TNT4J configuration resource: file, ZooKeeper node.
	 *
	 * @return returns path of TNT4J configuration resource
	 */
	public String getTnt4jCfgPath() {
		return tnt4jCfgPath;
	}

	/**
	 * Sets path string of TNT4J configuration resource: file, ZooKeeper node.
	 *
	 * @param tnt4jCfgPath
	 *            path of TNT4J configuration resource
	 */
	public void setTnt4jCfgPath(String tnt4jCfgPath) {
		this.tnt4jCfgPath = tnt4jCfgPath;
	}

	/**
	 * Adds TNT4J configuration property specific for this stream.
	 *
	 * @param key
	 *            property key
	 * @param value
	 *            property value
	 * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for
	 *         <tt>key</tt>.
	 */
	public String addTNT4JProperty(String key, String value) {
		if (tnt4jProperties == null) {
			tnt4jProperties = new HashMap<>();
		}

		return tnt4jProperties.put(key, value);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Loads {@link com.jkoolcloud.tnt4j.config.TrackerConfig} and setups default
	 * {@link com.jkoolcloud.tnt4j.source.Source}.
	 */
	@Override
	public void initialize() throws Exception {
		initializeTNT4JConfig();

		setupDefaultSource();
	}

	@Override
	public void handleConsumerThread(Thread t) throws IllegalStateException {
		getTracker(t);
	}

	private static void checkTrackerState(Tracker tracker) throws IllegalStateException {
		boolean tOpen = tracker != null && tracker.isOpen();
		if (!tOpen) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTStreamOutput.tracker.not.opened"));
		}
	}

	private void checkTracker(Tracker tracker) throws IllegalArgumentException {
		if (retryStateCheck) {
			checkTrackerWithRetry(tracker, CONN_RETRY_INTERVAL);
		} else {
			checkTrackerState(tracker);
		}
	}

	private void checkTrackerWithRetry(Tracker tracker, long retryPeriod) throws IllegalStateException {
		StreamsThread thread = null;
		if (Thread.currentThread() instanceof StreamsThread) {
			thread = (StreamsThread) Thread.currentThread();
		}

		boolean retryAttempt = false;
		do {
			try {
				checkTrackerState(tracker);

				if (retryAttempt) {
					logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"TNTStreamOutput.check.retry.successful");
				}

				return;
			} catch (IllegalStateException ise) {
				logger().log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTStreamOutput.check.failed", ise.getLocalizedMessage());
				logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTStreamOutput.check.failed.trace", ise);
				// resetTracker(tracker);
				if (thread == null) {
					throw ise;
				}
				retryAttempt = true;
				if (!thread.isStopRunning()) {
					logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"TNTStreamOutput.check.will.retry", TimeUnit.MILLISECONDS.toSeconds(retryPeriod));
					StreamsThread.sleep(retryPeriod);
				}
			}
		} while (thread != null && !thread.isStopRunning());
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setProperty(String name, Object value) {
		if (OutputProperties.PROP_TNT4J_CONFIG_FILE.equalsIgnoreCase(name)) {
			String path = (String) value;
			if (StringUtils.isNotEmpty(path) && !path.startsWith(FILE_PREFIX)) {
				path = FILE_PREFIX + path;
			}
			setTnt4jCfgPath(path);
		} else if (OutputProperties.PROP_TNT4J_PROPERTY.equalsIgnoreCase(name)) {
			Map.Entry<String, String> p = (Map.Entry<String, String>) value;
			addTNT4JProperty(p.getKey(), p.getValue());
		} else if (OutputProperties.PROP_TNT4J_CONFIG_ZK_NODE.equalsIgnoreCase(name)) {
			String path = (String) value;
			if (StringUtils.isNotEmpty(path) && !path.startsWith(ZK_PREFIX)) {
				path = ZK_PREFIX + path;
			}
			setTnt4jCfgPath(path);
		} else if (OutputProperties.PROP_RETRY_STATE_CHECK.equalsIgnoreCase(name)) {
			retryStateCheck = Boolean.parseBoolean((String) value);
		} else if (OutputProperties.PROP_SEND_STREAM_STATES.equalsIgnoreCase(name)) {
			sendStreamStates = Boolean.parseBoolean((String) value);

			if (stream != null) {
				if (sendStreamStates) {
					stream.addStreamListener(jKoolNotificationListener);
				} else {
					stream.removeStreamListener(jKoolNotificationListener);
				}
			}
		}
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				setProperty(prop.getKey(), prop.getValue());
			}
		}
	}

	/**
	 * Gets {@link Tracker} instance from {@link #trackersMap} matching {@link Thread#currentThread()} on which stream
	 * output is running. If no tracker found in trackers map - new one is created.
	 *
	 * @return tracker instance for activity item
	 *
	 * @throws IllegalStateException
	 *             indicates that created tracker is not opened and can not record activity data
	 *
	 * @see #getTracker(Thread)
	 */
	protected Tracker getTracker() throws IllegalStateException {
		return getTracker(Thread.currentThread());
	}

	/**
	 * Gets {@link Tracker} instance from {@link #trackersMap} matching provided thread on which stream output is
	 * running. If no tracker found in trackers map - new one is created.
	 *
	 * @param t
	 *            thread on which stream is running
	 * @return tracker instance for activity item
	 *
	 * @throws IllegalStateException
	 *             indicates that created tracker is not opened and can not record activity data
	 */
	protected Tracker getTracker(Thread t) throws IllegalStateException {
		synchronized (trackersMap) {
			Tracker tracker = trackersMap.get(getTrackersMapKey(t));
			if (tracker == null) {
				tracker = TrackingLogger.getInstance(trackerConfig.build());
				checkTracker(tracker);
				trackersMap.put(getTrackersMapKey(t), tracker);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTStreamOutput.built.new.tracker", (t == null ? "null" : t.getId()));
			}

			return tracker;
		}
	}

	private static String getTrackersMapKey(Thread t) {
		return t == null ? "null" : String.valueOf(t.getId()); // NON-NLS
	}

	@Override
	public void cleanup() {
		synchronized (trackersMap) {
			if (!trackersMap.isEmpty()) {
				for (Map.Entry<String, Tracker> te : trackersMap.entrySet()) {
					Tracker tracker = te.getValue();
					dumpTrackerStats(tracker, te.getKey());
					Utils.close(tracker);
				}

				trackersMap.clear();
			}
			closed = true;
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	// protected void closeTracker(String aiSourceFQN, Tracker tracker) {
	// String trackerId = getTrackersMapKey(Thread.currentThread(), aiSourceFQN);
	// synchronized (trackersMap) {
	// trackersMap.remove(trackerId);
	// }
	// dumpTrackerStats(tracker, trackerId);
	// Utils.close(tracker);
	// }

	protected void dumpTrackerStats(Tracker tracker) {
		dumpTrackerStats(tracker, tracker.getSource() == null ? "<UNKNOWN>" : tracker.getSource().getFQName()); // NON-NLS
	}

	protected void dumpTrackerStats(Tracker tracker, String trackerId) {
		if (tracker == null) {
			return;
		}

		logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"TNTStreamOutput.tracker.statistics", trackerId, Utils.toString(tracker.getStats()));
	}

	/**
	 * Checks stream and provided {@link Tracker} states to allow data streaming and opens tracker if it is not open.
	 *
	 * @param tracker
	 *            tracker instance to check state and to open if it is not open
	 * @throws java.io.IOException
	 *             if error opening tracker handle
	 *
	 * @see #sendActivity(com.jkoolcloud.tnt4j.tracker.Tracker, Object)
	 */
	protected void ensureTrackerOpened(Tracker tracker) throws IOException {
		if (!tracker.isOpen()) {
			try {
				tracker.open();
			} catch (IOException ioe) {
				logger().log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTStreamOutput.failed.to.open", tracker, ioe);
				throw ioe;
			}
		}
	}

	/**
	 * Performs initialization of tracker configuration using provided resource: file, ZooKeeper node.
	 */
	protected void initializeTNT4JConfig() throws IOException, InterruptedException {
		if (StringUtils.isEmpty(tnt4jCfgPath) || tnt4jCfgPath.startsWith(FILE_PREFIX)) {
			String cfgFilePath = StringUtils.isEmpty(tnt4jCfgPath) ? tnt4jCfgPath
					: tnt4jCfgPath.substring(FILE_PREFIX.length());
			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTStreamOutput.init.cfg.file", StringUtils.isEmpty(cfgFilePath)
							? System.getProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY) : cfgFilePath);
			trackerConfig = DefaultConfigFactory.getInstance().getConfig(DEFAULT_SOURCE_NAME, SourceType.APPL,
					cfgFilePath);

			applyUserTNT4JProperties();
		} else if (tnt4jCfgPath.startsWith(ZK_PREFIX)) {
			String cfgNodePath = tnt4jCfgPath.substring(ZK_PREFIX.length());
			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTStreamOutput.zk.cfg.monitor.tnt4j", cfgNodePath);

			ZKConfigManager.handleZKStoredConfiguration(cfgNodePath, new ZKConfigManager.ZKConfigChangeListener() {
				@Override
				public void applyConfigurationData(byte[] data) {
					reconfigureTNT4J(Utils.bytesReader(data));
				}
			});
		}

		// TODO: add TNT4J-Kafka configuration handling.
	}

	private void reconfigureTNT4J(Reader cfgReader) {
		if (trackerConfig == null) {
			trackerConfig = DefaultConfigFactory.getInstance().getConfig(DEFAULT_SOURCE_NAME, SourceType.APPL,
					cfgReader);

			applyUserTNT4JProperties();
		} else {
			TrackerConfigStore tcs = (TrackerConfigStore) trackerConfig;
			tcs.initConfig(cfgReader);

			applyUserTNT4JProperties();

			cleanup();
		}

		setupDefaultSource();
	}

	private void applyUserTNT4JProperties() {
		if (MapUtils.isNotEmpty(tnt4jProperties)) {
			for (Map.Entry<String, String> tnt4jProp : tnt4jProperties.entrySet()) {
				trackerConfig.setProperty(tnt4jProp.getKey(), tnt4jProp.getValue());
			}

			((TrackerConfigStore) trackerConfig).applyProperties();
		}
	}

	/**
	 * Performs default {@link com.jkoolcloud.tnt4j.source.Source} initialization and setup.
	 */
	protected void setupDefaultSource() {
		trackerConfig = trackerConfig.build();
		defaultSource = trackerConfig.getSource();
		// NOTE: removing APPL=streams "layer" from default source and copy SSN=streams value from config
		if (defaultSource.getSource() != null) {
			defaultSource = defaultSource.getSource();
		}
		defaultSource.setSSN(trackerConfig.getSource().getSSN());

		closed = false;
	}

	/**
	 * Sends activity data package to JKool Cloud using the specified tracker. Performs resend after defined period of
	 * time if initial sending fails.
	 * 
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param retryPeriod
	 *            period in milliseconds between activity resubmission in case of failure
	 * @param activityData
	 *            activity data to send
	 * @throws Exception
	 *             indicates an error when sending activity data to JKool Cloud
	 */
	protected void recordActivity(Tracker tracker, long retryPeriod, O activityData) throws Exception {
		if (tracker == null) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.tracker.null"));
		}

		if (activityData == null) {
			logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTStreamOutput.null.activity");
			return;
		}

		StreamsThread thread = null;
		if (Thread.currentThread() instanceof StreamsThread) {
			thread = (StreamsThread) Thread.currentThread();
		}

		boolean retryAttempt = false;
		do {
			try {
				sendActivity(tracker, activityData);

				if (retryAttempt) {
					logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"TNTStreamOutput.retry.successful");
				}
				return;
			} catch (IOException ioe) {
				logger().log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTStreamOutput.recording.failed", ioe.getLocalizedMessage());
				logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTStreamOutput.recording.failed.activity", activityData); // TODO: maybe use some formatter?
				logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"TNTStreamOutput.recording.failed.trace", ioe);
				resetTracker(tracker);
				if (thread == null) {
					throw ioe;
				}
				retryAttempt = true;
				if (!thread.isStopRunning()) {
					logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"TNTStreamOutput.will.retry", TimeUnit.MILLISECONDS.toSeconds(retryPeriod));
					StreamsThread.sleep(retryPeriod);
				}
			}
		} while (thread != null && !thread.isStopRunning());
	}

	private static void resetTracker(Tracker tracker) throws IOException {
		EventSink eSink = tracker.getEventSink();

		if (eSink != null) {
			eSink.reopen();
		}
	}

	/**
	 * Performs activity data package sending (logging) to JKool Cloud and checks tracker event sink state if there was
	 * any communication errors.
	 * 
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param activityData
	 *            activity data to send
	 * @throws IOException
	 *             if communication with JKool Cloud fails
	 *
	 * @see #ensureTrackerOpened(com.jkoolcloud.tnt4j.tracker.Tracker)
	 * @see #logJKCActivity(com.jkoolcloud.tnt4j.tracker.Tracker, Object)
	 */
	protected void sendActivity(Tracker tracker, O activityData) throws IOException {
		EventSink eSink = tracker.getEventSink();
		boolean handleErrorInternally = !(eSink instanceof BufferedEventSink);
		long dropCountBefore = handleErrorInternally
				? (long) tracker.getStats().get(Utils.qualify(getTrackerImpl(tracker), Tracker.KEY_DROP_COUNT)) : -1;

		ensureTrackerOpened(tracker);

		logJKCActivity(tracker, activityData);

		// TODO: review TNT4J API to get occurred exception, complete statistics fetching is not best way...
		if (handleErrorInternally) {
			if (eSink.errorState()) {
				if (eSink.getLastError() instanceof IOException) {
					throw (IOException) eSink.getLastError();
				}
			}

			long dropCountAfter = (long) tracker.getStats()
					.get(Utils.qualify(getTrackerImpl(tracker), Tracker.KEY_DROP_COUNT));

			if (dropCountBefore >= 0 && dropCountAfter > dropCountBefore) {
				throw new IOException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTStreamOutput.tracker.drop.detected"));
			}
		}
	}

	private static Tracker getTrackerImpl(Tracker tracker) {
		return tracker instanceof TrackingLogger ? ((TrackingLogger) tracker).getTracker() : tracker;
	}

	/**
	 * Logs given activity data using provided tracker to communicate JKool Cloud.
	 * 
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param activityData
	 *            activity data to send
	 */
	protected abstract void logJKCActivity(Tracker tracker, O activityData);

	/**
	 * Sends stream startup ("welcome") or shutdown message.
	 *
	 * @param status
	 *            stream status
	 */
	protected void sendStreamStateMessage(StreamStatus status) {
		Tracker tracker = getTracker();
		TrackingEvent sMsgEvent;
		if (status == StreamStatus.STARTED) {
			sMsgEvent = tracker.newEvent(OpLevel.INFO, OpType.START, "Streaming-session-start-event", // NON-NLS
					(String) null, "STREAM_START", // NON-NLS
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTStreamOutput.status.msg"),
					stream.getName(), status);
		} else {
			sMsgEvent = tracker.newEvent(OpLevel.INFO, OpType.STOP, "Streaming-session-shutdown-event", // NON-NLS
					(String) null, "STREAM_SHUTDOWN", // NON-NLS
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTStreamOutput.status.msg"),
					stream.getName(), status);
		}
		sMsgEvent.setSource(defaultSource);

		O wMsg = formatStreamStatusMessage(sMsgEvent);

		try {
			recordActivity(tracker, CONN_RETRY_INTERVAL, wMsg);
			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTStreamOutput.status.msg.sent", status);
		} catch (Exception exc) {
			logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"TNTStreamOutput.status.msg.failed", status);
		}
	}

	/**
	 * Formats stream status message to be output compliant format.
	 *
	 * @param statusMessage
	 *            stream status message
	 * @return output compliant status message
	 */
	protected abstract O formatStreamStatusMessage(TrackingEvent statusMessage);

	private class JKoolNotificationListener implements InputStreamListener {
		@Override
		public void onProgressUpdate(TNTInputStream<?, ?> stream, int current, int total) {
		}

		@Override
		public void onSuccess(TNTInputStream<?, ?> stream) {
		}

		@Override
		public void onFailure(TNTInputStream<?, ?> stream, String msg, Throwable exc, String code) {
		}

		@Override
		public void onStatusChange(TNTInputStream<?, ?> stream, StreamStatus status) {
			if (status == null) {
				return;
			}

			switch (status) {
			case STARTED:
			case FAILURE:
			case SUCCESS:
			case STOP:
				if (!isClosed()) {
					sendStreamStateMessage(status);
				}
				break;
			default:
				break;
			}
		}

		@Override
		public void onFinish(TNTInputStream<?, ?> stream, TNTInputStream.StreamStats stats) {
		}

		@Override
		public void onStreamEvent(TNTInputStream<?, ?> stream, OpLevel level, String message, Object source) {
		}
	}
}
