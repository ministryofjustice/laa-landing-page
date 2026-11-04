"""Summarise the ZAP active scan: alert instances, scanned endpoints and session state."""

import html
import json
import os
import re
import sys
from collections import Counter
from pathlib import Path
from urllib.parse import parse_qsl, urlsplit


WORK = Path("build/zap")
RISKS = (("3", "High"), ("2", "Medium"), ("1", "Low"))
ID_SEGMENT = re.compile(
    r"^(?:\d+|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$"
)
MAX_LISTED_ENDPOINTS = 400
MAX_LISTED_PATTERNS = 20


def cell(value):
    return html.escape(str(value)).replace("|", "&#124;").replace("\n", " ")


def normalized_path(url):
    path = re.sub(r"\(\).*$", "", urlsplit(url).path) or "/"
    return "/".join("{id}" if ID_SEGMENT.match(part) else part for part in path.split("/"))


def endpoint(method, url, data=None):
    # Drops queries and ZAP's "()(params)" node suffix, and folds IDs so test data isn't listed.
    path = normalized_path(url)
    params = ""
    if data:
        names = sorted({name for name in re.split(r"=&?", data) if name})
        params = f" ({', '.join(names)})" if names else ""
    return f"{method} {path}{params}"


def site_nodes(tree):
    stack = list(tree or [])
    while stack:
        node = stack.pop()
        if not isinstance(node, dict):
            continue
        if node.get("url") and node.get("method"):
            yield node
        stack.extend(node.get("children") or [])


def coverage(tree):
    nodes = list(site_nodes(tree))
    endpoints = sorted({endpoint(n["method"], n["url"], n.get("data")) for n in nodes})
    statuses = Counter(str(n.get("statusCode", "none")) for n in nodes)
    areas = Counter(e.split(" ", 1)[1].split("/")[1] or "/" for e in endpoints)
    return {"nodes": len(nodes), "endpoints": endpoints, "statuses": statuses, "areas": areas}


def safe_parameter_name(name):
    # Keep parameter names useful for debugging while excluding values and control characters.
    name = re.sub(r"[^A-Za-z0-9_.-]", "_", name)
    return name[:100] or "(unnamed)"


def request_patterns(tree):
    patterns = Counter()
    for node in site_nodes(tree):
        query_names = sorted({
            safe_parameter_name(name)
            for name, _ in parse_qsl(urlsplit(node["url"]).query, keep_blank_values=True)
        })
        body_names = sorted({
            safe_parameter_name(name)
            for name, _ in parse_qsl(node.get("data", ""), keep_blank_values=True)
        })
        if query_names or body_names:
            patterns[(
                node["method"],
                normalized_path(node["url"]),
                tuple(query_names),
                tuple(body_names),
            )] += 1
    return patterns


def query_pattern_markdown(patterns):
    lines = [
        "Sanitized request patterns from the exported Sites Tree. Counts are exported nodes, "
        "not AJAX spider crawl-state counts.",
        "",
        "Query values are discarded; parameter names are sanitized. Paths fold numeric and UUID "
        "segments to `{id}`.",
        "",
        "| Method | Path | Query parameter names | Form/body parameter names | Sites Tree nodes |",
        "|---|---|---|---|---:|",
    ]
    for (method, path, query_names, body_names), count in sorted(
        patterns.items(), key=lambda item: (-item[1], item[0])
    ):
        lines.append(
            f"| {cell(method)} | {cell(path)} | {cell(', '.join(query_names) or '-')} | "
            f"{cell(', '.join(body_names) or '-')} | {count} |"
        )
    if not patterns:
        lines.append("| - | - | No parameterized requests found | - | 0 |")
    return "\n".join(lines) + "\n"


def alert_rows(report):
    rows = []
    for site in report.get("site", []):
        for alert in site.get("alerts", []):
            rows.append({
                "risk": alert.get("riskcode", ""),
                "plugin": alert.get("pluginid", ""),
                "name": alert.get("alert") or alert.get("name", ""),
                "instances": int(alert.get("count") or len(alert.get("instances", []))),
            })
    order = {code: index for index, (code, _) in enumerate(RISKS)}
    return sorted(rows, key=lambda r: (order.get(r["risk"], 9), -r["instances"], r["name"]))


