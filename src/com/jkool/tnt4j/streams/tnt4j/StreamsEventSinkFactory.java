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

package com.jkool.tnt4j.streams.tnt4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import com.nastel.jkool.tnt4j.config.ConfigException;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.sink.AbstractEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

public class StreamsEventSinkFactory extends AbstractEventSinkFactory
{
  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (StreamsEventSinkFactory.class);

  private String url;
  private String accessToken;

  public StreamsEventSinkFactory ()
  {
    this (null, null);
  }

  public StreamsEventSinkFactory (String url, String accessToken)
  {
    this.url = url;
    this.accessToken = accessToken;
  }

  @Override
  public EventSink getEventSink (String name)
  {
    return new StreamsEventSink (name, url, accessToken, null);
  }

  @Override
  public EventSink getEventSink (String name, Properties props)
  {
    return getEventSink (name);
  }

  @Override
  public EventSink getEventSink (String name, Properties props, EventFormatter frmt)
  {
    return getEventSink (name, props);
  }

  @Override
  public void setConfiguration (Map<String, Object> props) throws ConfigException
  {
    super.setConfiguration (props);
    url = config.get ("Url") != null ? config.get ("Url").toString () : url;
    accessToken = config.get ("Token") != null ? config.get ("Token").toString () : accessToken;
    try
    {
      URI uri = new URI (url);
      url = uri.toString ();
    }
    catch (URISyntaxException exc)
    {
      ConfigException ce = new ConfigException (exc.toString (), props);
      ce.initCause (exc);
      throw ce;
    }
  }
}
