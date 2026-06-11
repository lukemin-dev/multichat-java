#!/usr/bin/env bash
set -euo pipefail

./scripts/compile.sh
java -cp build/classes:build/test-classes dev.lukemin.multichat.protocol.MessageCodecTest

