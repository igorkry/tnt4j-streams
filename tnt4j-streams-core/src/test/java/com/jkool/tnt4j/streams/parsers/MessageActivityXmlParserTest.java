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
import static org.mockito.Mockito.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;

import com.jkool.tnt4j.streams.configure.ParserProperties;
import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.fields.StreamFieldType;

/**
 * @author akausinis
 * @version 1.0
 */
public class MessageActivityXmlParserTest {

	private static final String DELIM = ",";

	@Test
	public void testProperties() throws Exception {
		Map<String, String> propertiesMap = new HashMap<String, String>() {
			{
				put(ParserProperties.PROP_SIG_DELIM, DELIM);
			}
		};
		MessageActivityXmlParser parser = new MessageActivityXmlParser();
		parser.setProperties(propertiesMap.entrySet());
		assertEquals(DELIM, parser.sigDelim);
	}

	@Test
	public void testapplyFieldValue() throws ParserConfigurationException, ParseException {
		MessageActivityXmlParser parser = new MessageActivityXmlParser();
		ActivityInfo ai = mock(ActivityInfo.class);
		ActivityField field = mock(ActivityField.class);
		Object value = "1, TEST, TEST, TEST,TEST, TEST, TEST, TEST";
		when(field.getFieldType()).thenReturn(StreamFieldType.Correlator);
		parser.applyFieldValue(ai, field, value);
		verify(field).getFieldType();
	}

}
