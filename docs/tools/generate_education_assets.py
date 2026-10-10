"""Generate original education sprites/models. Requires Pillow; run from any directory."""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
A=ROOT/'assets/bedrockify'; D=ROOT/'data/bedrockify'
SYMBOLS='? H He Li Be B C N O F Ne Na Mg Al Si P S Cl Ar K Ca Sc Ti V Cr Mn Fe Co Ni Cu Zn Ga Ge As Se Br Kr Rb Sr Y Zr Nb Mo Tc Ru Rh Pd Ag Cd In Sn Sb Te I Xe Cs Ba La Ce Pr Nd Pm Sm Eu Gd Tb Dy Ho Er Tm Yb Lu Hf Ta W Re Os Ir Pt Au Hg Tl Pb Bi Po At Rn Fr Ra Ac Th Pa U Np Pu Am Cm Bk Cf Es Fm Md No Lr Rf Db Sg Bh Hs Mt Ds Rg Cn Nh Fl Mc Lv Ts Og'.split()
assert len(SYMBOLS)==119

def data(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def png(name,image,kind='block'):
 path=A/'textures'/kind/(name+'.png');path.parent.mkdir(parents=True,exist_ok=True);image.save(path)
def loot(name,item=None):
 data(D/'loot_tables/blocks'/(name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'bedrockify:'+(item or name)}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
def cube(name):
 data(A/'blockstates'/(name+'.json'),{'variants':{'':{'model':'bedrockify:block/'+name}}})
 data(A/'models/block'/(name+'.json'),{'parent':'minecraft:block/cube_all','textures':{'all':'bedrockify:block/'+name}})
 data(A/'models/item'/(name+'.json'),{'parent':'bedrockify:block/'+name});loot(name)
def face(color):
 im=Image.new('RGBA',(32,32),color);dr=ImageDraw.Draw(im);dr.rectangle((0,0,31,31),outline=(38,44,48));dr.line((1,1,30,1),fill=(210,220,220));dr.line((1,1,1,30),fill=(210,220,220));return im,dr

def elements():
 font=ImageFont.load_default()
 for n,symbol in enumerate(SYMBOLS):
  color=(128,141,155)
  if n in [1,6,7,8,15,16,34]:color=(91,151,103)
  elif n in [2,10,18,36,54,86,118]:color=(120,157,188)
  elif n in [9,17,35,53,85,117]:color=(98,174,154)
  elif n in [3,11,19,37,55,87]:color=(185,98,94)
  elif n in [4,12,20,38,56,88]:color=(195,154,87)
  elif 57<=n<=71:color=(162,116,171)
  elif 89<=n<=103:color=(185,116,146)
  im,dr=face(color);dr.text((3,2),str(n) if n else '?',fill='white',font=font)
  box=dr.textbbox((0,0),symbol,font=font);dr.text(((32-box[2])/2,13),symbol,fill='white',font=font)
  name='element_'+str(n);png(name,im);cube(name)

def torches():
 for name,color in [('underwater',(85,197,222)),('blue',(67,133,239)),('red',(239,73,60)),('purple',(179,97,237)),('green',(99,210,69))]:
  name+='_torch';wall=name.replace('_torch','_wall_torch')
  im=Image.new('RGBA',(16,16));dr=ImageDraw.Draw(im);dr.rectangle((7,6,8,15),fill=(126,89,48));dr.line((7,7,7,15),fill=(183,139,80));dr.rectangle((6,3,9,6),fill=color);dr.rectangle((7,3,8,4),fill=(237,245,230));png(name,im)
  data(A/'blockstates'/(name+'.json'),{'variants':{'':{'model':'bedrockify:block/'+name}}})
  data(A/'blockstates'/(wall+'.json'),{'variants':{'facing='+f:{'model':'bedrockify:block/'+wall,'y':y} for f,y in [('east',0),('south',90),('west',180),('north',270)]}})
  for model,parent in [(name,'torch'),(wall,'template_torch_wall')]:data(A/'models/block'/(model+'.json'),{'parent':'minecraft:block/'+parent,'textures':{'torch':'bedrockify:block/'+name}})
  data(A/'models/item'/(name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'bedrockify:block/'+name}});loot(name);loot(wall,name)

def reducer():
 for side in ['front','side','top','bottom']:
  im,dr=face((164,168,165));dr.rectangle((4,4,27,27),fill=(68,79,83),outline=(48,54,58))
  if side=='front':
   dr.rectangle((9,6,22,12),fill=(100,166,128));dr.rectangle((6,19,25,23),fill=(137,149,155))
   for x in range(8,26,4):dr.line((x,20,x,22),fill=(50,58,63))
  elif side=='top':
   dr.rectangle((9,6,22,11),fill=(117,165,188));dr.line((15,12,15,18),fill=(170,180,183))
   for x in [8,15,22]:dr.rectangle((x-2,20,x+2,24),fill=(117,165,188))
  else:
   for y in range(8,25,4):dr.line((7,y,24,y),fill=(118,130,136))
  png('material_reducer_'+side,im)
 name='material_reducer'
 data(A/'models/block'/(name+'.json'),{'parent':'minecraft:block/orientable_with_bottom','textures':{k:'bedrockify:block/'+name+'_'+v for k,v in [('front','front'),('side','side'),('top','top'),('bottom','bottom')]}})
 data(A/'models/item'/(name+'.json'),{'parent':'bedrockify:block/'+name})
 data(A/'blockstates'/(name+'.json'),{'variants':{'facing='+f:{'model':'bedrockify:block/'+name,'y':y} for f,y in [('north',0),('east',90),('south',180),('west',270)]}});loot(name)
 for name,color in [('cerium',(67,133,239)),('mercuric',(239,73,60)),('potassium',(179,97,237)),('tungsten',(99,210,69))]:
  name+='_chloride';im=Image.new('RGBA',(16,16));dr=ImageDraw.Draw(im)
  dr.rectangle((6,1,9,3),fill=color);dr.rectangle((5,4,10,13),fill=(132,163,174));dr.rectangle((4,6,11,12),fill=(199,220,223));dr.rectangle((5,9,10,12),fill=color);dr.line((5,6,5,8),fill='white');png(name,im,'item')
  data(A/'models/item'/(name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'bedrockify:item/'+name}})

def crafting():
 for output,input_ in [('underwater_torch','element_12'),('blue_torch','cerium_chloride'),('red_torch','mercuric_chloride'),('purple_torch','potassium_chloride'),('green_torch','tungsten_chloride')]:
  data(D/'recipes/education'/(output+'.json'),{'type':'minecraft:crafting_shaped','category':'misc','pattern':['C','T'],'key':{'C':{'item':'bedrockify:'+input_},'T':{'item':'minecraft:torch'}},'result':{'item':'bedrockify:'+output,'count':1}})
  data(D/'advancements/recipes/education'/(output+'.json'),{'parent':'minecraft:recipes/root','criteria':{'ingredient':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['bedrockify:'+input_]}]}},'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'bedrockify:education/'+output}}},'requirements':[['ingredient','has_the_recipe']],'rewards':{'recipes':['bedrockify:education/'+output]}})

if __name__=='__main__':
 elements();torches();reducer();crafting();print('Generated all education assets and torch recipes')
