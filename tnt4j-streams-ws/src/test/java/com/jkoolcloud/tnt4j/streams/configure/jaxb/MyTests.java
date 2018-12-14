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

package com.jkoolcloud.tnt4j.streams.configure.jaxb;

import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

/**
 * The type My tests.
 *
 * @author akausinis
 * @version 1.0
 */
public class MyTests {

	/**
	 * Stream config read jaxb test.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void streamConfigReadJAXBTest() throws Exception {
		JAXBContext jc = JAXBContext.newInstance("com.jkoolcloud.tnt4j.streams.configure.jaxb"); // NON-NLS
		Unmarshaller u = jc.createUnmarshaller();
		TntDataSource sConfig = (TntDataSource) u
				.unmarshal(new File("./samples/restful-stream-json/tnt-data-source.xml"));

		assertEquals("Stream count does not match", 1, sConfig.getStream().size());
		assertEquals("Parsers count does not match", 1, sConfig.getParser().size());

		assertEquals("Unexpected stream name", "RESTfulSampleJSONStream", sConfig.getStream().get(0).getName());
	}
}