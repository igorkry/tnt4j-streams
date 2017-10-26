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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.fields.AggregationType;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.matchers.Matchers;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class that all activity parsers must extend. It provides some base functionality useful for all activity
 * parsers.
 *
 * @version $Revision: 1 $
 */
public abstract class ActivityParser {
	/**
	 * Name of activity parser
	 */
	private String name;

	private String[] tags;

	/**
	 * Constructs a new ActivityParser.
	 */
	protected ActivityParser() {
	}

	/**
	 * Returns logger used by this parser.
	 *
	 * @return parser logger
	 */
	protected abstract EventSink logger();

	/**
	 * Set configuration properties for the parser.
	 * <p>
	 * This method is called during the parsing of the configuration when all specified properties in the configuration
	 * have been loaded. In general, parsers should ignore properties that they do not recognize, since they may be
	 * valid for a subclass of the parser. If extending an existing parser subclass, the method from the base class
	 * should be called so that it can process any properties it requires.
	 *
	 * @param props
	 *            configuration properties to set
	 */
	public abstract void setProperties(Collection<Map.Entry<String, String>> props);

	/**
	 * Add an activity field definition to the set of fields supported by this parser.
	 *
	 * @param field
	 *            activity field to add
	 */
	public abstract void addField(ActivityField field);

	/**
	 * Parse the specified raw activity data, converting each field in raw data to its corresponding value for passing
	 * to JKool Cloud.
	 *
	 * @param stream
	 *            parent stream
	 * @param data
	 *            raw activity data to parse
	 * @return converted activity info, or {@code null} if raw activity data does not match format for this parser
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error parsing raw data string
	 * @see #isDataClassSupported(Object)
	 * @see GenericActivityParser#parsePreparedItem(com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	public abstract ActivityInfo parse(TNTInputStream<?, ?> stream, Object data)
			throws IllegalStateException, ParseException;

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	public abstract boolean isDataClassSupported(Object data);

	/**
	 * Sets the value for the field in the specified activity.
	 *
	 * @param ai
	 *            activity object whose field is to be set
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for this field
	 * @throws ParseException
	 *             if an error occurs while parsing provided activity data <tt>value</tt>
	 */
	protected void applyFieldValue(ActivityInfo ai, ActivityField field, Object value) throws ParseException {
		ai.applyField(field, value);
	}

	/**
	 * Sets the value for the field in the specified activity entity.
	 * <p>
	 * If field has stacked parser defined, then field value is parsed into separate activity using stacked parser. If
	 * field can be parsed by stacked parser, produced activity can be merged or added as a child into specified
	 * (parent) activity depending on stacked parser reference 'aggregation' attribute value.
	 *
	 * @param stream
	 *            parent stream
	 * @param ai
	 *            activity object whose field is to be set
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for this field
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error occurs while parsing provided activity data <tt>value</tt>
	 * @see #parse(TNTInputStream, Object)
	 */
	protected void applyFieldValue(TNTInputStream<?, ?> stream, ActivityInfo ai, ActivityField field, Object value)
			throws IllegalStateException, ParseException {

		applyFieldValue(ai, field, value);

		if (CollectionUtils.isNotEmpty(field.getStackedParsers())) {
			boolean applied = false;
			for (ActivityField.ParserReference parserRef : field.getStackedParsers()) {
				// TODO: tags
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.stacked.parser.applying", name, parserRef, field);
				try {
					applied = applyStackedParser(stream, ai, field, parserRef, value);

					if (applied) {
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.stacked.parser.applied", name, parserRef, field);
						break;
					}
				} catch (Exception exc) {
					logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.stacked.parser.failed", name, parserRef, field, exc);
				}
			}

			if (!applied) {
				logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.stacked.parsers.missed", name, field);
			}
		}
	}

