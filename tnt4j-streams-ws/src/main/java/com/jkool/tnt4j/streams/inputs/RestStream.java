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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.quartz.*;

import com.jkool.tnt4j.streams.scenario.WsScenario;
import com.jkool.tnt4j.streams.scenario.WsScenarioStep;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.jkool.tnt4j.streams.utils.WsStreamConstants;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a scheduled JAX-RS service call activity stream, where each call
 * responce is assumed to represent a single activity or event which should be
 * recorded.
 * <p>
 * Service call is performed by invoking
 * {@link org.apache.http.client.HttpClient#execute(HttpUriRequest)} with GET or
 * POST method request depending on scenario step configuration parameter
 * 'method'. Default method is GET.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see org.apache.http.client.HttpClient#execute(HttpUriRequest)
 */
public class RestStream extends AbstractWsStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(RestStream.class);

	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_REQ_METHOD_KEY = "reqMethod"; // NON-NLS

	/**
	 * Constructs an empty RestStream. Requires configuration settings to set
	 * input stream source.
	 */
	public RestStream() {
		super(LOGGER);
	}

	@Override
	protected JobDetail buildJob(WsScenario scenario, WsScenarioStep step, JobDataMap jobAttrs) {
		jobAttrs.put(JOB_PROP_URL_KEY, step.getUrlStr());
		jobAttrs.put(JOB_PROP_REQ_KEY, step.getRequest());
		jobAttrs.put(JOB_PROP_REQ_METHOD_KEY, step.getMethod());

		return JobBuilder.newJob(RestCallJob.class).withIdentity(scenario.getName() + ":" + step.getName()) // NON-NLS
				.usingJobData(jobAttrs).build();
	}

	/**
	 * Performs JAX-RS service call over HTTP GET method request.
	 *
	 * @param uriStr
	 *            JAX-RS service URI
	 * @return service responce string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	protected static String executeGET(String uriStr) throws Exception {
		if (StringUtils.isEmpty(uriStr)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS,
					"RestStream.cant.execute.get.request"), uriStr);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "RestStream.invoking.get.request"),
				uriStr);

		String respStr = null;
		CloseableHttpClient client = HttpClients.createDefault();
		try {
			HttpGet get = new HttpGet(uriStr);

			HttpResponse response = client.execute(get);
			int responseCode = response.getStatusLine().getStatusCode();
			if (responseCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
				throw new HttpResponseException(responseCode, response.getStatusLine().getReasonPhrase());
			}

			respStr = EntityUtils.toString(response.getEntity(), "UTF-8"); // NON-NLS
		} finally {
			Utils.close(client);
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
	 * @return service responce string
	 * @throws Exception
	 *             if exception occurs while performing JAX-RS service call
	 */
	protected static String executePOST(String uriStr, String reqData) throws Exception {
		if (StringUtils.isEmpty(uriStr)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS,
					"RestStream.cant.execute.post.request"), uriStr);
			return null;
		}

		if (StringUtils.isEmpty(reqData)) {
			return executeGET(uriStr);
		}

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "RestStream.invoking.post.request"),
				uriStr, reqData);

		String respStr = null;
		CloseableHttpClient client = HttpClients.createDefault();
		try {
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

			HttpResponse response = client.execute(post);
			int responseCode = response.getStatusLine().getStatusCode();
			if (responseCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
				throw new HttpResponseException(responseCode, response.getStatusLine().getReasonPhrase());
			}

			respStr = EntityUtils.toString(response.getEntity(), "UTF-8"); // NON-NLS
		} finally {
			Utils.close(client);
		}

		return respStr;
	}

	// protected static String executeGET2(String urlStr) throws Exception {
	// URL url = new URL(urlStr);
	// HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	// conn.setRequestMethod(ReqMethod.GET.name());
	// conn.setRequestProperty("Accept", "application/json");
	//
	// if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
	// throw new RuntimeException("Failed : HTTP error code : " +
	// conn.getResponseCode());
	// }
	//
	// BufferedReader br = new BufferedReader(new
	// InputStreamReader((conn.getInputStream())));
	//
	// StringBuilder sb = new StringBuilder();
	// String outStr;
	// while ((outStr = br.readLine()) != null) {
	// sb.append(outStr);
	// }
	// Utils.close(br);
	//
	// conn.disconnect();
	//
	// return sb.toString();
	// }
	//
	// protected static String executePOST2(String urlStr, String reqData)
	// throws Exception {
	// if (StringUtils.isEmpty(reqData)) {
	// return get2(urlStr);
	// }
	//
	// URL url = new URL(urlStr);
	// HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	// conn.setDoOutput(true);
	// conn.setRequestMethod(ReqMethod.POST.name());
	// conn.setRequestProperty("Content-Type", "application/json");
	//
	// if (reqData == null) {
	// reqData = "";
	// }
	//
	// OutputStream os = conn.getOutputStream();
	// os.write(reqData.getBytes());
	// os.flush();
	//
	// if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
	// throw new RuntimeException("Failed : HTTP error code : " +
	// conn.getResponseCode());
	// }
	//
	// BufferedReader br = new BufferedReader(new
	// InputStreamReader((conn.getInputStream())));
	//
	// StringBuilder sb = new StringBuilder();
	// String outStr;
	// while ((outStr = br.readLine()) != null) {
	// sb.append(outStr);
	// }
	//
	// Utils.close(br);
	//
	// conn.disconnect();
	//
	// return sb.toString();
	// }

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
			String respStr = null;

			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			AbstractWsStream stream = (AbstractWsStream) dataMap.get(JOB_PROP_STREAM_KEY);
			String urlStr = dataMap.getString(JOB_PROP_URL_KEY);
			String reqData = dataMap.getString(JOB_PROP_REQ_KEY);
			String reqMethod = dataMap.getString(JOB_PROP_REQ_METHOD_KEY);
			if (StringUtils.isEmpty(reqMethod)) {
				reqMethod = ReqMethod.GET.name();
			}

			try {
				if (ReqMethod.POST.name().equalsIgnoreCase(reqMethod)) {
					respStr = executePOST(urlStr, reqData);
				} else if (ReqMethod.GET.name().equalsIgnoreCase(reqMethod)) {
					respStr = executeGET(urlStr);
				}
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS,
						"RestStream.execute.exception"), exc);
			}

			if (StringUtils.isNotEmpty(respStr)) {
				stream.addInputToBuffer(respStr);
			}
		}
	}

}
