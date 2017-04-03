#! /bin/bash
SCRIPTPATH=`realpath $0`
RUNDIR=`dirname $SCRIPTPATH`
ZKOPTS="-Dtnt4j.zookeeper.config=$RUNDIR/../../config/zookeeper.properties"
KAFKAOPTS="-Dtnt4j.kafka.srv.config=$RUNDIR/../../config/kafka-server.properties"
STREAMSOPTS="$ZKOPTS $KAFKAOPTS"
../../bin/tnt4j-streams.sh -f:tnt-data-source.xml