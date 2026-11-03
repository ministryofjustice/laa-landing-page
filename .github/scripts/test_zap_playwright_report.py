import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import zap_playwright_report as report


class ZapReportTest(unittest.TestCase):
    def alert(self, url="http://localhost:43210/home?token=private"):
        return {
            "pluginId": "10020",
            "name": "Header | missing",
            "risk": "Medium",
            "confidence": "High",
            "url": url,
            "evidence": "private",
            "cookie": "private",
        }

    def test_only_local_application_traffic_is_included(self):
        self.assertTrue(report.application_url("http://localhost:43210/home"))
        for url in ("https://login.microsoftonline.com/", "http://localhost:8090/",
                    "http://localhost.evil:43210/", "http://localhost/"):
            self.assertFalse(report.application_url(url))

    def test_alert_export_removes_sensitive_fields_and_query(self):
        alert = report.sanitise_alert(self.alert())
        self.assertEqual(alert["url"], "http://localhost:43210/home")
        self.assertNotIn("private", json.dumps(alert))
        self.assertNotIn("evidence", alert)
        self.assertNotIn("cookie", alert)

    def test_summary_counts_instances_and_escapes_table_cells(self):
        alert = report.sanitise_alert(self.alert())
        text = report.markdown([alert, alert], 3)
        self.assertIn("| 0 | 2 | 0 | 0 |", text)
        self.assertIn("| Medium | 10020 | Header &#124; missing | 2 |", text)
        self.assertIn("3 application URLs", text)

    def test_empty_capture_fails_explicitly(self):
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(report, "OUTPUT", Path(directory)), \
                patch.object(report, "api", side_effect=[
                    {"recordsToScan": "0"}, {"urls": []}]):
            with self.assertRaisesRegex(RuntimeError, "captured no application URLs"):
                report.main()

    def test_passive_timeout_fails_explicitly(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "reports"
            summary = Path(directory) / "summary.md"
            with patch.object(report, "OUTPUT", output), \
                    patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": str(summary)}), \
                    patch.object(report.time, "monotonic", side_effect=[0, 601]), \
                    patch("builtins.print"), \
                    patch.object(report, "api", side_effect=[
                        {"recordsToScan": "42"},
                        {"currentTasks": []},
                        {"urls": ["http://localhost:43210/home"]},
                        {"alerts": [self.alert()]},
                        {"stats": {"stats.pscan.10020.time": "123"}},
                    ]):
                with self.assertRaisesRegex(RuntimeError, "42 records remain queued"):
                    report.main()
            self.assertIn("INCOMPLETE", summary.read_text())
            self.assertIn("42 records still queued", summary.read_text())
            self.assertEqual(len(json.loads((output / "report.json").read_text())), 1)

    def test_incomplete_empty_report_does_not_claim_no_alerts(self):
        text = report.markdown([], 1, remaining=5)
        self.assertIn("INCOMPLETE", text)
        self.assertNotIn("No passive alerts", text)

    def test_report_outputs_filter_external_alerts_and_wait_for_passive_scan(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "reports"
            summary = Path(directory) / "summary.md"
            with patch.object(report, "OUTPUT", output), \
                    patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": str(summary)}), \
                    patch.object(report.time, "sleep"), \
                    patch("builtins.print"), \
                    patch.object(report, "api", side_effect=[
                        {"recordsToScan": "1"},
                        {"currentTasks": []},
                        {"recordsToScan": "0"},
                        {"urls": ["http://localhost:43210/home"]},
                        {"alerts": [self.alert(), self.alert("https://external.example/")]},
                        {"stats": {"stats.pscan.10020.time": "123"}},
                    ]):
                report.main()
            alerts = json.loads((output / "report.json").read_text())
            self.assertEqual(len(alerts), 1)
            self.assertNotIn("private", (output / "report.json").read_text())
            self.assertEqual(summary.read_text(), (output / "report.md").read_text())
            self.assertIn("| 10020 | 0.123 |", summary.read_text())

    def test_running_tasks_do_not_export_paths_queries_or_extra_fields(self):
        task = {
            "name": "Slow rule",
            "url": "http://localhost:43210/admin/private?token=secret",
            "time": "12345",
            "cookie": "private",
        }
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(report, "OUTPUT", Path(directory)), \
                patch.object(report, "api", return_value={"currentTasks": [task]}), \
                patch("builtins.print"):
            report.record_tasks(4)
            text = (Path(directory) / "passive-tasks.jsonl").read_text()
        self.assertNotIn("private", text)
        self.assertNotIn("secret", text)
        self.assertEqual(json.loads(text)["tasks"], [{
            "rule": "Slow rule", "record_elapsed_ms": 12345, "host": "localhost",
        }])

    def test_rule_timings_are_sorted_and_exclude_non_timing_stats(self):
        with patch.object(report, "api", return_value={"stats": {
            "stats.pscan.10020.time": "123",
            "stats.pscan.10055.time": "4567",
            "stats.pscan.reqBodyTooBig": "5",
        }}):
            self.assertEqual(report.rule_timings(), [
                {"rule": "10055", "total_ms": 4567},
                {"rule": "10020", "total_ms": 123},
            ])


if __name__ == "__main__":
    unittest.main()
