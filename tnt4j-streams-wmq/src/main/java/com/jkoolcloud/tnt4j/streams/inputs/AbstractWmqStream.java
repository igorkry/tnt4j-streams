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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQHeaderIterator;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.WmqStreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsThread;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;

/**
 * Base class for WebSphere MQ activity stream, where activity data containing {@link MQMessage} is read from the
 * specified WMQ Object (queue or topic) on the given (possibly remote) queue manager.
 * <p>
 * It currently does not strip off any WMQ headers, assuming that the message data only contains the actual input for
 * the configured parsers.
 * <p>
 * This activity stream supports the following properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>QueueManager - Queue manager name. (Optional)</li>
 * <li>Queue - Queue name. (Required - at least one of 'Queue', 'Topic', 'Subscription', 'TopicString')</li>
 * <li>Topic - Topic name. (Required - at least one of 'Queue', 'Topic', 'Subscription', 'TopicString')</li>
 * <li>Subscription - Subscription name. (Required - at least one of 'Queue', 'Topic', 'Subscription',
 * 'TopicString')</li>
 * <li>TopicString - Topic string. (Required - at least one of 'Queue', 'Topic', 'Subscription', 'TopicString')</li>
 * <li>Host - WMQ connection host name. (Optional)</li>
 * <li>Port - WMQ connection port number. (Optional)</li>
 * <li>Channel - Server connection channel name. (Optional)</li>
 * <li>StripHeaders - identifies whether stream should strip WMQ message headers. (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of RAW activity data retrieved from {@link MQMessage}
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractWmqStream<T> extends TNTParseableInputStream<T> {
	/**
	 * Limit on number of consecutive read failures. When limit is reached, we're going to assume that there is an issue
	 * with the queue manager, or some other unrecoverable condition, and therefore close and reopen the connection.
	 */
	protected static final int MAX_CONSECUTIVE_FAILURES = 5;

	/**
	 * Delay between queue manager connection retries, in milliseconds.
	 */
	protected static final long QMGR_CONN_RETRY_INTERVAL = TimeUnit.SECONDS.toMillis(15);

	/**
	 * Represents Queue Manager connected to
	 */
	protected MQQueueManager qmgr = null;

	/**
	 * Represents Object (queue/topic) to read activity data messages from
	 */
	protected MQDestination dest = null;

	/**
	 * Get options used for reading messages from specified object
	 */
	protected MQGetMessageOptions gmo = null;

	/**
	 * Current count of number of successive get failures.
	 *
	 * @see #MAX_CONSECUTIVE_FAILURES
	 */
	protected int curFailCount = 0;

	// Stream properties
	private String qmgrName = null;
	private String queueName = null;
	private String topicName = null;
	private String subName = null;
	private String topicString = null;
	private String qmgrHostName = null;
	private int qmgrPort = 1414;
	private String qmgrChannelName = "SYSTEM.DEF.SVRCONN"; // NON-NLS
	private boolean stripHeaders = true;

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (WmqStreamProperties.PROP_QMGR_NAME.equalsIgnoreCase(name)) {
				qmgrName = value;
			} else if (WmqStreamProperties.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
				queueName = value;
			} else if (WmqStreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
				topicName = value;
			} else if (WmqStreamProperties.PROP_SUB_NAME.equalsIgnoreCase(name)) {
				subName = value;
			} else if (WmqStreamProperties.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
				topicString = value;
			} else if (WmqStreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
				qmgrHostName = value;
			} else if (WmqStreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
				qmgrPort = Integer.valueOf(value);
			} else if (WmqStreamProperties.PROP_CHANNEL_NAME.equalsIgnoreCase(name)) {
				qmgrChannelName = value;
			} else if (WmqStreamProperties.PROP_STRIP_HEADERS.equalsIgnoreCase(name)) {
				stripHeaders = Boolean.parseBoolean(value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WmqStreamProperties.PROP_QMGR_NAME.equalsIgnoreCase(name)) {
			return qmgrName;
		}
		if (WmqStreamProperties.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
			return queueName;
		}
		if (WmqStreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicName;
		}
		if (WmqStreamProperties.PROP_SUB_NAME.equalsIgnoreCase(name)) {
			return subName;
		}
		if (WmqStreamProperties.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
			return topicString;
		}
		if (WmqStreamProperties.PROP_HOST.equalsIgnoreCase(name)) {
			return qmgrHostName;
		}
		if (WmqStreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return qmgrPort;
		}
		if (WmqStreamProperties.PROP_CHANNEL_NAME.equalsIgnoreCase(name)) {
			return qmgrChannelName;
		}
		if (WmqStreamProperties.PROP_STRIP_HEADERS.equalsIgnoreCase(name)) {
			return stripHeaders;
		}

		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (StringUtils.isEmpty(queueName) && StringUtils.isEmpty(topicString) && StringUtils.isEmpty(topicName)
				&& StringUtils.isEmpty(subName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqStream.must.specify.one", StreamProperties.PROP_QUEUE_NAME, StreamProperties.PROP_TOPIC_NAME,
					StreamProperties.PROP_TOPIC_STRING, WmqStreamProperties.PROP_SUB_NAME));
		}
		// Prevents WMQ library from writing exceptions to stderr
		MQException.log = null;
		gmo = new MQGetMessageOptions();
		gmo.waitInterval = CMQC.MQWI_UNLIMITED;
		gmo.options &= ~CMQC.MQGMO_NO_SYNCPOINT;
		gmo.options |= CMQC.MQGMO_SYNCPOINT | CMQC.MQGMO_WAIT;
	}

	/**
	 * Checks if connection to queue manager is opened.
	 *
	 * @param mqe
	 *            MQ exception object
	 * @return flag indicating if connected to queue manager
	 */
	protected boolean isConnectedToQmgr(MQException mqe) {
		if (qmgr == null || !qmgr.isConnected()) {
			return false;
		}
		if (mqe != null && mqe.getCompCode() == MQConstants.MQCC_FAILED) {
			switch (mqe.getReason()) {
			case MQConstants.MQRC_CONNECTION_BROKEN:
			case MQConstants.MQRC_CONNECTION_ERROR:
			case MQConstants.MQRC_Q_MGR_NOT_ACTIVE:
			case MQConstants.MQRC_Q_MGR_NOT_AVAILABLE:
			case MQConstants.MQRC_Q_MGR_QUIESCING:
			case MQConstants.MQRC_Q_MGR_STOPPING:
			case MQConstants.MQRC_CONNECTION_QUIESCING:
			case MQConstants.MQRC_CONNECTION_STOPPING:
				return false;
			default:
				break;
			}
		}
		return true;
	}

	/**
	 * Establish connection to queue manager and open necessary objects for retrieving messages
	 *
	 * @throws Exception
	 *             if exception occurs connecting to queue manager or opening required objects
	 */
	protected void connectToQmgr() throws Exception {
		qmgr = null;
		dest = null;
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(CMQC.CONNECT_OPTIONS_PROPERTY, CMQC.MQCNO_HANDLE_SHARE_NONE);
		if (StringUtils.isNotEmpty(qmgrHostName)) {
			props.put(CMQC.HOST_NAME_PROPERTY, qmgrHostName);
			props.put(CMQC.PORT_PROPERTY, qmgrPort);
			props.put(CMQC.CHANNEL_PROPERTY, qmgrChannelName);
		}
		if (StringUtils.isEmpty(qmgrName)) {
			logger().log(OpLevel.INFO,
					StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStream.connecting.default"),
					props);
		} else {
			logger().log(OpLevel.INFO,
					StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStream.connecting.qm"),
					qmgrName, props);
		}
		qmgr = new MQQueueManager(qmgrName, props);
		int openOptions;
		if (StringUtils.isNotEmpty(topicString) || StringUtils.isNotEmpty(topicName)
				|| StringUtils.isNotEmpty(subName)) {
			openOptions = CMQC.MQSO_FAIL_IF_QUIESCING | CMQC.MQSO_CREATE
					| (StringUtils.isEmpty(subName) ? CMQC.MQSO_MANAGED : CMQC.MQSO_RESUME);
			if (StringUtils.isNotEmpty(subName)) {
				logger().log(OpLevel.INFO,
						StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
								"WmqStream.subscribing.to.topic1"),
						topicString, topicName, subName, String.format("%08X", openOptions)); // NON-NLS
				dest = qmgr.accessTopic(topicString, topicName, openOptions, null, subName);
			} else {
				logger().log(OpLevel.INFO,
						StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
								"WmqStream.subscribing.to.topic2"),
						topicString, topicName, String.format("%08X", openOptions)); // NON-NLS
				dest = qmgr.accessTopic(topicString, topicName, CMQC.MQTOPIC_OPEN_AS_SUBSCRIPTION, openOptions);
			}
		} else {
			openOptions = CMQC.MQOO_FAIL_IF_QUIESCING | CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_SAVE_ALL_CONTEXT
					| CMQC.MQOO_INQUIRE;
			logger().log(OpLevel.INFO,
					StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStream.opening.queue"),
					qmgrName, String.format("%08X", openOptions)); // NON-NLS
			dest = qmgr.accessQueue(queueName, openOptions);
		}
		logger().log(OpLevel.INFO,
				StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStream.reading.from"),
				dest.getName().trim(), String.format("%08X", gmo.options)); // NON-NLS
		curFailCount = 0;
	}

	@Override
	public T getNextItem() throws Exception {
		while (!isHalted() && !isConnectedToQmgr(null)) {
			try {
				connectToQmgr();
			} catch (MQException mqe) {
				if (isConnectedToQmgr(mqe)) {
					// connection to qmgr was successful, so we were not able to open/subscribe to required queue/topic,
					// so exit
					logger().log(OpLevel.ERROR, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
							"WmqStream.failed.opening"), formatMqException(mqe));
					return null;
				}
				logger().log(OpLevel.ERROR, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
						"WmqStream.failed.to.connect"), formatMqException(mqe));
				logger().log(OpLevel.INFO,
						StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
								"TNTInputStream.will.retry"),
						TimeUnit.MILLISECONDS.toSeconds(QMGR_CONN_RETRY_INTERVAL));
				if (!isHalted()) {
					StreamsThread.sleep(QMGR_CONN_RETRY_INTERVAL);
				}
			}
		}
		try {
			MQMessage mqMsg = new MQMessage();
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqStream.waiting.for.message"), dest.getName().trim());
			dest.get(mqMsg, gmo);
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStream.read.msg"),
					dest.getName().trim(), mqMsg.getMessageLength());
			// TODO: MQCFH mqcfh = new MQCFH(mqMsg); mqcfh.control != MQConstants.MQCFC_LAST;
			if (stripHeaders) {
				MQHeaderIterator hdrIt = new MQHeaderIterator(mqMsg);
				hdrIt.skipHeaders();
				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStream.stripped.wmq"));
			}
			T msgData = getActivityDataFromMessage(mqMsg);
			qmgr.commit();
			curFailCount = 0;
			addStreamedBytesCount(mqMsg.getMessageLength());
			// logger().log(OpLevel.DEBUG, "QUEUE {0} DEPTH: {1}", queueName, ((MQQueue) dest).getCurrentDepth());
			return msgData;
		} catch (MQException mqe) {
			curFailCount++;
			logger().log(OpLevel.ERROR,
					StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStream.failed.reading"),
					dest.getName().trim(), formatMqException(mqe));
			if (curFailCount >= MAX_CONSECUTIVE_FAILURES) {
				logger().log(OpLevel.ERROR,
						StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStream.reached.limit"),
						MAX_CONSECUTIVE_FAILURES);
				closeQmgrConnection();
				curFailCount = 0;
			}
			throw mqe;
		}
	}

	/**
	 * Gets RAW activity data from provided {@link MQMessage}.
	 *
	 * @param message
	 *            MQ message to get RAW activity data
	 * @return RAW activity data retrieved from MQ message
	 * @throws Exception
	 *             if any errors occurred getting RAW activity data from MQ message
	 */
	protected abstract T getActivityDataFromMessage(MQMessage message) throws Exception;

	/**
	 * Closes open objects and disconnects from queue manager.
	 */
	protected void closeQmgrConnection() {
		if (dest != null) {
			try {
				dest.close();
			} catch (MQException mqe) {
				try {
					logger().log(OpLevel.DEBUG,
							StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
									"WmqStream.error.closing"),
							dest.getClass().getName(), dest.getName(), formatMqException(mqe));
				} catch (MQException e) {
				}
			}
			dest = null;
		}
		if (qmgr != null) {
			try {
				qmgr.disconnect();
			} catch (MQException mqe) {
				try {
					logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
							"WmqStream.error.closing.qmgr"), qmgr.getName(), formatMqException(mqe));
				} catch (MQException e) {
				}
			}
			qmgr = null;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Closes open objects and disconnects from queue manager.
	 */
	@Override
	protected void cleanup() {
		closeQmgrConnection();

		super.cleanup();
	}

	/**
	 * Formats display string for WMQ Exceptions.
	 * <p>
	 * This implementation appends the {@code MQRC_} label for the reason code.
	 *
	 * @param mqe
	 *            WMQ exception
	 * @return string identifying exception, including {@code MQRC_} constant label
	 */
	protected static String formatMqException(MQException mqe) {
		return String.format("%s (%s)", mqe, MQConstants.lookupReasonCode(mqe.getReason())); // NON-NLS
	}
}
