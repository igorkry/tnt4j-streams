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

package com.jkool.tnt4j.streams.inputs;

import org.apache.commons.lang3.StringUtils;
import org.quartz.*;

import com.jkool.tnt4j.streams.fields.WsScenario;
import com.jkool.tnt4j.streams.fields.WsScenarioStep;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.jkool.tnt4j.streams.utils.WsStreamConstants;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * @author akausinis
 * @version 1.0 TODO
 */
public class CmdStream extends AbstractWsStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(CmdStream.class);

	/**
	 * Constructs an empty CmdStream. Requires configuration settings to set
	 * input stream source.
	 */
	public CmdStream() {
		super(LOGGER);
	}

	@Override
	protected JobDetail buildJob(WsScenario scenario, WsScenarioStep step, JobDataMap jobAttrs) {
		return JobBuilder.newJob(CmdCallJob.class).withIdentity(scenario.getName() + ":" + step.getName()) // NON-NLS
				.usingJobData(jobAttrs).usingJobData(JOB_PROP_REQ_KEY, step.getRequest()).build();
	}

	/**
	 * TODO
	 * 
	 * @param reqData
	 * @return
	 * @throws Exception
	 */
	protected static String executeCommand(String reqData) throws Exception {
		if (StringUtils.isEmpty(reqData)) {
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "CmdStream.cant.execute.cmd"),
					reqData);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "CmdStream.invoking.command"),
				reqData);

		Process p = Runtime.getRuntime().exec(reqData);

		return Utils.readInput(p.getInputStream(), false);
	}

	public static class CmdCallJob implements Job {

		public CmdCallJob() {
		}

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			String respStr = null;

			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			AbstractBufferedStream stream = (AbstractBufferedStream) dataMap.get(JOB_PROP_STREAM_KEY);
			String reqData = dataMap.getString(JOB_PROP_REQ_KEY);

			try {
				respStr = executeCommand(reqData);
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING,
						StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "CmdStream.execute.exception"),
						exc);
			}

			if (StringUtils.isNotEmpty(respStr)) {
				stream.addInputToBuffer(respStr);
			}
		}
	}
}
