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

import com.ibm.mq.MQMessage;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;

/**
 * Implements a WebSphere MQ activity stream, where activity data is {@link MQMessage} payload bytes ({@code byte[]}).
 * <p>
 * This activity stream requires parsers that can support {@code byte[]} data.
 * <p>
 * This activity stream supports properties from {@link AbstractWmqStream} (and higher hierarchy streams).
 *
 * @version $Revision: 1 $
 */
public class WmqStreamBin extends AbstractWmqStream<byte[]> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WmqStreamBin.class);

	/**
	 * Constructs an empty WmqStreamBin. Requires configuration settings to set input source.
	 */
	public WmqStreamBin() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected byte[] getActivityDataFromMessage(MQMessage mqMsg) throws Exception {
		byte[] msgData = new byte[mqMsg.getDataLength()];
		mqMsg.readFully(msgData);
		logger().log(OpLevel.TRACE,
				StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqStream.message.data"),
				msgData.length, Utils.toHexDump(msgData));
		return msgData;
	}
}
