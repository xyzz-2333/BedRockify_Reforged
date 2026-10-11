"""Validate alpha.12 resource links and serializer limits with the standard library."""
import json
import re
import hashlib
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
RES = REPO / 'src/main/resources'
ASSETS = RES / 'assets/bedrockify'


def load(path):
    def unique(pairs):
        result = {}
        for key, value in pairs:
            assert key not in result, f'{path}: duplicate key {key}'
            result[key] = value
        return result
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique)


def main():
    manifest_path = REPO / 'docs/education_resources.json'
    if manifest_path.exists():
        manifest = load(manifest_path)
        for texture in manifest['textures']:
            path = ASSETS / texture['destination']
            assert hashlib.sha256(path.read_bytes()).hexdigest() == texture['sha256'], f'Imported texture changed: {path}'
        for entry in manifest['locales']:
            lang = load(ASSETS / 'lang' / (entry['locale'] + '.json'))
            for n in range(119):
                assert lang.get(f'block.bedrockify.element_{n}'), f'Missing element {n} in {entry["locale"]}'
            for prefix in ('cerium', 'mercuric', 'potassium', 'tungsten'):
                assert lang.get(f'item.bedrockify.{prefix}_chloride')
            for color in ('underwater', 'blue', 'red', 'purple', 'green'):
                assert lang.get(f'block.bedrockify.{color}_torch')
                assert lang.get(f'block.bedrockify.{color}_wall_torch')
            assert lang.get('block.bedrockify.material_reducer')
        print(f'PASS: {len(manifest["textures"])} imported texture hashes, names in {len(manifest["locales"])} locales')

    literal_keys = set()
    for source in (REPO / 'src/main/java').rglob('*.java'):
        literal_keys.update(re.findall(r'Text\.translatable\("(bedrockify\.[^"]+)"', source.read_text(encoding='utf-8')))
    literal_keys = {key for key in literal_keys if not key.endswith('.')}
    for locale in ('en_us', 'zh_cn'):
        lang = load(ASSETS / 'lang' / (locale + '.json'))
        missing = sorted(literal_keys - lang.keys())
        assert not missing, f'{locale}: untranslated literal keys {missing}'
        for key in ('slowRegeneration', 'noAttackCooldown', 'regenerationInterval'):
            for suffix in ('', '.tooltip'):
                assert 'bedrockify.options.' + key + suffix in lang
        for key in ('collectOutputs', 'atomicNumber', 'unknown'):
            assert 'bedrockify.education.' + key in lang
        assert lang['bedrockify.education.atomicNumber'].count('%s') == 1
        for key in ('elements', 'chlorides'):
            assert 'bedrockify.creative.group.' + key in lang

    paths = sorted((RES / 'data/bedrockify/recipes/education/reducing').glob('*.json'))
    assert len(paths) == 30, f'Expected 30 default reducing recipes, got {len(paths)}'
    inputs = set()
    for path in paths:
        recipe = load(path)
        assert recipe['type'] == 'bedrockify:material_reducing'
        slots = 0
        assert recipe['elements']
        for part in recipe['elements']:
            assert type(part['element']) is int and 0 <= part['element'] <= 118
            assert type(part['count']) is int and 1 <= part['count'] <= 100
            slots += (part['count'] + 63) // 64
        assert 1 <= slots <= 9, f'{path}: exceeds reducer output slots'
        ingredients = recipe['ingredient']
        if isinstance(ingredients, dict):
            ingredients = [ingredients]
        assert ingredients
        for ingredient in ingredients:
            item = ingredient['item']
            assert item not in inputs, f'Ambiguous default recipe for {item}'
            inputs.add(item)

    content = (REPO / 'src/main/java/dev/bedrockify/forge/common/features/education/EducationContent.java').read_text(encoding='utf-8')
    torches = re.findall(r'torch\("([^"]+)"', content)
    blocks = [f'element_{n}' for n in range(119)] + ['material_reducer']
    items = blocks + torches
    blocks += torches + [name.replace('_torch', '_wall_torch') for name in torches]
    for name in blocks:
        states = load(ASSETS / 'blockstates' / (name + '.json'))
        for variant in states['variants'].values():
            model = variant['model'].removeprefix('bedrockify:')
            assert (ASSETS / 'models' / (model + '.json')).is_file(), model
        loot = load(RES / 'data/bedrockify/loot_tables/blocks' / (name + '.json'))
        drop = loot['pools'][0]['entries'][0]['name'].removeprefix('bedrockify:')
        assert drop in items, f'{name}: invalid drop {drop}'
    for name in items:
        load(ASSETS / 'models/item' / (name + '.json'))
    for name in torches:
        recipe = load(RES / 'data/bedrockify/recipes/education' / (name + '.json'))
        assert recipe['result']['item'] == 'bedrockify:' + name
        advancement = load(RES / 'data/bedrockify/advancements/recipes/education' / (name + '.json'))
        assert advancement['rewards']['recipes'] == ['bedrockify:education/' + name]
    print(f'PASS: 2 UI locales, {len(blocks)} blocks, {len(paths)} reducing recipes, {len(inputs)} unique inputs, {len(torches)} torch recipes')


if __name__ == '__main__':
    main()
