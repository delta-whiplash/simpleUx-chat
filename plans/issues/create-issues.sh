#!/usr/bin/env bash
# Creates the SimpleUX audit backlog (26 issues) on GitHub, in file order.
# Source document: ../2026-08-15-ux-audit-backlog.md
#
# Usage:
#   bash create-issues.sh            # create labels + all issues
#   DRY_RUN=1 bash create-issues.sh  # parse check only, creates nothing
set -uo pipefail
cd "$(dirname "$0")"
REPO="delta-whiplash/simpleUx-chat"

declare -A LABELS=(
  ["priority:critical"]="b60205|Critical priority"
  ["priority:high"]="d93f0b|High priority"
  ["priority:medium"]="fbca04|Medium priority"
  ["priority:low"]="0e8a16|Low priority"
  ["area:trust"]="5319e7|Trust & safety"
  ["area:drift"]="1d76db|Upstream drift / sync"
  ["area:architecture"]="c5def5|Frontend architecture"
  ["area:design"]="d4c5f9|Design system"
  ["area:ux"]="bfd4f2|UX / QoL"
  ["area:identity"]="006b75|App identity / coexistence"
  ["area:hygiene"]="e99695|Repo hygiene"
  ["drift:blocking"]="b60205|Blocks upstream merge"
  ["drift:high"]="d93f0b|High merge-conflict risk"
  ["drift:low"]="fbca04|Low merge-conflict risk"
  ["drift:none"]="cccccc|No merge-conflict risk"
)

echo "== Labels =="
for name in "${!LABELS[@]}"; do
  IFS='|' read -r color desc <<< "${LABELS[$name]}"
  if [[ "${DRY_RUN:-0}" == "1" ]]; then
    echo "  would create label: $name (#$color)"
  else
    if gh label create "$name" --repo "$REPO" --color "$color" --description "$desc" --force >/dev/null 2>&1; then
      echo "  label ok: $name"
    else
      echo "  label FAILED: $name"
    fi
  fi
done

echo
echo "== Issues =="
created=0
failed=0
for f in *.md; do
  labels_str=$(sed -n '1s/^<!-- labels: \(.*\) -->$/\1/p' "$f")
  title=$(sed -n '2s/^# //p' "$f")
  if [[ -z "$labels_str" || -z "$title" ]]; then
    echo "PARSE ERROR: $f"
    failed=$((failed + 1))
    continue
  fi
  body_file=$(mktemp)
  tail -n +3 "$f" >"$body_file"
  label_args=()
  IFS=',' read -ra lbls <<<"$labels_str"
  for l in "${lbls[@]}"; do
    l="${l// /}"
    label_args+=(--label "$l")
  done
  if [[ "${DRY_RUN:-0}" == "1" ]]; then
    echo "  would create: $title  [${labels_str}]  (body: $(wc -c <"$body_file") bytes)"
  else
    url=$(gh issue create --repo "$REPO" --title "$title" --body-file "$body_file" "${label_args[@]}" 2>&1)
    if [[ "$url" == https://* ]]; then
      echo "  created: $url"
      created=$((created + 1))
    else
      echo "  FAILED: $title"
      echo "         $url"
      failed=$((failed + 1))
    fi
  fi
  rm -f "$body_file"
done

echo
if [[ "${DRY_RUN:-0}" == "1" ]]; then
  echo "Dry run complete. Nothing was created."
else
  echo "Done. created=$created failed=$failed"
fi
