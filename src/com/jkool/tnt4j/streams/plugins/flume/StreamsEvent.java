/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.plugins.flume;

import org.apache.flume.Event;
import org.apache.flume.event.JSONEvent;

/**
 * <p>
 * Extends Apache Flume {@code JSONEvent} to add additional attributes, required
 * by TNT4J-Streams.
 * </p>
 *
 * @version $Revision: 1 $
 */
public class StreamsEvent extends JSONEvent {

	private String sinkName;
	private String chanelName;

	/**
	 * Constructs an StreamsEvent.
	 */
	public StreamsEvent() {
		super();
	}

	/**
	 * Constructs an StreamEvent based on Apache Flume output event data.
	 * Additionally sets event channel and sink names.
	 *
	 * @param event
	 *            event to wrap
	 * @param sinkName
	 *            event sink name
	 * @param chanelName
	 *            event channel name
	 */
	public StreamsEvent(Event event, String sinkName, String chanelName) {
		this(event);
		this.sinkName = sinkName;
		this.chanelName = chanelName;
	}

	/**
	 * Constructs an StreamEvent based on Apache Flume output event data.
	 *
	 * @param event
	 *            event to wrap
	 */
	public StreamsEvent(Event event) {
		this();
		this.setBody(event.getBody());
		this.setHeaders(event.getHeaders());
	}

	/**
	 * Returns event sink name.
	 *
	 * @return sink name
	 */
	public String getSinkName() {
		return sinkName;
	}

	/**
	 * Sets event sink name.
	 *
	 * @param sinkName
	 *            sink name
	 */
	public void setSinkName(String sinkName) {
		this.sinkName = sinkName;
	}

	/**
	 * Returns event channel name.
	 *
	 * @return channel name
	 */
	public String getChanelName() {
		return chanelName;
	}

	/**
	 * Sets event channel name.
	 *
	 * @param chanelName
	 *            channel name
	 */
	public void setChanelName(String chanelName) {
		this.chanelName = chanelName;
	}

}
