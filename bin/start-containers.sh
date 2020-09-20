#!/bin/bash

set -eu

docker network create --driver bridge meetuprsvps-local-network
docker run -d -t --name zookeeper --network=meetuprsvps-local-network \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  wurstmeister/zookeeper

docker run -d -t --name kafka-broker --network=meetuprsvps-local-network \
   -p 9092:9092 \
   -e KAFKA_BROKER_ID=1 \
   -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
   -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka-broker:9092 \
   -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
   wurstmeister/kafka:1.0.0

docker run -d -t --name spark-master --network meetuprsvps-local-network \
  -p 9999:8080 \
  -v "$(pwd)"/target/:/home/target/ \
  -v "$(pwd)"/python/:/home/python/ \
  -v "$(pwd)"/output/:/home/output/ \
  spark-ubuntu:1.0

docker exec spark-master start-master
docker run -d -t --name spark-worker-1 --network meetuprsvps-local-network \
  spark-ubuntu:1.0

docker exec spark-worker-1 start-slave spark://spark-master:7077
