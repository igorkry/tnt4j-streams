/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.parsers;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import org.apache.commons.collections.CollectionUtils;

import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.filters.StreamFilter;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;

//import com.jkool.tnt4j.streams.filters.StreamFilter;

/**
 * Base class that all activity parsers must extend. It provides some base
 * functionality useful for all activity parsers.
 *
 * @version $Revision: 2 $
 */
public abstract class ActivityParser {
	/**
	 * Parser logger.
	 */
	protected final EventSink logger;

	/**
	 * Constant for default delimiter symbol used by parsers.
	 */
	protected static final String DEFAULT_DELIM = ","; // NON-NLS

	/**
	 * Name of activity parser
	 */
	private String name;

	private String[] tags;

	/**
	 * List of filters being used by stream.
	 */
	protected final Collection<StreamFilter> filters = new LinkedList<StreamFilter>();

	/**
	 * Initializes ActivityParser.
	 *
	 * @param logger
	 *            logger used by activity parser
	 */
	protected ActivityParser(EventSink logger) {
		this.logger = logger;
	}

	/**
	 * Defines the mapping of activity fields to the location(s) in the raw data
	 * from which to extract their values.
	 */
	protected final Map<ActivityField, List<ActivityFieldLocator>> fieldMap = new HashMap<ActivityField, List<ActivityFieldLocator>>();

	/**
	 * Set properties for the parser.
	 * <p>
	 * This method is called during the parsing of the configuration when all
	 * specified properties in the configuration have been loaded. In general,
	 * parsers should ignore properties that they do not recognize, since they
	 * may be valid for a subclass of the parser. If extending an existing
	 * parser subclass, the method from the base class should be called so that
	 * it can process any properties it requires.
	 * </p>
	 *
	 * @param props
	 *            properties to set
	 *
	 * @throws Throwable
	 *             indicates error with properties
	 */
	public abstract void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable;

	/**
	 * Add an activity field definition to the set of fields supported by this
	 * parser.
	 *
	 * @param field
	 *            activity field to add
	 */
	public void addField(ActivityField field) {
		logger.log(OpLevel.DEBUG,
				StreamsResources.getStringFormatted("ActivityParser.adding.field", field.toDebugString()));
		fieldMap.put(field, field.getLocators());
	}

	/**
	 * Parse the specified raw activity data, converting each field in raw data
	 * to its corresponding value for passing to jKool Cloud Service.
	 *
	 * @param stream
	 *            parent stream
	 * @param data
	 *            raw activity data to parse
	 *
	 * @return converted activity info, or {@code null} if raw data string does
	 *         not match format for this parser
	 *
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error parsing raw data string
	 * @see #isDataClassSupported(Object)
	 */
	public abstract ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException;

	/**
	 * Returns whether this parser supports the given format of the activity
	 * data. This is used by activity streams to determine if the parser can
	 * parse the data in the format that the stream has it.
	 *
	 * @param data
	 *            data object whose class is to be verified
	 *
	 * @return {@code true} if this parser can process data in the specified
	 *         format, {@code false} otherwise
	 */
	public abstract boolean isDataClassSupported(Object data);

	/**
	 * Reads the next string (line) from the specified data input source.
	 *
	 * @param data
	 *            input source for activity data
	 *
	 * @return string, or {@code null} if end of input source has been reached
	 *
	 * @throws IllegalArgumentException
	 *             if the class of input source supplied is not supported.
	 */
	protected String getNextString(Object data) {
		if (data == null) {
			return null;
		}
		if (data instanceof String) {
			return (String) data;
		} else if (data instanceof byte[]) {
			return Utils.getString((byte[]) data);
		}
		BufferedReader rdr;
		if (data instanceof BufferedReader) {
			rdr = (BufferedReader) data;
		} else if (data instanceof Reader) {
			rdr = new BufferedReader((Reader) data);
		} else if (data instanceof InputStream) {
			rdr = new BufferedReader(new InputStreamReader((InputStream) data));
		} else {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted("ActivityParser.data.unsupported", data.getClass().getName()));
		}
		String str = null;
		try {
			str = rdr.readLine();
		} catch (EOFException eof) {
			logger.log(OpLevel.DEBUG, StreamsResources.getString("ActivityParser.data.end"), eof);
		} catch (IOException ioe) {
			logger.log(OpLevel.WARNING, StreamsResources.getString("ActivityParser.error.reading"), ioe);
		}

		return str;
	}

	/**
	 * Sets the value for the field in the specified activity.
	 *
	 * @param ai
	 *            activity object whose field is to be set
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for this field
	 *
	 * @throws ParseException
	 *             if an error parsing the specified value
	 */
	protected void applyFieldValue(ActivityInfo ai, ActivityField field, Object value) throws ParseException {
		ai.applyField(field, value);
	}

	/**
	 * Sets the value for the field in the specified activity. If field has
	 * stacked parser defined, then field value is parsed into separate activity
	 * using stacked parser. If field can be parsed by stacked parser, produced
	 * activity is merged into specified (parent) activity.
	 *
	 * @param stream
	 *            parent stream
	 * @param ai
	 *            activity object whose field is to be set
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for this field
	 *
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error parsing the specified value
	 *
	 * @see #parse(TNTInputStream, Object)
	 */
	protected void applyFieldValue(TNTInputStream stream, ActivityInfo ai, ActivityField field, Object value)
			throws IllegalStateException, ParseException {
		if (CollectionUtils.isNotEmpty(field.getStackedParsers())) {
			value = Utils.cleanActivityData(value);
			for (ActivityParser stackedParser : field.getStackedParsers()) {
				if (stackedParser.isDataClassSupported(value)) {
					ActivityInfo sai = stackedParser.parse(stream, value);

					if (sai != null) {
						ai.merge(sai);
						break;
					}
				}
			}
		} else {
			applyFieldValue(ai, field, value);
		}
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
	 * Adds the specified filter to the list of filters being used by this
	 * stream.
	 *
	 * @param filter
	 *            filter to add
	 */
	public void addFilter(StreamFilter filter) {
		filters.add(filter);
	}

	/**
	 * Applies all defined filters on specified activity item. If activity item
	 * matches any of filter criteria - such item is skipped from sending it to
	 * jKool Cloud Service.
	 *
	 * @param activityInfo
	 *            activity item data to check against defined filters
	 *
	 * @return {@code true} if activity data was filtered by any of defined
	 *         filters, {@code false} otherwise
	 */
	protected boolean filterActivity(ActivityInfo activityInfo) {
		boolean filtered = false;
		Iterator<StreamFilter> filtersIterator = filters.iterator();
		while (!filtered && filtersIterator.hasNext()) {
			StreamFilter filter = filtersIterator.next();
			filtered = filter.doFilterActivity(activityInfo);
		}
		return filtered;
	}

	/**
	 * Returns tag strings array of activity parser
	 * 
	 * @return tags strings array
	 */
	public String[] getTags() {
		return tags;
	}

	/**
	 * Sets activity parser tags string. Tags are separated using ",".
	 * 
	 * @param tags
	 *            tags string
	 */
	public void setTags(String tags) {
		this.tags = Utils.getTags(tags);
	}
}
