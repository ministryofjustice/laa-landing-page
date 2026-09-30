#!/bin/bash

# ./count_vulns.sh file1.sarif file2.sarif ...

extract_severities() {
  local FILE=$1
  jq -r '
    def from_score: if . >= 9 then "critical" elif . >= 7 then "high" elif . >= 4 then "medium" elif . > 0 then "low" else empty end;
    def from_level: if . == "error" then "high" elif . == "warning" then "medium" elif . == "note" then "low" else empty end;

    .runs[]? |
    # Per rule: numeric security-severity (CVSS-style) and default level
    (reduce (.tool.driver.rules[]?) as $r ({};
       .[$r.id] = {
         score: (try ($r.properties["security-severity"] | tonumber) catch 0),
         level: ($r.defaultConfiguration.level // "")
       })) as $rules
    |
    .results[]? |
    ($rules[.ruleId] // {score: 0, level: ""}) as $r |
    if $r.score > 0 then ($r.score | from_score)
    else ((.level // $r.level) | from_level)
    end
  ' "$FILE"
}

if [[ $# -eq 0 ]]; then
  echo "Usage: $0 file1.sarif [file2.sarif ...]"
  exit 1
fi

CRITICAL=0
HIGH=0
MEDIUM=0
LOW=0

for file in "$@"; do
  if [[ -f "$file" ]]; then
    echo "Processing $file"
    severities=$(extract_severities "$file")
    # Count severities case-insensitively without ${var,,} for bash <4
    while read -r severity; do
      # Lowercase severity for matching
      severity_lower=$(echo "$severity" | tr '[:upper:]' '[:lower:]')
      case "$severity_lower" in
        critical) ((CRITICAL++)) ;;
        high)     ((HIGH++)) ;;
        medium)   ((MEDIUM++)) ;;
        low)      ((LOW++)) ;;
      esac
    done <<< "$severities"
  else
    echo "File not found: $file"
  fi
done

# Output results
echo "CRITICAL=$CRITICAL"
echo "HIGH=$HIGH"
echo "MEDIUM=$MEDIUM"
echo "LOW=$LOW"

# Export to GitHub Actions environment file if applicable
if [[ -n "$GITHUB_ENV" ]]; then
  {
    echo "CRITICAL=$CRITICAL"
    echo "HIGH=$HIGH"
    echo "MEDIUM=$MEDIUM"
    echo "LOW=$LOW"
  } >> "$GITHUB_ENV"
fi
