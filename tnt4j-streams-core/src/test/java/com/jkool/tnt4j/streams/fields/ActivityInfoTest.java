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

package com.jkool.tnt4j.streams.fields;

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

import com.nastel.jkool.tnt4j.config.TrackerConfig;
import com.nastel.jkool.tnt4j.core.*;
import com.nastel.jkool.tnt4j.tracker.Tracker;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;
import com.nastel.jkool.tnt4j.uuid.UUIDFactory;

/**
 * @author akausinis
 * @version 1.0
 */
// Current coverage 71,1%
public class ActivityInfoTest {
	@Test
	public void testApplyField() throws ParseException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		createTestActivity(true);
	}

	private ActivityInfo createTestActivity(Boolean test) throws ParseException, NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ActivityInfo activityInfo = new ActivityInfo();
		for (StreamFieldType field : StreamFieldType.values()) {
			TestPair value = fillInField(field, activityInfo);
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

	private TestPair fillInField(StreamFieldType field, ActivityInfo activityInfo)
			throws ParseException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		ActivityField activityField = new ActivityField(field.name(), ActivityFieldDataType.String);
		TestPair valueT = new TestPair();
		switch (field) {
		// special cases
		case EventType:
			// in OpTypes there are more values than we handle in
			// Utils.mapOpType
			valueT.value = OpType.SEND;
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

		when(tracker.getConfiguration()).thenReturn(tConfig);
		when(tracker.newEvent(any(OpLevel.class), any(String.class), any(String.class), any(String.class),
				any(Object[].class))).thenReturn(tEvent);
		when(tConfig.getUUIDFactory()).thenReturn(uiFactory);
		when(uiFactory.newUUID()).thenReturn("TEST");
		when(tEvent.getOperation()).thenReturn(new Operation("TEST", OpType.SEND));
		ActivityInfo activityInfo = createTestActivity(false);
		activityInfo.recordActivity(tracker, 50L);

		verify(tracker).tnt(any(TrackingEvent.class));
		// Utils.close(verify(tracker));
	}

	@Test
	public void mergeTest() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, ParseException {
		ActivityInfo activityInfo = new ActivityInfo();
		ActivityInfo activityInfoToMerge = new ActivityInfo();
		for (StreamFieldType field : StreamFieldType.values()) {
			fillInField(field, activityInfo);

			activityInfoToMerge.merge(activityInfo);

			Method method = ActivityInfo.class.getMethod("get" + field.name());
			assertEquals("Value not equal", method.invoke(activityInfo), method.invoke(activityInfoToMerge));
		}
	}

	private Object getTestValueForClass(Class clazz) {
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
		} else if (className.equals("com.nastel.jkool.tnt4j.core.UsecTimestamp")) {
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
