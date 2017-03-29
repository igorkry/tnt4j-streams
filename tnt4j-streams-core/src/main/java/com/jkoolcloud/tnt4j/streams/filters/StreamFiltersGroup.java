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

package com.jkoolcloud.tnt4j.streams.filters;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

/**
 * Stream entities data (activity/field/locator) filters group.
 * <p>
 * Group containing multiple filters defined applies them sequentially and stops processing when founds first value
 * matching filter having handle type {@link HandleType#EXCLUDE}.
 *
 * @version $Revision: 1 $
 */
public class StreamFiltersGroup<T> implements StreamEntityFilter<T> {
	private String name;

	private List<StreamEntityFilter<T>> activityFilters = new ArrayList<>();

	/**
	 * Constructs a new StreamFiltersGroup.
	 *
	 * @param name
	 *            filters group name
	 */
	public StreamFiltersGroup(String name) {
		this.name = name;
	}

	/**
	 * Returns name of filters group
	 *
	 * @return name of filters group
	 */
	public String getName() {
		return name;
	}

	/**
	 * Adds filter to this group.
	 *
	 * @param filter
	 *            filter instance to be added to group
	 */
	public void addFilter(StreamEntityFilter<T> filter) {
		activityFilters.add(filter);
	}

	/**
	 * Removes filter from this group.
	 * 
	 * @param filter
	 *            filter instance to be removed from group
	 */
	public void removeFilter(StreamEntityFilter<T> filter) {
		activityFilters.remove(filter);
	}

	@Override
	public boolean doFilter(T value) throws FilterException {
		if (CollectionUtils.isNotEmpty(activityFilters)) {
			for (StreamEntityFilter<T> aFilter : activityFilters) {
				if (aFilter.doFilter(value)) {
					return true;
				}
			}
		}

		return false;
	}
}
