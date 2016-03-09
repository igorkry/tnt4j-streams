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

package com.jkool.tnt4j.streams.inputs;

import com.jkool.tnt4j.streams.fields.StreamStatus;

/**
 * @author akausinis
 * @version 1.0
 * @created 2016-03-07 15:03
 */
public interface InputStreamListener<T> {
	void onProgressUpdate(TNTInputStream stream, int current, int total);

	void onSuccess(TNTInputStream stream, T result);

	void onFailure(TNTInputStream stream, String msg, Throwable exc, Integer code);

	void onStatusChange(TNTInputStream stream, StreamStatus status);

	void onFinish(TNTInputStream stream);
}
