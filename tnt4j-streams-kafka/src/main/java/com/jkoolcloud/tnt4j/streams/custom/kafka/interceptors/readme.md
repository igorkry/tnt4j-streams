
To run interceptions sample run JUnit InterceptionsManagerTest.interceptionsTest. It runs sample producer/consumer app with bound interceptors. **NOTE:** JUnit test now is configured to use working dir ./tnt4j-streams/tnt4j-streams-kafka

InterceptionsManager has bound two InterceptionReporters: 
* tnt - to collect and post metrics to Kafka topic
* stream - to stream intercepted data to JKool

To stream Kafka topic contained metrics run "kafka-intercept" sample.

To bind interceptors to any producer/consumer alter configuration properties:
* producer: 
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor
```

* consumer:
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor
```
