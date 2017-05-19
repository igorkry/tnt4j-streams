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

package com.jkoolcloud.tnt4j.streams.outputs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
 * @see ActivityInfo#buildTrackable(Tracker, Collection)
 */
public class JKCloudActivityOutput extends AbstractJKCloudOutput<ActivityInfo, Trackable> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JKCloudActivityOutput.class);

	private boolean resolveServer = false;
	private boolean turnOutActivityChildren = false;
	private boolean buildFQNFromData = true;

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
		} else if (OutputProperties.PROP_TURN_OUT_CHILDREN.equalsIgnoreCase(name)) {
			turnOutActivityChildren = Boolean.parseBoolean((String) value);
		} else if (OutputProperties.PROP_BUILD_FQN_FROM_DATA.equalsIgnoreCase(name)) {
			buildFQNFromData = Boolean.parseBoolean((String) value);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 
	 * @see ActivityInfo#buildTrackable(Tracker, Collection)
	 */
	@Override
	public void logItem(ActivityInfo ai) throws Exception {
		String fqn = buildFQNFromData ? ai.getSourceFQN(resolveServer) : null;
		Tracker tracker = getTracker(null, Thread.currentThread());

		if (turnOutActivityChildren && ai.hasChildren()) {
			for (ActivityInfo cai : ai.getChildren()) {
				cai.merge(ai);
				Trackable t = cai.buildTrackable(tracker);
				recordActivity(tracker, CONN_RETRY_INTERVAL, t);
			}
		} else {
			List<Trackable> chTrackables = new ArrayList<>();
			Trackable t = ai.buildTrackable(tracker, chTrackables);
			recordActivity(tracker, CONN_RETRY_INTERVAL, t);

			for (Trackable chT : chTrackables) {
				recordActivity(tracker, CONN_RETRY_INTERVAL, chT);
			}
		}
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
