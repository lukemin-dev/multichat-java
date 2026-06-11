#!/usr/bin/env bash
set -euo pipefail

./scripts/compile.sh
java -cp build/classes dev.lukemin.multichat.server.ChatServer "${1:-5000}"

