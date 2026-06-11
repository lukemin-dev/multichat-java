#!/usr/bin/env bash
set -euo pipefail

./scripts/compile.sh
java -cp build/classes dev.lukemin.multichat.client.ChatClient "${1:-127.0.0.1}" "${2:-5000}" "${3:-guest}"

