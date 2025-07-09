#!/usr/bin/env bash
set -eu

MODULE="core"
TARGET="assembly"
DEST_JAR_NAME="${JAR_NAME:-imr.jar}"  # Default from .env if available

source "$(dirname "$0")/../.env"

# Parse optional arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --module)
      MODULE="$2"
      shift 2
      ;;
    --dest)
      DEST_JAR_NAME="$2"
      shift 2
      ;;
    *)
      echo "❌ Unknown option: $1"
      echo "Usage: $0 [--module MODULE_NAME] [--dest DEST_JAR_NAME]"
      exit 1
      ;;
  esac
done

# Move to the project root
cd "$(dirname "$0")/.."
echo "📁 Current working directory: $(pwd)"
echo "📦 Module: $MODULE"
echo "📄 Destination JAR name: $DEST_JAR_NAME"

echo "🔨 Building JAR with Mill..."
./mill "${MODULE}.${TARGET}"

# Mill outputs JARs to ./out/<module>/assembly.dest/out.jar by default
JAR_PATH="out/${MODULE}/assembly.dest/out.jar"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "❌ Deploy failed: $JAR_PATH not found"
  exit 1
fi

echo "☁️ Uploading $JAR_PATH to $BUCKET/jars/$DEST_JAR_NAME ..."
gsutil cp "$JAR_PATH" "$BUCKET/jars/$DEST_JAR_NAME"

echo "✅ Deployment complete."
