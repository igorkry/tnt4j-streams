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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenario;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Implements a scheduled JAX-WS service call activity stream, where each call response is assumed to represent a single
 * activity or event which should be recorded.
 * <p>
 * Service call is performed by invoking {@link SOAPConnection#call(SOAPMessage, Object)}. Provided request XML data is
 * set as {@link SOAPMessage} body data.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports properties from {@link AbstractWsStream} (and higher hierarchy streams).
 *
 * @version $Revision: 1 $
 *
 * @see ActivityParser#isDataClassSupported(Object)
 * @see SOAPConnection#call(SOAPMessage, Object)
 */
public class WsStream extends AbstractWsStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WsStream.class);
	private Semaphore semaphore = new Semaphore(1);

	/**
	 * Constructs an empty WsStream. Requires configuration settings to set input stream source.
	 */
	public WsStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected JobDetail buildJob(WsScenario scenario, WsScenarioStep step, JobDataMap jobAttrs) {
		jobAttrs.put(JOB_PROP_URL_KEY, step.getUrlStr());
		jobAttrs.put(JOB_PROP_REQ_KEY, step.getRequests());
		jobAttrs.put(JOB_PROP_SEMAPHORE, semaphore);

		return JobBuilder.newJob(WsCallJob.class).withIdentity(scenario.getName() + ':' + step.getName()) // NON-NLS
				.usingJobData(jobAttrs).build();
	}

	/**
	 * Performs JAX-WS service call using SOAP API.
	 *
	 * @param url
	 *            JAX-WS service URL
	 * @param soapRequestData
	 *            JAX-WS service request data: headers and body XML string
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-WS service call
	 */
	protected static String callWebService(String url, String soapRequestData, WsStream stream) throws Exception {
		if (StringUtils.isEmpty(url)) {
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "WsStream.cant.execute.request"),
					url);
			return null;
		}

		Map<String, String> headers = new HashMap<>();
		// separate SOAP message header values from request body XML
		BufferedReader br = new BufferedReader(new StringReader(soapRequestData));
		StringBuilder sb = new StringBuilder();
		try {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().startsWith("<")) { // NON-NLS
					sb.append(line).append(Utils.NEW_LINE);
				} else {
					int bi = line.indexOf(':'); // NON-NLS
					if (bi >= 0) {
						String hKey = line.substring(0, bi).trim();
						String hValue = line.substring(bi + 1).trim();
						headers.put(hKey, hValue);
					} else {
						sb.append(line).append(Utils.NEW_LINE);
					}
				}
			}
		} finally {
			Utils.close(br);
		}

		SOAPConnection soapConnection = getSoapConnection();

		soapRequestData = sb.toString();
		soapRequestData = stream.preProcess(soapRequestData);

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "WsStream.invoking.request"), url,
				soapRequestData);

		// Create SOAP message and set request XML as body
		SOAPMessage soapRequestMessage = createMessage(soapRequestData, stream, headers, true);

		// Send SOAP Message to SOAP Server
		stream.postProcess(soapRequestMessage);
		SOAPMessage soapResponse = soapConnection.call(soapRequestMessage, url);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "Response: {0}"),
				stream.convertSoapMessageToXMLString(soapResponse));

		if (soapResponse.getSOAPBody().hasFault()) {
			LOGGER.log(OpLevel.ERROR, "Failure response: {0} ", soapResponse.getSOAPBody().getFault().getFaultString());
			stream.handleFault(soapResponse.getSOAPBody().getFault());
			return null;
		}
		return stream.convertSoapMessageToXMLString(soapResponse);
	}

	protected String preProcess(String soapRequestData) {
		return soapRequestData;
	}

	protected void postProcess(SOAPMessage soapRequestMessage) throws Exception {
	}

	protected static SOAPConnection getSoapConnection() throws SOAPException {
		// Create SOAP Connection
		SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection soapConnection = soapConnectionFactory.createConnection();
		return soapConnection;
	}

	public static SOAPMessage createMessage(String soapRequestData, WsStream stream, Map<String, String> headers,
			boolean addSoapHeader) throws SOAPException, SAXException, IOException, ParserConfigurationException {
		SOAPMessage soapRequest = MessageFactory.newInstance().createMessage();

		// SOAPPart part = soapRequest.getSOAPPart();
		// SOAPEnvelope envelope = part.getEnvelope();
		// envelope.addNamespaceDeclaration();

		if (!headers.isEmpty()) {
			MimeHeaders mimeHeaders = soapRequest.getMimeHeaders();

			for (Map.Entry<String, String> e : headers.entrySet()) {
				mimeHeaders.addHeader(e.getKey(), e.getValue());
			}
		}

		if (addSoapHeader)
			stream.addSoapHeaders(soapRequest);

		SOAPBody body = soapRequest.getSOAPBody();

		stream.addBody(soapRequestData, body);
		soapRequest.saveChanges();
		return soapRequest;
	}

	protected void addSoapHeaders(SOAPMessage soapRequest) throws SOAPException {
	}

	protected void addBody(String soapRequestData, SOAPBody body)
			throws SAXException, IOException, SOAPException, ParserConfigurationException {
		// Create Request body XML document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();

		// TODO add catch to warn about bad body
		Document doc = builder.parse(new InputSource(new StringReader(soapRequestData)));
		body.addDocument(doc);
	}

	protected String convertSoapMessageToXMLString(SOAPMessage soapResponse) throws SOAPException, IOException {
		ByteArrayOutputStream soapResponseBaos = new ByteArrayOutputStream();
		soapResponse.writeTo(soapResponseBaos);
		String soapResponseXml = soapResponseBaos.toString();

		return soapResponseXml;
	}

	protected void handleFault(SOAPFault fault) throws Exception {
		throw new Exception(fault.getFaultString());
	}

	@Override
	protected ActivityInfo applyParsers(Object data) throws IllegalStateException, ParseException {
		ActivityInfo activityInfo = super.applyParsers(data);
		semaphore.release();
		return activityInfo;

	}

	/**
	 * Scheduler job to execute JAX-WS call.
	 */
	public static class WsCallJob implements Job {

		/**
		 * Constructs a new WsCallJob.
		 */
		public WsCallJob() {
		}

		@Override
		@SuppressWarnings("unchecked")
		public void execute(JobExecutionContext context) throws JobExecutionException {
			String respStr = null;

			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			WsStream stream = (WsStream) dataMap.get(JOB_PROP_STREAM_KEY);
			String urlStr = dataMap.getString(JOB_PROP_URL_KEY);
			List<String> reqsData = (List<String>) dataMap.get(JOB_PROP_REQ_KEY);
			Semaphore semaphore = (Semaphore) dataMap.get(JOB_PROP_SEMAPHORE);

			if (CollectionUtils.isNotEmpty(reqsData)) {
				for (String reqData : reqsData) {
					try {
						while (semaphore.tryAcquire()) {
							Thread.sleep(50);
						}
						respStr = callWebService(urlStr, reqData, stream);
					} catch (Exception exc) {
						LOGGER.log(OpLevel.WARNING, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
								"WsStream.execute.exception"), exc);
					} finally {
						if (StringUtils.isNotEmpty(respStr)) {
							stream.addInputToBuffer(respStr);
						} else {
							if (semaphore.availablePermits() < 1)
								semaphore.release();
						}

					}
				}
			}
		}
	}

	static {
		disableSslVerification();
	}

	private static void disableSslVerification() {
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (

		NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}
}
