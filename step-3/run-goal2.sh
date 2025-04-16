#!/usr/bin/env bash
set -ex

# Goal 2: Broadcast with topology optimization for low latency
# This script tests broadcast implementation that optimizes for latency

# Java file to run - can be overridden with environment variable
# Default implementation if not set
: ${JAVA_FILE:="Broadcast.java"}

# Network topology - options: grid, tree2, tree3, tree4
TOPOLOGY="tree4"

# Network latency in ms
LATENCY=100

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
  --topology "$TOPOLOGY" \
  --latency "$LATENCY" \
  --log-stderr
