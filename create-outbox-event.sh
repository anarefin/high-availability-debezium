#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Manual Outbox Event Creation${NC}"
echo "============================="

# Generate a random UUID for the event
UUID=$(uuidgen | tr '[:upper:]' '[:lower:]')
echo -e "${YELLOW}Generated UUID:${NC} $UUID"

# Create JSON payload for a manual product event
PRODUCT_JSON='{"id": 999, "name": "Manual Event Product", "description": "Created directly in outbox table", "price": 199.99}'
echo -e "${YELLOW}Event Payload:${NC} $PRODUCT_JSON"

# Create SQL statement to insert into outbox_events table
SQL_STATEMENT="INSERT INTO app.outbox_events (id, aggregate_type, aggregate_id, event_type, payload, created_at) 
               VALUES ('$UUID', 'PRODUCT', '999', 'PRODUCT_CREATED', '$PRODUCT_JSON', CURRENT_TIMESTAMP);"

echo -e "${YELLOW}Executing SQL:${NC}"
echo "$SQL_STATEMENT"

# Execute the SQL using psql inside the postgres container
docker compose exec postgres psql -U user -d appdb -c "$SQL_STATEMENT"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Successfully inserted event into outbox_events table${NC}"
else
    echo -e "${RED}Failed to insert event${NC}"
fi

echo "============================="
echo "Event should now be picked up by Debezium and sent to Kafka and then to RabbitMQ."
echo "Check the application logs to confirm the event flow."

# Check events in the outbox table
echo -e "${YELLOW}Verifying entry in outbox_events table:${NC}"
docker compose exec postgres psql -U user -d appdb -c "SELECT id, aggregate_type, event_type, created_at FROM app.outbox_events ORDER BY created_at DESC LIMIT 5;" 