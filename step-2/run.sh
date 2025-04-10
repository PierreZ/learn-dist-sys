#!/usr/bin/env bash
set -ex

# Java file to run - change this to test different implementations
JAVA_FILE="UniqueId.java"

# Make sure the file is executable
chmod +x "$JAVA_FILE"

# Build the file first to ensure it compiles
jbang build "$JAVA_FILE"

# Run the test with Maelstrom
../bin/maelstrom test -w unique-ids --bin "./$JAVA_FILE" --time-limit 5 --node-count 3 --availability total --nemesis partition
