#!/usr/bin/env python3
"""Cross-version save inventory / id-conversion tool (P18).

Operates on a COPY of a save directory (never in place) and walks the parts
of a Minecraft world save that can carry IC2 content:

  * ``level.dat`` / ``level.dat_old`` / ``playerdata`` (``players/``) ``*.dat``
  * every ``region/r.*.mca`` (block palettes, block entities, in-chunk entities)
  * every ``entities/r.*.mca`` (non-ticking entity storage)

The NBT layer is a byte-exact round trip: untouched files are copied verbatim
and untouched chunks inside a rewritten region file keep their original
compressed payload bytes, so only chunks whose ic2 ids were remapped change.

Subcommands:
  scan       inventory every ic2-namespaced name in the save (read only)
  convert    rewrite ids per save_id_map.json into a destination copy
  self-test  build a synthetic legacy-format save fixture and assert both paths

No third-party dependencies; Python 3.11+ stdlib only.
"""

from __future__ import annotations

import argparse
import copy
import glob
import gzip
import io
import json
import os
import re
import shutil
import struct
import sys
import tempfile
import zlib

IC2_ID = re.compile(r"^ic2:[a-z0-9_./-]+$")
ANY_ID = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
ITEM_STACK_HINTS = ("count", "Count", "components", "tag")

# ---------------------------------------------------------------------------
# NBT layer
# ---------------------------------------------------------------------------


class _Reader:
    def __init__(self, data: bytes) -> None:
        self.data = data
        self.pos = 0

    def byte(self) -> int:
        value = self.data[self.pos]
        self.pos += 1
        return value

    def take(self, n: int) -> bytes:
        value = self.data[self.pos : self.pos + n]
        self.pos += n
        return value

    def short(self) -> int:
        return struct.unpack(">h", self.take(2))[0]

    def int_(self) -> int:
        return struct.unpack(">i", self.take(4))[0]

    def long_(self) -> int:
        return struct.unpack(">q", self.take(8))[0]

    def float_(self) -> float:
        return struct.unpack(">f", self.take(4))[0]

    def double(self) -> float:
        return struct.unpack(">d", self.take(8))[0]

    def string(self) -> str:
        length = struct.unpack(">H", self.take(2))[0]
        return self.take(length).decode("utf-8", "surrogateescape")


class _Writer:
    def __init__(self) -> None:
        self.buffer = io.BytesIO()

    def byte(self, value: int) -> None:
        self.buffer.write(struct.pack(">B", value & 0xFF))

    def take(self, value: bytes) -> None:
        self.buffer.write(value)

    def short(self, value: int) -> None:
        self.buffer.write(struct.pack(">h", value))

    def int_(self, value: int) -> None:
        self.buffer.write(struct.pack(">i", value))

    def long_(self, value: int) -> None:
        self.buffer.write(struct.pack(">q", value))

    def float_(self, value: float) -> None:
        self.buffer.write(struct.pack(">f", value))

    def double(self, value: float) -> None:
        self.buffer.write(struct.pack(">d", value))

    def string(self, value: str) -> None:
        raw = value.encode("utf-8", "surrogateescape")
        self.buffer.write(struct.pack(">H", len(raw)))
        self.buffer.write(raw)

    def getvalue(self) -> bytes:
        return self.buffer.getvalue()


class ByteArray:
    """TAG_Byte_Array wrapper (keeps the payload byte-exact)."""

    def __init__(self, data: bytes) -> None:
        self.data = data

    def __eq__(self, other: object) -> bool:
        return isinstance(other, ByteArray) and other.data == self.data

    def __repr__(self) -> str:
        return f"ByteArray({len(self.data)} bytes)"


class IntArray:
    def __init__(self, values: list[int]) -> None:
        self.values = values

    def __eq__(self, other: object) -> bool:
        return isinstance(other, IntArray) and other.values == self.values


class LongArray:
    def __init__(self, values: list[int]) -> None:
        self.values = values

    def __eq__(self, other: object) -> bool:
        return isinstance(other, LongArray) and other.values == self.values


