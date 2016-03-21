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

package com.jkool.tnt4j.streams.configure.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.xml.bind.annotation.*;

/**
 * 
 * Defines TNT4J-Streams data source configuration.
 * 
 * 
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="parser" type="{}ParserType" maxOccurs="unbounded"/>
 *         &lt;element name="stream" type="{}StreamType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "parser", "stream" })
@XmlRootElement(name = "tnt-data-source")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T11:35:05+02:00", comments = "JAXB RI v2.2.4-2")
public class TntDataSource {

	@XmlElement(required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T11:35:05+02:00", comments = "JAXB RI v2.2.4-2")
	protected List<ParserType> parser;
	@XmlElement(required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T11:35:05+02:00", comments = "JAXB RI v2.2.4-2")
	protected List<StreamType> stream;

	/**
	 * Gets the value of the parser property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a
	 * snapshot. Therefore any modification you make to the returned list will
	 * be present inside the JAXB object. This is why there is not a
	 * <CODE>set</CODE> method for the parser property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getParser().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link ParserType }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T11:35:05+02:00", comments = "JAXB RI v2.2.4-2")
	public List<ParserType> getParser() {
		if (parser == null) {
			parser = new ArrayList<ParserType>();
		}
		return this.parser;
	}

	public void addParser(ParserType p) {
		getParser().add(p);
	}

	/**
	 * Gets the value of the stream property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a
	 * snapshot. Therefore any modification you make to the returned list will
	 * be present inside the JAXB object. This is why there is not a
	 * <CODE>set</CODE> method for the stream property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getStream().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link StreamType }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T11:35:05+02:00", comments = "JAXB RI v2.2.4-2")
	public List<StreamType> getStream() {
		if (stream == null) {
			stream = new ArrayList<StreamType>();
		}
		return this.stream;
	}

	public void addStream(StreamType s) {
		getStream().add(s);
	}

}
