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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkool.tnt4j.streams.utils.*;
import com.nastel.jkool.tnt4j.core.*;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.source.DefaultSourceFactory;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.source.SourceType;
import com.nastel.jkool.tnt4j.tracker.TimeTracker;
import com.nastel.jkool.tnt4j.tracker.Tracker;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;
import com.nastel.jkool.tnt4j.uuid.UUIDFactory;

/**
 * This class represents an activity (e.g. event or snapshot) to record to jKool
 * Cloud Service.
 *
 * @version $Revision: 11 $
 */
public class ActivityInfo {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityInfo.class);

	/**
	 * The constant to indicate undefined value.
	 */
	protected static final String UNSPECIFIED_LABEL = "<UNSPECIFIED>"; // NON-NLS

	private static final Map<String, String> HOST_CACHE = new ConcurrentHashMap<String, String>();

	private String serverName = null;
	private String serverIp = null;
	private String applName = null;
	private String userName = null;

	private String resourceName = null;

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
	private Collection<String> tag = null;
	private Object message = null;
	private String msgCharSet = null;
	private String msgEncoding = null;
	private Integer msgLength = null;
	private String msgMimeType = null;

	private Integer processId = null;
	private Integer threadId = null;

	private String category = null;

	private boolean filtered = false;

	private static final TimeTracker ACTIVITY_TIME_TRACKER = TimeTracker.newTracker(1000, TimeUnit.HOURS.toMillis(8));

	private Map<String, Object> activityProperties;

	/**
	 * Constructs a new ActivityInfo object.
	 */
	public ActivityInfo() {

	}

	/**
	 * Applies the given value(s) for the specified field to the appropriate
	 * internal data field for reporting field to the jKool Cloud Service.
	 *
	 * @param field
	 *            field to apply
	 * @param value
	 *            value to apply for this field, which could be an array of
	 *            objects if value for field consists of multiple locations
	 *
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field
	 *             definition (e.g. does not match defined format, etc.)
	 */
	public void applyField(ActivityField field, Object value) throws ParseException {
		LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted("ActivityInfo.applying.field", field, value));
		List<ActivityFieldLocator> locators = field.getLocators();
		if (value instanceof Object[]) {
			Object[] values = (Object[]) value;
			if (values.length == 1) {
				value = values[0];
			}
		}
		Object fieldValue;
		if (value instanceof Object[]) {
			Object[] values = (Object[]) value;
			if (field.isEnumeration()) {
				throw new ParseException(StreamsResources.getStringFormatted("ActivityInfo.multiple.locators", field),
						0);
			}
			if (locators.size() != values.length) {
				throw new ParseException(StreamsResources.getStringFormatted("ActivityInfo.failed.parsing", field), 0);
			}
			StringBuilder sb = new StringBuilder();
			for (int v = 0; v < values.length; v++) {
				ActivityFieldLocator locator = locators.get(v);
				String format = locator.getFormat();
				Object fmtValue = formatValue(field, locator, values[v]);
				if (v > 0) {
					sb.append(field.getSeparator());
				}
				if (fmtValue != null) {
					if (fmtValue instanceof UsecTimestamp && !StringUtils.isEmpty(format)) {
						sb.append(((UsecTimestamp) fmtValue).toString(format));
					} else {
						sb.append(getStringValue(fmtValue));
					}
				}
			}
			fieldValue = sb.toString();
		} else {
			if (locators == null) {
				fieldValue = value;
			} else {
				fieldValue = locators.size() > 1 ? value : formatValue(field, locators.get(0), value);
			}
		}
		if (fieldValue == null) {
			LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted("ActivityInfo.field.null", field));
			return;
		}
		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getStringFormatted("ActivityInfo.applying.field.value", field, fieldValue));
		setFieldValue(field, fieldValue);
	}

	/**
	 * Formats the value for the field based on the required internal data type
	 * of the field and the definition of the field.
	 *
	 * @param field
	 *            field whose value is to be formatted
	 * @param locator
	 *            locator information for value
	 * @param value
	 *            raw value of field
	 *
	 * @return formatted value of field in required internal data type
	 */
	protected Object formatValue(ActivityField field, ActivityFieldLocator locator, Object value) {
		if (value == null) {
			return null;
		}
		if (field.isEnumeration()) {
			if (value instanceof String) {
				String strValue = (String) value;
				value = StringUtils.containsOnly(strValue, "0123456789") ? Integer.valueOf(strValue) // NON-NLS
						: strValue.toUpperCase().trim();
			}
		}
		StreamFieldType fieldType = field.getFieldType();
		if (fieldType != null) {
			switch (fieldType) {
			case ElapsedTime:
				try {
					// Elapsed time needs to be converted to usec
					ActivityFieldUnitsType units = ActivityFieldUnitsType.valueOf(locator.getUnits());
					if (!(value instanceof Number)) {
						value = Long.valueOf(getStringValue(value));
					}
					value = TimestampFormatter.convert((Number) value, units, ActivityFieldUnitsType.Microseconds);
				} catch (Exception e) {
				}
				break;
			case ResourceName:
				value = getStringValue(value);
				break;
			case ServerIp:
				if (value instanceof InetAddress) {
					value = ((InetAddress) value).getHostAddress();
				}
				break;
			case ServerName:
				if (value instanceof InetAddress) {
					value = ((InetAddress) value).getHostName();
				}
				break;
			default:
				break;
			}
		}
		return value;
	}

	/**
	 * Sets field to specified value, handling any necessary conversions based
	 * on internal data type for field.
	 *
	 * @param field
	 *            field whose value is to be set
	 * @param fieldValue
	 *            formatted value based on locator definition for field
	 *
	 * @throws ParseException
	 *             if there are any errors with conversion to internal format
	 */
	private void setFieldValue(ActivityField field, Object fieldValue) throws ParseException {
		StreamFieldType fieldType = field.getFieldType();
		if (fieldType != null) {
			switch (fieldType) {
			case Message:
				message = fieldValue;
				break;
			case EventName:
				eventName = getStringValue(fieldValue);
				break;
			case EventType:
				eventType = Utils.mapOpType(fieldValue);
				break;
			case ApplName:
				applName = getStringValue(fieldValue);
				break;
			case Correlator:
				correlator = getStringValue(fieldValue);
				break;
			case ElapsedTime:
				elapsedTime = fieldValue instanceof Number ? ((Number) fieldValue).longValue()
						: Long.parseLong(getStringValue(fieldValue));
				break;
			case EndTime:
				endTime = fieldValue instanceof StreamTimestamp ? (StreamTimestamp) fieldValue
						: TimestampFormatter.parse(field.getFormat(), fieldValue, null, field.getLocale());
				break;
			case Exception:
				exception = getStringValue(fieldValue);
				break;
			case Location:
				location = getStringValue(fieldValue);
				break;
			case ReasonCode:
				reasonCode = getIntValue(fieldValue);
				break;
			case ResourceName:
				resourceName = getStringValue(fieldValue);
				break;
			case ServerIp:
				serverIp = getStringValue(fieldValue);
				break;
			case ServerName:
				serverName = getStringValue(fieldValue);
				break;
			case Severity:
				if (fieldValue instanceof Number) {
					severity = OpLevel.valueOf(((Number) fieldValue).intValue());
				} else {
					severity = OpLevel.valueOf(fieldValue);
				}
				break;
			case TrackingId:
				trackingId = getStringValue(fieldValue);
				break;
			case StartTime:
				startTime = fieldValue instanceof StreamTimestamp ? (StreamTimestamp) fieldValue
						: TimestampFormatter.parse(field.getFormat(), fieldValue, null, field.getLocale());
				break;
			case CompCode:
				if (fieldValue instanceof Number) {
					compCode = OpCompCode.valueOf(((Number) fieldValue).intValue());
				} else {
					compCode = OpCompCode.valueOf(fieldValue);
				}
				break;
			case Tag:
				addTag(Utils.getTags(fieldValue));
				break;
			case UserName:
				userName = getStringValue(fieldValue);
				break;
			case MsgCharSet:
				msgCharSet = getStringValue(fieldValue);
				break;
			case MsgEncoding:
				msgEncoding = getStringValue(fieldValue);
				break;
			case MsgLength:
				msgLength = getIntValue(fieldValue);
				break;
			case MsgMimeType:
				msgMimeType = getStringValue(fieldValue);
				break;
			case ProcessId:
				processId = getIntValue(fieldValue);
				break;
			case ThreadId:
				threadId = getIntValue(fieldValue);
				break;
			case Category:
				category = getStringValue(fieldValue);
				break;
			default:
				throw new IllegalArgumentException(
						StreamsResources.getStringFormatted("ActivityInfo.unrecognized.activity", field));
			}
		} else {
			addActivityProperty(field.getFieldTypeName(), fieldValue);
		}
		LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted("ActivityInfo.set.field", field, fieldValue));
	}

	/**
	 * Adds activity item property to item properties map. Properties from map
	 * are transferred as tracking event properties when {@code recordActivity}
	 * is invoked.
	 *
	 * @param propName
	 *            activity item property key
	 * @param propValue
	 *            activity item property value
	 *
	 * @return previous property value replaced by {@code propValue} or
	 *         {@code null} if there was no such activity property set
	 *
	 * @see Map#put(Object, Object)
	 * @see #recordActivity(Tracker, long)
	 */
	public Object addActivityProperty(String propName, Object propValue) {
		if (activityProperties == null) {
			activityProperties = new HashMap<String, Object>();
		}

		return activityProperties.put(propName, propValue);
	}

	/**
	 * Appends activity item tag collection with provided tag strings array
	 * contents.
	 *
	 * @param tags
	 *            tag strings array
	 */
	public void addTag(String[] tags) {
		if (ArrayUtils.isNotEmpty(tags)) {
			if (this.tag == null) {
				this.tag = new ArrayList<String>();
			}

			Collections.addAll(this.tag, tags);
		}
	}

	/**
	 * Makes fully qualified name of activity source. Name is made from stream
	 * parsed data attributes.
	 *
	 * @return fully qualified name of this activity source, or {@code null} if
	 *         no source defining attributes where parsed from stream.
	 */
	public Source getSource() {
		StringBuilder fqnB = new StringBuilder();

		addSourceValue(fqnB, SourceType.SERVER, serverName);
		addSourceValue(fqnB, SourceType.NETADDR, serverIp);
		addSourceValue(fqnB, SourceType.APPL, applName);
		addSourceValue(fqnB, SourceType.USER, userName);

		String fqn = fqnB.toString();

		return StringUtils.isEmpty(fqn) ? null : DefaultSourceFactory.getInstance().newFromFQN(fqn);
	}

	private static void addSourceValue(StringBuilder sb, SourceType type, String value) {
		if (StringUtils.isNotEmpty(value)) {
			if (sb.length() > 0) {
				sb.append("#"); // NON-NLS
			}
			if (!value.isEmpty()) {
				sb.append(type).append("=").append(value); // NON-NLS
			}
		}
	}

	/**
	 * Creates the appropriate data message to send to jKool Cloud Service and
	 * records the activity using the specified tracker.
	 *
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param retryPeriod
	 *            period in milliseconds between activity resubmission in case
	 *            of failure
	 *
	 * @throws Throwable
	 *             indicates an error building data message or sending data to
	 *             jKool Cloud Service
	 */
	public void recordActivity(Tracker tracker, long retryPeriod) throws Throwable {
		if (tracker == null) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getString("ActivityInfo.tracker.null"));
			return;
		}

		resolveServer();
		determineTimes();
		UUIDFactory uuidFactory = tracker.getConfiguration().getUUIDFactory();
		String evtName = StringUtils.isEmpty(eventName) ? UNSPECIFIED_LABEL : eventName;
		String trackId = StringUtils.isEmpty(trackingId) ? uuidFactory.newUUID() : trackingId;
		String correl = StringUtils.isEmpty(correlator) ? trackId : correlator;
		String resName = StringUtils.isEmpty(resourceName) ? UNSPECIFIED_LABEL : resourceName;
		TrackingEvent event = tracker.newEvent(severity == null ? OpLevel.INFO : severity, evtName, correl, "",
				(Object[]) null);
		event.setTrackingId(trackId);
		if (tag != null) {
			event.setTag(tag);
		}
		if (message != null) {
			if (message instanceof byte[]) {
				byte[] binData = (byte[]) message;
				event.setMessage(binData, (Object[]) null);
				event.setSize(msgLength == null ? binData.length : msgLength);
			} else {
				String strData = String.valueOf(message);
				event.setMessage(strData, (Object[]) null);
				event.setSize(msgLength == null ? strData.length() : msgLength);
			}
		}
		if (StringUtils.isNotEmpty(msgMimeType)) {
			event.setMimeType(msgMimeType);
		}
		if (StringUtils.isNotEmpty(msgEncoding)) {
			event.setEncoding(msgEncoding);
		}
		if (StringUtils.isNotEmpty(msgCharSet)) {
			event.setCharset(msgCharSet);
		}

		event.getOperation().setCompCode(compCode == null ? OpCompCode.SUCCESS : compCode);
		event.getOperation().setReasonCode(reasonCode);
		event.getOperation().setType(eventType);
		event.getOperation().setException(exception);
		event.getOperation().setLocation(location);
		event.getOperation().setResource(resName);
		event.getOperation().setUser(userName);
		event.getOperation().setTID(threadId == null ? Thread.currentThread().getId() : threadId);
		event.getOperation().setPID(processId == null ? Utils.getVMPID() : processId);
		event.getOperation().setSeverity(severity == null ? OpLevel.INFO : severity);
		event.start(startTime);
		event.stop(endTime, elapsedTime);

		addEventProperty(event, ActivityEventProperties.EVENT_PROP_APPL_NAME.getKey(), applName);
		addEventProperty(event, ActivityEventProperties.EVENT_PROP_SERVER_NAME.getKey(), serverName);
		addEventProperty(event, ActivityEventProperties.EVENT_PROP_SERVER_IP.getKey(), serverIp);

		if (activityProperties != null) {
			for (Map.Entry<String, Object> ape : activityProperties.entrySet()) {
				addEventProperty(event, ape.getKey(), ape.getValue());
			}
		}

		StreamsThread thread = null;
		if (Thread.currentThread() instanceof StreamsThread) {
			thread = (StreamsThread) Thread.currentThread();
		}
		boolean retryAttempt = false;
		do {
			if (event != null) {
				try {
					tracker.tnt(event);
					if (retryAttempt) {
						LOGGER.log(OpLevel.INFO, StreamsResources.getString("ActivityInfo.retry.successful"));
					}
					return;
				} catch (Throwable ioe) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString("ActivityInfo.recording.failed"), ioe);
					Utils.close(tracker);
					if (thread == null) {
						throw ioe;
					}
					retryAttempt = true;
					LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("ActivityInfo.will.retry",
							TimeUnit.MILLISECONDS.toSeconds(retryPeriod)));
					StreamsThread.sleep(retryPeriod);
				}
			}
		} while (thread != null && !thread.isStopRunning());
	}

	private static void addEventProperty(TrackingEvent event, String key, Object value) {
		if (event != null && value != null) {
			event.getOperation().addProperty(new Property(key, value));
		}
	}

	/**
	 * Resolves server name and/or IP Address based on values specified.
	 */
	private void resolveServer() {
		if (StringUtils.isEmpty(serverName) && StringUtils.isEmpty(serverIp)) {
			serverName = Utils.getLocalHostName();
			serverIp = Utils.getLocalHostAddress();
		} else if (StringUtils.isEmpty(serverName)) {
			if (StringUtils.isEmpty(serverIp)) {
				serverName = Utils.getLocalHostName();
				serverIp = Utils.getLocalHostAddress();
			} else {
				try {
					serverName = HOST_CACHE.get(serverIp);
					if (StringUtils.isEmpty(serverName)) {
						serverName = Utils.resolveAddressToHostName(serverIp);
						if (StringUtils.isEmpty(serverName)) {
							// Add entry so we don't repeatedly attempt to look
							// up unresolvable IP Address
							HOST_CACHE.put(serverIp, "");
						} else {
							HOST_CACHE.put(serverIp, serverName);
							HOST_CACHE.put(serverName, serverIp);
						}
					}
				} catch (Exception e) {
					serverName = serverIp;
				}
			}
		} else if (StringUtils.isEmpty(serverIp)) {
			serverIp = HOST_CACHE.get(serverName);
			if (StringUtils.isEmpty(serverIp)) {
				serverIp = Utils.resolveHostNameToAddress(serverName);
				if (StringUtils.isEmpty(serverIp)) {
					// Add entry so we don't repeatedly attempt to look up
					// unresolvable host name
					HOST_CACHE.put(serverName, "");
				} else {
					HOST_CACHE.put(serverIp, serverName);
					HOST_CACHE.put(serverName, serverIp);
				}
			}
		}
		if (StringUtils.isEmpty(serverIp)) {
			serverIp = " "; // prevents streams API from resolving it to the
			// local IP address
		}
	}

	/**
	 * Computes the unspecified operation times and/or elapsed time based on the
	 * specified ones.
	 */
	private void determineTimes() {
		if (elapsedTime < 0L) {
			elapsedTime = StringUtils.isEmpty(resourceName) ? TimeTracker.hitAndGet()
					: ACTIVITY_TIME_TRACKER.hitAndGet(resourceName);
		}
		if (endTime == null) {
			if (startTime != null) {
				endTime = new StreamTimestamp(startTime);
				endTime.add(0L, elapsedTime);
			} else {
				endTime = new StreamTimestamp();
			}
		}
		if (startTime == null) {
			startTime = new StreamTimestamp(endTime);
			startTime.subtract(0L, elapsedTime);
		}
	}

	/**
	 * Returns the appropriate string representation for the specified value.
	 *
	 * @param value
	 *            value to convert to string representation
	 *
	 * @return string representation of value
	 */
	private static String getStringValue(Object value) {
		if (value instanceof byte[]) {
			return Utils.getString((byte[]) value);
		}
		return String.valueOf(value);
	}

	private static Integer getIntValue(Object value) {
		return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(getStringValue(value));
	}

	/**
	 * Merges activity info data fields values. Values of fields are changed
	 * only if they currently hold default (initial) value.
	 *
	 * @param otherAi
	 *            activity info object to merge into this one
	 */
	public void merge(ActivityInfo otherAi) {
		if (StringUtils.isEmpty(serverName)) {
			serverName = otherAi.serverName;
		}
		if (StringUtils.isEmpty(serverIp)) {
			serverIp = otherAi.serverIp;
		}
		if (StringUtils.isEmpty(applName)) {
			applName = otherAi.applName;
		}
		if (StringUtils.isEmpty(userName)) {
			userName = otherAi.userName;
		}

		if (StringUtils.isEmpty(resourceName)) {
			resourceName = otherAi.resourceName;
		}

		if (StringUtils.isEmpty(eventName)) {
			eventName = otherAi.eventName;
		}
		if (eventType == null) {
			eventType = otherAi.eventType;
		}
		if (startTime == null) {
			startTime = otherAi.startTime;
		}
		if (endTime == null) {
			endTime = otherAi.endTime;
		}
		if (elapsedTime == -1L) {
			elapsedTime = otherAi.elapsedTime;
		}
		if (compCode == null) {
			compCode = otherAi.compCode;
		}
		if (reasonCode == 0) {
			reasonCode = otherAi.reasonCode;
		}
		if (StringUtils.isEmpty(exception)) {
			exception = otherAi.exception;
		}
		if (severity == null) {
			severity = otherAi.severity;
		}
		if (StringUtils.isEmpty(location)) {
			location = otherAi.location;
		}
		if (StringUtils.isEmpty(correlator)) {
			correlator = otherAi.correlator;
		}

		if (StringUtils.isEmpty(trackingId)) {
			trackingId = otherAi.trackingId;
		}
		if (otherAi.tag != null) {
			if (tag == null) {
				tag = new ArrayList<String>();
			}

			tag.addAll(otherAi.tag);
		}
		if (message == null) {
			message = otherAi.message;
		}
		if (StringUtils.isEmpty(msgCharSet)) {
			msgCharSet = otherAi.msgCharSet;
		}
		if (StringUtils.isEmpty(msgEncoding)) {
			msgEncoding = otherAi.msgEncoding;
		}
		if (msgLength == null) {
			msgLength = otherAi.msgLength;
		}
		if (StringUtils.isEmpty(msgMimeType)) {
			msgMimeType = otherAi.msgMimeType;
		}

		if (processId == null) {
			processId = otherAi.processId;
		}
		if (threadId == null) {
			threadId = otherAi.threadId;
		}

		if (StringUtils.isEmpty(category)) {
			category = otherAi.category;
		}

		filtered |= otherAi.filtered;

		if (otherAi.activityProperties != null) {
			if (activityProperties == null) {
				activityProperties = new HashMap<String, Object>();
			}

			activityProperties.putAll(otherAi.activityProperties);
		}
	}

	/**
	 * Gets server name.
	 *
	 * @return the server name
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * Gets server ip.
	 *
	 * @return the server ip
	 */
	public String getServerIp() {
		return serverIp;
	}

	/**
	 * Gets application name.
	 *
	 * @return the application name
	 */
	public String getApplName() {
		return applName;
	}

	/**
	 * Gets user name.
	 *
	 * @return the user name
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Gets resource name.
	 *
	 * @return the resource name
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * Gets event name.
	 *
	 * @return the event name
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * Gets event type.
	 *
	 * @return the event type
	 */
	public OpType getEventType() {
		return eventType;
	}

	/**
	 * Gets start time.
	 *
	 * @return the start time
	 */
	public StreamTimestamp getStartTime() {
		return startTime;
	}

	/**
	 * Gets end time.
	 *
	 * @return the end time
	 */
	public StreamTimestamp getEndTime() {
		return endTime;
	}

	/**
	 * Gets elapsed time.
	 *
	 * @return the elapsed time
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Gets activity completion code.
	 *
	 * @return the activity completion code
	 */
	public OpCompCode getCompCode() {
		return compCode;
	}

	/**
	 * Gets reason code.
	 *
	 * @return the reason code
	 */
	public int getReasonCode() {
		return reasonCode;
	}

	/**
	 * Gets exception/error message.
	 *
	 * @return the exception/error message
	 */
	public String getException() {
		return exception;
	}

	/**
	 * Gets severity.
	 *
	 * @return the severity
	 */
	public OpLevel getSeverity() {
		return severity;
	}

	/**
	 * Gets location.
	 *
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Gets tracking identifier.
	 *
	 * @return the tracking identifier
	 */
	public String getTrackingId() {
		return trackingId;
	}

	/**
	 * Gets activity tag strings collection.
	 *
	 * @return the activity tag strings collection
	 */
	public Collection<String> getTag() {
		return tag;
	}

	/**
	 * Gets activity correlator.
	 *
	 * @return the activity correlator
	 */
	public String getCorrelator() {
		return correlator;
	}

	/**
	 * Gets activity message data.
	 *
	 * @return the activity message data
	 */
	public Object getMessage() {
		return message;
	}

	/**
	 * Gets message char set.
	 *
	 * @return the message char set
	 */
	public String getMsgCharSet() {
		return msgCharSet;
	}

	/**
	 * Gets message encoding.
	 *
	 * @return the message encoding
	 */
	public String getMsgEncoding() {
		return msgEncoding;
	}

	/**
	 * Gets message length.
	 *
	 * @return the message length
	 */
	public int getMsgLength() {
		return msgLength;
	}

	/**
	 * Gets message MIME type.
	 *
	 * @return the message MIME type
	 */
	public String getMsgMimeType() {
		return msgMimeType;
	}

	/**
	 * Gets process identifier.
	 *
	 * @return the process identifier
	 */
	public Integer getProcessId() {
		return processId;
	}

	/**
	 * Gets thread identifier.
	 *
	 * @return the thread identifier
	 */
	public Integer getThreadId() {
		return threadId;
	}

	/**
	 * Gets activity category (i.e. snapshot category).
	 *
	 * @return the activity category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Returns activity filtering flag value.
	 *
	 * @return activity filtering flag value
	 */
	public boolean isFiltered() {
		return filtered;
	}

	/**
	 * Sets activity filtering flag value.
	 *
	 * @param filtered
	 *            {@code true} if activity is filtered out, {@code false}
	 *            otherwise
	 */
	public void setFiltered(boolean filtered) {
		this.filtered = filtered;
	}
}
