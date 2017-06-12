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

package com.jkoolcloud.tnt4j.streams.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.commons.collections4.MapUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * General logger utility methods used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public class LoggerUtils {
	private static final int LOG4J = 1 << 0;
	private static final int LOGBACK = 1 << 1;
	private static final int JUL = 1 << 2;
	private static final int SLF4J = 1 << 10; // NOTE: SLF4J may be as a logger (SLF4JSimple) or as wrapper.

	/**
	 * Detects used logging framework. Now recognizes these logger frameworks:
	 * <ul>
	 * <li>LOG4J</li>
	 * <li>JUL - Java util logging</li>
	 * <li>LOGBACK</li>
	 * <li>SLF4J - may be in combination with any from above</li>
	 * </ul>
	 *
	 * @param logger
	 *            logger event sink instance
	 *
	 * @return number containing used logging frameworks describing bits
	 */
	public static int detectLogger(EventSink logger) {
		int loggerMask = 0;

		Class<?> shc = logger.getSinkHandle().getClass();
		if (shc.getName().startsWith("org.apache.log4j.")) { // NON-NLS
			loggerMask |= LOG4J;
		} else if (shc.getName().startsWith("ch.qos.logback.")) { // NON-NLS
			loggerMask |= LOGBACK;
		} else if (shc.getName().startsWith("java.util.logging.")) { // NON-NLS
			loggerMask |= JUL;
		} else if (shc.getName().startsWith("org.slf4j.")) { // NON-NLS
			loggerMask |= SLF4J;

			if (shc.getName().equals("org.slf4j.impl.Log4jLoggerAdapter")) { // NON-NLS
				loggerMask |= LOG4J;
			} else if (shc.getName().equals("org.slf4j.impl.JDK14LoggerAdapter")) { // NON-NLS
				loggerMask |= JUL;
			} else {
				// TODO: anything else?
			}
		}

		return loggerMask;
	}

	/**
	 * Applies provided logger configuration data.
	 *
	 * @param cfgData
	 *            binary logger configuration data
	 * @param logger
	 *            logger instance
	 */
	public static void setLoggerConfig(byte[] cfgData, EventSink logger) {
		int lUsed = detectLogger(logger);

		if (Utils.matchMask(lUsed, LOG4J)) {
			setLog4jConfig(cfgData, logger);
		} else if (Utils.matchMask(lUsed, LOGBACK)) {
			setLogbackConfig(cfgData, logger);
		} else if (Utils.matchMask(lUsed, JUL)) {
			setJULConfig(cfgData, logger);
		} else {
			logger.log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "LoggerUtils.unknown.logger"));
		}
	}

	private static void setLog4jConfig(byte[] data, EventSink logger) {
		Properties loggerProps = new Properties();
		InputStream is = new ByteArrayInputStream(data);
		try {
			loggerProps.load(is);
		} catch (Exception exc) {
			logger.log(OpLevel.ERROR,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "LoggerUtils.log4j.load.error"),
					exc);
		} finally {
			Utils.close(is);
		}

		if (MapUtils.isEmpty(loggerProps)) {
			logger.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"LoggerUtils.log4j.empty.configuration"));
		} else {
			logger.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"LoggerUtils.log4j.reconfiguring"), loggerProps.size());
			try {
				// org.apache.log4j.PropertyConfigurator.configure(loggerProps);

				invoke("org.apache.log4j.PropertyConfigurator", "configure", new Class[] { Properties.class }, // NON-NLS
						loggerProps);
			} catch (Exception exc) {
				logger.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"LoggerUtils.log4j.reconfiguring.fail"), exc);
			}

			logger.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"LoggerUtils.log4j.reconfiguring.end"));
		}
	}

	private static void setJULConfig(byte[] data, EventSink logger) {
		InputStream is = new ByteArrayInputStream(data);
		try {
			logger.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "LoggerUtils.jul.reconfiguring"));
			java.util.logging.LogManager.getLogManager().readConfiguration(is);
		} catch (Exception exc) {
			logger.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"LoggerUtils.jul.reconfiguring.fail"), exc);
		} finally {
			Utils.close(is);
		}

		logger.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "LoggerUtils.jul.reconfiguring.end"));
	}

	private static void setLogbackConfig(byte[] data, EventSink logger) {
		InputStream is = new ByteArrayInputStream(data);
		try {
			logger.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"LoggerUtils.logback.reconfiguring"));
			// ch.qos.logback.classic.LoggerContext context = (ch.qos.logback.classic.LoggerContext)
			// org.slf4j.LoggerFactory.getILoggerFactory();
			Object context = invoke("org.slf4j.LoggerFactory", "getILoggerFactory", null); // NON-NLS
			// ch.qos.logback.classic.joran.JoranConfigurator jc = new ch.qos.logback.classic.joran.JoranConfigurator();
			Object jc = Utils.createInstance("ch.qos.logback.classic.joran.JoranConfigurator"); // NON-NLS
			// jc.setContext(context);
			Class<?> ctxClass = Class.forName("ch.qos.logback.classic.LoggerContext");
			invoke(jc, "setContext", new Class[] { ctxClass }, context); // NON-NLS
			// context.reset();
			invoke(context, "reset", null); // NON-NLS
			// jc.doConfigure(is);
			invoke(jc, "doConfigure", new Class[] { InputStream.class }, is); // NON-NLS
		} catch (Exception exc) {
			logger.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"LoggerUtils.logback.reconfiguring.fail"), exc);
		} finally {
			Utils.close(is);
		}

		logger.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
				"LoggerUtils.logback.reconfiguring.end"));
	}

	private static Object invoke(String className, String methodName, Class<?>[] paramTypes, Object... params)
			throws Exception {
		return invoke(null, className, methodName, paramTypes, params);
	}

	private static Object invoke(Object obj, String methodName, Class<?>[] paramTypes, Object... params)
			throws Exception {
		return invoke(obj, null, methodName, paramTypes, params);
	}

	private static Object invoke(Object obj, String className, String methodName, Class<?>[] paramTypes,
			Object... params) throws Exception {
		Class<?> cls = obj == null ? Class.forName(className) : obj.getClass();
		Method m = cls.getDeclaredMethod(methodName, paramTypes);
		return m.invoke(obj, params);
	}

}
