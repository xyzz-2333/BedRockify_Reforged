"""Extract static rotations from a supplied vanilla Bedrock animation file."""
from pathlib import Path
import argparse,hashlib,json,re
ROOT=Path(__file__).resolve().parents[2]
ORDER=['default','no','solemn','athena','brandish','honor','entertain','salute','riposte','zombie','cancan_a','cancan_b','hero']
BONES=['head','body','leftarm','rightarm','leftleg','rightleg']
def constant(value):
    if isinstance(value,(float,int)):return float(value)
    match=re.fullmatch(r'-this(?:\s*([+-])\s*(\d+(?:\.\d+)?))?',value.strip())
    if not match:raise ValueError(f'Unsupported rotation: {value}')
    return (float(match[2]) if match[1]=='+' else -float(match[2])) if match[1] else 0.0
def main():
    parser=argparse.ArgumentParser();parser.add_argument('animation_file',type=Path);args=parser.parse_args()
    raw=args.animation_file.read_bytes();animations=json.loads(raw)['animations']
    poses=[[ [constant(v) for v in animations[f'animation.armor_stand.{name}_pose']['bones'][bone]['rotation']] for bone in BONES] for name in ORDER]
    p=ROOT/'src/main/java/dev/bedrockify/forge/common/features/armorStand/ArmorStandPoses.java';p.parent.mkdir(parents=True,exist_ok=True)
    names=', '.join('"'+n+'"' for n in ORDER)
    rows=[]
    for pose in poses:rows.append('        {'+', '.join('{'+', '.join(f'{v:g}F' for v in bone)+'}' for bone in pose)+'}')
    code='''package dev.bedrockify.forge.common.features.armorStand;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.EulerAngle;

/** Generated static rotations from the supplied Bedrock vanilla animation file. */
public final class ArmorStandPoses {
    public static final String[] NAMES = {NAMES};
    private static final float[][][] ROTATIONS = {
ROWS
    };
    private ArmorStandPoses() {}
    public static int normalize(int pose) { return Math.floorMod(pose, NAMES.length); }
    public static int fromPower(int power) { return Math.min(12, Math.max(0, power)); }
    public static void apply(ArmorStandEntity stand, int pose) {
        float[][] p=ROTATIONS[normalize(pose)];
        stand.setHeadRotation(angle(p[0])); stand.setBodyRotation(angle(p[1]));
        stand.setLeftArmRotation(angle(p[2])); stand.setRightArmRotation(angle(p[3]));
        stand.setLeftLegRotation(angle(p[4])); stand.setRightLegRotation(angle(p[5]));
    }
    private static EulerAngle angle(float[] r) { return new EulerAngle(r[0],r[1],r[2]); }
}
'''.replace('{NAMES}','{'+names+'}').replace('ROWS',',\n'.join(rows))
    p.write_bytes(code.encode())
    manifest={'source':'resource_packs/vanilla/animations/armor_stand.animation.json','sha256':hashlib.sha256(raw).hexdigest(),'pose_order':ORDER,'bones':BONES,'rotations':poses,'conversion':'Static -this cancels the prior rotation; retain the constant term. Same degree axes as Java armor stand rotations.'}
    (ROOT/'docs/armor_stand_poses.json').write_bytes((json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode())
    print('Imported 13 poses, 6 bones each; no wiggle animation or local paths included.')
if __name__=='__main__':main()
