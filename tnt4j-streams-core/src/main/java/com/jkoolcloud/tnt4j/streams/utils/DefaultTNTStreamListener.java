/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.List;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.*;

/**
 * For internal development use.
 *
 * @author akausinis
 * @version 1.0
 */
public class DefaultTNTStreamListener implements InputStreamListener, StreamTasksListener {
	private final EventSink logger;

	/**
	 * Instantiates a new Default tnt stream listener.
	 *
	 * @param logger
	 *            the logger
	 */
	public DefaultTNTStreamListener(EventSink logger) {
		this.logger = logger;
	}

	@Override
	public void onProgressUpdate(TNTInputStream<?, ?> stream, int current, int total) {
		logger.log(OpLevel.CRITICAL, "Stream {0} progress {1}%", stream.getName(), ((double) current / total) * 100); // NON-NLS
	}

	@Override
	public void onSuccess(TNTInputStream<?, ?> stream) {
		logger.log(OpLevel.CRITICAL, "Stream {0} ended successfully!", stream.getName());// NON-NLS
	}

	@Override
	public void onFailure(TNTInputStream<?, ?> stream, String msg, Throwable exc, String code) {
		logger.log(OpLevel.CRITICAL, "Stream {0} has failed: msg={1}, exc={2}, code={3}", stream.getName(), msg, exc, // NON-NLS
				code);
	}

	@Override
	public void onStatusChange(TNTInputStream<?, ?> stream, StreamStatus status) {
		logger.log(OpLevel.CRITICAL, "Stream {0} status has changed {1}", stream.getName(), status); // NON-NLS
	}

	@Override
	public void onFinish(TNTInputStream<?, ?> stream, TNTInputStreamStatistics stats) {
		logger.log(OpLevel.CRITICAL, "Stream {0} finished! Statistics: {1}", stream.getName(), stats); // NON-NLS
	}

	@Override
	public void onStreamEvent(TNTInputStream<?, ?> stream, OpLevel level, String message, Object source) {
		logger.log(OpLevel.CRITICAL, "Stream {0} event occurred: level={1}, message={2}, source={3}", stream.getName(), // NON-NLS
				level, message, source);
	}

	@Override
	public void onReject(TNTInputStream<?, ?> stream, Runnable task) {
		logger.log(OpLevel.CRITICAL, "Stream {0} has rejected activity item streaming task {1}", stream.getName(), // NON-NLS
				task);
	}

	@Override
	public void onDropOff(TNTInputStream<?, ?> stream, List<Runnable> tasks) {
		logger.log(OpLevel.CRITICAL, "Stream {0} has dropped off {1} tasks!..", stream.getName(), tasks.size()); // NON-NLS
	}
}
