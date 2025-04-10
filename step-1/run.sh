#!/usr/bin/env bash
set -ex;

../bin/maelstrom test -w echo --bin ./Echo.java --node-count 1 --time-limit 10;