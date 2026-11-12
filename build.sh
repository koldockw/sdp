#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 --release 17 -d out src/moneypilot/*.java
jar --create --file MoneyPilot.jar --main-class moneypilot.Main -C out .
