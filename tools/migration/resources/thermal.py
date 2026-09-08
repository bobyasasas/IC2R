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

for locale, label in [('en_us', 'Click fluid with a container'), ('zh_cn', '用光标上的容器点击流体')]:
    path = ASSETS + 'lang/' + locale + '.json'
    data = json.loads((NEW / path).read_text()); data['ic2.tank.cursor'] = label; write(path, data)

write('data/ic2/recipe/electrolyzing/water.json', {
    'type': 'ic2:electrolyzing', 'input': {'id': 'minecraft:water', 'amount': 40}, 'eu_per_tick': 32, 'ticks': 200,
    'outputs': [{'direction': 'down', 'fluid': {'id': 'ic2:hydrogen', 'amount': 26}}, {'direction': 'up', 'fluid': {'id': 'ic2:oxygen', 'amount': 13}}],
})
for locale, label in [('en_us', 'Adjacent tanks'), ('zh_cn', '相邻输出储罐')]:
    path = ASSETS + 'lang/' + locale + '.json'
    data = json.loads((NEW / path).read_text()); data['ic2.electrolyzer.tanks'] = label; write(path, data)

for locale, second, tick in [('en_us', 'Per second', 'Per tick'), ('zh_cn', '每秒', '每刻')]:
    path = ASSETS + 'lang/' + locale + '.json'
    data = json.loads((NEW / path).read_text()); data.update({'ic2.regulator.second': second, 'ic2.regulator.tick': tick}); write(path, data)
