#!/bin/bash

BASE_COMPOSE_FILE="./docker/docker-compose.base.yml"
PROD_COMPOSE_FILE="./docker/docker-compose.prod.yml"

DEV_COMPOSE_OPTION="-f $BASE_COMPOSE_FILE"
PROD_COMPOSE_OPTION="-f $BASE_COMPOSE_FILE -f $PROD_COMPOSE_FILE"

case "$1" in
  dev)
    COMPOSE_FILE_OPTION=$DEV_COMPOSE_OPTION
    ;;
  prod)
    COMPOSE_FILE_OPTION=$PROD_COMPOSE_OPTION
    echo "Building and copying server.jar..."
    ./gradlew dockerMount
    ;;
  gradle-clean)
    echo "Cleaning gradle..."
    ./gradlew clean
    ./gradlew --stop
    exit 0
    ;;
  docker-clean)
    echo "Cleaning docker volumes..."
    docker compose $DEV_COMPOSE_OPTION down -v
    docker compose $PROD_COMPOSE_OPTION down -v
    exit 0
    ;;
  clean)
    $0 gradle-clean
    $0 docker-clean
    exit 0
    ;;
  *)
    echo "Usage: $0 {prod|dev} {start|stop} OR $0 clean"
    exit 1
    ;;
esac

case "$2" in
  start)
    echo "Starting Docker Compose services..."
    docker compose $COMPOSE_FILE_OPTION up -d --build
    if [ $? -ne 0 ]; then
        sleep 1s
        $0 $1 stop
    fi
    ;;
  stop)
    echo "Stopping Docker Compose services..."
    docker compose $COMPOSE_FILE_OPTION down
    docker system prune -f
    ;;
  *)
    echo "Usage: $0 {prod|dev} {start|stop} OR $0 clean"
    exit 1
    ;;
esac
