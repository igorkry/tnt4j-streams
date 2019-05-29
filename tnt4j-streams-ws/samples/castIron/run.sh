#! /bin/bash
export STREAMSOPTS="-Dorg.quartz.threadPool.threadCount=1"
# sourcing instead of executing to pass variables
. ../../bin/tnt4j-streams.sh -f:tnt-data-source.xml