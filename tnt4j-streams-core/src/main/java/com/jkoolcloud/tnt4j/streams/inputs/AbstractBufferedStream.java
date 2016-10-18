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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsThread;

/**
 * Base class for buffered input activity stream. RAW activity data retrieved from input source is placed into blocking
 * queue to be asynchronously processed by consumer thread(s).
 * <p>
 * This activity stream supports the following properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>BufferSize - maximal buffer queue capacity. Default value - 512. (Optional)</li>
 * <li>BufferOfferTimeout - how long to wait if necessary for space to become available when adding data item to buffer
 * queue. Default value - 45sec. (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 1 $
 *
 * @see ArrayBlockingQueue
 * @see BlockingQueue#offer(Object, long, TimeUnit)
 */
public abstract class AbstractBufferedStream<T> extends TNTParseableInputStream<T> {
	private static final int DEFAULT_INPUT_BUFFER_SIZE = 512;
	private static final int DEFAULT_INPUT_BUFFER_OFFER_TIMEOUT = 3 * 15; // NOTE: sec.
	private static final Object DIE_MARKER = new Object();

	private int bufferSize;
	private int bufferOfferTimeout = DEFAULT_INPUT_BUFFER_OFFER_TIMEOUT;

	/**
	 * RAW activity data items buffer queue. Items in this queue are processed asynchronously by consumer thread(s).
	 */
	protected BlockingQueue<Object> inputBuffer;

	/**
	 * Constructs a new AbstractBufferedStream.
	 */
	protected AbstractBufferedStream() {
		this(DEFAULT_INPUT_BUFFER_SIZE);
	}

	/**
	 * Constructs a new AbstractBufferedStream.
	 *
	 * @param bufferSize
	 *            default buffer size value. Actual value may be overridden by setting 'BufferSize' property.
	 */
	protected AbstractBufferedStream(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamProperties.PROP_BUFFER_SIZE.equalsIgnoreCase(name)) {
				bufferSize = Integer.parseInt(value);
			} else if (StreamProperties.PROP_OFFER_TIMEOUT.equalsIgnoreCase(name)) {
				bufferOfferTimeout = Integer.parseInt(value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_BUFFER_SIZE.equalsIgnoreCase(name)) {
			return bufferSize;
		}
		if (StreamProperties.PROP_OFFER_TIMEOUT.equalsIgnoreCase(name)) {
			return bufferOfferTimeout;
		}

		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		inputBuffer = new ArrayBlockingQueue<Object>(bufferSize, true);
		// inputBuffer = new SynchronousQueue<Object>(true);
	}

	/**
	 * Get the next activity data item to be processed. Method blocks and waits for activity input data available in
	 * input buffer. Input buffer is filled by {@link InputProcessor} thread.
	 *
	 * @return next activity data item, or {@code null} if there is no next item
	 *
	 * @throws Exception
	 *             if any errors occurred getting next item
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T getNextItem() throws Exception {
		if (inputBuffer == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"AbstractBufferedStream.changes.buffer.uninitialized"));
		}

		// Buffer is empty and producer input is ended. No more items going to
		// be available.
		if (inputBuffer.isEmpty() && isInputEnded()) {
			return null;
		}

		Object qe = inputBuffer.take();

		// Producer input was slower than consumer, but was able to put "DIE"
		// marker object to queue. No more items going to be available.
		if (DIE_MARKER.equals(qe)) {
			return null;
		}

		T activityInput = (T) qe;

		addStreamedBytesCount(getActivityItemByteSize(activityInput));

		return activityInput;
	}

	/**
	 * Gets activity data item size in bytes.
	 *
	 * @param activityItem
	 *            activity data item
	 * @return activity data item size in bytes
	 */
	protected abstract long getActivityItemByteSize(T activityItem);

	@Override
	protected void cleanup() {
		inputBuffer.clear();
		super.cleanup();
	}

	/**
	 * Adds input data to buffer for asynchronous processing. Input data may not be added if buffer size limit and offer
	 * timeout is exceeded.
	 *
	 * @param inputData
	 *            input data to add to buffer
	 * @return {@code true} if input data is added to buffer, {@code false} - otherwise
	 * @see BlockingQueue#offer(Object, long, TimeUnit)
	 */
	protected boolean addInputToBuffer(T inputData) {
		if (inputData != null && !isHalted()) {
			try {
				boolean added = inputBuffer.offer(inputData, bufferOfferTimeout, TimeUnit.SECONDS);

				if (!added) {
					logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"AbstractBufferedStream.changes.buffer.limit"), bufferOfferTimeout, inputData);
				}

				return added;
			} catch (InterruptedException exc) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"AbstractBufferedStream.offer.interrupted"), inputData);
			}
		}

		return false;
	}

	/**
	 * Checks if stream data input is ended.
	 *
	 * @return {@code true} if stream input ended, {@code false} - otherwise
	 * @see InputProcessor#isInputEnded()
	 */
	protected abstract boolean isInputEnded();

	/**
	 * Base class containing common features for stream input processor thread.
	 */
	protected abstract class InputProcessor extends StreamsThread {

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
		 * @return {@code true} input processor is interrupted or parent thread is halted
		 */
		protected boolean isStopping() {
			return isStopRunning() || isHalted();
		}

		/**
		 * Closes opened data input resources and marks stream data input as ended.
		 *
		 * @throws Exception
		 *             if fails to close opened resources due to internal error
		 */
		void close() throws Exception {
			// mark input end (in case producer thread is faster than consumer).
			markInputEnd();
			// add "DIE" marker to buffer (in case producer thread is slower
			// than waiting consumer).
			inputBuffer.offer(DIE_MARKER);
		}

		/**
		 * Shuts down stream input processor: interrupts thread and closes opened data input resources.
		 */
		protected void shutdown() {
			if (isStopRunning() && inputEnd) {
				// shot down already.
				return;
			}

			halt();
			try {
				close();
			} catch (Exception exc) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
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
