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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.*;

import org.junit.Test;

import com.jkool.tnt4j.streams.utils.UtilsTest;

/**
 * @author akausinis
 * @version 1.0
 */
public abstract class ActivityParserTestBase {
	ActivityParser parser;

	public abstract void prepare();

	public abstract void setPropertiesTest() throws Throwable;

	public abstract void isDataClassSupportedTest();

	public Collection<Map.Entry<String, String>> makeProperty(final String propertyName, final String testValue) {
		Collection<Map.Entry<String, String>> props = new ArrayList<Map.Entry<String, String>>() {
			{
				add(new AbstractMap.SimpleEntry(propertyName, String.valueOf(testValue)));
			}
		};
		return props;
	}

	public void setProperty(ActivityParser parser, final String propertyName, final Object testValue) throws Throwable {
		parser.setProperties(makeProperty(propertyName, testValue.toString()));
	}

	@Test
	public void setPropertiesNullFailtTest() throws Throwable {
		parser.setProperties(null);
	}

	@Test
	public void parserNameTest() {
		final String name = "Test";
		parser.setName(name);
		assertEquals(name, parser.getName());
	}

	// @Test
	// public void filterTest() {
	// final StreamFilter filter = mock(StreamFilter.class);
	// parser.addFilter(filter);
	// parser.addFilter(filter);
	// final ActivityInfo ai = mock(ActivityInfo.class);
	// parser.filterActivity(ai);
	// verify(filter, times(2)).doFilterActivity(ai);
	// }

	@Test
	public void tagsTest() {
		final String tag = "Test";
		parser.setTags(tag);
		assertEquals(tag, parser.getTags()[0]);
	}

	@Test
	public void getNextString() throws Throwable {

		final String testString = "Test\n";
		final String expectedString = "Test";
		final StringReader reader = UtilsTest.toReader(testString);
		final ByteArrayInputStream inputStream = UtilsTest.toInputStream(testString);
		List<Object> testCases = new ArrayList<Object>() {
			{
				add(expectedString);
				add(expectedString.getBytes());
				add(reader);
				add(inputStream);
			}
		};
		for (Object data : testCases) {
			System.out.println(data.getClass());
			assertEquals(expectedString, parser.getNextString(data));
		}
		assertNull(parser.getNextString(null));
	}

}
