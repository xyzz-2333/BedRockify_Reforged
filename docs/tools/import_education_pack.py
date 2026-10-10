"""Import registered education textures and display names from a supplied Bedrock ZIP.

Usage: python docs/tools/import_education_pack.py path/to/chemistry.zip
The source ZIP stays outside Git. Imported files and a provenance manifest are tracked.
"""
import argparse
import hashlib
import io
import json
import re
import zipfile
from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parents[2]
ASSETS = REPO / 'src/main/resources/assets/bedrockify'
MANIFEST = REPO / 'docs/education_resources.json'


def read_lang(archive, name):
    result = {}
    for line in archive.read(name).decode('utf-8-sig').splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        result[key] = value.split('\t#', 1)[0].strip()
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('pack', type=Path)
    args = parser.parse_args()
    textures = {}
    labels = {}
    with zipfile.ZipFile(args.pack) as archive:
        for source in sorted(archive.namelist()):
            match = re.fullmatch(r'textures/blocks/element_(\d{3})_([a-z]+)\.png', source)
            if match:
                number = int(match[1])
                assert 0 <= number <= 118
                textures[f'block/element_{number}.png'] = source
                labels[f'block.bedrockify.element_{number}'] = f'tile.element.{match[2]}.name'
        assert len(textures) == 119, 'Expected all 119 element textures'
        for side in ('front', 'side', 'top', 'bottom'):
            textures[f'block/material_reducer_{side}.png'] = f'textures/blocks/material_reducer_{side}.png'
        labels['block.bedrockify.material_reducer'] = 'tile.materialreducer.name'
        for color in ('underwater', 'blue', 'red', 'purple', 'green'):
            textures[f'block/{color}_torch.png'] = f'textures/blocks/torch_{color}.png'
            source_key = 'tile.underwater_torch.name' if color == 'underwater' else f'tile.colored_torch.{color}.name'
            labels[f'block.bedrockify.{color}_torch'] = source_key
            wall_name = f'block.bedrockify.{color}_wall_torch'
            labels[wall_name] = source_key
        for compound in ('cerium', 'mercuric', 'potassium', 'tungsten'):
            # These plain compound bottles reuse the pack's white jar sprite.
            textures[f'item/{compound}_chloride.png'] = 'textures/items/compounds/jar_white.png'
            labels[f'item.bedrockify.{compound}_chloride'] = f'item.compound.{compound}chloride.name'

        locales = {
            Path(name).stem.lower(): read_lang(archive, name)
            for name in archive.namelist()
            if re.fullmatch(r'texts/[a-z]{2}_[A-Z]{2}\.lang', name)
        }
        english = locales['en_us']
        for source_key in set(labels.values()):
            assert english.get(source_key), f'Missing source label {source_key}'

        # Validate every selected PNG before writing any imported resource.
        blobs = {}
        for destination, source in textures.items():
            blob = archive.read(source)
            with Image.open(io.BytesIO(blob)) as image:
                image.verify()
            blobs[destination] = blob

        imported = []
        for destination, blob in blobs.items():
            path = ASSETS / 'textures' / destination
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(blob)
            imported.append({'destination': 'textures/' + destination, 'source': textures[destination], 'sha256': hashlib.sha256(blob).hexdigest()})

        all_locales = sorted(set(locales) | {path.stem for path in (ASSETS / 'lang').glob('*.json')})
        coverage = []
        language_defaults = {'es': 'es_es', 'pt': 'pt_br', 'fr': 'fr_fr', 'en': 'en_us', 'zh': 'zh_cn'}
        for locale in all_locales:
            path = ASSETS / 'lang' / (locale + '.json')
            existing = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
            source_locale = locale if locale in locales else language_defaults.get(locale.split('_')[0])
            source_lang = locales.get(source_locale, {})
            fallback = 0
            for key, source_key in labels.items():
                translated = source_lang.get(source_key)
                if not translated:
                    translated = english[source_key]
                    fallback += 1
                existing[key] = translated
            path.write_text(json.dumps(existing, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
            coverage.append({'locale': locale, 'source_locale': source_locale or 'en_us', 'imported_keys': len(labels), 'english_fallbacks': fallback})

    manifest = {
        'source_archive': args.pack.name,
        'source_sha256': hashlib.sha256(args.pack.read_bytes()).hexdigest(),
        'scope': 'Registered alpha.12 education content only; no unused pack assets imported.',
        'textures': imported,
        'locales': coverage,
    }
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(f'Imported {len(imported)} unchanged PNGs and {len(labels)} name keys across {len(coverage)} locales')


if __name__ == '__main__':
    main()
