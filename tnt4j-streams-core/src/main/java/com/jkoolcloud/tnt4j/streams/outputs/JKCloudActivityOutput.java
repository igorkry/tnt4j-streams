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

import com.jkoolcloud.tnt4j.core.Snapshot;
import com.jkoolcloud.tnt4j.core.Trackable;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * Implements TNT4J-Streams output logger for activities provided as {@link ActivityInfo} entities to be recorded to
 * JKool Cloud over TNT4J and JESL APIs.
 *
 * @version $Revision: 1 $
 *
 * @see ActivityInfo#buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker)
 */
public class JKCloudActivityOutput extends AbstractJKCloudOutput<ActivityInfo, Trackable> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JKCloudActivityOutput.class);

	private boolean resolveServer = false;

	/**
	 * Constructs a new JKCloudActivityOutput.
	 */
	public JKCloudActivityOutput() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, Object value) {
		super.setProperty(name, value);

		if (OutputProperties.PROP_RESOLVE_SERVER.equalsIgnoreCase(name)) {
			resolveServer = Boolean.parseBoolean((String) value);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 
	 * @see ActivityInfo#buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker)
	 */
	@Override
	public void logItem(ActivityInfo ai) throws Exception {
		String fqn = ai.getSourceFQN(resolveServer);
		Tracker tracker = getTracker(fqn, Thread.currentThread());

		Trackable t = ai.buildTrackable(tracker);

		recordActivity(tracker, CONN_RETRY_INTERVAL, t);
	}

	@Override
	protected void logJKCActivity(Tracker tracker, Trackable trackable) {
		if (trackable instanceof TrackingActivity) {
			tracker.tnt((TrackingActivity) trackable);
		} else if (trackable instanceof Snapshot) {
			tracker.tnt((Snapshot) trackable);
		} else {
			tracker.tnt((TrackingEvent) trackable);
		}
	}
}
