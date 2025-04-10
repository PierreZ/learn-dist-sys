#!/usr/bin/env bash
set -ex

# Java file to run - can be overridden with environment variable
# Default implementation if not set
: ${JAVA_FILE:="Echo.java"}

# Make sure the file is executable
chmod +x "$JAVA_FILE"

# Build the file first to ensure it compiles
jbang build "$JAVA_FILE"

# Run the test with Maelstrom
../bin/maelstrom test -w echo --bin "./$JAVA_FILE" --node-count 1 --time-limit 10