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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements an activity data parser that assumes each activity data item is an plain java {@link ProducerRecord} data
 * structure, where each field is represented by declared class field and the field name is used to map each field into
 * its corresponding activity field.
 * <p>
 * List of supported field names:
 * <ul>
 * <li>topic - topic name record is being sent to</li>
 * <li>partition - partition identifier to which the record will be sent</li>
 * <li>timestamp - record timestamp value</li>
 * <li>key - record key</li>
 * <li>value - record data</li>
 * </ul>
 * <p>
 * If {@code key} or {@code value} contains complex data, use stacked parsers to parse that data. Or if it can be
 * treated as simple Java object (POJO), particular field value can be resolved defining class field names within
 * locator path string.
 * <p>
 * This activity parser supports configuration properties from {@link GenericActivityParser} (and higher hierarchy
 * parsers).
 *
 * @version $Revision: 1 $
 */
public class KafkaProducerRecordParser extends GenericActivityParser<ProducerRecord<?, ?>> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(KafkaProducerRecordParser.class);

	/**
	 * Constructs a new KafkaProducerRecordParser.
	 */
	public KafkaProducerRecordParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link org.apache.kafka.clients.producer.ProducerRecord}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return ProducerRecord.class.isInstance(data);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		// if (CollectionUtils.isNotEmpty(props)) {
		// for (Map.Entry<String, String> prop : props) {
		// String name = prop.getKey();
		// String value = prop.getValue();
		//
		// // no any additional properties are required yet.
		// if (false) {
		// logger().log(OpLevel.DEBUG,
		// StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
		// name, value);
		// }
		// }
		// }
	}

	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		String[] valPath = Utils.getNodePath(locStr, StreamsConstants.DEFAULT_PATH_DELIM);
		try {
			val = getRecordValue(valPath, cData.getData(), 0);
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"KafkaProducerRecordParser.resolve.locator.value.failed", exc);
		}

		return val;
	}

	/**
	 * Resolves {@link org.apache.kafka.clients.producer.ProducerRecord} instance field value defined by
	 * <tt>pRecord</tt> fields names <tt>path<tt> array.
	 * <p>
	 * If producer record <tt>key</tt> and <tt>value</tt> fields classes are known, it can be processed further defining
	 * field names of those classes as <tt>path</path> elements.
	 *
	 * @param path
	 *            fields path as array of producer record field names
	 * @param pRecord
	 *            producer record instance to resolve value
	 * @param i
	 *            processed locator path element index
	 * @return resolved producer record value, or {@code null} if value is not resolved
	 * @throws java.lang.RuntimeException
	 *             if field can't be found or accessed
	 *
	 * @see Utils#getFieldValue(String[], Object, int)
	 */
	protected Object getRecordValue(String[] path, ProducerRecord<?, ?> pRecord, int i) throws RuntimeException {
		if (ArrayUtils.isEmpty(path) || pRecord == null) {
			return null;
		}

		Object val = null;
		String propStr = path[i];

		if ("topic".equalsIgnoreCase(propStr)) { // NON-NLS
			val = pRecord.topic();
		} else if ("partition".equalsIgnoreCase(propStr)) { // NON-NLS
			val = pRecord.partition();
		} else if ("timestamp".equalsIgnoreCase(propStr)) { // NON-NLS
			val = pRecord.timestamp();
		} else if ("key".equalsIgnoreCase(propStr)) { // NON-NLS
			val = Utils.getFieldValue(path, pRecord.key(), i + 1);
		} else if ("value".equalsIgnoreCase(propStr)) { // NON-NLS
			val = Utils.getFieldValue(path, pRecord.value(), i + 1);
		}

		return val;
	}
}
