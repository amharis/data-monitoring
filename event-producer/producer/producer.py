import logging
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

    - Running monitoring jobs
    - Publishing results to kafka topic

    The monitoring job is run in a background daemon with threadpool of its own.
    Attributes
    ----------
    config_section : config_section
        dictionary containing necessary properties for connecting to Kafka cluster
    monitoring_jobs : list[Job]
        list of Job objects, to be periodically monitored
    duration_seconds : int
        interval between monitoring runs (in secs)
    kafka_producer : KafkaProducer
        client for writing messages to kafka

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

    # An alternative here is to use the outbox pattern.
    # However, this depends on how critical the events are.
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
            if self.event_count:
                if self.event_count < 1:
                    print(f"Count {self.event_count} achieved, exiting program !")
                    exit()

                self.event_count -= 1

            e = {
                "ts": int(time.time()),
                "deviceId": f"device{device}",
                "temperature": round(random.uniform(-5, 15), 2),
                "door": state[random.randint(0,1)]
            }
            events.append(e)


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


