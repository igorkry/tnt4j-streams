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

package com.jkool.tnt4j.streams.scenario;

import java.util.concurrent.TimeUnit;

/**
 * This class defines TNT4J-Streams-WS configuration simple scheduler
 * configuration data.
 *
 * @version $Revision: 1 $
 */
public class SimpleSchedulerData implements SchedulerData {
	private int interval = 0;
	private TimeUnit units;
	private Integer repeatCount;

	/**
	 * Constructs a new SimpleSchedulerData. Defines invocations interval in
	 * milliseconds.
	 *
	 * @param interval
	 *            request/call/command invocations interval.
	 */
	public SimpleSchedulerData(int interval) {
		this.interval = interval;
		this.units = TimeUnit.MILLISECONDS;
	}

	/**
	 * Returns request/call/command invocations interval
	 *
	 * @return request /call/command invocations interval.
	 */
	public int getInterval() {
		return interval;
	}

	/**
	 * Returns invocation interval time units.
	 *
	 * @return interval time units.
	 */
	public TimeUnit getUnits() {
		return units;
	}

	/**
	 * Sets invocation interval time units.
	 *
	 * @param units
	 *            interval time units.
	 */
	public void setUnits(TimeUnit units) {
		this.units = units;
	}

	/**
	 * Returns invocations count.
	 *
	 * @return invocations count
	 */
	public Integer getRepeatCount() {
		return repeatCount;
	}

	/**
	 * Sets invocations count.
	 *
	 * @param repeatCount
	 *            invocations count
	 */
	public void setRepeatCount(Integer repeatCount) {
		this.repeatCount = repeatCount;
	}
}
