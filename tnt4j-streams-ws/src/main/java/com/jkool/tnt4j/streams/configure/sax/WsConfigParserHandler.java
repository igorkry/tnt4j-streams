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

package com.jkool.tnt4j.streams.configure.sax;

import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.jkool.tnt4j.streams.fields.CronSchedulerData;
import com.jkool.tnt4j.streams.fields.SimpleSchedulerData;
import com.jkool.tnt4j.streams.fields.WsScenario;
import com.jkool.tnt4j.streams.fields.WsScenarioStep;
import com.jkool.tnt4j.streams.inputs.AbstractWsStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.WsStreamConstants;

/**
 * Extends default TNT4J-Streams configuration SAX parser handler and adds
 * additional TNT4J-Streams-WS configuration handling.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.configure.StreamsConfigLoader
 * @see com.jkool.tnt4j.streams.configure.sax.StreamsConfigSAXParser
 */
public class WsConfigParserHandler extends ConfigParserHandler {

	private static final String SCENARIO_ELMT = "scenario"; // NON-NLS
	private static final String STEP_ELMT = "step"; // NON-NLS
	private static final String SCHED_CRON_ELMT = "schedule-cron"; // NON-NLS
	private static final String SCHED_SIMPLE_ELMT = "schedule-simple"; // NON-NLS
	private static final String REQ_ELMT = "request"; // NON-NLS

	private static final String URL_ATTR = "url"; // NON-NLS
	private static final String METHOD_ATTR = "method"; // NON-NLS

	private static final String EXPRESSION_ATTR = "expression"; // NON-NLS

	private static final String INTERVAL_ATTR = "interval"; // NON-NLS
	private static final String REPEATS_ATTR = "repeatCount"; // NON-NLS

	private WsScenario currScenario;
	private WsScenarioStep currStep;

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();

		currScenario = null;
		currStep = null;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);

		if (SCENARIO_ELMT.equals(qName)) {
			processScenario(attributes);
		} else if (STEP_ELMT.equals(qName)) {
			processScenarioStep(attributes);
		} else if (SCHED_CRON_ELMT.equals(qName)) {
			processCronScheduler(attributes);
		} else if (SCHED_SIMPLE_ELMT.equals(qName)) {
			processSimpleScheduler(attributes);
		} else if (REQ_ELMT.equals(qName)) {
			processRequest(attributes);
		}
	}

	private void processScenario(Attributes attrs) throws SAXParseException {
		if (currScenario != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.malformed.configuration", SCENARIO_ELMT), currParseLocation);
		}

		String name = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			}
		}

		if (StringUtils.isEmpty(name)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", SCENARIO_ELMT, NAME_ATTR), currParseLocation);
		}

		currScenario = new WsScenario(name);
	}

	private void processScenarioStep(Attributes attrs) throws SAXException {
		if (currScenario == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.malformed.configuration2", STEP_ELMT, SCENARIO_ELMT),
					currParseLocation);
		}
		String name = null;
		String url = null;
		String method = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (URL_ATTR.equals(attName)) {
				url = attValue;
			} else if (METHOD_ATTR.equals(attName)) {
				method = attValue;
			}
		}
		if (StringUtils.isEmpty(name)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", STEP_ELMT, NAME_ATTR), currParseLocation);
		}

		currStep = new WsScenarioStep(name);
		currStep.setUrlStr(url);
		currStep.setMethod(method);
	}

	private void processCronScheduler(Attributes attrs) throws SAXException {
		if (currStep == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.malformed.configuration2", SCHED_CRON_ELMT, STEP_ELMT),
					currParseLocation);
		}

		if (currStep.getSchedulerData() != null) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"WsConfigParserHandler.element.has.multiple", STEP_ELMT, SCHED_CRON_ELMT, SCHED_SIMPLE_ELMT,
					getLocationInfo()));
		}

		String expression = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (EXPRESSION_ATTR.equals(attName)) {
				expression = attValue;
			}
		}
		if (StringUtils.isEmpty(expression)) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.missing.attribute", SCHED_CRON_ELMT, EXPRESSION_ATTR),
					currParseLocation);
		}

		CronSchedulerData schedData = new CronSchedulerData(expression);

		currStep.setSchedulerData(schedData);
	}

	private void processSimpleScheduler(Attributes attrs) throws SAXException {
		if (currStep == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.malformed.configuration2", SCHED_SIMPLE_ELMT, STEP_ELMT),
					currParseLocation);
		}

		if (currStep.getSchedulerData() != null) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"WsConfigParserHandler.element.has.multiple", STEP_ELMT, SCHED_CRON_ELMT, SCHED_SIMPLE_ELMT,
					getLocationInfo()));
		}

		Integer interval = null;
		String timeUnits = null;
		Integer repeatCount = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (INTERVAL_ATTR.equals(attName)) {
				interval = Integer.parseInt(attValue);
			} else if (UNITS_ATTR.equals(attName)) {
				timeUnits = attValue;
			} else if (REPEATS_ATTR.equals(attName)) {
				repeatCount = Integer.parseInt(attValue);
			}
		}
		if (interval == null || interval < 0) {
			throw new SAXException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_WS,
					"WsConfigParserHandler.attr.missing.or.illegal", SCHED_SIMPLE_ELMT, INTERVAL_ATTR, interval,
					getLocationInfo()));
		}

		SimpleSchedulerData schedData = new SimpleSchedulerData(interval);
		schedData.setUnits(
				StringUtils.isEmpty(timeUnits) ? TimeUnit.MILLISECONDS : TimeUnit.valueOf(timeUnits.toUpperCase()));
		schedData.setRepeatCount(repeatCount);

		currStep.setSchedulerData(schedData);
	}

	private void processRequest(Attributes attrs) throws SAXException {
		if (currStep == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.malformed.configuration2", REQ_ELMT, STEP_ELMT), currParseLocation);
		}

		cdata = new StringBuilder();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);

		try {
			if (SCENARIO_ELMT.equals(qName)) {
				if (currScenario.isEmpty()) {
					throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"WsConfigParserHandler.element.must.have.one", SCENARIO_ELMT, STEP_ELMT,
							getLocationInfo()));
				}

				((AbstractWsStream) currStream).addScenario(currScenario);
				currScenario = null;
			} else if (STREAM_ELMT.equals(qName)) {
				if (currStream instanceof AbstractWsStream) {
					if (CollectionUtils.isEmpty(((AbstractWsStream) currStream).getScenarios())) {
						throw new SAXException(StreamsResources.getStringFormatted(
								StreamsResources.RESOURCE_BUNDLE_CORE, "WsConfigParserHandler.element.must.have.one",
								STREAM_ELMT, SCENARIO_ELMT, getLocationInfo()));
					}
				}
			} else if (STEP_ELMT.equals(qName)) {
				if (StringUtils.isEmpty(currStep.getRequest()) && StringUtils.isEmpty(currStep.getUrlStr())) {
					throw new SAXParseException(
							StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
									"ConfigParserHandler.must.contain", STEP_ELMT, URL_ATTR, REQ_ELMT),
							currParseLocation);
				}

				currScenario.addStep(currStep);
				currStep = null;
			} else if (REQ_ELMT.equals(qName)) {
				currStep.setRequest(cdata == null ? null : cdata.toString());
				cdata = null;
			}
		} catch (SAXException exc) {
			throw exc;
		} catch (Exception e) {
			SAXException se = new SAXException(e.getLocalizedMessage() + getLocationInfo());
			se.initCause(e);
			throw se;
		}
	}

}
