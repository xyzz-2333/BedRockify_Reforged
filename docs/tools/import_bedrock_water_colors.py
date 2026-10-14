"""Import vanilla Bedrock water colors, resolving numbered resource-pack overlays."""
from pathlib import Path
import argparse,hashlib,json,re
ROOT=Path(__file__).resolve().parents[2]
# Explicit edition-name aliases. Unlisted modern Bedrock biomes inherit its default.
ALIASES={
 'snowy_plains':'ice_plains','ice_spikes':'ice_plains_spikes','swamp':'swampland',
 'dark_forest':'roofed_forest','old_growth_birch_forest':'birch_forest',
 'old_growth_pine_taiga':'mega_taiga','old_growth_spruce_taiga':'mega_spruce_taiga',
 'snowy_taiga':'cold_taiga','windswept_hills':'extreme_hills',
 'windswept_gravelly_hills':'extreme_hills_mutated','windswept_forest':'extreme_hills_plus_trees',
 'windswept_savanna':'savanna_mutated','sparse_jungle':'jungle_edge',
 'badlands':'mesa','eroded_badlands':'mesa_bryce','wooded_badlands':'mesa_plateau_stone',
 'snowy_beach':'cold_beach','stony_shore':'stone_beach','mushroom_fields':'mushroom_island',
 'nether_wastes':'hell','soul_sand_valley':'soulsand_valley',
 'end_highlands':'the_end','end_midlands':'the_end','small_end_islands':'the_end','end_barrens':'the_end'
}
JAVA_BIOMES="""the_void plains sunflower_plains snowy_plains ice_spikes desert swamp mangrove_swamp
forest flower_forest birch_forest dark_forest old_growth_birch_forest old_growth_pine_taiga
old_growth_spruce_taiga taiga snowy_taiga savanna savanna_plateau windswept_hills
windswept_gravelly_hills windswept_forest windswept_savanna jungle sparse_jungle bamboo_jungle
badlands eroded_badlands wooded_badlands meadow cherry_grove grove snowy_slopes frozen_peaks
jagged_peaks stony_peaks river frozen_river beach snowy_beach stony_shore warm_ocean lukewarm_ocean
deep_lukewarm_ocean ocean deep_ocean cold_ocean deep_cold_ocean frozen_ocean deep_frozen_ocean
mushroom_fields dripstone_caves lush_caves deep_dark nether_wastes warped_forest crimson_forest
soul_sand_valley basalt_deltas the_end end_highlands end_midlands small_end_islands end_barrens""".split()
def load(p):
 text=p.read_text(encoding='utf-8-sig')
 text=re.sub(r'("(?:[^"\\]|\\.)*")|(/\*.*?\*/|//[^\r\n]*)',lambda m:m[1] or '',text,flags=re.S)
 return json.loads(text)
def rank(p):return (0,) if p.name=='vanilla' else (1,)+tuple(map(int,p.name.removeprefix('vanilla_').split('.')))
def main():
 parser=argparse.ArgumentParser();parser.add_argument('resource_pack_root',type=Path);args=parser.parse_args()
 packs=sorted([p for p in args.resource_pack_root.iterdir() if p.name=='vanilla' or re.fullmatch(r'vanilla_\d+(?:\.\d+)*',p.name)],key=rank)
 biomes={};fogs={};sources=[]
 def source(p):
  rel=p.relative_to(args.resource_pack_root).as_posix()
  sources.append({'path':rel,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()});return rel
 for pack in packs:
  p=pack/'biomes_client.json'
  if p.is_file():
   src=source(p)
   for key,value in load(p)['biomes'].items():
    record=biomes.setdefault(key,{'values':{},'source':{}})
    record['values'].update(value);record['source'].update({k:src for k in value})
  for p in sorted((pack/'fogs').glob('*.json')):
   doc=load(p)['minecraft:fog_settings'];key=doc['description']['identifier'];src=source(p)
   fogs[key]={'values':doc.get('distance',{}).get('water',{}),'source':src}
 default=biomes['default']['values'];default_fog=fogs[default['fog_identifier']]
 result={};mapping=[]
 for java in sorted(JAVA_BIOMES):
  bedrock=ALIASES.get(java,java);record=biomes.get(bedrock)
  effective=dict(default);effective.update(record['values'] if record else {})
  fog=fogs.get(effective['fog_identifier'],default_fog)
  water=dict(default_fog['values']);water.update(fog['values'])
  surface=effective['water_surface_color'].upper();fog_color=water['fog_color'].upper()
  assert re.fullmatch('#[0-9A-F]{6}',surface) and re.fullmatch('#[0-9A-F]{6}',fog_color)
  key='minecraft:'+java;result[key]={'surface':surface,'fog':fog_color}
  mapping.append({'java':key,'bedrock':bedrock,'default_inherited':record is None,'surface':surface,'fog':fog_color,'surface_source':record['source'].get('water_surface_color',biomes['default']['source']['water_surface_color']) if record else biomes['default']['source']['water_surface_color'],'fog_source':fog['source'],'fog_end':water['fog_end'],'surface_transparency':effective['water_surface_transparency'],'alias_note':'Birch-family color mapping' if java=='old_growth_birch_forest' else ''})
 assert len(result)==64
 resources=ROOT/'src/main/resources/assets/bedrockify';resources.mkdir(parents=True,exist_ok=True)
 (resources/'water_colors.json').write_bytes((json.dumps({'format':1,'biomes':result},indent=2)+'\n').encode())
 manifest={'source':'User-supplied Bedrock 1.21.x vanilla resource packs','layer_order':[p.name for p in packs],'resolution':'Numbered overlays replace biome fields and whole fog resources. Missing biome entries inherit the final default; explicit Java-name aliases listed per entry.','scope':'Surface and underwater fog RGB only. Fog distances, transition curves and surface transparency are recorded but not ported.','sources':sources,'mapping':mapping}
 (ROOT/'docs/water_colors_sources.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
 print('Imported',len(result),'Java biome mappings from',len(sources),'source files')
 print('Default inherited:',[m['java'] for m in mapping if m['default_inherited']])
if __name__=='__main__':main()
