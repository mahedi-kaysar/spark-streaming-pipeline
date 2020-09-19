from kafka import KafkaConsumer

consumer = KafkaConsumer('meetup-rsvps-topic',
                         bootstrap_servers='kafka-broker:9092')


for msg in consumer:
    print(msg)

