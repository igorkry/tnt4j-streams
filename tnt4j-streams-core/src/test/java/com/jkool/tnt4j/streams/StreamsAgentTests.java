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

package com.jkool.tnt4j.streams;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Set;

import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;

import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamsAgentTests {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private StringWriter console;

	@Test
	public void testHelpArgument() throws Exception {
		interceptConsole();
		StreamsAgent.main("-h");
		System.out.flush();
		final String string = console.getBuffer().toString();
		final String expected = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "StreamsAgent.help")
				+ LINE_SEPARATOR;
		assertTrue("Console output does not contain expected string", string.contains(expected));
		Utils.close(console);
	}

	@Test
	public void testArgumentsFail() throws Exception {
		interceptConsole();
		final String argument = "-test";
		StreamsAgent.main(argument);
		System.out.flush();
		final String string = console.getBuffer().toString();
		String expected = StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
				"StreamsAgent.invalid.argument", argument);
		expected += LINE_SEPARATOR;
		expected += StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "StreamsAgent.help");
		expected += LINE_SEPARATOR;
		assertTrue("Console output does not contain expected string", string.contains(expected));
		Utils.close(console);
	}

	@Test
	public void testFileEmptyFail() throws Exception {
		interceptConsole();
		final String argument = "-f:";
		StreamsAgent.main(argument);
		System.out.flush();
		final String string = console.getBuffer().toString();
		String expected = StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
				"StreamsAgent.missing.cfg.file", argument);
		expected += LINE_SEPARATOR;
		expected += StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "StreamsAgent.help");
		expected += LINE_SEPARATOR;
		assertTrue("Console output does not contain expected string", string.contains(expected));
		Utils.close(console);
	}

	@Test
	public void testRunFromAPI() throws Exception {
		final String testStreamName = "TestStream";
		final File tempConfFile = File.createTempFile("testConfigutarion", ".xml");
		FileWriter fw = new FileWriter(tempConfFile);
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + LINE_SEPARATOR);
		sb.append("<tnt-data-source" + LINE_SEPARATOR
				+ "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + LINE_SEPARATOR
				+ "        xsi:noNamespaceSchemaLocation=\"../../../config/tnt-data-source.xsd\">" + LINE_SEPARATOR
				+ "   ");
		sb.append("<stream name=\"");
		sb.append(testStreamName);
		sb.append("\" class=\"com.jkool.tnt4j.streams.inputs.CharacterStream\">" + LINE_SEPARATOR
				+ "        <property name=\"HaltIfNoParser\" value=\"false\"/>" + LINE_SEPARATOR
				+ "        <property name=\"Port\" value=\"9595\"/>" + LINE_SEPARATOR + "    ");
		sb.append("</stream>" + LINE_SEPARATOR + "</tnt-data-source>");
		fw.write(sb.toString());
		fw.flush();
		Utils.close(fw);
		StreamsAgent.runFromAPI(tempConfFile.getAbsolutePath());
		Thread.sleep(500);
		tempConfFile.delete();
		final Set<Thread> threads = Thread.getAllStackTraces().keySet();
		for (Thread thread : threads) {
			if (thread.getName().contains(testStreamName)) {
				return;
			} else {
				continue;
			}
		}
		fail("No streams thread created");
	}

	private void interceptConsole() throws InterruptedException {
		console = new StringWriter();
		final WriterOutputStream writerOutputStream = new WriterOutputStream(console);
		final PrintStream out = new PrintStream(writerOutputStream);
		System.setOut(out);
		Thread.sleep(50);
	}
}