	/**
	 * Applies stacked parser to parse provided activity data <tt>value</tt>.
	 *
	 * @param stream
	 *            parent stream
	 * @param ai
	 *            activity object whose data to be altered
	 * @param field
	 *            activity field providing data
	 * @param parserRef
	 *            stacked parser reference
	 * @param value
	 *            data value to be parsed by stacked parser
	 * @return {@code true} if referenced stacked parser was applied, {@code false} - otherwise
	 * @throws ParseException
	 *             if an error occurs while parsing provided activity data <tt>value</tt>
	 */
	protected boolean applyStackedParser(TNTInputStream<?, ?> stream, ActivityInfo ai, ActivityField field,
			ActivityField.ParserReference parserRef, Object value) throws ParseException {
		if (parserRef.getParser().isDataClassSupported(value) && match(parserRef, value, ai, field)) {
			ActivityInfo sai = parserRef.getParser().parse(stream, value);

			if (sai != null) {
				if (parserRef.getAggregationType() == AggregationType.Join) {
					ai.addChild(sai);
				} else {
					ai.mergeAll(sai);
				}

				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if stacked parser reference defined match evaluation expressions evaluates to positive match value for
	 * provided activity data <tt>value</tt> or parsing context <tt>ai</tt>.
	 *
	 * @param parserRef
	 *            stacked parser reference
	 * @param value
	 *            activity data package to be parsed by stacked parser
	 * @param ai
	 *            activity entity providing parsing context data
	 * @param field
	 *            activity field
	 * @return {@code true} if referenced stacked parser matches activity <tt>data</tt> or parsing context <tt>ai</tt>,
	 *         {@code false} - otherwise
	 */
	protected boolean match(ActivityField.ParserReference parserRef, Object value, ActivityInfo ai,
			ActivityField field) {
		if (CollectionUtils.isNotEmpty(parserRef.getMatchExpressions())) {
			for (String matchExpression : parserRef.getMatchExpressions()) {
				boolean match;
				try {
					match = Matchers.evaluate(matchExpression, value, ai);
				} catch (Exception exc) {
					logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.match.evaluation.failed", name, field.getFieldTypeName(),
							parserRef.getParser().name, exc);
					match = false;
				}

				if (!match) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Checks if activity <tt>data</tt> package is supported by <tt>field</tt> referenced stacked parsers.
	 *
	 * @param field
	 *            activity field to get stacked parsers
	 * @param data
	 *            activity data package
	 * @param <T>
	 *            type of activity data package
	 * @return {@code true} if field has stacked parsers defined and activity data is supported by any of those parsers,
	 *         {@code false} - otherwise.
	 *
	 * @see #isDataClassSupported(Object)
	 */
	protected static <T> boolean isDataSupportedByStackedParser(ActivityField field, T data) {
		Collection<ActivityField.ParserReference> stackedParsers = field.getStackedParsers();

		if (stackedParsers != null) {
			for (ActivityField.ParserReference pRef : stackedParsers) {
				if (pRef.getParser().isDataClassSupported(data)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns name of activity parser
	 *
	 * @return name string of activity parser
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets name for activity parser
	 *
	 * @param name
	 *            name string value
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns tag strings array of activity parser
	 *
	 * @return tags strings array
	 */
	public String[] getTags() {
		return tags == null ? null : Arrays.copyOf(tags, tags.length);
	}

	/**
	 * Sets activity parser tags string. Tags are separated using
	 * "{@value com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser#DEFAULT_DELIM}".
	 *
	 * @param tags
	 *            tags string
	 */
	public void setTags(String tags) {
		this.tags = Utils.getTags(tags);
	}

	/**
	 * Returns whether this parser supports delimited locators in parser fields configuration. This allows user to
	 * define multiple locations for a field using locators delimiter
	 * {@link com.jkoolcloud.tnt4j.streams.configure.sax.ConfigParserHandler#LOC_DELIM} field value.
	 * <p>
	 * But if locators are some complex expressions like XPath functions, it may be better to deny this feature for a
	 * parser to correctly load locator expression.
	 *
	 * @return flag indicating if parser supports delimited locators
	 */
	public boolean canHaveDelimitedLocators() {
		return true;
	}

	/**
	 * Adds reference to specified entity object being used by this parser.
	 *
	 * @param refObject
	 *            entity object to reference
	 * @throws IllegalStateException
	 *             if referenced object can't be linked to parser
	 */
	public abstract void addReference(Object refObject) throws IllegalStateException;

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries
	 */
	protected abstract Object getActivityDataType();
}
