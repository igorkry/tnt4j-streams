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
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 1 $
 *
 * @see ArrayBlockingQueue
 * @see BlockingQueue#offer(Object, long, TimeUnit)
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
	protected void initialize() throws Exception {
		super.initialize();

		inputBuffer = new ArrayBlockingQueue<T>(INPUT_BUFFER_SIZE, true);
		// inputBuffer = new SynchronousQueue<T>(true);
	}

	/**
	 * Get the next activity data item to be processed. Method blocks and waits
	 * for activity input data available in input buffer. Input buffer is filled
	 * by {@link InputProcessor} thread.
	 *
	 * @return next activity data item, or {@code null} if there is no next item
	 *
	 * @throws Exception
	 *             if any errors occurred getting next item
	 */
	public T getNextItem() throws Exception {
		if (inputBuffer == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"AbstractBufferedStream.changes.buffer.uninitialized"));
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
	 * @return {@code true} if input data is added to buffer, {@code false} -
	 *         otherwise
	 *
	 * @see BlockingQueue#offer(Object, long, TimeUnit)
	 */
	protected boolean addInputToBuffer(T inputData) {
		if (inputData != null) {
			try {
				boolean added = inputBuffer.offer(inputData, INPUT_BUFFER_OFFER_TIMEOUT, TimeUnit.SECONDS);

				if (!added) {
					logger.log(OpLevel.WARNING,
							StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
									"AbstractBufferedStream.changes.buffer.limit", inputData));
				}

				return added;
			} catch (InterruptedException exc) {
				logger.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"AbstractBufferedStream.offer.interrupted"), exc);
				// halt();
			}
		}

		return false;
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
				logger.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"AbstractBufferedStream.input.close.error"), exc);
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
