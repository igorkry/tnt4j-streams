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

package com.jkool.tnt4j.streams.utils;

import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Base class for Streams threads.
 *
 * @version $Revision: 12 $
 *
 * @see Thread
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
	 * @see Thread#Thread()
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
	 * @see Thread#Thread(Runnable)
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
	 * @see Thread#Thread(Runnable, String)
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
	 * @see Thread#Thread(String)
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
	 * @see Thread#Thread(ThreadGroup, Runnable)
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
	 * @see Thread#Thread(ThreadGroup, Runnable, String)
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
	 * @see Thread#Thread(ThreadGroup, String)
	 */
	public StreamsThread(ThreadGroup threadGrp, String name) {
		super(threadGrp, name);
		setDefaultName();
	}

	/**
	 * <p>
	 * Sets default name for a Streams thread.
	 * <p>
	 * Prefixes current (default) thread name with thread's ID and strips off
	 * leading "com.jkool.tnt4j.streams." from thread name.
	 */
	protected void setDefaultName() {
		setName(String.format("%s:%s", getId(), getName().replaceFirst("com.jkool.tnt4j.streams.", ""))); // NON-NLS
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
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("StreamsThread.halt"));
		stopRunning = true;
		interrupt();
	}

	/**
	 * Causes the currently executing thread to sleep (temporarily cease
	 * execution) for the specified number of milliseconds. This method differs
	 * from {@link Thread#sleep(long)} (which it uses) in that it does not throw
	 * any exceptions. If the sleep is interrupted, then this method will just
	 * return.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 *
	 * @see Thread#sleep(long)
	 */
	public static void sleep(long millis) {
		long startTime = System.currentTimeMillis();
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("StreamsThread.sleep.interrupted",
					(System.currentTimeMillis() - startTime), millis));
		}
	}

	/**
	 * Causes the currently executing thread to sleep (temporarily cease
	 * execution) for the specified number of milliseconds. This method differs
	 * from {@link Thread#sleep(long)} (which it uses) in that it does not throw
	 * any exceptions, and if the sleep is interrupted other than to signal
	 * thread to terminate, this method will cause current thread to go
	 * "back to sleep" for the remainder of the time.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 *
	 * @see Thread#sleep(long)
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
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("StreamsThread.sleepFully.interrupted",
						interruptCount, sleepMillis, millis));
				if (remainMillis > 0L) {
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getStringFormatted("StreamsThread.sleepFully.remaining", remainMillis));
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
	 * @see Thread#join()
	 */
	public void waitFor(long millis) {
		long startTime = System.currentTimeMillis();
		try {
			join(millis);
		} catch (InterruptedException e) {
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("StreamsThread.wait.for",
				(System.currentTimeMillis() - startTime)));
	}
}
