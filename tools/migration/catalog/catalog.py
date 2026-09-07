#!/usr/bin/env python3
"""Extract the immutable Forge registry declarations using the JDK's Java parser."""
import base64
from collections import Counter
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[3]
KINDS = {'Ic2Items': 'item', 'Ic2Blocks': 'block', 'Ic2BlockEntities': 'block_entity',
         'Ic2Entities': 'entity', 'Ic2ScreenHandlers': 'menu', 'Ic2SoundEvents': 'sound',
         'Ic2RecipeSerializers': 'recipe_serializer', 'Ic2RecipeTypes': 'recipe_type',
         'Ic2Fluids': 'fluid_family', 'Ic2GameEvents': 'game_event'}


def extract():
    sources = [ROOT / f'legacy/forge-1.20.1/src/main/java/ic2/core/ref/{name}.java' for name in KINDS]
    java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java'
    output = subprocess.check_output([java, str(Path(__file__).with_name('RegistryScanner.java')),
                                      *map(str, sources)], text=True)
    entries = []
    for line in output.splitlines():
        source, field, method, name, encoded = line.split('\t')
        entries.append({'registry': KINDS[Path(source).stem], 'id': f'ic2:{name}',
                        'field': field, 'source': str(Path(source).relative_to(ROOT)),
                        'factory': method, 'expression': base64.b64decode(encoded).decode(),
                        'decision': 'preserve', 'status': 'pending'})
    keys = [(e['registry'], e['id']) for e in entries]
    assert len(keys) == len(set(keys)), 'duplicate registry ID'
    return entries


if __name__ == '__main__':
    entries = extract()
    target = ROOT / 'docs/migration/registry-catalog.json'
    if target.exists():
        previous = {(e['registry'], e['id']): e for e in json.loads(target.read_text())['entries']}
        for entry in entries:
            old = previous.get((entry['registry'], entry['id']), {})
            for key in ['decision', 'status', 'evidence']:
                if key in old:
                    entry[key] = old[key]
    target.write_text(json.dumps({'baseline': '5d292712bf792ca67a7e1e519216dd7c694cbac9',
                                 'entries': entries}, ensure_ascii=False, indent=2) + '\n')
    print(dict(Counter(e['registry'] for e in entries)))
