/*
 * Copyright 2014-2017 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.transform;

/**
 * This exception is thrown when data value transformation process fails.
 *
 * @version $Revision: 1 $
 */
public class TransformationException extends Exception {
	private static final long serialVersionUID = -9071248676984863804L;

	/**
	 * Constructs an {@code TransformationException} with the specified detail message.
	 *
	 * @param message
	 *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
	 *            method.
	 */
	public TransformationException(String message) {
		super(message);
	}

	/**
	 * Constructs an {@code TransformationException} with the specified detail message and cause.
	 *
	 * @param message
	 *            the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
	 *            value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public TransformationException(String message, Throwable cause) {
		super(message, cause);
	}
}
