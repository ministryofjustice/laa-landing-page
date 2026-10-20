#!/bin/bash

# ./count_snyk_vulns.sh [--details] file1.sarif file2.sarif ...

# Emits one line per finding: <severity>	<ruleId>	<package@version>	<fixed in>	<description>
extract_findings() {
  local FILE=$1
  jq -r '
    def from_score: if . >= 9 then "critical" elif . >= 7 then "high" elif . >= 4 then "medium" elif . > 0 then "low" else empty end;
    def from_level: if . == "error" then "high" elif . == "warning" then "medium" elif . == "note" then "low" else empty end;

    .runs[]? |
    # Per rule: numeric security-severity (CVSS-style), default level and description
    (reduce (.tool.driver.rules[]?) as $r ({};
       .[$r.id] = {
         score: (try ($r.properties["security-severity"] | tonumber) catch 0),
         level: ($r.defaultConfiguration.level // ""),
         desc: ($r.shortDescription.text // ""),
         # "Upgrade <pkg> to version(s) a, b or higher" entries from the remediation text
         fixes: [(($r.help.markdown // $r.help.text // "") | match("Upgrade ([^ ]+(?: [^ ]+)??) to versions? (.+?) or higher"; "g").captures | {p: .[0].string, v: .[1].string})]
       })) as $rules
    |
    .results[]? |
    ($rules[.ruleId] // {score: 0, level: "", desc: "", fixes: []}) as $r |
    (if $r.score > 0 then ($r.score | from_score)
     else ((.level // $r.level) | from_level)
     end) as $sev |
    select($sev != null) |
    ([.locations[]?.logicalLocations[]?.fullyQualifiedName] | unique) as $fqn |
    ($fqn | map(sub("@.*$"; ""))) as $names |
    ($fqn | join(", ")) as $pkg |
    # Fix versions for this package; if the text names other packages only, show those instead
    ([$r.fixes[] | select(.p as $p | any($names[]; . as $n | $p | contains($n))) | .v] | unique | join("; ")) as $own |
    ([$r.fixes[] | "\(.p) -> \(.v)"] | unique | join("; ")) as $all |
    (if $own != "" then $own elif $all != "" then "other: " + $all else "" end) as $fixed |
    [$sev, .ruleId, ($pkg | if . == "" then "-" else . end), ($fixed | if . == "" then "no fix" else . end), (($r.desc | select(. != "")) // (.message.text // "") | split("
")[0] | .[0:200])] | @tsv
  ' "$FILE"
}

extract_severities() {
  extract_findings "$1" | cut -f1
}

# --details: print a de-duplicated findings list (most severe first) and exit
if [[ "$1" == "--details" ]]; then
  shift
  for file in "$@"; do
    [[ -f "$file" ]] || continue
    echo "== $file"
    extract_findings "$file" |
      awk -F'	' 'BEGIN{r["critical"]=0;r["high"]=1;r["medium"]=2;r["low"]=3} {print r[$1] "	" toupper($1) "	" $2 "	" $3 "	" $4 "	" $5}' |
      sort -u | cut -f2-
  done
  exit 0
fi

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
