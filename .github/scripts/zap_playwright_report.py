"""Publish passive ZAP findings without exporting captured credentials or bodies."""

import html
import json
import os
import time
from collections import Counter
from pathlib import Path
from urllib.parse import urlencode, urlsplit, urlunsplit
from urllib.request import build_opener, ProxyHandler


API = "http://127.0.0.1:8090/JSON"
OUTPUT = Path("build/reports/zap")
CLIENT = build_opener(ProxyHandler({}))


def api(component, action, **parameters):
    url = f"{API}/{component}/view/{action}/?{urlencode(parameters)}"
    with CLIENT.open(url, timeout=30) as response:
        result = json.load(response)
    if "code" in result:
        raise RuntimeError(f"ZAP API error: {result}")
    return result


def application_url(url):
    parsed = urlsplit(url)
    return (
        parsed.scheme == "http"
        and parsed.hostname == "localhost"
        and parsed.port is not None
        and parsed.port != 8090
    )


def sanitise_alert(alert):
    parsed = urlsplit(alert["url"])
    return {
        "rule": alert["pluginId"],
        "name": alert["name"],
        "risk": alert["risk"],
        "confidence": alert["confidence"],
        "url": urlunsplit((parsed.scheme, parsed.netloc, parsed.path, "", "")),
    }


def markdown(alerts, url_count):
    counts = Counter(alert["risk"] for alert in alerts)
    lines = [
        "## ZAP passive scan of Playwright traffic",
        "",
        f"Captured **{url_count} application URLs** (including query variants).",
        "Passive analysis only: no active attacks or additional crawling.",
        "Coverage is limited to requests made by the Playwright browser.",
        "Findings are informational for this trial and do not fail the job.",
        "Existing deployed-scan rule exceptions are not applied.",
        "",
        "| High | Medium | Low | Informational |",
        "|---|---|---|---|",
        "| " + " | ".join(str(counts[risk]) for risk in
                         ("High", "Medium", "Low", "Informational")) + " |",
        "",
        "Counts represent alert instances, not unique vulnerabilities.",
        "",
        "| Risk | Rule | Finding | Instances |",
        "|---|---|---|---|",
    ]
    grouped = Counter((a["risk"], a["rule"], a["name"]) for a in alerts)
    ranks = {"High": 0, "Medium": 1, "Low": 2, "Informational": 3}
    for (risk, rule, name), count in sorted(
            grouped.items(), key=lambda item: (ranks[item[0][0]], item[0][1:])):
        safe_name = html.escape(name).replace("|", "&#124;").replace("\n", " ")
        lines.append(f"| {risk} | {rule} | {safe_name} | {count} |")
    if not alerts:
        lines.extend(["", "No passive alerts were reported for captured application traffic."])
    lines.extend([
        "",
        "Download the `playwright-zap-report` artifact for Markdown and sanitised JSON.",
        "Request/response bodies, cookies, evidence and URL queries are not exported.",
    ])
    return "\n".join(lines) + "\n"


def main():
    deadline = time.monotonic() + 180
    while int(api("pscan", "recordsToScan")["recordsToScan"]) != 0:
        if time.monotonic() >= deadline:
            raise RuntimeError("ZAP passive scanning did not finish within 180 seconds.")
        time.sleep(2)

    urls = [url for url in api("core", "urls")["urls"] if application_url(url)]
    if not urls:
        raise RuntimeError("ZAP captured no application URLs; check the Playwright proxy configuration.")

    alerts = []
    start = 0
    while True:
        batch = api("core", "alerts", start=start, count=500)["alerts"]
        alerts.extend(sanitise_alert(alert) for alert in batch if application_url(alert["url"]))
        if len(batch) < 500:
            break
        start += len(batch)

    report = markdown(alerts, len(urls))
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / "report.json").write_text(json.dumps(alerts, indent=2) + "\n", encoding="utf-8")
    (OUTPUT / "report.md").write_text(report, encoding="utf-8")
    print(report)
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
        summary.write(report)


if __name__ == "__main__":
    main()
