# Using Logstash with TNT4J-Streams

1. Configure tnt4j.properties and change line 
	"event.sink.factory.Token: ##############################" to yours JKoolCloud token 

2. Configure supplied sample Logstash configuration:
	change log file path to yours log path or
	configure input according your needs

3. Configure yours parsing properties in tnt-data-source.xml
	The supplied example is suitable for common apache log.

	NOTE: parsers can be mapped to activity event data from different sources using tags.
	One parser can be mapped to multiple tags. If no tag is defined for parser then such parser
	is applied to all activities.
	
4. Start JKoolCloud-Streams agent by running run.bat

5. Copy sample configuration to [logstashdir]\bin\ (or other directory of your choice)

6. Start Logstash with supplied sample Logstash configuration:

	[logstashdir]bin/logstash -f ls-tcp.cfg

5. Enjoy 