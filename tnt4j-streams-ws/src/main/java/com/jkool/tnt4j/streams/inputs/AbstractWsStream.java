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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import com.jkool.tnt4j.streams.fields.CronSchedulerData;
import com.jkool.tnt4j.streams.fields.SimpleSchedulerData;
import com.jkool.tnt4j.streams.fields.WsScenario;
import com.jkool.tnt4j.streams.fields.WsScenarioStep;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.WsStreamConstants;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Base class for scheduled service or system command request/call produced
 * activity stream, where each call/request responce is assumed to represent a
 * single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractWsStream extends AbstractBufferedStream<String> {

	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_STREAM_KEY = "streamObj"; // NON-NLS
	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_URL_KEY = "urlStr"; // NON-NLS
	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_REQ_KEY = "reqData"; // NON-NLS

	private List<WsScenario> scenarioList;

	private Scheduler scheduler;

	/**
	 * Constructs a new AbstractWsStream.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected AbstractWsStream(EventSink logger) {
		super(logger);
	}

	// /**
	// * {@inheritDoc}
	// */
	// @Override
	// public Object getProperty(String name) {
	//
	// return super.getProperty(name);
	// }
	//
	// /**
	// * {@inheritDoc}
	// */
	// @Override
	// public void setProperties(Collection<Map.Entry<String, String>> props)
	// throws Exception {
	// if (props == null) {
	// return;
	// }
	//
	// super.setProperties(props);
	//
	// for (Map.Entry<String, String> prop : props) {
	// String name = prop.getKey();
	// String value = prop.getValue();
	//
	// }
	// }

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();

		logger.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "AbstractWsStream.stream.initialized"),
				getName());

		loadScenarios();

		logger.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "AbstractWsStream.stream.ready"),
				getName());
	}

	/**
	 * Loads scenario steps into scheduler.
	 *
	 * @throws Exception
	 *             If any exception occurs while loading scenario steps to
	 *             scheduler
	 */
	protected void loadScenarios() throws Exception {
		boolean hasStepsDefined = false;
		if (CollectionUtils.isNotEmpty(scenarioList)) {
			for (WsScenario scenario : scenarioList) {
				if (!scenario.isEmpty()) {
					for (WsScenarioStep step : scenario.getStepsList()) {
						scheduleScenarioStep(scenario, step);
						hasStepsDefined = true;
					}
				}
			}
		}

		if (!hasStepsDefined) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_WS,
					"AbstractWsStream.no.scenarios.defined", getName()));
		}
	}

	private void scheduleScenarioStep(WsScenario scenario, WsScenarioStep step) throws SchedulerException {
		if (scheduler == null) {
			throw new SchedulerException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_WS,
					"AbstractWsStream.null.scheduler", getName()));
		}

		JobDataMap jobAttrs = new JobDataMap();
		jobAttrs.put(JOB_PROP_STREAM_KEY, this);

		JobDetail job = buildJob(scenario, step, jobAttrs);

		ScheduleBuilder scheduleBuilder;

		if (step.getSchedulerData() instanceof CronSchedulerData) {
			CronSchedulerData csd = (CronSchedulerData) step.getSchedulerData();
			scheduleBuilder = CronScheduleBuilder.cronSchedule(csd.getExpression());
		} else {
			SimpleSchedulerData ssd = (SimpleSchedulerData) step.getSchedulerData();

			scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
					.withIntervalInMilliseconds(ssd == null ? 1
							: ssd.getUnits() == null ? ssd.getInterval() : ssd.getUnits().toMillis(ssd.getInterval()))
					.withRepeatCount(ssd == null || ssd.getRepeatCount() == null ? 1 : ssd.getRepeatCount());
		}

		Trigger trigger = TriggerBuilder.newTrigger().withIdentity(job.getKey() + "Trigger").startNow() // NON-NLS
				.withSchedule(scheduleBuilder).build();

		scheduler.scheduleJob(job, trigger);
	}

	/**
	 * Builds scheduler job for call scenario step.
	 * 
	 * @param scenario
	 *            scenario details
	 * @param step
	 *            scenario step details
	 * @param jobAttrs
	 *            additional job attributes
	 * @return scheduler job detail object.
	 */
	protected abstract JobDetail buildJob(WsScenario scenario, WsScenarioStep step, JobDataMap jobAttrs);

	@Override
	protected long getActivityItemByteSize(String item) {
		return item == null ? 0 : item.getBytes().length;
	}

	/**
	 * Adds scenario to scenarios list.
	 *
	 * @param scenario
	 *            scenario to be added to list
	 */
	public void addScenario(WsScenario scenario) {
		if (scenarioList == null) {
			scenarioList = new ArrayList<WsScenario>();
		}

		scenarioList.add(scenario);
	}

	/**
	 * Returns list of defined streaming scenarios.
	 *
	 * @return list of streaming scenarios
	 */
	public List<WsScenario> getScenarios() {
		return scenarioList;
	}

	@Override
	protected void cleanup() {
		if (scenarioList != null) {
			scenarioList.clear();
		}

		if (scheduler != null) {
			try {
				scheduler.shutdown(true);
			} catch (SchedulerException exc) {
				logger.log(OpLevel.WARNING, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS,
						"AbstractWsStream.error.closing.scheduler"), exc);
			}
			scheduler = null;
		}

		super.cleanup();
	}

	@Override
	protected boolean isInputEnded() {
		boolean hasRunningSteps = true;

		Set<TriggerKey> triggerKeys = null;
		try {
			triggerKeys = scheduler.getTriggerKeys(null);
		} catch (SchedulerException exc) {
		}

		if (CollectionUtils.isNotEmpty(triggerKeys)) {
			for (TriggerKey tKey : triggerKeys) {
				try {
					Trigger t = scheduler.getTrigger(tKey);
					if (t.mayFireAgain()) {
						hasRunningSteps = false;
						break;
					}
				} catch (SchedulerException exc) {
				}
			}
		}

		return hasRunningSteps;
	}

	/**
	 * Request method types enumeration.
	 */
	enum ReqMethod {
		/**
		 * Request method GET.
		 */
		GET,
		/**
		 * Request method POST.
		 */
		POST,
		/**
		 * Request method COMMAND.
		 */
		COMMAND
	}

}
