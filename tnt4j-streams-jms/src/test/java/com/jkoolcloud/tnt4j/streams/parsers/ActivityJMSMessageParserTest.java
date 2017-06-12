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
import static org.mockito.Mockito.*;

import java.util.StringTokenizer;

import javax.jms.*;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.utils.JMSStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityJMSMessageParserTest extends ActivityMapParserTest {

	@Override
	@Before
	public void prepare() {
		parser = new ActivityJMSMessageParser();
	}

	@Test
	@Override
	public void isDataClassSupportedTest() {
		assertTrue("javax.jms.Message.class shall be supported ", parser.isDataClassSupported(mock(Message.class)));
		assertFalse("ActivityJMSMessageParser does not support Strings", parser.isDataClassSupported(String.class));
	}

	@Test
	@Override
	public void getDataMapTest() throws JMSException {
		final TextMessage message = mock(TextMessage.class);
		final String string = "TEST"; // NON-NLS
		when(message.getText()).thenReturn(string);
		((ActivityJMSMessageParser) parser).getDataMap(message);

		final BytesMessage messageB = mock(BytesMessage.class);
		((ActivityJMSMessageParser) parser).getDataMap(messageB);
		verify(messageB).readBytes(any(byte[].class));

		final MapMessage messageM = mock(MapMessage.class);
		StringTokenizer tokenizer = new StringTokenizer("TEST,TEST,TEST", ","); // NON-NLS
		when(messageM.getMapNames()).thenReturn(tokenizer);
		((ActivityJMSMessageParser) parser).getDataMap(messageM);
		verify(messageM, times(3)).getObject(anyString());

		final StreamMessage messageS = mock(StreamMessage.class);
		((ActivityJMSMessageParser) parser).getDataMap(messageS);
		verify(messageS).readBytes(any(byte[].class));

		final ObjectMessage messageO = mock(ObjectMessage.class);
		((ActivityJMSMessageParser) parser).getDataMap(messageO);
		verify(messageO).getObject();

	}

	@Test
	public void testRB() {
		String keyModule = "ActivityJMSMessageParser.payload.data.error";
		String keyCore = "ActivityField.field.type.name.empty";

		String rbs1 = StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME, keyModule);
		assertNotEquals("JMS resource bundle entry not found", keyModule, rbs1);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyModule);
		assertEquals("JMS resource bundle entry found in core", keyModule, rbs1);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyCore);
		assertNotEquals("Core resource bundle entry not found", keyCore, rbs1);
		rbs1 = StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME, keyCore);
		assertEquals("Core resource bundle entry found in jms", keyCore, rbs1);
	}

}
