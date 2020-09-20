#  Spark Streaming Pipeline

It analyzes real-time rsvp events from meetup WebSocket API (ws://stream.meetup.com/2/rsvps). 

The pipeline finds top K trending (not popular) topics every M minutes from meetup-rsvps streams.

The meetup-rsvps streams are in json format. Firstly a python producer program read the api feeds and publish to kafka broker in json format. Another job written in apache spark streaming
consumes the messages from the kafka_topic and deserialize into java objects. The objects will be filtered by given country and city and then the pipeline finds the total count of each topic (field name: urlkey)
in ascending order in a given M minutes time window. Finally, the first K topics are saved into a file.

## Required Software

* JDK 8+
* Maven
* Docker

Note: The whole program will be running into docker containers and tested into ubuntu host system.

## Build

```
mvn clean package
```

## Start necessary containers

```
$ sh bin/start-containers.sh
```

Note: Please make changes in docker volumes if necessary.

## Run Streaming Job

Open a terminal and submit the jar in spark cluster. Here is the template.
```
docker exec spark-master spark-submit \
  --class <Main class name with package> \
  <jar with dependencies> <kafkaBrokers> <inputTopicName> <windowDurationInMinutes> <numberOfTrendingTopicsPerMinutes> <countryToFilter> <cityToFilterName> <outputFilePath>
```
Note 1: to make cityToFilterName optional please use empty string using double quote "". 
Note 2: outputFilePath: please use directory of docker volume (/home/output/) that was defined during container creation in start-container.sh and use any file name.
Example with optional city filter:
```
docker exec spark-master spark-submit \
  --class org.example.TopKLessTrendingMeetupTopicsFinder\
  /home/target/spark-streaming-pipeline-1.0-SNAPSHOT-jar-with-dependencies.jar kafka-broker:9092 meetup-rsvps-topic 1 10 "us" "" /home/output/out.txt
```
Open another terminal and run the meetup_producer.py

```
docker exec spark-master python3 /home/python/meetup_producer.py
```

if you want to run the spark-submit again with different parameters please kill the existing running process by use the following commands.

```
docker exec spark-master ps -ef | grep java
docker exec spark-master kill -9 <id>
docker exec spark-master ps -ef | grep python3
docker exec spark-master kill -9 <id>
```

## Monitoring Spark Web UI and logs

Spark WebUI: <http://localhost:9999/>
Logs: check the output directory you defined.

```
docker exec spark-master tail /home/output/out.txt -f
```
## Stop containers

```
$ sh bin/start-containers.sh
```
