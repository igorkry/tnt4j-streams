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

package com.jkoolcloud.tnt4j.streams.inputs;

/**
 * TODO
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 1 $
 *
 * @see TNTInputStream#setInput(TNTStreamInput)
 * @see com.jkoolcloud.tnt4j.streams.outputs.TNTStreamOutput
 */
public interface TNTStreamInput<T> {

	/**
	 * TODO
	 * 
	 * @return
	 * @throws Exception
	 */
	T getNextItem() throws Exception;

	/**
	 * Performs initialization of stream input handler.
	 * 
	 * @throws Exception
	 *             indicates that stream input handler is not configured properly
	 */
	void initialize() throws Exception;

	/**
	 * Performs stream input handler cleanup.
	 *
	 * @see TNTInputStream#cleanup()
	 */
	void cleanup();

	/**
	 * Sets related {@link TNTInputStream} instance.
	 *
	 * @param inputStream
	 *            related input stream instance
	 */
	void setStream(TNTInputStream<?, ?> inputStream);
}
