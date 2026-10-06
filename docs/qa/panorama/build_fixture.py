"""Compile an isolated native-screen QA mod; no game resources are bundled."""
from pathlib import Path
import json,os,subprocess,zipfile
p=Path(__file__).resolve().parents[3];work=p/'run/panorama-fixture';work.mkdir(parents=True,exist_ok=True)
mapped=next(j for j in (p/'.gradle/loom-cache/minecraftMaven').rglob('*.jar') if not j.name.endswith('-sources.jar'))
jars=[mapped,p/'build/classes/java/main']+[j for j in (Path(os.environ['GRADLE_USER_HOME'])/'caches/modules-2/files-2.1').rglob('*.jar') if not j.name.endswith(('-sources.jar','-installer.jar')) and zipfile.is_zipfile(j)]
r=subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/javac'),'-proc:none','-encoding','UTF-8','-cp',os.pathsep.join(map(str,jars)),'-d',str(work/'classes'),str(Path(__file__).with_name('MenuPanoramaQa.java'))],capture_output=True,text=True)
if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
out=p/'run/client/mods/bedrockify-panorama-qa.jar'
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for f in (work/'classes').rglob('*.class'):z.write(f,f.relative_to(work/'classes').as_posix())
 z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[47,)"\nlicense="GPL-3.0"\n[[mods]]\nmodId="bedrockifypanoramaqa"\nversion="1"\ndisplayName="Panorama QA"\n')
 z.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':15,'description':'Development-only native panorama QA'}}))
print('Native-screen fixture compiled')
