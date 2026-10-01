#!/usr/bin/env bash
# UserPromptSubmit hook: append each raw prompt with a timestamp to docs/prompt-history.md.
set -euo pipefail

root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
log="$root/docs/prompt-history.md"
mkdir -p "$(dirname "$log")"
[ -f "$log" ] || printf '# Prompt History\n\nRaw prompts captured automatically by the UserPromptSubmit hook (`.claude/hooks/log-prompt.sh`).\n' > "$log"

input="$(cat)"
prompt="$(jq -r '.prompt // empty' <<<"$input")"
[ -n "$prompt" ] || exit 0
session="$(jq -r '.session_id // "unknown"' <<<"$input")"

{
  printf '\n## %s · session `%s`\n\n' "$(date '+%Y-%m-%d %H:%M:%S %Z')" "${session:0:8}"
  printf '````text\n%s\n````\n' "$prompt"
} >> "$log"
