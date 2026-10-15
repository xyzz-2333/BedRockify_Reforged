"""Import original education textures; never synthesizes replacement textures.
Usage: python docs/tools/import_education2.py chemistry.zip
"""
from pathlib import Path
import json,zipfile,hashlib,sys
R=Path(__file__).resolve().parents[2]; A=R/'src/main/resources/assets/bedrockify';D=R/'src/main/resources/data/bedrockify'
def write(p,data):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(data,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
def asset(path,data):write(A/path,data)
def data(path,value):write(D/path,value)
textures={};labels={}
def tex(dest,src):textures[dest]=src;return 'bedrockify:'+dest[:-4]
def cube(name,sides):
 asset(Path('blockstates')/(name+'.json'),{'variants':{'':{'model':'bedrockify:block/'+name}}})
 asset(Path('models/block')/(name+'.json'),{'parent':'minecraft:block/cube','textures':dict(sides,particle=sides['north'])})
 asset(Path('models/item')/(name+'.json'),{'parent':'bedrockify:block/'+name})
def loot(name):data(Path('loot_tables/blocks')/(name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'bedrockify:'+name}]}]})
colors=['','white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
for color in colors:
 name=(color+'_' if color else '')+'hardened_glass';bed=color.replace('light_gray','silver')
 face=tex('block/'+name+'.png','textures/blocks/hard_glass'+('_'+bed if color else '')+'.png')
 cube(name,{f:face for f in ['up','down','north','south','east','west']});loot(name)
 labels['block.bedrockify.'+name]='tile.hard_stained_glass.'+bed+'.name' if color else 'tile.hard_glass.name'
 pane=name+'_pane';edge='minecraft:block/'+(color+'_stained_' if color else '')+'glass_pane_top'
 for variant in ['post','side','side_alt','noside','noside_alt']:
  asset(Path('models/block')/(pane+'_'+variant+'.json'),{'parent':'minecraft:block/template_glass_pane_'+variant,'textures':{'pane':face,'edge':edge}})
 parts=[{'apply':{'model':'bedrockify:block/'+pane+'_post'}}]
 for direction,rotation,side,none in [('north',0,'side','noside'),('east',90,'side','noside'),('south',0,'side_alt','noside_alt'),('west',90,'side_alt','noside_alt')]:
  for yes,variant in [('true',side),('false',none)]:
   parts.append({'when':{direction:yes},'apply':{'model':'bedrockify:block/'+pane+'_'+variant,'y':rotation}})
 asset(Path('blockstates')/(pane+'.json'),{'multipart':parts})
 asset(Path('models/item')/(pane+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':face}});loot(pane)
 labels['block.bedrockify.'+pane]='tile.hard_stained_glass_pane.'+bed+'.name' if color else 'tile.hard_glass_pane.name'
 # Three glass and six compound items, matching the education crafting recipe.
 for block in [name,pane]:
  source='minecraft:'+(color+'_stained_' if color else '')+('glass_pane' if block==pane else 'glass')
  data(Path('recipes/education/education2')/(block+'.json'),{'type':'minecraft:crafting_shaped','pattern':['AAA','GGG','BBB'],'key':{'A':{'item':'bedrockify:aluminum_oxide'},'B':{'item':'bedrockify:boron_trioxide'},'G':{'item':source}},'result':{'item':'bedrockify:'+block,'count':3}})
name='underwater_tnt';faces={}
for face in ['top','side','bottom']:faces[face]=tex('block/'+name+'_'+face+'.png','textures/blocks/tnt_'+face+'_underwater.png')
cube(name,{'up':faces['top'],'down':faces['bottom'],**{f:faces['side'] for f in ['north','east','south','west']}});loot(name);labels['block.bedrockify.'+name]='tile.underwater_tnt.name'
data(Path('recipes/education/education2/underwater_tnt.json'),{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:tnt'},{'item':'bedrockify:element_11'}],'result':{'item':'bedrockify:underwater_tnt'}})
# Actual cuboid textures from the original education pack.
station_faces={
 'element_constructor':{'up':'top','down':'top','north':'front','south':'back','east':'side_a','west':'side_b'},
 'compound_creator':{'up':'top','down':'inside_bottom','north':'side_a','south':'side_b','east':'side_c','west':'side_c'},
 'lab_table':{'up':'top','down':'bottom','north':'front','south':'side_c','east':'side_a','west':'side_b'}}
for name,mapping in station_faces.items():
 sides={face:tex('block/'+name+'_'+source+'.png','textures/blocks/'+name+'_'+source+'.png') for face,source in mapping.items()}
 cube(name,sides);loot(name)
 asset(Path('blockstates')/(name+'.json'),{'variants':{'facing='+d:{'model':'bedrockify:block/'+name,'y':r} for d,r in [('north',0),('east',90),('south',180),('west',270)]}})
 labels['block.bedrockify.'+name]='tile.'+name.replace('_','')+'.name'
# name, formula by atomic number, original compound texture.
compounds=[
 ('salt',{11:1,17:1},'salt'),('sodium_oxide',{11:2,8:1},'jar_white'),('sodium_hydroxide',{11:1,8:1,1:1},'jar_white'),
 ('magnesium_nitrate',{12:1,7:2,8:6},'jar_yellow'),('iron_sulfide',{26:1,16:1},'jar_black'),('lithium_hydride',{3:1,1:1},'jar_white'),
 ('sodium_hydride',{11:1,1:1},'jar_light_grey'),('calcium_bromide',{20:1,35:2},'jar_white'),('magnesium_oxide',{12:1,8:1},'jar_white'),
 ('sodium_acetate',{6:2,1:3,11:1,8:2},'jar_white'),('luminol',{6:8,1:7,7:3,8:2},'jar_clear'),
 ('charcoal_compound',{6:7,1:4,8:1},'jar_black'),('sugar_compound',{6:6,1:12,8:6},'jar_white'),
 ('aluminum_oxide',{13:2,8:3},'jar_brown'),('boron_trioxide',{5:2,8:3},'jar_white'),('soap',{6:18,1:35,11:1,8:2},'jar_white'),
 ('polyethylene',{6:10,1:20},'jar_clear'),('magnesium_salts',{12:1,16:1,8:4},'jar_white'),('sulfate',{16:1,8:4},'jar_white'),
 ('barium_sulfate',{56:1,16:1,8:4},'jar_white'),('potassium_chloride',{19:1,17:1},'jar_white'),('mercuric_chloride',{80:1,17:2},'jar_white'),
 ('cerium_chloride',{58:1,17:3},'jar_white'),('tungsten_chloride',{74:1,17:6},'jar_blue'),('calcium_chloride',{20:1,17:2},'jar_white'),
 ('water_compound',{1:2,8:1},'beaker_blue'),('glue',{6:5,1:5,7:1,8:2},'glue'),('hypochlorite',{17:1,8:1},'beaker_white'),
 ('crude_oil',{6:9,1:20},'beaker_black'),('latex',{6:5,1:8},'beaker_white'),('potassium_iodide',{19:1,53:1},'beaker_yellow'),
 ('sodium_fluoride',{11:1,9:1},'beaker_white'),('benzene',{6:6,1:6},'beaker_yellow'),('ink_compound',{26:1,16:1,8:4},'beaker_black'),
 ('hydrogen_peroxide',{1:2,8:2},'beaker_clear'),('ammonia',{7:1,1:3},'beaker_clear'),('sodium_hypochlorite',{11:1,17:1,8:1},'beaker_clear')]
for name,formula,texture in compounds:
 t=tex('item/'+name+'.png','textures/items/compounds/'+texture+'.png')
 asset(Path('models/item')/(name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':t}})
 labels['item.bedrockify.'+name]='item.compound.'+name.removesuffix('_compound').replace('_','')+'.name'
 data(Path('recipes/education/education2/chemistry')/(name+'.json'),{'type':'bedrockify:chemistry','station':'compound_creator','inputs':{'bedrockify:element_'+str(k):v for k,v in formula.items()},'result':{'item':'bedrockify:'+name}})
for name,src,key in [('garbage','compounds/garbage','item.compound.garbage.name'),('bleach','bleach','item.bleach.name'),('super_fertilizer','rapid_fertilizer','item.rapid_fertilizer.name'),('ice_bomb','ice_bomb','item.ice_bomb.name')]:
 t=tex('item/'+name+'.png','textures/items/'+src+'.png');asset(Path('models/item')/(name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':t}});labels['item.bedrockify.'+name]=key
face=tex('block/heat_block.png','textures/blocks/chemical_heat.png');cube('heat_block',{f:face for f in ['up','down','north','south','east','west']});loot('heat_block');labels['block.bedrockify.heat_block']='tile.chemical_heat.name'
for name,inputs in [('bleach',{'water_compound':3,'sodium_hypochlorite':3}),('heat_block',{'element_26':1,'water_compound':1,'charcoal_compound':1,'salt':1}),('ice_bomb',{'sodium_acetate':4}),('super_fertilizer',{'ammonia':1,'element_15':1})]:
 data(Path('recipes/education/education2/chemistry')/(name+'.json'),{'type':'bedrockify:chemistry','station':'lab_table','inputs':{'bedrockify:'+k:v for k,v in inputs.items()},'result':{'item':'bedrockify:'+name}})
# Bleach restores the white base variant, including banners without carrying old patterns.
for color in colors[1:]:
 for family in ['wool','carpet','bed','banner']:
  data(Path('recipes/education/education2/bleach')/('bleach_'+color+'_'+family+'.json'),{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'bedrockify:bleach'},{'item':'minecraft:'+color+'_'+family}],'result':{'item':'minecraft:white_'+family}})
asset(Path('textures/block/heat_block.png.mcmeta'),{'animation':{'frametime':4}})
with zipfile.ZipFile(sys.argv[1]) as z:
 manifest=[]
 for dest,src in textures.items():
  raw=z.read(src);p=A/'textures'/dest;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
  manifest.append({'target':p.relative_to(R).as_posix(),'source':src,'sha256':hashlib.sha256(raw).hexdigest()})
 def lang(code):
  path='texts/'+code+'.lang'
  if path not in z.namelist():path='texts/en_US.lang'
  return {l.split('=',1)[0]:l.split('=',1)[1].split('\t#')[0].strip() for l in z.read(path).decode('utf-8-sig').splitlines() if '=' in l and not l.startswith('#')}
 en=lang('en_US')
 for p in (A/'lang').glob('*.json'):
  code=p.stem.split('_');local=lang(code[0]+'_'+code[1].upper());d=json.loads(p.read_text(encoding='utf-8'))
  for key,source in labels.items():d[key]=local.get(source,en.get(source,key.split('.')[-1].replace('_',' ').title()))
  zh=p.stem=='zh_cn'
  d['item.bedrockify.sugar_compound']='糖' if zh else 'Sugar'
  d['item.bedrockify.ink_compound']='墨汁' if zh else 'Ink'
  d.update({'bedrockify.creative.group.hardened_glass':'强化玻璃' if zh else 'Hardened Glass','bedrockify.creative.group.hardened_glass_panes':'强化玻璃板' if zh else 'Hardened Glass Panes','bedrockify.creative.group.compounds':'化合物' if zh else 'Compounds','bedrockify.education.combine':'合成' if zh else 'Combine','bedrockify.education.protons':'质子' if zh else 'Protons','bedrockify.education.neutrons':'中子' if zh else 'Neutrons','bedrockify.education.electrons':'电子' if zh else 'Electrons','bedrockify.education.invalidAtom':'需要 1–118 个质子且电子数相等' if zh else 'Use 1–118 protons and equal electrons','entity.bedrockify.underwater_tnt':local.get('tile.underwater_tnt.name','Underwater TNT'),'entity.bedrockify.ice_bomb':local.get('item.ice_bomb.name','Ice Bomb')})
  write(p,d)
# Keep shared alpha12 asset provenance accurate when a compound texture is corrected.
old_manifest=R/'docs/education_resources.json'
old=json.loads(old_manifest.read_text(encoding='utf-8'))
by_target={entry['target']:entry for entry in manifest}
for entry in old['textures']:
 target='src/main/resources/assets/bedrockify/'+entry['destination']
 if target in by_target:
  entry['source']=by_target[target]['source'];entry['sha256']=by_target[target]['sha256']
write(old_manifest,old)
write(R/'docs/education2_resources.json',{'source':'User-supplied Bedrock chemistry resource pack','textures':manifest,'compounds':[{ 'name':n,'elements':f} for n,f,t in compounds]})
print('Imported',len(textures),'original textures;',len(compounds),'compound recipes.')
