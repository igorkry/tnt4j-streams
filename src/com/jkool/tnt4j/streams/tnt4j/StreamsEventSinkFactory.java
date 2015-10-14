package com.jkool.tnt4j.streams.tnt4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

import com.nastel.jkool.tnt4j.config.ConfigException;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.sink.AbstractEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * @author akausinis
 * @version 1.0
 * @created 2015-10-08 11:20
 */
public class StreamsEventSinkFactory extends AbstractEventSinkFactory
{
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
  public void setConfiguration (Map<String, Object> settings) throws ConfigException
  {
    super.setConfiguration (settings);
    url = config.get ("Url") != null ? config.get ("Url").toString () : url;
    accessToken = config.get ("Token") != null ? config.get ("Token").toString () : accessToken;
    try
    {
      URI uri = new URI (url);
      url = uri.toString ();
    }
    catch (URISyntaxException exc)
    {
      ConfigException ce = new ConfigException (exc.toString (), settings);
      ce.initCause (exc);
      throw ce;
    }
  }
}
