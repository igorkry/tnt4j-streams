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

package com.jkool.tnt4j.streams.inputs;

import com.jkool.tnt4j.streams.utils.StreamsThread;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Base class for threads running an TNTInputStream.
 *
 * @version $Revision: 3 $
 * @see TNTInputStream
 */
public class StreamThread extends StreamsThread {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(StreamThread.class);

	/**
	 * TNTInputStream being executed by this thread.
	 */
	protected final TNTInputStream target;

	/**
	 * Creates thread to run specified TNTInputStream.
	 *
	 * @param target
	 *            the TNTInputStream to run
	 *
	 * @see java.lang.Thread#Thread(Runnable)
	 */
	public StreamThread(TNTInputStream target) {
		super(target);
		this.target = target;
		target.setOwnerThread(this);
	}

	/**
	 * Creates thread to run specified TNTInputStream.
	 *
	 * @param target
	 *            the TNTInputStream to run
	 * @param name
	 *            the name for thread
	 *
	 * @see java.lang.Thread#Thread(Runnable, String)
	 */
	public StreamThread(TNTInputStream target, String name) {
		super(target, name);
		this.target = target;
		target.setOwnerThread(this);
	}

	/**
	 * Creates thread to run specified TNTInputStream.
	 *
	 * @param group
	 *            the thread group new thread is to belong to
	 * @param target
	 *            the TNTInputStream to run
	 * @param name
	 *            the name for thread
	 *
	 * @see java.lang.Thread#Thread(ThreadGroup, Runnable, String)
	 */
	public StreamThread(ThreadGroup group, TNTInputStream target, String name) {
		super(group, target, name);
		this.target = target;
		target.setOwnerThread(this);
	}

	/**
	 * Creates thread to run specified TNTInputStream.
	 *
	 * @param group
	 *            the thread group new thread is to belong to
	 * @param target
	 *            the TNTInputStream to run
	 *
	 * @see java.lang.Thread#Thread(ThreadGroup, Runnable)
	 */
	public StreamThread(ThreadGroup group, TNTInputStream target) {
		super(group, target);
		this.target = target;
		target.setOwnerThread(this);
	}

	/**
	 * Gets the TNTInputStream being run by this thread.
	 *
	 * @return TNTInputStream being run
	 */
	public TNTInputStream getTarget() {
		return target;
	}
}
