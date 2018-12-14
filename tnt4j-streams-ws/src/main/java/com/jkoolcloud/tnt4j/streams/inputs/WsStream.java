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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Semaphore;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.scenario.WsRequest;
import com.jkoolcloud.tnt4j.streams.scenario.WsResponse;
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
 * This activity stream requires parsers that can support {@link String} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided string.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractWsStream}):
 * <ul>
 * <li>DisableSSL - flag indicating that stream should disable SSL context verification. Default value - {@code false}.
 * (Optional)</li>
 * <li>SynchronizeRequests - flag indicating that stream issued WebService requests shall be synchronized and handled in
 * configuration defined sequence - waiting for prior request to complete before issuing next. Default value -
 * {@code false}. (Optional)</li>
 * <li>List of custom WS Stream requests configuration properties. Put variable placeholder in request/step
 * configuration (e.g. {@code ${WsEndpoint}}) and put property with same name into stream properties list (e.g.
 * {@code "<property name="WsEndpoint" value="https://192.168.3.3/ws"/>"}) to have value mapped into request data.
 * (Optional)</li>
 * </ul>
 *
 * @version $Revision: 2 $
 *
 * @see ActivityParser#isDataClassSupported(Object)
 * @see SOAPConnection#call(SOAPMessage, Object)
 */
public class WsStream extends AbstractWsStream<String> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WsStream.class);

	/**
	 * Constant for name of built-in scheduler job property {@value}.
	 */
	protected static final String JOB_PROP_SEMAPHORE = "semaphoreObj"; // NON-NLS

	private Semaphore semaphore;

	private boolean disableSSL = false;
	private boolean synchronizeRequests = false;

	/**
	 * Contains custom WS Stream requests configuration properties.
	 */
	protected Map<String, String> wsProperties = new HashMap<>();

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
	protected long getActivityItemByteSize(WsResponse<String> item) {
		return item == null || item.getData() == null ? 0 : item.getData().getBytes().length;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();

				if (WsStreamProperties.PROP_DISABLE_SSL.equalsIgnoreCase(name)) {
					disableSSL = Utils.toBoolean(value);
				} else if (WsStreamProperties.PROP_SYNCHRONIZE_REQUESTS.equalsIgnoreCase(name)) {
					synchronizeRequests = Utils.toBoolean(value);
				} else {
					wsProperties.put(name, value);
				}
			}
		}

		if (disableSSL) {
			disableSslVerification();
		}

		if (synchronizeRequests) {
			semaphore = new Semaphore(1);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WsStreamProperties.PROP_DISABLE_SSL.equalsIgnoreCase(name)) {
			return disableSSL;
		}

		if (WsStreamProperties.PROP_SYNCHRONIZE_REQUESTS.equalsIgnoreCase(name)) {
			return synchronizeRequests;
		}

		return super.getProperty(name);
	}

	@Override
	protected JobDetail buildJob(String jobId, JobDataMap jobAttrs) {
		jobAttrs.put(JOB_PROP_SEMAPHORE, semaphore);

		return JobBuilder.newJob(WsCallJob.class).withIdentity(jobId).usingJobData(jobAttrs).build();
	}

	/**
	 * Performs JAX-WS service call using SOAP API.
	 *
	 * @param url
	 *            JAX-WS service URL
	 * @param soapRequestData
	 *            JAX-WS service request data: headers and body XML string
	 * @param stream
	 *            stream instance to use for service call
	 * @param scenario
	 *            scenario of executed request
	 * @return service response string
	 * @throws Exception
	 *             if exception occurs while performing JAX-WS service call
	 */
	protected static String callWebService(String url, String soapRequestData, WsStream stream, WsScenario scenario)
			throws Exception {
		if (StringUtils.isEmpty(url)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"WsStream.cant.execute.request", url);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"WsStream.invoking.request.raw", url, soapRequestData);

		RequestDataAndHeaders requestDataAndHeaders = new RequestDataAndHeaders().resolve(soapRequestData, stream);
		soapRequestData = stream.preProcess(requestDataAndHeaders.getRequest());

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"WsStream.invoking.request.prep", url, soapRequestData);

		// Create SOAP message and set request XML as body
		SOAPMessage soapRequestMessage = createMessage(soapRequestData, requestDataAndHeaders.getHeaders(), true,
				stream);

		// Send SOAP Message to SOAP Server
		SOAPConnection soapConnection = createSOAPConnection();
		SOAPMessage soapResponse = soapConnection.call(soapRequestMessage, url);
		String respXML = stream.toXMLString(soapResponse);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"WsStream.received.response", url, respXML);

		if (soapResponse.getSOAPBody().hasFault()) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"WsStream.received.failure.response", url, soapResponse.getSOAPBody().getFault().getFaultString());
			stream.handleFault(soapResponse.getSOAPBody().getFault(), scenario);
			return null;
		}

		return respXML;
	}

	/**
	 * Disables SSL context verification.
	 */
	protected static void disableSslVerification() {
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
			SSLContext sc = SSLContext.getInstance("SSL"); // NON-NLS
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
		} catch (GeneralSecurityException exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME), "WsStream.disable.ssl.failed",
					exc);
		}
	}

	/**
	 * Create a new {@link SOAPConnection} instance.
	 *
	 * @return SOAP connection instance
	 * @throws SOAPException
	 *             if there was an exception creating the SOAP connection object
	 */
	protected static SOAPConnection createSOAPConnection() throws SOAPException {
		// Create SOAP Connection
		SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection soapConnection = soapConnectionFactory.createConnection();
		return soapConnection;
	}

	/**
	 * Creates a new {@link javax.xml.soap.SOAPMessage} instance using provided request headers and body data.
	 *
	 * @param soapRequestData
	 *            SOAP request body data to add
	 * @param headers
	 *            SOAP request headers to add
	 * @param addStreamHeaders
	 *            flag indicating whether to add stream specific additional SOAP headers
	 * @param stream
	 *            stream instance to use while creating SOAP message
	 * @return SOAP message instance created using provided request data
	 * @throws SOAPException
	 *             if there was a problem saving changes to this message
	 * @throws SAXException
	 *             if any parse errors occur
	 * @throws IOException
	 *             if any I/O errors occur
	 * @throws ParserConfigurationException
	 *             if a {@link DocumentBuilder} cannot be created which satisfies the configuration requested
	 *
	 * @see #addSoapHeaders(javax.xml.soap.SOAPMessage)
	 * @see #addBody(javax.xml.soap.SOAPBody, String)
	 */
	public static SOAPMessage createMessage(String soapRequestData, Map<String, String> headers,
			boolean addStreamHeaders, WsStream stream)
			throws SOAPException, SAXException, IOException, ParserConfigurationException {
		SOAPMessage soapRequest = MessageFactory.newInstance().createMessage();

		// SOAPPart part = soapRequest.getSOAPPart();
		// SOAPEnvelope envelope = part.getEnvelope();
		// envelope.addNamespaceDeclaration();

		if (MapUtils.isNotEmpty(headers)) {
			MimeHeaders mimeHeaders = soapRequest.getMimeHeaders(); // TODO: SOAP headers???

			for (Map.Entry<String, String> e : headers.entrySet()) {
				mimeHeaders.addHeader(e.getKey(), e.getValue());
			}
		}

		if (addStreamHeaders) {
			stream.addSoapHeaders(soapRequest);
		}

		SOAPBody body = soapRequest.getSOAPBody();

		stream.addBody(body, soapRequestData);
		soapRequest.saveChanges();
		return soapRequest;
	}

	/**
	 * Appends stream specific additional headers data to SOAP request message.
	 *
	 * @param soapRequest
	 *            SOAP request message instance
	 * @throws SOAPException
	 *             if there was an error adding the SOAP message header
	 */
	protected void addSoapHeaders(SOAPMessage soapRequest) throws SOAPException {
	}

	/**
	 * Makes {@link org.w3c.dom.Document} from provided SOAP request body data string <tt>soapRequestData</tt> and adds
	 * is to SOAP message body <tt>body</tt> element.
	 *
	 * @param body
	 *            SOAP message body element
	 * @param soapRequestData
	 *            SOAP request body data string
	 * @throws SAXException
	 *             if any parse errors occur
	 * @throws IOException
	 *             if any I/O errors occur
	 * @throws SOAPException
	 *             if the {@link Document} cannot be added
	 * @throws ParserConfigurationException
	 *             if a {@link DocumentBuilder} cannot be created which satisfies the configuration requested
	 */
	protected void addBody(SOAPBody body, String soapRequestData)
			throws SAXException, IOException, SOAPException, ParserConfigurationException {
		// Create Request body XML document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();

		// TODO add catch to warn about bad body
		Document doc = builder.parse(new InputSource(new StringReader(soapRequestData)));
		body.addDocument(doc);
	}

	/**
	 * Converts provided SOAP message to XML representation.
	 *
	 * @param soapMsg
	 *            SOAP message instance to convert
	 * @return SOAP message XML representation string
	 * @throws SOAPException
	 *             if there was a problem in externalizing this SOAP message
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected String toXMLString(SOAPMessage soapMsg) throws SOAPException, IOException {
		try (ByteArrayOutputStream soapResponseBaos = new ByteArrayOutputStream()) {
			soapMsg.writeTo(soapResponseBaos);

			return soapResponseBaos.toString();
		}
	}

	/**
	 * Handles response contained SOAP fault. This stream just throws {@link java.lang.RuntimeException} instance with
	 * SOAP fault explanation as message.
	 *
	 * @param fault
	 *            SOAP fault to handle
	 * @param scenario
	 *            scenario of failed request
	 */
	protected void handleFault(SOAPFault fault, WsScenario scenario) {
		throw new RuntimeException(fault.getFaultString());
	}

	@Override
	protected ActivityInfo applyParsers(Object data) throws IllegalStateException, ParseException {
		try {
			return super.applyParsers(data);
		} finally {
			if (semaphore != null) {
				semaphore.release();
			}
		}
	}

	/**
	 * Fills in WS request fragment string having variable expressions with parameters stored in {@link #wsProperties}
	 * map.
	 *
	 * @param reqDataStr
	 *            WS request fragment string
	 * @return variable values filled in WS request fragment string
	 */
	protected String fillInRequestData(String reqDataStr) {
		return fillInRequestData(reqDataStr, wsProperties);
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
		public void execute(JobExecutionContext context) throws JobExecutionException {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			WsStream stream = (WsStream) dataMap.get(JOB_PROP_STREAM_KEY);
			WsScenarioStep scenarioStep = (WsScenarioStep) dataMap.get(JOB_PROP_SCENARIO_STEP_KEY);
			Semaphore semaphore = (Semaphore) dataMap.get(JOB_PROP_SEMAPHORE);

			if (!scenarioStep.isEmpty()) {
				String respStr;
				for (WsRequest<String> request : scenarioStep.getRequests()) {
					respStr = null;
					try {
						if (semaphore != null) {
							while (semaphore.tryAcquire()) {
								Thread.sleep(50);
							}
						}
						respStr = callWebService(stream.fillInRequestData(scenarioStep.getUrlStr()),
								stream.fillInRequestData(request.getData()), stream, scenarioStep.getScenario());
					} catch (Exception exc) {
						Utils.logThrowable(LOGGER, OpLevel.WARNING,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"WsStream.execute.exception", exc);
					} finally {
						if (StringUtils.isNotEmpty(respStr)) {
							stream.addInputToBuffer(new WsResponse<>(respStr, request.getTags()));
						} else {
							if (semaphore != null && semaphore.availablePermits() < 1) {
								semaphore.release();
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Container used to retrieve JAX-WS request headers and body from stream configuration defined request data.
	 */
	public static class RequestDataAndHeaders {
		private Map<String, String> headers;
		private String request;

		/**
		 * Returns SOAP request headers map.
		 *
		 * @return SOAP request headers map
		 */
		public Map<String, String> getHeaders() {
			return headers;
		}

		/**
		 * Returns SOAP request body string.
		 *
		 * @return SOAP request body string
		 */
		public String getRequest() {
			return request;
		}

		/**
		 * Resolves JAX-WS request headers and body data from stream configuration defined request data string.
		 *
		 * @param soapRequestData
		 *            JAX-WS service request data: headers and body XML string
		 * @param stream
		 *            stream instance to use for service call
		 * @return instance of this request data container
		 * @throws IOException
		 *             if an I/O error occurs reading request data
		 */
		public RequestDataAndHeaders resolve(String soapRequestData, WsStream stream) throws IOException {
			headers = new HashMap<>(5);
			StringBuilder sb = new StringBuilder();
			// separate SOAP message header values from request body XML
			try (BufferedReader br = new BufferedReader(new StringReader(soapRequestData))) {
				String line;
				while ((line = br.readLine()) != null) {
					if (line.trim().startsWith("<")) { // NON-NLS
						sb.append(line).append(Utils.NEW_LINE);
					} else {
						int bi = line.indexOf(':'); // NON-NLS
						if (bi >= 0) {
							String hKey = line.substring(0, bi).trim();
							String hValue = stream.fillInRequestData(line.substring(bi + 1).trim());
							headers.put(hKey, hValue);
						} else {
							sb.append(line).append(Utils.NEW_LINE);
						}
					}
				}
			}

			request = sb.toString();

			return this;
		}
	}
}