# tnt4j-stream-jmx configuration

To redirect `tnt4j-stream-jmx` (or any other TNT4J based producer) produced trackables, producer configuration file `tnt4j.properties` 
should contain such stanza:
```properties
    event.sink.factory.EventSinkFactory: com.jkoolcloud.tnt4j.sink.impl.SocketEventSinkFactory
    event.sink.factory.EventSinkFactory.eventSinkFactory: com.jkoolcloud.tnt4j.sink.impl.NullEventSinkFactory
    event.sink.factory.EventSinkFactory.Host: IP_OF_STREAMS_RUNNING_MACHINE
    event.sink.factory.EventSinkFactory.Port: 9009
    event.formatter: com.jkoolcloud.tnt4j.format.JSONFormatter
```
NOTE: change `IP_OF_STREAMS_RUNNING_MACHINE` to IP of machine running `TNT4J-Streams` `RedirectTNT4JStream`.