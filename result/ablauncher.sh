#!/bin/sh
rm *_*.txt
for i in 10 20 30 50 75 100 125 150 175 200 225 250 275 300 325 350
do
  ab -c $i -B localhost -n 20000 -H 'Content-type: application/json' -k http://localhost:8080/chitchat/latest/Iron%%20Man >$i'_latest.txt' 
  ab -c $i -B localhost -n 20000 -H 'Content-type: application/json' -k http://localhost:8080/chitchat/thread/3 >$i'_thread.txt'
  ab -c $i -B localhost -n 20000 -H 'Content-type: application/json' -k http://localhost:8080/chitchat/search?q=hulk >$i'_search.txt'
done
