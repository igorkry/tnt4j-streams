# Using Logstash with TNT4J-Streams

1. Configure tnt4j.properties and change line
    ```
	"event.sink.factory.Token: ##############################"
	```
	to yours JKoolCloud token

2. Configure supplied sample Logstash configuration:
	* change log file path to yours log path or
	* configure input according your needs

3. Configure your parsing properties for Logstash Grok. <br>
   Supplied sample is suitable for common Apache log.

4. Start TNT4J-Streams agent by running run.bat

5. Copy sample configuration to [logstashdir]/bin/ (or other directory of your choice)

6. Start Logstash with supplied sample Logstash configuration:
    ```
	[logstashdir]/bin/logstash -f ls-tcp.cfg
    ```
5. Enjoy 