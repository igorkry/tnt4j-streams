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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.MapUtils;

import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.config.TrackerConfigStore;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.source.DefaultSourceFactory;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsThread;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.Tracker;

/**
 * Base class for This interface defines operations commonly used by TNT4J-Streams outputs .
 *
 * @param <T>
 *            the type of handled activity data
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractJKCloudOutput<T> implements TNTOutput<T> {

	private static final String DEFAULT_SOURCE_NAME = "com.jkoolcloud.tnt4j.streams"; // NON-NLS
	/**
	 * Delay between retries to submit data package to jKool Cloud Service if some transmission failure occurs, in
	 * milliseconds.
	 */
	protected static final long CONN_RETRY_INTERVAL = TimeUnit.SECONDS.toMillis(15);

	private final EventSink logger;

	/**
	 * Used to deliver processed activity data to destination.
	 */
	private final Map<String, Tracker> trackersMap = new HashMap<String, Tracker>();

	private TrackerConfig streamConfig;
	private Source defaultSource;
	private String tnt4jCfgFilePath;
	private Map<String, String> tnt4jProperties;

	private TNTInputStream<?, ?> stream;

	/**
	 * Constructs a new AbstractJKCloudOutput.
	 *
	 * @param logger
	 *            logger used by stream output handler
	 */
	protected AbstractJKCloudOutput(EventSink logger) {
		this.logger = logger;
	}

	@Override
	public void setStream(TNTInputStream<?, ?> inputStream) {
		this.stream = inputStream;
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
	 * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for
	 *         <tt>key</tt>.
	 */
	public String addTNT4JProperty(String key, String value) {
		if (tnt4jProperties == null) {
			tnt4jProperties = new HashMap<String, String>();
		}

		return tnt4jProperties.put(key, value);
	}

	@Override
	public void initialize() throws Exception {
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
	}

	@Override
	public void handleConsumerThread(Thread t) {
		Tracker tracker = TrackingLogger.getInstance(streamConfig.build());
		trackersMap.put(getTrackersMapKey(t, defaultSource.getFQName()), tracker);
		logger.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.default.tracker"),
				(t == null ? "null" : t.getName()), defaultSource.getFQName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setProperty(String name, Object value) {
		if (OutputProperties.PROP_TNT4J_CONFIG_FILE.equalsIgnoreCase(name)) {
			setTnt4jCfgFilePath((String) value);
		} else if (OutputProperties.PROP_TNT4J_PROPERTY.equalsIgnoreCase(name)) {
			Map.Entry<String, String> p = (Map.Entry<String, String>) value;
			addTNT4JProperty(p.getKey(), p.getValue());
		}
	}

	/**
	 * Gets {@link Tracker} instance from {@link #trackersMap} matching provided activity item source FQN and thread on
	 * which stream is running.
	 *
	 * @param aiSourceFQN
	 *            activity item source FQN
	 * @param t
	 *            thread on which stream is running
	 * @return tracker instance for activity item
	 */
	protected Tracker getTracker(String aiSourceFQN, Thread t) {
		synchronized (trackersMap) {
			Tracker tracker = trackersMap
					.get(getTrackersMapKey(t, aiSourceFQN == null ? defaultSource.getFQName() : aiSourceFQN));
			if (tracker == null) {
				Source aiSource = DefaultSourceFactory.getInstance().newFromFQN(aiSourceFQN);
				aiSource.setSSN(defaultSource.getSSN());
				streamConfig.setSource(aiSource);
				tracker = TrackingLogger.getInstance(streamConfig.build());
				trackersMap.put(getTrackersMapKey(t, aiSource.getFQName()), tracker);
				logger.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTInputStream.build.new.tracker"), aiSource.getFQName());
			}

			return tracker;
		}
	}

	private static String getTrackersMapKey(Thread t, String sourceFQN) {
		return (t == null ? "null" : t.getId()) + ":?:" + sourceFQN; // NON-NLS
	}

	@Override
	public void cleanup() {
		if (!trackersMap.isEmpty()) {
			for (Map.Entry<String, Tracker> te : trackersMap.entrySet()) {
				Tracker tracker = te.getValue();
				Utils.close(tracker);
			}

			trackersMap.clear();
		}
	}

	/**
	 * Checks stream and provided {@link Tracker} states to allow data streaming and opens tracker if it is in not open.
	 * 
	 * @param tracker
	 *            tracker instance to check state and to open if it is not open
	 */
	protected void ensureTrackerOpened(Tracker tracker) {
		while (!stream.isHalted() && !tracker.isOpen()) {
			try {
				tracker.open();
			} catch (IOException ioe) {
				logger.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTInputStream.failed.to.connect"), tracker, ioe);
				Utils.close(tracker);
				logger.log(OpLevel.INFO,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.will.retry"),
						TimeUnit.MILLISECONDS.toSeconds(CONN_RETRY_INTERVAL));
				if (!stream.isHalted()) {
					StreamsThread.sleep(CONN_RETRY_INTERVAL);
				}
			}
		}
	}
}
