# alpha.11：菜单背景与创造搜索修正

目标仍为 Minecraft 1.20.1 / Forge 47.4.0 / Java 17。

## 修改

- 源码包名与 Gradle 分组改为 `dev.bedrockify.forge`，清理模组列表中的冗余源作者展示。保留 `LICENSE`、`NOTICE` 和来源记录；mod ID、资源 ID、NBT、配置文件路径及高级功能键不变。
- 原版选项菜单及设置子页面复用原版旋转全景，语言页补充独立背景绘制；列表关闭自身泥土背景，关闭开关后恢复原值。沿用“菜单全景背景”开关、原版旋转速度和资源包全景素材。
- 经典创造搜索框只绘制一次，文字在面板和物品之后独立绘制并裁剪。去掉该输入框的文字阴影，维持原版的返回宽度，保留光标、选区和中间插入的布局。其他输入框及原版回退界面仍使用原生绘制。

## 水色与投射物核查

Java 1.20.1 本来就有群系水色差异。现有 `worldColorNoise` 在原版草色与水色上叠加小幅明暗噪声，没有增加完整基岩版群系水色表。

[官方箭实体示例](https://learn.microsoft.com/en-us/minecraft/creator/reference/source/vanillabehaviorpack_snippets/entities/arrow?view=minecraft-bedrock-stable) 中玩家箭的 `power` 是 5.0，`gravity` 是 0.05。Java 1.20.1 原生弓满蓄力的速度参数为 3.0，箭重力同为 0.05。该示例最后更新于 2023-09-07，不能单独证明参考版本弓的完整发射逻辑和最终速度，因此本轮没有修改弓箭。

[官方雪球示例](https://learn.microsoft.com/en-us/minecraft/creator/reference/source/vanillabehaviorpack_snippets/entities/snowball?view=minecraft-bedrock-stable) 给出 `power=1.5`、`gravity=0.03`，与本地核对的 Java 原生参数一致。雪球及其他投掷物没有改动。

## 验证

干净构建、生产重映射、Mixin refmap 与访问拓宽校验通过。原生 Forge 开发客户端在 JEI 15.56.0.205、Curios 5.14.1 和独立测试夹具下通过 23 组、267 次操作：菜单与背景恢复、世界加载、创造搜索与特殊分页、界面状态记忆。夹具包含 12 个模组创造标签、900 个物品及 10900 个合成配方；客户端最大堆为 1 GiB，正常保存世界并退出。

额外使用实际键盘检查 Ctrl+A 全选、删除与搜索结果恢复，并检查修复后文字和蓝色选区截图。结果见 `docs/qa/survival/results/alpha11-*`；最终截图为 `alpha11-search-fixed.png`、`alpha11-search-selection.png`、`alpha11-options-panorama.png`。