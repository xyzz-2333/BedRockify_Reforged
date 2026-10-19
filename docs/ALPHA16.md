# alpha.16：经典方块与衰变药水

目标平台为 Java 1.20.1 / Forge。旧版反应器参照携带版 0.11 左右的规则；衰变药水参照现代基岩版。两者原本不处于同一游戏版本。

## 方块和获取

- 旧切石机是完整碰撞的装饰方块，可在创造栏或命令中取得；没有界面、加工用途或生存配方。
- 发光黑曜石发出 12 级光，硬度为 35；须用钻石或下界合金镐挖掘，掉落普通黑曜石。可在创造栏取得。
- 下界反应核以三颗钻石和六个铁锭合成，钻石放中列。挖掘返还这两种材料。

## 下界反应

只在主世界生存模式激活。以反应核为中心搭建 3×3×3：底层四角金块、其余圆石；中层四个边中点圆石、四角留空；顶层十字形五个圆石、四角留空。激活后，金块与圆石转为发光黑曜石，生成 17×17、35 格高的下界岩尖塔。反应持续 900 游戏刻，期间周期生成物品与僵尸猪灵；结束后反应核进入完成状态，发光黑曜石变为普通黑曜石。尖塔仅填充空气，不覆盖现有建筑。

尖塔的逐层外形、物品权重和生成频率目前是参照历史描述的拟合值；旧版二进制行为尚未校准。实现保留固定结构、45 秒时长和阶段状态，存档中的方块状态及计划方块刻延续反应。

## 衰变药水

注册 `bedrockify:decay` 药水类型：凋零 II，40 秒。饮用、喷溅、滞留和药水箭采用 Java 原版药水载体与作用规则；原版药水箭会将此效果缩短为 5 秒，滞留药水云的基础持续时间为 10 秒。四种形式在创造栏提供，也可用带 `Potion:"bedrockify:decay"` 的原版药水或箭物品命令获取。不添加没有依据的酿造配方。

## 材质来源

以下 PNG 原样取自 Minecraft Bedrock 1.21.94 安装资源包 `vanilla/textures/blocks`，未手绘或修改。源资源包保留了历史方块材质及反应核三个阶段。校验值为 SHA-256：

| 文件 | SHA-256 |
| --- | --- |
| glowing_obsidian.png | fd12af625299e85cbcd1338168fc6929cf6a00bd62730d28210c20cdafcbc1a0 |
| reactor_core_stage_0.png | 4622f07db6e1196c2214ee994980fafd566af17bbfb887b73160f4229d1a147f |
| reactor_core_stage_1.png | 350d0f91b7a221d0f46d7bdddeb9eb7e7f0569cba683e31a82337651057d3417 |
| reactor_core_stage_2.png | 6386b5dd77fc511ba8769af7c3f60cb00f26e6e17561e2b0952e6fbfb0496fa3 |
| stonecutter_bottom.png | dc527659388fd9b489c89ffd37060354fe0f3fb2984b3a65d9145a69c5e87720 |
| stonecutter_other_side.png | 9084a220d1682d24d8d3969dce7f0ab785feaca00c6af36ba64318d7e4237cbc |
| stonecutter_side.png | b60cc2941c8c83e608e2ce0f9a36c05577b5b5e4e3e8f2927b914e10e7ec0518 |
| stonecutter_top.png | 404f7d490dea472bd72bca9320c73f49c06cb4a22dfb9d99d981d8c6d265bfb1 |

历史机制参考：[Nether Reactor](https://minecraft.wiki/w/Nether_Reactor)、[Glowing Obsidian](https://minecraft.wiki/w/Glowing_Obsidian)。准确行为以原游戏实例后续测试为准。
