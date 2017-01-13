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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Map;

import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public final class TestUtils {

	public static void testPropertyList(TNTInputStream<?, ?> stream,
			Collection<Map.Entry<String, String>> propertiesToTest) {
		for (Map.Entry<String, String> property : propertiesToTest) {
			String name = property.getKey();
			Object result = stream.getProperty(name);
			assertNotNull("Property " + name + " is null", result); // NON-NLS
			assertEquals("Property not set as expected", property.getValue(), String.valueOf(result));
		}
	}
}
