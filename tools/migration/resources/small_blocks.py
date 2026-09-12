#!/usr/bin/env python3
"""Port the wind meter, refractory bricks and reinforced door assets.

The door reuses legacy's 32-variant blockstate verbatim: 26.1.2 vanilla door
blockstates also omit the powered property, and partial variant matching fills
it in. Both blocks drop themselves (the door only from its lower half).
"""
import json
import re
from base import ROOT, OLD, NEW, ASSETS, write, copy, model, item

source = (ROOT / 'neoforge/src/main/java/ic2/neoforge/registration/ModMaterialBlocks.java').read_text()
for identifier in ['refractory_bricks', 'reinforced_door']:
    assert 'registerBlock(\n                    "' + identifier in source or '"' + identifier in source, identifier
    item(identifier)
    path = ASSETS + 'blockstates/' + identifier + '.json'
    data = json.loads((OLD / path).read_text())
    write(path, data)
    for variant in data['variants'].values():
        model(variant['model'])
    loot = OLD / ('data/ic2/loot_tables/blocks/' + identifier + '.json')
    assert loot.exists(), identifier
    write('data/ic2/loot_table/blocks/' + identifier + '.json', json.loads(loot.read_text()))

# Electric hand tool: item model (parent ic2:item/tool/default) plus its texture.
item('wind_meter')

for locale in ['en_us', 'zh_cn']:
    path = ASSETS + 'lang/' + locale + '.json'
    data = json.loads((NEW / path).read_text())
    old = json.loads((OLD / path).read_text())
    data.update({key: old[key] for key in [
        'block.ic2.refractory_bricks',
        'block.ic2.reinforced_door',
        'item.ic2.wind_meter',
    ] if key in old})
    write(path, data)
