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

package com.jkool.tnt4j.streams.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

import com.nastel.jkool.tnt4j.core.UsecTimestamp;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Represents a timestamp that has microsecond accuracy. When constructing
 * timestamp values from string it uses {@code Locale} to parse it.
 * </p>
 *
 * @version $Revision: 3 $
 * @see com.nastel.jkool.tnt4j.core.UsecTimestamp
 */
public class StreamTimestamp extends UsecTimestamp {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(StreamTimestamp.class);

	private static final String[] digits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };

	private static final long serialVersionUID = 584224868408250622L;

	/**
	 * Creates Stream Timestamp based on current time with microsecond
	 * precision/accuracy
	 *
	 * @see com.nastel.jkool.tnt4j.utils.Utils#currentTimeUsec()
	 */
	public StreamTimestamp() {
		super();
	}

	/**
	 * Creates Stream Timestamp based on specified Date.
	 *
	 * @param date
	 *            timestamp to copy
	 * @throws NullPointerException
	 *             if date is {@code null}
	 */
	public StreamTimestamp(Date date) {
		super(date);
	}

	/**
	 * Creates Stream Timestamp based on specified millisecond timestamp.
	 *
	 * @param mSecs
	 *            timestamp, in milliseconds
	 *
	 * @throws IllegalArgumentException
	 *             if mSecs is negative
	 */
	public StreamTimestamp(long mSecs) {
		this(mSecs, 0);
	}

	/**
	 * Creates Stream Timestamp based on specified millisecond timestamp and
	 * fractional microsecond.
	 *
	 * @param mSecs
	 *            timestamp, in milliseconds
	 * @param uSecs
	 *            fraction microseconds
	 *
	 * @throws IllegalArgumentException
	 *             if any arguments are negative, or if uSecs is greater than
	 *             999
	 */
	public StreamTimestamp(long mSecs, long uSecs) {
		super(mSecs, uSecs);
	}

	/**
	 * Creates Stream Timestamp based on specified Timestamp, providing time in
	 * seconds resolution, and fractional microsecond.
	 *
	 * @param timestamp
	 *            database timestamp, seconds resolution
	 * @param uSecs
	 *            fraction microseconds
	 *
	 * @throws NullPointerException
	 *             if timestamp is {@code null}
	 * @throws IllegalArgumentException
	 *             if usecs is greater than 999999
	 */
	public StreamTimestamp(java.sql.Timestamp timestamp, long uSecs) {
		super(timestamp, uSecs);
	}

	/**
	 * Creates Stream Timestamp based on specified Stream Timestamp.
	 *
	 * @param other
	 *            timestamp to copy
	 *
	 * @throws NullPointerException
	 *             if timestamp is {@code null}
	 */
	public StreamTimestamp(StreamTimestamp other) {
		super(other);
	}

	/**
	 * <p>
	 * Creates Stream Timestamp from string representation of timestamp in the
	 * specified format.
	 * </p>
	 * <p>
	 * This is based on {@link SimpleDateFormat}, but extends its support to
	 * recognize microsecond fractional seconds. If number of fractional second
	 * characters is greater than 3, then it's assumed to be microseconds.
	 * Otherwise, it's assumed to be milliseconds (as this is the behavior of
	 * {@link SimpleDateFormat}.
	 *
	 * @param timeStampStr
	 *            timestamp string
	 * @param formatStr
	 *            format specification for timestamp string
	 * @param timeZoneId
	 *            time zone that timeStampStr represents. This is only needed
	 *            when formatStr does not include time zone specification and
	 *            timeStampStr does not represent a string in local time zone.
	 * @param locale
	 *            locale for date format to use.
	 *
	 * @throws IllegalArgumentException
	 *             if timeStampStr is {@code null} or timeStampStr is not in the
	 *             correct format
	 * @throws ParseException
	 *             if failed to parse string based on specified format
	 * @see java.util.TimeZone
	 * @since Revision: 10
	 */
	public StreamTimestamp(String timeStampStr, String formatStr, String timeZoneId, String locale)
			throws ParseException {
		if (timeStampStr == null) {
			throw new IllegalArgumentException(StreamsResources.getString("StreamTimestamp.null.timestamp.srt"));
		}
		long uSecs = 0L;
		SimpleDateFormat dateFormat;
		if (StringUtils.isEmpty(formatStr)) {
			dateFormat = new SimpleDateFormat();
		} else {
			// Java date formatter cannot deal with usecs, so we need to extract
			// those ourselves
			int fmtPos = formatStr.indexOf('S');
			if (fmtPos > 0) {
				int endFmtPos = formatStr.lastIndexOf('S');
				int fmtFracSecLen = endFmtPos - fmtPos + 1;
				if (fmtFracSecLen > 6) {
					throw new ParseException(
							StreamsResources.getString("StreamTimestamp.fraction.length.not.supported"), 0);
				}
				if (fmtFracSecLen > 3) {
					// format specification represents more than milliseconds,
					// assume microseconds
					int uSecEndPos = StringUtils.lastIndexOfAny(timeStampStr, digits);
					if (uSecEndPos > 2) {
						int uSecPos = timeStampStr.lastIndexOf('.', uSecEndPos) + 1;
						String uSecStr = String.format("%s", timeStampStr.substring(uSecPos, uSecEndPos + 1)); // NON-NLS
						if (uSecStr.length() < fmtFracSecLen) {
							uSecStr = StringUtils.rightPad(uSecStr, fmtFracSecLen, '0');
						} else if (uSecStr.length() > fmtFracSecLen) {
							uSecStr = uSecStr.substring(0, fmtFracSecLen);
						}
						uSecs = Long.parseLong(uSecStr);
						// trim off fractional part < microseconds from both
						// timestamp and format strings
						StringBuilder sb = new StringBuilder(timeStampStr);
						sb.delete(uSecPos - 1, uSecEndPos + 1);
						timeStampStr = sb.toString();
						sb.setLength(0);
						sb.append(formatStr);
						sb.delete(fmtPos - 1, endFmtPos + 1);
						formatStr = sb.toString();
					}
				}
			}
			dateFormat = StringUtils.isEmpty(locale) ? new SimpleDateFormat(formatStr)
					: new SimpleDateFormat(formatStr, Locale.forLanguageTag(locale));
		}
		if (!StringUtils.isEmpty(timeZoneId)) {
			dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneId));
		}
		Date date = dateFormat.parse(timeStampStr);
		setTimestampValues(date.getTime(), 0L, 0L);
		add(0L, uSecs);
	}

	/**
	 * Makes copy of Stream timestamp object.
	 *
	 * @return a clone of this instance.
	 *
	 * @throws CloneNotSupportedException
	 *             if the object's class does not support the {@code Cloneable}
	 *             interface. Subclasses that override the {@code clone} method
	 *             can also throw this exception to indicate that an instance
	 *             cannot be cloned.
	 * @see Object#clone()
	 */
	@Override
	public StreamTimestamp clone() throws CloneNotSupportedException {
		return (StreamTimestamp) super.clone();
	}
}
