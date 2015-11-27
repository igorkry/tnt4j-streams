# Using losgstash with JKoolCloud Strems

1. Configure tnt4j.properties and change line 
	"event.sink.factory.Token: ##############################" to yours JKoolCloud token 
	
2. Configure yours parsing properties in tnt-data-source.xml
	The suppleid example is suitable for common apache log
	
3. Configure supplied sample logstash configuration:
	change log file path to yours log path or 
	configure input according your needs
	
4. Start JKoolCloud-Streams agent by runnning run.bat

5. Copy sample configuration to [logstashdir]\bin\ (or other directory of your choise)

6. Start logstash with supplied sample logstash configuration:

	[logstashdir]bin/logstash -f ls-tcp.cfg

5. Enjoy 