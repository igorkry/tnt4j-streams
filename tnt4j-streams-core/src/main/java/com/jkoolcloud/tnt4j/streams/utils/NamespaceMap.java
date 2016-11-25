/*
 * Copyright 2014-2016 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Implements {@link NamespaceContext}, where every namespace prefix and URI relation is resolved from internally stored
 * {@link Map}.
 *
 * @version $Revision: 1 $
 */
public final class NamespaceMap implements NamespaceContext {
	private final Map<String, String> map = new HashMap<String, String>();

	/**
	 * Constructs a new namespaces map.
	 */
	public NamespaceMap() {
	}

	/**
	 * Adds mapping of namespace prefix to namespace URI.
	 *
	 * @param prefix
	 *            prefix to put into mapping
	 * @param uri
	 *            uri to put into mapping
	 */
	public void addPrefixUriMapping(String prefix, String uri) {
		map.put(prefix, uri);
	}

	@Override
	public String getNamespaceURI(String prefix) {
		String uri = map.get(prefix);
		if (uri == null) {
			uri = XMLConstants.XML_NS_URI;
		}
		return uri;
	}

	@Override
	public String getPrefix(String namespaceURI) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			if (Utils.equal(entry.getValue(), namespaceURI)) {
				return entry.getKey();
			}
		}
		return XMLConstants.DEFAULT_NS_PREFIX;
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		return map.keySet().iterator();
	}
}
