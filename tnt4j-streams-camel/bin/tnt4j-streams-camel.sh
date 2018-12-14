#! /bin/bash
if command -v realpath >/dev/null 2>&1; then
    SCRIPTPATH=`dirname $(realpath $0)`
else
    SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
fi

LIBPATH="$SCRIPTPATH/../*:$SCRIPTPATH/../lib/*"
ROUTEOPTS="-Droute.properties=$SCRIPTPATH/../config/route.properties -Droute.context=$SCRIPTPATH/../config/route-context.xml"
LOG4JOPTS="-Dlog4j.configuration=file:$SCRIPTPATH/../config/log4j.properties"
"$JAVA_HOME/bin/java" $LOG4JOPTS $ROUTEOPTS -classpath "$LIBPATH" org.apache.camel.spring.Main $*
