#!/usr/bin/env python3
"""Port rotor item models, observer textures and explanatory UI text."""
import json
from base import OLD, NEW, ASSETS, item, copy, write

rotors = ['wooden_rotor', 'bronze_rotor', 'iron_rotor', 'steel_rotor', 'carbon_rotor']
for rotor in rotors:
    item(rotor)
    texture = rotor.replace('wooden_', 'wood_') + '_model'
    copy(ASSETS + 'textures/item/rotor/' + texture + '.png')
labels = {
    'en_us': {
        'ic2.rotor.wind_range': 'Wind range: %s–%s', 'ic2.rotor.wind_water': 'Fits wind and water turbines', 'ic2.rotor.wind_only': 'Fits wind turbines',
        'ic2.rotor.wind': 'Wind: %s', 'ic2.rotor.obstructions': 'Obstructed: %s', 'ic2.rotor.output': '%s KU/t', 'ic2.rotor.health': 'Rotor %s%%',
        'ic2.rotor.status.no_rotor': 'Insert a rotor', 'ic2.rotor.status.invalid_facing': 'Place horizontally', 'ic2.rotor.status.no_space': 'Rotor is blocked',
        'ic2.rotor.status.interference': 'Another turbine is too close', 'ic2.rotor.status.low_wind': 'Not enough wind', 'ic2.rotor.status.running': 'Running',
        'ic2.rotor.status.overloaded': 'Strong wind: faster wear', 'ic2.rotor.status.disabled': 'Disabled', 'ic2.rotor.status.invalid_biome': 'Needs ocean or river', 'ic2.rotor.status.no_flow': 'No water movement',
    },
    'zh_cn': {
        'ic2.rotor.wind_range': '风力范围：%s–%s', 'ic2.rotor.wind_water': '适用于风力和水力动能机', 'ic2.rotor.wind_only': '适用于风力动能机',
        'ic2.rotor.wind': '风力：%s', 'ic2.rotor.obstructions': '遮挡：%s', 'ic2.rotor.output': '%s KU/t', 'ic2.rotor.health': '寿命 %s%%',
        'ic2.rotor.status.no_rotor': '请安装转子', 'ic2.rotor.status.invalid_facing': '需要水平放置', 'ic2.rotor.status.no_space': '转子被阻挡',
        'ic2.rotor.status.interference': '另一台动能机过近', 'ic2.rotor.status.low_wind': '风力不足', 'ic2.rotor.status.running': '运行中',
        'ic2.rotor.status.overloaded': '强风：加快磨损', 'ic2.rotor.status.disabled': '已禁用', 'ic2.rotor.status.invalid_biome': '需要海洋或河流', 'ic2.rotor.status.no_flow': '水流静止',
    },
}
for locale, values in labels.items():
    path = ASSETS + 'lang/' + locale + '.json'
    data, old = json.loads((NEW / path).read_text()), json.loads((OLD / path).read_text())
    data.update({'item.ic2.' + rotor: old['item.ic2.' + rotor] for rotor in rotors})
    data.update(values)
    write(path, data)
