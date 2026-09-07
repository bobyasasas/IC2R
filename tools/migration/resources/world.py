#!/usr/bin/env python3
"""Port world content, loot predicates and biome modifiers without changing generation weights."""
import json
from base import OLD, NEW, ASSETS, write, model, item

blocks = [prefix + metal + '_ore' for metal in ['lead', 'tin', 'uranium'] for prefix in ['', 'deepslate_']]
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
    item(identifier)
    path = ASSETS + 'blockstates/' + identifier + '.json'
    data = json.loads((OLD / path).read_text())
    for entry in data['variants'].values():
        for axis in ['x', 'y']:
            if axis in entry: entry[axis] %= 360
        model(entry['model'])
    write(path, data)
    path = 'data/ic2/loot_tables/blocks/' + identifier + '.json'
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
