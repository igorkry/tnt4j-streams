#! /bin/bash
# JMS_IMPL_LIBPATH=./lib/rabbitMQ/*
# JMS_IMPL_LIBPATH=./lib/solaceLib/*
# JMS_IMPL_LIBPATH=./lib/IBMMQ/*
# JMS_IMPL_LIBPATH=./lib/ActiveMQ/*
export LIBPATH="$LIBPATH:$JMS_IMPL_LIBPATH"
../../bin/tnt4j-streams.sh -f:tnt-data-source.xml