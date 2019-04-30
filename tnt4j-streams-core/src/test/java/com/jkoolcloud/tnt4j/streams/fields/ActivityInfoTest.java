/*
 * Copyright 2014-2018 JKOOL, LLC.
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.streams.filters.DefaultValueFilter;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
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
	private static OpType[] supportedEventTypes = { OpType.ACTIVITY, OpType.SNAPSHOT, OpType.EVENT };

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

			if (test) {
				Object result = activityInfo.getFieldValue(field.name());
				assertEquals("Value not equal", value.valueExpected, result);
			}
		}
		activityInfo.setEventName("TestActivity");
		return activityInfo;
	}

	private TestPair fillInField(StreamFieldType field, ActivityInfo activityInfo, OpType trackableType)
			throws ParseException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		ActivityField activityField = new ActivityField(field.name());
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
			valueT.valueExpected = OpCompCode.SUCCESS;
			valueT.value = OpCompCode.SUCCESS.toString();
			break;
		case Tag:
		case Correlator:
			String[] array = { "Cheese", "Pepperoni", "Black Olives" }; // NON-NLS
			valueT.value = Arrays.asList(array);
			break;
		case ServerIp:
			valueT.value = "127.0.0.1"; // NON-NLS
			break;
		case EventStatus:
			valueT.valueExpected = ActivityStatus.END;
			valueT.value = ActivityStatus.END.toString();
			break;
		default:
			// generic cases
			valueT.value = getTestValueForClass(field.getDataType());
			break;
		}
		if (valueT.valueExpected == null) {
			valueT.valueExpected = valueT.value;
		}

		activityInfo.applyField(activityField, valueT.value);
		System.out.println("Setting " + field.name() + " to " + valueT.value); // NON-NLS
		return valueT;
	}

	@Test
	public void testBuildTrackable() throws Exception {
		Tracker tracker = mock(Tracker.class);
		UUIDFactory uiFactory = mock(UUIDFactory.class);
		TrackerConfig tConfig = mock(TrackerConfig.class);
		TrackingEvent tEvent = mock(TrackingEvent.class);
		TrackingActivity tActivity = mock(TrackingActivity.class);
		PropertySnapshot snapshot = mock(PropertySnapshot.class);

		when(tActivity.getName()).thenReturn("TestActivity");

		when(tracker.getConfiguration()).thenReturn(tConfig);
		when(tracker.newEvent(any(OpLevel.class), any(String.class), any(String.class), any(String.class),
				any(Object[].class))).thenReturn(tEvent);
		when(tracker.newEvent(any(OpLevel.class), any(String.class), nullable(String.class), nullable(String.class),
				nullable(Object[].class))).thenReturn(tEvent);
		when(tracker.newActivity(any(OpLevel.class), any(String.class))).thenReturn(tActivity);
		when(tracker.newSnapshot(any(String.class))).thenReturn(snapshot);
		when(tracker.newSnapshot(any(String.class), any(String.class))).thenReturn(snapshot);

		when(tConfig.getUUIDFactory()).thenReturn(uiFactory);
		when(uiFactory.newUUID()).thenReturn("TEST"); // NON-NLS
		when(tEvent.getOperation()).thenReturn(new Operation("TEST", OpType.SEND)); // NON-NLS

		ActivityInfo activityInfo = createTestTrackable(false, OpType.ACTIVITY);
		TrackingActivity ta = (TrackingActivity) activityInfo.buildTrackable(tracker);
		assertNotNull("Built tracking activity is null", ta);
		tracker.tnt(ta);

		activityInfo = createTestTrackable(false, OpType.EVENT);
		TrackingEvent te = (TrackingEvent) activityInfo.buildTrackable(tracker);
		assertNotNull("Built tracking event is null", te);
		tracker.tnt(te);

		activityInfo = createTestTrackable(false, OpType.SNAPSHOT);
		PropertySnapshot ps = (PropertySnapshot) activityInfo.buildTrackable(tracker);
		assertNotNull("Built property snapshot is null", ps);
		tracker.tnt(ps);

		Utils.close(verify(tracker));
	}

	@Test
	public void testMerge() throws Exception {
		ActivityInfo activityInfo = new ActivityInfo();
		ActivityInfo activityInfoToMerge = new ActivityInfo();
		for (StreamFieldType field : StreamFieldType.values()) {
			fillInField(field, activityInfo, OpType.SEND);

			activityInfoToMerge.merge(activityInfo);

			Object v1 = activityInfo.getFieldValue(field.name());
			Object v2 = activityInfoToMerge.getFieldValue(field.name());
			assertEquals("Value not equal", v1, v2);
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testAddCorrelatorNull() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("Correlator");
		// ActivityField af2 = new ActivityField("Correlator2");
		ActivityFieldLocator testLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		// testLocator.formatValue("1");

		ActivityFieldLocator locator = new ActivityFieldLocator();
		locator.setDataType(ActivityFieldDataType.Binary);
		DefaultValueFilter filter = new DefaultValueFilter("EXCLUDE", "IS", "string", "0000");

		// locator.setFilter(filterGroup);
		af.addLocator(locator);
		af.setRequired("false");

		Object[] byteZero = { new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, null };

		ai.applyField(af, "1111");
		ai.applyField(af, "2222");
		ai.applyField(af, null);
		ai.applyField(af, byteZero);

		System.out.println(ai.getFieldValue("Correlator"));

		assertTrue("Incorrect resolved correlators count",
				ai.getFieldValue("Correlator").toString().split(",").length == 3);
		// should not be [1111, 2222, 000000000000000000000000000000000000000000000000, null]
		// should be [1111, 2222, 000000000000000000000000000000000000000000000000]
	}

	private Object getTestValueForClass(Class<?> clazz) {
		String className = clazz.getName();
		if (className.equals("java.lang.String")) { // NON-NLS
			return "TEST"; // NON-NLS
		} else if (className.equals("[Ljava.lang.String;")) { // NON-NLS
			return TestEnum.Skip;
		} else if (className.equals("java.lang.Integer")) { // NON-NLS
			return 111;
		} else if (className.equals("java.lang.Long")) { // NON-NLS
			return 111L;
		} else if (className.equals("java.lang.Enum")) { // NON-NLS
			return TestEnum.Skip;
		} else if (className.equals("com.jkoolcloud.tnt4j.core.UsecTimestamp")) { // NON-NLS
			return new UsecTimestamp(new Date());
		} else {
			fail("No such test case for class: " + className); // NON-NLS
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

	/**
	 * Test applying field value with simple value
	 */
	@Test
	public void testApplyFieldSimpleValue() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		af.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator"));

		ai.applyField(af, "TestValue");
		Object value = ai.getFieldValue("TestField");
		assertEquals(value, "TestValue");
		assertEquals(value, ai.getFieldValue("TestField"));
	}

	/**
	 * Test applying field value with array value, should return simplified value
	 */
	@Test
	public void testApplyFieldSingleArrayValue() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		af.addLocator(locator);

		String[] fieldValue = { "TestValue" };
		ai.applyField(af, fieldValue);
		Object value = ai.getFieldValue("TestField");
		assertEquals(value, af.aggregateFieldValue(fieldValue, ai));
		assertEquals(value, ai.getFieldValue("TestField"));
		assertEquals(value, "TestValue");
	}

	/**
	 * Test applying field value with array value, should return concatenated value if locator =1
	 */
	@Test
	public void testApplyFieldMultipleArrayValue() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		af.addLocator(locator);

		String[] fieldValue = { "TestValue", "TestValue2" };
		ai.applyField(af, fieldValue);
		Object value = ai.getFieldValue("TestField");
		String testString = Arrays.toString(fieldValue);
		assertEquals(value, testString.substring(1, testString.length() - 1).replace(" ", ""));
		assertEquals(value, ai.getFieldValue("TestField"));
	}

	/**
	 * Test applying field value with single array value, should return first element
	 */
	@Test
	public void testApplyFieldSimpleArrayValueMultipleLocator() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator2");

		af.addLocator(locator);
		af.addLocator(locator2);

		String[] fieldValue = { "TestValue" };
		ai.applyField(af, fieldValue);
		Object value = ai.getFieldValue("TestField");
		String testString = Arrays.toString(fieldValue);
		assertEquals(value, fieldValue[0]);
		assertEquals(value, ai.getFieldValue("TestField"));
	}

	/**
	 * Test applying field value when value array size not equals locators size, should fail
	 */
	@Test(expected = ParseException.class)
	public void testApplyFieldArrayValueMultipleLocatorSizeMismatch() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		ActivityFieldLocator locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		ActivityFieldLocator locator2 = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator2");

		af.addLocator(locator);
		af.addLocator(locator2);

		String[] fieldValue = { "TestValue", "TestValue", "TestValue", };
		ai.applyField(af, fieldValue);
		Object value = ai.getFieldValue("TestField");
		String testString = Arrays.toString(fieldValue);
		assertEquals(value, fieldValue[0]);
		assertEquals(value, ai.getFieldValue("TestField"));
	}

	@Test
	public void testApplyFieldTransperentLocator() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		af.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator"));
		af.setTransparent(true);

		ai.applyField(af, "TestValue");
		Object value = ai.getFieldValue("TestField");
		assertEquals(value, "TestValue");
		assertEquals(value, ai.getFieldValue("TestField"));
		assertFalse(ai.toString().contains("TestValue"));
	}

	@Test
	public void testGenericFieldNumber() throws Exception {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		ActivityFieldLocator testLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		testLocator.setDataType(ActivityFieldDataType.Generic);
		af.addLocator(testLocator);

		ai.applyField(af, "1");
		assertTrue(ai.getFieldValue("TestField") instanceof Integer);
	}

	@Test
	public void testGenericFieldNumberFloat() throws Exception {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		ActivityFieldLocator testLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		testLocator.setDataType(ActivityFieldDataType.Generic);
		af.addLocator(testLocator);

		ai.applyField(af, "1.0333");
		assertTrue(ai.getFieldValue("TestField") instanceof Float);
	}

	@Test
	public void testGenericFieldNotNumber() throws Exception {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		ActivityFieldLocator testLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		// testLocator.formatValue("1");
		af.addLocator(testLocator);

		ai.applyField(af, "1aaa");
		assertTrue(ai.getFieldValue("TestField") instanceof String);
	}

	@Test
	public void testAddCorrelatorNull2() throws ParseException {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("Correlator");
		// ActivityField af2 = new ActivityField("Correlator2");
		ActivityFieldLocator testLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		// testLocator.formatValue("1");

		ActivityFieldLocator locator = new ActivityFieldLocator();
		locator.setDataType(ActivityFieldDataType.Binary);
		DefaultValueFilter filter = new DefaultValueFilter("EXCLUDE", "IS", "string", "0000");

		// locator.setFilter(filterGroup);
		af.addLocator(locator);
		af.setRequired("false");

		Object[] byteZero = { new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, null };

		ai.applyField(af, "1111");
		ai.applyField(af, "2222");
		ai.applyField(af, null);
		ai.applyField(af, byteZero);

		System.out.println(ai.getFieldValue("Correlator"));

		assertTrue("Incorrect resolved correlators count",
				ai.getFieldValue("Correlator").toString().split(",").length == 3);
		// should not be [1111, 2222, 000000000000000000000000000000000000000000000000, null]
		// should be [1111, 2222, 000000000000000000000000000000000000000000000000]
	}

	@Test
	public void testEmptyAsNull() throws Exception {
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("TestField");
		af.setEmptyAsNull(true);

		ai.applyField(af, new Object[] { "", null, "" });
		Object fVal = ai.getFieldValue("TestField");
		assertNull("Non-null value for field", fVal);

		List<Object> coll = new ArrayList<>();
		coll.add("");
		coll.add(null);
		coll.add("");
		ai.applyField(af, coll);
		fVal = ai.getFieldValue("TestField");
		assertNull("Non-null value for field", fVal);

		af.setEmptyAsNull(false);

		ai.applyField(af, new Object[] { "", null, "" });
		fVal = ai.getFieldValue("TestField");
		assertEquals("Unexpected field value", ",,", fVal);

		ai.applyField(af, coll);
		fVal = ai.getFieldValue("TestField");
		assertEquals("Unexpected field value", ",,", fVal);
	}

	@Test
	public void testTimestampAsHex() throws ParseException {
		Integer TEST_VALUE = 0x5cb9c0d8;
		System.out.printf("The decimal: %d \n", TEST_VALUE);
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("startTime");
		ActivityFieldLocator testLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		testLocator.setDataType(ActivityFieldDataType.Timestamp);
		testLocator.setUnits("Seconds");
		af.addLocator(testLocator);

		ai.applyField(af, TEST_VALUE);
		Object startTime = ai.getFieldValue("startTime");
		System.out.printf("The result: %s \n", startTime);
		assertEquals((UsecTimestamp) startTime, new UsecTimestamp(1555677400000000L));
		// assertEquals();
	}

	@Test
	public void testTimestampString() throws ParseException {
		String TEST_VALUE = "1555404504581";
		System.out.printf("The value: %s \n", TEST_VALUE);
		ActivityInfo ai = new ActivityInfo();
		ActivityField af = new ActivityField("startTime");
		ActivityFieldLocator testLocator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "TestLocator");
		testLocator.setDataType(ActivityFieldDataType.Timestamp);
		testLocator.setUnits("Milliseconds");
		af.addLocator(testLocator);

		ai.applyField(af, TEST_VALUE);
		Object startTime = ai.getFieldValue("startTime");
		System.out.printf("The result: %s \n", startTime);
		assertEquals(Long.parseLong(TEST_VALUE) * 1000, ((UsecTimestamp) startTime).getTimeUsec());
	}
}
