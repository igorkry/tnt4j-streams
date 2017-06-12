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
 *
 * Defines a activity elements RAW data input stream.
 *
 *
 * <p>
 * Java class for Stream complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Stream">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="property" type="{}StreamProperty" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="parser-ref" type="{}Reference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="reference" type="{}Reference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="tnt4j-properties" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="property" type="{}Property" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="cache" type="{}Cache" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{}EntityAttributeGroup"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "property", "parserRef", "reference", "tnt4JProperties", "cache" })
@XmlSeeAlso({ Stream.class })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
public class OriginalStream {

	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<StreamProperty> property;
	@XmlElement(name = "parser-ref")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Reference> parserRef;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Reference> reference;
	@XmlElement(name = "tnt4j-properties")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	protected OriginalStream.Tnt4JProperties tnt4JProperties;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	protected List<Cache> cache;
	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "class", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	protected String clazz;

	public OriginalStream() {
	}

	public OriginalStream(String name, String clazz) {
		this.name = name;
		this.clazz = clazz;
	}

	/**
	 * Gets the value of the property property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the property property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getProperty().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link StreamProperty }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public List<StreamProperty> getProperty() {
		if (property == null) {
			property = new ArrayList<StreamProperty>();
		}
		return this.property;
	}

	public void addProperty(StreamProperty p) {
		getProperty().add(p);
	}

	public void addProperty(String name, String value) {
		getProperty().add(new StreamProperty(name, value));
	}

	/**
	 * Gets the value of the parserRef property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the parserRef property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getParserRef().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Reference }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Reference> getParserRef() {
		if (parserRef == null) {
			parserRef = new ArrayList<Reference>();
		}
		return this.parserRef;
	}

	public void addParserRef(Reference pr) {
		getParserRef().add(pr);
	}

	public void addParserRef(String name) {
		getParserRef().add(new Reference(name));
	}

	/**
	 * Gets the value of the reference property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the reference property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getReference().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Reference }
	 *
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Reference> getReference() {
		if (reference == null) {
			reference = new ArrayList<Reference>();
		}
		return this.reference;
	}

	public void addReference(Reference pr) {
		getReference().add(pr);
	}

	public void addReference(String name) {
		getReference().add(new Reference(name));
	}

	/**
	 * Gets the value of the tnt4JProperties property.
	 * 
	 * @return possible object is {@link OriginalStream.Tnt4JProperties }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public OriginalStream.Tnt4JProperties getTnt4JProperties() {
		return tnt4JProperties;
	}

	/**
	 * Sets the value of the tnt4JProperties property.
	 * 
	 * @param value
	 *            allowed object is {@link OriginalStream.Tnt4JProperties }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public void setTnt4JProperties(OriginalStream.Tnt4JProperties value) {
		this.tnt4JProperties = value;
	}

	/**
	 * Gets the value of the cache property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the cache property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getCache().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Cache }
	 *
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public List<Cache> getCache() {
		if (cache == null) {
			cache = new ArrayList<Cache>();
		}
		return this.cache;
	}

	public void addCache(Cache c) {
		getCache().add(c);
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public String getName() {
		return name;
	}

	/**
	 * Sets the value of the name property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * Gets the value of the clazz property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public String getClazz() {
		return clazz;
	}

	/**
	 * Sets the value of the clazz property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public void setClazz(String value) {
		this.clazz = value;
	}

	/**
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
	 *         &lt;element name="property" type="{}Property" maxOccurs="unbounded"/>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 * 
	 * 
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = { "property" })
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
	public static class Tnt4JProperties {

		@XmlElement(required = true)
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
		protected List<Property> property;

		/**
		 * Gets the value of the property property.
		 * 
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you
		 * make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE>
		 * method for the property property.
		 * 
		 * <p>
		 * For example, to add a new item, do as follows:
		 * 
		 * <pre>
		 * getProperty().add(newItem);
		 * </pre>
		 * 
		 * 
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link Property }
		 * 
		 * 
		 */
		@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T06:14:43+03:00", comments = "JAXB RI v2.2.4-2")
		public List<Property> getProperty() {
			if (property == null) {
				property = new ArrayList<Property>();
			}
			return this.property;
		}

		public void addProperty(Property p) {
			getProperty().add(p);
		}

		public void addProperty(String name, String value) {
			getProperty().add(new Property(name, value));
		}

	}

}
