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

package com.jkool.tnt4j.streams.custom.parsers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.jkool.tnt4j.streams.parsers.ActivityRegExParser;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a custom Apache access log parser based on RegEx parsing. User can
 * define RegEx string to parse log as for ordinary activity RegEx parser.
 * </p>
 * <p>
 * >But it is also possible to use Apache access log configuration pattern over
 * LogPattern parameter. Then RegEx is generated from it. Additional config
 * pattern tokens may be mapped to RegEx'es using ConfRegexMapping parameters.
 * </p>
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>LogPattern</li>
 * <li>ConfRegexMapping</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ApacheAccessLogParser extends ActivityRegExParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ApacheAccessLogParser.class);

	/**
	 * Constant for name of built-in {@value} property.
	 */
	protected static final String PROP_APACHE_LOG_PATTERN = "LogPattern"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	protected static final String PROP_CONF_REGEX_MAPPING = "ConfRegexMapping"; // NON-NLS

	private static final String APACHE_LOG_CONFIG_TOKEN_REPLACEMENT_REGEX = "%\\S*(%|\\w)"; // NON-NLS

	private static final String DEFAULT_LOG_TOKEN_REGEX = "(\\S+)"; // NON-NLS
	private static final String STATUS_LOG_TOKEN_REGEX = "(\\d{3})"; // NON-NLS
	private static final String REQUEST_LOG_TOKEN_REGEX = "((\\S+) (\\S+) (\\S+))"; // NON-NLS

	/**
	 * Apache access log configuration pattern string.
	 */
	protected String apacheLogPattern = null;

	/**
	 * Defines mapping between Apache access log configuration pattern token
	 * strings and RegEx strings used to parse log entries.
	 */
	protected final Map<String, String> configRegexMappings = new HashMap<String, String>();
	protected final Map<String, String> userRegexMappings = new HashMap<String, String>();

	/**
	 * Constructs an ApacheAccessLogParser.
	 */
	public ApacheAccessLogParser() {
		fillDefaultConfigRegexMappings();
	}

	/**
	 * Fills default Apache access log configuration to RegEx mappings.
	 */
	private void fillDefaultConfigRegexMappings() {
		configRegexMappings.put("%%", "%"); // NON-NLS NON-NLS
		configRegexMappings.put("%a", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%A", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%B", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%b", "(\\d+|-)"); // NON-NLS NON-NLS
		configRegexMappings.put("%*C", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*D", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*e", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%f", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%h", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%H", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*i", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%k", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%l", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%m", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*n", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*o", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*p", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*P", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%q", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*r", REQUEST_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%R", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*s", STATUS_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*t", "\\[([\\w:/]+\\s[+\\-]\\d{4})\\]"); // NON-NLS
																			// NON-NLS
		configRegexMappings.put("%*T", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*u", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%*U", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%v", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%V", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%X", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%I", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
		configRegexMappings.put("%O", DEFAULT_LOG_TOKEN_REGEX); // NON-NLS
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		super.setProperties(props);
		if (props == null) {
			return;
		}
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (PROP_APACHE_LOG_PATTERN.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					apacheLogPattern = value;
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getStringFormatted("ActivityParser.setting", name, value));
				}
			} else if (PROP_CONF_REGEX_MAPPING.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					int idx = value.indexOf('=');
					if (idx > 0) {
						String confKey = value.substring(0, idx);
						String regex = value.substring(idx + 1);

						String oldRegex = userRegexMappings.put(confKey, regex);
						LOGGER.log(OpLevel.DEBUG,
								StreamsResources.getStringFormatted("ActivityParser.setting", name, value));
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(
								"ApacheAccessLogParser.setting.regex.mapping", confKey, oldRegex, regex));
					}
				}
			}
			LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted("ActivityParser.ignoring", name));
		}

		if (pattern == null && StringUtils.isNotEmpty(apacheLogPattern)) {
			String regex = makeRegexPattern(apacheLogPattern);
			if (regex != null) {
				pattern = Pattern.compile(regex);
				LOGGER.log(OpLevel.DEBUG,
						StreamsResources.getStringFormatted("ApacheAccessLogParser.regex.made", regex));
			} else {
				LOGGER.log(OpLevel.TRACE, StreamsResources
						.getStringFormatted("ApacheAccessLogParser.could.not.make.regex", apacheLogPattern));
			}
		}
	}

	/**
	 * Makes log entry parsing RegEx from defined Apache access log
	 * configuration pattern string.
	 *
	 * @param apacheLogPattern
	 *            Apache access log configuration pattern string
	 *
	 * @return regular expression string, or {@code null} if can't make RegEx
	 *         string from defined Apache access log configuration pattern
	 *         string
	 */
	private String makeRegexPattern(String apacheLogPattern) {
		Pattern pattern = Pattern.compile(APACHE_LOG_CONFIG_TOKEN_REPLACEMENT_REGEX);
		Matcher matcher = pattern.matcher(apacheLogPattern);
		StringBuilder logRegexBuff = new StringBuilder();
		int pos = 0;
		while (matcher.find()) {
			logRegexBuff.append(apacheLogPattern.substring(pos, matcher.start()));
			logRegexBuff.append(mapConfigTokenToRegex(matcher.group()));
			pos = matcher.end();
		}

		String logRegex = logRegexBuff.toString().trim();
		// return logRegex.isEmpty() ? null : "(?m)^" + logRegex;
		return logRegex.isEmpty() ? null : "^" + logRegex; // NON-NLS
	}

	/**
	 * Maps Apache access log configuration pattern token to user defined RegEx
	 * string. If no user defined mapping is found, then default mapping is
	 * used.
	 *
	 * @param configToken
	 *            Apache access log configuration pattern token string
	 *
	 * @return RegEx string matching configuration token
	 */
	private String mapConfigTokenToRegex(String configToken) {
		String confRegexMapping = findMapping(configToken);
		if (confRegexMapping != null) {
			return confRegexMapping;
		}

		return mapConfigTokenRegexDefault(configToken);
	}

	/**
	 * Finds user defined mapping of Apache access log configuration pattern
	 * token to RegEx string.
	 *
	 * @param configToken
	 *            Apache access log configuration pattern token string
	 *
	 * @return mapped RegEx string
	 */
	private String findMapping(String configToken) {
		String regex = findRegexMapping(configToken, userRegexMappings);

		if (StringUtils.isEmpty(regex)) {
			regex = findRegexMapping(configToken, configRegexMappings);
		}

		return StringUtils.isEmpty(regex) ? null : regex;
	}

	private static String findRegexMapping(String configToken, Map<String, String> regexMappings) {
		if (regexMappings != null) {
			for (Map.Entry<String, String> e : regexMappings.entrySet()) {
				if (isMatchingPattern(configToken, e.getKey())) {
					return e.getValue();
				}
			}
		}

		return null;
	}

	/**
	 * Returns default RegEx string for defined Apache access log configuration
	 * token.
	 *
	 * @param configToken
	 *            Apache access log configuration token string
	 *
	 * @return default RegEx string for configuration token
	 */
	private static String mapConfigTokenRegexDefault(String configToken) {
		if (isMatchingPattern(configToken, "%*s")) { // NON-NLS
			return STATUS_LOG_TOKEN_REGEX;
		} else if (isMatchingPattern(configToken, "%*r")) { // NON-NLS
			return REQUEST_LOG_TOKEN_REGEX;
		}

		return DEFAULT_LOG_TOKEN_REGEX;
	}

	/**
	 * Checks if Apache access log configuration token matches defined pattern.
	 *
	 * @param configToken
	 *            Apache access log configuration token string
	 * @param pattern
	 *            pattern string to match configuration token
	 *
	 * @return true if Apache access log configuration token matches defined
	 *         pattern
	 */
	private static boolean isMatchingPattern(String configToken, String pattern) {
		String p = pattern.replace("*", "\\S*"); // NON-NLS

		return configToken.matches(p);
	}
}
