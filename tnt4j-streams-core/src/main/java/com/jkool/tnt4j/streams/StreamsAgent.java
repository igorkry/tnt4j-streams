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

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.StreamThread;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Main class for jKool LLC TNT4J-Streams standalone application.
 *
 * @version $Revision: 1 $
 */
public final class StreamsAgent {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(StreamsAgent.class);
	private static String cfgFileName = null;

	private StreamsAgent() {
	}

	/**
	 * Main entry point for running as a standalone application.
	 *
	 * @param args
	 *            command-line arguments. Supported arguments:
	 *            <table summary="TNT4J-Streams agent command line arguments">
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-f:&lt;cfg_file_name&gt;</td>
	 *            <td>(optional) Load TNT4J Streams data source configuration
	 *            from &lt;cfg_file_name&gt;</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;-h | -?</td>
	 *            <td>(optional) Print usage</td>
	 *            </tr>
	 *            </table>
	 */
	public static void main(String... args) {
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "StreamsAgent.start.main"));
		boolean argsValid = processArgs(args);
		if (argsValid) {
			loadConfigAndRun(cfgFileName);
		}
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param cfgFileName
	 *            stream configuration file name
	 */
	public static void runFromAPI(String cfgFileName) {
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "StreamsAgent.start.api"));
		loadConfigAndRun(cfgFileName);
	}

	/**
	 * Configure streams and parsers, and run each stream in its own thread.
	 * 
	 * @param cfgFileName
	 *            stream configuration file name
	 */
	private static void loadConfigAndRun(String cfgFileName) {
		try {
			StreamsConfig cfg = StringUtils.isEmpty(cfgFileName) ? new StreamsConfig() : new StreamsConfig(cfgFileName);
			Map<String, TNTInputStream> streamsMap = cfg.getStreams();
			if (streamsMap == null || streamsMap.isEmpty()) {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"StreamsAgent.no.activity.streams"));
			}
			ThreadGroup streamThreads = new ThreadGroup(StreamsAgent.class.getName() + "Threads"); // NON-NLS
			StreamThread ft;
			for (Map.Entry<String, TNTInputStream> streamEntry : streamsMap.entrySet()) {
				String streamName = streamEntry.getKey();
				TNTInputStream stream = streamEntry.getValue();
				ft = new StreamThread(streamThreads, stream,
						String.format("%s:%s", stream.getClass().getSimpleName(), streamName)); // NON-NLS
				ft.start();
			}
		} catch (Throwable t) {
			LOGGER.log(OpLevel.ERROR, String.valueOf(t.getLocalizedMessage()), t);
		}
	}

	/**
	 * Process and interprets command-line arguments.
	 *
	 * @param args
	 *            command-line arguments.
	 * 
	 * @return {@code true} if command-line arguments where valid to interpret,
	 *         {@code false} - otherwise
	 */
	private static boolean processArgs(String... args) {
		for (String arg : args) {
			if (StringUtils.isEmpty(arg)) {
				continue;
			}
			if (arg.startsWith("-f:")) { // NON-NLS
				cfgFileName = arg.substring(3);
				if (StringUtils.isEmpty(cfgFileName)) {
					System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
							"StreamsAgent.missing.cfg"));
					printUsage();
					return false;
				}
			} else if ("-h".equals(arg) || "-?".equals(arg)) { // NON-NLS
				printUsage();
				return false;
			} else {
				System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
						"StreamsAgent.invalid.argument", arg));
				printUsage();
				return false;
			}
		}

		return true;
	}

	/**
	 * Prints short standalone application usage manual.
	 */
	private static void printUsage() {
		System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "StreamsAgent.help"));
	}
}
