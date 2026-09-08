#!/usr/bin/env python3
"""Move runtime-initialized thermal recipes into native datapack resources."""
import json
from base import NEW, ASSETS, write

write('data/ic2/recipe/fermenting/biomass.json', {
    'type': 'ic2:fermenting', 'input': {'id': 'ic2:biomass', 'amount': 20},
    'result': {'id': 'ic2:biogas', 'amount': 400}, 'heat': 4000, 'fertilizer_interval': 500,
})
for locale, label in [('en_us', 'Heat (HU)'), ('zh_cn', '热量（HU）')]:
    path = ASSETS + 'lang/' + locale + '.json'
    data = json.loads((NEW / path).read_text()); data['ic2.fermenter.heat'] = label; write(path, data)

for hot, cold in [('minecraft:lava', 'ic2:pahoehoe_lava'), ('ic2:hot_coolant', 'ic2:coolant')]:
    write('data/ic2/recipe/cooling/' + hot.split(':')[1] + '.json', {
        'type': 'ic2:cooling', 'input': {'id': hot, 'amount': 1},
        'result': {'id': cold, 'amount': 1}, 'heat': 20,
    })
