/*
 * Copyright 2014-2017 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.configure.zookeeper;

/**
 * Lists constant values and predefined property names used by TNT4-Streams ZooKeeper (ZK) configuration.
 *
 * @version $Revision: 1 $
 */
public interface ZKConfigConstants {
	/**
	 * Constant for ZooKeeper node path delimiter.
	 */
	public static final String PATH_DELIM = "/"; // NON-NLS

	/**
	 * Constant for default ZooKeeper connection host - {@value}.
	 */
	static final String DEFAULT_CONN_HOST = "localhost"; // NON-NLS
	/**
	 * Constant for default ZooKeeper connection timeout - {@value}ms.
	 */
	static final int DEFAULT_CONN_TIMEOUT = 5000;

	/**
	 * Constant for name of TNT4J-Streams ZooKeeper configuration property {@value}.
	 */
	static final String PROP_ZK_CONN = "zk.conn"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams ZooKeeper configuration property {@value}.
	 */
	static final String PROP_ZK_CONN_TIMEOUT = "zk.conn.timeout"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams ZooKeeper configuration property {@value}.
	 */
	static final String PROP_ZK_STREAMS_PATH = "zk.streams.path"; // NON-NLS

	/**
	 * Constant for name of TNT4J-Streams ZooKeeper configuration property {@value}.
	 */
	static final String PROP_CONF_PATH_LOGGER = "logger.configuration.path"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams ZooKeeper configuration property {@value}.
	 */
	static final String PROP_CONF_PATH_TNT4J = "tnt4j.configuration.path"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams ZooKeeper configuration property {@value}.
	 */
	static final String PROP_CONF_PATH_TNT4J_KAFKA = "tnt4j-kafka.configuration.path"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams ZooKeeper configuration property {@value}.
	 */
	static final String PROP_CONF_PATH_STREAMS = "stream.configuration.path"; // NON-NLS
}