class NbtList(list):
    """List carrying its element tag type so empty lists round-trip."""

    def __init__(self, element_tag: int = 0, values: list = ()) -> None:
        super().__init__(values)
        self.element_tag = element_tag


# Tagged scalars: a plain Python int cannot tell TAG_Int from TAG_Long (level.dat
# RandomSeed/LastPlayed are longs), so the reader wraps every scalar in a subclass
# that remembers its NBT tag. They still behave as int/float for comparisons.
class NbtByte(int):
    tag = 1


class NbtShort(int):
    tag = 2


class NbtInt(int):
    tag = 3


class NbtLong(int):
    tag = 4


class NbtFloat(float):
    tag = 5


class NbtDouble(float):
    tag = 6


def _read_payload(reader: _Reader, tag: int):
    if tag == 1:
        return NbtByte(reader.byte())
    if tag == 2:
        return NbtShort(reader.short())
    if tag == 3:
        return NbtInt(reader.int_())
    if tag == 4:
        return NbtLong(reader.long_())
    if tag == 5:
        return NbtFloat(reader.float_())
    if tag == 6:
        return NbtDouble(reader.double())
    if tag == 7:
        return ByteArray(reader.take(reader.int_()))
    if tag == 8:
        return reader.string()
    if tag == 9:
        element_tag = reader.byte()
        length = reader.int_()
        if length < 0:
            length = 0
        return NbtList(element_tag, (_read_payload(reader, element_tag) for _ in range(length)))
    if tag == 10:
        compound = {}
        while True:
            inner = reader.byte()
            if inner == 0:
                return compound
            name = reader.string()
            compound[name] = _read_payload(reader, inner)
    if tag == 11:
        count = reader.int_()
        return IntArray([reader.int_() for _ in range(count)])
    if tag == 12:
        count = reader.int_()
        return LongArray([reader.long_() for _ in range(count)])
    raise ValueError(f"unsupported NBT tag {tag}")


def _write_payload(writer: _Writer, tag: int, value) -> None:
    if tag == 1:
        writer.byte(value)
    elif tag == 2:
        writer.short(value)
    elif tag == 3:
        writer.int_(value)
    elif tag == 4:
        writer.long_(value)
    elif tag == 5:
        writer.float_(value)
    elif tag == 6:
        writer.double(value)
    elif tag == 7:
        assert isinstance(value, ByteArray)
        writer.int_(len(value.data))
        writer.take(value.data)
    elif tag == 8:
        writer.string(value)
    elif tag == 9:
        assert isinstance(value, NbtList)
        writer.byte(value.element_tag)
        writer.int_(len(value))
        for element in value:
            _write_payload(writer, value.element_tag, element)
    elif tag == 10:
        assert isinstance(value, dict)
        for name, element in value.items():
            writer.byte(_tag_of(element))
            writer.string(name)
            _write_payload(writer, _tag_of(element), element)
        writer.byte(0)
    elif tag == 11:
        assert isinstance(value, IntArray)
        writer.int_(len(value.values))
        for element in value.values:
            writer.int_(element)
    elif tag == 12:
        assert isinstance(value, LongArray)
        writer.int_(len(value.values))
        for element in value.values:
            writer.long_(element)
    else:
        raise ValueError(f"unsupported NBT tag {tag}")


def _tag_of(value) -> int:
    tag = getattr(value, "tag", None)
    if isinstance(tag, int) and 1 <= tag <= 12:
        return tag
    if isinstance(value, bool):
        return 1
    if isinstance(value, int):
        return 3  # lenient default for hand-built fixtures
    if isinstance(value, float):
        return 6
    if isinstance(value, ByteArray):
        return 7
    if isinstance(value, str):
        return 8
    if isinstance(value, NbtList):
        return 9
    if isinstance(value, dict):
        return 10
    if isinstance(value, IntArray):
        return 11
    if isinstance(value, LongArray):
        return 12
    raise ValueError(f"value without a tag: {value!r}")


def nbt_load_bytes(raw: bytes) -> dict:
    """Parse an uncompressed root compound (TAG_Compound, empty name)."""
    reader = _Reader(raw)
    tag = reader.byte()
    if tag != 10:
        raise ValueError(f"expected a root compound, got tag {tag}")
    reader.string()
    return _read_payload(reader, 10)


