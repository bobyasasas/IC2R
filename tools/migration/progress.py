#!/usr/bin/env python3
"""Validate the migration ledger and render its GitHub-readable status view."""
import argparse
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
LABELS = {"todo": "待开始", "in_progress": "进行中", "done": "已完成", "blocked": "受阻"}


def generate():
    plan = json.loads((ROOT / "docs/migration/plan.json").read_text())
    inventory = json.loads((ROOT / "docs/migration/legacy-inventory.json").read_text())
    assert plan["baseline"] == inventory["baseline"], "baseline mismatch"
    legacy = ROOT / "legacy/forge-1.20.1/src/main/java"
    originals = {f["source"] for f in inventory["files"]}
    assert originals == {p.relative_to(legacy).as_posix() for p in legacy.rglob("*.java")}, "legacy source set changed"
    for entry in inventory["files"]:
        assert hashlib.sha256((legacy / entry["source"]).read_bytes()).hexdigest() == entry["sha256"], entry["source"]
    resources = json.loads((ROOT / "docs/migration/resource-inventory.json").read_text())
    legacy_resources = ROOT / "legacy/forge-1.20.1/src/main/resources"
    assert resources["baseline"] == plan["baseline"]
    for entry in resources["files"]:
        assert hashlib.sha256((legacy_resources / entry["path"]).read_bytes()).hexdigest() == entry["sha256"], entry["path"]
    recipe_catalog = ROOT / "docs/migration/recipe-catalog.json"
    recipes = json.loads(recipe_catalog.read_text())["entries"] if recipe_catalog.exists() else []
    recipe_sources = set()
    for recipe in recipes:
        assert recipe["source"] not in recipe_sources, recipe["source"]
        recipe_sources.add(recipe["source"])
        assert (legacy_resources / "data/ic2/recipes" / recipe["source"]).is_file(), recipe["source"]
        assert recipe["status"] in {"converted", "pending"}, recipe
        if recipe["status"] == "converted":
            assert (ROOT / recipe["target"]).is_file(), recipe["target"]
        else:
            assert recipe.get("reason"), recipe
    if recipes:
        assert recipe_sources == {path.relative_to(legacy_resources / "data/ic2/recipes").as_posix() for path in (legacy_resources / "data/ic2/recipes").rglob("*.json")}
    tasks = {t["id"]: t for t in plan["tasks"]}
    assert len(tasks) == len(plan["tasks"]), "duplicate task ID"

    def visit(task_id, ancestors):
        assert task_id not in ancestors, "cyclic task dependency"
        for dependency in tasks[task_id]["depends_on"]:
            assert dependency in tasks, f"unknown dependency {dependency}"
            visit(dependency, ancestors | {task_id})

    for task in tasks.values():
        assert task["status"] in LABELS and task["acceptance"], task["id"]
        visit(task["id"], set())
        for evidence in task["evidence"]:
            assert (ROOT / evidence).is_file(), f"missing evidence {evidence}"
        if task["status"] == "done":
            assert task["evidence"], f"missing evidence for {task['id']}"
            assert all(tasks[d]["status"] == "done" for d in task["depends_on"]), task["id"]
    sources = set()
    completed_ports = 0
    for port in plan["ports"]:
        assert port["source"] in originals and port["source"] not in sources, port["source"]
        sources.add(port["source"])
        assert (ROOT / port["target"]).is_file(), port["target"]
        completed_ports += tasks[port["task"]]["status"] == "done"
    # Cheap source guard, in addition to core's empty production dependency classpath.
    for source in (ROOT / "core/src/main/java").rglob("*.java"):
        assert not re.search(r'\b(?:net\.minecraft|net\.neoforged|net\.minecraftforge|ic2\.neoforge)\b', source.read_text()), source
    catalog_path = ROOT / "docs/migration/registry-catalog.json"
    catalog = json.loads(catalog_path.read_text())["entries"] if catalog_path.exists() else []
    keys = set()
    for entry in catalog:
        key = (entry["registry"], entry["id"])
        assert key not in keys, f"duplicate registry entry {key}"
        keys.add(key)
        assert entry["decision"] in {"preserve", "replace", "remove"}
        assert entry["status"] in {"pending", "partial", "implemented"}
        if entry["status"] != "pending":
            assert entry.get("evidence"), key
            assert all((ROOT / e).is_file() for e in entry["evidence"]), key
    packages = json.loads((ROOT / "docs/migration/work-packages.json").read_text())["packages"]
    assert len({p["id"] for p in packages}) == len(packages)
    for package in packages:
        assert package["parent"] in tasks and package["status"] in LABELS and package["acceptance"]
        assert all((ROOT / e).is_file() for e in package["evidence"]), package["id"]
        if package["status"] == "done":
            assert package["evidence"], package["id"]
    done = sum(t["status"] == "done" for t in tasks.values())
    lines = ["# NeoForge 26.1.2 迁移进度", "", f"更新：{plan['updated']} · {plan['target']}", "",
             f"阶段完成：**{done} / {len(tasks)}**。完整迁移的旧 Java 文件：**{completed_ports} / {len(originals)}**。",
             "", "这些计数不表示功能完成率或工时进度。当前是迁移开发版本，旧机器与旧世界兼容性仍待验收。", "",
             "此页由 `plan.json` 生成；修改后运行 `python3 tools/migration/progress.py`。", "",
             "| 任务 | 状态 | 前置任务 | 验收标准 |", "|---|---|---|---|"]
    for task in tasks.values():
        lines.append(f"| {task['id']} {task['title']} | {LABELS[task['status']]} | {', '.join(task['depends_on']) or '—'} | {task['acceptance']} |")
    if catalog:
        lines += ["", "## 注册迁移覆盖", "", "| 注册类别 | 已实现 | 部分实现 | 基线总数 |", "|---|---:|---:|---:|"]
        for kind in dict.fromkeys(e["registry"] for e in catalog):
            entries = [e for e in catalog if e["registry"] == kind]
            lines.append(f"| {kind} | {sum(e['status'] == 'implemented' for e in entries)} | {sum(e['status'] == 'partial' for e in entries)} | {len(entries)} |")
        lines += ["", "清单包含 17 个流体族及其动态生成的 85 个实际注册 ID；流体族行是分组，不另算功能。完整状态见 [注册清单](registry-catalog.json)。"]
    if recipes:
        lines += ["", "## 配方迁移覆盖", "", f"已转换并纳入加载测试：**{sum(r['status'] == 'converted' for r in recipes)} / {len(recipes)}**。", "", "转换计数不等于生存模式可达率；原料、工具与前置机器仍需逐步验收。", "", "[逐条状态与待迁移原因](recipe-catalog.json)"]
    lines += ["", "## 后续工作包", "", "阶段内按可独立验收的功能族推进；进行中表示仍有验收项未完成。", "", "| 工作包 | 阶段 | 状态 | 验收范围 |", "|---|---|---|---|"]
    for package in packages:
        lines.append(f"| {package['id']} {package['title']} | {package['parent']} | {LABELS[package['status']]} | {package['acceptance']} |")
    lines += ["", "## 验证证据", ""]
    for task in tasks.values():
        if task["evidence"]:
            links = ", ".join(f"[{Path(p).name}](../../{p})" for p in task["evidence"])
            lines.append(f"- {task['id']}：{links}")
    lines += ["", "## 依赖关系", "", "```mermaid", "flowchart TD"]
    for task in tasks.values():
        lines.append(f'  {task["id"]}["{task["id"]} {task["title"]} · {LABELS[task["status"]]}"]')
        for dependency in task["depends_on"]:
            lines.append(f'  {dependency} --> {task["id"]}')
    lines += ["```", "", "[架构与工作约定](architecture.md) · [原始文件清单](legacy-inventory.json)", ""]
    return "\n".join(lines)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if the committed view is stale")
    args = parser.parse_args()
    rendered = generate()
    target = ROOT / "docs/migration/STATUS.md"
    if args.check:
        assert target.read_text() == rendered, "stale STATUS.md; run tools/migration/progress.py"
        print("Migration ledger, baseline, boundaries and generated status: OK")
    else:
        target.write_text(rendered)
        print(target.relative_to(ROOT))
