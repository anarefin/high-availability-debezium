-- Create the schema
CREATE SCHEMA IF NOT EXISTS app;

-- Set up the main application table
CREATE TABLE IF NOT EXISTS app.products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Set up the outbox table for the outbox pattern
CREATE TABLE IF NOT EXISTS app.outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Set up the transactions table
CREATE TABLE IF NOT EXISTS app.transactions (
    id SERIAL PRIMARY KEY,
    amount DECIMAL(15, 2) NOT NULL,
    type VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create table for RabbitMQ transactions
CREATE TABLE IF NOT EXISTS app.rabbit_transactions (
    id SERIAL PRIMARY KEY,
    original_transaction_id INTEGER,
    amount DECIMAL(15, 2) NOT NULL,
    type VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_at TIMESTAMP NOT NULL
);

-- Add RabbitMQ extension for JSON handling (optional, used by Debezium)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Insert some sample data
INSERT INTO app.products (name, description, price) VALUES
    ('Laptop', 'High-performance laptop', 1299.99),
    ('Smartphone', 'Latest smartphone model', 799.99),
    ('Headphones', 'Noise-cancelling headphones', 199.99);

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA app TO "user";
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA app TO "user";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA app TO "user"; 