#!/usr/bin/env python3
"""Check packaging boundaries and the first migrated item's resource chain."""
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
        assert struct.unpack('>H', jar.read(name)[6:8])[0] == 69, f'not Java 25: {name}'
        assert name.startswith(('ic2/core/energy/', 'ic2/neoforge/')), f'unexpected class: {name}'
        assert not name.startswith('ic2/neoforge/test/'), f'development test leaked: {name}'
        assert b'net/minecraftforge/' not in jar.read(name), f'Forge reference: {name}'
    assert 'ic2/core/energy/ElectricalProfile.class' in names, 'core not embedded'
    assert not any(n.startswith(('data/ic2_tests/', 'legacy/')) for n in names)
    assert 'META-INF/mods.toml' not in names, 'legacy metadata leaked'
    item = json.loads(jar.read('assets/ic2/items/copper_plate.json'))
    assert item['model']['type'] == 'minecraft:model'
    namespace, model = item['model']['model'].split(':')
    model_data = json.loads(jar.read(f'assets/{namespace}/models/{model}.json'))
    namespace, texture = model_data['textures']['layer0'].split(':')
    assert jar.read(f'assets/{namespace}/textures/{texture}.png').startswith(b'\x89PNG\r\n\x1a\n')
    for locale in ['en_us', 'zh_cn']:
        assert json.loads(jar.read(f'assets/ic2/lang/{locale}.json'))['item.ic2.copper_plate']
print(f'{jars[0].name}: {len(classes)} Java 25 classes; core embedded; tests excluded; copper plate resources OK')
