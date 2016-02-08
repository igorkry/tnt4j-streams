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

package com.jkool.tnt4j.streams.configure;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.xml.sax.InputSource;

/**
 * @author akausinis
 * @version 1.0
 */
public class ConfigParserHandlerTest {
	@Rule
	public ErrorCollector collector = new ErrorCollector();

	List<File> excludeFile = new ArrayList<File>() {
		{
			add(new File("./Samples/tran-stitch/tnt-msg-activity.xml"));
		}
	};

	@Test
	public void samplesCanParseTest() throws Exception {

		List<File> sampleConfigurations = new ArrayList<File>();
		File samples = new File("./Samples/");
		if (!samples.isDirectory())
			fail();

		for (File subdir : samples.listFiles()) {
			if (subdir.isDirectory()) {
				for (File config : subdir.listFiles()) {
					if (!config.isDirectory() && config.getName().endsWith("xml") && !excludeFile.contains(config))
						sampleConfigurations.add(config);
				}
			}
		}

		for (File sampleConfiguration : sampleConfigurations) {

			System.out.println("Reading configuration file:" + sampleConfiguration.getAbsolutePath());
			Reader testReader = new FileReader(sampleConfiguration);
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			ConfigParserHandler hndlr = new ConfigParserHandler();
			parser.parse(new InputSource(testReader), hndlr);

			collector.checkThat(hndlr.getStreams(), notNullValue());
			collector.checkThat(hndlr.getParsers(), notNullValue());

			if (hndlr.getStreams() == null || hndlr.getParsers() == null)
				System.err.println("FAILED!");
		}

	}

}
