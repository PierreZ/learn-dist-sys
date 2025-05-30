#!/usr/bin/env bash
set -ex

# Goal 1: Basic broadcast with message deduplication
# This script tests the basic broadcast implementation focusing on avoiding message amplification

# Java file to run - can be overridden with environment variable
# Default implementation if not set
: ${JAVA_FILE:="Broadcast.java"}

# Make sure the file is executable
chmod +x "$JAVA_FILE"

# Build the file first to ensure it compiles
jbang build "$JAVA_FILE"

# Run the test with Maelstrom
../bin/maelstrom test \
  -w broadcast \
  --bin "./$JAVA_FILE" \
  --node-count 3 \
  --time-limit 20 \
  --rate 1 \
  --topology "grid" \
  --latency 0 \
  --log-stderr
