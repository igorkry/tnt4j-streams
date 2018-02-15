## Interceptors sample configuration 

### Common

If you are willing to run this sample having Kafka server running locally (default host/port is `localhost:9092`), the only thing you need 
to configure is set your JKool repository token in TNT4J configuration (file `tnt4j.properties`):
```properties
event.sink.factory.Token: YOUR-TOKEN
```
All the rest configuration is for advanced use cases: different kafka server host/port, different metrics reporting topic name, etc.

**NOTE:** check if your `tnt4j.properties` file has stanza:
```properties
source: com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics
```
if there is no such it must be you have an old build of streams or you have not built `tnt4j-streams-kafka` module.      

### Advanced

#### Interceptors binding

To bind interceptors to any producer/consumer alter configuration properties:
* producer: 
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor
```

* consumer:
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor
```
#### Interceptors collected metrics streaming

To configure interceptors collected metrics streaming to dedicated Kafka topic use file `./config/intercept/tnt4j_kafka.properties`

* Define stanza for source `com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics` (without this metrics will not be 
reported to dedicated Kafka topic):
    ```properties
      source: com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics
    ```
* Set TNT4J to use `KafkaSink` to send interceptors collected statistics to dedicated Kafka topic (e.g. `tnt4j_kafka_interceptor_metrics`):
    * referring Kafka producer configuration file  
    ```properties
      #### Kafka event sink factory configuration ####
      event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.kafka.KafkaEventSinkFactory
      event.sink.factory.topic: tnt4j_kafka_interceptor_metrics
      event.sink.factory.propFile: ../config/tnt4j-kafka.properties
      #### Kafka event sink factory configuration end ####
    ```
    * **OR** defining producer properties under sink configuration
    ```properties
      #### Kafka event sink factory configuration ####
      event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.kafka.KafkaEventSinkFactory
      event.sink.factory.topic: tnt4j_kafka_interceptor_metrics
      event.sink.factory.bootstrap.servers: localhost:9092
      event.sink.factory.acks: all
      event.sink.factory.retries: 0
      event.sink.factory.linger.ms: 1
      event.sink.factory.buffer.memory: 33554432
      event.sink.factory.key.serializer: org.apache.kafka.common.serialization.StringSerializer
      event.sink.factory.value.serializer: org.apache.kafka.common.serialization.StringSerializer
      #### Kafka event sink factory configuration end ####
    ``` 

#### Interceptors configuration

To configure interceptors use file `./config/intercept/interceptors.properties`. Configuration properties are:
* `metrics.report.period` - period (in seconds) of Kafka interceptors (and JMX) collected metrics reporting to dedicated Kafka topic.
* `metrics.report.delay` - delay (in seconds) before first metrics reporting is invoked. If not defined, it is equal to 
`metrics.report.period`.  
* `trace.kafka.messages` - flag indicating whether to trace (send to JKool) intercepted Kafka messages. **NOTE:** Kafka message fields and 
JKool events fields mapping is hardcoded for now.   

#### Kafka broker configuration

Default Kafka broker is configured to be on local machine (default host/port is `localhost:9092`). If this does not meet your environment, 
set it to match yours in those files:
* `consumer.properties` file property `bootstrap.servers` 
* `producer.properties` file property `bootstrap.servers`
* `tnt4j.properties` file property `event.sink.factory.bootstrap.servers`
* `tnt-data-source.xml` file Kafka stream property `bootstrap.servers` 

## Running interceptors sample

**NOTE:** before running this sample, ensure your Kafka server instance is running.

### Using run scripts 

1. run `runMetricsStreaming.bat(.sh)` - starts stream reading Kafka topic containing interceptors collected metrics and sending to JKool 
2. run `runInterceptions.bat(.sh)` - starts producer and consumer applications having bound interceptors to collect Kafka metrics and post 
it to Kafka stream listened topic. 

### Using JUnit (for advanced users, like developers)

1. To run interceptions sample run JUnit `InterceptionsManagerTest.interceptionsTest`. It runs sample producer/consumer app with bound 
interceptors. 

    **NOTE:** JUnit test now is configured to use working dir `./tnt4j-streams/tnt4j-streams-kafka`

    InterceptionsManager has bound two InterceptionReporters: 
    * tnt - to collect and post metrics to Kafka topic
    * stream - to stream intercepted data to JKool

2. To stream Kafka topic contained metrics to JKool, run `runMetricsStreaming.bat(.sh)` script.
