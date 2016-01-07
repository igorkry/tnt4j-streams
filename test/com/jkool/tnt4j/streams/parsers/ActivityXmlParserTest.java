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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.*;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.UtilsTest;
import com.sun.org.apache.xerces.internal.impl.xs.opti.DefaultDocument;
import scala.util.parsing.combinator.testing.Str;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityXmlParserTest extends GenericActivityParserTestBase {

	private final static Object simpleString = "<MsgData format=\"string\">Message Body</MsgData>";
	private TNTInputStream<String> is;

	@SuppressWarnings("unchecked")
	@Override
	@Before
	public void prepare() {
		try {
			parser = new ActivityXmlParser();
		} catch (ParserConfigurationException e) {
		}
		is = mock(TNTInputStream.class);
		final ActivityField field = mock(ActivityField.class);
		final ActivityFieldLocator locator = mock(ActivityFieldLocator.class);
		List<ActivityFieldLocator> locatorList = Collections.singletonList(locator);
		when(field.getLocators()).thenReturn(locatorList);
		when(locator.getLocator()).thenReturn("MsgData");
		parser.addField(field);
	}

	@Test
	public void simpleStringParseTest() throws IllegalStateException, ParseException {

		ActivityInfo ai = parser.parse(is, simpleString);
		assertNotNull(ai);
	}

	@Test
	public void simpleByteArrayParseTest() throws IllegalStateException, ParseException {
		ActivityInfo ai = parser.parse(is, simpleString.toString().getBytes());
		assertNotNull(ai);
	}

	@Test
	public void simpleBufferedReaderParseTest() throws IllegalStateException, ParseException {

		BufferedReader reader = new BufferedReader(new StringReader(simpleString.toString()));
		ActivityInfo ai = parser.parse(is, reader);
		assertNotNull(ai);
	}

	@Test
	public void simpleReaderParseTest() throws IllegalStateException, ParseException {

		Reader reader = new StringReader(simpleString.toString());
		ActivityInfo ai = parser.parse(is, reader);
		assertNotNull(ai);
	}

	@Test
	public void simpleInputStreamParseTest() throws IllegalStateException, ParseException {

		InputStream reader = new ByteArrayInputStream(simpleString.toString().getBytes());
		ActivityInfo ai = parser.parse(is, reader);
		assertNotNull(ai);
	}

	@Override
	@Test
	public void setPropertiesTest() throws Throwable {
		setProperty(parser, StreamsConfig.PROP_NAMESPACE, "NAMESPACE=asdcf");
		setProperty(parser, StreamsConfig.PROP_REQUIRE_ALL, true);
	}
	
	@Test
	public void getLocatorValue() throws ParseException, ParserConfigurationException, SAXException, IOException {
		TNTInputStream stream = mock(TNTInputStream.class);
		ActivityFieldLocator locator = new ActivityFieldLocator("MsgData");
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(UtilsTest.toInputStream(simpleString.toString()));
		
//		when(locator.getLocator()).thenReturn("MsgData");
//		when(locator.formatValue(any(Object.class))).thenCallRealMethod();
//		
		final Object locatorValue = ((ActivityXmlParser) parser).getLocatorValue(stream, locator, document);
		assertNotNull(locatorValue);
		//TODO
	}

}
