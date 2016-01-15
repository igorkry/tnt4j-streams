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

package com.jkool.tnt4j.streams.samples.custom;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.StreamThread;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Sample integration of TNT4J-Streams into an application.
 *
 * @version $Revision: 2 $
 */
public final class SampleIntegration {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(SampleIntegration.class);

	/**
	 * Configure streams and parsers, and run each stream in its own thread.
	 *
	 * @param cfgFileName
	 *            configuration file name
	 */
	public static void loadConfigAndRun(String cfgFileName) {
		try {
			StreamsConfig cfg = StringUtils.isEmpty(cfgFileName) ? new StreamsConfig() : new StreamsConfig(cfgFileName);
			Map<String, TNTInputStream> streamsMap = cfg.getStreams();
			if (streamsMap == null || streamsMap.isEmpty()) {
				throw new IllegalStateException("No Activity Streams found in configuration");
			}

			ThreadGroup streamThreads = new ThreadGroup("Streams");
			StreamThread ft;
			for (Map.Entry<String, TNTInputStream> streamEntry : streamsMap.entrySet()) {
				String streamName = streamEntry.getKey();
				TNTInputStream stream = streamEntry.getValue();
				ft = new StreamThread(streamThreads, stream, streamName);
				ft.start();
			}
		} catch (Throwable t) {
			LOGGER.log(OpLevel.ERROR, t.getMessage(), t);
		}
	}

	/**
	 * The following can be used if using the default configuration file with a
	 * single stream.
	 *
	 * @param cfgFileName
	 *            configuration file name
	 */
	public static void simpleConfigAndRun(String cfgFileName) {
		try {
			StreamsConfig cfg = new StreamsConfig();
			TNTInputStream stream = cfg.getStream("StreamName");
			StreamThread ft = new StreamThread(stream);
			ft.start();
		} catch (Throwable t) {
			LOGGER.log(OpLevel.ERROR, t.getMessage(), t);
		}
	}
}