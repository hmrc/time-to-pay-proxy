#!/bin/bash

# Combine split OpenAPI spec into application.yaml
# Requires: @redocly/cli

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
API_DIR="$PROJECT_ROOT/resources/public/api/conf/1.0"

cd "$PROJECT_ROOT"

echo "Validating OpenAPI structure..."
if redocly lint "$API_DIR/openapi-split/openapi.yaml" --config="project/redocly.yaml" --format=stylish; then
    echo "OpenAPI structure is valid"
else
    echo "OpenAPI has validation warnings (continuing anyway)"
fi

echo "Combining OpenAPI spec..."
redocly bundle "$API_DIR/openapi-split/openapi.yaml" \
    --config="project/redocly.yaml" \
    --output "$API_DIR/application.yaml" \
    --ext yaml \
    --dereferenced=false \
    --force
echo "Combined spec written to $API_DIR/application.yaml"
