#!/bin/bash
#
# File: build.sh
#
# To Run it: source build.sh
#
# The Output from script is these 2 files:
#   mv a.out ../bin/s
#   mv a.out ../bin/c
#
# To run these programs:
#   cd to ../bin
#
# Run them there because the monitoring script expects to find the logfiles
# in that folder
#

mkdir -p ../bin

gcc -g server.c applib.c
mv a.out ../bin/s

gcc -g client.c jitter_cache.c applib.c
mv a.out ../bin/c

echo look for ../bin/c and ../bin/s and run them there
