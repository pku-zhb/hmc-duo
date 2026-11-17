#!/usr/bin/env bash
# Repository hygiene check. Run before committing: tools/check-hygiene.sh
#   (default)  checks the staged diff
#   --all      checks every tracked file
# Optional private deny-list: put one extended-regex pattern per line in
# hygiene-deny.local.txt (ignored by Git) to block personal names, hosts or paths.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
mode="${1:-staged}"
fail=0

forbidden_files='(^|/)(AGENTS\.local\.md|hygiene-deny\.local\.txt|local\.properties|keystore\.properties|HANDOFF\.md)$|\.(jks|keystore|p12|pfx|pem|apk|aab|hprof)$|^(releases/local|archive)/'
patterns=(
  '(/Users/[A-Za-z0-9._-]+|/Volumes/[A-Za-z0-9._ -]+|/home/[a-z][A-Za-z0-9._-]*|[A-Za-z]:\\Users\\[A-Za-z0-9._-]+)'
  '~/[A-Za-z0-9._-]+'
  '(?i)(api[_-]?key|secret|token|passw(or)?d)[A-Za-z_]*["'"'"']?\s*[=:]\s*["'"'"'][A-Za-z0-9+/_=-]{16,}["'"'"']'
  '-----BEGIN [A-Z ]*PRIVATE KEY-----'
  '\b(gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|sk-[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}|xox[bap]-[A-Za-z0-9-]{10,})'
)
if [[ -f hygiene-deny.local.txt ]]; then
  while IFS= read -r line; do [[ -n "$line" && "$line" != \#* ]] && patterns+=("$line"); done < hygiene-deny.local.txt
fi

if [[ "$mode" == "--all" ]]; then
  files=$(git ls-files)
  added() { git grep -I -n -e '' -- "$1" 2>/dev/null | sed 's/^[^:]*:[0-9]*://'; }
else
  files=$(git diff --cached --name-only --diff-filter=AM)
  added() { git diff --cached -U0 -- "$1" | grep -E '^\+' | grep -v -E '^\+\+\+' | sed 's/^+//'; }
fi

while IFS= read -r f; do
  [[ -z "$f" ]] && continue
  if [[ "$f" =~ $forbidden_files ]]; then echo "forbidden file: $f"; fail=1; continue; fi
  case "$f" in AGENTS.md|tools/check-hygiene.sh|tools/hooks/*) continue ;; esac
  content=$(added "$f" || true)
  [[ -z "$content" ]] && continue
  for p in "${patterns[@]}"; do
    # credential-like literals are allowed as fake values in test sources
    if [[ "$f" == */src/test/* || "$f" == */src/androidTest/* ]] && [[ "$p" == *passw* ]]; then continue; fi
    if grep -I -q -P -e "$p" <<<"$content" 2>/dev/null; then
      echo "hygiene: $f matches a restricted pattern ($p)"; fail=1
    fi
  done
done <<<"$files"

exit $fail
