#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
CREATE TABLE MONITORING_RESULTS(
  id SERIAL PRIMARY KEY,
  device_id VARCHAR(10) NOT NULL,
  temperature FLOAT NOT NULL,
  timestamp timestamp without time zone NOT NULL,
  door_status NUMERIC NOT NULL
);
EOSQL
