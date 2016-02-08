/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityNameValueParserTest {

	private static final String TEST = "TEST=TESTVALUE\nTEST2=TESTVALUE2";
	private ActivityNameValueParser activityNameValueParser = new ActivityNameValueParser();;
	private TNTInputStream<String> stream = mock(TNTInputStream.class);;

	@Test
	public void testSetProperties() throws Exception {
		Map<String, String> propertiesMap = new HashMap<String, String>() {
			{
				put(StreamsConfig.PROP_FLD_DELIM, "\n");
				put(StreamsConfig.PROP_VAL_DELIM, "=");
				put(StreamsConfig.PROP_PATTERN, ".*");
				put(StreamsConfig.PROP_STRIP_QUOTES, "false");
			}
		};
		activityNameValueParser.setProperties(propertiesMap.entrySet());
		// assertEquals(propertiesMap.get(StreamsConfig.PROP_FLD_DELIM),
		// activityNameValueParser.fieldDelim.);
		assertEquals(propertiesMap.get(StreamsConfig.PROP_VAL_DELIM), activityNameValueParser.valueDelim.toString());
		assertEquals(propertiesMap.get(StreamsConfig.PROP_PATTERN), activityNameValueParser.pattern.toString());
		assertEquals(propertiesMap.get(StreamsConfig.PROP_STRIP_QUOTES),
				activityNameValueParser.stripQuotes ? "true" : "false");

	}

	@Test
	public void testParse() throws Exception {
		String testString = TEST;
		testSetProperties();
		activityNameValueParser.pattern = null;

		activityNameValueParser.parse(stream, testString);

		// TODO
	}

	@Test(expected = IllegalStateException.class)
	public void testParseExc() throws Exception {
		assertNull(activityNameValueParser.parse(stream, null));
		activityNameValueParser.fieldDelim = null;
		assertNull(activityNameValueParser.parse(stream, TEST));

		activityNameValueParser.valueDelim = null;
		assertNull(activityNameValueParser.parse(stream, TEST));

	}

}
