#!/bin/bash

echo "Stopping the Spring Boot application if it's running..."
PID=$(ps aux | grep 'java.*demo.*jar' | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "Killing process $PID"
    kill $PID
    sleep 2
fi

echo "Building and starting the Spring Boot application..."
./gradlew bootRun &

echo "Application restart initiated. Check logs for details." 