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

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * Provides functionality to calculate duration between two events in milliseconds.
 *
 * @version $Revision: 1 $
 */
public class Duration {
	private long startTime;

	private Duration() {
	}

	/**
	 * Sets period {@code "start time"} to current system time in milliseconds for this duration instance.
	 *
	 * @see #now()
	 */
	public void set() {
		this.startTime = now();
	}

	/**
	 * Calculates duration between {@code "now"} and {@code "start time"} in milliseconds.
	 *
	 * @return duration value in milliseconds
	 *
	 * @see #duration(long)
	 */
	public long duration() {
		return duration(this.startTime);
	}

	/**
	 * Calculates duration between {@code "now"} and {@code "start time"} using {@code tUnit} defined units.
	 *
	 * @return duration value in {@code tUnit} defined units
	 *
	 * @see #duration(long, java.util.concurrent.TimeUnit)
	 */
	public long duration(TimeUnit tUnit) {
		return duration(this.startTime, tUnit);
	}

	/**
	 * Represents duration value in human readable form: {@code "hours:minutes:seconds.millis"}
	 *
	 * @return human readable duration value string
	 *
	 * @see #durationHMS(long)
	 */
	public String durationHMS() {
		return durationHMS(this.startTime);
	}

	/**
	 * Calculates duration between {@code "now"} and {@code "start time"} in milliseconds, and resets period
	 * {@code "start time"} to {@code "now"} for this duration instance.
	 *
	 * @return duration value in milliseconds
	 */
	public long reset() {
		long now = now();
		long duration = now - startTime;
		startTime = now;

		return duration;
	}

	/**
	 * Returns system current time in milliseconds.
	 *
	 * @return system current time in milliseconds
	 *
	 * @see System#currentTimeMillis()
	 */
	public static long now() {
		return System.currentTimeMillis();
	}

	/**
	 * Constructs a new duration object instance and sets duration period {@code "start time"} value to current system
	 * time in milliseconds.
	 *
	 * @return constructed duration instance
	 *
	 * @see #set()
	 */
	public static Duration arm() {
		Duration d = new Duration();
		d.set();

		return d;
	}

	/**
	 * Calculates duration between {@code "now"} and provided {@code "startTime"} in milliseconds.
	 *
	 * @param startTime
	 *            duration period start time in milliseconds
	 * @return duration value in milliseconds
	 *
	 * @see #now()
	 */
	public static long duration(long startTime) {
		return now() - startTime;
	}

	/**
	 * Calculates duration between {@code "now"} and provided {@code "startTime"} using {@code tUnit} defined units.
	 *
	 * @param startTime
	 *            duration period start time in milliseconds
	 * @param tUnit
	 *            duration period time units
	 * @return duration value in {@code tUnit} defined units
	 *
	 * @see #duration(long)
	 * @see java.util.concurrent.TimeUnit#convert(long, java.util.concurrent.TimeUnit)
	 */
	public static long duration(long startTime, TimeUnit tUnit) {
		return tUnit.convert(duration(startTime), TimeUnit.MILLISECONDS);
	}

	/**
	 * Represents duration value in human readable form: {@code "hours:minutes:seconds.millis"}
	 *
	 * @param startTime
	 *            duration period start time in milliseconds
	 * @return human readable duration value string
	 *
	 * @see #duration(long)
	 * @see org.apache.commons.lang3.time.DurationFormatUtils#formatDurationHMS(long)
	 */
	public static String durationHMS(long startTime) {
		return DurationFormatUtils.formatDurationHMS(duration(startTime));
	}
}
