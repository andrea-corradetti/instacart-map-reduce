#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "$0")/../.env"

echo "🚀 Submitting Spark job to Dataproc..."

# Defaults from env
CLUSTER_NAME="${CLUSTER:-}"
JOB_ID=""
JAR_NAME="${JAR_NAME:-imr.jar}"
JAR_ARGS=()

# Separate script args and jar args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --)
      shift
      JAR_ARGS+=("$@")
      break
      ;;
    --cluster)
      CLUSTER_NAME="$2"
      shift 2
      ;;
    --id)
      JOB_ID="$2"
      shift 2
      ;;
    --jar-name)
      JAR_NAME="$2"
      shift 2
      ;;
    *)
      echo "❌ Unknown option: $1"
      echo "Usage: $0 [--cluster CLUSTER_NAME] [--id JOB_ID] [--jar-name JAR_NAME] [-- <JAR_ARGS>...]"
      exit 1
      ;;
  esac
done

if [[ -z "$CLUSTER_NAME" ]]; then
  echo "❌ Cluster name not set! Provide --cluster or set CLUSTER in .env"
  exit 1
fi

JAR_PATH="${JAR_PATH:-"$BUCKET"/jars/"$JAR_NAME"}"
INPUT_URI="${INPUT_URI:-"$BUCKET"/order_products.csv}"
OUTPUT_URI="${OUTPUT_URI:-"$BUCKET"/out/${JOB_ID:-latest}}"

echo "📦 Using jar:        $JAR_PATH"
echo "📥 Input URI:        $INPUT_URI"
echo "📤 Output URI:       $OUTPUT_URI"
echo "☁️ Cluster:          $CLUSTER_NAME"
[[ -n "$JOB_ID" ]] && echo "🆔 Job ID:           $JOB_ID"
[[ "${#JAR_ARGS[@]}" -gt 0 ]] && echo "📎 Jar Args:         ${JAR_ARGS[*]}"

PROPERTIES=spark.hadoop.mapreduce.outputcommitter.factory.class=org.apache.hadoop.mapreduce.lib.output.DataprocFileOutputCommitterFactory
PROPERTIES+=,spark.hadoop.mapreduce.fileoutputcommitter.marksuccessfuljobs=false

ARGS=(
  --cluster "$CLUSTER_NAME"
  --region "$REGION"
  --jar "$JAR_PATH"
  --properties "$PROPERTIES"
)

if [[ -n "$JOB_ID" ]]; then
  ARGS+=(--id "$JOB_ID")
fi

JAR_ARGS+=(
  --input "$INPUT_URI"
  --output "$OUTPUT_URI"
  --force-write
)

gcloud dataproc jobs submit spark "${ARGS[@]}" -- "${JAR_ARGS[@]}"

echo "✅ Job submitted."
