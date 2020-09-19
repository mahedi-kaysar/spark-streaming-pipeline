#!/usr/bin/env python3
import os
import websocket
import json
import time
from kafka import KafkaProducer
from kafka.errors import KafkaError


def on_message(ws, message):
     m = json.loads(message)
     print(message)
     producer.send('meetup-rsvps-topic', m)
     producer.flush()


def on_error(ws, error):
    print(error)


def on_close(ws):
    print("### closed ###")


#producer = KafkaProducer(bootstrap_servers=['localhost:9092'])

producer = KafkaProducer(value_serializer=lambda m: json.dumps(m).encode('utf-8'),
                         bootstrap_servers=['kafka-broker:9092'])
websocket.enableTrace(True)

while(1):
    ws = websocket.WebSocketApp("ws://stream.meetup.com/2/rsvps",
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close)
    ws.run_forever()
    print("Connection closed, retrying in 10 sec ...")
    time.sleep(10)
