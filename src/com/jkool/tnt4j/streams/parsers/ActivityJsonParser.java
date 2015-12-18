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

import java.util.Map;

import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements an activity data parser that assumes each activity data item is an
 * JSON format string, where each field is represented by a key/value pair and
 * the name is used to map each field onto its corresponding activity field.
 * </p>
 *
 * @version $Revision: 2 $
 */
// TODO: now works when one activity item is one JSON data package. No activity
// arrays parsing supported.
public class ActivityJsonParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityJsonParser.class);

	/**
	 * Constructs a new ActivityJsonParser.
	 */
	public ActivityJsonParser() {
		super(LOGGER);
	}

	/**
	 * Makes map object containing activity object data parsed from JSON.
	 * 
	 * @param data
	 *            activity object data object
	 * 
	 * @return activity object data map
	 */
	@Override
	protected Map<String, ?> getDataMap(Object data) {
		return Utils.fromJsonToMap(data);
	}
}
