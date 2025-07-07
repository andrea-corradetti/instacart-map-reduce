#!/usr/bin/env bash

set -euo pipefail

# Load config from .env one directory up (optional but useful)
set -a
source "$(dirname "$0")/../.env" 2>/dev/null || true
set +a

# Check that an action (create/delete) was passed
if [[ $# -lt 1 ]]; then
  echo "❌ Missing required action. Usage:"
  echo "   $0 create [--num-workers N]"
  echo "   $0 delete"
  exit 1
fi

ACTION="$1"
shift

# Parse optional flags (e.g., --num-workers)
while [[ $# -gt 0 ]]; do
  case "$1" in
    --num-workers)
      NUM_WORKERS="$2"
      shift 2
      ;;
    *)
      echo "❌ Unknown option: $1"
      exit 1
      ;;
  esac
done

IMAGE_VERSION="${IMAGE_VERSION:-2.2-debian11}"
LOG_DIR="$BUCKET"/spark-job-history

if [[ "$ACTION" == "create" ]]; then
  echo "🛠️ Creating cluster '$CLUSTER' with $NUM_WORKERS workers..."


  gcloud dataproc clusters create "$CLUSTER" \
    --metric-sources spark \
    --enable-component-gateway \
    --region "$REGION" \
    --image-version "$IMAGE_VERSION" \
    --master-boot-disk-size 100 \
    --worker-boot-disk-size 100 \
    --master-machine-type n4-standard-4 \
    --worker-machine-type n4-standard-4 \
    --num-workers "$NUM_WORKERS" \
    --properties spark:spark.history.fs.logDirectory="$LOG_DIR",spark:spark.eventLog.dir="$LOG_DIR"


  echo "✅ Cluster created."

elif [[ "$ACTION" == "delete" ]]; then
  echo "🗑️ Deleting cluster '$CLUSTER'..."

  gcloud dataproc clusters delete "$CLUSTER" \
    --region "$REGION" \
    --quiet

  echo "✅ Cluster deleted."

else
  echo "❌ Unknown action: $ACTION"
  echo "Usage: $0 {create|delete} [--num-workers N]"
  exit 1
fi