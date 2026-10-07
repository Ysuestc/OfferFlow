#!/usr/bin/env python3
"""Run database verification against an owned, temporary local MySQL instance."""
import argparse
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import socket
import subprocess
import tempfile
import time
import uuid
import urllib.request
import zipfile

WORKSPACE = Path(__file__).resolve().parents[1]
WINDOWS_FLAGS = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0


def controlled_environment():
    # External Spring settings must not redirect verification to a personal DB.
    return {key: value for key, value in os.environ.items()
            if not key.startswith("SPRING_")}


def verify_jar(root, database_url, username, password):
    candidates = list((WORKSPACE / "target").glob("offerflow-*.jar"))
    if len(candidates) != 1:
        raise RuntimeError("Expected exactly one built OfferFlow Jar")
    jar = candidates[0]
    with zipfile.ZipFile(jar) as archive:
        bundled_frontend = "BOOT-INF/classes/static/index.html" in archive.namelist()
        if any("BOOT-INF/lib/lombok-" in name or "Test.class" in name
               or "IT.class" in name for name in archive.namelist()):
            raise RuntimeError("Development classes found in production Jar")

    def check(mode, url, should_start):
        environment = controlled_environment()
        environment.update(DB_URL=url, DB_USERNAME=username, DB_PASSWORD=password)
        log_path = root / (mode + "-jar.log")
        command = ["java", "-jar", str(jar), f"--spring.profiles.active={mode}",
                   "--server.address=127.0.0.1", "--server.port=0",
                   "--spring.datasource.hikari.connection-timeout=1000",
                   "--spring.datasource.hikari.initialization-fail-timeout=1000"]
        with log_path.open("wb") as log:
            process = subprocess.Popen(command, cwd=WORKSPACE, env=environment,
                                       stdout=log, stderr=subprocess.STDOUT,
                                       creationflags=WINDOWS_FLAGS)
            try:
                if not should_start:
                    if process.wait(timeout=30) == 0:
                        raise RuntimeError("Unavailable database did not fail startup")
                    text = log_path.read_text(encoding="utf-8", errors="replace")
                    if "Communications link failure" not in text or "Started OfferFlowApplication" in text:
                        raise RuntimeError("Expected database connection failure was not observed")
                    print("PASS mysql Jar rejects unavailable database", flush=True)
                    return
                for _ in range(200):
                    text = log_path.read_text(encoding="utf-8", errors="replace")
                    port = re.search(r"Tomcat started on port (\d+)", text)
                    if port:
                        with urllib.request.urlopen(
                                f"http://127.0.0.1:{port.group(1)}/api/v1/health",
                                timeout=3) as response:
                            if response.status != 200 or json.load(response)["data"]["status"] != "UP":
                                raise RuntimeError("Unexpected Jar health response")
                        print(f"PASS {mode} Jar HTTP health", flush=True)
                        with urllib.request.urlopen(
                                f"http://127.0.0.1:{port.group(1)}/api/v1/workspace", timeout=3) as response:
                            if json.load(response)["data"]["available"] != (mode == "mysql"):
                                raise RuntimeError("Incorrect Jar workspace mode")
                        if bundled_frontend:
                            with urllib.request.urlopen(f"http://127.0.0.1:{port.group(1)}/", timeout=3) as response:
                                if response.status != 200 or "OfferFlow" not in response.read().decode("utf-8"):
                                    raise RuntimeError("Bundled frontend entry unavailable")
                            print(f"PASS {mode} Jar frontend and workspace mode", flush=True)
                        return
                    if process.poll() is not None:
                        raise RuntimeError(f"{mode} Jar startup failed")
                    time.sleep(0.1)
                raise RuntimeError(f"{mode} Jar startup timed out")
            finally:
                if process.poll() is None:
                    process.terminate()
                    process.wait(timeout=15)

    check("standalone", database_url, True)
    check("mysql", database_url, True)
    with socket.socket() as reserved:
        reserved.bind(("127.0.0.1", 0))
        # A bound, non-listening port cannot be taken by another database.
        check("mysql", f"jdbc:mysql://127.0.0.1:{reserved.getsockname()[1]}/unavailable", False)


