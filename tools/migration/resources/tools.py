#!/usr/bin/env python3
"""Migrate tool models using the shared vanilla resource converter."""
from base import item, model, write
for identifier in ['wrench', 'electric_wrench', 'forge_hammer', 'cutter',
                   'drill', 'diamond_drill', 'iridium_drill',
                   'scanner', 'advanced_scanner', 'frequency_transmitter',
                   'mining_filter_card', 'iron_cutting_blade', 'steel_cutting_blade',
                   'diamond_cutting_blade']:
    item(identifier)

write("data/ic2/tags/item/toolbox_tools.json", {"values": ["ic2:" + tool for tool in ["wrench", "electric_wrench", "forge_hammer", "cutter", "treetap", "electric_treetap", "scanner", "advanced_scanner"]]})

for state in ['close', 'open']:
    model('ic2:item/tool/tool_box/' + state)
write('assets/ic2/items/tool_box.json', {'model': {'type': 'minecraft:range_dispatch',
    'property': 'ic2:tool_box_open', 'entries': [{'threshold': 1, 'model': {'type': 'minecraft:model', 'model': 'ic2:item/tool/tool_box/open'}}],
    'fallback': {'type': 'minecraft:model', 'model': 'ic2:item/tool/tool_box/close'}}})
