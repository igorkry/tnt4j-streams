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

package com.jkoolcloud.tnt4j.streams.configure.jaxb;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for StreamProperties.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="StreamProperties">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="DateTime"/>
 *     &lt;enumeration value="FileName"/>
 *     &lt;enumeration value="Host"/>
 *     &lt;enumeration value="Port"/>
 *     &lt;enumeration value="QueueManager"/>
 *     &lt;enumeration value="Queue"/>
 *     &lt;enumeration value="Topic"/>
 *     &lt;enumeration value="Subscription"/>
 *     &lt;enumeration value="TopicString"/>
 *     &lt;enumeration value="Channel"/>
 *     &lt;enumeration value="StripHeaders"/>
 *     &lt;enumeration value="StartFromLatest"/>
 *     &lt;enumeration value="FileReadDelay"/>
 *     &lt;enumeration value="HaltIfNoParser"/>
 *     &lt;enumeration value="UseExecutors"/>
 *     &lt;enumeration value="ExecutorThreadsQuantity"/>
 *     &lt;enumeration value="ExecutorRejectedTaskOfferTimeout"/>
 *     &lt;enumeration value="ExecutorsTerminationTimeout"/>
 *     &lt;enumeration value="ExecutorsBoundedModel"/>
 *     &lt;enumeration value="Keystore"/>
 *     &lt;enumeration value="KeystorePass"/>
 *     &lt;enumeration value="KeyPass"/>
 *     &lt;enumeration value="JNDIFactory"/>
 *     &lt;enumeration value="JMSConnFactory"/>
 *     &lt;enumeration value="ServerURI"/>
 *     &lt;enumeration value="UserName"/>
 *     &lt;enumeration value="Password"/>
 *     &lt;enumeration value="UseSSL"/>
 *     &lt;enumeration value="ReadLines"/>
 *     &lt;enumeration value="RestartOnInputClose"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "StreamProperties")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-02-22T04:46:33+02:00", comments = "JAXB RI v2.2.4-2")
public enum StreamProperties {

	/**
	 * 
	 * Property value represents an initial, base, or default date, time, or
	 * date/time.
	 * 
	 * 
	 */
	@XmlEnumValue("DateTime") DATE_TIME("DateTime"),

	/**
	 * 
	 * Property value represents a file name.
	 * 
	 * 
	 */
	@XmlEnumValue("FileName") FILE_NAME("FileName"),

	/**
	 * 
	 * Property value represents a connection host name.
	 * 
	 * 
	 */
	@XmlEnumValue("Host") HOST("Host"),

	/**
	 * 
	 * Property value is a connection port number, interpreted based on the
	 * context in which it is used.
	 * 
	 * 
	 */
	@XmlEnumValue("Port") PORT("Port"),

	/**
	 * 
	 * Property value is a queue manager name.
	 * 
	 * 
	 */
	@XmlEnumValue("QueueManager") QUEUE_MANAGER("QueueManager"),

	/**
	 * 
	 * Property value is a queue name.
	 * 
	 * 
	 */
	@XmlEnumValue("Queue") QUEUE("Queue"),

	/**
	 * 
	 * Property value is a topic name.
	 * 
	 * 
	 */
	@XmlEnumValue("Topic") TOPIC("Topic"),

	/**
	 * 
	 * Property value is a subscription name.
	 * 
	 * 
	 */
	@XmlEnumValue("Subscription") SUBSCRIPTION("Subscription"),

	/**
	 * 
	 * Property value is a topic string to subscribe to.
	 * 
	 * 
	 */
	@XmlEnumValue("TopicString") TOPIC_STRING("TopicString"),

	/**
	 * 
	 * Property value is a channel name.
	 * 
	 * 
	 */
	@XmlEnumValue("Channel") CHANNEL("Channel"),

	/**
	 * 
	 * Property identifies whether stream should strip RAW activity data (i.e.
	 * WMQ message) headers.
	 * 
	 * 
	 */
	@XmlEnumValue("StripHeaders") STRIP_HEADERS("StripHeaders"),

	/**
	 * 
	 * Property identifies that streaming should be performed from latest log
	 * entry. If 'false' - then latest log file is streamed from beginning.
	 * 
	 * 
	 */
	@XmlEnumValue("StartFromLatest") START_FROM_LATEST("StartFromLatest"),

