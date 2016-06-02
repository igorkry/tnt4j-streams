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

package com.jkoolcloud.tnt4j.streams.configure.sax;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;

import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.inputs.TNTParseableInputStream;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class ConfigParserHandlerTest {

	private static List<String> skipConfigurationsList;
	private static File samplesDir;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		initSamplesDir();
	}

	private static void initSamplesDir() throws Exception {
		skipConfigurationsList = new ArrayList<String>();

		samplesDir = new File("./samples/");
		if (!samplesDir.isDirectory()) {
			samplesDir = new File("./tnt4j-streams-core/samples/");
			if (!samplesDir.isDirectory()) {
				fail("Samples root directory doesn't exist");
			}
		} else {
			skipConfigurationsList.add("java-stream");
		}
	}

	@Test
	public void streamsSamplesConfigTest() throws Exception {
		validateConfigs(samplesDir, "tnt-data-source*.xml", true, skipConfigurationsList);
		validateConfigs(samplesDir, "parsers*.xml", false, null);
	}

	protected void validateConfigs(File samplesDir, String configFileWildcard, boolean checkStreams,
			List<String> skipFiles) throws Exception {
		Collection<File> sampleConfigurations = FileUtils.listFiles(samplesDir,
				FileFilterUtils
						.asFileFilter((FilenameFilter) new WildcardFileFilter(configFileWildcard, IOCase.INSENSITIVE)),
				TrueFileFilter.INSTANCE);

		Collection<File> sampleConfigurationsFiltered = new ArrayList<File>(sampleConfigurations);
		if (CollectionUtils.isNotEmpty(skipFiles)) {
			for (File sampleConfiguration : sampleConfigurations) {
				for (String skipFile : skipFiles) {
					if (sampleConfiguration.getAbsolutePath().contains(skipFile))
						sampleConfigurationsFiltered.remove(sampleConfiguration);
				}
			}
		}

		for (File sampleConfiguration : sampleConfigurationsFiltered) {
			System.out.println("Reading configuration file: " + sampleConfiguration.getAbsolutePath());
			Reader testReader = new FileReader(sampleConfiguration);
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			ConfigParserHandler hndlr = new ConfigParserHandler();
			parser.parse(new InputSource(testReader), hndlr);

			assertNotNull("Parsed streams config data is null", hndlr.getStreamsConfigData());
			boolean parseable = true;
			if (checkStreams) {
				assertTrue("No configured streams", hndlr.getStreamsConfigData().isStreamsAvailable());

				parseable = false;
				for (TNTInputStream<?, ?> s : hndlr.getStreamsConfigData().getStreams()) {
					if (s instanceof TNTParseableInputStream) {
						parseable = true;
						break;
					}
				}
			}
			if (parseable) {
				assertTrue("No configured parsers", hndlr.getStreamsConfigData().isParsersAvailable());
			}

			Utils.close(testReader);
		}
	}

}
