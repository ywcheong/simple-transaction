#!/usr/bin/env bash

set -euo pipefail

if [[ "${1:-}" == "stop" ]]; then
  echo "Stopping..."
  docker compose -f stress.docker-compose.yml down -v
  exit 0
fi


NUM_WORKERS=4

docker compose -f stress.docker-compose.yml \
  up -d --build --scale locust-worker="$NUM_WORKERS"

echo ""
echo "!! ============================================== !!"
echo "!! Locust UI Now running at http://localhost:8089"
echo "!! (# Workers: ${NUM_WORKERS})"
echo "!!"
echo "!! To stop Locust, execute ./run-stress.sh stop"
echo "!! ============================================== !!"