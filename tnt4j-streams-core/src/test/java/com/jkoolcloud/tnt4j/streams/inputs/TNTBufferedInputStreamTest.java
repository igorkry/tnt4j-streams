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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.PropertiesTestBase;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;

/**
 * @author akausinis
 * @version 1.0
 */
public class TNTBufferedInputStreamTest extends PropertiesTestBase {
	private EventSink logger;

	@Before
	public void initTest() {
		logger = mock(EventSink.class);
	}

	@Test
	public void setPropertiesTest() throws Exception {
		TNTBufferedInputStream my = Mockito.mock(TNTBufferedInputStream.class, Mockito.CALLS_REAL_METHODS);
		final Collection<Entry<String, String>> props = getPropertyList()
				.add(StreamProperties.PROP_BUFFER_SIZE, "99999").add(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "5")
				.add(StreamProperties.PROP_OFFER_TIMEOUT, "999").build();
		my.setProperties(props);
		for (Map.Entry<String, String> property : props) {
			String name = property.getKey();
			String expectedValue = property.getValue();
			assertEquals("Property not set as expected", expectedValue, my.getProperty(name).toString());
		}
	}

	@Test
	public void setPropertiesNullTest() throws Exception {
		TNTBufferedInputStream my = Mockito.mock(TNTBufferedInputStream.class, Mockito.CALLS_REAL_METHODS);
		my.setProperties(null);
		assertNull(my.getProperty("PROP_EXECUTORS_BOUNDED"));
	}

}
