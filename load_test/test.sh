#!/bin/bash

processes="${1-1}"
dataFileName="${2-data}"

run_client() {
  numbers=$(cat $1 | wc -l)
  start=$(date +%s.%N)
  nc localhost 4000 < "$1"
  end=$(date +%s.%N)

  runtime=$( echo "$end - $start" | bc -l )

  echo "sent $numbers in ${runtime}s"
}

for i in $(seq 1 "$processes"); do
  run_client "$dataFileName$i" &
done
wait