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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.inputs.*;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Main class for jKool LLC TNT4J-Streams standalone application.
 *
 * @version $Revision: 1 $
 */
public final class StreamsAgent {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(StreamsAgent.class);

	private static final String PARAM_STREAM_CFG = "-f:"; // NON-NLS
	private static final String PARAM_PARSER_CFG = "-p:"; // NON-NLS
	private static final String PARAM_SKIP_UNPARSED = "-s"; // NON-NLS
	private static final String PARAM_HELP1 = "-h"; // NON-NLS
	private static final String PARAM_HELP2 = "-?"; // NON-NLS

	private static String cfgFileName = null;
	private static boolean noStreamConfig = false;
	private static boolean haltOnUnparsed = true;

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
	 *            <td>&nbsp;-p:&lt;cfg_file_name&gt;</td>
	 *            <td>(optional) Load parsers configuration from
	 *            &lt;cfg_file_name&gt;</td>
	 *            </tr>
	 *            <tr>
	 *            <td>&nbsp;&nbsp;</td>
	 *            <td>&nbsp;&nbsp;&nbsp;-s</td>
	 *            <td>(optional) Skip unparsed activity data entries and
	 *            continue streaming</td>
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
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsAgent.start.main"));
		boolean argsValid = processArgs(args);
		if (argsValid) {
			loadConfigAndRun(cfgFileName);
			// DefaultTNTStreamListener dsl = new
			// DefaultTNTStreamListener(LOGGER);
			// loadConfigAndRun(cfgFileName, dsl, dsl);
		}
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param cfgFileName
	 *            stream configuration file name
	 */
	public static void runFromAPI(String cfgFileName) {
		runFromAPI(cfgFileName, null, null);
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param cfgFileName
	 *            stream configuration file name
	 * @param streamListener
	 *            input stream listener
	 * @param streamTasksListener
	 *            stream tasks listener
	 */
	public static void runFromAPI(String cfgFileName, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsAgent.start.api"));
		loadConfigAndRun(cfgFileName, streamListener, streamTasksListener);
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param cfgFile
	 *            stream configuration file
	 */
	public static void runFromAPI(File cfgFile) {
		runFromAPI(cfgFile, null, null);
	}

	/**
	 * Main entry point for running as a API integration.
	 *
	 * @param cfgFile
	 *            stream configuration file
	 * @param streamListener
	 *            input stream listener
	 * @param streamTasksListener
	 *            stream tasks listener
	 */
	public static void runFromAPI(File cfgFile, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		LOGGER.log(OpLevel.INFO,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsAgent.start.api"));
		loadConfigAndRun(cfgFile, streamListener, streamTasksListener);
	}

	private static void loadConfigAndRun(String cfgFileName) {
		loadConfigAndRun(cfgFileName, null, null);
	}

	private static void loadConfigAndRun(String cfgFileName, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		try {
			initAndRun(
					StringUtils.isEmpty(cfgFileName) ? new StreamsConfigLoader() : new StreamsConfigLoader(cfgFileName),
					streamListener, streamTasksListener);
		} catch (Exception e) {
			LOGGER.log(OpLevel.ERROR, String.valueOf(e.getLocalizedMessage()), e);
		}
	}

	private static void loadConfigAndRun(File cfgFile) {
		loadConfigAndRun(cfgFile, null, null);
	}

	private static void loadConfigAndRun(File cfgFile, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) {
		try {
			initAndRun(cfgFile == null ? new StreamsConfigLoader() : new StreamsConfigLoader(cfgFile), streamListener,
					streamTasksListener);
		} catch (Exception e) {
			LOGGER.log(OpLevel.ERROR, String.valueOf(e.getLocalizedMessage()), e);
		}
	}

	/**
	 * Configure streams and parsers, and run each stream in its own thread.
	 *
	 * @param cfg
	 *            stream configuration
	 * @param streamListener
	 *            input stream listener
	 * @param streamTasksListener
	 *            stream tasks listener
	 */
	private static void initAndRun(StreamsConfigLoader cfg, InputStreamListener streamListener,
			StreamTasksListener streamTasksListener) throws Exception {
		if (cfg == null) {
			return;
		}

		Collection<TNTInputStream<?, ?>> streams;
		if (noStreamConfig) {
			streams = initPiping(cfg);
		} else {
			streams = cfg.getStreams();
		}
		if (CollectionUtils.isEmpty(streams)) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"StreamsAgent.no.activity.streams"));
		}

		ThreadGroup streamThreads = new ThreadGroup(StreamsAgent.class.getName() + "Threads"); // NON-NLS
		StreamThread ft;
		for (TNTInputStream<?, ?> stream : streams) {
			if (streamListener != null) {
				stream.addStreamListener(streamListener);
			}

			if (streamTasksListener != null) {
				stream.addStreamTasksListener(streamTasksListener);
			}

			ft = new StreamThread(streamThreads, stream,
					String.format("%s:%s", stream.getClass().getSimpleName(), stream.getName())); // NON-NLS
			ft.start();
		}
	}

	private static Collection<TNTInputStream<?, ?>> initPiping(StreamsConfigLoader cfg) throws Exception {
		Collection<TNTInputStream<?, ?>> streams = new ArrayList<TNTInputStream<?, ?>>(1);

		Map<String, String> props = new HashMap<String, String>(1);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(haltOnUnparsed));

		PipedStream pipeStream = new PipedStream();
		pipeStream.setName("DefaultSystemPipeStream"); // NON-NLS
		pipeStream.setProperties(props.entrySet());

		Collection<ActivityParser> parsers = cfg.getParsers();
		if (CollectionUtils.isEmpty(parsers)) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"StreamsAgent.no.piped.activity.parsers"));
		}
		for (ActivityParser parser : parsers) {
			if (parser != null) {
				pipeStream.addParser(parser);
			}
		}

		streams.add(pipeStream);

		return streams;
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
			if (arg.startsWith(PARAM_STREAM_CFG) || arg.startsWith(PARAM_PARSER_CFG)) {
				if (StringUtils.isNotEmpty(cfgFileName)) {
					System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.invalid.args"));
					printUsage();
					return false;
				}

				cfgFileName = arg.substring(3);
				if (StringUtils.isEmpty(cfgFileName)) {
					System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"StreamsAgent.missing.cfg.file", arg.substring(0, 3)));
					printUsage();
					return false;
				}

				noStreamConfig = arg.startsWith(PARAM_PARSER_CFG);
			} else if (PARAM_SKIP_UNPARSED.equals(arg)) {
				haltOnUnparsed = false;
			} else if (PARAM_HELP1.equals(arg) || PARAM_HELP2.equals(arg)) {
				printUsage();
				return false;
			} else {
				System.out.println(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
		System.out.println(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsAgent.help"));
	}
}
