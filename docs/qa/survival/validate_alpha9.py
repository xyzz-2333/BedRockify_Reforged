"""Native inventory state restoration, screen transitions and Forge mod-tab pages."""
import json,time,uuid,sys
from pathlib import Path
root=Path(sys.argv[1]).resolve();records=[];passed=[]
def send(action,scope='survival',**fields):
 cmd={'action':action,'request_id':uuid.uuid4().hex,**fields};result=root/(scope+'-qa-result.json');result.unlink(missing_ok=True)
 tmp=root/(scope+'-qa-command.tmp');tmp.write_text(json.dumps(cmd,ensure_ascii=False));tmp.replace(root/(scope+'-qa-command.json'))
 end=time.monotonic()+60
 while time.monotonic()<end:
  try:v=json.loads(result.read_text())
  except (FileNotFoundError,json.JSONDecodeError):time.sleep(.05);continue
  if v.get('request_id')!=cmd['request_id']:time.sleep(.05);continue
  records.append({'scope':scope,'command':cmd,'result':v});assert v.get('ok'),v;return v
 raise TimeoutError(cmd)
def until(check,scope='survival'):
 end=time.monotonic()+20
 while time.monotonic()<end:
  v=send('status',scope=scope)
  if check(v):return v
  time.sleep(.1)
 raise AssertionError(v)
def opened(kind):
 send(kind);v=until(lambda v:kind.title()+'Screen' in v['screen'] and v.get('recipe_count',0)>0)
 if not v['book_open']:send('user_toggle_book');v=send('status')
 if v['craftable_only']:send('filter');v=send('status')
 return v
def signature(v):return tuple(v[k] for k in ['category','search_text','page','expanded_groups'])
def creativeGroups(v):return sorted(part.strip() for part in v['expanded'].strip('[]').split(',') if part.strip())
try:
 send('close');send('memory_setting',enabled=True,search=True);send('memory_reset');send('setup')
 until(lambda v:v['hotbar'][0]=='64 oak_log')
 v=opened('inventory');send('search',text='');send('category',index=1);send('expand',enabled=True)
 send('scroll',amount=-1);v=send('scroll',amount=-1);expected=signature(v);assert v['page']==2 and v['expanded_groups']
 send('close');v=opened('inventory');assert signature(v)==expected,(signature(v),expected)
 v=send('resize',width=450,height=260);assert signature(v)==expected and v['classic']
 v=send('resize',width=400,height=230);assert not v['classic']
 v=send('resize',width=640,height=360);assert signature(v)==expected and v['classic']
 passed.append('inventory category, groups and page survive reopening and layout fallback')
 send('category',index=0);send('search',text='木板');v=send('scroll',amount=-1);expected=signature(v);assert v['page']==1
 send('close');send('memory_reload');v=opened('inventory');assert signature(v)==expected
 send('jei_recipes');until(lambda v:'RecipesGui' in v['screen']);send('return_screen');v=until(lambda v:'InventoryScreen' in v['screen']);assert signature(v)==expected
 passed.append('Chinese search and offset survive disk reload and real JEI navigation')
 v=opened('crafting');send('category',index=3);send('search',text='ingot');v=send('scroll',amount=-1);craft=signature(v);assert v['page']==1
 send('close');v=opened('inventory');assert signature(v)==expected
 send('close');v=opened('crafting');assert signature(v)==craft
 passed.append('2x2 inventory and 3x3 crafting profiles remain independent')
 send('memory_setting',enabled=True,search=False);send('close');v=opened('inventory');assert v['search_text']=='' and v['page']==0
 send('close');v=opened('crafting');assert v['search_text']=='' and v['page']==0
 send('memory_setting',enabled=False,search=True);send('category',index=4);send('close');v=opened('inventory');assert v['category']==0 and not v['expanded_groups']
 passed.append('search-memory and complete-memory switches restore default behavior')
 send('close');send('memory_setting',enabled=True,search=True);send('memory_reset');send('command',text='gamemode creative');time.sleep(.4);send('creative')
 v=send('tab',scope='creative',id='minecraft:building_blocks')
 w=v['columns']*20+32;send('mouse',scope='creative',x=v['x']+w-45,y=v['y']+13,button=0)
 v=send('scroll',scope='creative',amount=-2);first=v['first_visible'];groups=creativeGroups(v);assert first>0
 send('tab',scope='creative',id='minecraft:natural_blocks');send('tab',scope='creative',id='minecraft:building_blocks');v=send('status',scope='creative');assert v['first_visible']==first and creativeGroups(v)==groups
 send('close',scope='creative');send('memory_reload');send('creative');v=send('status',scope='creative');assert v['tab']=='minecraft:building_blocks' and v['first_visible']==first and creativeGroups(v)==groups
 v=send('resize',scope='creative',width=450,height=360);assert v['first_visible']>0 and creativeGroups(v)==groups
 send('resize',scope='creative',width=640,height=360)
 passed.append('creative per-tab grouping and list offset survive tabs, disk reload and resizing')
 send('tab',scope='creative',id='minecraft:search');send('search',scope='creative',text='minecraft:');v=send('scroll',scope='creative',amount=-2);first=v['first_visible'];assert first>0
 send('close',scope='creative');send('memory_reload');send('creative');v=send('status',scope='creative');assert v['search_text']=='minecraft:' and v['first_visible']==first and v['tab']=='minecraft:search'
 send('search',scope='creative',text='木板');send('close',scope='creative');send('creative');v=send('status',scope='creative');assert v['search_text']=='木板'
 passed.append('creative English and Chinese searches restore through native search trees')
 v=send('status',scope='creative');send('page',scope='creative',index=v['page_count']-1);send('tab',scope='creative',id='bedrockifyqa:tab_9');send('search',scope='creative',text='sample')
 send('close',scope='creative');send('memory_reload');send('creative');v=send('status',scope='creative');assert v['tab']=='bedrockifyqa:tab_9' and v['tab'] in v['tabs'] and v['search_text']=='sample'
 v=send('page',scope='creative',index=0);visible=v['tabs'];send('close',scope='creative');send('creative');v=send('status',scope='creative');assert v['tabs']==visible and v['tab']=='bedrockifyqa:tab_9'
 passed.append('mod tab and visible Forge page are remembered independently')
 send('tab',scope='creative',id='minecraft:hotbar');v=send('status',scope='creative');assert not v['classic'] and v['slots']==54
 send('close',scope='creative');send('creative');v=send('status',scope='creative');assert v['tab']=='minecraft:hotbar' and not v['classic'] and v['slots']==54
 send('tab',scope='creative',id='minecraft:inventory');send('close',scope='creative');send('creative');v=send('status',scope='creative');assert v['tab']=='minecraft:inventory' and not v['classic']
 passed.append('saved-hotbar and inventory special tabs keep native layouts')
 send('close',scope='creative');v=send('memory_state');assert v['state_bytes']<65536
 passed.append('persisted state is bounded lightweight metadata')
 output={'ok':True,'passed':passed,'operations':len(records),'records':records};print(json.dumps({'ok':True,'passed':passed,'operations':len(records)},ensure_ascii=False,indent=2))
except Exception as e:
 output={'ok':False,'passed':passed,'error':repr(e),'operations':len(records),'records':records};print(json.dumps({k:v for k,v in output.items() if k!='records'},ensure_ascii=False,indent=2));raise
finally:(root/'alpha9-memory-validation.json').write_text(json.dumps(output,ensure_ascii=False,indent=2))
