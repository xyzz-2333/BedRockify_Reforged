"""Exact known 1.20.1 inputs from Minecraft Wiki's Material Reducer table.
No chemical guesses for modded items; extend these recipes with a datapack.
"""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[2]/'src/main/resources/data/bedrockify/recipes/education/reducing'
ROOT.mkdir(parents=True,exist_ok=True)
def recipe(name,items,parts):
 ingredients=[{'item':i if ':' in i else 'minecraft:'+i} for i in items]
 assert sum((n+63)//64 for _,n in parts)<=9
 value={'type':'bedrockify:material_reducing','ingredient':ingredients,'elements':[{'element':e,'count':n} for e,n in parts]}
 (ROOT/(name+'.json')).write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8',newline='\n')
woods=[]
for w in 'oak spruce birch jungle acacia dark_oak mangrove cherry'.split():
 woods.extend([w+'_'+s for s in ['log','wood','planks','stairs','slab']]+['stripped_'+w+'_'+s for s in ['log','wood']])
recipe('wood',woods,[(6,49),(1,6),(8,44),(7,1)])
recipe('grass',['grass_block','mycelium'],[(6,15),(8,70),(7,8),(15,7)])
recipe('dirt',['dirt'],[(14,21),(20,8),(15,3),(7,3),(26,13),(12,6),(30,2),(29,2),(8,42)])
stone='andesite diorite granite polished_andesite polished_diorite polished_granite gravel obsidian red_sand sand stone quartz_block quartz_pillar chiseled_quartz_block smooth_quartz quartz_stairs quartz_slab smooth_quartz_stairs smooth_quartz_slab nether_quartz_ore quartz'.split()
for base in ['sandstone','red_sandstone']:
 stone.extend([base,'cut_'+base,'chiseled_'+base,'smooth_'+base,base+'_stairs',base+'_slab','cut_'+base+'_slab','smooth_'+base+'_stairs','smooth_'+base+'_slab'])
recipe('silica',stone,[(14,33),(8,67)])
recipe('cobblestone','cobblestone cobblestone_stairs cobblestone_slab cobblestone_wall lava_bucket magma_block'.split(),[(8,47),(14,28),(13,8),(26,5),(20,4),(11,3),(19,3),(12,2)])
recipe('mossy_cobblestone',['mossy_cobblestone','mossy_cobblestone_wall'],[(8,45),(14,28),(13,8),(26,5),(20,4),(11,3),(19,3),(12,2),(6,2)])
colors='white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()
recipe('clay',['clay','terracotta']+[c+'_glazed_terracotta' for c in colors],[(14,77),(13,17),(26,3),(12,1),(20,1),(8,1)])
recipe('water','water_bucket ice packed_ice blue_ice snow_block snowball snow'.split(),[(1,67),(8,33)])
recipe('coal','coal_ore coal coal_block'.split(),[(1,6),(6,50),(8,43),(7,1)])
for name,n in [('copper',29),('iron',26),('gold',79),('diamond',6)]:recipe(name+'_ore',[name+'_ore'],[(n,64),(14,12),(8,24)])
recipe('lapis','lapis_ore lapis_lazuli lapis_block'.split(),[(11,13),(16,8),(13,13),(8,53),(14,13)])
recipe('redstone','redstone_ore redstone redstone_block'.split(),[(6,31),(92,31),(0,38)])
recipe('emerald','emerald_ore emerald emerald_block'.split(),[(13,15),(4,25),(14,20),(8,40)])
for name,n,items in [('iron',26,['iron_ingot','iron_nugget','iron_block']),('gold',79,['gold_ingot','gold_nugget','gold_block']),('diamond',6,['diamond','diamond_block'])]:recipe(name,items,[(n,100)])
recipe('netherrack',['netherrack'],[(14,64),(8,18),(80,15),(0,3)])
recipe('end_stone',['end_stone'],[(14,59),(6,13),(0,28)])
recipe('glowstone',['glowstone','glowstone_dust'],[(18,20),(5,20),(36,20),(10,20),(0,20)])
recipe('soul_sand',['soul_sand'],[(14,37),(8,3),(0,60)])
recipe('ink_sac',['ink_sac'],[(26,1),(16,1),(8,4)])
recipe('sugar',['sugar'],[(6,6),(1,12),(8,6)])
recipe('charcoal',['charcoal'],[(6,7),(1,4),(8,1)])
for name,n,cl in [('cerium',58,3),('mercuric',80,2),('potassium',19,1),('tungsten',74,6)]:recipe(name+'_chloride',['bedrockify:'+name+'_chloride'],[(n,1),(17,cl)])
print('Generated',len(list(ROOT.glob('*.json'))),'material reducing recipes')
