-------------------------------------------------------------------------------
Author: Steve Koscho - March 15, 2015

Project: Time Synchronization from
         Distributed Systems from
         Professional Masters Program UW CS552

Note: Course and lectures are posted online
      I am doing the coursework on my own accord for no credit

This is a client/server application using sockets and UDP on Linux.

-------------------------------------------------------------------------------
Building Instructions

    These instructions are for Linux
        cd src
        source build.sh

    The output is 2 executables
        ../bin/s
        ../bin/c

    Developed on: Linux Ubuntu version 14.4.1
-------------------------------------------------------------------------------
Running Instructions - localhost

    cd bin
    ./s &
    ./c &

This tests the app on localhost.

    Default IP addr = localhost
    Default Srvr Port = 9990

Both of these programs will redirect stdout and stderr to a logfile with a
unique filename of the form:

    client_xxxxxxxxxxxxxxxx.log
    server_xxxxxxxxxxxxxxxx.log

Where the xxxxxxxxxxxxxxxx is tnow timestamp.
-------------------------------------------------------------------------------

Running Instructions - remote server

    cd bin
    ./c RemoteIP &
    ./s &

    NOTE: No override for port presently
          Will use Srvr Port = 9990

-------------------------------------------------------------------------------
Monitoring the run and collecting the data

NOTE:
- Let the client app continue to run in the background
- cd dogreps and run one of the bash scripts to extract the data

Run this:
    cd dogreps
    source dogreps.sh

OR, you can run dogreps.sh in a loop with doloop.sh to monitor the results
every minute.

After running dogreps.sh, find the extracted data:

    ls dogreps\out\zz_rtt.txt
    ls dogreps\out\zz_theta.txt

Use these data sets to make the histograms and compute statistics from
-------------------------------------------------------------------------------
