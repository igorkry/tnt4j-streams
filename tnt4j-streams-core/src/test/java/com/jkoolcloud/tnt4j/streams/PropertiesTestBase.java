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

package com.jkoolcloud.tnt4j.streams;

import static org.junit.Assert.assertNotNull;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public abstract class PropertiesTestBase {

	@SuppressWarnings("serial")
	public static class PropertyList extends ArrayList<AbstractMap.SimpleEntry<String, String>> {
		public PropertyList add(String key, String Value) {
			add(new AbstractMap.SimpleEntry<String, String>(key, Value));
			return this;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Collection<Entry<String, String>> build() {
			return (Collection) this;
		}
	}

	public static PropertyList getPropertyList() {
		return new PropertyList();
	}

	public static void testPropertyList(TNTInputStream<?, ?> stream,
			Collection<Entry<String, String>> propertiesToTest) {
		for (Entry<String, String> property : propertiesToTest) {
			Object result = stream.getProperty(property.getKey());
			assertNotNull("Property " + property.getKey() + " was NULL", result);

		}
	}

}
