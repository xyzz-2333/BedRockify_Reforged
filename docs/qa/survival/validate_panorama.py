"""Verify real vanilla screens, rotation, controls and loading in a QA world."""
import json,sys,time,uuid
from pathlib import Path
root=Path(sys.argv[1]).resolve();records=[];passed=[]
def send(action,**fields):
 cmd={'action':action,'request_id':uuid.uuid4().hex,**fields};result=root/'panorama-qa-result.json';result.unlink(missing_ok=True)
 tmp=root/'panorama-qa-command.tmp';tmp.write_text(json.dumps(cmd,ensure_ascii=False));tmp.replace(root/'panorama-qa-command.json')
 end=time.monotonic()+60
 while time.monotonic()<end:
  try:r=json.loads(result.read_text())
  except (FileNotFoundError,json.JSONDecodeError):time.sleep(.05);continue
  if r.get('request_id')==cmd['request_id']:records.append({'command':cmd,'result':r});assert r.get('ok'),r;return r
  time.sleep(.05)
 raise TimeoutError(action)
def wait(test):
 end=time.monotonic()+30
 while time.monotonic()<end:
  r=send('status')
  if test(r):return r
  time.sleep(.1)
 raise AssertionError(r)
try:
 wait(lambda r:r['screen']=='TitleScreen');assert send('status')['enabled']
 send('select_world');v=wait(lambda r:r.get('world_count',0)>0 and not r['dirt'] and not r['shadows'])
 assert any(w['message']=='Play Selected World' for w in v['widgets']);a=v['rotation'];time.sleep(.4);assert send('status')['rotation']!=a
 passed.append('world menu uses a rotating panorama while native list controls remain')
 send('select_first');v=send('status');assert v['selected'] and next(w for w in v['widgets'] if w['message']=='Play Selected World')['active']
 search=next(w for w in v['widgets'] if w['class']=='TextFieldWidget');send('mouse',x=search['x']+5,y=search['y']+5);send('char',text='zz-no-such-world');v=send('status');assert v['search']=='zz-no-such-world'
 send('key',code=256);wait(lambda r:r['screen']=='TitleScreen')
 passed.append('world selection, search and Escape retain native behavior')
 send('select_world');send('toggle',enabled=False);wait(lambda r:r.get('dirt') is True and r.get('shadows') is True)
 send('toggle',enabled=True);wait(lambda r:r.get('dirt') is False and r.get('shadows') is False)
 passed.append('the same world list restores its original background when disabled')
 send('resize',width=500,height=300);v=wait(lambda r:r.get('width')==500 and not r['dirt']);assert v['height']==300
 send('select_world');v=wait(lambda r:not r['dirt']);assert v['width']==640 and v['height']==360
 passed.append('panorama and native list adapt to screen resizing')
 send('speed',value=0);time.sleep(.2);a=send('status')['rotation'];time.sleep(.3);assert send('status')['rotation']==a;send('speed',value=1)
 passed.append('rotation honors the vanilla panorama-speed option')
 for action,screen in [('loading_probe','LevelLoadingScreen'),('terrain_probe','DownloadingTerrainScreen'),('progress_probe','ProgressScreen'),('message_probe','MessageScreen')]:
  send(action);v=wait(lambda r:r['screen']==screen and r['frames'].get(screen,0)>1);a=v['rotation'];time.sleep(.2);assert send('status')['rotation']!=a
 passed.append('generation, terrain download, progress and preparation screens rotate')
 send('widget_mode',enabled=False);send('loading_probe');time.sleep(.2);a=send('status')['rotation'];time.sleep(.2);assert send('status')['rotation']!=a
 send('widget_mode',enabled=True);send('toggle',enabled=False);time.sleep(.2);a=send('status')['rotation'];time.sleep(.2);assert send('status')['rotation']==a;send('toggle',enabled=True)
 passed.append('panorama is independent of loading-widget mode and can be disabled')
 send('options_probe');time.sleep(.2);a=send('status')['rotation'];time.sleep(.2);assert send('status')['rotation']==a
 passed.append('unrelated options screens keep their original background')
 send('select_world');wait(lambda r:r.get('world_count',0)>0 and not r['dirt']);send('load_first');v=wait(lambda r:r['world_ready'])
 assert v['frames'].get('LevelLoadingScreen',0)>0 and v['frames'].get('MessageScreen',0)>0
 passed.append('native world entry succeeds with loading background hooks enabled')
 out={'ok':True,'passed':passed,'operations':len(records),'records':records};print(json.dumps({k:v for k,v in out.items() if k!='records'},indent=2))
except Exception as e:
 out={'ok':False,'passed':passed,'error':repr(e),'operations':len(records),'records':records};print(json.dumps({k:v for k,v in out.items() if k!='records'},indent=2));raise
finally:(root/'alpha10-panorama-validation.json').write_text(json.dumps(out,ensure_ascii=False,indent=2))
