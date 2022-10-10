#!/bin/bash

numbers="${1-10000000}"
fileName="${2-data}"
numFiles="${3-5}"

for i in $(seq 1 "$numFiles"); do
  rm -f "$fileName$i"
  touch "$fileName$i"
  echo "Generating file $fileName$i"
  awk -v seed="$RANDOM" -v var="$numbers" 'BEGIN {
    srand(seed);
    for (i = 0; i < var; i++) {
      printf("%09d\n", int(rand() * 1000000000))
    }
  }' >> "$fileName$i"
  echo "-1" >> "$fileName$i"
done
