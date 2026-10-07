#!/usr/bin/env python3
"""Run a persistent, private local OfferFlow instance without using an existing MySQL service."""
import argparse
import json
import os
from pathlib import Path
import re
import secrets
import socket
import subprocess
import time
import urllib.request
import zipfile

WORKSPACE = Path(__file__).resolve().parents[1]
WINDOWS_FLAGS = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0


def lock_instance(path):
    stream = path.open("a+b")
    stream.seek(0)
    # Reading the locked byte fails on Windows before the non-blocking probe.
    if path.stat().st_size == 0:
        stream.write(b"0")
        stream.flush()
    stream.seek(0)
    try:
        if os.name == "nt":
            import msvcrt
            msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
        else:
            import fcntl
            fcntl.flock(stream.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        stream.close()
        raise RuntimeError("This local instance is already running")
    return stream


def mysql(binary, port, password, sql):
    environment = os.environ.copy()
    environment["MYSQL_PWD"] = password
    return subprocess.run([str(binary), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                           f"--port={port}", "--user=root", "--batch", "--skip-column-names"],
                          input=sql, encoding="utf-8", capture_output=True, env=environment,
                          timeout=10, creationflags=WINDOWS_FLAGS)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mysql-bin", type=Path, help="Required when starting the instance")
    parser.add_argument("--stop", action="store_true", help="Request graceful shutdown of the named owned instance")
    parser.add_argument("--port", type=int, default=8080, help="Loopback web port (0 selects a free port)")
    parser.add_argument("--instance", default="local", help="Separate ignored data directory name")
    args = parser.parse_args()
    if not re.fullmatch(r"[a-z][a-z0-9-]{0,30}", args.instance) or not 0 <= args.port <= 65535:
        parser.error("Invalid instance name or web port")
    parent = WORKSPACE / "private-data"
    root = parent / args.instance
    stop_request = root / "stop-request"
    if parent.resolve() != parent or root.resolve() != root or stop_request.resolve() != stop_request:
        raise RuntimeError("Private data boundary changed; refusing operation")
    if args.stop:
        if not root.is_dir():
            print("Local instance is not running; no data changed.", flush=True)
            return 0
        try:
            probe = lock_instance(root / ".run-lock")
        except RuntimeError:
            stop_request.write_text("stop", encoding="ascii")
            print("Graceful stop requested; saved data will be retained.", flush=True)
        else:
            probe.close()
            print("Local instance is not running; no data changed.", flush=True)
        return 0
    if args.mysql_bin is None:
        parser.error("--mysql-bin is required when starting")
    binary_dir = args.mysql_bin.resolve(strict=True)
    suffix = ".exe" if os.name == "nt" else ""
    server = binary_dir / ("mysqld" + suffix)
    client = binary_dir / ("mysql" + suffix)
    if not server.is_file() or not client.is_file():
        parser.error("mysql-bin must contain mysqld and mysql")
    jar = WORKSPACE / "target/offerflow-0.1.0-SNAPSHOT.jar"
    if not jar.is_file():
        parser.error("Build the application first: python scripts/build.py")
    with zipfile.ZipFile(jar) as archive:
        if "BOOT-INF/classes/static/index.html" not in archive.namelist():
            parser.error("Jar has no frontend; run scripts/build.py first")
    parent.mkdir(exist_ok=True)
    root.mkdir(exist_ok=True)
    # Reject links / junctions resolving outside the intended workspace-owned instance.
    if parent.resolve() != parent or root.resolve() != root:
        raise RuntimeError("Private data boundary changed; refusing startup")
    lock = lock_instance(root / ".run-lock")
    stop_request.unlink(missing_ok=True)
    data = root / "mysql-data"
    config_path = root / "credentials.json"
    database_process = None
    app_process = None
    ready = False
    root_password = ""
    try:
        if config_path.exists():
            config = json.loads(config_path.read_text(encoding="utf-8"))
            if not data.is_dir():
                raise RuntimeError("Local datadir is missing; existing credentials retained")
        else:
            if data.exists():
                raise RuntimeError("Unrecognized datadir; no files changed")
            initialized = subprocess.run([str(server), "--no-defaults", "--initialize-insecure",
                    f"--basedir={binary_dir.parent}", f"--datadir={data}"],
                    capture_output=True, timeout=90, creationflags=WINDOWS_FLAGS)
            if initialized.returncode:
                raise RuntimeError("Owned MySQL initialization failed; see retained data directory")
            config = {"rootPassword": secrets.token_hex(24), "appPassword": secrets.token_hex(24), "bootstrap": True}
            config_path.write_text(json.dumps(config), encoding="utf-8")
            if os.name != "nt":
                config_path.chmod(0o600)
        if data.resolve() != data:
            raise RuntimeError("Datadir boundary changed; refusing startup")
        root_password = config["rootPassword"]
        if not re.fullmatch(r"[a-f0-9]{48}", root_password) or not re.fullmatch(r"[a-f0-9]{48}", config["appPassword"]):
            raise RuntimeError("Unexpected local credential format")
        with socket.socket() as sock:
            sock.bind(("127.0.0.1", 0))
            db_port = sock.getsockname()[1]
        server_args = [str(server), "--no-defaults", f"--basedir={binary_dir.parent}", f"--datadir={data}",
                       "--bind-address=127.0.0.1", f"--port={db_port}", "--mysqlx=0"]
        if os.name == "nt":
            server_args.extend(["--no-monitor", "--console"])
        with (root / "mysql.log").open("wb") as database_log, (root / "application.log").open("wb") as app_log:
            database_process = subprocess.Popen(server_args, stdout=database_log, stderr=subprocess.STDOUT,
                                                 creationflags=WINDOWS_FLAGS)
            for _ in range(120):
                if database_process.poll() is not None:
                    raise RuntimeError("Owned MySQL startup failed; see private-data instance mysql.log")
                result = mysql(client, db_port, root_password, "SELECT @@datadir;")
                if result.returncode and config.get("bootstrap"):
                    result = mysql(client, db_port, "", "SELECT @@datadir;")
                    if result.returncode == 0:
                        root_password = ""
                if result.returncode == 0:
                    if Path(result.stdout.strip()).resolve() != data.resolve():
                        raise RuntimeError("Refusing to configure an unexpected database")
                    ready = True
                    break
                time.sleep(0.25)
            else:
                raise RuntimeError("Owned MySQL readiness timed out")
            if config.get("bootstrap"):
                setup = mysql(client, db_port, root_password, f"""
                    CREATE DATABASE IF NOT EXISTS offerflow CHARACTER SET utf8mb4;
                    CREATE USER IF NOT EXISTS 'offerflow_app'@'127.0.0.1' IDENTIFIED BY '{config["appPassword"]}';
                    ALTER USER 'offerflow_app'@'127.0.0.1' IDENTIFIED BY '{config["appPassword"]}';
                    GRANT ALL ON offerflow.* TO 'offerflow_app'@'127.0.0.1';
                    ALTER USER 'root'@'localhost' IDENTIFIED BY '{config["rootPassword"]}';
                """)
                if setup.returncode:
                    raise RuntimeError("Owned database bootstrap failed; data retained")
                root_password = config["rootPassword"]
                config["bootstrap"] = False
                temporary = root / "credentials.tmp"
                temporary.write_text(json.dumps(config), encoding="utf-8")
                if os.name != "nt":
                    temporary.chmod(0o600)
                temporary.replace(config_path)
            environment = {key: value for key, value in os.environ.items() if not key.startswith("SPRING_")}
            environment.update(DB_URL=f"jdbc:mysql://127.0.0.1:{db_port}/offerflow",
                               DB_USERNAME="offerflow_app", DB_PASSWORD=config["appPassword"])
            app_process = subprocess.Popen(["java", "-jar", str(jar), "--spring.profiles.active=mysql",
                    "--server.address=127.0.0.1", f"--server.port={args.port}"], cwd=WORKSPACE, env=environment,
                    stdout=app_log, stderr=subprocess.STDOUT, creationflags=WINDOWS_FLAGS)
            for _ in range(240):
                if app_process.poll() is not None:
                    raise RuntimeError("Application startup failed; see private-data instance application.log")
                log = (root / "application.log").read_text(encoding="utf-8", errors="replace")
                match = re.search(r"Tomcat started on port (\d+)", log)
                if match:
                    url = f"http://127.0.0.1:{match.group(1)}"
                    with urllib.request.urlopen(url + "/api/v1/workspace", timeout=3) as response:
                        if not json.load(response)["data"]["available"]:
                            raise RuntimeError("Business mode unavailable")
                    print("OfferFlow ready: " + url, flush=True)
                    print("Data retained in private-data/" + args.instance + ". Press Ctrl+C to stop.", flush=True)
                    break
                time.sleep(0.25)
            else:
                raise RuntimeError("Application readiness timed out")
            while app_process.poll() is None and database_process.poll() is None:
                if stop_request.exists():
                    stop_request.unlink()
                    print("Stopping owned processes; saved records are retained.", flush=True)
                    return 0
                time.sleep(0.5)
            raise RuntimeError("Owned application or database exited unexpectedly; data retained")
    except KeyboardInterrupt:
        print("Stopping owned processes; saved records are retained.", flush=True)
        return 0
    finally:
        if app_process is not None and app_process.poll() is None:
            app_process.terminate()
            app_process.wait(timeout=20)
        if database_process is not None and database_process.poll() is None:
            if ready:
                mysql(client, db_port, root_password, "SHUTDOWN;")
            try:
                database_process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                database_process.terminate()
                database_process.wait(timeout=10)
        lock.close()


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as exception:
        raise SystemExit(str(exception))
