#!/usr/bin/env python3
"""Write the small test-only world-generation fixture with stdlib NBT encoding."""
import gzip
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
def string(value):
    data = value.encode(); return struct.pack('>H', len(data)) + data

def tag(kind, name, payload):
    return bytes([kind]) + string(name) + payload

def integer(name, value):
    return tag(3, name, struct.pack('>i', value))

def int_list(name, values):
    return tag(9, name, b'\x03' + struct.pack('>i', len(values)) + b''.join(struct.pack('>i', value) for value in values))

def compound_list(name, values):
    return tag(9, name, b'\x0a' + struct.pack('>i', len(values)) + b''.join(value + b'\x00' for value in values))

palette = [tag(8, 'Name', string('minecraft:grass_block'))]
blocks = [int_list('pos', [x, 0, z]) + integer('state', 0) for x in range(17) for z in range(17)]
root = integer('DataVersion', 4790) + int_list('size', [17, 24, 17]) + compound_list('palette', palette) + compound_list('blocks', blocks) + compound_list('entities', [])
target = ROOT / 'neoforge/src/gameTest/resources/data/ic2_tests/structure/world_room.nbt'
target.write_bytes(gzip.compress(tag(10, '', root + b'\x00'), mtime=0))
