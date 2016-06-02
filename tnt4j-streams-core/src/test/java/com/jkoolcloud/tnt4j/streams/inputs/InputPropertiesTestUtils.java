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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;

/**
 * @author akausinis
 * @version 1.0
 */
public final class InputPropertiesTestUtils {

	public static Collection<Map.Entry<String, String>> makeTestPropertiesSet(final String propertyName,
			final Object testValue) {

		Collection<Map.Entry<String, String>> props = new ArrayList<Entry<String, String>>(1);
		props.add(new AbstractMap.SimpleEntry<String, String>(propertyName, String.valueOf(testValue)));
		return props;
	}

	public static void testInputPropertySetAndGet(TNTInputStream<?, ?> input, final String propertyName,
			final Object testValue) throws Exception {
		final Collection<Entry<String, String>> props = makeTestPropertiesSet(propertyName, testValue);
		input.setProperties(props);
		Assert.assertEquals(testValue, input.getProperty(propertyName));
	}
}
