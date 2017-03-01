/*
 * Copyright 2014-2016 JKOOL, LLC.
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.config.TrackerConfigStore;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.sink.impl.BufferedEventSink;
import com.jkoolcloud.tnt4j.source.DefaultSourceFactory;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigManager;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsThread;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.Tracker;

/**
 * Base class for TNT4J-Streams output handler. Handles {@link Tracker} initialization, configuration and caching. Picks
 * tracker to use according streamed data source FQN and stream running {@link Thread}.
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
		TrackingLogger tracker = TrackingLogger.getInstance(trackerConfig.build());
		checkTrackerState(tracker);
		synchronized (trackersMap) {
			trackersMap.put(getTrackersMapKey(t, defaultSource.getFQName()), tracker);
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"TNTStreamOutput.default.tracker"),
					(t == null ? "null" : t.getName()), defaultSource.getFQName());
		}
	}

	private static void checkTrackerState(TrackingLogger tracker) throws IllegalStateException {
		boolean tOpen = tracker != null && tracker.isOpen();
		Tracker logger = tracker == null ? null : tracker.getTracker();
		boolean lOpen = logger != null && logger.isOpen();
		EventSink eSink = logger == null ? null : logger.getEventSink();
		boolean esOpen = eSink != null && eSink.isOpen();

		if (!tOpen || !lOpen || !esOpen) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTStreamOutput.tracker.not.opened"));
		}
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
		}
	}

	/**
	 * Gets {@link Tracker} instance from {@link #trackersMap} matching provided activity item source FQN and thread on
	 * which stream is running. If no tracker found in trackers map - new one is created.
	 *
	 * @param aiSourceFQN
	 *            activity item source FQN
	 * @param t
	 *            thread on which stream is running
	 * @return tracker instance for activity item
	 *
	 * @throws IllegalStateException
	 *             indicates that created tracker is not opened and can not record activity data
	 */
	protected Tracker getTracker(String aiSourceFQN, Thread t) throws IllegalStateException {
		synchronized (trackersMap) {
			Tracker tracker = trackersMap
					.get(getTrackersMapKey(t, aiSourceFQN == null ? defaultSource.getFQName() : aiSourceFQN));
			if (tracker == null) {
				Source aiSource = DefaultSourceFactory.getInstance().newFromFQN(aiSourceFQN);
				aiSource.setSSN(defaultSource.getSSN());
				trackerConfig.setSource(aiSource);
				tracker = TrackingLogger.getInstance(trackerConfig.build());
				checkTrackerState((TrackingLogger) tracker);
				trackersMap.put(getTrackersMapKey(t, aiSource.getFQName()), tracker);
				logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTStreamOutput.build.new.tracker"), aiSource.getFQName());
			}

			return tracker;
		}
	}

	private static String getTrackersMapKey(Thread t, String sourceFQN) {
		return (t == null ? "null" : t.getId()) + ":?:" + sourceFQN; // NON-NLS
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
		}
	}

	private void dumpTrackerStats(Tracker tracker) {
		dumpTrackerStats(tracker, tracker.getSource() == null ? "<UNKNOWN>" : tracker.getSource().getFQName()); // NON-NLS
	}

	private void dumpTrackerStats(Tracker tracker, String trackerId) {
		if (tracker == null) {
			return;
		}

		logger().log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTStreamOutput.tracker.statistics"),
				trackerId, Utils.toString(tracker.getStats()));
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
				logger().log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTStreamOutput.failed.to.open"), tracker, ioe);
				// dumpTrackerStats(tracker);
				// Utils.close(tracker);
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
			logger().log(OpLevel.INFO,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTStreamOutput.init.cfg.file"),
					StringUtils.isEmpty(cfgFilePath) ? System.getProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY)
							: cfgFilePath);
			trackerConfig = DefaultConfigFactory.getInstance().getConfig(DEFAULT_SOURCE_NAME, SourceType.APPL,
					cfgFilePath);

			applyUserTNT4JProperties();
		} else if (tnt4jCfgPath.startsWith(ZK_PREFIX)) {
			String cfgNodePath = tnt4jCfgPath.substring(ZK_PREFIX.length());
			logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTStreamOutput.zk.cfg.monitor.tnt4j"), cfgNodePath);

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
		// NOTE: removing APPL=streams "layer" from default source and copy
		// SSN=streams value from config
		trackerConfig = trackerConfig.build();
		defaultSource = trackerConfig.getSource().getSource();
		defaultSource.setSSN(trackerConfig.getSource().getSSN());
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
			logger().log(OpLevel.TRACE,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTStreamOutput.null.activity"));
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
					logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"TNTStreamOutput.retry.successful"));
				}
				return;
			} catch (IOException ioe) {
				logger().log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTStreamOutput.recording.failed"), ioe.getLocalizedMessage());
				logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTStreamOutput.recording.failed.activity"), activityData); // TODO: maybe use some formatter?
				logger().log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTStreamOutput.recording.failed.trace"), ioe);
				dumpTrackerStats(tracker);
				Utils.close(tracker);
				if (thread == null) {
					throw ioe;
				}
				retryAttempt = true;
				if (!thread.isStopRunning()) {
					logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"TNTStreamOutput.will.retry"), TimeUnit.MILLISECONDS.toSeconds(retryPeriod));
					StreamsThread.sleep(retryPeriod);
				}
			}
		} while (thread != null && !thread.isStopRunning());
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
		boolean handleErrorInternally = !(tracker.getEventSink() instanceof BufferedEventSink);

		ensureTrackerOpened(tracker);

		logJKCActivity(tracker, activityData);

		if (tracker.getEventSink().errorState() && handleErrorInternally) {
			if (tracker.getEventSink().getLastError() instanceof IOException) {
				throw (IOException) tracker.getEventSink().getLastError();
			}
		}
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
}
