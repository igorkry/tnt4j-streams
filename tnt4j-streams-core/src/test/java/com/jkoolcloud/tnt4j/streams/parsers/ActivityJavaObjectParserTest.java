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

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityJavaObjectParserTest {

	@Test
	public void setPropertiesTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		HashMap<String, String> myMap = new HashMap<>();
		myMap.put(ParserProperties.PROP_LOC_PATH_DELIM, "TEST_DELIM"); // NON-NLS
		Collection<Map.Entry<String, String>> props = myMap.entrySet();
		testParser.setProperties(props);
	}

	@Test
	public void setPropertiesWhenNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		testParser.setProperties(null);
	}

	@Test
	public void isDataClassSupportedTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		assertTrue(testParser.isDataClassSupported("TEST")); // NON-NLS
	}

	@Test
	public void parseTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		testParser.parse(stream, "test"); // NON-NLS
	}

	@Test
	public void parseWhenDataNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		assertNull(testParser.parse(stream, null));
	}

	@Test
	public void getLocatorValueExceptionTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "555"); // NON-NLS
		assertNull(testParser.getLocatorValue(stream, fieldLocator, ""));
	}

	@Test
	public void getLocatorValueWhenLocatorIsNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		assertNull(testParser.getLocatorValue(stream, null, ""));
	}

	@Test
	public void getLocatorValueWhenLocatorIsEmptyTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "");
		assertNull(testParser.getLocatorValue(stream, fieldLocator, ""));
	}

	@Test
	public void getLocatorValueWhenTypeisStreamPropTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp,
				"ExecutorThreadsQuantity"); // NON-NLS
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		Map<String, String> props = new HashMap<>(2);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(true));
		props.put(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "5");
		stream.setProperties(props.entrySet());
		assertEquals(5, testParser.getLocatorValue(stream, fieldLocator, ""));
	}

	@Test
	public void getFieldValueWhenDataIsNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "test"); // NON-NLS
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		assertNull(testParser.getLocatorValue(stream, fieldLocator, null));
	}

	@Test
	public void getFieldValueWhenPathIsNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "."); // NON-NLS
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		assertNull(testParser.getLocatorValue(stream, fieldLocator, ""));
	}

	@Test
	@Ignore("Not finished")
	public void getFieldValueTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index,
				"testName.testNumber"); // NON-NLS
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		MyClasTest prop = new MyClasTest();
		Object locValue = testParser.getLocatorValue(stream, fieldLocator, prop);
	}

	@Test
	@Ignore("Not finished")
	public void getFieldValueWhenTwoSameFieldsTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index,
				"testNumber.isActive"); // NON-NLS
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		MyClasTest prop = new MyClasTest();
		Object locValue = testParser.getLocatorValue(stream, fieldLocator, prop);
		assertNotNull(locValue);
		assertEquals("Excpected value does not match", prop.isActive, locValue);
	}

	@Test
	public void getFieldValueWhenOneFieldTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "testNumber"); // NON-NLS
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		MyClasTest prop = new MyClasTest();
		Object locValue = testParser.getLocatorValue(stream, fieldLocator, prop);
		assertNotNull(locValue);
		assertEquals("Excpected value does not match", prop.testNumber, locValue);
	}

	@Ignore("Not finished")
	@Test
	public void parsePreparedItemTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		AbstractBufferedStream<?> stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		testParser.parse(stream, "");
	}

	public class MyClasTest {
		public String testName = "Test Name"; // NON-NLS
		public int testNumber = 123456789;
		public boolean isActive = false;
	}
}