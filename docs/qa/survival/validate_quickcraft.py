"""Native server-owned quick crafting; no test classes enter a release build."""
import ctypes,json,os,sys,time,uuid
from pathlib import Path
root=Path(sys.argv[1]).resolve();records=[];passed=[]
x11=ctypes.CDLL('libX11.so.6');xt=ctypes.CDLL('libXtst.so.6')
x11.XOpenDisplay.argtypes=[ctypes.c_char_p];x11.XOpenDisplay.restype=ctypes.c_void_p
d=x11.XOpenDisplay(os.environ['DISPLAY'].encode());assert d
x11.XStringToKeysym.argtypes=[ctypes.c_char_p];x11.XStringToKeysym.restype=ctypes.c_ulong
x11.XKeysymToKeycode.argtypes=[ctypes.c_void_p,ctypes.c_ulong];x11.XKeysymToKeycode.restype=ctypes.c_uint
x11.XFlush.argtypes=[ctypes.c_void_p];xt.XTestFakeKeyEvent.argtypes=[ctypes.c_void_p,ctypes.c_uint,ctypes.c_int,ctypes.c_ulong]
shift=x11.XKeysymToKeycode(d,x11.XStringToKeysym(b'Shift_L'))
def shift_down(value):
 xt.XTestFakeKeyEvent(d,shift,int(value),0);x11.XFlush(d);time.sleep(.12)
def send(action,**fields):
 cmd={'action':action,'request_id':uuid.uuid4().hex,**fields};result=root/'survival-qa-result.json';result.unlink(missing_ok=True)
 tmp=root/'survival-qa-command.tmp';tmp.write_text(json.dumps(cmd,ensure_ascii=False));tmp.replace(root/'survival-qa-command.json')
 end=time.monotonic()+60
 while time.monotonic()<end:
  try:v=json.loads(result.read_text())
  except (FileNotFoundError,json.JSONDecodeError):time.sleep(.05);continue
  if v.get('request_id')!=cmd['request_id']:time.sleep(.05);continue
  records.append({'command':cmd,'result':v});assert v.get('ok'),v;return v
 raise TimeoutError(cmd)
def until(check):
 end=time.monotonic()+15
 while time.monotonic()<end:
  v=send('status')
  if check(v):return v
  time.sleep(.1)
 raise AssertionError(v)
def amount(v,item,bag=False):
 first=10 if v['screen'].endswith('CraftingScreen') else 9
 slots=v['slots'][first:first+36] if bag else v['slots'][1:]
 return sum(int(s['item'].split(' ',1)[0]) for s in slots if s['item'].endswith(' '+item))
def fresh(kind='inventory',items=None,**fields):
 send('close');until(lambda v:v.get('server_sync_id')==0);send('memory_setting',enabled=False,search=True)
 items=items or [{'id':'minecraft:oak_log','count':64}]
 send('quickcraft_setup',items=items,**fields)
 until(lambda v:v['hotbar'][0]==str(items[0]['count'])+' '+items[0]['id'].split(':')[1])
 send(kind);v=until(lambda v:kind.title()+'Screen' in v['screen'] and v.get('recipe_count',0)>0 and v['sync_id']==v['server_sync_id'])
 if not v['book_open']:send('user_toggle_book')
 if v['craftable_only']:send('filter')
 send('category',index=0)
 return send('search',text='')
def preview(recipe):
 send('search',text=recipe);send('recipe',id=recipe)
 return until(lambda v:v['slots'][0]['item']!='0 air')
def click(recipe,batch=False):
 if batch:shift_down(True)
 try:send('recipe',id=recipe)
 finally:
  if batch:shift_down(False)
 return until(lambda v:not v.get('crafting_active'))