def markdown(rows, cov, session_status, patterns=None):
    names = dict(RISKS)
    lines = ["### ZAP active scan", ""]
    if session_status == "valid":
        lines.append("Test session was still valid after the scan.")
    elif session_status:
        lines.append(f"**Test session was not valid after the scan ({cell(session_status)}): "
                     "later requests may have hit the login redirect, so results are unreliable.**")
    lines += ["", "| Risk | Alert types | Instances |", "|---|---|---|"]
    for code, name in RISKS:
        matching = [r for r in rows if r["risk"] == code]
        lines.append(f"| {name} | {len(matching)} | {sum(r['instances'] for r in matching)} |")
    if rows:
        lines += ["", "| Risk | Rule | Finding | Instances |", "|---|---|---|---|"]
        lines += [f"| {names.get(r['risk'], r['risk'])} | {cell(r['plugin'])} | {cell(r['name'])} | "
                  f"{r['instances']} |" for r in rows]
    lines += ["", "#### Coverage", ""]
    if cov is None:
        lines.append("Sites Tree export not found, so coverage is unknown.")
        return "\n".join(lines) + "\n"
    lines.append(f"ZAP scanned **{cov['nodes']} Sites Tree nodes**, covering "
                 f"**{len(cov['endpoints'])} endpoints** (queries removed, IDs shown as `{{id}}`).")
    lines.append("Response codes: " + ", ".join(
        f"{cell(code)}: {count}" for code, count in sorted(cov["statuses"].items())) + ".")
    lines += ["", "| Area | Endpoints |", "|---|---|"]
    lines += [f"| /{cell(area).lstrip('/')} | {count} |" for area, count in cov["areas"].most_common()]
    listed = cov["endpoints"][:MAX_LISTED_ENDPOINTS]
    lines += ["", "<details><summary>Endpoints scanned</summary>", "", "```text"]
    lines += [e.replace("```", "'''") for e in listed]
    if len(cov["endpoints"]) > len(listed):
        lines.append(f"... {len(cov['endpoints']) - len(listed)} more in the artifact")
    lines += ["```", "", "</details>"]
    lines += ["", "#### Sanitized query and form patterns", ""]
    lines.append(
        f"Found **{len(patterns or {})} distinct parameter patterns** across "
        f"**{sum((patterns or {}).values())} Sites Tree nodes**. Values are omitted; "
        "the full pattern table is in `zap-active-query-patterns.md`."
    )
    lines += ["", "| Method | Path | Query parameter names | Form/body parameter names | Nodes |",
              "|---|---|---|---|---:|"]
    for (method, path, query_names, body_names), count in sorted(
        (patterns or {}).items(), key=lambda item: (-item[1], item[0])
    )[:MAX_LISTED_PATTERNS]:
        lines.append(
            f"| {cell(method)} | {cell(path)} | {cell(', '.join(query_names) or '-')} | "
            f"{cell(', '.join(body_names) or '-')} | {count} |"
        )
    if len(patterns or {}) > MAX_LISTED_PATTERNS:
        lines.append(f"| ... | ... | ... | ... | {len(patterns) - MAX_LISTED_PATTERNS} more in artifact |")
    return "\n".join(lines) + "\n"


def main():
    report_path = WORK / "zap-active-report.json"
    if not report_path.exists():
        print("::warning::No ZAP report was produced.")
        return 0
    rows = alert_rows(json.loads(report_path.read_text()))
    tree_path = WORK / "sites-tree.json"
    tree = json.loads(tree_path.read_text()) if tree_path.exists() else None
    cov = coverage(tree) if tree is not None else None
    patterns = request_patterns(tree) if tree is not None else None
    session_status = os.environ.get("SESSION_STATUS", "")
    summary = markdown(rows, cov, session_status, patterns)
    (WORK / "zap-active-summary.md").write_text(summary)
    if cov is not None:
        (WORK / "zap-active-endpoints.txt").write_text("\n".join(cov["endpoints"]) + "\n")
        (WORK / "zap-active-query-patterns.md").write_text(query_pattern_markdown(patterns))
    print(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as handle:
            handle.write(summary)
    high = sum(1 for r in rows if r["risk"] == "3")
    if os.environ.get("ENFORCE") == "true" and high:
        print(f"::error::{high} high-risk alert type(s) found.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
