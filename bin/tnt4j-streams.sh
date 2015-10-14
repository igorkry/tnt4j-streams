#! /bin/sh
RUNDIR=`pwd`
CLASSPATH="$RUNDIR/../tnt4j-streams.jar:$RUNDIR/../lib/*:$RUNDIR/../lib/tnt4j-log4j12/*"
TNT4JOPTS=-Dtnt4j.config="$RUNDIR/../config/tnt4j.properties"
java $TNT4JOPTS -classpath $CLASSPATH com.jkool.tnt4j.streams.StreamsAgent $*