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

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;

import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.jkool.tnt4j.streams.fields.WsScenario;
import com.jkool.tnt4j.streams.fields.WsScenarioStep;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.WsStreamConstants;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * @author akausinis
 * @version 1.0 TODO
 */
public class WsStream extends AbstractWsStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WsStream.class);

	/**
	 * Constructs an empty WsStream. Requires configuration settings to set
	 * input stream source.
	 */
	public WsStream() {
		super(LOGGER);
	}

	@Override
	protected JobDetail buildJob(WsScenario scenario, WsScenarioStep step, JobDataMap jobAttrs) {
		return JobBuilder.newJob(WsCallJob.class).withIdentity(scenario.getName() + ":" + step.getName()) // NON-NLS
				.usingJobData(jobAttrs).usingJobData(JOB_PROP_URL_KEY, step.getUrlStr())
				.usingJobData(JOB_PROP_REQ_KEY, step.getRequest()).build();
	}

	/**
	 * TODO
	 *
	 * @param soapRequestXml
	 * @param url
	 * @return
	 * @throws Exception
	 */
	protected static String callWebService(String soapRequestXml, String url) throws Exception {
		if (StringUtils.isEmpty(url)) {
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "WsStream.cant.execute.request"),
					url);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "WsStream.invoking.request"), url,
				soapRequestXml);

		// Create Request body XML document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(soapRequestXml)));

		// Create SOAP Connection
		SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection soapConnection = soapConnectionFactory.createConnection();

		// Create SOAP message and set request XML as body
		SOAPMessage soapRequest = MessageFactory.newInstance().createMessage();

		// SOAPPart part = soapRequest.getSOAPPart();
		// SOAPEnvelope envelope = part.getEnvelope();
		// envelope.addNamespaceDeclaration();

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
	 * TODO
	 */
	public static class WsCallJob implements Job {

		/**
		 * TODO
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
				respStr = callWebService(reqData, urlStr);
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING,
						StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_WS, "WsStream.execute.exception"),
						exc);
			}

			if (StringUtils.isNotEmpty(respStr)) {
				stream.addInputToBuffer(respStr);
			}
		}
	}
}
