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

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQHeaderIterator;
import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.StreamsThread;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a WebSphere MQ activity stream, where activity data is read from
 * the specified WMQ Object (queue or topic) on the given (possibly remote)
 * queue manager.
 * </p>
 * <p>
 * This activity stream requires parsers that can support {@code String} data.
 * It currently does not strip off any WMQ headers, assuming that the message
 * data only contains the actual input for the configured parsers.
 * </p>
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>QueueManager</li>
 * <li>Queue</li>
 * <li>Topic</li>
 * <li>Subscription</li>
 * <li>TopicString</li>
 * <li>Host</li>
 * <li>Port</li>
 * <li>Channel</li>
 * <li>StripHeaders</li>
 * </ul>
 *
 * @version $Revision: 10 $
 */
public class WmqStream extends TNTInputStream<String> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WmqStream.class);

	/**
	 * Limit on number of consecutive read failures. When limit is reached,
	 * we're going to assume that there is an issue with the queue manager, or
	 * some other unrecoverable condition, and therefore close and reopen the
	 * connection.
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

	/**
	 * Construct empty WmqStream. Requires configuration settings to set input
	 * source.
	 */
	public WmqStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_QMGR_NAME.equalsIgnoreCase(name)) {
				qmgrName = value;
			} else if (StreamsConfig.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
				queueName = value;
			} else if (StreamsConfig.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
				topicName = value;
			} else if (StreamsConfig.PROP_SUB_NAME.equalsIgnoreCase(name)) {
				subName = value;
			} else if (StreamsConfig.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
				topicString = value;
			} else if (StreamsConfig.PROP_HOST.equalsIgnoreCase(name)) {
				qmgrHostName = value;
			} else if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
				qmgrPort = Integer.valueOf(value);
			} else if (StreamsConfig.PROP_CHANNEL_NAME.equalsIgnoreCase(name)) {
				qmgrChannelName = value;
			} else if (StreamsConfig.PROP_STRIP_HEADERS.equalsIgnoreCase(name)) {
				stripHeaders = Boolean.parseBoolean(value);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_QMGR_NAME.equalsIgnoreCase(name)) {
			return qmgrName;
		}
		if (StreamsConfig.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
			return queueName;
		}
		if (StreamsConfig.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicName;
		}
		if (StreamsConfig.PROP_SUB_NAME.equalsIgnoreCase(name)) {
			return subName;
		}
		if (StreamsConfig.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
			return topicString;
		}
		if (StreamsConfig.PROP_HOST.equalsIgnoreCase(name)) {
			return qmgrHostName;
		}
		if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
			return qmgrPort;
		}
		if (StreamsConfig.PROP_CHANNEL_NAME.equalsIgnoreCase(name)) {
			return qmgrChannelName;
		}
		if (StreamsConfig.PROP_STRIP_HEADERS.equalsIgnoreCase(name)) {
			return stripHeaders;
		}
		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Throwable {
		super.initialize();
		if (StringUtils.isEmpty(queueName) && StringUtils.isEmpty(topicString) && StringUtils.isEmpty(topicName)
				&& StringUtils.isEmpty(subName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted("WmqStream.must.specify.one",
					StreamsConfig.PROP_QUEUE_NAME, StreamsConfig.PROP_TOPIC_NAME, StreamsConfig.PROP_TOPIC_STRING,
					StreamsConfig.PROP_SUB_NAME));
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
	 *
	 * @return flag identifying if connected to queue manager
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
	 * Establish connection to queue manager and open necessary objects for
	 * retrieving messages
	 *
	 * @throws Throwable
	 *             if error connecting to queue manager or opening required
	 *             objects
	 */
	protected void connectToQmgr() throws Throwable {
		qmgr = null;
		dest = null;
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(CMQC.CONNECT_OPTIONS_PROPERTY, CMQC.MQCNO_HANDLE_SHARE_NONE);
		if (!StringUtils.isEmpty(qmgrHostName)) {
			props.put(CMQC.HOST_NAME_PROPERTY, qmgrHostName);
			props.put(CMQC.PORT_PROPERTY, qmgrPort);
			props.put(CMQC.CHANNEL_PROPERTY, qmgrChannelName);
		}
		if (StringUtils.isEmpty(qmgrName)) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("WmqStream.connecting.default", props));
		} else {
			LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("WmqStream.connecting.qm", qmgrName, props));
		}
		qmgr = new MQQueueManager(qmgrName, props);
		int openOptions;
		if (!StringUtils.isEmpty(topicString) || !StringUtils.isEmpty(topicName) || !StringUtils.isEmpty(subName)) {
			openOptions = CMQC.MQSO_FAIL_IF_QUIESCING | CMQC.MQSO_CREATE
					| (StringUtils.isEmpty(subName) ? CMQC.MQSO_MANAGED : CMQC.MQSO_RESUME);
			if (!StringUtils.isEmpty(subName)) {
				LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("WmqStream.subscribing.to.topic1",
						topicString, topicName, subName, String.format("%08X", openOptions))); // NON-NLS
				dest = qmgr.accessTopic(topicString, topicName, openOptions, null, subName);
			} else {
				LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("WmqStream.subscribing.to.topic2",
						topicString, topicName, String.format("%08X", openOptions))); // NON-NLS
				dest = qmgr.accessTopic(topicString, topicName, CMQC.MQTOPIC_OPEN_AS_SUBSCRIPTION, openOptions);
			}
		} else {
			openOptions = CMQC.MQOO_FAIL_IF_QUIESCING | CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_SAVE_ALL_CONTEXT;
			LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("WmqStream.opening.queue", qmgrName,
					String.format("%08X", openOptions))); // NON-NLS
			dest = qmgr.accessQueue(queueName, openOptions);
		}
		LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("WmqStream.reading.from", dest.getName().trim(),
				String.format("%08X", gmo.options))); // NON-NLS
		curFailCount = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getNextItem() throws Throwable {
		while (!isHalted() && !isConnectedToQmgr(null)) {
			try {
				connectToQmgr();
			} catch (MQException mqe) {
				if (isConnectedToQmgr(mqe)) {
					// connection to qmgr was successful, so we were not able to
					// open/subscribe
					// to required queue/topic, so exit
					LOGGER.log(OpLevel.ERROR,
							StreamsResources.getStringFormatted("WmqStream.failed.opening", formatMqException(mqe)));
					return null;
				}
				LOGGER.log(OpLevel.ERROR,
						StreamsResources.getStringFormatted("WmqStream.failed.to.connect", formatMqException(mqe)));
				LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("TNTInputStream.will.retry",
						TimeUnit.MILLISECONDS.toSeconds(QMGR_CONN_RETRY_INTERVAL)));
				if (!isHalted()) {
					StreamsThread.sleep(QMGR_CONN_RETRY_INTERVAL);
				}
			}
		}
		try {
			MQMessage mqMsg = new MQMessage();
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getStringFormatted("WmqStream.waiting.for.message", dest.getName().trim()));
			dest.get(mqMsg, gmo);
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("WmqStream.read.msg", dest.getName().trim(),
					mqMsg.getMessageLength()));
			if (stripHeaders) {
				MQHeaderIterator hdrIt = new MQHeaderIterator(mqMsg);
				hdrIt.skipHeaders();
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("WmqStream.stripped.wmq"));
			}
			String msgData = mqMsg.readStringOfByteLength(mqMsg.getDataLength());
			LOGGER.log(OpLevel.TRACE,
					StreamsResources.getStringFormatted("WmqStream.message.data", msgData.length(), msgData));
			qmgr.commit();
			curFailCount = 0;
			return msgData;
		} catch (MQException mqe) {
			curFailCount++;
			LOGGER.log(OpLevel.ERROR, StreamsResources.getStringFormatted("WmqStream.failed.reading",
					dest.getName().trim(), formatMqException(mqe)));
			if (curFailCount >= MAX_CONSECUTIVE_FAILURES) {
				LOGGER.log(OpLevel.ERROR,
						StreamsResources.getStringFormatted("WmqStream.reached.limit", MAX_CONSECUTIVE_FAILURES));
				closeQmgrConnection();
				curFailCount = 0;
			}
			throw mqe;
		}
	}

	/**
	 * Closes open objects and disconnects from queue manager.
	 */
	protected void closeQmgrConnection() {
		if (dest != null) {
			try {
				dest.close();
			} catch (MQException mqe) {
				try {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("WmqStream.error.closing"),
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
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("WmqStream.error.closing.qmgr",
							qmgr.getName(), formatMqException(mqe)));
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
	 * </p>
	 */
	@Override
	protected void cleanup() {
		closeQmgrConnection();

		super.cleanup();
	}

	/**
	 * <p>
	 * Formats display string for WMQ Exceptions.
	 * </p>
	 * <p>
	 * This implementation appends the {@code MQRC_} label for the reason code.
	 * </p>
	 *
	 * @param mqe
	 *            WMQ exception
	 *
	 * @return string identifying exception, including {@code MQRC_} constant
	 *         label
	 */
	protected String formatMqException(MQException mqe) {
		return String.format("%s (%s)", mqe, MQConstants.lookupReasonCode(mqe.getReason())); // NON-NLS
	}
}
