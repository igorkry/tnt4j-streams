#! /bin/bash
RUNDIR=`pwd`
ZKOPTS="-Dtnt4j.zookeeper.config=$RUNDIR/../config/zookeeper.properties"
KAFKAOPTS="-Dtnt4j.kafka.srv.config=$RUNDIR/../config/kafka-server.properties"
STREAMSOPTS="$ZKOPTS $KAFKAOPTS"
../../bin/tnt4j-streams -f:tnt-data-source.xml