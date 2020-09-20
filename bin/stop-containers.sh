#!/bin/bash

set -eu
set -o pipefail

docker rm --force spark-master
docker rm --force spark-worker-1
docker rm --force kafka-broker
docker rm --force zookeeper