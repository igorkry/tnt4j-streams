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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.*;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.UtilsTest;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityXmlParserTest extends GenericActivityParserTestBase {

	private final static Object simpleString = "<MsgData format=\"string\" value=\"Message Body\"/>"; // NON-NLS
	private TNTInputStream<String, ActivityInfo> is;

	@Override
	@Before
	@SuppressWarnings("unchecked")
	public void prepare() {
		parser = new ActivityXmlParser();
		is = mock(TNTInputStream.class);
		final ActivityField field = mock(ActivityField.class);
		final ActivityFieldLocator locator = mock(ActivityFieldLocator.class);
		List<ActivityFieldLocator> locatorList = Collections.singletonList(locator);
		when(field.getLocators()).thenReturn(locatorList);
		when(locator.clone()).thenReturn(locator);
		when(locator.getLocator()).thenReturn("MsgData/@value"); // NON-NLS
		parser.addField(field);
	}

	@Test
	public void simpleStringParseTest() throws Exception {

		ActivityInfo ai = parser.parse(is, simpleString);
		assertNotNull(ai);
	}

	@Test
	public void simpleByteArrayParseTest() throws Exception {
		ActivityInfo ai = parser.parse(is, simpleString.toString().getBytes());
		assertNotNull(ai);
	}

	@Test
	public void simpleBufferedReaderParseTest() throws Exception {

		BufferedReader reader = new BufferedReader(new StringReader(simpleString.toString()));
		ActivityInfo ai = parser.parse(is, reader);
		assertNotNull(ai);
	}

	@Test
	public void simpleReaderParseTest() throws Exception {

		Reader reader = new StringReader(simpleString.toString());
		ActivityInfo ai = parser.parse(is, reader);
		assertNotNull(ai);
	}

	@Test
	public void simpleInputStreamParseTest() throws Exception {

		InputStream bis = new ByteArrayInputStream(simpleString.toString().getBytes());
		ActivityInfo ai = parser.parse(is, bis);
		assertNotNull(ai);
	}

	@Override
	@Test
	public void setPropertiesTest() throws Exception {
		setProperty(parser, ParserProperties.PROP_NAMESPACE, "NAMESPACE=asdcf"); // NON-NLS
		setProperty(parser, ParserProperties.PROP_REQUIRE_ALL, true);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void getLocatorValue() throws Exception {
		TNTInputStream<?, ?> stream = mock(TNTInputStream.class);
		ActivityFieldLocator locator = new ActivityFieldLocator("Label", "MsgData/@value"); // NON-NLS
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(UtilsTest.toInputStream(simpleString.toString()));

		// when(locator.getLocator()).thenReturn("MsgData");
		// when(locator.formatValue(any(Object.class))).thenCallRealMethod();
		//
		final Object locatorValue = ((ActivityXmlParser) parser).getLocatorValue(stream, locator, document);
		assertNotNull(locatorValue);
		// TODO
	}

}
