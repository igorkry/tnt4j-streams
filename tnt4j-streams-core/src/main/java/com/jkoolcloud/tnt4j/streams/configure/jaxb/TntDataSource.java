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

package com.jkoolcloud.tnt4j.streams.configure.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.xml.bind.annotation.*;

/**
 * Defines TNT4J-Streams data source configuration.
 * 
 * 
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="java-object" type="{}JavaObject" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="parser" type="{}Parser" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="stream" type="{}Stream" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "javaObject", "parser", "stream" })
@XmlRootElement(name = "tnt-data-source")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
public class TntDataSource {

	@XmlElement(name = "java-object")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<JavaObject> javaObject;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Parser> parser;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Stream> stream;

	/**
	 * Gets the value of the javaObject property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the javaObject property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getJavaObject().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link JavaObject }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<JavaObject> getJavaObject() {
		if (javaObject == null) {
			javaObject = new ArrayList<JavaObject>();
		}
		return this.javaObject;
	}

	public void addJavaObject(JavaObject j) {
		getJavaObject().add(j);
	}

	/**
	 * Gets the value of the parser property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the parser property.
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
	 * Objects of the following type(s) are allowed in the list {@link Parser }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Parser> getParser() {
		if (parser == null) {
			parser = new ArrayList<Parser>();
		}
		return this.parser;
	}

	public void addParser(Parser p) {
		getParser().add(p);
	}

	/**
	 * Gets the value of the stream property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the stream property.
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
	 * Objects of the following type(s) are allowed in the list {@link Stream }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T04:53:31+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Stream> getStream() {
		if (stream == null) {
			stream = new ArrayList<Stream>();
		}
		return this.stream;
	}

	public void addStream(Stream s) {
		getStream().add(s);
	}
}
