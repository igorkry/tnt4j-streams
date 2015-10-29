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

package com.jkool.tnt4j.streams.fields;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.jkool.tnt4j.streams.utils.*;
import com.nastel.jkool.tnt4j.core.*;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.source.DefaultSourceFactory;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.source.SourceType;
import com.nastel.jkool.tnt4j.tracker.Tracker;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;
import org.apache.commons.lang3.StringUtils;

/**
 * This class represents an activity (e.g. event or snapshot) to record with jKool Cloud Service.
 *
 * @version $Revision: 11 $
 */
public class ActivityInfo
{
  public static final String UNSPECIFIED_LABEL = "<UNSPECIFIED>";
  private static final String SNAPSHOT_CATEGORY = "TNT4J-Streams-event-snapshot";

  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (ActivityInfo.class);
  private static final Map<String, String> HOST_CACHE = new ConcurrentHashMap<String, String> ();

  private static final long RETRY_INTVL = 15000L;

  private String serverName = null;      //??
  private String serverIp = null;                     //??
  private String applName = null;      //??
  private String userName = null;

  private String resourceName = null;            //??              op.resourceName

  private String eventName = null;
  private OpType eventType = null;
  private StreamTimestamp startTime = null;
  private StreamTimestamp endTime = null;
  private long elapsedTime = -1L;
  private OpCompCode compCode = null;
  private int reasonCode = 0;
  private String exception = null;
  private OpLevel severity = null;
  private String location = null;
  private String correlator = null;

  private String trackingId = null;
  private String msgTag = null;
  private Object msgData = null;
  private String msgValue = null;
  private String msgCharSet = null;
  private String msgEncoding = null;
  private Integer msgLength = null;
  private String msgMimeType = null;

  private Integer processId = 0;
  private Integer threadId = 0;

  private String snapshotCategory = null;

  /**
   * Constructs an ActivityInfo object.
   */
  public ActivityInfo ()
  {
  }

  /**
   * Applies the given value(s) for the specified field to the appropriate internal data
   * field for reporting field to the jKool Cloud Service.
   *
   * @param field field to apply
   * @param value value to apply for this field, which could be an array of objects
   *              if value for field consists of multiple locations
   *
   * @throws ParseException if an error parsing the specified value based on the field
   *                        definition (e.g. does not match defined format, etc.)
   */
  public void applyField (ActivityField field, Object value) throws ParseException
  {
    LOGGER.log (OpLevel.TRACE, "Applying field {0} from: {1}", field, value);
    List<ActivityFieldLocator> locators = field.getLocators ();
    if (value instanceof Object[])
    {
      Object[] values = (Object[]) value;
      if (values.length == 1)
      {
        value = values[0];
      }
    }
    Object fieldValue;
    if (value instanceof Object[])
    {
      Object[] values = (Object[]) value;
      if (field.isEnumeration ())
      {
        throw new ParseException ("Field " + field + ", multiple locators are not supported for enumeration-based fields", 0);
      }
      if (locators.size () != values.length)
      {
        throw new ParseException ("Failed parsing field: " + field + ", number of values does not match number of locators", 0);
      }
      StringBuilder sb = new StringBuilder ();
      for (int v = 0; v < values.length; v++)
      {
        ActivityFieldLocator locator = locators.get (v);
        String format = locator.getFormat ();
        Object fmtValue = formatValue (field, locator, values[v]);
        if (v > 0)
        {
          sb.append (field.getSeparator ());
        }
        if (fmtValue != null)
        {
          if (fmtValue instanceof StreamTimestamp && !StringUtils.isEmpty (format))
          {
            sb.append (((UsecTimestamp) fmtValue).toString (format));
          }
          else
          {
            sb.append (getStringValue (fmtValue));
          }
        }
      }
      fieldValue = sb.toString ();
    }
    else
    {
      if (locators == null)
      {
        fieldValue = value;
      }
      else
      {
        fieldValue = locators.size () > 1 ? value : formatValue (field, locators.get (0), value);
      }
    }
    if (fieldValue == null)
    {
      LOGGER.log (OpLevel.TRACE, "Field {0} resolves to null value, not applying field", field);
      return;
    }
    LOGGER.log (OpLevel.TRACE, "Applying field {0}, value = {1}", field, fieldValue);
    setFieldValue (field, fieldValue);
  }

