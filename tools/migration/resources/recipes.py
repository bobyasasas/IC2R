#!/usr/bin/env python3
"""Convert supported recipes and common item tags; record every deferred recipe explicitly."""
import json
from pathlib import Path
from base import ROOT, OLD, NEW, write

registered = {'ic2:' + p.stem for p in (NEW / 'assets/ic2/items').glob('*.json')}
recipe_root = OLD / 'data/ic2/recipes'
ledger = []
ledger_path = ROOT / 'docs/migration/recipe-catalog.json'
previous = json.loads(ledger_path.read_text())['entries'] if ledger_path.exists() else []
processing_types = {'ic2:macerator', 'ic2:extractor', 'ic2:compressor', 'ic2:metal_former_extruding', 'ic2:metal_former_rolling', 'ic2:metal_former_cutting'}
supported = processing_types | {'ic2:centrifuge', 'ic2:ore_washer', 'ic2:canner_bottle', 'ic2:canner_enrich', 'ic2:shaped', 'ic2:shapeless', 'minecraft:crafting_shaped', 'minecraft:crafting_shapeless', 'minecraft:smelting', 'minecraft:blasting', 'ic2:macerator', 'ic2:extractor', 'ic2:compressor'}


def common_tag(tag):
    if not tag.startswith('forge:'):
        return tag
    path = tag[6:]
    roots = {'stone': 'stones', 'sand': 'sands', 'cobblestone': 'cobblestones', 'glass': 'glass_blocks', 'slimeballs': 'slime_balls'}
    root, separator, suffix = path.partition('/')
    return 'c:' + roots.get(root, root) + separator + suffix


def ingredient(value):
    if isinstance(value, list):
        return {'neoforge:ingredient_type': 'neoforge:compound', 'children': [ingredient(entry) for entry in value]}
    if set(value) - {'item', 'tag', 'count'}:
        raise ValueError('ingredient requires component/fluid conversion: ' + repr(value))
    if 'item' in value:
        identifier = {'minecraft:chain': 'minecraft:iron_chain'}.get(value['item'], value['item'])
        if identifier.startswith('ic2:') and identifier not in registered:
            raise ValueError('unported item ' + identifier)
        if identifier == 'ic2:tool_box':
            return {'neoforge:ingredient_type': 'neoforge:components', 'items': identifier,
                    'components': {'ic2:toolbox_contents': []}}
        return identifier
    if 'tag' in value:
        return '#' + common_tag(value['tag'])
    raise ValueError('unsupported ingredient ' + repr(value))


def stack(value):
    if isinstance(value, str):
        value = {'item': value}
    identifier = {'minecraft:chain': 'minecraft:iron_chain'}.get(value['item'], value['item'])
    if identifier.startswith('ic2:') and identifier not in registered:
        raise ValueError('unported result ' + identifier)
    if 'nbt' in value:
        raise ValueError('NBT result requires component conversion')
    return {'id': identifier, 'count': value.get('count', 1)}


