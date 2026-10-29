# alpha.16：经典方块与衰变药水

目标平台为 Java 1.20.1 / Forge。旧版反应器参照携带版 0.11 左右的规则；衰变药水参照现代基岩版。两者原本不处于同一游戏版本。

## 方块和获取

- 旧切石机是完整碰撞的装饰方块，可在创造栏或命令中取得；没有界面、加工用途或生存配方。
- 发光黑曜石发出 12 级光，硬度为 35；须用钻石或下界合金镐挖掘，掉落普通黑曜石。可在创造栏取得。
- 下界反应核以三颗钻石和六个铁锭合成，钻石放中列。挖掘返还这两种材料。

## 下界反应

在主世界以反应核为中心搭建 3×3×3：底层四角金块、其余圆石；中层四角圆石、四个边中点留空；顶层十字形五个圆石、四角留空。生存和创造模式均可点击激活，结构不正确时会显示提示。结构正确时生成下界岩尖塔；圆石在第 2～4 级、金块在第 7～9 级逐层转为发光黑曜石。第 43～45 级从上到下将核心周围的 3×3 层转为黑曜石，计数超过 920 游戏刻后反应核进入完成状态。完成后，附近有玩家时继续按附近最多三只的槽位生成僵尸猪灵；玩家离开时清除本反应核生成的僵尸猪灵。尖塔底层、房间与三段倾斜屋顶按 0.11 包体的构造参数生成；房间会清空原有方块，墙、地板和屋顶会覆盖原有方块。

点击反馈中的模式错误、激活、玩家距离及高度提示，取自携带版 0.11.0 APK 的 `en_US-pocket.lang` 和 `zh_CN-pocket.lang`。0.11.0 二进制原本禁止创造模式激活；本模组依照测试需求允许创造模式激活，并另加了已结束、维度和无建造权限提示。

下限以底层金块的 Y 坐标为准：世界最低层及最低层＋1 禁止激活，从最低层＋2 开始允许，不再使用固定的正 Y 下限。Java 主世界最低 Y 为 -64 时，金块最低可放在 -62，核心为 -61，尖塔两层地板从 -64 开始生成。尖塔生成可直接替换地板、墙和顶棚范围内的基岩，也可清空房间内的基岩；范围外的基岩保留。此下限与破基岩行为依据 2026-10-29 用户实机测试反馈校正。

计数周期、批次节点和批次数量已按携带版 0.11.0 二进制的 `NetherReactorTileEntity::tick`、`checkLevelChange`、`getNumItemsPerLevel` 和 `getNumEnemiesPerLevel` 调整。第 10、13、20、22、25、30、34、36、38、40 级（每级 20 刻）执行批量生成；计数超过 920 刻后结束。批次计数从 0 开始，首次批次先递增再计算数量，因此前三批各 15 件，随后四批为 `max(0, random % 42 - 4)`，最后三批为 `max(0, random % 27 - 2)`。前三批各尝试生成 2 只僵尸猪灵，第 4、5 批随机尝试 0 或 1 只。结构转化顺序亦按 `tickGlowingRedstoneTransformation` 与 `turnGlowingObsidianLayerToObsidian` 实现；刷怪槽位仍需旧版实机校准。

2026-10-28 使用 `minecraftWorlds(1).zip` 中同一世界的两个 0.11 实测结果校准尖塔。反应核中心为 `(5,5,20)`；中途拆核的 `测试2` 有 3003 块下界岩，正常结束的 `TDcGAG4jAgA=` 有 2495 块。后者的下界岩是前者的严格子集，两者直接相差 508 块。中断存档的东墙在相对 `(8,-1,4)`、`(8,0,4)` 处有两个连续缺口，结合原版构造函数按入口开口处理；补齐后完整尖塔有 3005 块，范围为相对 `x,z=-8..8`、`y=-3..30`。正常结束结果相对完整模型的 510 个缺口全部位于原版可破损范围，底部两层地板完整。

修复三段屋顶只沿 X 轴倾斜的公式：现在使用 `radius + height + (x + z) / 2`，中段使用 `radius + height + (-x - z) / 2`，整数除法向零截断；局部高度满足 `y < 2 * radius + height`。生成和结束破损共用该高度公式。正常结束时下层墙随机移除约三分之一、屋顶周边随机移除约四分之一；内侧斜面保留。中途拆核直接停止后续运行，完成状态不重复破损。破损概率来自 0.11 原生函数，不由单个存档的缺口比例反推，也不要求 Java 与原版生成同一组随机缺口。

