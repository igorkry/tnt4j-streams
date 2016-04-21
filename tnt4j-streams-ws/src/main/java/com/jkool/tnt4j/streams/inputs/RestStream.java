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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
public class RestStream extends AbstractWsStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(RestStream.class);

	/**
	 * TODO
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
		return JobBuilder.newJob(RestCallJob.class).withIdentity(scenario.getName() + ":" + step.getName()) // NON-NLS
				.usingJobData(jobAttrs).usingJobData(JOB_PROP_URL_KEY, step.getUrlStr())
				.usingJobData(JOB_PROP_REQ_KEY, step.getRequest())
				.usingJobData(JOB_PROP_REQ_METHOD_KEY, step.getMethod()).build();
	}

	/**
	 * TODO.
	 *
	 * @param uriStr
	 *            the uri str
	 * @return the string
	 * @throws Exception
	 *             the exception
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
			respStr = EntityUtils.toString(response.getEntity(), "UTF-8"); // NON-NLS

			// BufferedReader rd = new BufferedReader(new
			// InputStreamReader(response.getEntity().getContent()));
			// String line = "";
			// while ((line = rd.readLine()) != null) {
			// System.out.println(line);
			// }
		} finally {
			Utils.close(client);
		}

		return respStr;
	}

	/**
	 * TODO.
	 *
	 * @param uriStr
	 *            the uri str
	 * @param reqData
	 *            the req data
	 * @return the string
	 * @throws Exception
	 *             the exception
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
			// StringEntity reqEntity = new
			// StringEntity("{\'name1\':\'value1\',\'name2\':\'value2\'}");
			reqEntity.setContentType("application/json"); // NON-NLS

			// List<BasicNameValuePair> nameValuePairs = new
			// ArrayList<BasicNameValuePair>(1);
			// nameValuePairs.add(new BasicNameValuePair("name", "value"));
			// //you
			// can as many name value pair as you want in the list.
			// post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			post.setEntity(reqEntity);
			HttpResponse response = client.execute(post);
			// NOTE: what if response is not OK
			respStr = EntityUtils.toString(response.getEntity(), "UTF-8"); // NON-NLS

			// BufferedReader rd = new BufferedReader(new
			// InputStreamReader(response.getEntity().getContent()));
			// String line = "";
			// while ((line = rd.readLine()) != null) {
			// System.out.println(line);
			// }
		} finally {
			Utils.close(client);
		}

		return respStr;
	}

	// protected static String executeGET2(String urlStr) throws Exception {
	// // URL url = new
	// // URL("http://localhost:8080/RESTfulExample/json/product/get");
	// URL url = new URL(urlStr);
	// HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	// conn.setRequestMethod("GET");
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
	// System.out.println("Output from Server .... \n");
	// while ((outStr = br.readLine()) != null) {
	// sb.append(outStr);
	// }
	//
	// conn.disconnect();
	//
	// return sb.toString();
	// }
	//
	// protected static String executePOST2(String urlStr, String reqData)
	// throws
	// Exception {
	// if (StringUtils.isEmpty(reqData)) {
	// return get2(urlStr);
	// }
	//
	// // URL url = new
	// // URL("http://localhost:8080/RESTfulExample/json/product/post");
	// URL url = new URL(urlStr);
	// HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	// conn.setDoOutput(true);
	// conn.setRequestMethod("POST");
	// conn.setRequestProperty("Content-Type", "application/json");
	//
	// // String reqData = "{\"qty\":100,\"name\":\"iPad 4\"}";
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
	// System.out.println("Output from Server .... \n");
	// while ((outStr = br.readLine()) != null) {
	// sb.append(outStr);
	// }
	//
	// conn.disconnect();
	//
	// return sb.toString();
	// }

	/**
	 * TODO
	 */
	public static class RestCallJob implements Job {

		/**
		 * TODO
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
				reqMethod = "GET"; // NON-NLS
			}

			try {
				if ("POST".equalsIgnoreCase(reqMethod)) { // NON-NLS
					respStr = executePOST(urlStr, reqData);
				} else if ("GET".equalsIgnoreCase(reqMethod)) { // NON-NLS
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
