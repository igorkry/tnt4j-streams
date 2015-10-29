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
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.types.MessageType;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>This class extends the basic activity XML parser for handling data specific
 * to messaging operations.  It provides additional transformations of the raw
 * activity data collected for specific fields.</p>
 * <p>In particular, this class will convert the signature and correlation field
 * values from a tokenized list of items into a value in the appropriate form
 * required by the jKool Cloud Service.</p>
 * <p>This parser supports the following properties (in addition to those
 * supported by {@link ActivityXmlParser}):
 * <ul>
 * <li>SignatureDelim</li>
 * </ul>
 *
 * @version $Revision: 6 $
 */
public class MessageActivityXmlParser extends ActivityXmlParser
{
  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (MessageActivityXmlParser.class);
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
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProperties (Collection<Map.Entry<String, String>> props) throws Throwable
  {
    if (props == null)
    {
      return;
    }
    super.setProperties (props);
    for (Map.Entry<String, String> prop : props)
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
      case TrackingId:
        Object[] sigItems = null;
        if (value instanceof Object[])
        {
          sigItems = (Object[]) value;
        }
        else if (value instanceof String)
        {
          String sigStr = (String) value;
          if (sigStr.contains (sigDelim))
          {
            sigItems = sigStr.split (sigDelim);
          }
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
            {
              continue;
            }
            switch (i)
            {
              case 0:
                msgType = item instanceof Number ? MessageType.valueOf (((Number) item).intValue ())
                                                 : MessageType.valueOf (Integer.parseInt (item.toString ()));
                break;
              case 1:
                msgFormat = item.toString ();
                break;
              case 2:
                msgId = item instanceof byte[] ? (byte[]) item : item.toString ().getBytes ();
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
          LOGGER.log (OpLevel.TRACE,
                      "Message Signature ({0}):  msgType={1}  msgFormat={2}  msgId={3} [{4}]  userId={5}  putApplType={6}  putApplName={7}  putDate={8}  putTime={9}",
                      value, msgType, msgFormat, msgId == null ? "null" : new String (Utils.encodeHex (msgId)),
                      msgId == null ? "null" : new String (msgId), msgUser, msgApplType, msgApplName, msgPutDate, msgPutTime);
        }
        break;
      default:
        break;
    }
    super.applyFieldValue (ai, field, value);
  }
}
