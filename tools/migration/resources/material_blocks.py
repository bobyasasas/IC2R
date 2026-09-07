#!/usr/bin/env python3
"""Port inert block assets and retain mining-tool requirements."""
import json
import re
from base import ROOT, OLD, NEW, ASSETS, write, copy, model, item

source = (ROOT / 'neoforge/src/main/java/ic2/neoforge/registration/ModMaterialBlocks.java').read_text()
identifiers = re.findall(r'add\(result, "([a-z_]+)"', source)
for identifier in identifiers:
    item(identifier)
    path = ASSETS + 'blockstates/' + identifier + '.json'
    data = json.loads((OLD / path).read_text())
    write(path, data)
    for variant in data['variants'].values():
        model(variant['model'])
    write('data/ic2/loot_table/blocks/' + identifier + '.json', json.loads((OLD / ('data/ic2/loot_tables/blocks/' + identifier + '.json')).read_text()))
for locale in ['en_us', 'zh_cn']:
    path = ASSETS + 'lang/' + locale + '.json'
    data = json.loads((NEW / path).read_text())
    old = json.loads((OLD / path).read_text())
    data.update({'block.ic2.' + identifier: old['block.ic2.' + identifier] for identifier in identifiers})
    write(path, data)
registered = {'ic2:' + p.stem for p in (NEW / (ASSETS + 'blockstates')).glob('*.json')}
for namespace in ['minecraft', 'forge', 'ic2']:
    folder = OLD / ('data/' + namespace + '/tags/blocks')
    for file in folder.rglob('*.json'):
        data = json.loads(file.read_text())
        values = [value for value in data['values'] if isinstance(value, str) and value in registered]
        if not values: continue
        relative = file.relative_to(folder)
        target = f'data/{"c" if namespace == "forge" else namespace}/tags/block/{relative}'
        existing = json.loads((NEW / target).read_text()) if (NEW / target).exists() else {'values': []}
        write(target, {'replace': False, 'values': sorted(set(existing['values']) | set(values))})
