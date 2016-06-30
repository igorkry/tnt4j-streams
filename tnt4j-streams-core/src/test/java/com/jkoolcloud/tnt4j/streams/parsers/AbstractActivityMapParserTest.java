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

import java.util.*;

import org.junit.Test;
import org.mockito.Mockito;

import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class AbstractActivityMapParserTest {
	@Test
	public void setPropertiesTest() throws Exception {
		AbstractActivityMapParser testParser = Mockito.mock(ActivityMapParser.class, Mockito.CALLS_REAL_METHODS);
		HashMap<String, String> myMap = new HashMap<String, String>();
		myMap.put(ParserProperties.PROP_VAL_DELIM, ";");
		myMap.put(ParserProperties.PROP_LOC_PATH_DELIM, "TEST_DELIM");
		Collection<Map.Entry<String, String>> props = myMap.entrySet();
		testParser.setProperties(props);
	}

	@Test
	public void parseWhenDataIsNullTest() throws Exception {
		AbstractActivityMapParser testParser = Mockito.mock(ActivityMapParser.class, Mockito.CALLS_REAL_METHODS);
		TNTInputStream my = Mockito.mock(TNTInputStream.class, Mockito.CALLS_REAL_METHODS);
		assertNull(testParser.parse(my, null));
	}

	@Test
	public void parseTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		Map<String, String> myMap = new HashMap<String, String>();
		myMap.put("test", "OK");
		myMap.put("status", "finished");
		testParser.parse(stream, myMap);
	}

	@Test
	public void parseWhenDataIsEmptyTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		Map<String, String> myMap = new HashMap<String, String>();
		assertNull(testParser.parse(stream, myMap));
	}

	@Test
	public void getLocatorValueTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "555");
		Map<String, String> myMap = new HashMap<String, String>();
		myMap.put("test", "OK");
		myMap.put("555", "hello");
		assertEquals("hello", testParser.getLocatorValue(stream, fieldLocator, myMap));
	}

	@Test
	public void getLocatorValueWhenFieldLocatorNullTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		Map<String, String> myMap = new HashMap<String, String>();
		myMap.put("test", "OK");
		myMap.put("status", "finished");
		assertNull(testParser.getLocatorValue(stream, null, myMap));
	}

	@Test
	public void getLocatorValueWhenLocatorEmptyTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "");
		Map<String, String> myMap = new HashMap<String, String>();
		myMap.put("test", "OK");
		myMap.put("status", "finished");
		assertNull(testParser.getLocatorValue(stream, fieldLocator, myMap));
	}

	@Test
	public void getLocatorValueTypeTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp, "333");
		Map<String, String> myMap = new HashMap<String, String>();
		myMap.put("test", "OK");
		myMap.put("status", "finished");
		assertNull(testParser.getLocatorValue(stream, fieldLocator, myMap));
	}

	@Test
	public void getLocatorValueWhenDataIsNullTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "333");
		assertNull(testParser.getLocatorValue(stream, fieldLocator, null));
	}

	@Test
	public void getLocatorValuePathTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "333.555");
		Map<String, Object> myMap = new HashMap<String, Object>();
		myMap.put("333", new HashMap<String, String>());
		myMap.put("555", Arrays.asList("test1"));
		assertNull(testParser.getLocatorValue(stream, fieldLocator, myMap));
	}

	@Test
	public void getLocatorValuePathListTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "333.0.222");
		Map<String, Object> testMap = new HashMap<String, Object>();
		testMap.put("test_key", "test_value");
		Map<String, Object> myMap = new HashMap<String, Object>();
		myMap.put("333", Arrays.asList(testMap, "test2", "test3"));
		myMap.put("status", "TEST");
		assertNull(testParser.getLocatorValue(stream, fieldLocator, myMap));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getLocatorValueNumberExceptionTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "333.test.222");
		Map<String, Object> myMap = new HashMap<String, Object>();
		myMap.put("333", Arrays.asList("test1", "test2", "test3"));
		myMap.put("status", "TEST");
		List<String> output = (List<String>) testParser.getLocatorValue(stream, fieldLocator,
				myMap);
		assertEquals(myMap.get("333"), output);
	}

	@Test
	public void getLocatorValueInstanceTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "333.test.222");
		Map<String, Object> myMap = new HashMap<String, Object>();
		myMap.put("333", "TEST1");
		assertEquals("TEST1", testParser.getLocatorValue(stream, fieldLocator, myMap));
	}

	@Test
	public void getLocatorValueEmptyLocatorTest() throws Exception {
		ActivityMapParser testParser = new ActivityMapParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, ".");
		Map<String, Object> myMap = new HashMap<String, Object>();
		myMap.put("333", "TEST1");
		assertNull(testParser.getLocatorValue(stream, fieldLocator, myMap));
	}
}
