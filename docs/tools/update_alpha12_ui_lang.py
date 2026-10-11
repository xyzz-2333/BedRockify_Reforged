"""Merge alpha.12 settings and screen text without rewriting existing entries."""
import json
from pathlib import Path

LANG = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/bedrockify/lang'
TRANSLATIONS = {
    'en_us': {
        'bedrockify.options.slowRegeneration': 'Slow natural regeneration',
        'bedrockify.options.slowRegeneration.tooltip': 'Disable fast saturation healing and use the configured natural healing interval. The naturalRegeneration game rule still applies.',
        'bedrockify.options.regenerationInterval': 'Natural healing interval',
        'bedrockify.options.regenerationInterval.tooltip': 'Time between healing 1 health point (half a heart). Default: 4 seconds. Requires slow natural regeneration.',
        'bedrockify.options.noAttackCooldown': 'Disable attack cooldown',
        'bedrockify.options.noAttackCooldown.tooltip': 'Attacks use full strength without charging. Damage immunity time still applies. Multiplayer uses the server setting.',
        'block.bedrockify.material_reducer': 'Material Reducer',
        'bedrockify.education.collectOutputs': 'Collect all outputs first',
        'bedrockify.education.atomicNumber': 'Atomic number: %s',
        'bedrockify.education.unknown': 'Unknown composition',
        'bedrockify.creative.group.elements': 'Elements',
        'bedrockify.creative.group.chlorides': 'Chlorides',
        'block.bedrockify.underwater_torch': 'Underwater Torch',
        'block.bedrockify.blue_torch': 'Blue Torch',
        'block.bedrockify.red_torch': 'Red Torch',
        'block.bedrockify.purple_torch': 'Purple Torch',
        'block.bedrockify.green_torch': 'Green Torch',
    },
    'zh_cn': {
        'bedrockify.options.slowRegeneration': '慢速自然回血',
        'bedrockify.options.slowRegeneration.tooltip': '关闭饱和度快速回血，按设定间隔自然回血。仍遵循 naturalRegeneration 游戏规则。',
        'bedrockify.options.regenerationInterval': '自然回血间隔',
        'bedrockify.options.regenerationInterval.tooltip': '每次恢复 1 点生命值（半颗心）的时间间隔。默认 4 秒，需启用慢速自然回血。',
        'bedrockify.options.noAttackCooldown': '取消攻击蓄力',
        'bedrockify.options.noAttackCooldown.tooltip': '攻击无需蓄力即可发挥完整威力，保留受伤无敌时间。多人游戏遵循服务端设置。',
        'block.bedrockify.material_reducer': '材料分解器',
        'bedrockify.education.collectOutputs': '请先领取所有产物',
        'bedrockify.education.atomicNumber': '原子序数：%s',
        'bedrockify.education.unknown': '未知组成',
        'bedrockify.creative.group.elements': '元素',
        'bedrockify.creative.group.chlorides': '氯化物',
        'block.bedrockify.underwater_torch': '水下火把',
        'block.bedrockify.blue_torch': '蓝色火把',
        'block.bedrockify.red_torch': '红色火把',
        'block.bedrockify.purple_torch': '紫色火把',
        'block.bedrockify.green_torch': '绿色火把',
    },
}

if __name__ == '__main__':
    for locale, entries in TRANSLATIONS.items():
        path = LANG / (locale + '.json')
        original = path.read_text(encoding='utf-8')
        existing = json.loads(original)
        additions = {key: value for key, value in entries.items() if key not in existing}
        if additions:
            suffix = json.dumps(additions, ensure_ascii=False, indent=2)[1:-2]
            path.write_text(original.rstrip().removesuffix('}').rstrip() + ',' + suffix + '\n}\n', encoding='utf-8', newline='\n')
        print(locale, len(additions), 'entries added')
