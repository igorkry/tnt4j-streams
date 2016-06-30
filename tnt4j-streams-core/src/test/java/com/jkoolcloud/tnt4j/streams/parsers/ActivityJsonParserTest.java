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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jkoolcloud.tnt4j.streams.PropertiesTestBase;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityJsonParserTest extends PropertiesTestBase {

	@Test
	public void setPropertiesWhenNullTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		// final Collection<Entry<String, String>> props =
		// getPropertyList().build();
		parser.setProperties(null);
	}

	@Test
	public void setPropertiesWhenPropDoesNotMatchTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		final Collection<Entry<String, String>> props = getPropertyList()
				.add(StreamProperties.PROP_HALT_ON_PARSER, "true").build();
		parser.setProperties(props);
	}

	@Test
	public void parseWhenDataIsNullTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		assertNull(parser.parse(stream, null));
	}

	@Test(expected = ParseException.class)
	public void parseExceptionTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		Map<String, String> myMap = new HashMap<String, String>();
		parser.parse(stream, myMap);
	}

	@Ignore("Not finished")
	@Test
	public void parseTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityField field = new ActivityField("test_name", ActivityFieldDataType.String);
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}";
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		parser.addField(field);
		parser.parse(stream, jsonContext);
	}

	@Test
	public void parseWhenStringIsEmptyTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		String jsonString = "";
		assertNull(parser.parse(stream, jsonString));
	}

	@Test
	public void getLocatorValueTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "test");
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}";
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		parser.getLocatorValue(stream, aLocator, jsonContext);
	}

	@Test
	public void getLocatorValueWhenLocatorIsNullTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}";
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		assertNull(parser.getLocatorValue(stream, null, jsonContext));
	}

	@Test
	public void getLocatorValueWhenLocatorEmptyTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "");
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}";
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		assertNull(parser.getLocatorValue(stream, aLocator, jsonContext));
	}

	@Test
	public void getLocatorValueWhenTypeExpectedTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		final Collection<Entry<String, String>> props = getPropertyList()
				.add(StreamProperties.PROP_HALT_ON_PARSER, "true").add(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "5")
				.build();
		stream.setProperties(props);
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp,
				"ExecutorThreadsQuantity");
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}";
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		assertEquals(5, parser.getLocatorValue(stream, aLocator, jsonContext));
	}

	@Test
	public void getLocatorValueStartsWithJsonPathTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "$.test");
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\"}";
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		assertEquals("OK", parser.getLocatorValue(stream, aLocator, jsonContext));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getLocatorValueJsonPathIsListTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "$.values");
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\",\"values\":[4, 5, 6]}";
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		List<Object> testData = new ArrayList<Object>(Arrays.asList(4, 5, 6));
		testData.equals(parser.getLocatorValue(stream, aLocator, jsonContext));
	}

	@Test
	public void getLocatorValueJsonPathIsEmptyListTest() throws Exception {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator aLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "$.values");
		String jsonString = "{\"test\":\"OK\",\"status\":\"finished\",\"values\":[]}";
		DocumentContext jsonContext = JsonPath.parse(jsonString);
		List<Object> testData = new ArrayList<Object>(Arrays.asList());
		testData.equals(parser.getLocatorValue(stream, aLocator, jsonContext));
	}

	@Test
	public void getNextJSONStringWhenNullTest() {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		assertNull(parser.getNextJSONString(null, false));
	}

	@Test
	public void getNextJSONStringWhenByteArrayTest() {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		byte[] myvar = "testing some data".getBytes();
		assertEquals(Utils.getString((byte[]) myvar), parser.getNextJSONString(myvar, false));
	}

	@Test
	public void getNextJSONStringWhenBufferedReaderTest() {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		InputStream textStream = new ByteArrayInputStream("testing some data".getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(textStream));
		assertEquals("testing some data", parser.getNextJSONString(br, false));
	}

	@Test
	public void getNextJSONStringWhenBufferedReaderTrueTest() {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		InputStream textStream = new ByteArrayInputStream("testing some data".getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(textStream));
		assertEquals("testing some data", parser.getNextJSONString(br, true));
	}

	@Test
	public void getNextJSONStringWhenReaderTest() {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		String stringToBeParsed = "Testing some tests";
		StringReader reader = new StringReader(stringToBeParsed);
		assertEquals("Testing some tests", parser.getNextJSONString(reader, false));
	}

	@Test
	public void getNextJSONStringWhenInputStreamTest() {
		ActivityJsonParser parser = Mockito.mock(ActivityJsonParser.class, Mockito.CALLS_REAL_METHODS);
		String stringToBeParsed = "Testing";
		InputStream inputStream = new ByteArrayInputStream(stringToBeParsed.getBytes());
		assertEquals("Testing", parser.getNextJSONString(inputStream, false));
	}

}
