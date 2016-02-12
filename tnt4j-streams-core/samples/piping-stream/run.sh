#!/usr/bin/env bash
tail -f orders.log | jk-pipe parsers.xml

read -p "Press [Enter] key to exit..."