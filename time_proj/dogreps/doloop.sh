#!/bin/bash

forever=1

while test $forever -eq 1
do
    clear
    source dogreps.sh $fname
    echo Sleep 60 seconds ...
    sleep 60
done

