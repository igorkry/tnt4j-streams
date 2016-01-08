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
package com.jkool.tnt4j.streams.utils;

import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamTimestampTest {

	@Test(expected = IllegalArgumentException.class)
	public void testStreamTimestampException() throws ParseException {
		StreamTimestamp timestamp = new StreamTimestamp(null, "ANY", "ANY", "ANY");
	}

	@Test()
	public void testStreamTimestamp() throws ParseException {
		final Date now = new Date();
		final String testDate = String.valueOf(now.getTime());
		StreamTimestamp timestamp = new StreamTimestamp("15.01.06", "YY.MM.DD", "ANY", "ANY");

		final String defaultFormat = SimpleDateFormat.getDateInstance().format(now);
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.format(now).toString();
		timestamp = new StreamTimestamp(sdf.format(now).toString(), null, "ANY", "ANY");

		timestamp = new StreamTimestamp("13:03:15.22525", "HH:mm:ss.SSSSSS", "ANY", "ANY");

	}

	@Test(expected = ParseException.class)
	public void testStreamMicroSecondsError() throws ParseException {
		StreamTimestamp timestamp = new StreamTimestamp("13:03:15.22525", "HH:mm:ss.SSSSSSSSSSSS", "ANY", "ANY");

	}

	@Test
	public void testStreamTimestampDate() {
		assertNotNull(new StreamTimestamp(new Date()));
	}

	@Test
	public void testStreamTimestampLong() {
		assertNotNull(new StreamTimestamp((new Date()).getTime()));
	}

	@Test
	public void testStreamTimestampLongLong() {
		final int uSecs = 555;
		assertNotNull(new StreamTimestamp((new Date()).getTime(), uSecs));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testStreamTimestampLongLongFail() {
		final int uSecs = 5555;
		assertNotNull(new StreamTimestamp((new Date()).getTime(), uSecs));
	}

	@Test
	public void testStreamTimestampStreamTimestamp() throws CloneNotSupportedException {
		final int uSecs = 555;
		assertNotNull(new StreamTimestamp((new StreamTimestamp((new Date()).getTime(), uSecs))).clone());
	}
}
