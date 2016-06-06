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

package com.jkoolcloud.tnt4j.streams.inputs;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.tracker.Tracker;

/**
 * @author akausinis
 * @version 1.0
 */
public class TNTInputStreamTest extends InputTestBase {

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
		ts.addParser(parser);
		streamThread.start();
		when(ai.isFiltered()).thenReturn(false);

		Thread.sleep(500);
		verify(ai).recordActivity(any(Tracker.class), any(Long.class));
		ts.halt();
		ts.cleanup();

	}

	@Test
	public void setPropertiesTest() throws Exception {
		final Collection<Entry<String, String>> props = getPropertyList()
				.add(StreamProperties.PROP_HALT_ON_PARSER, "true").add(StreamProperties.PROP_HALT_ON_PARSER, "true")
				.add(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "5")
				.add(StreamProperties.PROP_USE_EXECUTOR_SERVICE, "true")
				.add(StreamProperties.PROP_DATETIME, new Date().toString())
				.add(StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT, "500")
				.add(StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT, "500")
				.add(StreamProperties.PROP_EXECUTORS_BOUNDED, "true").build();
		ts.setProperties(props);
		for (Map.Entry<String, String> property : props) {
			String name = property.getKey();
			String expectedValue = property.getValue();
			assertEquals("Property not set as expected", expectedValue, ts.getProperty(name).toString());
		}

	}
	
	@Test
	public void setPropertiesIfNullTest() throws Exception{
		TNTInputStream my = Mockito.mock(TNTInputStream.class, Mockito.CALLS_REAL_METHODS);
		my.setProperties(null);
		assertNull(my.getProperty("PROP_EXECUTORS_BOUNDED"));
	}
	
	@Test
	public void getBoundedExecutorServiceTest() throws Exception{
		final Collection<Entry<String, String>> props = getPropertyList()
				.add(StreamProperties.PROP_HALT_ON_PARSER, "true").add(StreamProperties.PROP_HALT_ON_PARSER, "true")
				.add(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "5")
				.add(StreamProperties.PROP_USE_EXECUTOR_SERVICE, "true")
				.add(StreamProperties.PROP_DATETIME, new Date().toString())
				.add(StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT, "500")
				.add(StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT, "500")
				.add(StreamProperties.PROP_EXECUTORS_BOUNDED, "true").build();
		ts.setProperties(props);
		ts.initialize();
	}
	
	@Test
	public void getDefaultExecutorServiceTest() throws Exception{
		final Collection<Entry<String, String>> props = getPropertyList()
				.add(StreamProperties.PROP_HALT_ON_PARSER, "true").add(StreamProperties.PROP_HALT_ON_PARSER, "true")
				.add(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "5")
				.add(StreamProperties.PROP_USE_EXECUTOR_SERVICE, "true")
				.add(StreamProperties.PROP_DATETIME, new Date().toString())
				.add(StreamProperties.PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT, "500")
				.add(StreamProperties.PROP_EXECUTORS_TERMINATION_TIMEOUT, "500")
				.add(StreamProperties.PROP_EXECUTORS_BOUNDED, "false").build();
		ts.setProperties(props);
		ts.initialize();
		ts.halt();
	}
	
	@Test(expected=IllegalStateException.class)
	public void runTest(){
		ts.ownerThread = null;
		ts.run();
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

	@Test
	public void testInputListeners() {
		final InputStreamListener inputStreamListenerMock = mock(InputStreamListener.class);
		ts.addStreamListener(inputStreamListenerMock);
		ts.notifyProgressUpdate(1, 10);
		verify(inputStreamListenerMock).onProgressUpdate(ts, 1, 10);
		ts.notifyStreamSuccess();
		verify(inputStreamListenerMock).onSuccess(ts);
		ts.notifyFailed("", new Exception(), "");
		verify(inputStreamListenerMock).onFailure(eq(ts), eq(""), any(Exception.class), eq(""));
		verify(inputStreamListenerMock).onStatusChange(ts, StreamStatus.FAILURE);
		ts.notifyStatusChange(StreamStatus.STARTED);
		verify(inputStreamListenerMock).onStatusChange(ts, StreamStatus.STARTED);
		ts.notifyFinished();
		verify(inputStreamListenerMock).onFinish(eq(ts), any(TNTInputStream.StreamStats.class));
		ts.notifyStreamEvent(OpLevel.NOTICE, "", "");
		verify(inputStreamListenerMock).onStreamEvent(ts, OpLevel.NOTICE, "", "");

	}

	@Test
	public void addInputTaskListenerNullTest(){
		final StreamTasksListener streamTaskListenerMock = mock(StreamTasksListener.class);
		final Runnable runnable = mock(Runnable.class);
		ts.addStreamTasksListener(null);
		ts.notifyStreamTaskRejected(runnable);
		verify(streamTaskListenerMock, never()).onReject(eq(ts), any(Runnable.class));
	}
	
	@Test
	public void addStreamListenerNullTest(){
		InputStreamListener inputStreamListenerMock = mock(InputStreamListener.class);
		ts.addStreamListener(null);
		ts.notifyProgressUpdate(1, 10);
		verify(inputStreamListenerMock, never()).onProgressUpdate(ts, 1, 10);
	}
	
	@Test
	public void removeStreamListenerTest(){
		final InputStreamListener inputStreamListenerMock = mock(InputStreamListener.class);
		ts.addStreamListener(inputStreamListenerMock);
		ts.removeStreamListener(inputStreamListenerMock);
		ts.notifyProgressUpdate(1, 10);
		verify(inputStreamListenerMock, never()).onProgressUpdate(ts, 1, 10);
	}
	
	@Test
	public void removeStreamTasksListenerTest(){
		final StreamTasksListener streamTaskListenerMock = mock(StreamTasksListener.class);
		final Runnable runnable = mock(Runnable.class);
		ts.addStreamTasksListener(streamTaskListenerMock);
		ts.removeStreamTasksListener(streamTaskListenerMock);
		ts.notifyStreamTaskRejected(runnable);
		verify(streamTaskListenerMock, never()).onReject(eq(ts), any(Runnable.class));
	}
	
	@Test
	public void streamStatsTest(){
		assertEquals(ts.getTotalActivities(), ts.getStreamStatistics().getActivitiesTotal());
		assertEquals(ts.getCurrentActivity(), ts.getStreamStatistics().getCurrActivity());
		assertEquals(ts.getTotalBytes(), ts.getStreamStatistics().getTotalBytes());
		assertEquals(ts.getStreamedBytesCount(), ts.getStreamStatistics().getBytesStreamed());
		assertEquals(ts.getElapsedTime(), ts.getStreamStatistics().getElapsedTime());
		assertEquals(ts.getSkippedActivitiesCount(), ts.getStreamStatistics().getSkippedActivities());
	}
	
	@Test
	public void cleanUpTest(){
		final InputStreamListener inputStreamListenerMock = mock(InputStreamListener.class);
		final StreamTasksListener streamTaskListenerMock = mock(StreamTasksListener.class);
		final Runnable runnable = mock(Runnable.class);
		final java.util.List<Runnable> list = Collections.singletonList(runnable);
		ts.addStreamListener(inputStreamListenerMock);
		ts.addStreamTasksListener(streamTaskListenerMock);
		ts.cleanup();
		ts.notifyStreamTasksDropOff(list);
		verify(streamTaskListenerMock, never()).onDropOff(ts, list);
		ts.notifyProgressUpdate(1, 10);
		verify(inputStreamListenerMock, never()).onProgressUpdate(ts, 1, 10);
		
	}
	
	@Test
	public void getOwnerThreadTest(){
		ts.ownerThread.setName("TEST_STREAM");
		assertEquals("TEST_STREAM", ts.getOwnerThread().getName());
	}
	
	@Test
	public void testInputTaskListener() {
		final StreamTasksListener streamTaskListenerMock = mock(StreamTasksListener.class);
		final Runnable runnable = mock(Runnable.class);
		ts.addStreamTasksListener(streamTaskListenerMock);
		ts.notifyStreamTaskRejected(runnable);
		verify(streamTaskListenerMock).onReject(eq(ts), any(Runnable.class));

		final java.util.List<Runnable> list = Collections.singletonList(runnable);
		ts.notifyStreamTasksDropOff(list);
		verify(streamTaskListenerMock).onDropOff(ts, list);

	}

	private class TestStream extends TNTParseableInputStream<String> {
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
		public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data)
				throws IllegalStateException, ParseException {
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
