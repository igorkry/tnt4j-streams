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

import com.nastel.jkool.tnt4j.core.UsecTimestamp;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>Represents a timestamp that has microsecond accuracy.</p>
 *
 * @version $Revision: 3 $
 * @see com.nastel.jkool.tnt4j.core.UsecTimestamp
 */
public class Timestamp extends UsecTimestamp
{
  private static final long serialVersionUID = 584224868408250622L;

  public Timestamp ()
  {
  }

  public Timestamp (long msecs)
  {
    super (msecs);
  }

  public Timestamp (long msecs, int usecs)
  {
    super (msecs, usecs);
  }

  public Timestamp (java.sql.Timestamp timestamp, int usecs)
  {
    super (timestamp, usecs);
  }

  public Timestamp (Timestamp other)
  {
    super (other);
  }

  /**
   * <p>Creates UsecTimestamp from string representation of timestamp in the
   * specified format.</p>
   * <p>This is based on {@link SimpleDateFormat}, but extends its support to
   * recognize microsecond fractional seconds.  If number of fractional second
   * characters is greater than 3, then it's assumed to be microseconds.
   * Otherwise, it's assumed to be milliseconds (as this is the behavior of
   * {@link SimpleDateFormat}.
   *
   * @param timeStampStr timestamp string
   * @param formatStr    format specification for timestamp string
   * @param timeZoneId   time zone that timeStampStr represents. This is only needed when formatStr does not include
   *                     time zone specification and timeStampStr does not represent a string in local time zone.
   * @param locale       locale for date format to use.
   *
   * @throws NullPointerException     if timeStampStr is {@code null}
   * @throws IllegalArgumentException if timeStampStr is not in the correct format
   * @throws ParseException           if failed to parse string based on specified format
   * @see java.util.TimeZone
   * @since Revision: 10
   */
  public Timestamp (String timeStampStr, String formatStr, String timeZoneId, String locale) throws ParseException
  {
    if (timeStampStr == null)
    { throw new NullPointerException ("timeStampStr must be non-null"); }
    int usecs = 0;
    SimpleDateFormat dateFormat;
    if (StringUtils.isEmpty (formatStr))
    {
      dateFormat = new SimpleDateFormat ();
    }
    else
    {
      // Java date formatter cannot deal with usecs, so we need to extract those ourselves
      int fmtPos = formatStr.indexOf ('S');
      if (fmtPos > 0)
      {
        int endFmtPos = formatStr.lastIndexOf ('S');
        int fmtFracSecLen = endFmtPos - fmtPos + 1;
        if (fmtFracSecLen > 6)
        { throw new ParseException ("Date format containing more than 6 significant digits for fractional seconds is not supported", 0); }
        if (fmtFracSecLen > 3)
        {
          // format specification represents more than milliseconds, assume microseconds
          int usecEndPos = StringUtils.lastIndexOfAny (timeStampStr, "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
          if (usecEndPos > 2)
          {
            int usecPos = timeStampStr.lastIndexOf ('.', usecEndPos) + 1;
            String usecStr = String.format ("%s", timeStampStr.substring (usecPos, usecEndPos + 1));
            if (usecStr.length () < fmtFracSecLen)
            { usecStr = StringUtils.rightPad (usecStr, fmtFracSecLen, '0'); }
            else if (usecStr.length () > fmtFracSecLen)
            { usecStr = usecStr.substring (0, fmtFracSecLen); }
            usecs = Integer.parseInt (usecStr);
            // trim off fractional part < microseconds from both timestamp and format strings
            StringBuilder sb = new StringBuilder (timeStampStr);
            sb.delete (usecPos - 1, usecEndPos + 1);
            timeStampStr = sb.toString ();
            sb.setLength (0);
            sb.append (formatStr);
            sb.delete (fmtPos - 1, endFmtPos + 1);
            formatStr = sb.toString ();
          }
        }
      }
      if (StringUtils.isEmpty (locale))
      {
        dateFormat = new SimpleDateFormat (formatStr);
      }
      else
      {
        dateFormat = new SimpleDateFormat (formatStr, Locale.forLanguageTag (locale));
      }
    }
    if (!StringUtils.isEmpty (timeZoneId))
    {
      dateFormat.setTimeZone (TimeZone.getTimeZone (timeZoneId));
    }
    Date date = dateFormat.parse (timeStampStr);
    setTimestampValues (date.getTime (), 0, 0);
    add (0, usecs);
  }
}
