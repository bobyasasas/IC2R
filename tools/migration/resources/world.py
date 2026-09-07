#!/usr/bin/env python3
"""Port world content, loot predicates and biome modifiers without changing generation weights."""
import json
from base import OLD, NEW, ASSETS, write, copy, model, item

blocks = [prefix + metal + '_ore' for metal in ['lead', 'tin', 'uranium'] for prefix in ['', 'deepslate_']]
blocks += ['rubber_' + name for name in ['button', 'door', 'fence', 'fence_gate', 'pressure_plate', 'slab', 'stairs', 'trapdoor', 'sign', 'wall_sign']]
blocks += ['rubber_log', 'stripped_rubber_log', 'rubber_wood', 'stripped_rubber_wood', 'rubber_leaves', 'rubber_sapling', 'rubber_planks']


def loot(node):
    if isinstance(node, list):
        return [loot(value) for value in node]
    if not isinstance(node, dict):
        return node
    node = {key: loot(value) for key, value in node.items()}
    if node.get('condition') == 'minecraft:match_tool':
        predicate = node['predicate']
        if 'enchantments' in predicate:
            conditions = predicate.pop('enchantments')
            predicate.setdefault('predicates', {})['minecraft:enchantments'] = [
                {'enchantments': entry['enchantment'], 'levels': entry['levels']} for entry in conditions]
    return node


for identifier in blocks:
    if identifier != 'rubber_wall_sign': item(identifier)
    path = ASSETS + 'blockstates/' + identifier + '.json'
    data = json.loads((OLD / path).read_text())
    def blockstate_models(node):
        if isinstance(node, list):
            for child in node: blockstate_models(child)
        elif isinstance(node, dict):
            if 'model' in node:
                model(node['model'])
                for axis in ['x', 'y']:
                    if axis in node: node[axis] %= 360
            for child in node.values(): blockstate_models(child)
    blockstate_models(data)
    write(path, data)
    path = 'data/ic2/loot_tables/blocks/' + identifier + '.json'
    if (OLD / path).exists():
        write(path.replace('/loot_tables/', '/loot_table/'), loot(json.loads((OLD / path).read_text())))

write(ASSETS + 'items/rubber_leaves.json', {'model': {'type': 'minecraft:model', 'model': 'ic2:item/rubber_leaves', 'tints': [{'type': 'minecraft:constant', 'value': 6723908}]}})
for identifier in ['treetap', 'electric_treetap']:
    item(identifier)
def worldgen(node):
    if isinstance(node, list): return [worldgen(value) for value in node]
    if not isinstance(node, dict): return node
    node = {key: worldgen(value) for key, value in node.items()}
    if node.get('type') == 'minecraft:uniform' and isinstance(node.get('value'), dict):
        node.update(node.pop('value'))
    return node

for file in sorted((OLD / 'data/ic2/worldgen').rglob('*.json')):
    write(file.relative_to(OLD).as_posix(), worldgen(json.loads(file.read_text())))
for file in sorted((OLD / 'data/ic2/forge/biome_modifier').glob('*.json')):
    data = json.loads(file.read_text())
    data['type'] = data['type'].replace('forge:', 'neoforge:')
    data['biomes'] = data['biomes'].replace('#forge:', '#c:')
    write('data/ic2/neoforge/biome_modifier/' + file.name, data)

# Since 26.1 leaves use a dedicated support tag rather than minecraft:logs.
write("data/minecraft/tags/block/prevents_nearby_leaf_decay.json", {"replace": False, "values": ["#c:rubber_logs"]})

# Modern vanilla shape/interaction queries depend on these family tags.
for name, tags in {
    'button': ['wooden_buttons'], 'door': ['wooden_doors'],
    'fence': ['wooden_fences'], 'fence_gate': ['fence_gates'],
    'pressure_plate': ['wooden_pressure_plates'], 'slab': ['wooden_slabs'],
    'stairs': ['wooden_stairs'], 'trapdoor': ['wooden_trapdoors'],
}.items():
    for tag in tags:
        for kind in ['block', 'item']:
            write(f'data/minecraft/tags/{kind}/{tag}.json', {'replace': False, 'values': ['ic2:rubber_' + name]})

copy(ASSETS + 'textures/entity/signs/rubber.png')
for tag, name in [('standing_signs', 'rubber_sign'), ('wall_signs', 'rubber_wall_sign')]:
    write(f'data/minecraft/tags/block/{tag}.json', {'replace': False, 'values': ['ic2:' + name]})
write('data/minecraft/tags/item/signs.json', {'replace': False, 'values': ['ic2:rubber_sign']})

# Wall signs share the standing sign loot, but remain separate block IDs.
write('data/ic2/loot_table/blocks/rubber_wall_sign.json', json.loads((OLD / 'data/ic2/loot_tables/blocks/rubber_sign.json').read_text()))

# Fix recovered loot: double slabs return both halves, doors drop only once.
write('data/ic2/loot_table/blocks/rubber_slab.json', {
    'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
        'type': 'minecraft:item', 'name': 'ic2:rubber_slab', 'functions': [
            {'function': 'minecraft:set_count', 'count': 2, 'conditions': [
                {'condition': 'minecraft:block_state_property', 'block': 'ic2:rubber_slab', 'properties': {'type': 'double'}}]},
            {'function': 'minecraft:explosion_decay'}]}]}]})
write('data/ic2/loot_table/blocks/rubber_door.json', {
    'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
        'type': 'minecraft:item', 'name': 'ic2:rubber_door'}], 'conditions': [
            {'condition': 'minecraft:block_state_property', 'block': 'ic2:rubber_door', 'properties': {'half': 'lower'}},
            {'condition': 'minecraft:survives_explosion'}]}]})
