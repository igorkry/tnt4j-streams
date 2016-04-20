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

package com.jkool.tnt4j.streams.configure;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.parsers.ActivityParser;

/**
 * This class contains streams and parsers objects loaded from configuration.
 *
 * @version $Revision: 1 $
 */
public class StreamsConfigData {

	private Map<String, ActivityParser> parsers = null;
	private Map<String, TNTInputStream> streams = null;

	// public StreamsConfigData() {
	// parsers = new HashMap<String, ActivityParser>();
	// streams = new HashMap<String, TNTInputStream>();
	// }

	/**
	 * Returns the collection of streams found in the configuration.
	 *
	 * @return collection of configuration defined streams or {@code null} if
	 *         there is no streams defined
	 */
	public Collection<TNTInputStream> getStreams() {
		return streams == null ? null : streams.values();
	}

	/**
	 * Returns the collection of parsers found in the configuration.
	 *
	 * @return collection of configuration defined parsers or {@code null} if
	 *         there is no parsers defined
	 */
	public Collection<ActivityParser> getParsers() {
		return parsers == null ? null : parsers.values();
	}

	/**
	 * Returns the stream with the specified name.
	 *
	 * @param streamName
	 *            name of stream, as specified in configuration file
	 * @return stream with specified name, or {@code null} if no such stream
	 */
	public TNTInputStream getStream(String streamName) {
		return streams == null ? null : streams.get(streamName);
	}

	/**
	 * Returns the parser with the specified name.
	 *
	 * @param parserName
	 *            name of parser, as specified in configuration file
	 * @return parser with specified name, or {@code null} if no such parser
	 */
	public ActivityParser getParser(String parserName) {
		return parsers == null ? null : parsers.get(parserName);
	}

	/**
	 * Checks if configuration has any streams defined.
	 *
	 * @return {@code true} if streams collection contains at least one stream
	 *         object, {@code false} - otherwise.
	 */
	public boolean isStreamsAvailable() {
		return MapUtils.isNotEmpty(streams);
	}

	/**
	 * Checks if configuration has any parsers defined.
	 *
	 * @return {@code true} if parsers collection contains at least one parser
	 *         object, {@code false} - otherwise.
	 */
	public boolean isParsersAvailable() {
		return MapUtils.isNotEmpty(parsers);
	}

	/**
	 * Adds stream object to streams collection.
	 *
	 * @param stream
	 *            stream object to add
	 */
	public void addStream(TNTInputStream stream) {
		if (streams == null) {
			streams = new HashMap<String, TNTInputStream>();
		}

		streams.put(stream.getName(), stream);
	}

	/**
	 * Adds activity parser object to parsers collection.
	 *
	 * @param parser
	 *            activity parser to add
	 */
	public void addParser(ActivityParser parser) {
		if (parsers == null) {
			parsers = new HashMap<String, ActivityParser>();
		}

		parsers.put(parser.getName(), parser);
	}
}