def nbt_dump_bytes(root: dict) -> bytes:
    writer = _Writer()
    writer.byte(10)
    writer.string("")
    _write_payload(writer, 10, root)
    return writer.getvalue()


def nbt_load(raw: bytes) -> dict:
    """Parse gzip- or zlib-wrapped NBT (level.dat / playerdata / chunk payloads)."""
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    else:
        raw = zlib.decompress(raw)
    return nbt_load_bytes(raw)


def nbt_dump(root: dict, compression: int) -> bytes:
    raw = nbt_dump_bytes(root)
    if compression == 1:
        return gzip.compress(raw, mtime=0)
    return zlib.compress(raw)


# ---------------------------------------------------------------------------
# Anvil region layer
# ---------------------------------------------------------------------------

REGION_HEADER = 8192
SECTOR = 4096


class Chunk:
    def __init__(self, index: int, root: dict, compression: int, raw_payload: bytes) -> None:
        self.index = index
        self.root = root
        self.compression = compression
        self.raw_payload = raw_payload  # original compressed bytes, kept byte-exact

    @property
    def modified(self) -> bool:
        return self.raw_payload is None


class RegionData:
    def __init__(self, chunks: dict[int, Chunk | None], timestamps: bytes) -> None:
        self.chunks = chunks
        self.timestamps = timestamps  # raw 4096-byte timestamp sector


def _decompress(payload: bytes, compression: int) -> dict:
    if compression == 2:
        raw = zlib.decompress(payload)
    elif compression == 1:
        raw = gzip.decompress(payload)
    else:
        raise ValueError(f"unsupported chunk compression byte {compression}")
    return nbt_load_bytes(raw)


def read_region(path: str) -> RegionData:
    """Parse a region file; None marks an external (unsupported) chunk slot."""
    chunks: dict[int, Chunk | None] = {}
    with open(path, "rb") as handle:
        header = handle.read(REGION_HEADER)
    if len(header) < REGION_HEADER:
        return RegionData(chunks, b"\0" * 4096)
    locations = header[:4096]
    timestamps = header[4096:]
    with open(path, "rb") as handle:
        for index in range(1024):
            entry = locations[index * 4 : index * 4 + 4]
            offset = int.from_bytes(entry[:3], "big")
            sectors = entry[3]
            timestamp = struct.unpack(">I", timestamps[index * 4 : index * 4 + 4])[0]
            if offset == 0 and sectors == 0:
                if timestamp:
                    # External chunk storage: the payload lives in c.<x>.<z>.mca.
                    chunks[index] = None
                continue
            handle.seek(offset * SECTOR)
            length = struct.unpack(">I", handle.read(4))[0]
            compression = handle.read(1)[0]
            payload = handle.read(length - 1)
            root = _decompress(payload, compression)
            chunks[index] = Chunk(index, root, compression, payload)
    return RegionData(chunks, timestamps)


