/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.filters;

import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.fields.StreamFieldType;
import com.jkool.tnt4j.streams.utils.StreamTimestamp;

/**
 * TODO
 */
public class DefaultActivityFieldsFilter implements StreamFilter {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean doFilterActivity(ActivityInfo activityInfo) {
		StreamFieldType sft = null;

		switch (sft) {
		case ApplName:
			break;
		case Category:
			break;
		case CompCode:
			break;
		case Correlator:
			break;
		case ElapsedTime:
			break;
		case EndTime:
			break;
		case EventName:
			break;
		case EventType:
			break;
		case Exception:
			break;
		case Location:
			break;
		case Message:
			break;
		case MsgCharSet:
			break;
		case MsgEncoding:
			break;
		case MsgLength:
			break;
		case MsgMimeType:
			break;
		case ProcessId:
			break;
		case ReasonCode:
			break;
		case ResourceName:
			break;
		case ServerIp:
			break;
		case ServerName:
			break;
		case Severity:
			break;
		case StartTime:
			break;
		case Tag:
			break;
		case ThreadId:
			break;
		case TrackingId:
			break;
		case UserName:
			break;
		default:
		}

		StreamTimestamp st = activityInfo.getStartTime();
		StreamTimestamp et = activityInfo.getEndTime();
		long elt = activityInfo.getElapsedTime();

		return false;
	}
}
