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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Base class for buffered input activity stream. RAW activity data retrieved
 * from input source is placed into blocking queue to be asynchronously
 * processed by consumer thread(s).
 * </p>
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 1 $
 *
 * @see java.util.concurrent.ArrayBlockingQueue
 * @see java.util.concurrent.BlockingQueue#offer(Object, long, TimeUnit)
 */
public abstract class AbstractBufferedStream<T> extends TNTInputStream<T> {
	private static final int INPUT_BUFFER_SIZE = 1024 * 10;
	private static final int INPUT_BUFFER_OFFER_TIMEOUT = 15; // NOTE: sec.

	/**
	 * RAW activity data items buffer queue. Items in this queue are processed
	 * asynchronously by consumer thread(s).
	 */
	protected BlockingQueue<T> inputBuffer;

	/**
	 * Constructs a new AbstractBufferedStream.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected AbstractBufferedStream(EventSink logger) {
		super(logger);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Throwable {
		super.initialize();

		inputBuffer = new ArrayBlockingQueue<T>(INPUT_BUFFER_SIZE, true);
		// inputBuffer = new SynchronousQueue<T>(true);
	}

	/**
	 * Get the next activity data item to be processed. Method blocks and waits
	 * for activity input data available in input buffer. Input buffer is filled
	 * by {@code InputProcessor} thread.
	 *
	 * @return next activity data item, or {@code null} if there is no next item
	 *
	 * @throws Throwable
	 *             if any errors occurred getting next item
	 */
	public T getNextItem() throws Throwable {
		if (inputBuffer == null) {
			throw new IllegalStateException(
					StreamsResources.getString("AbstractBufferedStream.changes.buffer.uninitialized"));
		}

		if (inputBuffer.isEmpty() && isInputEnded()) {
			return null;
		}

		T activityInput = inputBuffer.take();

		return activityInput;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		inputBuffer.clear();
		super.cleanup();
	}

	/**
	 * Adds input data to buffer for asynchronous processing. Input data may not
	 * be added if buffer size limit and offer timeout is exceeded.
	 *
	 * @param inputData
	 *            input data to add to buffer
	 *
	 * @see java.util.concurrent.BlockingQueue#offer(Object, long, TimeUnit)
	 */
	protected void addInputToBuffer(T inputData) {
		if (inputData != null) {
			try {
				boolean added = inputBuffer.offer(inputData, INPUT_BUFFER_OFFER_TIMEOUT, TimeUnit.SECONDS);

				if (!added) {
					logger.log(OpLevel.WARNING, StreamsResources
							.getStringFormatted("AbstractBufferedStream.changes.buffer.limit", inputData));
				}
			} catch (InterruptedException exc) {
				logger.log(OpLevel.WARNING, StreamsResources.getString("AbstractBufferedStream.offer.interrupted"),
						exc);
				// halt();
			}
		}
	}

	/**
	 * Checks if stream data input is ended.
	 *
	 * @return {@code true} if stream input ended, {@code false} - otherwise
	 *
	 * @see InputProcessor#isInputEnded()
	 */
	protected abstract boolean isInputEnded();

	/**
	 * Base class containing common features for stream input processor thread.
	 */
	protected abstract class InputProcessor extends Thread {

		/**
		 * Input processor attribute identifying that input processing is
		 * interrupted.
		 */
		private boolean interrupted = false;

		private boolean inputEnd = false;

		/**
		 * Constructs a new InputProcessor.
		 *
		 * @param name
		 *            the name of the new thread
		 */
		protected InputProcessor(String name) {
			super(name);

			setDaemon(true);
		}

		/**
		 * Checks if input processor should stop running.
		 *
		 * @return {@code true} input processor is interrupted or parent thread
		 *         is halted
		 */
		protected boolean isStopping() {
			return interrupted || isHalted();
		}

		/**
		 * Stops this thread.
		 */
		void halt() {
			interrupted = true;
			interrupt();
			// join();
		}

		/**
		 * Closes opened data input resources and marks stream data input as
		 * ended.
		 * 
		 * @throws Exception
		 *             if fails to close opened resources due to internal error
		 */
		void close() throws Exception {
			markInputEnd();
		}

		/**
		 * Shuts down stream input processor: interrupts thread and closes
		 * opened data input resources.
		 */
		protected void shutdown() {
			halt();
			try {
				close();
			} catch (Exception exc) {
				logger.log(OpLevel.WARNING, StreamsResources.getString("AbstractBufferedStream.input.close.error"),
						exc);
			}
		}

		/**
		 * Changes stream data input ended flag value to {@code true}
		 */
		protected void markInputEnd() {
			inputEnd = true;
		}

		/**
		 * Checks if stream data input is ended.
		 *
		 * @return {@code true} if stream input ended, {@code false} - otherwise
		 */
		protected boolean isInputEnded() {
			return inputEnd;
		}
	}
}
