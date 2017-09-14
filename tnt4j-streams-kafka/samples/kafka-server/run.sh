#! /bin/bash
if command -v realpath >/dev/null 2>&1; then
    SCRIPTPATH=`dirname $(realpath $0)`
else
    SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
fi

ZKOPTS="-Dtnt4j.zookeeper.config=$SCRIPTPATH/../../config/zookeeper.properties"
KAFKAOPTS="-Dtnt4j.kafka.srv.config=$SCRIPTPATH/../../config/kafka-server.properties"
STREAMSOPTS="$ZKOPTS $KAFKAOPTS"
../../bin/tnt4j-streams.sh -f:tnt-data-source.xml