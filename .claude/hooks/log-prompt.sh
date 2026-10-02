#!/usr/bin/env bash
# UserPromptSubmit hook: append each raw prompt with a timestamp to docs/prompt-history.md.
set -euo pipefail

root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
log="$root/docs/prompt-history.md"
mkdir -p "$(dirname "$log")"
[ -f "$log" ] || printf '# Prompt History\n\nRaw prompts captured automatically by the UserPromptSubmit hook (`.claude/hooks/log-prompt.sh`).\n' > "$log"

input="$(cat)"

# jq is not installed everywhere (notably Git Bash on Windows, where this hook silently
# did nothing until 2026-10-02), so fall back to perl's core JSON::PP.
read_field() {
  if command -v jq >/dev/null 2>&1; then
    jq -r --arg f "$1" '.[$f] // empty' <<<"$input"
  else
    FIELD="$1" perl -MJSON::PP -0777 -ne 'binmode(STDOUT, ":encoding(UTF-8)"); print decode_json($_)->{$ENV{FIELD}} // ""' <<<"$input"
  fi
}

prompt="$(read_field prompt)"
[ -n "$prompt" ] || exit 0
session="$(read_field session_id)"
[ -n "$session" ] || session="unknown"

{
  printf '\n## %s · session `%s`\n\n' "$(date '+%Y-%m-%d %H:%M:%S %Z')" "${session:0:8}"
  printf '````text\n%s\n````\n' "$prompt"
} >> "$log"
