import logging
import sys
import time, datetime
import traceback

import requests
import re
import json
import random

from apscheduler.executors.pool import ThreadPoolExecutor
from datetime import timezone

from kafka import KafkaProducer
from apscheduler.schedulers.background import BackgroundScheduler

logger = logging.getLogger()
TOPIC_KEY="topic_name"

class Producer:
    """
    Model class for

    - Running event production jobs
    - Publishing results to kafka topic

    The event producing bot is run in a background daemon with threadpool of its own.
    Attributes
    ----------
    config_section : config_section
        dictionary containing necessary properties for connecting to Kafka cluster
    devices : int
        number of devices between 1-5
    event_count : int
        number of events to produce
    interval_seconds : KafkaProducer
        interval between event generation

    Methods
    -------
    """
    def __init__(self, config_section, devices, event_count, interval_seconds):
        self.config_section = config_section
        self.devices = devices
        self.event_count = event_count
        self.interval_seconds = interval_seconds

    def setup_producer(self):
        logger.info(f"kafka conf: {self.config_section}")
        self.kafka_producer = KafkaProducer(
            bootstrap_servers=self.config_section['bootstrap_servers'],
            #value_serializer=lambda v: v.encode('utf-8')
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )

    def send_to_kafka(self, events):
        try:
            for e in events:
                logger.debug(f"Sending to kafka: {e}")
                self.kafka_producer.send(self.config_section[TOPIC_KEY], e)
        except Exception as ex:
            # Using Catch all to ensure continuous monitoring
            # a repeated error could be detected and corresponding job could be paused.
            traceback.print_exc()
            logger.error(f"Exception: {str(ex)}")
        finally:
            # nothing to cleanup
            pass

    def produce_events(self):
        """
        Method for running monitoring jobs periodically, and push results to 'KafkaProducer'
        """
        logger.info("running event production")
        state = ["open", "close"]
        events = []

        for device in range(1,self.devices+1):
            if self.event_count != None:
                if self.event_count < 1:
                    logger.info(f"Count {self.event_count} achieved, exiting program !")
                    self.close_producer()
                    sys.exit()

                self.event_count -= 1

            e = {
                "ts": int(time.time()),
                "deviceId": f"device{device}",
                "temperature": round(random.uniform(-5, 15), 2),
                "door": state[random.randint(0,1)]
            }
            events.append(e)

        logger.debug(f"Event count: {self.event_count}")
        logger.debug(f"Events: {events}")
        self.send_to_kafka(events)
        return events

    def start_producing(self):
        """
        Start a BackgroundScheduler to run monitoring jobs
        """
        logger.info("Starting scheduled job to produce events")
        self.event_service = BackgroundScheduler(daemon=True, executors = {'default': ThreadPoolExecutor(1), })
        self.event_service.add_job(func=self.produce_events, trigger="interval", seconds=self.interval_seconds)
        self.event_service.start()

    def close_producer(self):
        if hasattr(self, "event_service") and self.event_service is not None and self.event_service.running:
            self.event_service.shutdown(wait=True)
        if self.kafka_producer is not None and self.kafka_producer:
            self.kafka_producer.close()


