"""Regression checks for local startup; only loopback HTTP and temporary files are used."""
import io
import json
import os
from pathlib import Path
import sys
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import Mock, patch
import urllib.error
import urllib.request

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import run_local


def response(payload):
    return io.BytesIO(json.dumps(payload).encode("utf-8"))


class ReadinessTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.log = Path(self.directory.name) / "application.log"
        self.log.write_text("Tomcat started on port 8082 (http)", encoding="utf-8")
        self.app = Mock()
        self.app.poll.return_value = None
        self.database = Mock()
        self.database.poll.return_value = None

    def test_real_loopback_probe_bypasses_unavailable_proxy(self):
        class WorkspaceHandler(BaseHTTPRequestHandler):
            def do_GET(self):
                self.server.requested_path = self.path
                body = json.dumps({"data": {"available": True}}).encode()
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, *args):
                pass

        with ThreadingHTTPServer(("127.0.0.1", 0), WorkspaceHandler) as server:
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            port = server.server_port
            self.log.write_text(f"Tomcat started on port {port} (http)", encoding="utf-8")
            with patch.dict(os.environ, {"http_proxy": "http://127.0.0.1:1", "no_proxy": ""}), \
                    patch("urllib.request.proxy_bypass", return_value=False):
                try:
                    with self.assertRaises(urllib.error.URLError):
                        urllib.request.build_opener().open(f"http://127.0.0.1:{port}", timeout=1)
                    url = run_local.wait_for_application(self.app, self.database, self.log)
                    self.assertEqual(url, f"http://127.0.0.1:{port}")
                    self.assertEqual(server.requested_path, "/api/v1/workspace")
                    self.assertEqual(os.environ["http_proxy"], "http://127.0.0.1:1")
                finally:
                    server.shutdown()
                    thread.join(timeout=2)

    def test_temporary_connection_timeout_and_http_errors_recover(self):
        opener = Mock()
        opener.open.side_effect = [
            urllib.error.URLError(ConnectionRefusedError()),
            TimeoutError(),
            urllib.error.HTTPError("http://127.0.0.1:8082", 503, "not ready", None, None),
            response({"data": {"available": True}}),
        ]
        with patch.object(run_local.urllib.request, "build_opener", return_value=opener), \
                patch.object(run_local.time, "sleep") as sleep:
            self.assertEqual(run_local.wait_for_application(self.app, self.database, self.log),
                             "http://127.0.0.1:8082")
        self.assertEqual(opener.open.call_count, 4)
        self.assertEqual(sleep.call_count, 3)

    def test_continuous_refusal_has_deadline_and_safe_error(self):
        opener = Mock()
        opener.open.side_effect = urllib.error.URLError("private diagnostic must not leak")
        with patch.object(run_local.urllib.request, "build_opener", return_value=opener), \
                patch.object(run_local.time, "monotonic", side_effect=[0, 0, 0, 0, 1]), \
                patch.object(run_local.time, "sleep"):
            with self.assertRaisesRegex(RuntimeError, "readiness timed out; saved data retained") as failure:
                run_local.wait_for_application(self.app, self.database, self.log, timeout=1)
        self.assertNotIn("private diagnostic", str(failure.exception))
        self.assertEqual(opener.open.call_args.kwargs["timeout"], 1)

    def test_no_startup_log_also_has_deadline(self):
        self.log.write_text("Starting application", encoding="utf-8")
        with patch.object(run_local.time, "monotonic", side_effect=[0, 0, 0, 1]), \
                patch.object(run_local.time, "sleep"), \
                patch.object(run_local.urllib.request, "build_opener") as create_opener:
            with self.assertRaisesRegex(RuntimeError, "readiness timed out"):
                run_local.wait_for_application(self.app, self.database, self.log, timeout=1)
        create_opener.return_value.open.assert_not_called()

    def test_owned_process_failure_never_reports_ready(self):
        for process, message in [(self.app, "Application startup failed"),
                                 (self.database, "Owned MySQL exited")]:
            with self.subTest(message=message):
                process.poll.return_value = 1
                with self.assertRaisesRegex(RuntimeError, message):
                    run_local.wait_for_application(self.app, self.database, self.log)
                process.poll.return_value = None

    def test_false_or_invalid_business_responses_fail_safely(self):
        for payload, message in [({"data": {"available": False}}, "Business mode unavailable"),
                                 ({"data": {"available": "true"}}, "Business mode unavailable"),
                                 ([], "Invalid local workspace response"),
                                 ({"data": None}, "Invalid local workspace response")]:
            with self.subTest(payload=payload):
                opener = Mock()
                opener.open.return_value = response(payload)
                with patch.object(run_local.urllib.request, "build_opener", return_value=opener):
                    with self.assertRaisesRegex(RuntimeError, message):
                        run_local.wait_for_application(self.app, self.database, self.log)

    def test_malformed_json_has_no_raw_error(self):
        opener = Mock()
        opener.open.return_value = io.BytesIO(b"private non-JSON response")
        with patch.object(run_local.urllib.request, "build_opener", return_value=opener):
            with self.assertRaisesRegex(RuntimeError, "Invalid local workspace response") as failure:
                run_local.wait_for_application(self.app, self.database, self.log)
        self.assertNotIn("private non-JSON", str(failure.exception))

    def test_exit_during_probe_is_not_ready(self):
        opener = Mock()
        opener.open.return_value = response({"data": {"available": True}})
        self.app.poll.side_effect = [None, 1]
        with patch.object(run_local.urllib.request, "build_opener", return_value=opener):
            with self.assertRaisesRegex(RuntimeError, "Owned process exited"):
                run_local.wait_for_application(self.app, self.database, self.log)


if __name__ == "__main__":
    unittest.main()
