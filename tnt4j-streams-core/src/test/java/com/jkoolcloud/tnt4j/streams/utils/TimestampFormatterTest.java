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

package com.jkoolcloud.tnt4j.streams.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class TimestampFormatterTest {

	@Test
	public void testConvert() throws ParseException {
		Number testNum = 100000L;
		for (TimeUnit fromUnits : TimeUnit.values()) {
			for (TimeUnit toUnits : TimeUnit.values()) {
				TimestampFormatter.convert(testNum, fromUnits, toUnits);
				TimestampFormatter.convert(testNum, toUnits, fromUnits);
			}
		}
	}

	@Test
	public void testParse() throws ParseException {
		Date date = new Date();
		TimestampFormatter formatter = new TimestampFormatter(TimeUnit.MILLISECONDS);
		assertNotNull(TimestampFormatter.parse(TimeUnit.MILLISECONDS, date));
		assertNotNull(formatter.parseAny(date));
		assertNotNull(TimestampFormatter.parse(TimeUnit.MICROSECONDS, Calendar.getInstance()));
		assertNotNull(formatter.parseAny(Calendar.getInstance()));
		assertNotNull(TimestampFormatter.parse(TimeUnit.SECONDS, date.getTime()));
		assertNotNull(formatter.parseAny(String.valueOf(date.getTime())));
		assertNotNull(formatter.parseAny(date.getTime()));
		// formatter.setPattern("# ", Locale.FRENCH.toString()); TODO
		// assertNotNull(formatter.parse(String.valueOf(date.getTime())));
		assertNotNull(TimestampFormatter.parse(TimeUnit.DAYS, 4.70));
		assertNotNull(TimestampFormatter.parse(TimeUnit.HOURS, 14.25));
		assertNotNull(TimestampFormatter.parse(TimeUnit.MINUTES, 37.35));
		assertNotNull(TimestampFormatter.parse(TimeUnit.SECONDS, 1469715537.366));
		assertNotNull(TimestampFormatter.parse(TimeUnit.MILLISECONDS, 1469715537366.751));
		assertNotNull(TimestampFormatter.parse(TimeUnit.MICROSECONDS, 15537366751.124));
		assertNotNull(TimestampFormatter.parse(TimeUnit.NANOSECONDS, 377366751124.642));
	}

	@Test(expected = ParseException.class)
	public void testParseExcepion() throws ParseException {
		TimestampFormatter.parse(TimeUnit.MICROSECONDS, "TEST"); // NON-NLS
	}

	@Test(expected = ParseException.class)
	public void testParseExcepion2() throws ParseException {
		TimestampFormatter formatter = new TimestampFormatter(TimeUnit.MILLISECONDS);
		formatter.parseAny(this);
	}

	@Test
	public void testTimeZone() {
		TimestampFormatter formatter = new TimestampFormatter(TimeUnit.MILLISECONDS);
		String timezone = TimeZone.getDefault().toString();
		formatter.setTimeZone(timezone);
		assertEquals(timezone, formatter.getTimeZone());
	}

	@Test
	public void testTimeUnitsShift() {
		TimeUnit tu = TimestampFormatter.shiftDown(TimeUnit.DAYS);
		assertEquals(TimeUnit.HOURS, tu);
		tu = TimestampFormatter.shiftDown(tu);
		assertEquals(TimeUnit.MINUTES, tu);
		tu = TimestampFormatter.shiftDown(tu);
		assertEquals(TimeUnit.SECONDS, tu);
		tu = TimestampFormatter.shiftDown(tu);
		assertEquals(TimeUnit.MILLISECONDS, tu);
		tu = TimestampFormatter.shiftDown(tu);
		assertEquals(TimeUnit.MICROSECONDS, tu);
		tu = TimestampFormatter.shiftDown(tu);
		assertEquals(TimeUnit.NANOSECONDS, tu);
		tu = TimestampFormatter.shiftDown(tu);
		assertEquals(TimeUnit.NANOSECONDS, tu);

		tu = TimestampFormatter.shiftUp(tu);
		assertEquals(TimeUnit.MICROSECONDS, tu);
		tu = TimestampFormatter.shiftUp(tu);
		assertEquals(TimeUnit.MILLISECONDS, tu);
		tu = TimestampFormatter.shiftUp(tu);
		assertEquals(TimeUnit.SECONDS, tu);
		tu = TimestampFormatter.shiftUp(tu);
		assertEquals(TimeUnit.MINUTES, tu);
		tu = TimestampFormatter.shiftUp(tu);
		assertEquals(TimeUnit.HOURS, tu);
		tu = TimestampFormatter.shiftUp(tu);
		assertEquals(TimeUnit.DAYS, tu);
		tu = TimestampFormatter.shiftUp(tu);
		assertEquals(TimeUnit.DAYS, tu);
	}
}
