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

import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.scenario.WsRequest;
import com.jkoolcloud.tnt4j.streams.scenario.WsResponse;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Implements a scheduled JDBC query call activity stream, where each query call returned {@link java.sql.ResultSet} row
 * is assumed to represent a single activity or event which should be recorded.
 * <p>
 * JDBC query call is performed by invoking {@link java.sql.Connection#prepareStatement(String)} and
 * {@link java.sql.PreparedStatement#executeQuery()}.
 * <p>
 * This activity stream requires parsers that can support {@link java.sql.ResultSet} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided result set.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractWsStream}):
 * <ul>
 * <li>set of JDBC driver supported properties used to invoke
 * {@link DriverManager#getConnection(String, java.util.Properties)}. (Optional)</li>
 * <li>when {@value com.jkoolcloud.tnt4j.streams.configure.StreamProperties#PROP_USE_EXECUTOR_SERVICE} is set to
 * {@code true} and {@value com.jkoolcloud.tnt4j.streams.configure.StreamProperties#PROP_EXECUTOR_THREADS_QTY} is
 * greater than {@code 1}, value for that property is reset to {@code 1} since {@link java.sql.ResultSet} can't be
 * accessed in multi-thread manner.</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see java.sql.DriverManager#getConnection(String, java.util.Properties)
 * @see java.sql.Connection#prepareStatement(String)
 * @see java.sql.PreparedStatement#executeQuery()
 */
public class JDBCStream extends AbstractWsStream<ResultSet> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JDBCStream.class);

	protected Map<String, String> jdbcProperties = new HashMap<>();

	/**
	 * Constructs an empty JDBCStream. Requires configuration settings to set input stream source.
	 */
	public JDBCStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();

				jdbcProperties.put(name, value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		String pValue = jdbcProperties.get(name);
		if (pValue != null) {
			return pValue;
		}

		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		boolean useExecService = (boolean) getProperty(StreamProperties.PROP_USE_EXECUTOR_SERVICE);
		int threadCount = (int) getProperty(StreamProperties.PROP_EXECUTOR_THREADS_QTY);
		if (useExecService && threadCount > 1) {
			logger().log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.resetting.thread.count.property", threadCount);
			setProperty(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "1");
		}

		super.initialize();
	}

	@Override
	protected long getActivityItemByteSize(WsResponse<ResultSet> item) {
		return 0; // TODO
	}

	@Override
	protected JobDetail buildJob(String jobId, JobDataMap jobAttrs) {
		return JobBuilder.newJob(JdbcCallJob.class).withIdentity(jobId).usingJobData(jobAttrs).build();
	}

	@Override
	protected boolean isItemConsumed(WsResponse<ResultSet> item) {
		if (item == null || item.getData() == null) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.rs.consumption.null");
			return true;
		}

		ResultSet rs = item.getData();
		try {
			if (rs.isClosed() || !rs.next()) {
				Statement st = rs.getStatement();
				Connection conn = st == null ? null : st.getConnection();

				Utils.close(rs);
				Utils.close(st);
				Utils.close(conn);

				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"JDBCStream.rs.consumption.done");
				return true;
			}

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.rs.consumption.marker.new", rs.getRow());
			return false;
		} catch (SQLException exc) {
			Utils.logThrowable(logger(), OpLevel.WARNING,
					StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.rs.consumption.exception", exc);
			return true;
		}
	}

	@Override
	protected boolean initItemForParsing(WsResponse<ResultSet> item) {
		return !isItemConsumed(item);
	}

	/**
	 * Performs JDBC query call.
	 *
	 * @param url
	 *            DB connection URL
	 * @param user
	 *            DB user name
	 * @param pass
	 *            DB user password
	 * @param query
	 *            DB query
	 * @param stream
	 *            stream instance to use for JDBC query execution
	 * @return JDBC call returned result set {@link java.sql.ResultSet}
	 * @throws SQLException
	 *             if exception occurs while performing JDBC call
	 */
	protected static ResultSet executeJdbcCall(String url, String user, String pass, String query, JDBCStream stream)
			throws SQLException {
		if (StringUtils.isEmpty(url)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.db.conn.not.defined", url);
			return null;
		}

		if (StringUtils.isEmpty(query)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.query.not.defined", query);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.invoking.get.request", query);

		Connection dbConn;
		if (stream.jdbcProperties.isEmpty()) {
			dbConn = DriverManager.getConnection(url, user, pass);
		} else {
			Properties connProps = new Properties();
			connProps.setProperty("user", user); // NON-NLS
			connProps.setProperty("password", pass); // NON-NLS

			for (Map.Entry<String, String> pe : stream.jdbcProperties.entrySet()) {
				connProps.setProperty(pe.getKey(), pe.getValue());
			}
			dbConn = DriverManager.getConnection(url, connProps);
		}
		PreparedStatement statement = dbConn.prepareStatement(query);
		ResultSet rs = statement.executeQuery();

		return rs;
	}

	/**
	 * Fills in JDBC query string having variable expressions with parameters stored in {@link #jdbcProperties} map and
	 * streams cache {@link com.jkoolcloud.tnt4j.streams.utils.StreamsCache}.
	 *
	 * @param reqDataStr
	 *            JDBC query string
	 * @return variable values filled in JDBC query string
	 *
	 * @see #fillInRequestData(String, java.util.Map)
	 * @see #fillInRequestCacheData(String)
	 */
	protected String fillInRequestData(String reqDataStr) {
		String frd = fillInRequestData(reqDataStr, jdbcProperties);
		frd = fillInRequestCacheData(frd);

		return frd;
	}

	/**
	 * Scheduler job to execute JDBC call.
	 */
	public static class JdbcCallJob implements Job {

		/**
		 * Constructs a new JdbcCallJob.
		 */
		public JdbcCallJob() {
		}

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			JDBCStream stream = (JDBCStream) dataMap.get(JOB_PROP_STREAM_KEY);
			// WsScenario scenario = (WsScenario) dataMap.get(JOB_PROP_SCENARIO_KEY);
			WsScenarioStep scenarioStep = (WsScenarioStep) dataMap.get(JOB_PROP_SCENARIO_STEP_KEY);

			if (!scenarioStep.isEmpty()) {
				ResultSet respRs;
				for (WsRequest<String> request : scenarioStep.getRequests()) {
					respRs = null;

					try {
						respRs = executeJdbcCall(scenarioStep.getUrlStr(), scenarioStep.getUsername(),
								scenarioStep.getPassword(), stream.fillInRequestData(request.getData()), stream);
					} catch (Exception exc) {
						Utils.logThrowable(LOGGER, OpLevel.WARNING,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"JDBCStream.execute.exception", exc);
					} finally {
						if (respRs != null) {
							stream.addInputToBuffer(new WsResponse<>(respRs, request.getTag()));
						}
					}
				}
			}
		}
	}
}
