"""Exercise native creative text input, results, page navigation and UI fallback."""
import json,sys,time,uuid
from pathlib import Path
root=Path(sys.argv[1]);records=[];passed=[]
def send(action,**fields):
 cmd={'action':action,'request_id':uuid.uuid4().hex,**fields};result=root/'creative-qa-result.json';result.unlink(missing_ok=True)
 tmp=root/'creative-qa-command.tmp';tmp.write_text(json.dumps(cmd,ensure_ascii=False));tmp.replace(root/'creative-qa-command.json')
 end=time.monotonic()+60
 while time.monotonic()<end:
  try:r=json.loads(result.read_text())
  except (FileNotFoundError,json.JSONDecodeError):time.sleep(.05);continue
  if r.get('request_id')==cmd['request_id']:records.append({'command':cmd,'result':r});assert r.get('ok'),r;return r
  time.sleep(.05)
 raise TimeoutError(action)
try:
 send('open');s=send('tab',id='minecraft:search');assert s['classic'] and s['search_visible']
 send('search',text='');send('mouse',x=s['search_x']+3,y=s['search_y']+3,button=0)
 s=send('char',text='diamond');assert s['search_text']=='diamond' and s['items']>0 and s['search_focused'];send('key',code=259);s=send('status');assert s['search_text']=='diamon'
 passed.append('English input, native search results and Backspace')
 send('search',text='');s=send('char',text='木板');assert s['search_text']=='木板'
 send('key',code=259);s=send('status');assert s['search_text']=='木'
 send('search',text='');s=send('char',text='wwww测试');assert s['search_text']=='wwww测试'
 send('key',code=268);s=send('char',text='Q');assert s['search_text']=='Qwwww测试'
 send('key',code=269);send('key',code=259);s=send('status');assert s['search_text']=='Qwwww测'
 send('key',code=268);send('key',code=261);s=send('status');assert s['search_text']=='wwww测'
 passed.append('Chinese and mixed input, Home/End, insertion, Backspace and Delete')
 s=send('search',text='wwww'*20);assert len(s['search_text'])==50
 s=send('resize',width=500,height=300);assert s['classic'] and s['search_width']>0
 s=send('resize',width=640,height=360);assert s['classic']
 passed.append('long input uses native limit and resized search geometry')
 s=send('tab',id='minecraft:building_blocks');assert not s['search_visible']
 s=send('tab',id='minecraft:hotbar');assert not s['classic'] and s['source_items']==0
 s=send('tab',id='minecraft:inventory');assert not s['classic']
 s=send('tab',id='minecraft:search');assert s['classic'] and s['search_visible']
 s=send('page',index=1);assert s['page_count']>1
 s=send('tab',id='bedrockifyqa:tab_0');assert s['search_visible']
 send('search',text='');send('mouse',x=s['search_x']+2,y=s['search_y']+2,button=0);s=send('char',text='planks');assert s['search_text']=='planks' and s['items']>0
 passed.append('Forge mod-tab search and native saved-hotbar/inventory pages')
 s=send('fallback');s=send('tab',id='minecraft:search');assert not s['classic']
 send('search',text='');send('mouse',x=s['search_x']+2,y=s['search_y']+2,button=0);s=send('char',text='diamond');assert s['search_text']=='diamond' and s['items']>0
 send('open');send('tab',id='minecraft:search');send('search',text='wwww测试')
 passed.append('disabled classic UI retains native search rendering and input')
 out={'ok':True,'passed':passed,'operations':len(records),'records':records};print(json.dumps({k:v for k,v in out.items() if k!='records'},ensure_ascii=False,indent=2))
except Exception as e:
 out={'ok':False,'passed':passed,'error':repr(e),'operations':len(records),'records':records};print(json.dumps({k:v for k,v in out.items() if k!='records'},ensure_ascii=False,indent=2));raise
finally:(root/'alpha11-creative-validation.json').write_text(json.dumps(out,ensure_ascii=False,indent=2))
