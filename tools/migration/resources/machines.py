#!/usr/bin/env python3
"""Reproducibly port passive cable and machine assets to vanilla model formats."""
import json
import re
from base import ROOT, OLD, NEW, ASSETS, write, copy, model, item

machines = ['metal_former', 'solar_generator', 'geo_generator', 'semifluid_generator', 'batbox', 'cesu', 'mfe', 'mfsu', 'lv_transformer', 'mv_transformer', 'hv_transformer', 'ev_transformer', 'canner', 'iron_furnace', 'generator', 'electric_furnace', 'macerator', 'extractor', 'compressor']
for identifier in machines:
    item(identifier)
    path = ASSETS + 'blockstates/' + identifier + '.json'
    data = json.loads((OLD / path).read_text())
    # Minecraft's model rotations are nonnegative quarter-turns.
    for variant in data['variants'].values():
        for axis in ['x', 'y']:
            if axis in variant: variant[axis] %= 360
        model(variant['model'])
    # Every MachineBlock now uses six directions; supply any old horizontal-only variants.
    for active in ['false', 'true']:
        key = 'facing=north,active=' + active
        if key not in data['variants']: continue
        for side, rotation in [('up', 270), ('down', 90)]:
            data['variants'].setdefault('facing=' + side + ',active=' + active, {**data['variants'][key], 'x': rotation})
    write(path, data)

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

legacy_blocks = (ROOT / 'legacy/forge-1.20.1/src/main/java/ic2/core/ref/Ic2Blocks.java').read_text()
for identifier in machines + [entry[0] for entry in cables]:
    drop = identifier
    if identifier in machines:
        declaration = re.search(r'public static final (?:Block|Ic2TileEntityBlock) ' + identifier.upper() + r'\s*=(.*?);', legacy_blocks, re.S).group(1)
        default_drop = re.search(r'DefaultDrop\.(\w+)', declaration).group(1)
        drop = {'Self': identifier, 'Machine': 'machine', 'AdvMachine': 'advanced_machine', 'Generator': 'generator'}[default_drop]
    entry = {'type': 'minecraft:item', 'name': 'ic2:' + identifier}
    if drop != identifier:
        entry = {'type': 'minecraft:alternatives', 'children': [
            {**entry, 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {'items': '#ic2:wrenches'}}]},
            {'type': 'minecraft:item', 'name': 'ic2:' + drop}]}
    write(f'data/ic2/loot_table/blocks/{identifier}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [entry], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': ['ic2:' + identifier for identifier in machines]})
for locale in ['en_us', 'zh_cn']:
    path = ASSETS + 'lang/' + locale + '.json'
    current, old = json.loads((NEW / path).read_text()), json.loads((OLD / path).read_text())
    for identifier in machines + [entry[0] for entry in cables]:
        key = 'block.ic2.' + identifier
        current[key] = old[key]
    write(path, current)

for locale, label in [('en_us', 'Sunlight: %s%%'), ('zh_cn', '日照：%s%%')]:
    path = ASSETS + 'lang/' + locale + '.json'
    data = json.loads((NEW / path).read_text())
    data['ic2.solar.sunlight'] = label
    write(path, data)