	/**
	 * 
	 * Property identifies delay is seconds between file reading iterations.
	 * 
	 * 
	 */
	@XmlEnumValue("FileReadDelay") FILE_READ_DELAY("FileReadDelay"),

	/**
	 * 
	 * Property identifies whether stream should halt if none of the parsers can
	 * parse activity RAW data. If set to 'false' - puts log entry and
	 * continues.
	 * 
	 * 
	 */
	@XmlEnumValue("HaltIfNoParser") HALT_IF_NO_PARSER("HaltIfNoParser"),

	/**
	 * 
	 * Property identifies identifies whether stream should use executor service
	 * to process activities data items asynchronously or not.
	 * 
	 * 
	 */
	@XmlEnumValue("UseExecutors") USE_EXECUTORS("UseExecutors"),

	/**
	 * 
	 * Property defines executor service thread pool size.
	 * 
	 * 
	 */
	@XmlEnumValue("ExecutorThreadsQuantity") EXECUTOR_THREADS_QUANTITY("ExecutorThreadsQuantity"),

	/**
	 * 
	 * Property defines time to wait (in seconds) for a executor service to
	 * terminate.
	 * 
	 * 
	 */
	@XmlEnumValue("ExecutorRejectedTaskOfferTimeout") EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT("ExecutorRejectedTaskOfferTimeout"),

	/**
	 * 
	 * Property defines time to wait (in seconds) for a task to be inserted into
	 * bounded queue if max. queue size is reached.
	 * 
	 * 
	 */
	@XmlEnumValue("ExecutorsTerminationTimeout") EXECUTORS_TERMINATION_TIMEOUT("ExecutorsTerminationTimeout"),

	/**
	 * 
	 * Property identifies whether executor service should use bounded tasks
	 * queue model.
	 * 
	 * 
	 */
	@XmlEnumValue("ExecutorsBoundedModel") EXECUTORS_BOUNDED_MODEL("ExecutorsBoundedModel"),

	/**
	 * 
	 * Property defines keystore path.
	 * 
	 * 
	 */
	@XmlEnumValue("Keystore") KEYSTORE("Keystore"),

	/**
	 * 
	 * Property defines keystore password.
	 * 
	 * 
	 */
	@XmlEnumValue("KeystorePass") KEYSTORE_PASS("KeystorePass"),

	/**
	 * 
	 * Property defines key password.
	 * 
	 * 
	 */
	@XmlEnumValue("KeyPass") KEY_PASS("KeyPass"),

	/**
	 * 
	 * Property defines JNDI context factory class name.
	 * 
	 * 
	 */
	@XmlEnumValue("JNDIFactory") JNDI_FACTORY("JNDIFactory"),

	/**
	 * 
	 * Property defines JMS connection factory class name.
	 * 
	 * 
	 */
	@XmlEnumValue("JMSConnFactory") JMS_CONN_FACTORY("JMSConnFactory"),

	/**
	 * 
	 * Property defines a connection server URI.
	 * 
	 * 
	 */
	@XmlEnumValue("ServerURI") SERVER_URI("ServerURI"),

	/**
	 * 
	 * Property defines a user/login name.
	 * 
	 * 
	 */
	@XmlEnumValue("UserName") USER_NAME("UserName"),

	/**
	 * 
	 * Property defines a user/login password.
	 * 
	 * 
	 */
	@XmlEnumValue("Password") PASSWORD("Password"),

	/**
	 * 
	 * Property identifies whether connection should use SSL.
	 * 
	 * 
	 */
	@XmlEnumValue("UseSSL") USE_SSL("UseSSL"),

	/**
	 * 
	 * Property indicates that complete activity RAW data (i.e. JSON) package is
	 * single line.
	 * 
	 * 
	 */
	@XmlEnumValue("ReadLines") READ_LINES("ReadLines"),

	/**
	 * 
	 * Property indicates to restart stream if input socked gets closed.
	 * 
	 * 
	 */
	@XmlEnumValue("RestartOnInputClose") RESTART_ON_INPUT_CLOSE("RestartOnInputClose");
	private final String value;

	StreamProperties(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static StreamProperties fromValue(String v) {
		for (StreamProperties c : StreamProperties.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
