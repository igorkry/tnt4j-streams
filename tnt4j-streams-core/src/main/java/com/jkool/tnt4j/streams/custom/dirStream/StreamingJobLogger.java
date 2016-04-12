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

package com.jkool.tnt4j.streams.custom.dirStream;

import com.jkool.tnt4j.streams.inputs.StreamingStatus;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.nastel.jkool.tnt4j.core.OpLevel;

/**
 * For internal development use.
 * 
 * @version $Revision: 1 $
 */
public class StreamingJobLogger implements StreamingJobListener<Object> {

	public StreamingJobLogger() {

	}

	@Override
	public void onProgressUpdate(StreamingJob job, int current, int total) {
		System.out.println("Streaming progress update: job=" + job + " progress=" + current + "/" + total);
	}

	@Override
	public void onSuccess(StreamingJob job) {
		System.out.println("Streaming job success: job=" + job);
	}

	@Override
	public void onFailure(StreamingJob job, String msg, Throwable exc, String code) {
		System.out.println("Streaming job error: job=" + job + " msg=" + msg + " code=" + code + " exc=" + exc);
	}

	@Override
	public void onStatusChange(StreamingJob job, StreamingStatus status) {
		System.out.println("Streaming job status change: job=" + job + " status=" + status);
	}

	@Override
	public void onFinish(StreamingJob job, TNTInputStream.StreamStats stats) {
		System.out.println("Streaming job finished: job=" + job + " stats=" + stats);
	}

	@Override
	public void onStreamEvent(StreamingJob job, OpLevel level, String message, Object source) {
		System.out.println("Streaming job event occurred: job=" + job + " level=" + level + " msg=" + message
				+ " source=" + source);
	}
}
