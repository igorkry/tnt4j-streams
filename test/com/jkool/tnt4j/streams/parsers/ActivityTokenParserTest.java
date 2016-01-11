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


import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import java.text.ParseException;
import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityTokenParserTest extends GenericActivityParserTestBase {

	@Override
	@Test
	public void setPropertiesTest() throws Throwable {
		setProperty(parser, StreamsConfig.PROP_FLD_DELIM, ";");
		setProperty(parser, StreamsConfig.PROP_PATTERN, "\\S+");
		setProperty(parser, StreamsConfig.PROP_STRIP_QUOTES, true);

	}

	@Override
	@Before
	public void prepare() {
		parser = new ActivityTokenParser();
	}
	
	@Test
	public void testParse() throws Throwable {
		final TNTInputStream stream = mock(TNTInputStream.class);
		final Object data = "TEST";
		setPropertiesTest();
		assertNotNull(parser.parse(stream, data ));
	}

	@Test
	public void testParseDoensMatch() throws Throwable {
		final TNTInputStream stream = mock(TNTInputStream.class);
		final Object data = "TEST TTT";
		setPropertiesTest();
		assertNull(parser.parse(stream, data ));
	}
	
	
	@Test
	public void testGetLocatorValueAsProperty() throws ParseException {
		final TNTInputStream stream = mock(TNTInputStream.class);
		final ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.StreamProp, "TEST");
		((ActivityTokenParser)parser).getLocatorValue(stream, locator, null);
		verify(stream).getProperty(any(String.class));
	}
	
	@Test
	public void testGetLocatorAsIndex() throws ParseException {
		final TNTInputStream stream = mock(TNTInputStream.class);
		final ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "2");
		String[] fields = {"FAIL", "GOOD"};
		Object result = ((ActivityTokenParser)parser).getLocatorValue(stream, locator, fields );
		assertEquals("GOOD", result);
	}
}
