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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.jkoolcloud.tnt4j.streams.PropertiesTestBase;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;
import com.jkoolcloud.tnt4j.streams.inputs.CamelBufferedStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityJavaObjectParserTest extends PropertiesTestBase {

	@Test
	public void setPropertiesTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		HashMap<String, String> myMap = new HashMap<String, String>();
		myMap.put(ParserProperties.PROP_LOC_PATH_DELIM, "TEST_DELIM");
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
		assertTrue(testParser.isDataClassSupported("TEST"));
	}

	@Test
	public void parseTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		testParser.parse(mock(CamelBufferedStream.class), "test");
	}

	@Test
	public void parseWhenDataNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		assertNull(testParser.parse(mock(CamelBufferedStream.class), null));
	}

	@Test
	public void getLocatorValueExceptionTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "555");
		assertNull(testParser.getLocatorValue(mock(CamelBufferedStream.class), fieldLocator, ""));
	}

	@Test
	public void getLocatorValueWhenLocatorIsNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		assertNull(testParser.getLocatorValue(mock(CamelBufferedStream.class), null, ""));
	}

	@Test
	public void getLocatorValueWhenLocatorIsEmptyTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "");
		assertNull(testParser.getLocatorValue(mock(CamelBufferedStream.class), fieldLocator, ""));
	}

	@Test
	public void getLocatorValueWhenTypeisStreamPropTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp,
				"ExecutorThreadsQuantity");
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		final Collection<Entry<String, String>> props = getPropertyList()
				.add(StreamProperties.PROP_HALT_ON_PARSER, "true").add(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "5")
				.build();
		stream.setProperties(props);
		assertEquals(5, testParser.getLocatorValue(stream, fieldLocator, ""));
	}

	@Test
	public void getFieldValueWhenDataIsNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "test");
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		assertNull(testParser.getLocatorValue(stream, fieldLocator, null));
	}

	@Test
	public void getFieldValueWhenPathIsNullTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, ".");
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		assertNull(testParser.getLocatorValue(stream, fieldLocator, ""));
	}

	@Test
	public void getFieldValueTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index,
				"testName.testNumber");
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		MyClasTest prop = new MyClasTest();
		testParser.getLocatorValue(stream, fieldLocator, prop);
	}

	@Test
	public void getFieldValueWhenTwoSameFieldsTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index,
				"testNumber.isActive");
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		MyClasTest prop = new MyClasTest();
		assertNotNull(testParser.getLocatorValue(stream, fieldLocator, prop));
	}

	@Test
	public void getFieldValueWhenOneFieldTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		ActivityFieldLocator fieldLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "testNumber");
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		MyClasTest prop = new MyClasTest();
		assertNotNull(testParser.getLocatorValue(stream, fieldLocator, prop));
	}

	@Ignore("Not finished")
	@Test
	public void parsePreparedItemTest() throws Exception {
		ActivityJavaObjectParser testParser = new ActivityJavaObjectParser();
		AbstractBufferedStream stream = Mockito.mock(AbstractBufferedStream.class, Mockito.CALLS_REAL_METHODS);
		testParser.parse(stream, "");
	}

	public class MyClasTest {
		public String testName = "Test Name";
		public int testNumber = 123456789;
		public boolean isActive = false;
	}
}