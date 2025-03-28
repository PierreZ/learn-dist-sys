#!/usr/bin/env bash

../bin/maelstrom test -w unique-ids --bin ./ids.java --time-limit 5 --rate 1000 --node-count 3 --availability total --nemesis partition
