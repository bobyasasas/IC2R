#!/usr/bin/env python3
"""Validate a release tag and package the reobfuscated Forge JAR and tagged sources."""
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tomllib
import zipfile


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


def main():
    root = Path(__file__).resolve().parents[2]
    os.chdir(root)
    tag = os.environ["RELEASE_TAG"]
    if not re.fullmatch(r"2\.\d+\.\d+-ex120(?:-[A-Za-z0-9][A-Za-z0-9._-]*)?", tag):
        raise ValueError(f"Unsupported release tag: {tag}")
    version = re.search(r"^mod_version=(.+)$", Path("gradle.properties").read_text(), re.M)[1]
    if tag != version and not tag.startswith(version + "-"):
        raise ValueError(f"Tag {tag} does not match mod_version={version}")
    commit = git("rev-parse", f"refs/tags/{tag}^{{commit}}")
    if commit != git("rev-parse", "HEAD"):
        raise ValueError("Checkout does not match the release tag")
    notes = Path("docs/releases") / f"{tag}.md"
    if not notes.is_file():
        raise FileNotFoundError(notes)
    if "--check" in sys.argv:
        print(f"Validated {tag} at {commit}, mod version {version}")
        return

    built = Path("build/libs") / f"ic2-forge-{version}.jar"
    with zipfile.ZipFile(built) as jar:
        metadata = tomllib.loads(jar.read("META-INF/mods.toml").decode())
        if not any(m["modId"] == "ic2" and m["version"] == version for m in metadata["mods"]):
            raise ValueError("Built JAR has unexpected mod metadata")
        classes = [n for n in jar.namelist() if n.endswith(".class")]
        if not classes or any(int.from_bytes(jar.read(n)[6:8], "big") != 61 for n in classes):
            raise ValueError("Expected Java 17 class files")
    output = Path("dist")
    if output.exists():
        shutil.rmtree(output)
    output.mkdir()
    artifact = output / f"ic2-forge-{tag}.jar"
    shutil.copyfile(built, artifact)
    source = output / f"IC2R-{tag}-sources.zip"
    subprocess.run([
        "git", "archive", "--format=zip", f"--prefix=IC2R-{tag}/",
        f"--output={source}", commit,
    ], check=True)
    checksums = []
    for path in [artifact, source]:
        checksums.append(f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n")
    (output / "SHA256SUMS.txt").write_text("".join(checksums))
    print(f"Packaged {tag}: {len(classes)} Java 17 classes; source commit {commit}")


if __name__ == "__main__":
    main()
