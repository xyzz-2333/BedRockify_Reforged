"""Read the supplied MCPE 0.11 archive without opening or modifying either world.

Supports this archive's legacy 128-high terrain records and LevelDB raw/zlib
tables plus write-ahead logs. This is not a general Bedrock world converter.
"""

import argparse
import hashlib
import json
from pathlib import Path
import struct
import zipfile
import zlib


def varint(data, pos=0):
    value = shift = 0
    while True:
        byte = data[pos]
        pos += 1
        value |= (byte & 127) << shift
        if byte < 128:
            return value, pos
        shift += 7


def table_entries(data):
    end = len(data) - 4 - 4 * struct.unpack_from('<I', data, len(data) - 4)[0]
    pos, key = 0, b''
    while pos < end:
        shared, pos = varint(data, pos)
        size, pos = varint(data, pos)
        length, pos = varint(data, pos)
        key = key[:shared] + data[pos:pos + size]
        pos += size
        value = data[pos:pos + length]
        pos += length
        yield key, value


def read_table(data):
    def block(handle):
        offset, pos = varint(handle)
        size, _ = varint(handle, pos)
        raw = data[offset:offset + size]
        compression = data[offset + size]
        if compression == 0:
            return raw
        if compression == 2:
            return zlib.decompress(raw)
        if compression == 4:
            return zlib.decompress(raw, -15)
        raise ValueError(f'Unsupported table compression: {compression}')

    footer = data[-48:-8]
    _, pos = varint(footer)
    _, pos = varint(footer, pos)
    for _, handle in table_entries(block(footer[pos:])):
        for key, value in table_entries(block(handle)):
            tag = struct.unpack('<Q', key[-8:])[0]
            yield key[:-8], tag >> 8, tag & 255, value


def read_log(data):
    fragments = b''
    for start in range(0, len(data), 32768):
        pos, end = start, min(start + 32768, len(data))
        while pos + 7 <= end:
            _, length, kind = struct.unpack_from('<IHB', data, pos)
            pos += 7
            if kind == 0:
                break
            if pos + length > end:
                raise ValueError('Truncated LevelDB log record')
            part = data[pos:pos + length]
            pos += length
            fragments = part if kind in (1, 2) else fragments + part
            if kind not in (1, 4):
                continue
            sequence, count = struct.unpack_from('<QI', fragments)
            cursor = 12
            for index in range(count):
                typ = fragments[cursor]
                cursor += 1
                size, cursor = varint(fragments, cursor)
                key = fragments[cursor:cursor + size]
                cursor += size
                value = b''
                if typ:
                    size, cursor = varint(fragments, cursor)
                    value = fragments[cursor:cursor + size]
                    cursor += size
                yield key, sequence + index, typ, value


def terrain(archive, world):
    records = {}
    for name in sorted(archive.namelist()):
        if not name.startswith(world + '/db/'):
            continue
        suffix = Path(name).suffix
        if suffix not in ('.ldb', '.log'):
            continue
        data = archive.read(name)
        for key, sequence, typ, value in (read_table(data) if suffix == '.ldb' else read_log(data)):
            if key not in records or sequence > records[key][0]:
                records[key] = sequence, typ, value
    netherrack = set()
    for key, (_, typ, data) in records.items():
        if not typ or len(key) != 9 or key[-1] != 0x30:
            continue
        if len(data) != 83200:
            raise ValueError('Expected legacy 16x16x128 terrain record')
        chunk_x, chunk_z = struct.unpack('<ii', key[:8])
        for index, block in enumerate(data[:32768]):
            if block == 87:
                netherrack.add((chunk_x * 16 + (index >> 11) - 5,
                               (index & 127) - 5,
                               chunk_z * 16 + ((index >> 7) & 15) - 20))
    return netherrack


def extract(path):
    with zipfile.ZipFile(path) as archive:
        completed = terrain(archive, 'TDcGAG4jAgA=')
        interrupted = terrain(archive, '测试2')
    if (len(completed), len(interrupted)) != (2495, 3003):
        raise ValueError('Archive differs from the two supplied 0.11 test results')

    def layers(blocks):
        return [','.join(f'{sum(1 << (x + 8) for x in range(-8, 9) if (x, y, z) in blocks):05x}'
                         for z in range(-8, 9)) for y in range(-3, 31)]

    return {
        'source': path.name,
        'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
        'center': [5, 5, 20],
        'format': 'Layers y=-3..30; each comma-separated row is z=-8..8; bit x+8 is netherrack.',
        'interrupted_access_holes': [[8, -1, 4], [8, 0, 4]],
        'interrupted_count': len(interrupted),
        'completed_count': len(completed),
        'completed': layers(completed),
        'interrupted': layers(interrupted),
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('archive', type=Path)
    parser.add_argument('--check-fixture', type=Path)
    parser.add_argument('--write-fixture', type=Path)
    args = parser.parse_args()
    result = extract(args.archive)
    if args.check_fixture:
        if result != json.loads(args.check_fixture.read_text(encoding='utf-8')):
            raise SystemExit('Fixture differs from archive')
        print('PASS: fixture matches the supplied archive')
    if args.write_fixture:
        args.write_fixture.parent.mkdir(parents=True, exist_ok=True)
        args.write_fixture.write_bytes((json.dumps(result, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))
    print(json.dumps({key: value for key, value in result.items() if key not in ('completed', 'interrupted')},
                     ensure_ascii=False, indent=2))
