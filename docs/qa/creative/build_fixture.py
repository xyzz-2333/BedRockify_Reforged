"""Build the isolated creative fixture after a Forge development build."""
import os,subprocess,zipfile
from pathlib import Path

project=Path(__file__).resolve().parents[3]
work=project/'run/creative-fixture';work.mkdir(parents=True,exist_ok=True)
mapped=next(p for p in (project/'.gradle/loom-cache/minecraftMaven').rglob('*.jar') if not p.name.endswith('-sources.jar'))
cache=Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/modules-2/files-2.1'
cp=os.pathsep.join(map(str,[mapped,project/'build/classes/java/main',*cache.rglob('*.jar')]))
r=subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/javac'),'-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(work/'classes'),str(Path(__file__).with_name('CreativeInventoryQa.java'))],capture_output=True,text=True)
print(r.stdout+r.stderr)
if r.returncode:raise SystemExit(r.returncode)
mods=project/'run/client/mods';mods.mkdir(parents=True,exist_ok=True)
out=mods/'bedrockify-creative-qa.jar';tmp=out.with_suffix('.jar.tmp')
with zipfile.ZipFile(tmp,'w',zipfile.ZIP_DEFLATED) as z:
    for p in (work/'classes').rglob('*.class'):z.write(p,p.relative_to(work/'classes').as_posix())
    z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[47,)"\nlicense="GPL-3.0"\n[[mods]]\nmodId="bedrockifyqa"\nversion="1"\ndisplayName="BedrockIfy Creative QA"\n')
    z.writestr('pack.mcmeta','{"pack":{"pack_format":15,"description":"Development-only creative fixture"}}')
    z.writestr('assets/bedrockifyqa/models/item/sample.json','{"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/diamond"}}')
tmp.replace(out)
print(out)
