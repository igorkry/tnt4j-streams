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

package com.jkoolcloud.tnt4j.streams.parsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityNameValueParserTest {

	private static final String TEST = "TEST=TESTVALUE\nTEST2=TESTVALUE2";
	private ActivityNameValueParser activityNameValueParser = new ActivityNameValueParser();
	private TNTInputStream<?, ?> stream = mock(TNTInputStream.class);

	@Test
	public void testSetProperties() throws Exception {
		Map<String, String> propertiesMap = new HashMap<String, String>() {
			{
				put(ParserProperties.PROP_FLD_DELIM, "\n");
				put(ParserProperties.PROP_VAL_DELIM, "=");
				put(ParserProperties.PROP_PATTERN, ".*");
				put(ParserProperties.PROP_STRIP_QUOTES, "false");
			}
		};
		activityNameValueParser.setProperties(propertiesMap.entrySet());
		// assertEquals(propertiesMap.get(ParserProperties.PROP_FLD_DELIM),
		// activityNameValueParser.fieldDelim.);
		assertEquals(propertiesMap.get(ParserProperties.PROP_VAL_DELIM), activityNameValueParser.valueDelim);
		assertEquals(propertiesMap.get(ParserProperties.PROP_PATTERN), activityNameValueParser.pattern.toString());
		assertEquals(propertiesMap.get(ParserProperties.PROP_STRIP_QUOTES),
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
	}

	@Test(expected = IllegalStateException.class)
	public void parseDelimExceptionTest() throws Exception {
		activityNameValueParser.valueDelim = null;
		activityNameValueParser.parse(stream, "Test");
	}

	@Test
	public void parseEmptyDataTest() throws Exception {
		assertNull(activityNameValueParser.parse(stream, ""));
	}

	@Test
	public void parseWhenPatternNotNullTest() throws Exception {
		Pattern pattern = Pattern.compile("\\d+");
		activityNameValueParser.pattern = pattern;
		assertNull(activityNameValueParser.parse(stream, "test"));
	}

	@Test
	public void parseWhenPatternNotNullMatchesTest() throws Exception {
		Pattern pattern = Pattern.compile("\\d+");
		activityNameValueParser.pattern = pattern;
		assertNotNull(activityNameValueParser.parse(stream, "14"));
	}

	@Test
	public void setPropertiesWhenNullTest() throws Exception {
		activityNameValueParser.setProperties(null);
	}

	@Test
	public void setPropertiesWhenValueEmptyTest() throws Exception {
		Map<String, String> props = new HashMap<String, String>(1);
		props.put(ParserProperties.PROP_FLD_DELIM, "");
		activityNameValueParser.setProperties(props.entrySet());
		assertNull(activityNameValueParser.fieldDelim);
	}

	@Test
	public void setPropertiesWhenOtherValueEmptyTest() throws Exception {
		Map<String, String> props = new HashMap<String, String>(1);
		props.put(ParserProperties.PROP_PATTERN, "");
		activityNameValueParser.setProperties(props.entrySet());
		assertNull(activityNameValueParser.pattern);
	}

	@Test
	public void setPropertiesWhenNotEqualsNameTest() throws Exception {
		Map<String, String> props = new HashMap<String, String>(1);
		props.put(ParserProperties.PROP_NAMESPACE, "Test");
		activityNameValueParser.setProperties(props.entrySet());
		assertTrue(activityNameValueParser.stripQuotes);
	}
}
