#!/bin/bash

set -eu

docker rm --force spark-master
docker rm --force spark-worker-1
docker rm --force kafka-broker
docker rm --force zookeeper
docker network rm meetuprsvps-local-network
