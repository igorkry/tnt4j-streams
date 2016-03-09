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

package com.jkool.tnt4j.streams.inputs;

import static org.junit.Assert.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamProperties;

/**
 * @author akausinis
 * @version 1.0
 */
public class AbstractFileLineStreamTest {
	boolean inputEnded = false;
	AbstractFileLineStream afls = new FileLineStream();

	@Test
	public void setPropertiesTest() throws Exception {
		final String fileName = "c:/Windows/schemas/TSWorkSpace/tswcx.xml";
		Collection<Map.Entry<String, String>> props = new ArrayList<Map.Entry<String, String>>(1);
		props.add(new AbstractMap.SimpleEntry<String, String>(StreamProperties.PROP_FILENAME, fileName));

		afls.setProperties(props);
		final Object property = afls.getProperty(StreamProperties.PROP_FILENAME);
		assertTrue(fileName.equals(property));
	}

}
