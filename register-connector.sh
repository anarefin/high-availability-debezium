#!/bin/bash

# Wait for Kafka Connect to be available
echo "Waiting for Kafka Connect to be available..."
while [ $(curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/connectors) -ne 200 ]; do
    echo "Kafka Connect not yet available, waiting..."
    sleep 5
done

echo "Kafka Connect is available, registering connector..."

# Register the connector
curl -X POST -H "Content-Type: application/json" \
    --data @debezium-connector-config.json \
    http://localhost:8083/connectors

echo "Connector registration request sent. Checking status..."

# Wait a moment for the connector to be registered
sleep 5

# Check if the connector is registered
curl -s http://localhost:8083/connectors/app-connector/status | jq .

echo "Setup completed." 