# tnt4j-streams
TNT4J Streams allows data streaming, parsing from various data sources into TNT4J event sinks

# Installation of tnt4j-streams Apache Flume plugin

1. Copy `tnt4j-streams-core.jar` and `tnt4j-streams-flume-plugin.jar` to [flume_path]/lib directory
2. Copy tnt4j-streams Apache Flume plugin sample config files to [flume_path]/config/:

	* my-flume.properties 			(located [tnt4j-streams-dir]/samples/apache-flume/)
	* tnt4.properties  				(located [tnt4j-streams-dir]/config/)
	* log4j.properties [overwrite/merge] 	(located [tnt4j-streams-dir]/config/)
	* tnt-data-source.xml 			(located [tnt4j-streams-dir]/samples/apache-flume/)

3. Adjust properties to your needs

	* change my-flume.properties:
		```
		"agent.sources.seqGenSrc.spoolDir  = <LOGS DIR>"
		```
	 to yours log directory or sink in your favor.
	* change source interceptor configuration according to your log format
	* change tnt4.properties:
	    ```
		"event.sink.factory.Token: ##############################"
		```
	 to yours JKoolCloud token

	
4. Run Flume:
    ```
    [flume_path]/bin/flume-ng agent --conf ./conf/ -f conf/my-flume.properties -n agent
    ```

	
# Q & A

Q: 	My log is not parsed correctly?

A: 	Default sample is for on common Apache log format.
	In order to change ir you may need to change <parser> or its <properties>
	Look in documentation for more examples.
	
Q: 	Can I stream to different machine?

A: Yes. Change host and/or port where TNT4J-Streams is running.
			
		
	
	
