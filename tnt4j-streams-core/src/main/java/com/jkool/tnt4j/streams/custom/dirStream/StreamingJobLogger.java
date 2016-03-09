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

/**
 * @author akausinis
 * @version 1.0 TODO
 */
public class StreamingJobLogger implements StreamingJobListener<Object> {
	@Override
	public void onProgressUpdate(StreamingJob job, int current, int total) {
		System.out.println("Streaming progress update " + job + " " + current + "/" + total);
	}

	@Override
	public void onSuccess(StreamingJob job, Object result) {
		System.out.println("Streaming job success: " + job + " " + result);
	}

	@Override
	public void onFailure(StreamingJob job, String msg, Throwable exc, Integer code) {
		System.out.println("Streaming job error: " + job + " " + msg + " " + code + " " + exc);
	}

	@Override
	public void onStatusChange(StreamingJob job, StreamingJobStatus status) {
		System.out.println("Streaming job status change: " + job + " " + status);
	}

	@Override
	public void onFinish(StreamingJob job) {
		System.out.println("Streaming job finished: " + job);
	}
}