for file in sorted(recipe_root.rglob('*.json')):
    old = json.loads(file.read_text())
    relative = file.relative_to(recipe_root).as_posix()
    record = {'source': relative, 'type': old.get('type'), 'status': 'pending'}
    ledger.append(record)
    if old.get('type') not in supported:
        record['reason'] = 'recipe family not ported'
        continue
    try:
        if 'conditions' in old:
            raise ValueError('conditional recipe requires explicit condition conversion')
        if old['type'] in {'ic2:ore_washer', 'ic2:centrifuge'}:
            results = old['result'] if isinstance(old['result'], list) else [old['result']]
            new = {'type': old['type'], 'ingredient': ingredient(old['ingredient']), 'input_count': old['ingredient'].get('count', 1),
                   'results': [stack(result) for result in results]}
            new['water' if old['type'] == 'ic2:ore_washer' else 'min_heat'] = old['amount' if old['type'] == 'ic2:ore_washer' else 'minHeat']
        elif old['type'] == 'ic2:canner_bottle':
            def counted(value):
                return {'ingredient': ingredient(value), 'count': value.get('count', 1)}
            new = {'type': old['type'], 'container': counted(old['container_ingredient']), 'additive': counted(old['fill_ingredient']), 'result': stack(old['result'])}
        elif old['type'] == 'ic2:canner_enrich':
            def fluid(value):
                return {'id': value['fluid'], 'amount': value['amount']}
            additive = old['additive_ingredient']
            new = {'type': old['type'], 'input_fluid': fluid(old['input_ingredient']), 'additive': {'ingredient': ingredient(additive), 'count': additive.get('count', 1)}, 'result': fluid(old['result'])}
        elif old['type'] in processing_types:
            inputs = old['ingredient']
            results = old['result'] if isinstance(old['result'], list) else [old['result']]
            assert len(results) == 1 or old.get('weighted'), relative
            new = {'type': old['type'], 'ingredient': ingredient(inputs), 'input_count': inputs.get('count', 1),
                   'results': [{'stack': stack(result), 'weight': result.get('weight', 1)} for result in results]}
        else:
            new = {key: value for key, value in old.items() if key not in {'result', 'ingredient', 'ingredients', 'key'}}
            new['result'] = stack(old['result'])
            if 'ingredient' in old: new['ingredient'] = ingredient(old['ingredient'])
            if 'ingredients' in old: new['ingredients'] = [ingredient(value) for value in old['ingredients']]
            if 'key' in old: new['key'] = {key: ingredient(value) for key, value in old['key'].items()}
        write('data/ic2/recipe/' + relative, new)
        record['status'] = 'converted'
        record['target'] = 'neoforge/src/main/resources/data/ic2/recipe/' + relative
    except ValueError as error:
        record['reason'] = str(error)

for namespace in ['ic2', 'forge']:
    for file in sorted((OLD / ('data/' + namespace + '/tags/items')).rglob('*.json')):
        relative = file.relative_to(OLD / ('data/' + namespace + '/tags/items'))
        data = json.loads(file.read_text())
        values = []
        for value in data['values']:
            identifier = value if isinstance(value, str) else value['id']
            if identifier.startswith('#'):
                identifier = '#' + common_tag(identifier[1:])
                values.append({'id': identifier, 'required': False})
            elif identifier.startswith('ic2:') and identifier not in registered or not identifier.startswith(('minecraft:', 'ic2:')):
                values.append({'id': identifier, 'required': False})
            else:
                values.append(identifier)
        target_tag = common_tag(namespace + ':' + relative.as_posix().removesuffix('.json'))
        target_namespace, target_path = target_tag.split(':')
        write(f'data/{target_namespace}/tags/item/{target_path}.json', {'replace': False, 'values': values})

# Remove only outputs previously owned by this converter if a recipe becomes unsupported.
current_targets = {entry['target'] for entry in ledger if entry['status'] == 'converted'}
for entry in previous:
    target = entry.get('target')
    if target and target not in current_targets:
        path = (ROOT / target).resolve()
        assert path.is_relative_to((NEW / 'data/ic2/recipe').resolve()) and path.suffix == '.json'
        path.unlink(missing_ok=True)

ledger_path.write_text(json.dumps({'entries': ledger}, indent=2, ensure_ascii=False) + '\n')
from collections import Counter
print(Counter(entry['status'] for entry in ledger))
print(Counter(entry['type'] for entry in ledger if entry['status'] == 'converted'))

manifest = ROOT / 'neoforge/src/gameTest/resources/ic2_tests/converted-recipes.json'
manifest.parent.mkdir(parents=True, exist_ok=True)
manifest.write_text(json.dumps(['ic2:' + entry['source'].removesuffix('.json') for entry in ledger if entry['status'] == 'converted'], indent=2) + '\n')