存档逐层掩码保存在 `docs/tools/alpha16-tests/resources/data/bedrockify/reactor/mcpe011-spire.json`，包含原始 ZIP 的 SHA-256 和两份结果。可用 `python docs/tools/extract_reactor_011_fixture.py "minecraftWorlds(1).zip" --check-fixture docs/tools/alpha16-tests/resources/data/bedrockify/reactor/mcpe011-spire.json` 只读重建并核对。`gradlew --gradle-user-home .gradle-user runServer -Palpha16GameTests --offline` 执行七项 GameTest；测试逐块比对塔形、检查正常结束只破损一次，并在拆核及拆核后放置新核心的情况下保留待处理计时，等待 960 刻确认塔体不变；另验证金块位于最低两层时拒绝激活且不改动环境、最低层＋2 时激活并替换两层基岩地板、清空房间基岩且保留范围外的基岩。不调整掉落、刷怪或其他激活条件。

测试启动前清理各自场景及附近旧核心，避免 GameTest 保留的世界中，先前运行的尖塔和计时影响逐块比对；快照失败时报告实际变化的位置及前后方块状态。

已从旧版二进制的普通掉落函数解析出八个等概率分支：萤石粉×3、西瓜种子、棕色蘑菇、红色蘑菇、甘蔗、仙人掌、下界石英×4、南瓜种子。普通掉落种类和单次数量已按此表更新。`spawnItems` 对每件物品调用 `spawnItem`，后者直接调用普通掉落函数；另有低概率物品函数，但不在这条路径上。物品与刷怪位置采用原版的水平半径 3～7、固定垂直偏移 -1 范围。

## 衰变药水

注册 `bedrockify:decay` 药水类型：凋零 II，40 秒。饮用、喷溅、滞留和药水箭采用 Java 原版药水载体与作用规则；原版药水箭会将此效果缩短为 5 秒，滞留药水云的基础持续时间为 10 秒。四种形式在创造栏提供，也可用带 `Potion:"bedrockify:decay"` 的原版药水或箭物品命令获取。不添加没有依据的酿造配方。

## 材质来源

旧切石机和发光黑曜石 PNG 原样取自 Minecraft Bedrock 1.21.94 安装资源包 `vanilla/textures/blocks`。已与携带版 0.11.0 APK 的图集逐像素比对，内容相同。反应核三个阶段则从携带版 0.11.0 的 `assets/images/terrain-atlas.tga` 按 `terrain.meta` 坐标提取为 16×16 PNG；其像素与 1.21.94 保留的历史材质不同。均未手绘。校验值为 SHA-256：

| 文件 | SHA-256 |
| --- | --- |
| glowing_obsidian.png | fd12af625299e85cbcd1338168fc6929cf6a00bd62730d28210c20cdafcbc1a0 |
| reactor_core_stage_0.png | f285962a7ae9443733f5e003fa45b76a575d65ee1c8d2cb4fd379564bb59295d |
| reactor_core_stage_1.png | f2f4334d72b82092c52692e8c5fef2822a4d616247b0eb6f4c9e9b1a89e796e2 |
| reactor_core_stage_2.png | f551054a98b9dca0c02a609115d41c537544966532ce4eaddf0bb35ce683a44c |
| stonecutter_bottom.png | dc527659388fd9b489c89ffd37060354fe0f3fb2984b3a65d9145a69c5e87720 |
| stonecutter_other_side.png | 9084a220d1682d24d8d3969dce7f0ab785feaca00c6af36ba64318d7e4237cbc |
| stonecutter_side.png | b60cc2941c8c83e608e2ce0f9a36c05577b5b5e4e3e8f2927b914e10e7ec0518 |
| stonecutter_top.png | 404f7d490dea472bd72bca9320c73f49c06cb4a22dfb9d99d981d8c6d265bfb1 |

历史机制参考：[Nether Reactor](https://minecraft.wiki/w/Nether_Reactor)、[Glowing Obsidian](https://minecraft.wiki/w/Glowing_Obsidian)。准确行为以原游戏实例后续测试为准。
