#!/usr/bin/env python3
"""Upgrade models use a component-driven range selector instead of legacy item predicates."""
import json
from base import OLD, ASSETS, write, copy, model, item

for name in ['overclocker', 'transformer', 'energy_storage', 'ejector', 'pulling', 'fluid_ejector', 'fluid_pulling']:
    identifier = name + '_upgrade'
    path = ASSETS + 'models/item/' + identifier + '.json'
    data = json.loads((OLD / path).read_text())
    if 'overrides' not in data:
        item(identifier)
        continue
    overrides = data.pop('overrides')
    write(path, data)
    model(data['parent'])
    for texture in data['textures'].values():
        copy(ASSETS + 'textures/' + texture[4:] + '.png')
    entries = []
    for number, entry in enumerate(overrides, 1):
        model(entry['model'])
        entries.append({'threshold': number, 'model': {'type': 'minecraft:model', 'model': entry['model']}})
    write(ASSETS + 'items/' + identifier + '.json', {'model': {
        'type': 'minecraft:range_dispatch', 'property': 'ic2:upgrade_direction', 'entries': entries,
        'fallback': {'type': 'minecraft:model', 'model': 'ic2:item/' + identifier}}})