def run_mysql(binary, port, sql):
    return subprocess.run(
        [str(binary), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
         f"--port={port}", "--user=root", "--password=", "--batch", "--skip-column-names"],
        input=sql, encoding="utf-8", capture_output=True, timeout=10,
        creationflags=WINDOWS_FLAGS,
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mysql-bin", required=True, type=Path,
                        help="Directory containing mysqld, mysql and mysqladmin")
    parser.add_argument("--probe", action="store_true",
                        help="Only verify isolated initialization/startup/cleanup")
    args = parser.parse_args()
    binary_dir = args.mysql_bin.resolve(strict=True)
    suffix = ".exe" if os.name == "nt" else ""
    binaries = {name: binary_dir / (name + suffix)
                for name in ("mysqld", "mysql", "mysqladmin")}
    for binary in binaries.values():
        if not binary.is_file():
            parser.error(f"Missing binary: {binary.name}")

    temp_parent = Path(tempfile.gettempdir()).resolve()
    root = Path(tempfile.mkdtemp(prefix="offerflow-mysql-it-")).resolve()
    data = root / "data"
    process = None
    owned_server_ready = False
    try:
        initialized = subprocess.run(
            [str(binaries["mysqld"]), "--no-defaults", "--initialize-insecure",
             f"--basedir={binary_dir.parent}", f"--datadir={data}"],
            capture_output=True, timeout=90, creationflags=WINDOWS_FLAGS,
        )
        if initialized.returncode:
            raise RuntimeError("Temporary MySQL initialization failed")
        with socket.socket() as sock:
            sock.bind(("127.0.0.1", 0))
            port = sock.getsockname()[1]

        server_args = [
            str(binaries["mysqld"]), "--no-defaults",
            f"--basedir={binary_dir.parent}", f"--datadir={data}",
            "--bind-address=127.0.0.1", f"--port={port}", "--mysqlx=0", "--skip-log-bin",
        ]
        if os.name == "nt":
            # MySQL's Windows monitor otherwise leaves a child on forced exit.
            server_args.extend(["--no-monitor", "--console"])
        with (root / "server.log").open("wb") as log:
            process = subprocess.Popen(
                server_args, stdout=log, stderr=subprocess.STDOUT,
                creationflags=WINDOWS_FLAGS,
            )
            for _ in range(120):
                if process.poll() is not None:
                    raise RuntimeError("Temporary MySQL exited before readiness")
                result = run_mysql(
                    binaries["mysql"], port, "SELECT VERSION(), @@datadir;"
                )
                if result.returncode == 0:
                    version, actual_data = result.stdout.strip().split("\t")
                    if Path(actual_data).resolve() != data.resolve():
                        raise RuntimeError("Refusing to use an unexpected server")
                    owned_server_ready = True
                    break
                time.sleep(0.25)
            else:
                raise RuntimeError("Temporary MySQL startup timed out")

            token = uuid.uuid4().hex
            db_name = f"offerflow_it_{token}"
            migration_db = db_name + "_migration"
            workspace_db = db_name + "_workspace"
            password = secrets.token_hex(24)
            setup = run_mysql(binaries["mysql"], port, f"""
                CREATE DATABASE {db_name} CHARACTER SET utf8mb4;
                CREATE DATABASE {migration_db} CHARACTER SET utf8mb4;
                CREATE DATABASE {workspace_db} CHARACTER SET utf8mb4;
                CREATE USER 'offerflow_test'@'127.0.0.1' IDENTIFIED BY '{password}';
                GRANT ALL ON {db_name}.* TO 'offerflow_test'@'127.0.0.1';
                GRANT ALL ON {migration_db}.* TO 'offerflow_test'@'127.0.0.1';
                GRANT ALL ON {workspace_db}.* TO 'offerflow_test'@'127.0.0.1';
            """)
            if setup.returncode:
                raise RuntimeError("Unable to prepare owned test schemas")
            print(f"Isolated MySQL {version} ready", flush=True)
            if args.probe:
                return 0
            test_env = controlled_environment()
            test_env.update({
                "OFFERFLOW_TEST_DB_URL":
                    f"jdbc:mysql://127.0.0.1:{port}/{db_name}",
                "OFFERFLOW_TEST_MIGRATION_DB_URL":
                    f"jdbc:mysql://127.0.0.1:{port}/{migration_db}",
                "OFFERFLOW_TEST_WORKSPACE_DB_URL":
                    f"jdbc:mysql://127.0.0.1:{port}/{workspace_db}",
                "OFFERFLOW_TEST_DB_USERNAME": "offerflow_test",
                "OFFERFLOW_TEST_DB_PASSWORD": password,
            })
            # Fixed arguments; the workspace path is passed as cwd, not shell text.
            command = (["cmd.exe", "/d", "/c", ".\\mvnw.cmd"]
                       if os.name == "nt" else ["./mvnw"])
            completed = subprocess.Popen(
                command + ["-B", "-ntp", "-Pmysql-integration", "verify"],
                cwd=WORKSPACE, env=test_env, creationflags=WINDOWS_FLAGS,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                encoding="utf-8", errors="replace",
            )
            for line in completed.stdout:
                print(line, end="", flush=True)
            code = completed.wait()
            if code == 0:
                verify_jar(root, test_env["OFFERFLOW_TEST_DB_URL"], "offerflow_test", password)
            return code
    finally:
        if process is not None and process.poll() is None:
            if owned_server_ready:
                subprocess.run(
                    [str(binaries["mysqladmin"]), "--no-defaults", "--protocol=TCP",
                     "--host=127.0.0.1", f"--port={port}", "--user=root", "--password=", "shutdown"],
                    capture_output=True, timeout=30, creationflags=WINDOWS_FLAGS,
                )
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.terminate()
                process.wait(timeout=10)
        # Cleanup only the exact directory created by this invocation.
        if root.parent != temp_parent or root.resolve() != root:
            raise RuntimeError("Temporary cleanup boundary changed; files retained")
        for attempt in range(10):
            try:
                shutil.rmtree(root)
                print("Owned MySQL stopped and temporary files removed", flush=True)
                break
            except PermissionError:
                if attempt == 9:
                    raise RuntimeError("Temporary cleanup failed; directory retained")
                time.sleep(0.2)


if __name__ == "__main__":
    raise SystemExit(main())
