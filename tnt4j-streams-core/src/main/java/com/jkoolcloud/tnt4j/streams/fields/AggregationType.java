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

package com.jkoolcloud.tnt4j.streams.fields;

/**
 * List the supported activity entities data aggregation types.
 * 
 * @version $Revision: 1 $
 */
public enum AggregationType {
	/**
	 * Activity entity data shall be merged into parent activity entity.
	 */
	Merge,

	/**
	 * Activity entity shall be added as a child into parent activity entity.
	 */
	Join
}
