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

package com.jkoolcloud.tnt4j.streams.custom.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jayway.jsonpath.DocumentContext;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Extends {@link ActivityJsonParser} to parse JSON data packages containing parent
 * {@link com.jkoolcloud.tnt4j.tracker.TrackingActivity}/{@link com.jkoolcloud.tnt4j.tracker.TrackingEvent} and array of
 * child {@link com.jkoolcloud.tnt4j.core.Snapshot} entities. Primary use of this parser is to parse performance metrics
 * packages collected by system performance monitors like collectd or Nagios. If 'ChildrenField' property is not
 * defined, acts like ordinary {@link ActivityJsonParser}.
 * <p>
 * This parser supports the following properties (in addition to those supported by {@link ActivityJsonParser}):
 * <ul>
 * <li>ChildrenField - field name referencing JSON data structure containing child snapshot definitions. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class SnapshotsJsonParser extends ActivityJsonParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(SnapshotsJsonParser.class);

	private String snapshotsFieldName = null;
	private ActivityField snapshotsField;

	private boolean snapshotsFieldInitialized = false;

	/**
	 * Constructs a new SnapshotsJsonParser.
	 */
	public SnapshotsJsonParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}
		super.setProperties(props);
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (ParserProperties.PROP_CHILDREN_FIELD.equalsIgnoreCase(name)) {
				snapshotsFieldName = value;
			}
		}
	}

	private void initSnapshotsField() {
		if (StringUtils.isNotEmpty(snapshotsFieldName)) {
			for (ActivityField field : fieldList) {
				if (field.getFieldTypeName().equalsIgnoreCase(snapshotsFieldName)) {
					snapshotsField = field;
				}
			}

			if (snapshotsField != null) {
				removeField(snapshotsField);
			}
		}

		snapshotsFieldInitialized = true;
	}

	@Override
	protected ActivityInfo parsePreparedItem(TNTInputStream<?, ?> stream, String dataStr, DocumentContext data)
			throws ParseException {
		if (!snapshotsFieldInitialized) {
			initSnapshotsField();
		}

		ActivityInfo ai = super.parsePreparedItem(stream, dataStr, data);

		if (snapshotsField != null) {
			Object fValue = Utils.simplifyValue(parseLocatorValues(snapshotsField, stream, data));

			if (fValue == null) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"SnapshotsJsonParser.no.snapshots"), getName(), snapshotsFieldName);
			} else {
				Object[] snapshotsData = Utils.makeArray(fValue);

				for (Object snapshotData : snapshotsData) {
					ActivityInfo parsedItem = applySnapshotParsers(stream, snapshotData);
					if (parsedItem == null) {
						logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"SnapshotsJsonParser.no.snapshot.parser"), snapshotData);
					} else {
						ai.addChild(parsedItem);
					}
				}
			}
		}

		return ai;
	}

	private ActivityInfo applySnapshotParsers(TNTInputStream<?, ?> stream, Object resolvedValue) throws ParseException {
		if (CollectionUtils.isNotEmpty(snapshotsField.getStackedParsers())) {
			for (ActivityParser parser : snapshotsField.getStackedParsers()) {
				ActivityInfo parsedItem = parser.parse(stream, resolvedValue);
				if (parsedItem != null) {
					return parsedItem;
				}
			}
		}

		return null;
	}
}
