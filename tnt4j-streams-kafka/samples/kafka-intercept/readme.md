
To run interceptions sample run JUnit `InterceptionsManagerTest.interceptionsTest`. It runs sample producer/consumer app with bound 
interceptors. 

**NOTE:** JUnit test now is configured to use working dir `./tnt4j-streams/tnt4j-streams-kafka`

InterceptionsManager has bound two InterceptionReporters: 
* tnt - to collect and post metrics to Kafka topic
* stream - to stream intercepted data to JKool

To stream Kafka topic contained metrics run "kafka-intercept" sample.

## Interceptors configuration 

### Interceptors binding

To bind interceptors to any producer/consumer alter configuration properties:
* producer: 
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor
```

* consumer:
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor
```
### Interceptors collected metrics streaming

To configure interceptors use file `./config/intercept/tnt4j_kafka.properties`

Set TNT4J to use `KafkaSink` to send interceptors collected statistics to dedicated Kafka topic (e.g. `tnt4j_kafka_interceptor_metrics`):

```properties
  #### Kafka event sink factory configuration ####
  event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.kafka.KafkaEventSinkFactory
  event.sink.factory.propFile: ../config/tnt4j-kafka.properties
  event.sink.factory.topic: tnt4j_kafka_interceptor_metrics
  #### Kafka event sink factory configuration end ####
```

### Interceptors configuration

To configure interceptors use file `./config/intercept/interceptors.properties`

No it allows to enable/disable Kafka messages tracing - sending intercepted Kafka messages to JKool. **NOTE:** Fields mapping is hardcoded now.   

## Running interceptors sample

1. run `runMetricsStreaming.bat(.sh)` - starts stream reading Kafka topic containing interceptors collected metrics and sending to JKool 
2. run `runInterceptions.bat(.sh)` - starts producer and consumer applications having bound interceptors 