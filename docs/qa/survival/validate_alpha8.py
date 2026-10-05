"""Alpha.8 native search events, layout bounds and actual JEI runtime registrations."""
import json,time,uuid,sys
from pathlib import Path
root=Path(sys.argv[1]).resolve();records=[];passed=[]
def send(action,**fields):
    cmd={'action':action,'request_id':uuid.uuid4().hex,**fields}
    result=root/'survival-qa-result.json';result.unlink(missing_ok=True)
    tmp=root/'survival-qa-command.tmp';tmp.write_text(json.dumps(cmd),encoding='utf-8');tmp.replace(root/'survival-qa-command.json')
    deadline=time.monotonic()+60
    while time.monotonic()<deadline:
        try:v=json.loads(result.read_text())
        except (FileNotFoundError,json.JSONDecodeError):time.sleep(.05);continue
        if v.get('request_id')!=cmd['request_id']:time.sleep(.05);continue
        records.append({'command':cmd,'result':v});assert v.get('ok'),v;return v
    raise TimeoutError(cmd)
def until(predicate):
    deadline=time.monotonic()+15
    while time.monotonic()<deadline:
        v=send('status')
        if predicate(v):return v
        time.sleep(.1)
    raise AssertionError(v)
def clear(v):
    for ch in v['search_text']:v=send('key',code=259)
    assert v['search_text']=='';return v
try:
    send('close');send('toggle_setting',enabled=True);send('setup')
    until(lambda v:v['hotbar'][0]=='64 oak_log')
    for kind in ['inventory','crafting']:
        send(kind);v=until(lambda v:kind.title()+'Screen' in v['screen'])
        if not v['book_open']:send('user_toggle_book');v=send('status')
        assert v['classic'] and (v['background_width'],v['background_height'])==(202,210)
        assert v['x']==v['recipe_left']+190
        assert v['slots'][36 if kind=='inventory' else 37]['x']==12
        assert v['slots'][44 if kind=='inventory' else 45]['x']+18==190
        if kind=='inventory':
            assert v['slots'][5]['x']-2+20==30 and v['slots'][8]['y']-2+20==96
            assert (v['slots'][45]['x'],v['slots'][45]['y'])==(92,78)
        else:
            assert v['slots'][1]['y']==24 and v['slots'][10]['y']==110
        initial=v['filtered_count'];passes=v['matching_passes'];b=v['search_bounds']
        v=send('mouse',x=b['x']+8,y=b['y']+6,button=0)
        assert v['search_focused'] and not v['searching']
        for ch in 'oak_planks':
            send('key',code=ord(ch.upper()) if ch!='_' else 45);v=send('char',text=ch)
        assert v['search_text']=='oak_planks' and 0<v['filtered_count']<initial
        assert v['matching_passes']==passes
        v=send('key',code=69);assert v['search_focused'] and kind.title()+'Screen' in v['screen']
        v=clear(v)
        # IME commits can arrive directly as charTyped, without preceding keyPressed.
        v=send('char',text='木板')
        assert v['search_text']=='木板' and 0<v['filtered_count']<initial and v['matching_passes']==passes
        v=clear(v)
        v=send('char',text='nonexistent_alpha_eight');assert v['filtered_count']==0
        v=clear(v);assert v['filtered_count']==initial
        v=send('mouse',x=v['x']+12,y=v['y']+98,button=0);assert not v['search_focused']
        v=send('char',text='a');assert v['search_text']==''
        passed.append(kind+': keyboard, direct IME chars, blur and cached filtering')
        for slot in v['slots'][1:5 if kind=='inventory' else 10]:
            p=send('jei_probe',x=slot['x']+8,y=slot['y']+8);assert p['jei_click_areas']==[]
        old=(140,35) if kind=='inventory' else (100,40)
        arrow=(163,58) if kind=='inventory' else (138,52)
        p=send('jei_probe',x=old[0],y=old[1]);assert not p['jei_click_areas']
        p=send('jei_probe',x=arrow[0],y=arrow[1]);assert len(p['jei_click_areas'])==1
        expected={'x':156,'y':50,'width':14,'height':16} if kind=='inventory' else {'x':125,'y':43,'width':26,'height':18}
        assert p['jei_click_areas'][0]==expected
        p=send('jei_capture',x=arrow[0],y=arrow[1]);assert p['jei_capture_preserved']
        send('user_toggle_book');p=send('jei_probe',x=arrow[0],y=arrow[1]);assert len(p['jei_click_areas'])==1
        send('user_toggle_book');send('disable_redisplay')
        p=send('jei_probe',x=old[0],y=old[1]);assert len(p['jei_click_areas'])==1 and p['background_width']==176
        p=send('jei_probe',x=arrow[0],y=arrow[1]);assert not p['jei_click_areas']
        send('toggle_setting',enabled=True);send('redisplay')
        passed.append(kind+': native JEI registration, input grid, capture and vanilla fallback')
        print('PASS',kind,flush=True)
    send('close');send('inventory')
    print('PASS',len(records),'commands;',len(passed),'cases',flush=True)
finally:
    (root/'alpha8-input-layout-validation.json').write_text(json.dumps({'passed':passed,'records':records},ensure_ascii=False,indent=2),encoding='utf-8')
