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

import java.security.MessageDigest;

import com.jkool.tnt4j.streams.types.MessageType;
import com.nastel.jkool.tnt4j.core.OpType;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * General utility methods.
 *
 * @version $Revision: 16 $
 */
public final class Utils extends com.nastel.jkool.tnt4j.utils.Utils
{

  private Utils ()
  {
  }

  /**
   * Base64 encodes the specified sequence of bytes.
   *
   * @param src byte sequence to encode
   *
   * @return encoded byte sequence
   */
  public static byte[] base64Encode (byte[] src)
  {
    return Base64.encodeBase64 (src);
  }

  /**
   * Base64 decodes the specified sequence of bytes.
   *
   * @param src byte sequence to decode
   *
   * @return decoded byte sequence
   */
  public static byte[] base64Decode (byte[] src)
  {
    return Base64.decodeBase64 (src);
  }

  /**
   * Converts an array of bytes into an array of characters representing the hexadecimal values.
   *
   * @param src byte sequence to encode
   *
   * @return encoded character sequence
   */
  public static char[] encodeHex (byte[] src)
  {
    return Hex.encodeHex (src);
  }

  /**
   * Converts a string representing hexadecimal values into an array of bytes.
   *
   * @param str String to convert
   *
   * @return decoded byte sequence
   */
  public static byte[] decodeHex (String str)
  {
    byte[] ba = null;
    try
    {
      ba = Hex.decodeHex (str.toCharArray ());
    }
    catch (DecoderException e)
    {
    }
    return ba;
  }

  private static final MessageDigest MSG_DIGEST = getMD5Digester ();

  /**
   * <p>Generates a new unique message signature.  This signature is expected to be
   * used for creating a new message instance, and is intended to uniquely
   * identify the message regardless of which application is processing it.</p>
   * <p>It is up to the individual stream to determine which of these attributes is
   * available/required to uniquely identify a message.  In order to identify a message
   * within two different transports, the streams for each transport must provide the
   * same values.</p>
   *
   * @param msgType     message type
   * @param msgFormat   message format
   * @param msgId       message identifier
   * @param userId      user that originated the message
   * @param putApplType type of application that originated the message
   * @param putApplName name of application that originated the message
   * @param putDate     date (GMT) the message was originated
   * @param putTime     time (GMT) the message was originated
   *
   * @return unique message signature
   */
  public static String computeSignature (
      MessageType msgType, String msgFormat, byte[] msgId, String userId, String putApplType, String putApplName, String putDate,
      String putTime)
  {
    synchronized (MSG_DIGEST)
    {
      return computeSignature (MSG_DIGEST, msgType, msgFormat, msgId, userId, putApplType, putApplName, putDate, putTime);
    }
  }

  /**
   * <p>Generates a new unique message signature.  This signature is expected to be
   * used for creating a new message instance, and is intended to uniquely
   * identify the message regardless of which application is processing it.</p>
   * <p>It is up to the individual stream to determine which of these attributes is
   * available/required to uniquely identify a message.  In order to identify a message
   * within two different transports, the streams for each transport must provide the
   * same values.</p>
   *
   * @param _msgDigest  message type
   * @param msgType     message type
   * @param msgFormat   message format
   * @param msgId       message identifier
   * @param userId      user that originated the message
   * @param putApplType type of application that originated the message
   * @param putApplName name of application that originated the message
   * @param putDate     date (GMT) the message was originated
   * @param putTime     time (GMT) the message was originated
   *
   * @return unique message signature
   */
  public static String computeSignature (
      MessageDigest _msgDigest, MessageType msgType, String msgFormat, byte[] msgId, String userId, String putApplType, String putApplName,
      String putDate, String putTime)
  {
    _msgDigest.reset ();
    if (msgType != null)
    {
      _msgDigest.update (String.valueOf (msgType.value ()).getBytes ());
    }
    if (msgFormat != null)
    {
      _msgDigest.update (msgFormat.trim ().getBytes ());
    }
    if (msgId != null)
    {
      _msgDigest.update (msgId);
    }
    if (userId != null)
    {
      _msgDigest.update (userId.trim ().toLowerCase ().getBytes ());
    }
    if (putApplType != null)
    {
      _msgDigest.update (putApplType.trim ().getBytes ());
    }
    if (putApplName != null)
    {
      _msgDigest.update (putApplName.trim ().getBytes ());
    }
    if (putDate != null)
    {
      _msgDigest.update (putDate.trim ().getBytes ());
    }
    if (putTime != null)
    {
      _msgDigest.update (putTime.trim ().getBytes ());
    }
    return new String (base64Encode (_msgDigest.digest ()));
  }

  /**
   * Maps OpType enumeration literal from string or numeric value.
   *
   * @param opType object to be mapped to OpType enumeration literal
   *
   * @return OpType mapping or {@code null} if mapping not found.
   */
  public static OpType mapOpType (Object opType)
  {
    if (opType == null)
    {
      return null;
    }
    if (opType instanceof Number)
    {
      return mapOpType (((Number) opType).intValue ());
    }
    return mapOpType (opType.toString ());
  }

  private static OpType mapOpType (String opType)
  {
    if (opType.equalsIgnoreCase ("OTHER"))
    {
      return OpType.OTHER;
    }
    if (opType.equalsIgnoreCase ("START"))
    {
      return OpType.START;
    }
    if (opType.equalsIgnoreCase ("OPEN"))
    {
      return OpType.OPEN;
    }
    if (opType.equalsIgnoreCase ("SEND"))
    {
      return OpType.SEND;
    }
    if (opType.equalsIgnoreCase ("RECEIVE"))
    {
      return OpType.RECEIVE;
    }
    if (opType.equalsIgnoreCase ("CLOSE"))
    {
      return OpType.CLOSE;
    }
    if (opType.equalsIgnoreCase ("END"))
    {
      return OpType.STOP;
    }
    if (opType.equalsIgnoreCase ("INQUIRE"))
    {
      return OpType.INQUIRE;
    }
    if (opType.equalsIgnoreCase ("SET"))
    {
      return OpType.SET;
    }
    if (opType.equalsIgnoreCase ("CALL"))
    {
      return OpType.CALL;
    }
    if (opType.equalsIgnoreCase ("URL"))
    {
      return OpType.OTHER;
    }
    if (opType.equalsIgnoreCase ("BROWSE"))
    {
      return OpType.BROWSE;
    }
    return OpType.OTHER;
  }

  private static OpType mapOpType (int opType)
  {
    switch (opType)
    {
      case 0:
        return OpType.OTHER;
      case 1:
        return OpType.START;
      case 2:
        return OpType.OPEN;
      case 3:
        return OpType.SEND;
      case 4:
        return OpType.RECEIVE;
      case 5:
        return OpType.CLOSE;
      case 6:
        return OpType.STOP;
      case 7:
        return OpType.INQUIRE;
      case 8:
        return OpType.SET;
      case 9:
        return OpType.CALL;
      case 10://URL
        return OpType.OTHER;
      case 11:
        return OpType.BROWSE;
      default:
        return OpType.OTHER;
    }
  }
}
