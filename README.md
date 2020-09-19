mvn clean package
./bin/spark-submit --class org.example.MeetupRsvpsAnalizer /home/mkaysar/projects/StreamingPipeline/target/StreamingPipeline-1.0-SNAPSHOT-jar-with-dependencies.jar



docker build -t spark-ubuntu:1.0 .

docker run -d -t --name master --network spark-network -p 9999:8080 spark-ubuntu:1.0
docker exec master start-master
docker run -d -t --name worker-1 --network spark-network spark-ubuntu:1.0
docker exec worker-1 start-slave spark://master:7077
docker run -d -t --name zookeeper-server --network spark-network -p 2181:2181 -e ALLOW_ANONYMOUS_LOGIN=yes wurstmeister/zookeeper
docker run -d -t --name kafka-server --network spark-network \
  -e ALLOW_PLAINTEXT_LISTENER=yes -e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper-server:2181 -e KAFKA_ADVERTISED_HOST_NAME=localhost,127.0.0.1 \
  -p 7203:7203 -p 9092:9092  wurstmeister/kafka:1.0.0
  
docker run -d -t --name kafka-server --network spark-network \
-e KAFKA_BROKER_ID=1 \
-e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka-server:9092,CONNECTIONS_FROM_HOST://localhost:19092 \
-e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONNECTIONS_FROM_HOST:PLAINTEXT \
-e KAFKA_ZOOKEEPER_CONNECT=zookeeper-server:2181 \
-e ALLOW_PLAINTEXT_LISTENER=yes \
-e KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true \
-p 7203:7203 -p 9092:9092 -p 19092:19092 \
wurstmeister/kafka:1.0.0


docker run --network=spark-network --rm --detach --name kafka-broker \
           -p 9092:9092 \
           -e KAFKA_BROKER_ID=1 \
           -e KAFKA_ZOOKEEPER_CONNECT=zookeeper-server:2181 \
           -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka-broker:9092 \
           -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
           -e KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true \
           wurstmeister/kafka:1.0.0


docker exec master spark-submit --class org.example.MeetupRsvpsAnalizer /home/StreamingPipeline-1.0-SNAPSHOT-jar-with-dependencies.jar



