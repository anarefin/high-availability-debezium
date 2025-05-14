#!/bin/bash

# Check if connector exists and get its status
echo "Checking connector status..."
curl -s http://localhost:8083/connectors/outbox-connector/status | jq .

# Get detailed information about the connector
echo -e "\nConnector configuration details:"
curl -s http://localhost:8083/connectors/outbox-connector | jq .

# List all connectors
echo -e "\nAll available connectors:"
curl -s http://localhost:8083/connectors | jq .

# Check tasks status
echo -e "\nConnector tasks:"
curl -s http://localhost:8083/connectors/outbox-connector/tasks | jq . 