#!/usr/bin/env bash

set -eu

MODULE="core"
TARGET="assembly"

source "$(dirname "$0")/../.env"

# Move to the project root
cd "$(dirname "$0")/.."
echo "📁 Current working directory: $(pwd)"

echo "🔨 Building JAR with Mill..."
./mill "${MODULE}.${TARGET}"

# Mill outputs JARs to ./out/<module>/assembly.dest/out.jar by default
JAR_PATH="out/${MODULE}/assembly.dest/out.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "❌ Deploy failed: $JAR_PATH not found"
  exit 1
fi

echo "☁️ Uploading $JAR_PATH to $BUCKET/jars/$JAR_NAME ..."
gsutil cp "$JAR_PATH" "$BUCKET/jars/$JAR_NAME"

echo "✅ Deployment complete."