  /**
   * Formats the value for the field based on the required internal data type of
   * the field and the definition of the field.
   *
   * @param field   field whose value is to be formatted
   * @param locator locator information for value
   * @param value   raw value of field
   *
   * @return formatted value of field in required internal data type
   */
  protected Object formatValue (ActivityField field, ActivityFieldLocator locator, Object value)
  {
    if (value == null)
    {
      return null;
    }
    if (field.isEnumeration ())
    {
      if (value instanceof String)
      {
        String strValue = (String) value;
        value = StringUtils.containsOnly (strValue, "0123456789") ? Integer.valueOf (strValue) : strValue.toUpperCase ().trim ();
      }
    }
    switch (field.getFieldType ())
    {
      case ElapsedTime:
        try
        {
          // Elapsed time needs to be converted to usec
          ActivityFieldUnitsType units = ActivityFieldUnitsType.valueOf (locator.getUnits ());
          if (!(value instanceof Number))
          {
            value = Long.valueOf (String.valueOf (value));
          }
          value = TimestampFormatter.convert ((Number) value, units, ActivityFieldUnitsType.Microseconds);
        }
        catch (Exception e)
        {
        }
        break;
      case ResourceName:
        value = getStringValue (value);
        break;
      case ServerIp:
        if (value instanceof InetAddress)
        {
          value = ((InetAddress) value).getHostAddress ();
        }
        break;
      case ServerName:
        if (value instanceof InetAddress)
        {
          value = ((InetAddress) value).getHostName ();
        }
        break;
      default:
        break;
    }
    return value;
  }

  /**
   * Sets field to specified value, handling any necessary conversions
   * based on internal data type for field.
   *
   * @param field      field whose value is to be set
   * @param fieldValue formatted value based on locator definition for field
   *
   * @throws ParseException if there are any errors with conversion to internal format
   */
  private void setFieldValue (ActivityField field, Object fieldValue) throws ParseException
  {
    switch (field.getFieldType ())
    {
      case Message:
        msgData = fieldValue;
        break;
      case EventName:
        eventName = getStringValue (fieldValue);
        break;
      case EventType:
        eventType = Utils.mapOpType (fieldValue);
        break;
      case ApplName:
        applName = getStringValue (fieldValue);
        break;
      case Correlator:
        correlator = getStringValue (fieldValue);
        break;
      case ElapsedTime:
        elapsedTime = fieldValue instanceof Number ? ((Number) fieldValue).longValue () : Long.parseLong (getStringValue (fieldValue));
        break;
      case EndTime:
        endTime = fieldValue instanceof StreamTimestamp ? (StreamTimestamp) fieldValue
                                                        : TimestampFormatter.parse (field.getFormat (), fieldValue, null,
                                                                                    field.getLocale ());
        break;
      case Exception:
        exception = getStringValue (fieldValue);
        break;
      case Location:
        location = getStringValue (fieldValue);
        break;
      case ReasonCode:
        reasonCode = getIntValue (fieldValue);
        break;
      case ResourceName:
        resourceName = getStringValue (fieldValue);
        break;
      case ServerIp:
        serverIp = getStringValue (fieldValue);
        break;
      case ServerName:
        serverName = getStringValue (fieldValue);
        break;
      case Severity:
        if (fieldValue instanceof Number)
        {
          severity = OpLevel.valueOf (((Number) fieldValue).intValue ());
        }
        else
        {
          severity = OpLevel.valueOf (fieldValue);
        }
        break;
      case TrackingId:
        trackingId = getStringValue (fieldValue);
        break;
      case StartTime:
        startTime = fieldValue instanceof StreamTimestamp ? (StreamTimestamp) fieldValue
                                                          : TimestampFormatter.parse (field.getFormat (), fieldValue, null,
                                                                                      field.getLocale ());
        break;
      case CompCode:
        if (fieldValue instanceof Number)
        {
          compCode = OpCompCode.valueOf (((Number) fieldValue).intValue ());
        }
        else
        {
          compCode = OpCompCode.valueOf (fieldValue);
        }
        break;
      case Tag:
        msgTag = getStringValue (fieldValue);
        break;
      case UserName:
        userName = getStringValue (fieldValue);
        break;
      case Value:
        msgValue = getStringValue (fieldValue);
        break;
      case MsgCharSet:
        msgCharSet = getStringValue (fieldValue);
        break;
      case MsgEncoding:
        msgEncoding = getStringValue (fieldValue);
        break;
      case MsgLength:
        msgLength = getIntValue (fieldValue);
        break;
      case MsgMimeType:
        msgMimeType = getStringValue (fieldValue);
        break;
      case ProcessID:
        processId = getIntValue (fieldValue);
        break;
      case ThreadID:
        threadId = getIntValue (fieldValue);
        break;
      case Category:
        snapshotCategory = getStringValue (fieldValue);
        break;
      default:
        throw new IllegalArgumentException ("Unrecognized Activity field: " + field);
    }
    LOGGER.log (OpLevel.TRACE, "Set field {0} to '{1}'", field, fieldValue);
  }

