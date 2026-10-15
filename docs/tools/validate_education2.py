from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parents[2];A=R/'src/main/resources/assets/bedrockify';D=R/'src/main/resources/data/bedrockify'
m=json.loads((R/'docs/education2_resources.json').read_text(encoding='utf-8'))
for entry in m['textures']:
 assert hashlib.sha256((R/entry['target']).read_bytes()).hexdigest()==entry['sha256'],entry['target']
recipes=list((D/'recipes/education/education2/chemistry').glob('*.json'))
assert len(recipes)==41
signatures=set()
for p in recipes:
 d=json.loads(p.read_text());assert d['type']=='bedrockify:chemistry'
 signature=(d['station'],tuple(sorted(d['inputs'].items())))
 assert signature not in signatures,p
 signatures.add(signature)
 for item,count in d['inputs'].items():assert count>0 and count<=64
for p in (A/'models').rglob('*.json'):
 d=json.loads(p.read_text())
 for ref in d.get('textures',{}).values():
  if ref.startswith('bedrockify:'):assert (A/'textures'/(ref.split(':',1)[1]+'.png')).exists(),(p,ref)
for code in ['en_us','zh_cn']:
 d=json.loads((A/'lang'/(code+'.json')).read_text(encoding='utf-8'))
 for name in ['element_constructor','compound_creator','lab_table','underwater_tnt','heat_block','hardened_glass','red_hardened_glass_pane']:assert 'block.bedrockify.'+name in d
print(f'PASS: {len(m["textures"])} original texture hashes, 41 unique chemistry recipes, model texture references, UI names')
