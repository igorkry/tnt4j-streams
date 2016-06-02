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

import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.tracker.Tracker;

/**
 * @author akausinis
 * @version 1.0 TODO
 */
public class JKCloudActivityOutput extends AbstractJKCloudOutput<ActivityInfo> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JKCloudActivityOutput.class);

	/**
	 * TODO
	 */
	public JKCloudActivityOutput() {
		super(LOGGER);
	}

	@Override
	public void sendItem(ActivityInfo ai) throws Exception {
		Tracker tracker = getTracker(ai.getSourceFQN(), Thread.currentThread());

		ensureTrackerOpened(tracker);

		ai.recordActivity(tracker, CONN_RETRY_INTERVAL);
	}
}
