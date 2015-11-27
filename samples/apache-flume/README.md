# tnt4j-streams
TNT4J Streams allows data streaming, parsing from various data sources into TNT4J event sinks

# Installation of tnt-4j flume plugin

1. Copy tnt-4j-streams-*-all.jar to [flume_path]/lib directory
2. Copy tnt-4j sample config files to [flume_path]/config/:
	my-flume.properties 			(located [tnt-4j-dir]/samples/apache-flume/)
	tnt4.properties  				(located [tnt-4j-dir]/config/)
	log4j.properties [overwrite] 	(located [tnt-4j-dir]/config/)
	tnt-data-source.xml 			(located [tnt-4j-dir]/samples/apache-flume/)
3. Ajust properties to your needs
	a. my-flume.properties 
		change "agent.sources.seqGenSrc.spoolDir  = <LOGS DIR>" to yours log directory
		or sink in your favor.
	b. 	tnt4.properties
		change "event.sink.factory.Token: ##############################" to yours JKoolCloud token
	c.  change tnt-data-source.xml config according yours log format
	
4. Run Flume: [flume_path]/bin/flume-ng agent --conf ./conf/ -f conf/my-flume.properties -n agent
	
	
	
# Q & A

Q: 	My log is not parsed correctly?
A: 	Default sample is for on common apache log format.
	In order to change ir you may need to change <parser> or its <properties>
	Look in documentation for more examples.
	
Q: 	Can I stream to different machine?
	Yes. Change host and/or port where TNT4J-Streams is running.
			
		
	
	
