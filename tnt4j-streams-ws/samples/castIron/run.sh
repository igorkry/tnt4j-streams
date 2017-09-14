#! /bin/bash
if command -v realpath >/dev/null 2>&1; then
    SCRIPTPATH=`dirname $(realpath $0)`
else
    SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
fi

set LIBPATH="$SCRIPTPATH/../../tnt4j-streams-ws-1.1.0-SNAPSHOT.jar"
set STREAMSOPTS="-Dorg.quartz.threadPool.threadCount=1"
../../bin/tnt4j-streams.sh -f:tnt-data-source.xml