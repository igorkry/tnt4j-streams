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
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;

/**
 * Implements Kafka intercepted messages trace reporter used stream. Stream itself does nothing, only initiates
 * streaming process. Reporter uses just stream output to send intercepted messages data to JKoolCloud.
 *
 * @version $Revision: 1 $
 */
class KafkaMsgTraceStream extends AbstractBufferedStream<ActivityInfo> {
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
	public boolean addInputToBuffer(ActivityInfo inputData) throws IllegalStateException {
		return super.addInputToBuffer(inputData);
	}

	@Override
	protected ActivityInfo makeActivityInfo(ActivityInfo data) throws Exception {
		return data;
	}

	@Override
	protected long getActivityItemByteSize(ActivityInfo activityItem) {
		return 0; // TODO
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
