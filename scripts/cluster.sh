#!/usr/bin/env bash

set -euo pipefail

# Load config from .env one directory up (optional but useful)
set -a
source "$(dirname "$0")/../.env"
set +a

# Check that an action (create/delete) was passed
if [[ $# -lt 1 ]]; then
  echo "❌ Missing required action. Usage:"
  echo "   $0 create [--num-workers N] [--name CLUSTER_NAME]"
  echo "   $0 delete [--name CLUSTER_NAME]"
  exit 1
fi

ACTION="$1"
shift

# Default cluster name from env
CLUSTER_NAME="${CLUSTER:-}"

# Default num workers (optional)
NUM_WORKERS=0

# Parse optional flags
while [[ $# -gt 0 ]]; do
  case "$1" in
    --num-workers)
      NUM_WORKERS="$2"
      shift 2
      ;;
    --name)
      CLUSTER_NAME="$2"
      shift 2
      ;;
    *)
      echo "❌ Unknown option: $1"
      exit 1
      ;;
  esac
done

# Validate cluster name presence
if [[ -z "$CLUSTER_NAME" ]]; then
  echo "❌ Cluster name not specified. Pass --name or set CLUSTER in .env"
  exit 1
fi

IMAGE_VERSION="${IMAGE_VERSION:-2.2-debian11}"

if [[ "$ACTION" == "create" ]]; then
  echo "🛠️ Creating cluster '$CLUSTER_NAME' with $NUM_WORKERS workers..."

  LOG_DIR="$BUCKET"/spark-job-history
  N_CORES=4

  PROPERTIES="spark:spark.history.fs.logDirectory=$LOG_DIR"
  PROPERTIES+=",spark:spark.eventLog.dir=$LOG_DIR"
  PROPERTIES+=",spark:spark.history.custom.executor.log.url.applyIncompleteApplication=false"
  PROPERTIES+=",spark:spark.history.custom.executor.log.url={{YARN_LOG_SERVER_URL}}/{{NM_HOST}}:{{NM_PORT}}/{{CONTAINER_ID}}/{{CONTAINER_ID}}/{{USER}}/{{FILE_NAME}}"

  # Spark on dataproc runs in client mode by default and will not detect all vcores in the cluster
  # Running in cluster mode would make logging inconvenient for a small app
  if [[ $NUM_WORKERS -gt 0 ]]; then
    PROPERTIES+=",spark:spark.default.parallelism=$((NUM_WORKERS * N_CORES))"
  else
    PROPERTIES+=",spark:spark.spark.dynamicAllocation.enabled=false"
    PROPERTIES+=",spark:spark.executor.cores=$N_CORES"
    PROPERTIES+=",spark:spark.executor.instances=0"
  fi

  COMMON_OPTIONS=(
    --metric-sources spark
    --enable-component-gateway
    --project "$PROJECT"
    --region "$REGION"
    --image-version "$IMAGE_VERSION"
    --master-boot-disk-size 100
    --worker-boot-disk-size 100
    --master-machine-type n4-highmem-"$N_CORES"
    --worker-machine-type n4-standard-"$N_CORES"
    --properties "$PROPERTIES"
  )

  if [[ "$NUM_WORKERS" -eq 0 ]]; then
    echo "🔧 Using single-node configuration"
    gcloud dataproc clusters create "$CLUSTER_NAME" \
      "${COMMON_OPTIONS[@]}" \
      --single-node
  else
    gcloud dataproc clusters create "$CLUSTER_NAME" \
      "${COMMON_OPTIONS[@]}" \
      --num-workers "$NUM_WORKERS"
  fi

  echo "✅ Cluster created."

elif [[ "$ACTION" == "delete" ]]; then
  echo "🗑️ Deleting cluster '$CLUSTER_NAME'..."

  gcloud dataproc clusters delete "$CLUSTER_NAME" \
    --project "$PROJECT" \
    --region "$REGION" \
    --quiet

  echo "✅ Cluster deleted."

else
  echo "❌ Unknown action: $ACTION"
  echo "Usage: $0 {create|delete} [--num-workers N] [--name CLUSTER_NAME]"
  exit 1
fi
