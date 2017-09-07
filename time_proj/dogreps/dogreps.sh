#!/bin/bash

echo "-------------------------------------------------------------------------"
echo "PROCESSING Client Log File"

mkdir -p _out

echo "Select an input logfile"

select fname in ../bin/client_*.log;
do
    echo you picked $fname \($REPLY\)
    break;
done

grep  _send  $fname > _out/_send.log
grep  _recv  $fname > _out/_recv.log
grep  _data  $fname > _out/_data.log
grep  _lost  $fname > _out/_noreply.log

echo ""
echo "using wc -l to count:"
echo "      - How many requests sent"
echo "      - How many replies never came back"
echo ""
echo "VERIFY: Make sure these add up"
echo "        num_no_reply + num_data ==> num_send"
echo "        num_send ==> num_send"
echo "        num_recv ==> num_send - num_no_reply"
wc -l _out/*.log

echo ""
echo "Extracting RTT and Theta samples from the log"
cut -f 2 _out/_data.log > _out/zz_rtt.txt
cut -f 3 _out/_data.log > _out/zz_theta.txt

echo ""
echo "note: cd _out and look for these files"
ls _out

echo ""
echo "name              rtt             theta       smoothed_theta"
tail -n 20 _out/_data.log
