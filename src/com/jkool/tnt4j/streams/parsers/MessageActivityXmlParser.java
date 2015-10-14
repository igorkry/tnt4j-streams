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

package com.jkool.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map.Entry;
import javax.xml.parsers.ParserConfigurationException;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.types.MessageType;
import com.jkool.tnt4j.streams.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * <p>This class extends the basic activity XML parser for handling data specific
 * to messaging operations.  It provides additional transformations of the raw
 * activity data collected for specific fields.</p>
 * <p>In particular, this class will convert the signature and correlation field
 * values from a tokenized list of items into a value in the appropriate form
 * required by the analyzer.</p>
 * <p>This parser supports the following properties (in addition to those
 * supported by {@link ActivityXmlParser}):
 * <ul>
 * <li>SignatureDelim</li>
 * </ul>
 * </p>
 *
 * @version $Revision: 6 $
 */
public class MessageActivityXmlParser extends ActivityXmlParser
{
  private static final Logger logger = Logger.getLogger (MessageActivityXmlParser.class);
  /**
   * Contains the field separator (set by {@code SignatureDelim} property) - Default: ","
   */
  protected String sigDelim = ",";

  /**
   * Constructs a MessageActivityXmlParser.
   *
   * @throws ParserConfigurationException if any errors configuring the parser
   */
  public MessageActivityXmlParser () throws ParserConfigurationException
  {
    super ();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProperties (Collection<Entry<String, String>> props) throws Throwable
  {
    if (props == null)
    { return; }
    super.setProperties (props);
    for (Entry<String, String> prop : props)
    {
      String name = prop.getKey ();
      String value = prop.getValue ();
      if (StreamsConfig.PROP_SIG_DELIM.equalsIgnoreCase (name))
      {
        if (!StringUtils.isEmpty (value))
        {
          sigDelim = value;
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>This method applies custom handling for setting field values.  This
   * method will construct the signature to use for the message from the
   * specified value, which is assumed to be a string containing the inputs
   * required for the message signature calculation, with each input separated
   * by the delimiter specified in property {@code SignatureDelim}.</p>
   * <p>The signature items MUST be specified in the following order:</p>
   * <ol>
   * <li>Message Type</li>
   * <li>Message Format</li>
   * <li>Message ID</li>
   * <li>Message User</li>
   * <li>Message Application Type</li>
   * <li>Message Application Name</li>
   * <li>Message Date</li>
   * <li>Message Time</li>
   * </ol>
   * <p>Individual items can be omitted, but must contain a place holder (except
   * for trailing items).</p>
   */
  @Override
  protected void applyFieldValue (ActivityInfo ai, ActivityField field, Object value) throws ParseException
  {
    switch (field.getFieldType ())
    {
      case Correlator:
      case Signature:
        Object[] sigItems = null;
        if (value instanceof Object[])
        { sigItems = (Object[]) value; }
        else if (value instanceof String)
        {
          String sigStr = (String) value;
          if (sigStr.contains (sigDelim))
          { sigItems = sigStr.split (sigDelim); }
        }
        if (sigItems != null)
        {
          MessageType msgType = null;
          String msgFormat = null;
          byte[] msgId = null;
          String msgUser = null;
          String msgApplType = null;
          String msgApplName = null;
          String msgPutDate = null;
          String msgPutTime = null;
          for (int i = 0; i < sigItems.length; i++)
          {
            Object item = sigItems[i];
            if (item == null)
            { continue; }
            switch (i)
            {
              case 0:
                if (item instanceof Number)
                { msgType = MessageType.valueOf (((Number) item).intValue ()); }
                else
                { msgType = MessageType.valueOf (Integer.parseInt (item.toString ())); }
                break;
              case 1:
                msgFormat = item.toString ();
                break;
              case 2:
                if (item instanceof byte[])
                { msgId = (byte[]) item; }
                else
                { msgId = item.toString ().getBytes (); }
                break;
              case 3:
                msgUser = item.toString ();
                break;
              case 4:
                msgApplType = item.toString ();
                break;
              case 5:
                msgApplName = item.toString ();
                break;
              case 6:
                msgPutDate = item.toString ();
                break;
              case 7:
                msgPutTime = item.toString ();
                break;
              default:
                break;
            }
          }
          value = Utils.computeSignature (msgType, msgFormat, msgId, msgUser, msgApplType, msgApplName, msgPutDate, msgPutTime);
          if (logger.isTraceEnabled ())
          {
            logger.trace ("Message Signature (" + value + "):  msgType=" + msgType + "  msgFormat=" + msgFormat +
                          "  msgId=" + (msgId == null ? "null" : new String (Utils.encodeHex (msgId))) +
                          " [" + (msgId == null ? "null" : new String (msgId)) + "]" +
                          "  userId=" + msgUser + "  putApplType=" + msgApplType + "  putApplName=" + msgApplName +
                          "  putDate=" + msgPutDate + "  putTime=" + msgPutTime);
          }
        }
        break;
      default:
        break;
    }
    super.applyFieldValue (ai, field, value);
  }
}
