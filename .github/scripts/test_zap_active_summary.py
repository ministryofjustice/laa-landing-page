import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import zap_active_summary as summary


TREE = [{
    "node": "Sites",
    "children": [{
        "node": "http://localhost:8080",
        "url": "http://localhost:8080",
        "method": "GET",
        "statusCode": 302,
        "children": [
            {"node": "home", "url": "http://localhost:8080/home", "method": "GET", "statusCode": 200},
            {"node": "admin", "children": [
                {"node": "GET:users(page)", "url": "http://localhost:8080/admin/users?page=2&search=secret",
                 "method": "GET", "statusCode": 200},
                {"node": "manage", "url": "http://localhost:8080/admin/users/manage/"
                 "0f8fad5b-d9cb-469f-a165-70867728950e", "method": "GET", "statusCode": 200},
                {"node": "save", "url": "http://localhost:8080/admin/user/save()(email,firstName)",
                 "method": "POST", "data": "email=&firstName=", "statusCode": 302},
            ]},
        ],
    }],
}]

REPORT = {"site": [{"alerts": [
    {"pluginid": "10055", "alert": "CSP | inline", "riskcode": "2", "count": "3"},
    {"pluginid": "40018", "alert": "SQL Injection", "riskcode": "3", "count": "1"},
    {"pluginid": "10037", "alert": "X-Powered-By", "riskcode": "1", "instances": [{}, {}]},
]}]}


class ZapActiveSummaryTest(unittest.TestCase):
    def test_endpoints_drop_queries_and_fold_ids(self):
        cov = summary.coverage(TREE)
        self.assertEqual(cov["nodes"], 5)
        self.assertEqual(cov["endpoints"], [
            "GET /",
            "GET /admin/users",
            "GET /admin/users/manage/{id}",
            "GET /home",
            "POST /admin/user/save (email, firstName)",
        ])
        self.assertNotIn("secret", "\n".join(cov["endpoints"]))
        self.assertEqual(cov["areas"]["admin"], 3)
        self.assertEqual(cov["statuses"]["302"], 2)

    def test_alerts_report_instances_in_risk_order(self):
        rows = summary.alert_rows(REPORT)
        self.assertEqual([r["plugin"] for r in rows], ["40018", "10055", "10037"])
        self.assertEqual(rows[2]["instances"], 2)
        text = summary.markdown(rows, summary.coverage(TREE), "valid")
        self.assertIn("| Medium | 1 | 3 |", text)
        self.assertIn("CSP &#124; inline", text)
        self.assertIn("still valid", text)

    def test_query_patterns_aggregate_variants_without_values(self):
        tree = json.loads(json.dumps(TREE))
        tree[0]["children"][0]["children"][1]["children"].append({
            "node": "GET:users(page)",
            "url": "http://localhost:8080/admin/users?page=91&search=another-private-value",
            "method": "GET",
            "statusCode": 200,
        })
        patterns = summary.request_patterns(tree)
        key = ("GET", "/admin/users", ("page", "search"), ())
        self.assertEqual(patterns[key], 2)
        text = summary.query_pattern_markdown(patterns)
        self.assertIn("page, search", text)
        self.assertIn("| 2 |", text)
        self.assertNotIn("secret", text)
        self.assertNotIn("another-private-value", text)
        self.assertIn("not AJAX spider crawl-state counts", text)

    def test_lost_session_and_missing_coverage_are_flagged(self):
        text = summary.markdown([], None, "HTTP 302")
        self.assertIn("not valid after the scan (HTTP 302)", text)
        self.assertIn("coverage is unknown", text)

    def test_main_enforces_high_alerts_only_when_enabled(self):
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            (work / "zap-active-report.json").write_text(json.dumps(REPORT))
            (work / "sites-tree.json").write_text(json.dumps(TREE))
            with patch.object(summary, "WORK", work), patch("builtins.print"):
                self.assertEqual(summary.main(), 0)
                with patch.dict(os.environ, {"ENFORCE": "true"}, clear=True):
                    self.assertEqual(summary.main(), 1)
            self.assertIn("GET /home", (work / "zap-active-endpoints.txt").read_text())
            self.assertIn("page, search", (work / "zap-active-query-patterns.md").read_text())

    def test_missing_report_is_a_warning(self):
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(summary, "WORK", Path(directory)), patch("builtins.print"):
                self.assertEqual(summary.main(), 0)


if __name__ == "__main__":
    unittest.main()
