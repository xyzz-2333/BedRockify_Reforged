# 投射物参数分析

基岩基准：用户提供的安装包，Appx 身份版本 `1.21.2301.0`。按用户要求假定旧投射物近期行为未变。Java 基准：本项目实际使用的 Minecraft 1.20.1 / Forge 47.4.0 映射字节码。

## 已确认参数

| 类型 | Java | 基岩行为包显式值 | 本次处理 |
| --- | --- | --- | --- |
| 末影珍珠 | 初速度 1.5、重力 0.03、空气保留比例 0.99、水中 0.8 | power 1.5、gravity 0.025、inertia 1、liquid_inertia 1 | 调整重力与两种阻力 |
| 玩家投掷三叉戟 | 初速度 2.5、重力约 0.05、水中保留比例 0.99 | power 4、gravity 0.1、liquid_inertia 0.99 | 初速度改为 4、重力改为 0.1；水中参数相同 |
| 雪球、鸡蛋 | 初速度 1.5、重力 0.03 | power 1.5、gravity 0.03；阻力省略 | 保留原版，未发现已证实的调整依据 |
| 箭 | 玩家满蓄力弓初速度与实体基础参数不同 | 实体 power 1.6、gravity 0.05 | 保留原版；未确认基岩弓发射端逻辑 |

基岩文件位于 `data/behavior_packs/vanilla/entities`，分别为 `ender_pearl.json`、`thrown_trident.json`、`snowball.json`、`egg.json`、`arrow.json`。兼容包 `vanilla_1.16.100` 与 `vanilla_1.17.20` 中对应显式参数相同；兼容数据不是旧引擎源码。

Java 字节码已核对 `ThrownEntity.tick/getGravity`、`PersistentProjectileEntity.tick`、`TridentEntity.getDragInWater` 与 `TridentItem.onStoppedUsing`。只修改指定种类，药水、经验瓶以及模组投射物保留自己的重力实现。

## 实现边界

- 沿用 Java 逐 tick 移动、阻力、重力与碰撞顺序；没有声称还原基岩闭源引擎内部顺序。
- 基岩三叉戟 `power=4` 作为本次效果近似的发射速度。尚未通过逆向证明玩家发射端不进一步调整它。
- 三叉戟初速度仅调整玩家投掷；溺尸、发射器和其他模组发射逻辑不改。重力对原版三叉戟实体统一调整。
- 服务端同步投射物规则到客户端，规则协议升为 2，客户端与服务端需使用本次匹配版本。
- `docs/tools/compare_projectile_trajectories.py` 在相同角度与高度下计算选定参数的轨迹，供检查改动方向；结果不是基岩实测射程。
- 安装程序中未发现 `.pdb` 或反编译源码；本次未完成 `Minecraft.Windows.exe` 的函数逆向。
