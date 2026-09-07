#!/usr/bin/env python3
"""Validate packaged classes, development boundaries, item models and sound resources."""
import json
from pathlib import Path
import struct
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[2]
jars = [p for p in (ROOT / 'neoforge/build/libs').glob('*.jar') if not p.name.endswith('-sources.jar')]
assert len(jars) == 1, 'build one mod version from a clean output directory'
with zipfile.ZipFile(jars[0]) as jar:
    names = set(jar.namelist())
    assert len(names) == len(jar.namelist()), 'duplicate ZIP entries'
    metadata = tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode())
    assert [mod['modId'] for mod in metadata['mods']] == ['ic2']
    classes = [n for n in names if n.endswith('.class')]
    assert classes, 'no compiled classes'
    for name in classes:
        bytecode = jar.read(name)
        assert struct.unpack('>H', bytecode[6:8])[0] == 69, f'not Java 25: {name}'
        assert name.startswith(('ic2/core/', 'ic2/neoforge/')), f'unexpected class: {name}'
        assert not name.startswith('ic2/neoforge/test/'), f'development test leaked: {name}'
        assert b'net/minecraftforge/' not in bytecode, f'Forge reference: {name}'
        if name.startswith('ic2/core/'):
            assert not any(p in bytecode for p in [b'net/minecraft/', b'net/neoforged/', b'ic2/neoforge/']), name
        if not name.startswith('ic2/neoforge/client/'):
            assert b'net/minecraft/client/' not in bytecode, f'client dependency in common code: {name}'
    assert 'ic2/core/energy/ElectricalProfile.class' in names, 'core not embedded'
    assert not any(n.startswith(('data/ic2_tests/', 'legacy/')) for n in names)
    assert 'META-INF/mods.toml' not in names, 'legacy metadata leaked'

    visited = set()
    def check_model(identifier, ancestors=()):
        namespace, model = identifier.split(':') if ':' in identifier else ('minecraft', identifier)
        if namespace == 'minecraft':
            return
        assert identifier not in ancestors, f'cyclic model parent: {identifier}'
        if identifier in visited:
            return
        data = json.loads(jar.read(f'assets/{namespace}/models/{model}.json'))
        assert 'overrides' not in data, f'legacy predicates not migrated: {identifier}'
        if 'parent' in data:
            check_model(data['parent'], (*ancestors, identifier))
        for texture in data.get('textures', {}).values():
            if texture.startswith('#'):
                continue
            ns, name = texture.split(':') if ':' in texture else ('minecraft', texture)
            if ns != 'minecraft':
                assert jar.read(f'assets/{ns}/textures/{name}.png').startswith(b'\x89PNG\r\n\x1a\n')
        visited.add(identifier)

    def check_item_model(node):
        if isinstance(node, dict):
            if node.get('type') == 'minecraft:model':
                check_model(node['model'])
            if node.get('type') == 'neoforge:fluid_container':
                for material in node.get('textures', {}).values():
                    texture = material if isinstance(material, str) else material['sprite']
                    if texture.startswith('ic2:'):
                        assert jar.read(f'assets/ic2/textures/{texture[4:]}.png').startswith(b'\x89PNG\r\n\x1a\n')
            for value in node.values():
                check_item_model(value)
        elif isinstance(node, list):
            for value in node:
                check_item_model(value)

    def check_blockstate(node):
        if isinstance(node, dict):
            if 'model' in node:
                check_model(node['model'])
            for value in node.values():
                check_blockstate(value)
        elif isinstance(node, list):
            for value in node:
                check_blockstate(value)

    for name in names:
        if name.startswith('assets/ic2/blockstates/') and name.endswith('.json'):
            check_blockstate(json.loads(jar.read(name)))

    item_names = []
    for name in sorted(names):
        if name.startswith('assets/ic2/items/') and name.endswith('.json'):
            check_item_model(json.loads(jar.read(name)))
            item_names.append(name.removeprefix('assets/ic2/items/').removesuffix('.json'))
    for locale in ['en_us', 'zh_cn']:
        language = json.loads(jar.read(f'assets/ic2/lang/{locale}.json'))
        for item in item_names:
            assert language.get(f'item.ic2.{item}') or language.get(f'block.ic2.{item}'), f'{locale}: {item}'
    sounds = json.loads(jar.read('assets/ic2/sounds.json'))
    for event in sounds.values():
        for entry in event['sounds']:
            if isinstance(entry, dict) and entry.get('type') == 'event':
                continue
            sound = entry if isinstance(entry, str) else entry['name']
            namespace, name = sound.split(':')
            if namespace != 'minecraft':
                assert f'assets/{namespace}/sounds/{name}.ogg' in names, sound
print(f'{jars[0].name}: {len(classes)} Java 25 classes; {len(item_names)} item definitions; '
      f'{len(visited)} model dependencies; core, server boundaries and sounds OK')
