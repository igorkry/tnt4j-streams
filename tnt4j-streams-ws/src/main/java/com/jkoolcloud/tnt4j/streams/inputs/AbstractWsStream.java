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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.scenario.*;
import com.jkoolcloud.tnt4j.streams.utils.StreamsCache;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Base class for scheduled service or system command request/call produced activity stream, where each call/request
 * response is assumed to represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support {@link String} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided string.
 * <p>
 * This activity stream supports configuration properties from
 * {@link com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream} (and higher hierarchy streams).
 *
 * @param <T>
 *            type of handled response data
 *
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractWsStream<T> extends AbstractBufferedStream<WsResponse<T>> {

	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_STREAM_KEY = "streamObj"; // NON-NLS
	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_SCENARIO_STEP_KEY = "scenarioStepObj"; // NON-NLS

	private List<WsScenario> scenarioList;

	private static Scheduler scheduler;
	private static final ReentrantLock schedInitLock = new ReentrantLock();

	// @Override
	// public void setProperties(Collection<Map.Entry<String, String>> props) {
	// super.setProperties(props);
	//
	// if (CollectionUtils.isNotEmpty(props)) {
	// for (Map.Entry<String, String> prop : props) {
	// String name = prop.getKey();
	// String value = prop.getValue();
	//
	// }
	// }
	// }
	//
	// @Override
	// public Object getProperty(String name) {
	// return super.getProperty(name);
	// }

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		schedInitLock.lock();
		try {
			if (scheduler == null) {
				scheduler = StdSchedulerFactory.getDefaultScheduler();
				scheduler.start();
			}
		} finally {
			schedInitLock.unlock();
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"AbstractWsStream.scheduler.started", getName());

		loadScenarios();
	}

	/**
	 * Loads scenario steps into scheduler.
	 *
	 * @throws Exception
	 *             If any exception occurs while loading scenario steps to scheduler
	 */
	private void loadScenarios() throws Exception {
		int scenariosCount = 0;
		if (CollectionUtils.isNotEmpty(scenarioList)) {
			for (WsScenario scenario : scenarioList) {
				if (!scenario.isEmpty()) {
					for (WsScenarioStep step : scenario.getStepsList()) {
						scheduleScenarioStep(step);
					}
					scenariosCount++;
				}
			}
		}

		if (scenariosCount == 0) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"AbstractWsStream.no.scenarios.defined", getName()));
		} else {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"AbstractWsStream.stream.scenarios.loaded", getName(), scenariosCount);
		}
	}

	/**
	 * Schedules scenario step to be executed by step defined scheduler configuration data.
	 *
	 * @param step
	 *            scenario step instance to schedule
	 * @throws SchedulerException
	 *             if scheduler fails to schedule job for defined step
	 */
	protected void scheduleScenarioStep(WsScenarioStep step) throws SchedulerException {
		if (scheduler == null) {
			throw new SchedulerException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
					"AbstractWsStream.null.scheduler", getName()));
		}

		String enabledProp = step.getProperty("Enabled"); // NON-NLS
		if (StringUtils.equalsIgnoreCase("false", enabledProp)) { // NON-NLS
			logger().log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"AbstractWsStream.stream.step.disabled", getName(), step.getScenario().getName(), step.getName());
			return;
		}

		JobDataMap jobAttrs = new JobDataMap();
		jobAttrs.put(JOB_PROP_STREAM_KEY, this);
		jobAttrs.put(JOB_PROP_SCENARIO_STEP_KEY, step);

		String jobId = step.getScenario().getName() + ':' + step.getName();

		JobDetail job = buildJob(jobId, jobAttrs);

		ScheduleBuilder<?> scheduleBuilder;
		AbstractSchedulerData schedulerData = (AbstractSchedulerData) step.getSchedulerData();

		if (schedulerData instanceof CronSchedulerData) {
			CronSchedulerData csd = (CronSchedulerData) schedulerData;
			scheduleBuilder = CronScheduleBuilder.cronSchedule(csd.getExpression());
		} else {
			SimpleSchedulerData ssd = (SimpleSchedulerData) schedulerData;
			Integer repCount = ssd == null ? null : ssd.getRepeatCount();

			if (repCount != null && repCount == 0) {
				return;
			}

			if (repCount == null) {
				repCount = 1;
			}

			TimeUnit timeUnit = ssd == null ? TimeUnit.SECONDS : ssd.getUnits();
			long interval = ssd == null ? 1 : ssd.getInterval();

			scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
					.withIntervalInMilliseconds(timeUnit.toMillis(interval))
					.withRepeatCount(repCount > 0 ? repCount - 1 : repCount);
		}

		Date startAt = schedulerData == null ? new Date() : schedulerData.getStartAt();
		Trigger trigger = TriggerBuilder.newTrigger().withIdentity(job.getKey() + "Trigger") // NON-NLS
				.startAt(startAt).withSchedule(scheduleBuilder).build();

		if (schedulerData != null && schedulerData.getStartDelay() != null && schedulerData.getStartDelay() > 0) {
			logger().log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"AbstractWsStream.stream.step.start.delayed", getName(), jobId, schedulerData.getStartDelay(),
					schedulerData.getStartDelayUnits());
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"AbstractWsStream.stream.scheduling.job", getName(), jobId);
		scheduler.scheduleJob(job, trigger);
	}

	/**
	 * Builds scheduler job for call scenario step.
	 *
	 * @param jobId
	 *            job identifier
	 * @param jobAttrs
	 *            additional job attributes
	 * @return scheduler job detail object.
	 */
	protected abstract JobDetail buildJob(String jobId, JobDataMap jobAttrs);

	/**
	 * Adds scenario to scenarios list.
	 *
	 * @param scenario
	 *            scenario to be added to list
	 */
	public void addScenario(WsScenario scenario) {
		if (scenarioList == null) {
			scenarioList = new ArrayList<>();
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
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.error.closing.scheduler", exc);
			}
			scheduler = null;
		}

		super.cleanup();
	}

	/**
	 * Removes all inactive jobs from stream scheduler.
	 */
	protected void purgeInactiveSchedulerJobs() {
		if (scheduler != null) {
			try {
				int rCount = 0;
				Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(null);
				if (CollectionUtils.isNotEmpty(triggerKeys)) {
					for (TriggerKey tKey : triggerKeys) {
						try {
							Trigger t = scheduler.getTrigger(tKey);
							if (t != null && !t.mayFireAgain()) {
								scheduler.deleteJob(t.getJobKey());
								rCount++;
							}
						} catch (SchedulerException exc) {
						}
					}
				}

				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"AbstractWsStream.scheduler.removed.inactive.jobs", getName(), rCount);
			} catch (SchedulerException exc) {
			}
		}
	}

	@Override
	protected boolean isInputEnded() {
		try {
			List<JobExecutionContext> runningJobs = scheduler.getCurrentlyExecutingJobs();
			if (CollectionUtils.isNotEmpty(runningJobs)) {
				return false;
			}
		} catch (SchedulerException exc) {
		}

		try {
			Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(null);
			if (CollectionUtils.isNotEmpty(triggerKeys)) {
				for (TriggerKey tKey : triggerKeys) {
					try {
						Trigger t = scheduler.getTrigger(tKey);
						if (t != null && t.mayFireAgain()) {
							return false;
						}
					} catch (SchedulerException exc) {
					}
				}
			}
		} catch (SchedulerException exc) {
		}

		offerDieMarker();
		return true;
	}

	@Override
	public String[] getDataTags(Object data) {
		return data instanceof WsResponse<?> ? ((WsResponse<?>) data).getTags() : super.getDataTags(data);
	}

	@Override
	protected ActivityInfo applyParsers(Object data, String... tags) throws IllegalStateException, ParseException {
		return super.applyParsers(data instanceof WsResponse<?> ? ((WsResponse<?>) data).getData() : data, tags);
	}

	/**
	 * Performs pre-processing of request/command/query body data: it can be expressions evaluation, filling in variable
	 * values and so on.
	 *
	 * @param requestData
	 *            request/command/query body data
	 * @return preprocessed request/command/query body data string
	 */
	protected String preProcess(String requestData) {
		return requestData;
	}

	/**
	 * Fills in request/query/command string having variable expressions with parameters stored in stream configuration
	 * properties map and streams cache {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            JDBC query string
	 * @return variable values filled in JDBC query string
	 *
	 * @see #fillInRequestData(String, String)
	 */
	protected String fillInRequestData(String reqDataStr) {
		return fillInRequestData(reqDataStr, (String) null);
	}

	/**
	 * Fills in request/query/command string having variable expressions with parameters stored in stream configuration
	 * properties map map and streams cache {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            JDBC query string
	 * @param format
	 *            format of value to fill
	 * @return variable values filled in JDBC query string
	 *
	 * @see #fillInRequestData(String, java.util.Map)
	 * @see #fillInRequestCacheData(String, String)
	 */
	protected String fillInRequestData(String reqDataStr, String format) {
		String frd = fillInRequestData(reqDataStr, getConfigProperties());
		frd = fillInRequestCacheData(frd, format);

		return frd;
	}

	/**
	 * Returns streams specific configuration properties map.
	 *
	 * @return streams specific configuration properties map
	 */
	protected Map<String, String> getConfigProperties() {
		return null;
	}

	/**
	 * Fills in request/query/command string having variable expressions with parameters stored in
	 * {@code streamProperties} map.
	 *
	 * @param reqDataStr
	 *            request/query/command string
	 * @param streamProperties
	 *            stream properties map
	 * @return variable values filled in request/query/command string
	 */
	protected String fillInRequestData(String reqDataStr, Map<String, String> streamProperties) {
		if (StringUtils.isEmpty(reqDataStr) || MapUtils.isEmpty(streamProperties)) {
			return reqDataStr;
		}

		List<String> vars = new ArrayList<>();
		Utils.resolveExpressionVariables(vars, reqDataStr);
		// Utils.resolveCfgVariables(vars, reqDataStr);

		String reqData = reqDataStr;
		if (CollectionUtils.isNotEmpty(vars)) {
			String varVal;
			for (String rdVar : vars) {
				varVal = streamProperties.get(Utils.getVarName(rdVar));
				if (varVal != null) {
					reqData = reqData.replace(rdVar, varVal);
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractWsStream.filling.req.data.variable", rdVar, Utils.toString(varVal));
				}
			}
		}

		return reqData;
	}

	/**
	 * Fills in request/query/command string having variable expressions with parameters stored in streams cache
	 * {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            request/query/command string
	 * @param format
	 *            format of value to fill
	 * @return variable values filled in request/query/command string
	 */
	protected String fillInRequestCacheData(String reqDataStr, String format) {
		if (StringUtils.isEmpty(reqDataStr)) {
			return reqDataStr;
		}

		List<String> vars = new ArrayList<>();
		Utils.resolveExpressionVariables(vars, reqDataStr);
		// Utils.resolveCfgVariables(vars, reqDataStr);

		String reqData = reqDataStr;
		if (CollectionUtils.isNotEmpty(vars)) {
			String varVal;
			for (String rdVar : vars) {
				Object cValue = StreamsCache.getValue(Utils.getVarName(rdVar));
				varVal = formattedValue(cValue, format);
				if (varVal != null) {
					reqData = reqData.replace(rdVar, varVal);
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"AbstractWsStream.filling.req.data.variable", rdVar, Utils.toString(varVal));
				}
			}
		}

		return reqData;
	}

	/**
	 * Formats provided value as a string using defined format pattern.
	 *
	 * @param cValue
	 *            value to format
	 * @param format
	 *            format pattern of the value
	 * @return formatted value string
	 */
	protected String formattedValue(Object cValue, String format) {
		if (StringUtils.isNotEmpty(format)) {
			if (cValue instanceof UsecTimestamp) {
				return ((UsecTimestamp) cValue).toString(format);
			} else if (cValue instanceof Date) {
				SimpleDateFormat df = new SimpleDateFormat(format);
				return df.format(cValue);
			} else if (cValue instanceof Number) {
				DecimalFormat df = new DecimalFormat(format);
				return df.format(cValue);
			} else {
				return Utils.toString(cValue);
			}
		}

		return Utils.toString(cValue);
	}
}
