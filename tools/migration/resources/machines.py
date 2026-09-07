#!/usr/bin/env python3
"""Reproducibly port passive cable and first-machine assets to vanilla model formats."""
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


machines = ['generator', 'electric_furnace']
for identifier in machines:
    item(identifier)
    path = ASSETS + 'blockstates/' + identifier + '.json'
    copy(path)
    for variant in json.loads((OLD / path).read_text())['variants'].values():
        model(variant['model'])

cables = [('glass_fibre_cable', 'glass', 0, .25)]
for material, maximum, diameter in [('copper', 1, .25), ('gold', 2, .1875), ('iron', 3, .375), ('tin', 1, .25)]:
    for insulation in range(maximum + 1):
        prefix = ['', 'insulated_', 'double_insulated_', 'triple_insulated_'][insulation]
        cables.append((prefix + material + '_cable', material, insulation, diameter + .125 * insulation))
for identifier, material, insulation, diameter in cables:
    item(identifier)
    texture = f'ic2:block/wiring/cable/{material}_cable_{insulation}'
    # The legacy glass cable uses a colored texture even in its unpainted state.
    if not (OLD / (ASSETS + 'textures/' + texture[4:] + '.png')).exists():
        texture += '_light_gray'
    copy(ASSETS + 'textures/' + texture[4:] + '.png')
    lo, hi = (1 - diameter) * 8, (1 + diameter) * 8
    parts = []
    for side in ['center', 'down', 'up', 'north', 'south', 'west', 'east']:
        start, end = [lo] * 3, [hi] * 3
        if side in ['west', 'down', 'north']:
            axis = {'west': 0, 'down': 1, 'north': 2}[side]
            start[axis], end[axis] = 0, lo
        elif side != 'center':
            axis = {'east': 0, 'up': 1, 'south': 2}[side]
            start[axis], end[axis] = hi, 16
        name = f'block/cable/{identifier}_{side}'
        write(ASSETS + 'models/' + name + '.json', {'textures': {'cable': texture, 'particle': texture}, 'elements': [{'from': start, 'to': end, 'faces': {face: {'texture': '#cable'} for face in ['down', 'up', 'north', 'south', 'west', 'east']}}]})
        part = {'apply': {'model': 'ic2:' + name}}
        if side != 'center':
            part['when'] = {side: 'true'}
        parts.append(part)
    write(ASSETS + 'blockstates/' + identifier + '.json', {'multipart': parts})

for identifier in machines + [entry[0] for entry in cables]:
    write(f'data/ic2/loot_table/blocks/{identifier}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'ic2:' + identifier}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': ['ic2:' + identifier for identifier in machines]})
for locale in ['en_us', 'zh_cn']:
    path = ASSETS + 'lang/' + locale + '.json'
    current, old = json.loads((NEW / path).read_text()), json.loads((OLD / path).read_text())
    for identifier in machines + [entry[0] for entry in cables]:
        key = 'block.ic2.' + identifier
        current[key] = old[key]
    write(path, current)
