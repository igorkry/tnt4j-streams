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

package com.jkool.tnt4j.streams.utils;

import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Base class for Streams threads.
 *
 * @version $Revision: 12 $
 * @see java.lang.Thread
 */
public class StreamsThread extends Thread {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(StreamsThread.class);

	/**
	 * Flag indicating that tread should stop running.
	 */
	protected boolean stopRunning = false;

	/**
	 * Creates new Streams thread.
	 *
	 * @see java.lang.Thread#Thread()
	 */
	public StreamsThread() {
		setDefaultName();
	}

	/**
	 * Creates new Streams thread.
	 *
	 * @param target
	 *            object whose {@code run} method is called
	 *
	 * @see java.lang.Thread#Thread(Runnable)
	 */
	public StreamsThread(Runnable target) {
		super(target);
		setDefaultName();
	}

	/**
	 * Creates new Streams thread.
	 *
	 * @param target
	 *            object whose {@code run} method is called
	 * @param name
	 *            name of thread
	 *
	 * @see java.lang.Thread#Thread(Runnable, String)
	 */
	public StreamsThread(Runnable target, String name) {
		super(target, name);
		setDefaultName();
	}

	/**
	 * Creates new Streams thread.
	 *
	 * @param name
	 *            name of thread
	 *
	 * @see java.lang.Thread#Thread(String)
	 */
	public StreamsThread(String name) {
		super(name);
		setDefaultName();
	}

	/**
	 * Creates new Streams thread.
	 *
	 * @param threadGrp
	 *            thread group to add new thread to
	 * @param target
	 *            object whose {@code run} method is called
	 *
	 * @see java.lang.Thread#Thread(ThreadGroup, Runnable)
	 */
	public StreamsThread(ThreadGroup threadGrp, Runnable target) {
		super(threadGrp, target);
		setDefaultName();
	}

	/**
	 * Creates new Streams thread.
	 *
	 * @param threadGrp
	 *            thread group to add new thread to
	 * @param target
	 *            object whose {@code run} method is called
	 * @param name
	 *            name of thread
	 *
	 * @see java.lang.Thread#Thread(ThreadGroup, Runnable, String)
	 */
	public StreamsThread(ThreadGroup threadGrp, Runnable target, String name) {
		super(threadGrp, target, name);
		setDefaultName();
	}

	/**
	 * Creates new Streams thread.
	 *
	 * @param threadGrp
	 *            thread group to add new thread to
	 * @param name
	 *            name of thread
	 *
	 * @see java.lang.Thread#Thread(ThreadGroup, String)
	 */
	public StreamsThread(ThreadGroup threadGrp, String name) {
		super(threadGrp, name);
		setDefaultName();
	}

	/**
	 * <p>
	 * Sets default name for a Streams thread.
	 * </p>
	 * <p>
	 * Prefixes current (default) thread name with thread's ID and strips off
	 * leading "com.jkool.tnt4j.streams." from thread name.
	 * </p>
	 */
	protected void setDefaultName() {
		setName(getId() + ":" + getName().replaceFirst("com.jkool.tnt4j.streams.", ""));
	}

	/**
	 * Indicates whether the thread was signaled to stop running.
	 *
	 * @return {@code true} if thread signaled to stop, {@code false} if not
	 */
	public boolean isStopRunning() {
		return stopRunning;
	}

	/**
	 * Stops this thread.
	 */
	public void halt() {
		LOGGER.log(OpLevel.DEBUG, "Signaled to terminate");
		stopRunning = true;
		interrupt();
	}

	/**
	 * Causes the currently executing thread to sleep (temporarily cease
	 * execution) for the specified number of milliseconds. This method differs
	 * from {@link java.lang.Thread#sleep(long)} (which it uses) in that it does
	 * not throw any exceptions. If the sleep is interrupted, then this method
	 * will just return.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 *
	 * @see java.lang.Thread#sleep(long)
	 */
	public static void sleep(long millis) {
		long startTime = System.currentTimeMillis();
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			LOGGER.log(OpLevel.DEBUG, "Sleep interrupted after {0} msec. (initial={1})",
					(System.currentTimeMillis() - startTime), millis);
		}
	}

	/**
	 * Causes the currently executing thread to sleep (temporarily cease
	 * execution) for the specified number of milliseconds. This method differs
	 * from {@link java.lang.Thread#sleep(long)} (which it uses) in that it does
	 * not throw any exceptions, and if the sleep is interrupted other than to
	 * signal thread to terminate, this method will cause current thread to go
	 * "back to sleep" for the remainder of the time.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 *
	 * @see java.lang.Thread#sleep(long)
	 */
	public void sleepFully(long millis) {
		long startTime = 0L;
		long remainMillis = millis;
		int interruptCount = 0;
		while (remainMillis > 0L) {
			try {
				startTime = System.currentTimeMillis();
				Thread.sleep(remainMillis);
				remainMillis = 0L; // not interrupted, stop sleeping
			} catch (InterruptedException e) {
				// if sleep interrupted because thread was signaled to
				// terminate, then return
				if (stopRunning) {
					return;
				}
				// subtract from sleep time the amount of time we were actually
				// asleep
				long sleepMillis = System.currentTimeMillis() - startTime;
				remainMillis -= sleepMillis;
				interruptCount++;
				LOGGER.log(OpLevel.DEBUG, "Sleep interrupted (count={0}), after {1} msec. (initial={2})",
						interruptCount, sleepMillis, millis);
				if (remainMillis > 0L) {
					LOGGER.log(OpLevel.DEBUG, "   Going back to sleep for {0} msec.", remainMillis);
				}
			}
		}
	}

	/**
	 * Waits at most millis milliseconds for this thread to die. A timeout of 0
	 * means to wait forever. Differs from {@code java.lang.Thread#join()},
	 * which it wraps, in that it does not throw an exception when interrupted.
	 *
	 * @param millis
	 *            time to wait in milliseconds
	 *
	 * @see java.lang.Thread#join()
	 */
	public void waitFor(long millis) {
		long startTime = System.currentTimeMillis();
		try {
			join(millis);
		} catch (InterruptedException e) {
		}
		LOGGER.log(OpLevel.DEBUG, "Completed waiting for thread to die in {0} msec.",
				(System.currentTimeMillis() - startTime));
	}
}
