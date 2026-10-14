"""Validate generated palette coverage, provenance and resource values."""
from pathlib import Path
import importlib.util,json,re
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('importer',Path(__file__).with_name('import_bedrock_water_colors.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def main():
 palette=json.loads((ROOT/'src/main/resources/assets/bedrockify/water_colors.json').read_text())
 manifest=json.loads((ROOT/'docs/water_colors_sources.json').read_text())
 assert palette['format']==1
 rows=palette['biomes'];assert set(rows)=={'minecraft:'+k for k in m.JAVA_BIOMES}
 mapping=manifest['mapping'];assert len(mapping)==len(rows)==64
 for entry in mapping:
  assert rows[entry['java']]=={k:entry[k] for k in ('surface','fog')}
  for color in rows[entry['java']].values():assert re.fullmatch('#[0-9A-F]{6}',color)
 for source in manifest['sources']:
  assert not Path(source['path']).is_absolute() and '..' not in Path(source['path']).parts
  assert re.fullmatch('[0-9a-f]{64}',source['sha256'])
 assert rows['minecraft:mangrove_swamp']['surface']!=rows['minecraft:mangrove_swamp']['fog']
 assert rows['minecraft:warm_ocean']['surface']!=rows['minecraft:warm_ocean']['fog']
 print('PASS: 64 water mappings, distinct surface/fog data and relative source provenance')
if __name__=='__main__':main()
