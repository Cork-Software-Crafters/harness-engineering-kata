#!/bin/bash
# Runs quality checks on python/src and outputs violations.
# Exit 0 = pass, exit 1 = violations found (with details on stdout).

ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
cd "$ROOT/python" 2>/dev/null || exit 0

VIOLATIONS=""

# 1. flake8 checks (function length, complexity, magic numbers, class attrs, etc.)
FLAKE=$(flake8 src/ 2>&1)
[ -n "$FLAKE" ] && VIOLATIONS+="$FLAKE"$'\n'

# 2. File length check (max 150 lines per file)
while IFS= read -r f; do
  LINES=$(wc -l < "$f")
  if [ "$LINES" -gt 150 ]; then
    VIOLATIONS+="$f:1:1: FILE-LENGTH File has $LINES lines (max 150)"$'\n'
  fi
done < <(find src/ -name "*.py" ! -name "__init__.py")

if [ -n "$VIOLATIONS" ]; then
  echo "$VIOLATIONS"
  exit 1
fi

exit 0
