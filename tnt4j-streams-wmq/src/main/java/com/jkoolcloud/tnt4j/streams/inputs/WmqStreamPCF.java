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

package com.jkoolcloud.tnt4j.streams.inputs;

import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.PCFConstants;
import com.ibm.mq.pcf.PCFContent;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFParameter;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;

/**
 * Implements a WebSphere MQ activity stream, where activity data is {@link MQMessage} transformed to
 * {@link PCFContent}.
 * <p>
 * This activity stream requires parsers that can support {@link PCFContent} data.
 * <p>
 * This activity stream supports properties from {@link AbstractWmqStream} (and higher hierarchy streams).
 *
 * @version $Revision: 2 $
 */
public class WmqStreamPCF extends AbstractWmqStream<PCFContent> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WmqStreamPCF.class);

	/**
	 * Constructs an empty WmqStreamPCF. Requires configuration settings to set input source.
	 */
	public WmqStreamPCF() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected PCFContent getActivityDataFromMessage(MQMessage mqMsg) throws Exception {
		PCFMessage msgData = new PCFMessage(mqMsg);
		// appending missing MQMD fields to PCF message
		handleMQMDParameters(msgData, mqMsg);

		logger().log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqStream.message.data", msgData.size(), msgData.toString());

		return msgData;
	}

	private void handleMQMDParameters(PCFMessage msgData, MQMessage mqMsg) {
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqStreamPCF.adding.mq.to.pcf");

		addParameterIfAbsent(msgData, MQConstants.MQIACF_REPORT, mqMsg.report);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_MSG_TYPE, mqMsg.messageType);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_EXPIRY, mqMsg.expiry);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_FEEDBACK, mqMsg.feedback);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_ENCODING, mqMsg.encoding);
		addParameterIfAbsent(msgData, MQConstants.MQIA_CODED_CHAR_SET_ID, mqMsg.characterSet);
		addParameterIfAbsent(msgData, MQConstants.MQCACH_FORMAT_NAME, mqMsg.format);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_PRIORITY, mqMsg.priority);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_PERSISTENCE, mqMsg.persistence);
		addParameterIfAbsent(msgData, MQConstants.MQBACF_MSG_ID, mqMsg.messageId);
		addParameterIfAbsent(msgData, MQConstants.MQBACF_CORREL_ID, mqMsg.correlationId);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_BACKOUT_COUNT, mqMsg.backoutCount);
		addParameterIfAbsent(msgData, MQConstants.MQCACF_REPLY_TO_Q, mqMsg.replyToQueueName);
		addParameterIfAbsent(msgData, MQConstants.MQCACF_REPLY_TO_Q_MGR, mqMsg.replyToQueueManagerName);
		addParameterIfAbsent(msgData, MQConstants.MQCACF_USER_IDENTIFIER, mqMsg.userId);
		addParameterIfAbsent(msgData, MQConstants.MQBACF_ACCOUNTING_TOKEN, mqMsg.accountingToken);
		addParameterIfAbsent(msgData, MQConstants.MQCACF_APPL_IDENTITY_DATA, mqMsg.applicationIdData);
		addParameterIfAbsent(msgData, MQConstants.MQIA_APPL_TYPE, mqMsg.putApplicationType);
		addParameterIfAbsent(msgData, MQConstants.MQCACF_APPL_NAME, mqMsg.putApplicationName);
		addParameterIfAbsent(msgData, MQConstants.MQCACF_APPL_ORIGIN_DATA, mqMsg.applicationOriginData);
		addParameterIfAbsent(msgData, MQConstants.MQBACF_GROUP_ID, mqMsg.groupId);
		addParameterIfAbsent(msgData, MQConstants.MQIACH_MSG_SEQUENCE_NUMBER, mqMsg.messageSequenceNumber);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_OFFSET, mqMsg.offset);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_MSG_FLAGS, mqMsg.messageFlags);
		addParameterIfAbsent(msgData, MQConstants.MQIACF_ORIGINAL_LENGTH, mqMsg.originalLength);

		// addParameterIfAbsent(msgData, MQConstants.MQCACF_PUT_DATE, mqMsg.putDateTime);
		// addParameterIfAbsent(msgData, MQConstants.MQCACF_PUT_TIME, mqMsg.putDateTime);
	}

	private void addParameterIfAbsent(PCFMessage msg, int paramId, Object val) {
		if (val == null) {
			return;
		}

		logger().log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqStreamPCF.adding.pcf.param", paramId, Utils.toString(val));

		PCFParameter paramV = msg.getParameter(paramId);
		if (paramV == null) {
			if (val instanceof Integer) {
				msg.addParameter(paramId, (int) val);
			} else if (val instanceof Long) {
				msg.addParameter(paramId, (long) val);
			} else if (val instanceof byte[]) {
				msg.addParameter(paramId, (byte[]) val);
			} else if (val instanceof int[]) {
				msg.addParameter(paramId, (int[]) val);
			} else if (val instanceof String) {
				msg.addParameter(paramId, (String) val);
			} else if (val instanceof long[]) {
				msg.addParameter(paramId, (long[]) val);
			} else if (val instanceof String[]) {
				msg.addParameter(paramId, (String[]) val);
			} else {
				throw new IllegalArgumentException(
						StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
								"WmqStreamPCF.parameter.incompatible", paramId, val.getClass().getSimpleName()));
			}
		} else {
			logger().log(OpLevel.WARNING, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqStreamPCF.parameter.exists", PCFConstants.lookupParameter(paramId), paramV.getValue(),
					Utils.toString(val));
		}
	}
}