def write_region(path: str, data: RegionData) -> None:
    """Rewrite a region file; untouched chunks keep their exact payload bytes."""
    chunks = data.chunks
    locations = bytearray(4096)
    timestamps = bytearray(data.timestamps)
    body = io.BytesIO()
    sector = 2
    for index in range(1024):
        chunk = chunks.get(index)
        if chunk is None:
            continue
        if chunk.modified:
            payload = nbt_dump(chunk.root, chunk.compression)
            blob = struct.pack(">I", len(payload) + 1) + bytes([chunk.compression]) + payload
        else:
            blob = struct.pack(">I", len(chunk.raw_payload) + 1) + bytes([chunk.compression]) + chunk.raw_payload
        locations[index * 4 : index * 4 + 3] = sector.to_bytes(3, "big")
        locations[index * 4 + 3] = max(1, (len(blob) + SECTOR - 1) // SECTOR)
        body.write(blob)
        pad = (SECTOR - len(blob) % SECTOR) % SECTOR
        if pad:
            body.write(b"\0" * pad)
        sector += (len(blob) + pad) // SECTOR
    with open(path, "wb") as handle:
        handle.write(bytes(locations))
        handle.write(bytes(timestamps))
        handle.write(body.getvalue())


def iter_region_files(save_dir: str, kind: str) -> list[str]:
    """Every region file of a kind (region|entities|poi) inside the save copy."""
    pattern = os.path.join(save_dir, "**", kind, "r.*.mca")
    return sorted(glob.glob(pattern, recursive=True))


def iter_dat_files(save_dir: str) -> list[str]:
    dats = []
    for name in ("level.dat", "level.dat_old"):
        path = os.path.join(save_dir, name)
        if os.path.isfile(path):
            dats.append(path)
    for sub in ("playerdata", "players"):
        for path in glob.glob(os.path.join(save_dir, sub, "**", "*.dat"), recursive=True):
            dats.append(path)
    return dats


# ---------------------------------------------------------------------------
# Scanner
# ---------------------------------------------------------------------------


class Report:
    def __init__(self) -> None:
        self.counts: dict[str, dict[str, int]] = {}
        self.properties: dict[str, set[str]] = {}
        self.files_scanned = 0
        self.chunks = 0
        self.dataversions: dict[str, set[int]] = {}
        self.unparsed: list[str] = []

    def count(self, category: str, identifier: str) -> None:
        self.counts.setdefault(category, {}).setdefault(identifier, 0)
        self.counts[category][identifier] += 1

    def to_json(self) -> dict:
        return {
            "files_scanned": self.files_scanned,
            "chunks": self.chunks,
            "data_versions": {k: sorted(v) for k, v in self.dataversions.items()},
            "counts": {k: dict(sorted(v.items())) for k, v in self.counts.items()},
            "block_properties": {k: sorted(v) for k, v in sorted(self.properties.items())},
            "unparsed_files": self.unparsed,
        }


def _looks_like_item(compound: dict) -> bool:
    if not isinstance(compound, dict):
        return False
    identifier = compound.get("id")
    if not isinstance(identifier, str) or not ANY_ID.match(identifier):
        return False
    return any(hint in compound for hint in ITEM_STACK_HINTS)


def scan_value(value, report: Report, in_item: bool = False) -> None:
    if isinstance(value, dict):
        identifier = value.get("id")
        if isinstance(identifier, str):
            if IC2_ID.match(identifier) and any(hint in value for hint in ITEM_STACK_HINTS):
                report.count("item", identifier)
                in_item = True
        for child in value.values():
            scan_value(child, report, in_item)
    elif isinstance(value, NbtList):
        for child in value:
            scan_value(child, report, in_item)
    elif isinstance(value, str) and IC2_ID.match(value):
        report.count("other", value)


def scan_chunk_root(root: dict, report: Report) -> None:
    report.chunks += 1
    for section in root.get("sections", []) or []:
        states = section.get("block_states") or {}
        for entry in states.get("palette", []) or []:
            name = entry.get("Name")
            if isinstance(name, str):
                report.count("palette", name)
                props = entry.get("Properties")
                if isinstance(props, dict):
                    report.properties.setdefault(name, set()).update(props.keys())
    for entity in root.get("entities", []) or []:
        identifier = entity.get("id")
        if isinstance(identifier, str):
            report.count("entity", identifier)
    for block_entity in root.get("block_entities", []) or []:
        identifier = block_entity.get("id")
        if isinstance(identifier, str):
            report.count("block_entity", identifier)
            for key, child in block_entity.items():
                if key in ("Items", "Inventory", "Recipes"):
                    scan_value(child, report)
    for entity in root.get("Entities", []) or []:
        identifier = entity.get("id")
        if isinstance(identifier, str):
            report.count("entity", identifier)


def scan_save(save_dir: str) -> Report:
    report = Report()
    for path in iter_dat_files(save_dir):
        report.files_scanned += 1
        try:
            with open(path, "rb") as handle:
                root = nbt_load(handle.read())
        except Exception as error:  # noqa: BLE001 - report and keep scanning
            report.unparsed.append(f"{path}: {error}")
            continue
        version = root.get("DataVersion")
        if isinstance(version, int):
            report.dataversions.setdefault(os.path.basename(path), set()).add(version)
        scan_value(root, report)
    for kind in ("region", "entities"):
        for path in iter_region_files(save_dir, kind):
            report.files_scanned += 1
            try:
                region = read_region(path)
            except Exception as error:  # noqa: BLE001
                report.unparsed.append(f"{path}: {error}")
                continue
            for chunk in region.chunks.values():
                if chunk is None:
                    report.unparsed.append(f"{path}: external chunk slot (unsupported, see docs)")
                    continue
                version = chunk.root.get("DataVersion")
                if isinstance(version, int):
                    report.dataversions.setdefault(kind, set()).add(version)
                scan_chunk_root(chunk.root, report)
    return report


# ---------------------------------------------------------------------------
# Converter
# ---------------------------------------------------------------------------


class IdMap:
    def __init__(self, mapping: dict) -> None:
        self.block: dict[str, str] = mapping.get("block", {})
        self.block_entity: dict[str, str | None] = mapping.get("block_entity", {})
        self.item: dict[str, str] = mapping.get("item", {})
        self.entity: dict[str, str] = mapping.get("entity", {})
        self.fluid: dict[str, str] = mapping.get("fluid", {})

    @classmethod
    def load(cls, path: str) -> "IdMap":
        with open(path, encoding="utf-8") as handle:
            return cls(json.load(handle))


class ConvertStats:
    def __init__(self) -> None:
        self.renamed: dict[str, int] = {}
        self.dropped: dict[str, int] = {}
        # ic2 ids met and left untouched: identity is the catalog default (1198/1199
        # preserve), so absence from the map is by design, not an error. Reviewers
        # compare this against the scan report instead of trusting it blindly.
        self.preserved: dict[str, dict[str, int]] = {}
        self.chunks_rewritten = 0
        self.chunks_kept = 0

    def rename(self, category: str, amount: int = 1) -> None:
        self.renamed[category] = self.renamed.get(category, 0) + amount

    def drop(self, category: str) -> None:
        self.dropped[category] = self.dropped.get(category, 0) + 1

    def mark_preserved(self, category: str, identifier: str) -> None:
        bucket = self.preserved.setdefault(category, {})
        bucket[identifier] = bucket.get(identifier, 0) + 1

    def to_json(self) -> dict:
        return {
            "renamed": self.renamed,
            "dropped": self.dropped,
            "preserved": self.preserved,
            "chunks_rewritten": self.chunks_rewritten,
            "chunks_kept": self.chunks_kept,
        }


def convert_palette(section: dict, id_map: IdMap, stats: ConvertStats) -> None:
    states = section.get("block_states") or {}
    for entry in states.get("palette", []) or []:
        name = entry.get("Name")
        if not isinstance(name, str):
            continue
        replacement = id_map.block.get(name)
        if replacement is None:
            if name.startswith("ic2:"):
                stats.mark_preserved("palette", name)
            continue
        entry["Name"] = replacement
        stats.rename("palette")


def convert_chunk_root(root: dict, id_map: IdMap, stats: ConvertStats) -> None:
    sections = root.get("sections")
    if isinstance(sections, NbtList):
        for section in sections:
            if isinstance(section, dict):
                convert_palette(section, id_map, stats)
    entities = root.get("entities")
    if isinstance(entities, NbtList):
        _convert_entities(entities, id_map, stats)
    ticking = root.get("Entities")
    if isinstance(ticking, NbtList):
        _convert_entities(ticking, id_map, stats)
    block_entities = root.get("block_entities")
    if isinstance(block_entities, NbtList):
        keep = NbtList(block_entities.element_tag, [])
        for block_entity in block_entities:
            identifier = block_entity.get("id") if isinstance(block_entity, dict) else None
            if not isinstance(identifier, str):
                keep.append(block_entity)
                continue
            replacement = id_map.block_entity.get(identifier)
            if replacement is None:
                if identifier.startswith("ic2:"):
                    stats.mark_preserved("block_entity", identifier)
                keep.append(block_entity)
                continue
            if replacement == "":
                # mapped to "drop": the new mod implements this block-level, no BE
                stats.drop(f"block_entity:{identifier}")
                continue
            block_entity["id"] = replacement
            stats.rename("block_entity")
            keep.append(block_entity)
        root["block_entities"] = keep
        # machine inventories live inside the block entities, not at chunk root
        convert_items(keep, id_map, stats)
    items = root.get("Items")  # legacy chunk-level container (pre-1.17 layout)
    if items is not None:
        convert_items(items, id_map, stats)


def _convert_entities(entities: NbtList, id_map: IdMap, stats: ConvertStats) -> None:
    for entity in entities:
        if not isinstance(entity, dict):
            continue
        identifier = entity.get("id")
        if isinstance(identifier, str):
            replacement = id_map.entity.get(identifier)
            if replacement is None:
                if identifier.startswith("ic2:"):
                    stats.mark_preserved("entity", identifier)
            else:
                entity["id"] = replacement
                stats.rename("entity")
        # entities can carry stacks too (item frames, minecarts, dropped gear)
        convert_items(entity, id_map, stats)


def convert_items(value, id_map: IdMap, stats: ConvertStats) -> None:
    """Remap item stack ids anywhere below `value` (in place)."""
    if isinstance(value, NbtList):
        for child in value:
            convert_items(child, id_map, stats)
    elif isinstance(value, dict):
        identifier = value.get("id")
        if isinstance(identifier, str) and any(hint in value for hint in ITEM_STACK_HINTS):
            replacement = id_map.item.get(identifier)
            if replacement is None:
                if identifier.startswith("ic2:"):
                    stats.mark_preserved("item", identifier)
            else:
                value["id"] = replacement
                stats.rename("item")
        for child in value.values():
            convert_items(child, id_map, stats)


def convert_save(src: str, dst: str, id_map: IdMap, *, write: bool) -> dict:
    if os.path.abspath(src) == os.path.abspath(dst):
        raise SystemExit("convert refuses in-place operation: --dst must differ from --src (P18 operates on copies)")
    if write and os.path.exists(dst):
        raise SystemExit(f"refusing to overwrite existing destination {dst}")
    stats = ConvertStats()
    if write:
        os.makedirs(dst, exist_ok=True)
    # plain files first (level.dat, level.dat_old)
    for rel in ("level.dat", "level.dat_old"):
        src_path = os.path.join(src, rel)
        if not os.path.isfile(src_path):
            continue
        with open(src_path, "rb") as handle:
            raw = handle.read()
        root = nbt_load(raw)
        convert_items(root, id_map, stats)
        if write:
            _ensure_parent(os.path.join(dst, rel))
            with open(os.path.join(dst, rel), "wb") as handle:
                handle.write(nbt_dump(root, 1))
    for sub in ("playerdata", "players"):
        for path in glob.glob(os.path.join(src, sub, "**", "*.dat"), recursive=True):
            rel = os.path.relpath(path, src)
            with open(path, "rb") as handle:
                root = nbt_load(handle.read())
            convert_items(root, id_map, stats)
            if write:
                out = os.path.join(dst, rel)
                _ensure_parent(out)
                with open(out, "wb") as handle:
                    handle.write(nbt_dump(root, 1))
    # region + entities containers
    for kind in ("region", "entities"):
        for path in iter_region_files(src, kind):
            rel = os.path.relpath(path, src)
            region = read_region(path)
            out_chunks: dict[int, Chunk | None] = {}
            for index, chunk in region.chunks.items():
                if chunk is None:
                    out_chunks[index] = None
                    continue
                working = copy.deepcopy(chunk.root)
                convert_chunk_root(working, id_map, stats)
                if working != chunk.root:
                    stats.chunks_rewritten += 1
                    out_chunks[index] = Chunk(index, working, chunk.compression, None)
                else:
                    stats.chunks_kept += 1
                    out_chunks[index] = chunk
            if write:
                out_path = os.path.join(dst, rel)
                _ensure_parent(out_path)
                write_region(out_path, RegionData(out_chunks, region.timestamps))
    if write:
        # carry over everything the conversion does not touch so the copy stays playable
        shutil.copytree(
            src,
            dst,
            dirs_exist_ok=True,
            ignore=shutil.ignore_patterns("region", "entities", "level.dat", "level.dat_old", "playerdata", "players"),
        )
    return stats.to_json()


def _ensure_parent(path: str) -> None:
    parent = os.path.dirname(path)
    if parent:
        os.makedirs(parent, exist_ok=True)


# ---------------------------------------------------------------------------
# Self test: a synthetic legacy-format fixture proves both code paths.
# ---------------------------------------------------------------------------

SELF_TEST_MAP = {
    "block": {"ic2:legacy_block": "ic2:modern_block"},
    "block_entity": {"ic2:itnt": ""},
    "item": {"ic2:old_item": "ic2:new_item"},
    "entity": {"ic2:legacy_beast": "ic2:modern_beast"},
    "fluid": {},
}


def _fixture_chunk(x: int, z: int) -> dict:
    palette = NbtList(
        10,
        [
            {"Name": "ic2:legacy_block"},
            {"Name": "minecraft:stone", "Properties": {"facing": "north"}},
        ],
    )
    section = {"Y": 0, "block_states": {"palette": palette, "data": LongArray([0] * 64)}, "biomes": {"palette": NbtList(10, [{"Name": "minecraft:plains"}])}}
    block_entities = NbtList(
        10,
        [
            {"id": "ic2:sign", "x": x, "y": 1, "z": z},
            {
                "id": "ic2:electric_compressor",
                "x": x + 1,
                "y": 1,
                "z": z,
                "energy": 1000.0,
                "Items": NbtList(10, [{"id": "ic2:old_item", "Count": 1, "Slot": 0}]),
            },
            {"id": "ic2:itnt", "x": x + 2, "y": 1, "z": z},
        ],
    )
    entities = NbtList(10, [{"id": "ic2:legacy_beast", "Pos": NbtList(5, [0.0, 1.0, 0.0])}])
    return {
        "DataVersion": 3465,  # 1.20.1
        "xPos": x,
        "zPos": z,
        "Status": "full",
        "sections": NbtList(10, [section]),
        "block_entities": block_entities,
        "entities": entities,
        "PostProcessing": NbtList(12, []),
    }


def _fixture_dat() -> dict:
    return {
        "DataVersion": 3465,
        "Inventory": NbtList(10, [{"id": "ic2:old_item", "Count": 64, "Slot": 0}]),
    }


def self_test(tmp: str) -> None:
    src = os.path.join(tmp, "legacy-save")
    region_dir = os.path.join(src, "region")
    os.makedirs(region_dir)
    with open(os.path.join(src, "level.dat"), "wb") as handle:
        handle.write(nbt_dump(_fixture_dat(), 1))
    chunks = {
        66: Chunk(66, _fixture_chunk(-1, -1), 2, None),
    }
    write_region(os.path.join(region_dir, "r.-1.-1.mca"), RegionData(chunks, b"\0" * 4096))

    report = scan_save(src).to_json()
    assert report["counts"]["palette"]["ic2:legacy_block"] == 1, report
    assert report["counts"]["palette"]["minecraft:stone"] == 1, report
    assert report["block_properties"]["minecraft:stone"] == ["facing"], report
    assert report["counts"]["block_entity"]["ic2:sign"] == 1, report
    assert report["counts"]["block_entity"]["ic2:electric_compressor"] == 1, report
    assert report["counts"]["item"]["ic2:old_item"] == 2, report  # BE + player inventory
    assert report["counts"]["entity"]["ic2:legacy_beast"] == 1, report
    assert report["data_versions"]["region"] == [3465], report

    # in-place conversion is refused
    try:
        convert_save(src, src, IdMap(SELF_TEST_MAP), write=True)
    except SystemExit:
        pass
    else:
        raise AssertionError("in-place conversion was not refused")

    dst = os.path.join(tmp, "converted-save")
    stats = convert_save(src, dst, IdMap(SELF_TEST_MAP), write=True)
    assert stats["renamed"]["palette"] == 1, stats
    assert stats["renamed"]["item"] == 2, stats  # machine inventory + player inventory
    assert stats["renamed"]["entity"] == 1, stats
    assert stats["dropped"] == {"block_entity:ic2:itnt": 1}, stats
    # ids absent from the map are preserved by design (catalog identity default)
    assert stats["preserved"] == {"block_entity": {"ic2:sign": 1, "ic2:electric_compressor": 1}}, stats
    assert stats["chunks_rewritten"] == 1 and stats["chunks_kept"] == 0, stats

    converted = scan_save(dst).to_json()
    assert converted["counts"]["palette"].get("ic2:modern_block") == 1, converted
    assert "ic2:legacy_block" not in converted["counts"]["palette"], converted
    assert "ic2:itnt" not in converted["counts"]["block_entity"], converted
    assert "ic2:sign" in converted["counts"]["block_entity"], converted
    assert converted["counts"]["item"].get("ic2:new_item") == 2, converted
    assert converted["counts"]["entity"].get("ic2:modern_beast") == 1, converted

    # byte-exact round trip for an untouched region: convert again with an empty map
    dst2 = os.path.join(tmp, "identity-save")
    convert_save(src, dst2, IdMap({"block": {}, "block_entity": {}, "item": {}, "entity": {}, "fluid": {}}), write=True)
    with open(os.path.join(dst2, "region", "r.-1.-1.mca"), "rb") as handle:
        original = open(os.path.join(src, "region", "r.-1.-1.mca"), "rb").read()
        assert handle.read() == original, "identity conversion changed chunk payload bytes"

    # playerdata conversion
    player_dir = os.path.join(src, "players")
    os.makedirs(player_dir, exist_ok=True)
    with open(os.path.join(player_dir, "legacy.dat"), "wb") as handle:
        handle.write(nbt_dump(_fixture_dat(), 1))
    dst3 = os.path.join(tmp, "player-converted")
    convert_save(src, dst3, IdMap(SELF_TEST_MAP), write=True)
    with open(os.path.join(dst3, "players", "legacy.dat"), "rb") as handle:
        player = nbt_load(handle.read())
    assert player["Inventory"][0]["id"] == "ic2:new_item", player

    print("self-test: PASS (scan, convert, drop, byte-exact round trip, playerdata)")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)

    scan_parser = sub.add_parser("scan", help="inventory ic2 ids in a save copy (read only)")
    scan_parser.add_argument("--src", required=True)
    scan_parser.add_argument("--out", help="write the JSON report here instead of stdout")

    convert_parser = sub.add_parser("convert", help="rewrite ic2 ids per the id map into a fresh copy")
    convert_parser.add_argument("--src", required=True, help="source save COPY (never your only copy)")
    convert_parser.add_argument("--dst", required=True, help="destination path (must not exist unless dry run)")
    convert_parser.add_argument("--map", default=os.path.join(os.path.dirname(__file__), "save_id_map.json"))
    convert_parser.add_argument("--write", action="store_true", help="actually write the converted copy (default: dry run)")
    convert_parser.add_argument("--out", help="write the JSON stats report here instead of stdout")

    sub.add_parser("self-test", help="run the built-in fixture test")

    args = parser.parse_args(argv)
    if args.command == "self-test":
        with tempfile.TemporaryDirectory() as tmp:
            self_test(tmp)
        return 0

    if args.command == "scan":
        report = scan_save(args.src)
        payload = json.dumps(report.to_json(), ensure_ascii=False, indent=2, sort_keys=True)
        if args.out:
            with open(args.out, "w", encoding="utf-8") as handle:
                handle.write(payload + "\n")
            print(f"scan report written to {args.out}")
        else:
            print(payload)
        return 0

    id_map = IdMap.load(args.map)
    stats = convert_save(args.src, args.dst, id_map, write=args.write)
    payload = json.dumps(stats, ensure_ascii=False, indent=2, sort_keys=True)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(payload + "\n")
        print(f"convert stats written to {args.out}")
    else:
        print(payload)
    if not args.write:
        print("dry run only; pass --write to create the converted copy")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
