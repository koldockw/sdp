#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 --release 17 -d out src/moneypilot/*.java test/moneypilot/*.java
java -Djava.awt.headless=true -cp out moneypilot.ProjectTest
