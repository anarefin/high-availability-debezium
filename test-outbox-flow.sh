#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Testing Outbox Pattern Flow${NC}"
echo "============================="

# Create a new product
echo -e "${YELLOW}1. Creating a new product...${NC}"
PRODUCT_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
    -d '{"name":"Test Product","description":"Created via API for testing outbox pattern","price":99.99}' \
    http://localhost:8080/api/products)

echo "Response: $PRODUCT_RESPONSE"

# Extract the product ID from the response
PRODUCT_ID=$(echo $PRODUCT_RESPONSE | sed -n 's/.*"id":\([0-9]*\).*/\1/p')

if [ -z "$PRODUCT_ID" ]; then
    echo "Failed to extract product ID. Check if the API is running and accessible."
    exit 1
fi

echo "Created product with ID: $PRODUCT_ID"
echo "============================="

# Wait a moment for the event to be processed
echo "Waiting for Debezium to process the outbox event..."
sleep 5

# Check connector status
echo -e "${YELLOW}2. Checking connector status...${NC}"
curl -s http://localhost:8083/connectors/outbox-connector/status | jq .
echo "============================="

# Check Kafka topics to see if the event was captured
echo -e "${YELLOW}3. Listing Kafka topics to verify event capture...${NC}"
docker compose exec kafka-1 kafka-topics --bootstrap-server kafka-1:19092 --list
echo "============================="

# Update the product
echo -e "${YELLOW}4. Updating the product...${NC}"
curl -s -X PUT -H "Content-Type: application/json" \
    -d '{"name":"Updated Test Product","description":"Updated via API for testing outbox pattern","price":129.99}' \
    http://localhost:8080/api/products/$PRODUCT_ID
echo "============================="

# Wait a moment for the event to be processed
echo "Waiting for the update event to be processed..."
sleep 5

# Delete the product
echo -e "${YELLOW}5. Deleting the product...${NC}"
curl -s -X DELETE http://localhost:8080/api/products/$PRODUCT_ID
echo "============================="

echo "Test flow completed. Check application logs to see the full event flow."
echo "To see Kafka messages, run:"
echo "docker compose exec kafka-1 kafka-console-consumer --bootstrap-server kafka-1:19092 --topic app.outbox_events --from-beginning"
echo "To see RabbitMQ messages, check the RabbitMQ management UI at http://localhost:15672 (guest/guest)" 