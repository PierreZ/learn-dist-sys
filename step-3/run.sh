#!/usr/bin/env bash
set -ex

# Java file to run - change this to test different implementations
JAVA_FILE="Broadcast.java"

# Make sure the file is executable
chmod +x "$JAVA_FILE"

# Build the file first to ensure it compiles
jbang build "$JAVA_FILE"

# Run the test with Maelstrom
../bin/maelstrom test -w broadcast --bin "./$JAVA_FILE" --node-count 5 --time-limit 20 --rate 10
