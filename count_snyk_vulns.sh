#!/bin/bash

# ./count_vulns.sh file1.sarif file2.sarif ...

extract_severities() {
  local FILE=$1
  jq -r '
    # Build a map from ruleId to severity (from properties.problem.severity or defaultConfiguration.level)
    (
      .runs[0].tool.driver.rules[]? |
      { (.id): (
          .properties.problem.severity // .defaultConfiguration.level // "unknown"
        )
      }
    ) as $severityMap
    |
    # For each result, get the severity from the map by ruleId
    .runs[0].results[]? |
    $severityMap[.ruleId] // empty
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
