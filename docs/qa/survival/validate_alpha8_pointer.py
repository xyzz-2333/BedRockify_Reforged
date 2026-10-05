"""Targeted native mouse/hit tests for alpha.8; run in the disposable QA world."""
import json,time,uuid,sys
from pathlib import Path

root=Path(sys.argv[1]).resolve();records=[]
def send(channel,action,**fields):
    command={'action':action,**fields};result=root/(channel+'-qa-result.json')
    if channel=='survival':command['request_id']=uuid.uuid4().hex
    result.unlink(missing_ok=True);tmp=root/(channel+'-qa-command.tmp')
    tmp.write_text(json.dumps(command),encoding='utf-8');tmp.replace(root/(channel+'-qa-command.json'))
    deadline=time.monotonic()+40
    while time.monotonic()<deadline:
        try:value=json.loads(result.read_text(encoding='utf-8'))
        except (FileNotFoundError,json.JSONDecodeError):time.sleep(.05);continue
        if channel=='survival' and value.get('request_id')!=command['request_id']:time.sleep(.05);continue
        records.append({'channel':channel,'command':command,'result':value})
        assert value.get('ok'),value
        return value
    raise TimeoutError(command)
def s(action,**fields):return send('survival',action,**fields)
def c(action,**fields):return send('creative',action,**fields)
def until(predicate):
    deadline=time.monotonic()+10
    while time.monotonic()<deadline:
        state=s('status')
        if predicate(state):return state
        time.sleep(.1)
    raise AssertionError(state)
try:
    s('close');s('toggle_setting',enabled=True);s('setup')
    until(lambda v:v['hotbar'][0]=='64 oak_log');v=s('inventory')
    if not v['book_open']:v=s('user_toggle_book')
    v=s('search',text='minecraft:oak_planks')
    assert v['pages']==1 and v['page']==0
    recipe_x=v['recipe_left'];y=v['y']
    for x in [recipe_x+146,recipe_x+170]:
        v=s('mouse',x=x,y=y+195,button=0);assert v['page']==0 and v['pages']==1
    print('PASS single-page disabled navigation consumes clicks without changing page',flush=True)
    v=s('search',text='');assert v['pages']>1
    v=s('mouse',x=recipe_x+170,y=y+195,button=0);assert v['page']==1
    v=s('mouse',x=recipe_x+146,y=y+195,button=0);assert v['page']==0
    print('PASS enabled next/previous buttons page through actual recipes',flush=True)
    # Verify real relocated empty inventory cells through screen pointer events.
    v=s('status');src=v['slots'][36];dst=v['slots'][9]
    s('mouse',x=v['x']+src['x']+8,y=v['y']+src['y']+8,button=0)
    until(lambda v:v['cursor']=='64 oak_log')
    s('mouse',x=v['x']+dst['x']+8,y=v['y']+dst['y']+8,button=0)
    until(lambda v:v['slots'][9]['item']=='64 oak_log' and v['cursor']=='0 air')
    assert v['slots'][5]['x']-2+20==30 and v['slots'][8]['y']-2+20==96
    assert v['slots'][45]['x']-2==90 and v['slots'][45]['y']-2==76
    print('PASS empty survival inventory accepts held stacks; armor/offhand borders align',flush=True)
    s('close');s('command',text='gamemode creative');time.sleep(.3)
    s('command',text='clear');time.sleep(.3)
    v=c('open');v=c('tab',id='bedrockifyqa:tab_0')
    assert v['classic'] and v['disabled_picker_cells']>0
    cols=v['columns'];rows=v['rows'];x=v['x'];y=v['y']
    occupied=(x+22,y+38);blank=(x+22+6*20,y+38+2*20)
    assert c('probe',x=occupied[0],y=occupied[1])['hit_slot']==0
    assert c('probe',x=blank[0],y=blank[1])['hit_slot']==-1
    v=c('mouse',x=occupied[0],y=occupied[1],button=0);held=v['cursor'];assert held!='0 air'
    v=c('mouse',x=blank[0],y=blank[1],button=0);assert v['cursor']==held
    hotbar_x=x+(cols*20+32-180)//2+10;hotbar_y=y+52+rows*20
    assert c('probe',x=hotbar_x,y=hotbar_y)['hit_slot']==cols*rows
    v=c('mouse',x=hotbar_x,y=hotbar_y,button=0);assert v['cursor']=='0 air' and v['hotbar0']==held
    print('PASS empty catalogue cell is neither hit nor clickable; empty creative hotbar remains usable',flush=True)
    # Search rebuilds from populated to completely empty and back.
    v=c('search',text='zzzz_nonexistent_alpha7');assert v['items']==0
    assert c('probe',x=occupied[0],y=occupied[1])['hit_slot']==-1
    v=c('search',text='');assert v['items']>0
    assert c('probe',x=occupied[0],y=occupied[1])['hit_slot']==0
    print('PASS populated/empty search transitions refresh native slot availability',flush=True)
finally:
    (root/'alpha8-pointer-validation.json').write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf-8')
