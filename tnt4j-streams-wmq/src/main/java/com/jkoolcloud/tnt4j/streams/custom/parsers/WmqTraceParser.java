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

package com.jkoolcloud.tnt4j.streams.custom.parsers;

import com.ibm.mq.pcf.MQCFGR;
import com.ibm.mq.pcf.PCFMessage;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityPCFParser;

/**
 * Implements an activity data parser that assumes each activity data item is an IBM MQ {@link PCFMessage} activity
 * trace parameters group ({@link MQCFGR} having parameter value {@code MQGACF_ACTIVITY_TRACE}).
 * <p>
 * Now it does all the same as {@link ActivityPCFParser} and left only for backward compatibility.
 * <p>
 * This parser supports configuration properties from {@link ActivityPCFParser} (and higher hierarchy parsers).
 *
 * @version $Revision: 2 $
 * @deprecated use {@link ActivityPCFParser} instead.
 */
@Deprecated
public class WmqTraceParser extends ActivityPCFParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WmqTraceParser.class);

	/**
	 * Constructs a new WmqTraceParser.
	 */
	public WmqTraceParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - ACTIVITY TRACE MESSAGE
	 */
	@Override
	protected String getActivityDataType() {
		return "ACTIVITY TRACE MESSAGE"; // NON-NLS
	}
}
