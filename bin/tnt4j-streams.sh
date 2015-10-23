#! /bin/sh
RUNDIR=`pwd`
CLASSPATH="$RUNDIR/../tnt4j-streams.jar:$RUNDIR/../lib/*:$RUNDIR/../lib/tnt4j-log4j12/*"
TNT4JOPTS=-Dtnt4j.config="$RUNDIR/../config/tnt4j.properties"
LOG4JOPTS=-Dlog4j.configuration="file:%RUNDIR%/../config/log4j.properties"
java LOG4JOPTS $TNT4JOPTS -classpath $CLASSPATH com.jkool.tnt4j.streams.StreamsAgent $*