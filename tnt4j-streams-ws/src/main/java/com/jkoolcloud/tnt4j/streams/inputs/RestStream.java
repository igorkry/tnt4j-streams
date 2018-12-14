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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.quartz.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.scenario.WsRequest;
import com.jkoolcloud.tnt4j.streams.scenario.WsResponse;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Implements a scheduled JAX-RS service call activity stream, where each call response is assumed to represent a single
 * activity or event which should be recorded.
 * <p>
 * Service call is performed by invoking {@link org.apache.http.client.HttpClient#execute(HttpUriRequest)} with GET or
 * POST method request depending on scenario step configuration parameter 'method'. Default method is GET.
 * <p>
 * This activity stream requires parsers that can support {@link String} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided string.
 * <p>
 * This activity stream supports configuration properties from {@link AbstractWsStream} (and higher hierarchy streams).
 *
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see org.apache.http.client.HttpClient#execute(HttpUriRequest)
 */
public class RestStream extends AbstractWsStream<String> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(RestStream.class);

	/**
	 * Constructs an empty RestStream. Requires configuration settings to set input stream source.
	 */
	public RestStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected long getActivityItemByteSize(WsResponse<String> item) {
		return item == null || item.getData() == null ? 0 : item.getData().getBytes().length;
	}

	@Override
	protected JobDetail buildJob(String jobId, JobDataMap jobAttrs) {
		return JobBuilder.newJob(RestCallJob.class).withIdentity(jobId).usingJobData(jobAttrs).build();
	}

	/**
	 * Performs JAX-RS service call over HTTP GET method request.
	 *
	 * @param uriStr
	 *            JAX-RS service URI
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	protected static String executeGET(String uriStr) throws Exception {
		return executeGET(uriStr, null, null);
	}

	/**
	 * Performs JAX-RS service call over HTTP GET method request.
	 *
	 * @param uriStr
	 *            JAX-RS service URI
	 * @param username
	 *            user name used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @param password
	 *            password used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	protected static String executeGET(String uriStr, String username, String password) throws Exception {
		if (StringUtils.isEmpty(uriStr)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"RestStream.cant.execute.get.request", uriStr);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.invoking.get.request", uriStr);

		String respStr = null;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet get = new HttpGet(uriStr);

			respStr = executeRequest(client, get, username, password);
		}

		return respStr;
	}

	/**
	 * Performs JAX-RS service call over HTTP POST method request.
	 *
	 * @param uriStr
	 *            JAX-RS service URI
	 * @param reqData
	 *            request data
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	protected static String executePOST(String uriStr, String reqData) throws Exception {
		return executePOST(uriStr, reqData, null, null);
	}

	/**
	 * Performs JAX-RS service call over HTTP POST method request.
	 *
	 * @param uriStr
	 *            JAX-RS service URI
	 * @param reqData
	 *            request data
	 * @param username
	 *            user name used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @param password
	 *            password used to perform request if service authentication is needed, or {@code null} if no
	 *            authentication
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	public static String executePOST(String uriStr, String reqData, String username, String password) throws Exception {
		if (StringUtils.isEmpty(uriStr)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"RestStream.cant.execute.post.request", uriStr);
			return null;
		}

		if (StringUtils.isEmpty(reqData)) {
			return executeGET(uriStr, username, password);
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.invoking.post.request", uriStr, reqData);

		String respStr = null;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost post = new HttpPost(uriStr);
			StringEntity reqEntity = new StringEntity(reqData == null ? "" : reqData);
			// here instead of JSON you can also have XML
			reqEntity.setContentType("application/json"); // NON-NLS

			// List<BasicNameValuePair> nameValuePairs = new
			// ArrayList<BasicNameValuePair>(1);
			// nameValuePairs.add(new BasicNameValuePair("name", "value"));
			// // you can have as many name value pair as you want in the list.
			// post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			post.setEntity(reqEntity);

			respStr = executeRequest(client, post, username, password);
		}

		return respStr;
	}

	private static String executeRequest(HttpClient client, HttpUriRequest req, String username, String password)
			throws IOException {
		HttpResponse response;

		if (StringUtils.isNotEmpty(username)) {
			// CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			// UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
			// credentialsProvider.setCredentials(AuthScope.ANY, credentials);
			//
			// // HttpClient client =
			// // HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
			//
			// URI reqURI = req.getURI();
			// HttpHost targetHost = new HttpHost(reqURI.getHost(), reqURI.getPort(), reqURI.getScheme());
			// AuthCache authCache = new BasicAuthCache();
			// authCache.put(targetHost, new BasicScheme());
			//
			// HttpClientContext context = HttpClientContext.create();
			// context.setCredentialsProvider(credentialsProvider);
			// context.setAuthCache(authCache);
			//
			// response = client.execute(req, context);

			String credentialsStr = username + ":" + password; // NON-NLS
			String encoding = DatatypeConverter.printBase64Binary(credentialsStr.getBytes());
			req.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding); // NON-NLS
			response = client.execute(req);
		} else {
			response = client.execute(req);
		}

		int responseCode = response.getStatusLine().getStatusCode();
		if (responseCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
			throw new HttpResponseException(responseCode, response.getStatusLine().getReasonPhrase());
		}

		String respStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"RestStream.received.response", req.getURI(), respStr);

		return respStr;
	}

	/**
	 * Scheduler job to execute JAX-RS call.
	 */
	public static class RestCallJob implements Job {

		/**
		 * Constructs a new RestCallJob.
		 */
		public RestCallJob() {
		}

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			RestStream stream = (RestStream) dataMap.get(JOB_PROP_STREAM_KEY);
			WsScenarioStep scenarioStep = (WsScenarioStep) dataMap.get(JOB_PROP_SCENARIO_STEP_KEY);
			String reqMethod = scenarioStep.getMethod();

			if (StringUtils.isEmpty(reqMethod)) {
				reqMethod = ReqMethod.GET.name();
			}

			if (ReqMethod.POST.name().equalsIgnoreCase(reqMethod)) {
				if (!scenarioStep.isEmpty()) {
					String respStr;
					for (WsRequest<String> request : scenarioStep.getRequests()) {
						respStr = null;
						try {
							String processedRequest = stream.preProcess(request.getData());
							respStr = executePOST(scenarioStep.getUrlStr(), processedRequest,
									scenarioStep.getUsername(), scenarioStep.getPassword());
						} catch (Exception exc) {
							Utils.logThrowable(LOGGER, OpLevel.WARNING,
									StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"RestStream.execute.exception", exc);
						}

						if (StringUtils.isNotEmpty(respStr)) {
							stream.addInputToBuffer(new WsResponse<>(respStr, request.getTags()));
						}
					}
				}
			} else if (ReqMethod.GET.name().equalsIgnoreCase(reqMethod)) {
				String respStr = null;
				try {
					respStr = executeGET(scenarioStep.getUrlStr(), scenarioStep.getUsername(),
							scenarioStep.getPassword());
				} catch (Exception exc) {
					Utils.logThrowable(LOGGER, OpLevel.WARNING,
							StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"RestStream.execute.exception", exc);
				}

				if (StringUtils.isNotEmpty(respStr)) {
					stream.addInputToBuffer(new WsResponse<>(respStr));
				}
			}
		}
	}

	/**
	 * Request method types enumeration.
	 */
	protected enum ReqMethod {
		/**
		 * Request method GET.
		 */
		GET,
		/**
		 * Request method POST.
		 */
		POST,
	}

}
