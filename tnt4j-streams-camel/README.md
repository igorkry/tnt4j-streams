# tnt4j-streams-camel

Camel component for tnt4j-streams offers easy way to send data to [jKoolCloud](https://www.jkoolcloud.com). Camel uses URI's to work with 
many of transport and messaging models such as HTTP, ActiveMQ, JMS and others.

# Model

Apache Camel serves as TNT4J-streams input, TNT4J-streams parses text data to [jKoolCloud](https://www.jkoolcloud.com) event, snapshot or 
activity and send's to [jKoolCloud](https://www.jkoolcloud.com) server.
```
                   Apache Camel
                        |
                        v
           TNT4J-streams-camel component
                        |
                        v
               TNT4J-streams input
                        |
                        v
               TNT4J-streams parser 
                        |
                        v
                  TNTJ4 and JESL
                        |
                        v
                    jKoolCloud
```

## TNT4J URI's

To send data to [jKoolCloud](https://www.jkoolcloud.com) Using Camel simply use:
```
    jkool://pathtoParserXmlFile/tnt-data-source.xml?token=myJKoolCloudToken
```

available options:
token - your [jKoolCloud](https://www.jkoolcloud.com) token
geoAddress - field for geoAddress
dataCenter - field dataCenter
appl - application field
rootFQN - rootFQN field

## Using as OSGi application with Apache service mix

### STEP 1:

Make a project using mavent archetype `camel-archetype-blueprint`

```cmd
 mvn archetype:generate \
  -DarchetypeGroupId=org.apache.camel.archetypes \
  -DarchetypeArtifactId=camel-archetype-blueprint \
  -DarchetypeVersion=2.14.4 \
  -DarchetypeRepository=https://repository.apache.org/content/groups/snapshots-g
```

### STEP 2:

Edit src\main\resources\OSGI-INF\blueprint\blueprint.xml to match you domain needs.

To stream data to jKool use:

```xml
<...>
	<to uri="jkool://g:/workspace\tnt4j-streams/tnt4j-streams-camel/samples/samples/tnt-data-source.xml?token=QZHpqxccKN############sbW1vi1u"/>
<...>
```

Add dependency to `tnt4j-streams-camel`

```xml
	<dependency>
		<groupId>com.jkoolcloud.tnt4j.streams</groupId>
		<artifactId>tnt4j-streams-camel</artifactId>
		<version>1.0.0</version>
	</dependency> 
```

### STEP 3:

Build project 'mvn install'

### STEP 4:

Install [Apache ServiceMix](http://servicemix.apache.org/downloads.html)

Run Apache ServiceMix.

### STEP 5:

Install group 
```cmd
	install -s mvn:groupID/artifactID
```

ServiceMix runs the component by default.
