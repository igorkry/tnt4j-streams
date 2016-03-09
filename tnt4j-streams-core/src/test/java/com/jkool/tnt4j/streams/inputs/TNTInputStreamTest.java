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

package com.jkool.tnt4j.streams.inputs;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamProperties;
import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.StreamsThread;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.tracker.Tracker;

/**
 * @author akausinis
 * @version 1.0
 */
public class TNTInputStreamTest {

	private EventSink logger;
	private ActivityParser parser;
	private TestStream ts;
	private StreamThread streamThread;
	public ActivityInfo ai;

	@Before
	public void initTest() {
		logger = mock(EventSink.class);
		parser = new TestActivityParser(logger);
		ai = mock(ActivityInfo.class);
		ts = new TestStream(logger);
		streamThread = new StreamThread(ts);
	}

	@Test
	public void recordActivityTest() throws Exception {
		StreamsThread inputStreamThread = new StreamsThread(ts);
		inputStreamThread.start();
		ts.addParser(parser);
		when(ai.isFiltered()).thenReturn(false);
		Thread.sleep(1000);
		verify(ai).recordActivity(any(Tracker.class), any(Long.class));
		ts.halt();
		ts.cleanup();

	}

	@Test
	public void setPropertiesTest() throws Exception {
		Collection<Map.Entry<String, String>> props = new ArrayList<Entry<String, String>>(6);
		props.add(new AbstractMap.SimpleEntry<String, String>(StreamProperties.PROP_HALT_ON_PARSER, "true"));
		props.add(new AbstractMap.SimpleEntry<String, String>(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "5"));
		props.add(new AbstractMap.SimpleEntry<String, String>(StreamProperties.PROP_USE_EXECUTOR_SERVICE, "true"));
		props.add(new AbstractMap.SimpleEntry<String, String>(
				StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT, "500"));
		props.add(new AbstractMap.SimpleEntry<String, String>(StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT,
				"500"));
		props.add(new AbstractMap.SimpleEntry<String, String>(StreamProperties.PROP_EXECUTORS_BOUNDED, "true"));

		ts.setProperties(props);
		for (Map.Entry<String, String> property : props) {
			String name = property.getKey();
			String expectedValue = property.getValue();
			assertEquals("Prperty not set as expected", expectedValue, ts.getProperty(name).toString());
		}

	}

	@Test
	public void testMakeActivityInfo() throws Exception {

		assertNull(ts.makeActivityInfo("TEST"));
	}

	@Test(expected = ParseException.class)
	public void testMakeActivityInfoFails() throws Exception {
		ts.addParser(parser);
		assertNotNull(ts.makeActivityInfo("TESTPARSEEXCEPTION"));
	}

	//
	@Test
	public void testApplyParsers() throws IllegalStateException, ParseException {
		final ActivityParser parser = mock(ActivityParser.class);
		String[] tags = { "TestTag" };
		String[] falseTags = { "TestTagNot" };
		when(parser.getTags()).thenReturn(tags);
		when(parser.parse(any(TNTInputStream.class), any())).thenReturn(new ActivityInfo());

		assertNull(ts.applyParsers(null));
		ts.addParser(parser);

		// Data class nor supported
		assertNull(ts.applyParsers("TEST"));

		when(parser.isDataClassSupported(any())).thenReturn(true);

		ActivityInfo ai = ts.applyParsers("TEST");
		assertNotNull(ai);

		ts.applyParsers(tags, "TEST");
		assertNotNull(ai);

		ts.applyParsers(falseTags, "TEST");

		verify(parser, times(3)).parse(any(TNTInputStream.class), any());

	}

	private class TestStream extends TNTInputStream<String> {
		// BlockingQueue<String> buffer = new ArrayBlockingQueue<String>(5);
		boolean used = false;

		protected TestStream(EventSink logger) {
			super(logger);
			// buffer.offer("TEST");

		}

		@Override
		public String getNextItem() throws Exception {
			if (used)
				return null;
			used = true;
			return "TEST";
		}

	}

	private class TestActivityParser extends ActivityParser {

		protected TestActivityParser(EventSink logger) {
			super(logger);
		}

		@Override
		public void setProperties(Collection<Entry<String, String>> props) throws Exception {
		}

		@Override
		public void addField(ActivityField field) {
		}

		@Override
		public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
			if (data.equals("TESTPARSEEXCEPTION"))
				throw new ParseException("TESTPARSEEXCEPTION", 0);
			return ai;
		}

		@Override
		public boolean isDataClassSupported(Object data) {
			return true;
		}

	}

}
