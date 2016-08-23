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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;

import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenario;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * <p>
 * Implements a scheduled JAX-WS service call activity stream, where each call response is assumed to represent a single
 * activity or event which should be recorded.
 * <p>
 * Service call is performed by invoking {@link SOAPConnection#call(SOAPMessage, Object)}. Provided request XML data is
 * set as {@link SOAPMessage} body data.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 *
 * @version $Revision: 1 $
 *
 * @see ActivityParser#isDataClassSupported(Object)
 * @see SOAPConnection#call(SOAPMessage, Object)
 */
public class WsStream extends AbstractWsStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WsStream.class);

	/**
	 * Constructs an empty WsStream. Requires configuration settings to set input stream source.
	 */
	public WsStream() {
		super(LOGGER);
	}

	@Override
	protected JobDetail buildJob(WsScenario scenario, WsScenarioStep step, JobDataMap jobAttrs) {
		jobAttrs.put(JOB_PROP_URL_KEY, step.getUrlStr());
		jobAttrs.put(JOB_PROP_REQ_KEY, step.getRequest());

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
	protected static String callWebService(String url, String soapRequestData) throws Exception {
		if (StringUtils.isEmpty(url)) {
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "WsStream.cant.execute.request"),
					url);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "WsStream.invoking.request"), url,
				soapRequestData);

		Map<String, String> headers = new HashMap<String, String>();
		// separate SOAP message header values from request body XML
		BufferedReader br = new BufferedReader(new StringReader(soapRequestData));
		StringBuilder sb = new StringBuilder();
		try {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().startsWith("<")) { // NON-NLS
					sb.append(line).append('\n'); // NON-NLS
				} else {
					int bi = line.indexOf(':'); // NON-NLS
					if (bi >= 0) {
						String hKey = line.substring(0, bi).trim();
						String hValue = line.substring(bi + 1).trim();
						headers.put(hKey, hValue);
					} else {
						sb.append(line).append('\n'); // NON-NLS
					}
				}
			}
		} finally {
			Utils.close(br);
		}

		soapRequestData = sb.toString();

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "WsStream.invoking.request"), url,
				soapRequestData);

		// Create Request body XML document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(soapRequestData)));

		// Create SOAP Connection
		SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection soapConnection = soapConnectionFactory.createConnection();

		// Create SOAP message and set request XML as body
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

		SOAPBody body = soapRequest.getSOAPBody();
		body.addDocument(doc);
		soapRequest.saveChanges();

		// Send SOAP Message to SOAP Server
		SOAPMessage soapResponse = soapConnection.call(soapRequest, url);

		ByteArrayOutputStream soapResponseBaos = new ByteArrayOutputStream();
		soapResponse.writeTo(soapResponseBaos);
		String soapResponseXml = soapResponseBaos.toString();

		return soapResponseXml;
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
			String respStr = null;

			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			AbstractWsStream stream = (AbstractWsStream) dataMap.get(JOB_PROP_STREAM_KEY);
			String urlStr = dataMap.getString(JOB_PROP_URL_KEY);
			String reqData = dataMap.getString(JOB_PROP_REQ_KEY);

			try {
				respStr = callWebService(urlStr, reqData);
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
						"WsStream.execute.exception"), exc);
			}

			if (StringUtils.isNotEmpty(respStr)) {
				stream.addInputToBuffer(respStr);
			}
		}
	}
}
