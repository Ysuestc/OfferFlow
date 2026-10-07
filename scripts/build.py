#!/usr/bin/env python3
"""Build the browser workspace and a single executable OfferFlow Jar."""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess

WORKSPACE = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--node", type=Path, help="Optional Node executable, without changing global installation")
    parser.add_argument("--npm-cli", type=Path, help="Optional npm-cli.js path when npm is not beside Node")
    args = parser.parse_args()
    node = args.node or Path(shutil.which("node") or "node")
    node = node.resolve(strict=True)
    version = subprocess.check_output([str(node), "--version"], text=True).strip()
    match = re.fullmatch(r"v(\d+)\.(\d+)\.(\d+)", version)
    if not match or tuple(map(int, match.groups())) < (22, 12, 0):
        parser.error("Node 22.12+ is required; Node 24 is recommended")
    npm_command = shutil.which("npm.cmd" if os.name == "nt" else "npm")
    npm_cli = args.npm_cli
    if npm_cli is None and npm_command:
        npm_path = Path(npm_command).resolve()
        npm_cli = (npm_path.parent / "node_modules/npm/bin/npm-cli.js") if os.name == "nt" else npm_path
    if npm_cli is None or not npm_cli.is_file():
        parser.error("npm is required; use --npm-cli for a custom installation")
    environment = os.environ.copy()
    environment["PATH"] = str(node.parent) + os.pathsep + environment.get("PATH", "")
    frontend = WORKSPACE / "frontend"
    for command in ("ci", "run"):
        options = ["ci", "--no-fund", "--no-audit"] if command == "ci" else ["run", "build"]
        subprocess.run([str(node), str(npm_cli)] + options, cwd=frontend, env=environment, check=True)
    entry = WORKSPACE / "target/frontend/static/index.html"
    if not entry.is_file():
        raise RuntimeError("Frontend build did not produce its entry")
    generated_static = WORKSPACE / "target/classes/static"
    if generated_static.exists():
        if generated_static.resolve() != generated_static or not generated_static.resolve().is_relative_to((WORKSPACE / "target").resolve()):
            raise RuntimeError("Generated resource boundary changed; refusing cleanup")
        shutil.rmtree(generated_static)
    maven = ["cmd.exe", "/d", "/c", ".\\mvnw.cmd"] if os.name == "nt" else ["./mvnw"]
    subprocess.run(maven + ["-B", "-ntp", "-Pfrontend", "verify"], cwd=WORKSPACE, env=environment, check=True)
    print("Built target/offerflow-0.1.0-SNAPSHOT.jar with browser workspace", flush=True)


if __name__ == "__main__":
    main()
