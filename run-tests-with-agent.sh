#!/usr/bin/env bash

set -euo pipefail

echo "Running tests..."
mvn clean test -Pnative -Dagent=true -Dmaven.test.failure.ignore=true -Djacoco.skip=true

echo "Post-processing native-image files"
INPUT_DIR=(target/native/agent-output/test/session-*)
OUTPUT_DIR="src/main/resources/META-INF/native-image/com.zaxxer/HikariCP"
mkdir -p "$OUTPUT_DIR"
native-image-configure generate-conditional --user-code-filter=native/user-code-filter.json --input-dir="$INPUT_DIR" --output-dir="$OUTPUT_DIR"
