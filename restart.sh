#!/bin/bash

echo "Stopping all containers..."
docker compose down

echo "Starting containers..."
docker compose up -d

echo "Waiting for services to initialize (60 seconds)..."
sleep 20

echo "Registering Debezium connector..."
./register-connector.sh

echo "Restart completed." 