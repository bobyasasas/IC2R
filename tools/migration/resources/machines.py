#!/usr/bin/env python3
"""Reproducibly port passive cable and machine assets to vanilla model formats."""
import json
import re
from base import ROOT, OLD, NEW, ASSETS, write, copy, model, item

machines = ['steam_kinetic_generator', 'sorting_machine', 'magnetizer', 'trade_o_mat', 'item_buffer', 'blast_furnace', 'matter_generator', 'nuclear_reactor', 'reactor_chamber', 'reactor_fluid_port', 'reactor_access_hatch', 'reactor_redstone_port', 'rci_rsh', 'rci_lzh', 'uu_scanner', 'pattern_storage', 'teleporter', 'pump', 'miner', 'advanced_miner', 'personal_chest', 'wooden_storage_box', 'bronze_storage_box', 'iron_storage_box', 'steel_storage_box', 'iridium_storage_box', 'steam_generator', 'steam_repressurizer', 'rt_heat_generator', 'rt_generator', 'batbox_chargepad', 'cesu_chargepad', 'mfe_chargepad', 'mfsu_chargepad', 'condenser', 'fluid_regulator', 'electrolyzer', 'tank', 'liquid_heat_exchanger', 'fermenter', 'water_kinetic_generator', 'wind_kinetic_generator', 'manual_kinetic_generator', 'solid_heat_generator', 'fluid_heat_generator', 'electric_heat_generator', 'electric_kinetic_generator', 'stirling_generator', 'kinetic_generator', 'metal_former', 'ore_washing_plant', 'centrifuge', 'recycler', 'induction_furnace', 'water_generator', 'wind_generator', 'solar_generator', 'geo_generator', 'semifluid_generator', 'batbox', 'cesu', 'mfe', 'mfsu', 'lv_transformer', 'mv_transformer', 'hv_transformer', 'ev_transformer', 'canner', 'iron_furnace', 'generator', 'electric_furnace', 'macerator', 'extractor', 'compressor', 'block_cutter']
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
    for key, variant in list(data['variants'].items()):
        properties = dict(pair.split('=') for pair in key.split(',') if pair)
        if properties.get('facing') != 'north': continue
        for side, rotation in [('up', 270), ('down', 90)]:
            target = {**properties, 'facing': side}
            data['variants'].setdefault(','.join(f'{k}={v}' for k, v in target.items()), {**variant, 'x': rotation})
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

for language, text in [('en_us', 'Heat: %s / %s'), ('zh_cn', '热量：%s / %s')]:
    path = ASSETS + 'lang/' + language + '.json'
    data = json.loads((NEW / path).read_text())
    data['ic2.tooltip.processing_heat'] = text
    write(path, data)

# Legacy snow_layer is the modern snow item; duplicate aliases collapse here.
write('data/ic2/tags/item/recycler_blacklist.json', {'values': ['minecraft:glass_pane', 'minecraft:stick', 'minecraft:snowball', 'minecraft:snow']})
write('data/ic2/tags/item/recycler_whitelist.json', {'values': []})

copy(ASSETS + 'textures/item/rotor/iron_rotor_model.png')

for language, labels in [('en_us', {'ic2.wind.overload': 'Overspeed: %s%%', 'ic2.wind.obstructions': 'Obstructions: %s', 'ic2.water.nearby': 'Water blocks: %s', 'ic2.tooltip.generation': '%s EU/t'}), ('zh_cn', {'ic2.wind.overload': '超速负荷：%s%%', 'ic2.wind.obstructions': '遮挡方块：%s', 'ic2.water.nearby': '周围水块：%s', 'ic2.tooltip.generation': '%s EU/t'})]:
    path = ASSETS + 'lang/' + language + '.json'
    data = json.loads((NEW / path).read_text())
    data.update(labels)
    write(path, data)

for language, labels in [('en_us', {'ic2.work.heat': 'Stored: %s HU', 'ic2.work.heat_rate': '%s HU/t', 'ic2.work.kinetic': 'Stored: %s KU', 'ic2.work.kinetic_rate': '%s KU/t', 'ic2.work.voltage': 'Output: %s EU'}), ('zh_cn', {'ic2.work.heat': '储存：%s HU', 'ic2.work.heat_rate': '%s HU/t', 'ic2.work.kinetic': '储存：%s KU', 'ic2.work.kinetic_rate': '%s KU/t', 'ic2.work.voltage': '输出电压：%s EU'})]:
    path = ASSETS + 'lang/' + language + '.json'
    data = json.loads((NEW / path).read_text())
    data.update(labels)
    write(path, data)

for language, labels in [('en_us', {'ic2.manual.added': '+%s KU (%s / 1000)', 'ic2.manual.hungry': 'Eat before turning the crank.'}), ('zh_cn', {'ic2.manual.added': '+%s KU（%s / 1000）', 'ic2.manual.hungry': '需要先补充食物才能转动手柄。'})]:
    path = ASSETS + 'lang/' + language + '.json'
    data = json.loads((NEW / path).read_text())
    data.update(labels)
    write(path, data)
