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

package com.jkoolcloud.tnt4j.streams.outputs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.Activity;
import com.jkoolcloud.tnt4j.core.Snapshot;
import com.jkoolcloud.tnt4j.core.Trackable;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.source.DefaultSourceFactory;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.source.SourceFactory;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;

/**
 * Implements TNT4J-Streams output logger for activities provided as {@link ActivityInfo} entities to be recorded to
 * jKoolCloud over TNT4J and JESL APIs.
 * <p>
 * This output supports the following configuration properties (in addition to those supported by
 * {@link com.jkoolcloud.tnt4j.streams.outputs.AbstractJKCloudOutput}):
 * <ul>
 * <li>ResolveServerFromDNS - flag indicating whether to resolve activity entity host name/IP from DNS server. Default
 * value - {@code false}. (Optional)</li>
 * <li>SplitRelatives - flag indicating whether to send activity entity child entities independently merging data from
 * both parent and child entity fields into produced entity. Default value - {@code false}. (Optional)</li>
 * <li>BuildSourceFQNFromStreamedData - flag indicating whether to set streamed activity entity {@link Source} FQN build
 * from activity fields data instead of default on configured in 'tnt4j.properties'. Default value - {@code true}.
 * (Optional)</li>
 * <li>SourceFQN - {@link Source} FQN pattern to be used when building it from streamed activity entity fields values.
 * Format is: SourceType1=${FieldName1}#SourceType2=${FieldName2}#SourceType3=${FieldName3}... . Default value -
 * 'APPL=${ApplName}#USER=${UserName}#SERVER=${ServerName}#NETADDR=${ServerIp}#GEOADDR=${Location}'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see ActivityInfo#buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Collection)
 */
public class JKCloudActivityOutput extends AbstractJKCloudOutput<ActivityInfo, Trackable> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JKCloudActivityOutput.class);
	private static final String DEFAULT_SOURCE_FQN = "APPL=${ApplName}#SERVER=${ServerName}#NETADDR=${ServerIp}#GEOADDR=${Location}"; // NON-NLS

	private boolean resolveServer = false;
	private boolean splitRelatives = false;
	private boolean buildFQNFromData = true;
	private String sourceFQN = null;

	/**
	 * Constructs a new JKCloudActivityOutput.
	 */
	public JKCloudActivityOutput() {
		super();
	}

	/**
	 * Constructs a new JKCloudActivityOutput.
	 *
	 * @param name
	 *            output name value
	 */
	public JKCloudActivityOutput(String name) {
		super(name);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setProperty(String name, Object value) {
		super.setProperty(name, value);

		if (OutputProperties.PROP_RESOLVE_SERVER.equalsIgnoreCase(name)) {
			resolveServer = Utils.toBoolean((String) value);
		} else if (OutputProperties.PROP_SPLIT_RELATIVES.equalsIgnoreCase(name)
				|| OutputProperties.PROP_TURN_OUT_CHILDREN.equalsIgnoreCase(name)) {
			splitRelatives = Utils.toBoolean((String) value);
		} else if (OutputProperties.PROP_BUILD_FQN_FROM_DATA.equalsIgnoreCase(name)) {
			buildFQNFromData = Utils.toBoolean((String) value);
		} else if (OutputProperties.PROP_SOURCE_FQN.equalsIgnoreCase(name)) {
			sourceFQN = (String) value;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 
	 * @see ActivityInfo#buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Collection)
	 */
	@Override
	public void logItem(ActivityInfo ai) throws Exception {
			Tracker tracker = getTracker();
			ai.resolveServer(resolveServer);
			String aiFQN = buildFQNFromData ? StringUtils.isEmpty(sourceFQN) ? DEFAULT_SOURCE_FQN : sourceFQN : null;

			if (splitRelatives && ai.hasChildren()) {
				for (ActivityInfo cai : ai.getChildren()) {
					cai.merge(ai);
					Trackable t = cai.buildTrackable(tracker);
					alterTrackableSource(tracker, t, cai, aiFQN);
					recordActivity(tracker, CONN_RETRY_INTERVAL, t);
				}
			} else {
				List<Trackable> chTrackables = new ArrayList<>();
				Trackable t = ai.buildTrackable(tracker, chTrackables);
				alterTrackableSource(tracker, t, ai, aiFQN);
				recordActivity(tracker, CONN_RETRY_INTERVAL, t);

				for (int i = 0; i < chTrackables.size(); i++) {
					Trackable chT = chTrackables.get(i);
					ActivityInfo cai = ai.getChildren().get(i);
					alterTrackableSource(tracker, chT, cai, aiFQN);
					recordActivity(tracker, CONN_RETRY_INTERVAL, chT);
				}
			}
		}

	private static void alterTrackableSource(Tracker tracker, Trackable t, ActivityInfo ai, String fqn) {
		if (StringUtils.isNotEmpty(fqn)) {
			Source tSrc = buildSource(tracker, ai.getSourceFQN(fqn));
			t.setSource(tSrc);

			Collection<Snapshot> snapshots = null;
			if (t instanceof Activity) {
				snapshots = ((Activity) t).getSnapshots();
			} else if (t instanceof TrackingEvent) {
				snapshots = ((TrackingEvent) t).getOperation().getSnapshots();
			}

			if (CollectionUtils.isNotEmpty(snapshots)) {
				List<ActivityInfo> cais = ai.getChildren();
				int i = 0;
				for (Snapshot s : snapshots) {
					ActivityInfo cai = (cais == null || i >= cais.size()) ? null : cais.get(i++);
					if (cai == null) {
						s.setSource(tSrc);
					} else {
						s.setSource(buildSource(tracker, cai.getSourceFQN(fqn)));
					}
				}
			}
		}
	}

	private static Source buildSource(Tracker tracker, String sourceFQN) {
		if (StringUtils.isEmpty(sourceFQN)) {
			return null;
		}
		SourceFactory sf = tracker == null ? DefaultSourceFactory.getInstance()
				: tracker.getConfiguration().getSourceFactory();
		Source source = sf.newFromFQN(sourceFQN);
		source.setSSN(sf.getSSN());

		return source;

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

	@Override
	public Trackable formatStreamStatusMessage(TrackingEvent statusMessage) {
		return statusMessage;
	}
}