  /**
   * Makes fully qualified name of activity source. Name is made from stream parsed data attributes.
   *
   * @return fully qualified name of this activity source, or {@code null} if no source defining attributes where parsed from stream.
   */
  public Source getSource ()
  {

    StringBuilder fqnB = new StringBuilder ();
    if (StringUtils.isNotEmpty (serverName))
    {
      if (fqnB.length () > 0)
      {
        fqnB.append ("#");
      }
      fqnB.append (SourceType.SERVER).append ("=").append (serverName);
    }
    if (StringUtils.isNotEmpty (serverIp))
    {
      if (fqnB.length () > 0)
      {
        fqnB.append ("#");
      }
      fqnB.append (SourceType.NETADDR).append ("=").append (serverIp);
    }
    if (StringUtils.isNotEmpty (applName))
    {
      if (fqnB.length () > 0)
      {
        fqnB.append ("#");
      }
      fqnB.append (SourceType.APPL).append ("=").append (applName);
    }
    if (StringUtils.isNotEmpty (userName))
    {
      if (fqnB.length () > 0)
      {
        fqnB.append ("#");
      }
      fqnB.append (SourceType.USER).append ("=").append (userName);
    }
    String fqn = fqnB.toString ();

    return StringUtils.isEmpty (fqn) ? null : DefaultSourceFactory.getInstance ().newFromFQN (fqn);
  }

