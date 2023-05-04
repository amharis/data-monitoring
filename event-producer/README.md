# Monitoring Service

Monitoring service is a small python service which accepts monitoring jobs in json format and periodically
performs health checks on them.

The metrics collected are
- http status code
- response latency
- optionlly checking if contents of website match a supplied regex

The application has following configuration files. 
- **config.ini** : for managing connection related properties to kafka cluster and postgres
database, and another for providing jobs to be monitored.
- **jobs.json** : jobs which application should monitor
- **schema.json** : Validating jobs input.
- **logging.yaml** : logging configuration


### Sample jobs configuration:
```
{
"jobs": [ 
    {
        "name": "job1",
        "url": "https://www.google.com",
        "regex": ""
    },
    {
        "name": "job2",
        "url": "https://www.bing.com",
        "regex": ".*"
    }
]}
```

## Run
**Note:** this application needs python3 (it has been tested on MacOS with python3)

### Setup
- Checkout repo: 
- cd to project root
- python3 -m venv .venv # create a virtualenv
- source .venv/bin/activate
- pip3 install -r requirements.txt

### Run Command
- `python3 app.py  -cf config.ini -jf jobs.json -sf schema.json`

**where**
  - cf:  path to app config file
  - jf:  path to jobs file
  - sf:  path to schema file

The command provided as example should be enough to run the application (provided aiven account credits are available)

**Note** the setup is bit cumbersome and should be containerized, but focusing on python code as outlined in instructions

### Running test and Formatting checks
- pytest -s test/integration_test.py
- mypy app.py ./producer ./consumer

## Running kafka and PostGresql locally

run `docker-compose up -d` from project root

### DB and Kafka cluster setup

```shell
harisabdullah@Hariss-MBP-2 kafka-poc % docker exec broker \
kafka-topics --bootstrap-server broker:9092 \
             --create \
             --topic monitoring-test
             
# connect as postgres user
psql 'postgres://localhost:5432' -U postgres
CREATE ROLE postgres WITH LOGIN PASSWORD 'postgres' WITH CREATEDB CREATEROLE;
CREATE DATABASE monitoring_results;
CREATE TABLE monitoring_results (
                    id SERIAL PRIMARY KEY,
                    job_id VARCHAR(255) NOT NULL,
                    response_code INTEGER NOT NULL,
                    response_latency INTEGER NOT NULL,
                    event_timestamp timestamp without time zone NOT NULL,
                    regex_filter text
            );              


DESCRIBE TABLE monitoring_results;
# Describe database
monitoring_results=# \d monitoring_results;
# Describe table
\dt
# change to database
monitoring_results=# \c monitoring_results
# list data bases
monitoring_results=# \l 
```
## Attributions
Following resources were consulted during the course of development
- https://towardsdatascience.com/how-to-build-a-simple-kafka-producer-and-consumer-with-python-a967769c4742
- https://github.com/dpkp/kafka-python
- https://www.psycopg.org/docs/cursor.html
- https://superfastpython.com/threadpoolexecutor-in-python/