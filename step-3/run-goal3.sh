#!/usr/bin/env bash
set -ex

# Goal 3: Broadcast with partition tolerance
# This script tests broadcast implementation that handles network partitions

# Java file to run - can be overridden with environment variable
# Default implementation if not set
: ${JAVA_FILE:="Broadcast.java"}

# Network topology - options: grid, tree2, tree3, tree4
TOPOLOGY="grid"

# Network latency in ms
LATENCY=100

# Make sure the file is executable
chmod +x "$JAVA_FILE"

# Build the file first to ensure it compiles
jbang build "$JAVA_FILE"

# Run the test with Maelstrom, with the partition nemesis enabled
../bin/maelstrom test \
  -w broadcast \
  --bin "./$JAVA_FILE" \
  --node-count 5 \
  --time-limit 30 \
  --rate 10 \
  --topology "$TOPOLOGY" \
  --latency "$LATENCY" \
  --nemesis partition
