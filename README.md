#  Spark Streaming Pipeline

It analyzes real-time rsvp events from meetup WebSocket API (ws://stream.meetup.com/2/rsvps).

## Pipeline Architecture

## Required Software

* JDK 8+
* Maven
* Docker

## Build

```
mvn clean package
```

## Start necessary containers

```
$ sh bin/start-containers.sh
```

## Run Streaming Job

Open a terminal and submit the jar in spark cluster
```
docker exec spark-master spark-submit \
  --class org.example.MeetupRsvpsAnalizer \
  /home/target/spark-streaming-pipeline-1.0-SNAPSHOT-jar-with-dependencies.jar
```
Open another terminal and run the meetup_producer.py

```
docker exec spark-master python3 /home/python/meetup_producer.py
```

## Monitoring Spark Web UI and logs

Spark WebUI: <http://localhost:9999/>
Logs: check the output directory you defined.

```
tail -f <output_dir>/trending-topics*
```
## Stop containers

```
$ sh bin/start-containers.sh
```
