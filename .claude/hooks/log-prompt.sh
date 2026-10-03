#!/usr/bin/env bash
# UserPromptSubmit hook: append each raw prompt with a timestamp to the branch's prompt history.
#
# master logs to docs/prompt-history.md; every other branch gets its own file, so a long-running
# feature branch does not interleave its prompts with the trunk's and force a merge conflict on
# every single prompt.
set -euo pipefail

root="${CLAUDE_PROJECT_DIR:-$(pwd)}"

# Branch name → log file. `feat/kmp-migration` becomes docs/prompt-history-kmp-migration.md: the
# conventional type prefix is dropped because it says nothing about the work, and anything left
# that is not a word character becomes a dash.
branch="$(git -C "$root" rev-parse --abbrev-ref HEAD 2>/dev/null || echo master)"
case "$branch" in
  master|main|HEAD)
    log="$root/docs/prompt-history.md"
    title="Prompt History"
    blurb="Raw prompts captured automatically by the UserPromptSubmit hook (\`.claude/hooks/log-prompt.sh\`). Prompts made on a feature branch live in that branch's own \`docs/prompt-history-<branch>.md\`."
    ;;
  *)
    slug="$(printf '%s' "$branch" | sed -E 's#^(feat|feature|fix|chore|docs|refactor|test)/##; s#[^A-Za-z0-9]+#-#g; s#^-+|-+$##g' | tr 'A-Z' 'a-z')"
    [ -n "$slug" ] || slug="branch"
    log="$root/docs/prompt-history-$slug.md"
    title="Prompt History — \`$branch\`"
    blurb="Raw prompts captured automatically by the UserPromptSubmit hook (\`.claude/hooks/log-prompt.sh\`) while working on the \`$branch\` branch. The trunk's log is \`docs/prompt-history.md\`."
    ;;
esac

mkdir -p "$(dirname "$log")"
[ -f "$log" ] || printf '# %s\n\n%s\n' "$title" "$blurb" > "$log"

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
