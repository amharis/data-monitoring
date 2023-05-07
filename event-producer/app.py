#!/usr/bin/python
import argparse
from configparser import ConfigParser

import atexit
import logging
import time
import yaml
import logging.config

from producer.producer import Producer

KAFKA_CONFIG_SECTION="kafka"
CONSUMER_POLL_INTERVAL_SECS="consumer_poll_interval_secs"

logger = logging.getLogger()

def setup_logger(verbose=False, file='logging.yaml'):
    """
    log_formatter = logging.Formatter("%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s")
    logger = logging.getLogger()

    #file_handler = logging.FileHandler("{0}/{1}.log".format(logPath, fileName))
    file_handler = logging.handlers.RotatingFileHandler(LOGFILE, maxBytes=(1048576*5), backupCount=7)
    file_handler.setFormatter(log_formatter)
    logger.addHandler(file_handler)

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(log_formatter)
    logger.addHandler(console_handler)

    if verbose:
        logger.setLevel(logging.DEBUG)
    """
    with open(file) as f:
        config_dict = yaml.safe_load(f)
        config_dict.setdefault('version', 1)
        logging.config.dictConfig(config_dict)

    if verbose:
        logging.getLogger().setLevel(logging.DEBUG)

def parse_config(file: str):
    # create a parser
    parser = ConfigParser()
    # read config file
    parser.read(file)
    return parser

def get_config_section(args, section_name: str) -> {str, str}:
    # parse conf for postgres and kafka integrations
    config_parser = parse_config(args.conf_file)

    section = {}
    if config_parser.has_section(section_name):
        params = config_parser.items(section_name)
        for param in params:
            section[param[0]] = param[1]
    else:
        raise Exception(f'Section {section} not found in config')

    return section

def init_application(args, kafka_config, devices, interval_seconds, event_count):
    producer = Producer(kafka_config, devices=devices, interval_seconds=interval_seconds, event_count=event_count)

    producer.setup_producer()
    #producer.start_producing()

    return producer

def cleanup(producer):
    producer.close_producer()

"""
{
 "ts": 1680977924,
 "deviceId": "device1",
 "temperature": 12.3,
 "door": "open"
}

Interval between events (default: 5 seconds)
o Max number of events to generate (default: unlimited, i.e. program must be
manually interrupted to stop)
2
o Number of devices (default: 5)
§ The interval parameter is per device, e.g if configured for 5 devices
with an interval of 5 seconds, then 5 events are generated every 5
seconds (1 per device)
"""
if __name__ == "__main__":
    setup_logger(True)
    logger.info(f"Starting Bot event producer service")

    parser = argparse.ArgumentParser()

    parser.add_argument("-cf", "--conf-file", help="location of config for Kafka cluster")
    parser.add_argument("-i", "--interval", type=int, default=5, choices=range(5,11), help="interval in secs between event generation (5-10)")
    parser.add_argument("-d", "--devices", type=int, default=5, choices=range(1,6),help="number of devices (1-5)")
    parser.add_argument("-n", "--event-count", type=int, default=None, help="number of events to produce, default is unlimited")
    args = parser.parse_args()

    print(f"args: {args}")

    kafka_config = get_config_section(args, KAFKA_CONFIG_SECTION)

    producer = init_application(
        args, kafka_config=kafka_config, devices=args.devices, interval_seconds=args.interval, event_count=args.event_count)

    atexit.register(cleanup, producer)
    while True:
        producer.produce_events()
        time.sleep(args.interval)

