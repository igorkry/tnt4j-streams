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

package com.jkoolcloud.tnt4j.streams.fields;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;
import com.jkoolcloud.tnt4j.uuid.UUIDFactory;

/**
 * @author akausinis
 * @version 1.0
 */
// Current coverage 71,1%
public class ActivityInfoTest {
	OpType[] supportedEventTypes = { OpType.ACTIVITY, OpType.SNAPSHOT, OpType.EVENT };

	@Test
	public void testApplyField() throws Exception {
		for (OpType opType : supportedEventTypes) {
			createTestTrackable(true, opType);
		}
	}

	private ActivityInfo createTestTrackable(Boolean test, OpType trackableType) throws Exception {
		ActivityInfo activityInfo = new ActivityInfo();
		for (StreamFieldType field : StreamFieldType.values()) {
			TestPair value = fillInField(field, activityInfo, trackableType);
			if (value == null) {
				continue;
			}

			Method method = ActivityInfo.class.getMethod("get" + field.name());
			if (test) {
				final Object result = method.invoke(activityInfo);
				assertEquals("Value not equal", value.valueExpected, result);
			}
		}
		return activityInfo;
	}

	private TestPair fillInField(StreamFieldType field, ActivityInfo activityInfo, OpType trackableType)
			throws ParseException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		ActivityField activityField = new ActivityField(field.name(), ActivityFieldDataType.String);
		TestPair valueT = new TestPair();
		switch (field) {
		// special cases
		case EventType:
			// in OpTypes there are more values than we handle in
			// Utils.mapOpType
			valueT.value = trackableType;
			break;
		case Severity:
			valueT.valueExpected = OpLevel.DEBUG;
			valueT.value = OpLevel.DEBUG.toString();
			break;
		case CompCode:
			valueT.valueExpected = OpCompCode.WARNING;
			valueT.value = OpCompCode.WARNING.toString();
			break;
		case Tag:
		case Correlator:
			String[] array = { "Cheese", "Pepperoni", "Black Olives" };
			valueT.value = Arrays.asList(array);
			break;
		case ServerIp:
			valueT.value = "127.0.0.1";
			break;
		default:
			// generic cases
			valueT.value = getTestValueForClass(field.getDataType());
			break;
		}
		if (valueT.valueExpected == null)
			valueT.valueExpected = valueT.value;

		activityInfo.applyField(activityField, valueT.value);
		System.out.println("Setting " + field.name() + " to " + valueT.value);
		return valueT;
	}

	@Test
	public void recordActivityTest() throws Exception {
		Tracker tracker = mock(Tracker.class);
		UUIDFactory uiFactory = mock(UUIDFactory.class);
		TrackerConfig tConfig = mock(TrackerConfig.class);
		TrackingEvent tEvent = mock(TrackingEvent.class);
		TrackingActivity tActivity = mock(TrackingActivity.class);
		PropertySnapshot snapshot = mock(PropertySnapshot.class);

		when(tracker.getConfiguration()).thenReturn(tConfig);
		when(tracker.newEvent(any(OpLevel.class), any(String.class), any(String.class), any(String.class),
				any(Object[].class))).thenReturn(tEvent);
		when(tracker.newActivity(any(OpLevel.class), any(String.class))).thenReturn(tActivity);
		when(tracker.newSnapshot(any(String.class))).thenReturn(snapshot);

		when(tConfig.getUUIDFactory()).thenReturn(uiFactory);
		when(uiFactory.newUUID()).thenReturn("TEST");
		when(tEvent.getOperation()).thenReturn(new Operation("TEST", OpType.SEND));

		ActivityInfo activityInfo = createTestTrackable(false, OpType.ACTIVITY);
		activityInfo.recordActivity(tracker, 50L);
		verify(tracker).tnt(any(TrackingActivity.class));

		activityInfo = createTestTrackable(false, OpType.EVENT);
		activityInfo.recordActivity(tracker, 50L);
		verify(tracker).tnt(any(TrackingEvent.class));

		activityInfo = createTestTrackable(false, OpType.SNAPSHOT);
		activityInfo.recordActivity(tracker, 50L);
		verify(tracker).tnt(any(PropertySnapshot.class));

		// Utils.close(verify(tracker));
	}

	@Test
	public void mergeTest() throws Exception {
		ActivityInfo activityInfo = new ActivityInfo();
		ActivityInfo activityInfoToMerge = new ActivityInfo();
		for (StreamFieldType field : StreamFieldType.values()) {
			fillInField(field, activityInfo, OpType.SEND);

			activityInfoToMerge.merge(activityInfo);

			Method method = ActivityInfo.class.getMethod("get" + field.name());
			assertEquals("Value not equal", method.invoke(activityInfo), method.invoke(activityInfoToMerge));
		}
	}

	private Object getTestValueForClass(Class<?> clazz) {
		final String className = clazz.getName();
		if (className.equals("java.lang.String")) {
			return "TEST";
		} else if (className.equals("[Ljava.lang.String;")) {
			return TestEnum.Skip;
		} else if (className.equals("java.lang.Integer")) {
			return 111;
		} else if (className.equals("java.lang.Long")) {
			return 111L;
		} else if (className.equals("java.lang.Enum")) {
			return TestEnum.Skip;
		} else if (className.equals("com.jkoolcloud.tnt4j.core.UsecTimestamp")) {
			return new UsecTimestamp(new Date());
		} else {
			fail("No such test case for class: " + className);
		}
		return null;
	}

	private static enum TestEnum {
		TestEnum1, Skip
	}

	private class TestPair {
		public Object value;
		public Object valueExpected;
	}

}
