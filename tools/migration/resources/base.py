#!/usr/bin/env python3
"""Shared resource conversion helpers; importing this module does not modify files."""
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[3]
OLD = ROOT / 'legacy/forge-1.20.1/src/main/resources'
NEW = ROOT / 'neoforge/src/main/resources'
ASSETS = 'assets/ic2/'


def write(path, data):
    target = NEW / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')


def copy(path):
    target = NEW / path
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(OLD / path, target)


def model(identifier):
    if not identifier.startswith('ic2:'):
        return
    path = ASSETS + 'models/' + identifier[4:] + '.json'
    data = json.loads((OLD / path).read_text())
    assert 'loader' not in data and 'overrides' not in data, path
    write(path, data)
    if 'parent' in data:
        model(data['parent'])
    for texture in data.get('textures', {}).values():
        if texture.startswith('ic2:'):
            copy(ASSETS + 'textures/' + texture[4:] + '.png')


def item(identifier):
    model('ic2:item/' + identifier)
    write(ASSETS + 'items/' + identifier + '.json', {'model': {'type': 'minecraft:model', 'model': 'ic2:item/' + identifier}})


