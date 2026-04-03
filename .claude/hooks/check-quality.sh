#!/bin/bash
# Runs flake8 quality checks on python/src and outputs violations.
# Exit 0 = pass, exit 1 = violations found (with details on stdout).

cd "$(git rev-parse --show-toplevel)/python" 2>/dev/null || exit 0

OUTPUT=$(flake8 src/ 2>&1)
if [ -n "$OUTPUT" ]; then
  echo "$OUTPUT"
  exit 1
fi

exit 0
