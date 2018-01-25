/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace;

import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;

/**
 * Implements Kafka intercepted messages trace reporter used stream. Stream itself does nothing, only initiates
 * streaming process. Reporter uses just stream output to send intercepted messages data to JKool Cloud.
 *
 * @version $Revision: 1 $
 */
class KafkaMsgTraceStream extends AbstractBufferedStream<Object> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(KafkaMsgTraceStream.class);

	private boolean ended = false;

	/**
	 * Constructs a new KafkaMsgTraceStream.
	 */
	public KafkaMsgTraceStream() {
		setName("KafkaMsgTraceStream"); // NON-NLS
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected long getActivityItemByteSize(Object activityItem) {
		return 0;
	}

	@Override
	protected boolean isInputEnded() {
		return ended;
	}

	/**
	 * Marks stream input end.
	 */
	public void markEnded() {
		this.ended = true;
	}

}
