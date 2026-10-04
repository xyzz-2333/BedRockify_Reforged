"""Build an isolated Forge fixture: 100 woods, 100 metals, plus optional recipe pressure."""
import json, os, subprocess, zipfile
from pathlib import Path

project = Path(__file__).resolve().parents[3]
work = project / 'run/survival-fixture'
work.mkdir(parents=True, exist_ok=True)
mapped = next(p for p in (project / '.gradle/loom-cache/minecraftMaven').rglob('*.jar') if not p.name.endswith('-sources.jar'))
cache = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home()/'.gradle'))) / 'caches/modules-2/files-2.1'
jars = [mapped, project/'build/classes/java/main'] + list(cache.rglob('*.jar'))
cp = os.pathsep.join(map(str,jars))
(work/'classpath.txt').write_text(cp)
compiled=subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/javac'),'-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(work/'classes'),str(Path(__file__).with_name('SurvivalInventoryQa.java'))],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
print(compiled.stdout)
if compiled.returncode:raise SystemExit(compiled.returncode)
stress = int(os.environ.get('SURVIVAL_STRESS_RECIPES', '10000'))
mods = project/'run/client/mods'; mods.mkdir(parents=True,exist_ok=True)
out = mods/'bedrockify-survival-qa.jar'
staged = mods/'bedrockify-survival-qa.jar.tmp'
with zipfile.ZipFile(staged,'w',zipfile.ZIP_DEFLATED) as z:
    def data(path,value): z.writestr(path,json.dumps(value,separators=(',',':'),ensure_ascii=False))
    for p in (work/'classes').rglob('*.class'): z.write(p,p.relative_to(work/'classes').as_posix())
    z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[47,)"\nlicense="GPL-3.0"\n[[mods]]\nmodId="bedrockifysurvivalqa"\nversion="1"\ndisplayName="BedrockIfy Survival QA"\n')
    data('pack.mcmeta',{'pack':{'pack_format':15,'description':'Development-only survival recipe fixture'}})
    lang={}
    tags={'minecraft:logs':[],'minecraft:planks':[],'minecraft:wooden_stairs':[],'minecraft:wooden_slabs':[],
          'minecraft:wooden_doors':[],'minecraft:wooden_fences':[],'forge:ingots':[],'forge:nuggets':[],'forge:storage_blocks':[]}
    def recipe(name,inputs,output,count=1,nbt=None):
        result={'item':'bedrockifysurvivalqa:'+output,'count':count}
        if nbt is not None:result['nbt']=nbt
        data('data/bedrockifysurvivalqa/recipes/'+name+'.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':v} for v in inputs],'result':result})
    for i in range(100):
        for t,tag in [('log','minecraft:logs'),('planks','minecraft:planks'),('stairs','minecraft:wooden_stairs'),('slab','minecraft:wooden_slabs'),('door','minecraft:wooden_doors'),('fence','minecraft:wooden_fences'),('ingot','forge:ingots'),('nugget','forge:nuggets'),('block','forge:storage_blocks')]:
            name=('wood_' if t in ['log','planks','stairs','slab','door','fence'] else 'metal_')+str(i)+'_'+t
            tags[tag].append('bedrockifysurvivalqa:'+name)
            lang['item.bedrockifysurvivalqa.'+name]='QA '+('木材' if name.startswith('wood') else '金属')+str(i)+' '+{'log':'原木','planks':'木板','stairs':'楼梯','slab':'台阶','door':'门','fence':'栅栏','ingot':'锭','nugget':'粒','block':'块'}[t]
            data('assets/bedrockifysurvivalqa/models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:block/oak_planks' if name.startswith('wood') else 'minecraft:item/iron_ingot'}})
        recipe('wood_'+str(i)+'_planks',['bedrockifysurvivalqa:wood_'+str(i)+'_log'],'wood_'+str(i)+'_planks',4)
        for t,count in [('stairs',4),('slab',6),('door',3),('fence',3)]:
            recipe('wood_'+str(i)+'_'+t,['bedrockifysurvivalqa:wood_'+str(i)+'_planks']*3,'wood_'+str(i)+'_'+t,count)
        recipe('metal_'+str(i)+'_nuggets',['bedrockifysurvivalqa:metal_'+str(i)+'_ingot'],'metal_'+str(i)+'_nugget',9)
        recipe('metal_'+str(i)+'_ingot',['bedrockifysurvivalqa:metal_'+str(i)+'_nugget']*9,'metal_'+str(i)+'_ingot')
        recipe('metal_'+str(i)+'_block',['bedrockifysurvivalqa:metal_'+str(i)+'_ingot']*9,'metal_'+str(i)+'_block')
        recipe('metal_'+str(i)+'_from_block',['bedrockifysurvivalqa:metal_'+str(i)+'_block'],'metal_'+str(i)+'_ingot',9)
    for i in range(stress): recipe('stress_'+str(i),['minecraft:diamond'],'metal_0_nugget',1,{'qa_variant':i})
    for key,items in tags.items():
        ns,path=key.split(':');data('data/'+ns+'/tags/items/'+path+'.json',{'replace':False,'values':items})
    for language in ['zh_cn','en_us']:data('assets/bedrockifysurvivalqa/lang/'+language+'.json',lang)
    # Optional Curios fixture: exercise a real extended inventory, not just
    # the presence of its API. These resources are ignored without Curios.
    data('data/bedrockifysurvivalqa/curios/slots/ring.json', {'size': 2, 'add_cosmetic': True})
    data('data/bedrockifysurvivalqa/curios/entities/player.json', {'entities': ['minecraft:player'], 'slots': ['ring']})
    data('data/curios/tags/items/ring.json', {'replace': False, 'values': ['minecraft:diamond']})
staged.replace(out)
print(json.dumps({'fixture':str(out),'registered_items':900,'fixture_recipes':900+stress,'stress_nbt_variants':stress}))
