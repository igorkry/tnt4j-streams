/*
 * Copyright 2014-2017 JKOOL, LLC.
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

import org.apache.commons.lang3.StringUtils;
import org.quartz.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenario;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Implements a scheduled system command call activity stream, where each call response is assumed to represent a single
 * activity or event which should be recorded.
 * <p>
 * System command call is performed by invoking {@link Runtime#exec(String)}.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports properties from {@link AbstractWsStream} (and higher hierarchy streams).
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see java.lang.Runtime#exec(String)
 */
public class CmdStream extends AbstractWsStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(CmdStream.class);

	/**
	 * Constructs an empty CmdStream. Requires configuration settings to set input stream source.
	 */
	public CmdStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected JobDetail buildJob(WsScenario scenario, WsScenarioStep step, JobDataMap jobAttrs) {
		jobAttrs.put(JOB_PROP_REQ_KEY, step.getRequest());

		return JobBuilder.newJob(CmdCallJob.class).withIdentity(scenario.getName() + ':' + step.getName()) // NON-NLS
				.usingJobData(jobAttrs).build();
	}

	/**
	 * Performs system command call.
	 * 
	 * @param cmdData
	 *            command data: name and parameters
	 * @return command response string
	 * @throws Exception
	 *             if exception occurs while performing system command call
	 */
	protected static String executeCommand(String cmdData) throws Exception {
		if (StringUtils.isEmpty(cmdData)) {
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "CmdStream.cant.execute.cmd"),
					cmdData);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "CmdStream.invoking.command"),
				cmdData);

		Process p = Runtime.getRuntime().exec(cmdData);

		return Utils.readInput(p.getInputStream(), false);
	}

	/**
	 * Scheduler job to execute system command call.
	 */
	public static class CmdCallJob implements Job {

		/**
		 * Constructs a new CmdCallJob.
		 */
		public CmdCallJob() {
		}

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			String respStr = null;

			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			AbstractWsStream stream = (AbstractWsStream) dataMap.get(JOB_PROP_STREAM_KEY);
			String reqData = dataMap.getString(JOB_PROP_REQ_KEY);

			try {
				respStr = executeCommand(reqData);
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
						"CmdStream.execute.exception"), exc);
			}

			if (StringUtils.isNotEmpty(respStr)) {
				stream.addInputToBuffer(respStr);
			}
		}
	}
}
