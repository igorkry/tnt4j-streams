## Running IBM CastIron monitoring stream

### Overview

`CastIronWsStream` pulls orchestration and log data from IBM Cast Iron instance and publishes it as JKoolCloud events.
 
To setup simple IBM Cast Iron orchestration you can see [this video tutorial](https://www.youtube.com/watch?v=ct8BoI8qlnY).

### IBM Cast Iron WebService endpoint features

See [CastIron WebService WSDL](https://www.ibm.com/support/knowledgecenter/en/SSGR73_7.5.1/com.ibm.wci.api.doc/ci00003.html) for more 
information about available operations and other WS related definitions. 

### Configure CastIronWsStream

There is nothing to additionally install in your IBM Cast Iron installation. Just setup `tnt-data-source.xml` and `tnt4j.properties`. 

* Within `tnt4j.properties` set `TOKEN` to one you have bound to your JKool user.

* Within `tnt-data-source.xml` change your's IBM Cast Iron login parameters:                             
```xml
    <sec:username>admin</sec:username>
    <sec:password>slabs123!</sec:password>
```
							
and specify your CI WebService endpoint URL like:   
```xml
    <property name="WsEndpoint" value="https://192.168.3.3/ws"/>
``` 

* To run streaming hit `run.bat`/`run.sh` in `samples/castIron` directory. 

### Configure sampling rate

In CastIron monitoring streams configuration, see scenario steps named `PollLogs`/`PollJobs`. These steps are configured to use simple 
scheduler (`schedule-simple`) having three parameters:
* `interval` - scheduler repetitions interval.
* `units` - time units for scheduler repetitions interval.
* `repeatCount` - count of scheduler repetitions to be done, or `-1` - infinite.

It is also possible to have more advanced scheduler configuration by using cron scheduler (`schedule-cron`). This scheduler is configured 
using cron expressions (see [cron expression generator](https://www.freeformatter.com/cron-expression-generator-quartz.html)). To define 
expression use `expression` parameter. 

**NOTE:** each step requests are performed in synchronized order where every request is performed after previous one has completed.

### Adding additional metrics

To have basic additional metrics it is enough to do those steps: 
* put requests of IBM Cast Iron WebService WSDL defined operations into scenario steps for `CastIronWsStream`.
* add dedicated parsers to parse these requests.

### Streaming to AutoPilot

To stream collected metrics to autopilot instance use `run_autopilot.bat/run_autopilot.sh` run script files. Those files by default uses 
`tnt4j_autopilot.properties` for output sinks configuration. 

To configure host and port properties of your running `AutoPilot` instance change values for:
```properties
    event.sink.factory.EventSinkFactory.Host: 127.0.0.1
	event.sink.factory.EventSinkFactory.Port: 6001
```  
