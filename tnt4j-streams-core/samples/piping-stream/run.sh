#!/usr/bin/env bash
tail -f orders.log | jk-pipe.sh parsers.xml -s

read -p "Press [Enter] key to exit..."