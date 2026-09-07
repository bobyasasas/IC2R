#!/usr/bin/env python3
"""Audit a recovered IC2 build against its source JAR without running either mod.

Run `bash gradlew build` first, then:
    python3 tools/recovery/verify.py /path/to/original.jar

Uses ASM already downloaded by ForgeGradle. Matching instruction listings are
strong evidence; differing listings require review, not automatic rejection.
Matching references/signatures alone does not prove behavioral equivalence.
"""
import collections
import difflib
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile


def resources_equal(name, left, right):
    if left.replace(b"\r\n", b"\n") == right.replace(b"\r\n", b"\n"):
        return True
    if name.endswith(".json"):
        try:
            return json.loads(left) == json.loads(right)
        except (ValueError, UnicodeError):
            pass
    return False


def main():
    repo = Path(__file__).resolve().parents[2]
    original = Path(sys.argv[1]).resolve()
    version = re.search(r"^mod_version=(.+)$", (repo / "gradle.properties").read_text(), re.M)[1]
    rebuilt = Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else repo / "build/libs" / f"ic2-forge-{version}.jar"
    output = repo / "build/recovery-audit"
    output.mkdir(parents=True, exist_ok=True)

    names = {}
    for line in (repo / "build/createMcpToSrg/output.tsrg").read_text().splitlines():
        if not line.startswith("\t") or line.startswith("\t\t"):
            continue
        parts = line.split()
        official, srg = parts[0], parts[-1]
        if re.fullmatch(r"[fm]_\d+_", srg):
            if srg in names and names[srg] != official:
                raise ValueError(f"Ambiguous mapping for {srg}")
            names[srg] = official
    mapping = output / "names.tsv"
    mapping.write_text("".join(f"{k}\t{v}\n" for k, v in sorted(names.items())))

    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    asm_root = gradle_home / "caches/modules-2/files-2.1/org.ow2.asm"
    modules = ["asm", "asm-tree", "asm-commons", "asm-util"]
    versions = set.intersection(*(set(p.name for p in (asm_root / m).iterdir() if p.is_dir()) for m in modules))
    asm_version = max(versions, key=lambda v: tuple(int(n) for n in re.findall(r"\d+", v)))
    jars = [next((asm_root / m / asm_version).rglob(f"{m}-{asm_version}.jar")) for m in modules]
    cp = os.pathsep.join(map(str, [output, *jars]))
    subprocess.run(["javac", "-cp", cp, "-d", str(output), str(Path(__file__).with_name("ClassAudit.java"))], check=True)
    for label, jar in [("original", original), ("rebuilt", rebuilt)]:
        if (output / label).exists():
            shutil.rmtree(output / label)
        subprocess.run(["java", "-cp", cp, "ClassAudit", str(mapping), str(jar), str(output / label)], check=True)

    def inventory(label, suffix):
        root = output / label
        return {p.relative_to(root).as_posix(): p.read_text() for p in root.rglob("*" + suffix)}

    left, right = inventory("original", ".asm"), inventory("rebuilt", ".asm")
    common = left.keys() & right.keys()
    different = sorted(n for n in common if left[n] != right[n])
    left_api, right_api = inventory("original", ".api"), inventory("rebuilt", ".api")
    reference_pattern = re.compile(r"^    (INVOKE(?:STATIC|VIRTUAL|INTERFACE|SPECIAL)|GET(?:STATIC|FIELD)|PUT(?:STATIC|FIELD)|LDC) ")

    def references(value):
        return collections.Counter(line.strip() for line in value.splitlines() if reference_pattern.match(line))

    report = {
        "original_sha256": hashlib.sha256(original.read_bytes()).hexdigest(),
        "rebuilt_sha256": hashlib.sha256(rebuilt.read_bytes()).hexdigest(),
        "original_classes": len(left),
        "rebuilt_classes": len(right),
        "identical_normalized_classes": len(common) - len(different),
        "different_normalized_classes": different,
        "missing_classes": sorted(left.keys() - right.keys()),
        "extra_classes": sorted(right.keys() - left.keys()),
        "declaration_differences": sorted(n for n in left_api.keys() & right_api.keys() if left_api[n] != right_api[n]),
        "reference_count_differences": sorted(n for n in different if references(left[n]) != references(right[n])),
    }
    with zipfile.ZipFile(original) as a, zipfile.ZipFile(rebuilt) as b:
        def noncode(z):
            return {n for n in z.namelist() if not n.endswith(("/", ".class"))}
        ra, rb = noncode(a), noncode(b)
        report.update({
            "original_resources": len(ra),
            "rebuilt_resources": len(rb),
            "missing_resources": sorted(ra - rb),
            "extra_resources": sorted(rb - ra),
            "different_resources": sorted(n for n in ra & rb if not resources_equal(n, a.read(n), b.read(n))),
        })
    (output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    with (output / "bytecode.diff").open("w") as f:
        for n in different:
            f.writelines(difflib.unified_diff(left[n].splitlines(True), right[n].splitlines(True), "original/" + n, "rebuilt/" + n))
    print(json.dumps(report, indent=2))
    failures = ["missing_classes", "extra_classes", "declaration_differences", "reference_count_differences", "missing_resources", "extra_resources", "different_resources"]
    return int(any(report[k] for k in failures))


if __name__ == "__main__":
    raise SystemExit(main())
