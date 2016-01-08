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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.StringTokenizer;

import javax.jms.*;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityJMSMessageParserTest extends ActivityMapParserTest {

	@Test
	@Override
	public void isDataClassSupportedTest() {
		parser = new ActivityJMSMessageParser();
		assertTrue(parser.isDataClassSupported(mock(javax.jms.Message.class)));
		assertFalse(parser.isDataClassSupported(String.class));
	}

	@Test
	public void testDataMap() throws JMSException {
		parser = new ActivityJMSMessageParser();
		final TextMessage message = mock(TextMessage.class);
		final String string = "TEST";
		when(message.getText()).thenReturn(string);
		((ActivityJMSMessageParser) parser).getDataMap(message);

		final BytesMessage messageB = mock(BytesMessage.class);
		((ActivityJMSMessageParser) parser).getDataMap(messageB);
		verify(messageB).readBytes(any(byte[].class));

		final MapMessage messageM = mock(MapMessage.class);
		StringTokenizer tokenizer = new StringTokenizer("TEST,TEST,TEST", ",");
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

}
