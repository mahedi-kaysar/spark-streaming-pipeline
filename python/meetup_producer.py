#!/usr/bin/env python3

import os
import websocket
import json
import time
from kafka import KafkaProducer
from kafka.errors import KafkaError

producer = KafkaProducer(
    value_serializer=lambda m: json.dumps(m).encode('utf-8'),
    bootstrap_servers=['kafka-broker:9092'])

def on_message(ws, message):
     m = json.loads(message)
     producer.send('meetup-rsvps-topic', m)
     producer.flush()

def on_error(ws, error):
    print(error)

def on_close(ws):
    print("### closed ###")

if __name__ == "__main__":
    websocket.enableTrace(True)
    ws = websocket.WebSocketApp(
        "ws://stream.meetup.com/2/rsvps",
        on_message=on_message,
        on_error=on_error,
        on_close=on_close)

    ws.run_forever()