  /**
   * Creates the appropriate data message to send to jKool Cloud Service and
   * records the activity using the specified tracker.
   *
   * @param tracker communication gateway to use to record activity
   *
   * @throws Throwable indicates an error building data message or sending data
   *                   to jKool Cloud Service
   */
  public void recordActivity (Tracker tracker) throws Throwable
  {
    if (tracker == null)
    {
      LOGGER.log (OpLevel.WARNING, "Activity destination not specified, activity not being recorded");
      return;
    }
    resolveServer ();
    determineTimes ();
    String trackId = StringUtils.isEmpty (trackingId) ? UUID.randomUUID ().toString () : trackingId;
    String correl = StringUtils.isEmpty (correlator) ? trackId : correlator;
    TrackingEvent event = tracker.newEvent (severity == null ? OpLevel.INFO : severity, eventName, correl, "", (Object[]) null);
    event.setTrackingId (trackId);
    event.setTag (msgTag);
    if (msgData != null)
    {
      if (msgData instanceof byte[])
      {
        byte[] binData = (byte[]) msgData;
        event.setMessage (binData, (Object[]) null);
        event.setSize (msgLength == null ? binData.length : msgLength);
      }
      else
      {
        String strData = String.valueOf (msgData);
        event.setMessage (strData, (Object[]) null);
        event.setSize (msgLength == null ? strData.length () : msgLength);
      }
    }
    if (StringUtils.isNotEmpty (msgMimeType))
    {
      event.setMimeType (msgMimeType);
    }
    if (StringUtils.isNotEmpty (msgEncoding))
    {
      event.setEncoding (msgEncoding);
    }
    if (StringUtils.isNotEmpty (msgCharSet))
    {
      event.setCharset (msgCharSet);
    }

    event.getOperation ().setCompCode (compCode == null ? OpCompCode.SUCCESS : compCode);
    event.getOperation ().setReasonCode (reasonCode);
    event.getOperation ().setType (eventType);
    event.getOperation ().setException (exception);
    event.getOperation ().setLocation (location);
    event.getOperation ().setResource (StringUtils.isEmpty (resourceName) ? UNSPECIFIED_LABEL : resourceName);
    event.getOperation ().setUser (userName);
    event.getOperation ().setTID (threadId == null ? Thread.currentThread ().getId () : threadId);
    event.getOperation ().setPID (processId == null ? Utils.getVMPID () : processId);
    event.getOperation ().setSeverity (severity == null ? OpLevel.INFO : severity);
    if (StringUtils.isNotEmpty (msgValue))
    {
      event.getOperation ().addProperty (new Property ("MsgValue", msgValue));
    }
    event.start (startTime);
    event.stop (endTime, elapsedTime);
    Snapshot snapshot = tracker.newSnapshot (StringUtils.isEmpty (snapshotCategory) ? SNAPSHOT_CATEGORY : snapshotCategory,
                                             event.getOperation ().getName ());
    //snapshot.setParentId (event);
    snapshot.add (Constants.XML_APPL_NAME_LABEL, applName);
    snapshot.add (Constants.XML_SERVER_NAME_LABEL, serverName);
    snapshot.add (Constants.XML_SERVER_IP_LABEL, serverIp);
    snapshot.add (Constants.XML_SERVER_CPU_COUNT_LABEL, 1);
    snapshot.add (Constants.XML_APPL_USER_LABEL, userName);
    snapshot.add (Constants.XML_RESMGR_SERVER_LABEL, serverName);
    snapshot.add (Constants.XML_LUW_SIGNATURE_LABEL, UUID.randomUUID ().toString ());
    snapshot.add (Constants.XML_LUW_TID_LABEL, threadId == null ? Thread.currentThread ().getId () : threadId);
    snapshot.add (Constants.XML_LUW_PID_LABEL, processId == null ? Utils.getVMPID () : processId);
    snapshot.add (Constants.XML_LUW_START_TIME_SEC_LABEL, startTime);
    snapshot.add (Constants.XML_LUW_END_TIME_SEC_LABEL, endTime);
    snapshot.add (Constants.XML_LUW_STATUS_LABEL, compCode == OpCompCode.ERROR ? ActivityStatus.EXCEPTION : ActivityStatus.END);
    snapshot.add (Constants.XML_OP_FUNC_LABEL, eventName);
    snapshot.add (Constants.XML_OP_TYPE_LABEL, eventType == null ? OpType.OTHER : eventType);
    snapshot.add (Constants.XML_OP_USER_NAME_LABEL, userName);
    snapshot.add (Constants.XML_OP_CC_LABEL, compCode == null ? OpCompCode.SUCCESS : compCode);
    snapshot.add (Constants.XML_OP_RC_LABEL, reasonCode);
    snapshot.add (Constants.XML_OP_EXCEPTION_LABEL, exception);
    snapshot.add (Constants.XML_OP_START_TIME_SEC_LABEL, startTime);
    snapshot.add (Constants.XML_OP_END_TIME_SEC_LABEL, endTime);
    snapshot.add (Constants.XML_OP_ELAPSED_TIME_LABEL, elapsedTime);
    snapshot.add (Constants.XML_OP_SEVERITY_LABEL, severity == null ? OpLevel.INFO : severity);
    snapshot.add (Constants.XML_OP_LOCATION_LABEL, location);
    snapshot.add (Constants.XML_OP_CORRELATOR_LABEL, correlator);
    snapshot.add (Constants.XML_OP_RES_NAME_LABEL, StringUtils.isEmpty (resourceName) ? UNSPECIFIED_LABEL : resourceName);
    snapshot.add (Constants.XML_MSG_SIGNATURE_LABEL, trackId);
    snapshot.add (Constants.XML_MSG_TAG_LABEL, msgTag);
    snapshot.add (Constants.XML_MSG_CORRELATOR_LABEL, correlator);
    snapshot.add (Constants.XML_MSG_VALUE_LABEL, msgValue);
    if (msgData != null)
    {
      if (msgData instanceof byte[])
      {
        byte[] binData = (byte[]) msgData;
        snapshot.add (Constants.XML_NAS_MSG_BINDATA_LABEL, binData);
        snapshot.add (Constants.XML_MSG_SIZE_LABEL, msgLength == null ? binData.length : msgLength);
      }
      else
      {
        String strData = String.valueOf (msgData);
        snapshot.add (Constants.XML_NAS_MSG_STRDATA_LABEL, strData);
        snapshot.add (Constants.XML_MSG_SIZE_LABEL, msgLength == null ? strData.length () : msgLength);
      }
    }
    event.getOperation ().addSnapshot (snapshot);
    StreamsThread thread = null;
    if (Thread.currentThread () instanceof StreamsThread)
    {
      thread = (StreamsThread) Thread.currentThread ();
    }
    boolean retryAttempt = false;
    do
    {
      if (event != null)
      {
        try
        {
          tracker.tnt (event);
          if (retryAttempt)
          {
            LOGGER.log (OpLevel.INFO, "Activity recording retry successful");
          }
          return;
        }
        catch (Throwable ioe)
        {
          LOGGER.log (OpLevel.ERROR, "Failed recording activity", ioe);
          tracker.close ();
          if (thread == null)
          {
            throw ioe;
          }
          retryAttempt = true;
          LOGGER.log (OpLevel.INFO, "Will retry recording in {0} seconds", RETRY_INTVL / 1000L);
          StreamsThread.sleep (RETRY_INTVL);
        }
      }
    }
    while (thread != null && !thread.isStopRunning ());
  }

