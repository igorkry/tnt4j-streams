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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WmqParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.WmqUtils;

/**
 * This class extends the basic activity XML parser for handling data specific to messaging operations. It provides
 * additional transformations of the raw activity data collected for specific fields.
 * <p>
 * In particular, this class will convert the {@link StreamFieldType#TrackingId} and {@link StreamFieldType#Correlator}
 * fields values from a tokenized list of items into a value in the appropriate form required by the JKool Cloud.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link ActivityXmlParser}):
 * <ul>
 * <li>SignatureDelim - signature fields delimiter. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class MessageActivityXmlParser extends ActivityXmlParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(MessageActivityXmlParser.class);
	/**
	 * Contains the field separator (set by {@code SignatureDelim} property) - Default:
	 * "{@value com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser#DEFAULT_DELIM}"
	 */
	protected String sigDelim = DEFAULT_DELIM;

	/**
	 * Constructs a new MessageActivityXmlParser.
	 */
	public MessageActivityXmlParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();
				if (WmqParserProperties.PROP_SIG_DELIM.equalsIgnoreCase(name)) {
					if (StringUtils.isNotEmpty(value)) {
						sigDelim = value;
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.setting", name, value);
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method applies custom handling for setting field values. This method will construct the signature to use for
	 * the message from the specified value, which is assumed to be a string containing the inputs required for the
	 * message signature calculation, with each input separated by the delimiter specified in property
	 * {@code SignatureDelim}.
	 * <p>
	 * To initiate signature calculation, {@code field} "value type" attribute must be set to
	 * {@value com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants#VT_SIGNATURE}.
	 * <p>
	 * The signature items MUST be specified in the following order:
	 * <ol>
	 * <li>Message Type</li>
	 * <li>Message Format</li>
	 * <li>Message ID</li>
	 * <li>Message User</li>
	 * <li>Message Application Type</li>
	 * <li>Message Application Name</li>
	 * <li>Message Date</li>
	 * <li>Message Time</li>
	 * <li>Correlator ID</li>
	 * </ol>
	 * <p>
	 * Individual items can be omitted, but must contain a place holder (except for trailing items).
	 *
	 * @see WmqUtils#computeSignature(Object, String, EventSink)
	 */
	@Override
	protected void applyFieldValue(ActivityInfo ai, ActivityField field, Object value) throws ParseException {
		StreamFieldType fieldType = field.getFieldType();
		if (fieldType != null && WmqStreamConstants.VT_SIGNATURE.equalsIgnoreCase(field.getValueType())) {
			switch (fieldType) {
			case Correlator:
			case TrackingId:
				value = WmqUtils.computeSignature(value, sigDelim, logger());
				break;
			default:
				break;
			}
		}

		super.applyFieldValue(ai, field, value);
	}
}
