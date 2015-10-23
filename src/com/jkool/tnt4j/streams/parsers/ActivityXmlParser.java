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

package com.jkool.tnt4j.streams.parsers;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.*;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * <p>Implements an activity data parser that assumes each activity data item is
 * an XML string, with the value for each field being retrieved from a particular
 * XML element or attribute.</p>
 * <p>This parser supports reading the activity data from several types of input
 * sources, and supports input streams containing multiple XML documents.  If
 * there are multiple XML documents, each document must start with {@code "<?xml ...>"},
 * and be separated by a new line.</p>
 * <p>This parser supports the following properties:
 * <ul>
 * <li>Namespace</li>
 * </ul>
 *
 * @version $Revision: 7 $
 */
public class ActivityXmlParser extends ActivityParser
{
  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (ActivityXmlParser.class);

  /**
   * Contains the field separator (set by {@code SignatureDelim} property) - Default: ","
   */
  protected NamespaceMap namespaces = null;

  private final XPath xPath;
  private final DocumentBuilder builder;
  private final StringBuilder xmlBuffer;
  protected boolean requireAll = false;

  /**
   * Creates a new activity XML string parser.
   *
   * @throws ParserConfigurationException if any errors configuring the parser
   */
  public ActivityXmlParser () throws ParserConfigurationException
  {
    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ();
    domFactory.setNamespaceAware (true);
    builder = domFactory.newDocumentBuilder ();
    XPathFactory xPathFactory = XPathFactory.newInstance ();
    xPath = xPathFactory.newXPath ();
    xmlBuffer = new StringBuilder (1024);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProperties (Collection<Map.Entry<String, String>> props) throws Throwable
  {
    if (props == null)
    {
      return;
    }
    for (Map.Entry<String, String> prop : props)
    {
      String name = prop.getKey ();
      String value = prop.getValue ();
      if (StreamsConfig.PROP_NAMESPACE.equalsIgnoreCase (name))
      {
        if (!StringUtils.isEmpty (value))
        {
          if (namespaces == null)
          {
            namespaces = new NamespaceMap ();
            namespaces.addPrefixUriMapping (XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
          }
          String[] nsFields = value.split ("=");
          namespaces.addPrefixUriMapping (nsFields[0], nsFields[1]);
          LOGGER.log (OpLevel.DEBUG, "Adding {0} mapping {1}", name, value);
        }
      }
      else if (StreamsConfig.PROP_REQUIRE_ALL.equalsIgnoreCase (name))
      {
        if (!StringUtils.isEmpty (value))
        {
          requireAll = Boolean.parseBoolean (value);
          LOGGER.log (OpLevel.DEBUG, "Setting {0} to '{1}'", name, value);
        }
      }
      LOGGER.log (OpLevel.TRACE, "Ignoring property {0}", name);
    }
    if (namespaces != null)
    {
      xPath.setNamespaceContext (namespaces);
    }
  }

  /**
   * {@inheritDoc}
   * <p> This parser supports the following class types
   * (and all classes extending/implementing any of these):</p>
   * <ul>
   * <li>{@code java.lang.String}</li>
   * <li>{@code java.io.Reader}</li>
   * <li>{@code java.io.InputStream}</li>
   * <li>{@code org.w3c.dom.Document}</li>
   * </ul>
   */
  @Override
  public boolean isDataClassSupported (Object data)
  {
    return String.class.isInstance (data) ||
           Reader.class.isInstance (data) ||
           InputStream.class.isInstance (data) ||
           Document.class.isInstance (data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ActivityInfo parse (TNTInputStream stream, Object data) throws IllegalStateException, ParseException
  {
    if (data == null)
    {
      return null;
    }
    LOGGER.log (OpLevel.DEBUG, "Parsing: {0}", data);
    ActivityInfo ai = new ActivityInfo ();
    ActivityField field = null;
    try
    {
      Document xmlDoc;
      if (data instanceof Document)
      {
        xmlDoc = (Document) data;
      }
      else
      {
        String xmlString = getNextXmlString (data);
        if (StringUtils.isEmpty (xmlString))
        {
          return null;
        }
        xmlDoc = builder.parse (IOUtils.toInputStream (xmlString));
      }
      String[] savedFormats = null;
      String[] savedUnits = null;
      String[] savedLocales = null;
      // apply fields for parser
      Object value;
      for (Map.Entry<ActivityField, List<ActivityFieldLocator>> fieldEntry : fieldMap.entrySet ())
      {
        value = null;
        field = fieldEntry.getKey ();
        List<ActivityFieldLocator> locations = fieldEntry.getValue ();
        if (locations != null)
        {
          // need to save format and units specification from config in case individual entry in activity data overrides it
          if (savedFormats == null || savedFormats.length < locations.size ())
          {
            savedFormats = new String[locations.size ()];
            savedUnits = new String[locations.size ()];
          }
          if (locations.size () == 1)
          {
            ActivityFieldLocator loc = locations.get (0);
            savedFormats[0] = loc.getFormat ();
            savedUnits[0] = loc.getUnits ();
            value = getLocatorValue (stream, loc, xmlDoc);
            if (value == null && requireAll && !"false".equalsIgnoreCase (loc.getRequired ()))
            {
              LOGGER.log (OpLevel.TRACE, "Required locator not found: {0}", field);
              return null;
            }
          }
          else
          {
            Object[] values = new Object[locations.size ()];
            for (int li = 0; li < locations.size (); li++)
            {
              ActivityFieldLocator loc = locations.get (li);
              savedFormats[li] = loc.getFormat ();
              savedUnits[li] = loc.getUnits ();
              values[li] = getLocatorValue (stream, loc, xmlDoc);
              if (values[li] == null && requireAll && !"false".equalsIgnoreCase (loc.getRequired ()))
              {
                LOGGER.log (OpLevel.TRACE, "Required locator not found: {0}", field);
                return null;
              }
            }
            value = values;
          }
        }
        applyFieldValue (ai, field, value);
        if (locations != null && savedFormats != null)
        {
          for (int li = 0; li < locations.size (); li++)
          {
            ActivityFieldLocator loc = locations.get (li);
            loc.setFormat (savedFormats[li], null);
            loc.setUnits (savedUnits[li]);
          }
        }
      }
    }
    catch (Exception e)
    {
      ParseException pe = new ParseException ("Failed parsing data for field " + field, 0);
      pe.initCause (e);
      throw pe;
    }
    return ai;
  }

  private Object getLocatorValue (TNTInputStream stream, ActivityFieldLocator locator, Document xmlDoc)
      throws XPathExpressionException, ParseException
  {
    Object val = null;
    if (locator != null)
    {
      String locStr = locator.getLocator ();
      if (!StringUtils.isEmpty (locStr))
      {
        if (locator.getBuiltInType () == ActivityFieldLocatorType.StreamProp)
        {
          val = stream.getProperty (locStr);
        }
        else
        {
          // get value for locator (element)
          XPathExpression expr = xPath.compile (locStr);
          String strVal = (String) expr.evaluate (xmlDoc, XPathConstants.STRING);
          if (!StringUtils.isEmpty (strVal))
          {
            // Get list of attributes and their values for current element
            NodeList attrs = (NodeList) xPath.evaluate (locStr + "/@*", xmlDoc, XPathConstants.NODESET);
            int length = attrs == null ? 0 : attrs.getLength ();
            if (length > 0)
            {
              String format = null;
              String locale = null;
              boolean formatAttrSet = false;
              for (int i = 0; i < length; i++)
              {
                Attr attr = (Attr) attrs.item (i);
                String attrName = attr.getName ();
                String attrValue = attr.getValue ();
                if ("datatype".equals (attrName))
                {
                  locator.setDataType (ActivityFieldDataType.valueOf (attrValue));
                }
                else if ("format".equals (attrName))
                {
                  format = attrValue;
                  formatAttrSet = true;
                }
                else if ("locale".equals (attrName))
                {
                  locale = attrValue;
                }
                else if ("units".equals (attrName))
                {
                  locator.setUnits (attrValue);
                }
              }
              if (formatAttrSet)
              {
                locator.setFormat (format, locale);
              }
            }
            val = strVal.trim ();
          }
        }
      }
      val = locator.formatValue (val);
    }
    return val;
  }

  /**
   * Reads the next complete XML document string from the specified data input source and returns
   * it as a string.  If the data input source contains multiple XML documents, then each document
   * must start with "&lt;?xml", and be separated by a new line.
   *
   * @param data input source for activity data
   *
   * @return XML document string, or {@code null} if end of input source has been reached
   */
  protected String getNextXmlString (Object data)
  {
    if (data == null)
    {
      return null;
    }
    if (data instanceof String)
    {
      return (String) data;
    }
    BufferedReader rdr;
    if (data instanceof BufferedReader)
    {
      rdr = (BufferedReader) data;
    }
    else if (data instanceof Reader)
    {
      rdr = new BufferedReader ((Reader) data);
    }
    else if (data instanceof InputStream)
    {
      rdr = new BufferedReader (new InputStreamReader ((InputStream) data));
    }
    else
    {
      throw new IllegalArgumentException ("data in the format of a " + data.getClass ().getName () + " is not supported");
    }
    String xmlString = null;
    try
    {
      for (String line; xmlString == null && (line = rdr.readLine ()) != null; )
      {
        if (line.startsWith ("<?xml"))
        {
          if (xmlBuffer.length () > 0)
          {
            xmlString = xmlBuffer.toString ();
            xmlBuffer.setLength (0);
          }
        }
        xmlBuffer.append (line);
      }
    }
    catch (EOFException eof)
    {
      LOGGER.log (OpLevel.DEBUG, "Reached end of xml data stream", eof);
    }
    catch (IOException ioe)
    {
      LOGGER.log (OpLevel.WARNING, "Error reading from xml data stream", ioe);
    }
    if (xmlString == null && xmlBuffer.length () > 0)
    {
      xmlString = xmlBuffer.toString ();
      xmlBuffer.setLength (0);
    }
    return xmlString;
  }

  private static class NamespaceMap implements NamespaceContext
  {
    protected final Map<String, String> map = new HashMap<String, String> ();

    private NamespaceMap ()
    {
    }

    public void addPrefixUriMapping (String prefix, String uri)
    {
      map.put (prefix, uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespaceURI (String prefix)
    {
      String uri = map.get (prefix);
      if (uri == null)
      {
        uri = XMLConstants.XML_NS_URI;
      }
      return uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrefix (String namespaceURI)
    {
      for (Map.Entry<String, String> entry : map.entrySet ())
      {
        if (entry.getValue ().equals (namespaceURI))
        {
          return entry.getKey ();
        }
      }
      return XMLConstants.DEFAULT_NS_PREFIX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getPrefixes (String namespaceURI)
    {
      return map.keySet ().iterator ();
    }
  }
}
