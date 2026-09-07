#!/usr/bin/env python3
"""Port fluid sprites, native dynamic bucket models and classic cell definitions."""
import ast
import json
import re
from base import ROOT, OLD, NEW, ASSETS, write, copy, item, model

source = (ROOT / 'legacy/forge-1.20.1/src/main/java/ic2/core/ref/Ic2Fluids.java').read_text()
definitions = []
for name, args in re.findall(r'public static final EnvFluidHandler.FluidRefs (\w+) = create\((.*?)\);', source, re.S):
    definitions.append(ast.literal_eval('[' + args.replace('false', 'False').replace('true', 'True').replace('null', 'None') + ']'))
for definition in definitions:
    identifier, density, viscosity, luminosity, temperature, gaseous, still, flowing, color = definition
    for texture in {still + '_still', flowing + '_flow' if flowing else still + '_still'}:
        path = ASSETS + 'textures/block/fluid/' + texture + '.png'
        copy(path)
        if (OLD / (path + '.mcmeta')).exists(): copy(path + '.mcmeta')
    write(ASSETS + 'items/' + identifier + '_bucket.json', {'model': {'type': 'neoforge:fluid_container', 'fluid': 'ic2:' + identifier,
        'textures': {'base': 'minecraft:item/bucket', 'fluid': 'neoforge:item/mask/bucket_fluid'}, 'flip_gas': True}})
    # LiquidBlock renders through the fluid model; the empty block model supplies particles.
    block_model = 'block/fluid/' + identifier
    write(ASSETS + 'models/' + block_model + '.json', {'textures': {'particle': 'ic2:block/fluid/' + still + '_still'}})
    write(ASSETS + 'blockstates/fluid_block_' + identifier + '.json', {'variants': {'': {'model': 'ic2:' + block_model}}})

item('filled_tin_can')
for identifier in ['water_cell', 'lava_cell'] + [definition[0] + '_cell' for definition in definitions]:
    item(identifier)
# Generic cell tint is derived from its native fluid capability.
path = ASSETS + 'models/item/facade_cell.json'
empty = json.loads((OLD / path).read_text());empty.pop('overrides', None);write(path, empty)
for texture in empty['textures'].values():copy(ASSETS + 'textures/' + texture[4:] + '.png')
model(empty['parent'])
copy(ASSETS + 'textures/item/cell/fluid_cell_window.png')
write(ASSETS + 'items/facade_cell.json', {'model': {'type': 'neoforge:fluid_container', 'fluid': 'minecraft:empty',
    'textures': {'base': 'ic2:item/cell/facade_cell', 'fluid': 'ic2:item/cell/fluid_cell_window'}, 'flip_gas': False}})

for locale in ['en_us', 'zh_cn']:
    path = ASSETS + 'lang/' + locale + '.json'
    data = json.loads((NEW / path).read_text())
    for definition in definitions:
        identifier = definition[0]
        # Legacy translations use ic2.fluid.* rather than the modern registry description key.
        name = data.get('fluid.ic2.' + identifier) or data.get('ic2.fluid.' + identifier) or data.get('block.ic2.fluid_block_' + identifier)
        assert name, identifier
        data['fluid.ic2.' + identifier] = name
        data['item.ic2.' + identifier + '_bucket'] = name + ('桶' if locale == 'zh_cn' else ' Bucket')
    data['ic2.item.fluid_cell.contents'] = '%s单元' if locale == 'zh_cn' else '%s Cell'
    write(path, data)
