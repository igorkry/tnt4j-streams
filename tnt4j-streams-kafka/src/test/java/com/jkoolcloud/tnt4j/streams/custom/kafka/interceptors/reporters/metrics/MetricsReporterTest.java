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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.*;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser;

/**
 * @author akausinis
 * @version 1.0
 */
public class MetricsReporterTest {

	@Test
	public void collectJMXMetricsAndReport() throws Exception {

		// Prepare part : create and register Mbean
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		TestMbean mbean = new TestMbean("my.name:type=Custom");
		mbean.setAttribute("Long", 15L);
		mbean.setAttribute("Float", 15f);
		mBeanServer.registerMBean(mbean, mbean.name());
		Map<String, Object> attrsMap = new HashMap<>();
		MetricsReporter.collectMetricsJMX("my.name", "my.name:*", mBeanServer, attrsMap);

		// Assure created right
		assertThat(attrsMap.get("my.nameLong"), instanceOf(Long.class));
		assertThat(attrsMap.get("my.nameFloat"), instanceOf(Float.class));

		// Serialize to string
		ObjectMapper mapper = new ObjectMapper();
		String valueToWriteToTopic = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(attrsMap);

		// Try parsing
		DocumentContext context = JsonPath.parse(valueToWriteToTopic);
		Object longV = context.read("$['my.nameLong']");
		Object floatV = context.read("$['my.nameFloat']");

		// Try the way Streams actually does this
		ActivityJsonParser parser = new ActivityJsonParser();
		parser.setDefaultDataType(ActivityFieldDataType.AsInput);
		ActivityField af = new ActivityField("Long");
		ActivityField af2 = new ActivityField("Float");
		af.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "$['my.nameLong']",
				parser.getDefaultDataType()));
		af2.addLocator(new ActivityFieldLocator(ActivityFieldLocatorType.Label, "$['my.nameFloat']",
				parser.getDefaultDataType()));
		parser.addField(af);
		parser.addField(af2);
		ActivityInfo ai = parser.parse(mock(TNTInputStream.class), valueToWriteToTopic);
		assertThat(ai.getFieldValue("Long"), instanceOf(Integer.class));
		assertThat(ai.getFieldValue("Float"), instanceOf(Double.class));

		assertThat(longV, instanceOf(Integer.class));
		assertThat(floatV, instanceOf(Double.class));
	}

	private static class TestMbean implements DynamicMBean {
		private final ObjectName objectName;
		private final Map<String, Object> attrs;

		public TestMbean(String mbeanName) throws MalformedObjectNameException {
			attrs = new HashMap<>();
			objectName = new ObjectName(mbeanName);
		}

		public ObjectName name() {
			return objectName;
		}

		public void setAttribute(String name, Object attr) {
			attrs.put(name, attr);
		}

		@Override
		public Object getAttribute(String name) throws AttributeNotFoundException, MBeanException, ReflectionException {
			if (attrs.containsKey(name)) {
				return attrs.get(name);
			} else {
				throw new AttributeNotFoundException("Could not find attribute " + name);
			}
		}

		@Override
		public AttributeList getAttributes(String[] names) {
			AttributeList list = new AttributeList();
			for (String name : names) {
				try {
					list.add(new Attribute(name, getAttribute(name)));
				} catch (Exception e) {
				}
			}
			return list;
		}

		public Object removeAttribute(String name) {
			return attrs.remove(name);
		}

		@Override
		public MBeanInfo getMBeanInfo() {
			MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[this.attrs.size()];
			int i = 0;
			for (Map.Entry<String, Object> entry : this.attrs.entrySet()) {
				String attribute = entry.getKey();
				Object metric = entry;
				attrs[i] = new MBeanAttributeInfo(attribute, double.class.getName(), "TEST", true, false, false);
				i += 1;
			}
			return new MBeanInfo(getClass().getName(), "", attrs, null, null, null);
		}

		@Override
		public Object invoke(String name, Object[] params, String[] sig) throws MBeanException, ReflectionException {
			throw new UnsupportedOperationException("Set not allowed.");
		}

		@Override
		public void setAttribute(Attribute attribute)
				throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
			throw new UnsupportedOperationException("Set not allowed.");
		}

		@Override
		public AttributeList setAttributes(AttributeList list) {
			throw new UnsupportedOperationException("Set not allowed.");
		}

	}

}