#!/bin/bash

set -eu
set -o pipefail

docker exec -d spark-master spark-submit \
  --class org.example.MeetupRsvpsAnalizer \
  /home/target/spark-streaming-pipeline-1.0-SNAPSHOT-jar-with-dependencies.jar