#!/bin/bash
#
# In a sepeate window, start the Flask REST API:
#   python main.py
#
# On the localhost, this script will hit the end-points.
# Watch the screen on the SenseHat LED display for the output
#
counter=0
while [ $counter -le 15 ]
do
    echo $counter
    ((counter++))
    speed=$(($counter * 10))
    curl "http://localhost:5000/temp/{$counter}"
    curl "http://localhost:5000/fan/speed/{$speed}"
    curl http://localhost:5000/fan/on
    sleep 4
    curl http://localhost:5000/fan/off
    sleep 4
done
echo All done