try:
 v=fresh();assert (v['background_width'],v['background_height'])==(198,198)
 assert v['slots'][1]['y']==34 and v['slots'][36]['y']==172
 toggle=next(b for b in v['buttons'] if 'RecipeBookToggle' in b['class']);assert toggle['y']==v['y']+8
 passed.append('compact layout and lower recovery button')
 v=preview('minecraft:oak_planks');assert amount(v,'oak_planks',True)==0
 v=click('minecraft:oak_planks');assert amount(v,'oak_planks',True)==4 and amount(v,'oak_log')==63 and v['cursor']=='0 air'
 v=click('minecraft:oak_planks');assert amount(v,'oak_planks',True)==8 and amount(v,'oak_log')==62
 passed.append('first click previews; repeated clicks craft one recipe directly into inventory')
 fresh(items=[{'id':'minecraft:oak_log','count':1}]);preview('minecraft:oak_planks');v=click('minecraft:oak_planks')
 assert amount(v,'oak_planks',True)==4 and amount(v,'oak_log')==0
 passed.append('the last ingredient already in the grid remains craftable')
 fresh();send('search',text='minecraft:oak_planks');v=click('minecraft:oak_planks',True)
 assert amount(v,'oak_planks',True)==64 and amount(v,'oak_log')==48 and v['cursor']=='0 air'
 passed.append('Shift from an unselected recipe produces one stack, not all available material')
 fresh(items=[{'id':'minecraft:oak_log','count':3}]);send('search',text='minecraft:oak_planks');v=click('minecraft:oak_planks',True)
 assert amount(v,'oak_planks',True)==12 and amount(v,'oak_log')==0
 passed.append('insufficient material stops with a partial stack')
 fresh(items=[{'id':'bedrockifysurvivalqa:metal_0_ingot','count':64}]);send('search',text='bedrockifysurvivalqa:metal_0_nuggets');v=click('bedrockifysurvivalqa:metal_0_nuggets',True)
 assert amount(v,'metal_0_nugget',True)==63 and amount(v,'metal_0_ingot')==57
 passed.append('nine-output recipes stop at 63 instead of exceeding a stack')
 fresh('crafting',items=[{'id':'minecraft:iron_ingot','count':64}]);send('search',text='minecraft:bucket');v=click('minecraft:bucket',True)
 assert amount(v,'bucket',True)==16 and amount(v,'iron_ingot')==16
 passed.append('a sixteen-item stack limit is respected')
 fresh('crafting',items=[{'id':'minecraft:oak_planks','count':64},{'id':'minecraft:oak_planks','count':64}]);send('search',text='minecraft:oak_stairs');v=click('minecraft:oak_stairs',True)
 assert amount(v,'oak_stairs',True)==64 and amount(v,'oak_planks')==32
 passed.append('native shaped 3x3 batch ingredients and output')
 fresh(items=[{'id':'minecraft:oak_log','count':64},{'id':'minecraft:diamond','count':1}]);until(lambda v:v['hotbar'][1]=='1 diamond' and v['cursor']=='0 air');send('slot',id=37,button=0,type='PICKUP');until(lambda v:v['cursor']=='1 diamond');preview('minecraft:oak_planks');v=click('minecraft:oak_planks')
 assert amount(v,'oak_planks',True)==0 and v['cursor']=='1 diamond'
 passed.append('occupied cursor is preserved')
 fresh(full=True);preview('minecraft:oak_planks');v=click('minecraft:oak_planks',True)
 assert amount(v,'oak_planks',True)==0 and amount(v,'oak_log')==64 and v['cursor']=='0 air'
 passed.append('full inventory leaves output and ingredients untouched')
 fresh(items=[{'id':'minecraft:diamond','count':64}]);v=preview('bedrockifysurvivalqa:stress_123');nbt=v['slots'][0]['nbt'];v=click('bedrockifysurvivalqa:stress_123')
 actual=next(s for s in v['slots'][9:45] if s['item']=='1 metal_0_nugget');assert actual['nbt']==nbt and amount(v,'diamond')==63
 passed.append('server-selected overlapping recipe output NBT remains intact')
 fresh('crafting',items=[{'id':'minecraft:milk_bucket','count':1}]*3+[{'id':'minecraft:sugar','count':2},{'id':'minecraft:egg','count':1},{'id':'minecraft:wheat','count':3}]);preview('minecraft:cake');v=click('minecraft:cake')
 assert amount(v,'cake',True)==1 and amount(v,'bucket')==3
 passed.append('native crafting remainder buckets are preserved')
 fresh();send('search',text='minecraft:oak_planks');shift_down(True)
 try:send('recipe',id='minecraft:oak_planks')
 finally:shift_down(False)
 send('jei_recipes');until(lambda v:'RecipesGui' in v['screen']);time.sleep(.4);send('return_screen');v=until(lambda v:'InventoryScreen' in v['screen'])
 assert not v['crafting_active'];before=amount(v,'oak_planks',True);time.sleep(.3);v=send('status');assert amount(v,'oak_planks',True)==before
 passed.append('JEI screen transition cancels pending quick crafting')
 send('close');send('memory_setting',enabled=True,search=True)
 out={'ok':True,'passed':passed,'operations':len(records),'records':records};print(json.dumps({k:v for k,v in out.items() if k!='records'},ensure_ascii=False,indent=2))
except Exception as e:
 out={'ok':False,'error':repr(e),'passed':passed,'operations':len(records),'records':records};print(json.dumps({k:v for k,v in out.items() if k!='records'},ensure_ascii=False,indent=2));raise
finally:
 shift_down(False);(root/'draft-quickcraft-validation.json').write_text(json.dumps(out,ensure_ascii=False,indent=2))
