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
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

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
	 *
	 * @throws ParserConfigurationException
	 *             if any errors configuring the parser
	 */
	public MessageActivityXmlParser() throws ParserConfigurationException {
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
			if (ParserProperties.PROP_SIG_DELIM.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(value)) {
					sigDelim = value;
					logger().log(OpLevel.DEBUG,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
							name, value);
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
	 */
	@Override
	protected void applyFieldValue(ActivityInfo ai, ActivityField field, Object value) throws ParseException {
		StreamFieldType fieldType = field.getFieldType();
		if (fieldType != null) {
			switch (fieldType) {
			case Correlator:
			case TrackingId:
				Object[] sigItems = null;
				if (value instanceof Object[]) {
					sigItems = (Object[]) value;
				} else if (value instanceof String) {
					String sigStr = (String) value;
					if (sigStr.contains(sigDelim)) {
						sigItems = sigStr.split(Pattern.quote(sigDelim));
					}
				}
				if (sigItems != null) {
					MessageType msgType = null;
					String msgFormat = null;
					byte[] msgId = null;
					String msgUser = null;
					String msgApplType = null;
					String msgApplName = null;
					String msgPutDate = null;
					String msgPutTime = null;
					byte[] correlId = null;
					for (int i = 0; i < sigItems.length; i++) {
						Object item = sigItems[i];
						if (item == null) {
							continue;
						}
						switch (i) {
						case 0:
							msgType = MessageType.valueOf(item instanceof Number ? ((Number) item).intValue()
									: Integer.parseInt(item.toString()));
							break;
						case 1:
							msgFormat = item.toString();
							break;
						case 2:
							msgId = item instanceof byte[] ? (byte[]) item : item.toString().getBytes();
							break;
						case 3:
							msgUser = item.toString();
							break;
						case 4:
							msgApplType = item.toString();
							break;
						case 5:
							msgApplName = item.toString();
							break;
						case 6:
							msgPutDate = item.toString();
							break;
						case 7:
							msgPutTime = item.toString();
							break;
						case 8:
							correlId = item instanceof byte[] ? (byte[]) item : item.toString().getBytes();
							break;
						default:
							break;
						}
					}
					value = Utils.computeSignature(msgType, msgFormat, msgId, msgUser, msgApplType, msgApplName,
							msgPutDate, msgPutTime, correlId);
					logger().log(OpLevel.TRACE,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
									"MessageActivityXmlParser.msg.signature"),
							value, msgType, msgFormat, msgId == null ? "null" : Utils.encodeHex(msgId),
							msgId == null ? "null" : new String(msgId), msgUser, msgApplType, msgApplName, msgPutDate,
							msgPutTime);
				}
				break;
			default:
				break;
			}
		}
		super.applyFieldValue(ai, field, value);
	}
}
