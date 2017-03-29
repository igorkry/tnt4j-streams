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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.UtilsTest;

/**
 * @author akausinis
 * @version 1.0
 */
public abstract class GenericActivityParserTestBase extends ActivityParserTestBase {

	@Override
	@Test
	public void isDataClassSupportedTest() {
		assertTrue(parser.isDataClassSupported("TEST")); // NON-NLS
		assertTrue(parser.isDataClassSupported("TEST".getBytes())); // NON-NLS
		assertTrue(parser.isDataClassSupported(mock(Reader.class)));
		assertTrue(parser.isDataClassSupported(mock(InputStream.class)));
		assertFalse(parser.isDataClassSupported(this.getClass()));
	}

	public void parsePreparedItemTest() {
		mock(TNTInputStream.class);
	}

	@Test
	public void getNextString() throws Exception {
		GenericActivityParser<?> gParser = (GenericActivityParser<?>) parser;
		final String testString = "Test\n"; // NON-NLS
		final String expectedString = "Test"; // NON-NLS
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
			assertEquals(expectedString, gParser.getNextActivityString(data));
		}
		assertNull(gParser.getNextActivityString(null));
	}

	@Test
	public void getNextStringWhenBufferedReaderInstanceTest() {
		GenericActivityParser<?> gParser = (GenericActivityParser<?>) parser;
		InputStream textStream = new ByteArrayInputStream("test".getBytes()); // NON-NLS
		BufferedReader br = new BufferedReader(new InputStreamReader(textStream));
		assertEquals("test", gParser.getNextActivityString(br));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getNextStringWhenOtherInstanceTest() {
		GenericActivityParser<?> gParser = (GenericActivityParser<?>) parser;
		String stringToBeParsed = "Testing some tests"; // NON-NLS
		gParser.getNextActivityString(555);
	}

}
