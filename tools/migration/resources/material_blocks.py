#!/usr/bin/env python3
"""Port inert block assets and retain mining-tool requirements."""
import json
import re
from base import ROOT, OLD, NEW, ASSETS, write, copy, model, item

source = (ROOT / 'neoforge/src/main/java/ic2/neoforge/registration/ModMaterialBlocks.java').read_text()
identifiers = re.findall(r'add\(result, "([a-z_]+)"', source)
# Miner support blocks share this inert-block pipeline. The tip keeps no item form
# and no legacy lang entry, and neither block has a legacy loot table, so breaking
# them drops nothing and recovery goes through the miner's withdraw mode.
miner_blocks = ['mining_pipe', 'mining_pipe_tip']
for identifier in identifiers + miner_blocks:
    if identifier not in miner_blocks or identifier == 'mining_pipe':
        item(identifier)
    path = ASSETS + 'blockstates/' + identifier + '.json'
    data = json.loads((OLD / path).read_text())
    write(path, data)
    for variant in data['variants'].values():
        model(variant['model'])
    loot = OLD / ('data/ic2/loot_tables/blocks/' + identifier + '.json')
    if loot.exists():
        write('data/ic2/loot_table/blocks/' + identifier + '.json', json.loads(loot.read_text()))
for locale in ['en_us', 'zh_cn']:
    path = ASSETS + 'lang/' + locale + '.json'
    data = json.loads((NEW / path).read_text())
    old = json.loads((OLD / path).read_text())
    data.update({'block.ic2.' + identifier: old['block.ic2.' + identifier]
                 for identifier in identifiers + ['mining_pipe']})
    write(path, data)
registered = {'ic2:' + p.stem for p in (NEW / (ASSETS + 'blockstates')).glob('*.json')}
# Follow tag references as well as direct block IDs. Dropping #forge:rubber_logs from
# minecraft:logs would make native leaves fail to recognize their supporting trunk.
definitions = {}
for namespace in ['minecraft', 'forge', 'ic2']:
    folder = OLD / ('data/' + namespace + '/tags/blocks')
    for file in folder.rglob('*.json'):
        key = namespace + ':' + file.relative_to(folder).as_posix().removesuffix('.json')
        definitions[key] = json.loads(file.read_text())['values']

def identifier(value):
    return value if isinstance(value, str) else value['id']

def contains_ported(key, ancestors=frozenset()):
    if key in ancestors: return False
    return any(identifier(value) in registered or
               identifier(value).startswith('#') and contains_ported(identifier(value)[1:], ancestors | {key})
               for value in definitions.get(key, []))

def common(value):
    return value.replace('forge:', 'c:', 1) if value.startswith(('forge:', '#forge:')) else value

for key, entries in definitions.items():
    if not contains_ported(key): continue
    values = [common(identifier(value)) for value in entries
              if identifier(value) in registered or
              identifier(value).startswith('#') and contains_ported(identifier(value)[1:])]
    namespace, relative = common(key).split(':')
    target = f'data/{namespace}/tags/block/{relative}.json'
    existing = json.loads((NEW / target).read_text()) if (NEW / target).exists() else {'values': []}
    write(target, {'replace': False, 'values': sorted(set(existing['values']) | set(values))})
