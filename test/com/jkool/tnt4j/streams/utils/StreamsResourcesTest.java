/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.utils;

import static org.junit.Assert.*;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.ResourceBundle;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamsResourcesTest {

	@Test
	public void testGetBundle() {
		assertNotNull(StreamsResources.getBundle());
	}

	@Test
	public void testGetString() {
		final String bundle = "resources/tnt4j-streams";
		final ResourceBundle bundle2 = ResourceBundle.getBundle(bundle);
		final Enumeration<String> keys = bundle2.getKeys();
		while (keys.hasMoreElements()) {
			final String nextElement = keys.nextElement();
			final String resource = StreamsResources.getString(nextElement);
			assertNotNull(resource);
			if (resource.contains("{1}") && !resource.contains("{2}")) {
				final String testVar = "TEST";
				final String formated = StreamsResources.getStringFormatted(resource, testVar);
				MessageFormat.format(resource, testVar);
				assertEquals(String.format(resource, testVar), formated);
			}
		}
	}

	@Test
	public void testNull() {
		Object string = StreamsResources.getString((String) null);
		assertNull(string);
		string = StreamsResources.getStringFormatted(null);
		assertNull(string);
		string = StreamsResources.getString((Enum<?>) null);
		assertNull(string);
	}

}