  /**
   * Resolves server name and/or IP Address based on values specified.
   */
  private void resolveServer ()
  {
    if (StringUtils.isEmpty (serverName) && StringUtils.isEmpty (serverIp))
    {
      serverName = Utils.getLocalHostName ();
      serverIp = Utils.getLocalHostAddress ();
    }
    else if (StringUtils.isEmpty (serverName))
    {
      if (StringUtils.isEmpty (serverIp))
      {
        serverName = Utils.getLocalHostName ();
        serverIp = Utils.getLocalHostAddress ();
      }
      else
      {
        try
        {
          serverName = HOST_CACHE.get (serverIp);
          if (StringUtils.isEmpty (serverName))
          {
            serverName = Utils.resolveAddressToHostName (serverIp);
            if (StringUtils.isEmpty (serverName))
            {
              // Add entry so we don't repeatedly attempt to look up unresolvable IP Address
              HOST_CACHE.put (serverIp, "");
            }
            else
            {
              HOST_CACHE.put (serverIp, serverName);
              HOST_CACHE.put (serverName, serverIp);
            }
          }
        }
        catch (Exception e)
        {
          serverName = serverIp;
        }
      }
    }
    else if (StringUtils.isEmpty (serverIp))
    {
      serverIp = HOST_CACHE.get (serverName);
      if (StringUtils.isEmpty (serverIp))
      {
        serverIp = Utils.resolveHostNameToAddress (serverName);
        if (StringUtils.isEmpty (serverIp))
        {
          // Add entry so we don't repeatedly attempt to look up unresolvable host name
          HOST_CACHE.put (serverName, "");
        }
        else
        {
          HOST_CACHE.put (serverIp, serverName);
          HOST_CACHE.put (serverName, serverIp);
        }
      }
    }
    if (StringUtils.isEmpty (serverIp))
    {
      serverIp = " "; // prevents streams API from resolving it to the local IP address
    }
  }

  /**
   * Computes the unspecified operation times and/or elapsed time based
   * on the specified ones.
   */
  private void determineTimes ()
  {
    if (elapsedTime < 0L)
    {
      elapsedTime = 0L;
    }
    if (endTime == null)
    {
      if (startTime != null)
      {
        endTime = new StreamTimestamp (startTime);
        endTime.add (0L, elapsedTime);
      }
      else
      {
        endTime = new StreamTimestamp ();
      }
    }
    if (startTime == null)
    {
      startTime = new StreamTimestamp (endTime);
      startTime.subtract (0L, elapsedTime);
    }
  }

  /**
   * Returns the appropriate string representation for the specified value.
   *
   * @param value value to convert to string representation
   *
   * @return string representation of value
   */
  private static String getStringValue (Object value)
  {
    if (value instanceof byte[])
    {
      return new String ((byte[]) value);
    }
    return String.valueOf (value);
  }

  private static Integer getIntValue (Object value)
  {
    return value instanceof Number ? ((Number) value).intValue () : Integer.parseInt (getStringValue (value));
  }

  public String getServerName ()
  {
    return serverName;
  }

  public String getServerIp ()
  {
    return serverIp;
  }

  public String getApplName ()
  {
    return applName;
  }

  public String getUserName ()
  {
    return userName;
  }

  public String getResourceName ()
  {
    return resourceName;
  }

  public String getEventName ()
  {
    return eventName;
  }

  public OpType getEventType ()
  {
    return eventType;
  }

  public StreamTimestamp getStartTime ()
  {
    return startTime;
  }

  public StreamTimestamp getEndTime ()
  {
    return endTime;
  }

  public long getElapsedTime ()
  {
    return elapsedTime;
  }

  public OpCompCode getCompCode ()
  {
    return compCode;
  }

  public int getReasonCode ()
  {
    return reasonCode;
  }

  public String getException ()
  {
    return exception;
  }

  public OpLevel getSeverity ()
  {
    return severity;
  }

  public String getLocation ()
  {
    return location;
  }

  public String getTrackingId ()
  {
    return trackingId;
  }

  public String getMsgTag ()
  {
    return msgTag;
  }

  public String getCorrelator ()
  {
    return correlator;
  }

  public Object getMsgData ()
  {
    return msgData;
  }

  public String getMsgValue ()
  {
    return msgValue;
  }

  public String getMsgCharSet ()
  {
    return msgCharSet;
  }

  public String getMsgEncoding ()
  {
    return msgEncoding;
  }

  public int getMsgLength ()
  {
    return msgLength;
  }

  public String getMsgMimeType ()
  {
    return msgMimeType;
  }

  public Integer getProcessId ()
  {
    return processId;
  }

  public Integer getThreadId ()
  {
    return threadId;
  }

  public String getSnapshotCategory ()
  {
    return snapshotCategory;
  }
}
