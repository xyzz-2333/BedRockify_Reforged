# BedrockIfy Reforged — Forge 1.20.1

本仓库基于BedrockIfy，把部分基岩版特性带到 **Minecraft 1.20.1 Java**。

当前版本 ：**alpha.11**。

## 安装与配置

1. 安装 Minecraft **1.20.1** 与 **Forge 47.4.0**。
2. 将 `bedrockify-forge-1.9.1-forge-alpha.11+mc1.20.1.jar` 放入 `mods`。
3. 客户端还需安装依赖： **[Cloth Config 11.x 的 Forge 版本](https://modrinth.com/mod/cloth-config/version/11.1.136+forge)**，
4. 游戏中按 **B** 打开配置，也可以在 Forge 模组列表或游戏选项界面打开。B 可以在按键设置中更改。
5. 炼药锅、配方、骨粉和世界生成等共同逻辑需要服务端安装同版模组；界面类功能只在客户端生效。
运行时不需要 Fabric Loader、Fabric API 或 Architectury API。

配置位置沿用上游：
- `config/bedrockify/bedrockifyClient.json`：客户端功能。
- `config/bedrockify/bedrockifyCommon.json`：共同玩法功能。
- `config/bedrockify/bedrockifyMixins.json`：底层注入开关，修改后重启游戏。
- `config/bedrockify/inventoryUiState.json`：物品栏分类、展开组、搜索词与列表位置。

## 功能总览

### 1. 基岩经典物品栏

把 Java 版物品栏换成基岩经典布局，同时保留原版槽位、配方判定和网络行为。

**生存背包与工作台**

- 基岩经典风格的生存背包、2 × 2 合成与工作台 3 × 3 合成页面。原版盔甲、副手、玩家槽位、合成处理器和网络操作保持原有身份，关闭界面时恢复槽位位置。
- 附加槽位、非原版屏幕类或窗口不足 470 × 250 个 GUI 单位时使用原界面。
- 外观按参考图的像素结构绘制，贴图为 `assets/bedrockify/textures/gui/survival.png`，可由资源包覆盖背景、槽位和按钮外观。

**配方栏与状态记忆**

### 2. HUD 与手持物品（上游功能）

- 左上角坐标、帧率、纸娃娃与区块地图；坐标文字垂直位置、帧率显示位置、屏幕安全区偏移均可调。
- 快捷栏上方的手持物品名称与信息，含附魔、药水、容器（潜影盒/箱子）等提示内容，背景不透明度可调。
- 聊天框基岩样式、布局与安全区偏移；左侧加载画面（含区块地图）与保存状态提示。
- 快捷栏透明度、物品栏格高亮颜色、拖动物品放大、空手摆动速度、第三人称进食动画、快捷栏物品动画、立体钓鱼浮漂。
- 隐藏主菜单的 JAVA EDITION 副标题。
- 配置入口：B 键、Forge 模组列表、游戏选项菜单按钮，按钮位置可选；设置界面已完整汉化。

### 3. 视觉效果（上游功能）

- 方块表面明暗、太阳眩光与天空颜色衰减，强度可调（部分路径会被光影/阴影模组停用）。
- 环境颜色噪声；剪毛后保留绵羊颜色；经验等级文字样式；经典 HUD 选中框使用中性灰白色。

### 4. 世界与玩法（上游功能）

- 基岩版炼药锅：盛放染色水与药水，并用染色水给皮革装备染色。
- 基岩版合成与染色规则，默认对齐基岩 1.20.10 之后的船、木桶与蛛网规则；可切换“旧版基岩配方（怀旧）”。修改配方后需重新打开世界。
- 枯树与倒木世界生成、骨粉催生甘蔗与蕨、动物进食粒子、火焰附加点火。
- 基岩版世界加载画面与加载提示、世界选择/生成/加载使用 1.20.1 原版旋转全景（可在界面设置关闭，资源包仍可替换全景素材）。

### 5. 其他（上游功能）

- 前向搭桥辅助。
- 创造飞行渐进减速（默认 25%，0% 保留原版惯性）；按跳跃键结束鞘翅滑翔。

## 本地构建

需要完整 JDK 17，首次构建需要联网下载 Gradle、Minecraft、Forge 和依赖。

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

Windows 使用 `gradlew.bat build`、`gradlew.bat runClient`。

采用 Architectury Loom 的 Forge 模式，开发时保留 Yarn 名称以减少无关的源码重命名；发布包会重映射到 Forge 的运行时名称，并在打包前校验 Mixin 引用映射与访问规则。

## 兼容性

- 与主流模组均兼容（实际测试环境：Forge 47.4.0，158个模组）
- Embeddium / Rubidium 安装时自动停用部分旧光照注入；Oculus 安装时停用上游自定义光照注入。这是保守处理，不代表已通过完整整合包兼容性验证。
- 与其他创造栏或背包界面模组冲突时，可在 B 键 → 高级兼容设置中单独关闭对应注入并重启，界面会退回 Java 原版布局。

## 已知缺口

- Forge GUI 覆写的部分 HUD 路径尚未迁移到 Forge overlays：个别非零屏幕安全区偏移、经验条及血条样式可能不生效。默认坐标与快捷栏已测试。
- 未迁移的配套集成：AppleSkin 安全区 HUD 联动。
- 大型整合包、多人联机尚未系统验证；仓库目前没有单元测试（`test` 为 NO-SOURCE），验证依赖 `docs/qa` 下的开发夹具与实机操作记录。

## 文档索引

| 文档 | 内容 |
| --- | --- |
| [CHANGELOG.md](CHANGELOG.md) | 各发布版本简要记录 |
| [docs/PORTING.md](docs/PORTING.md) | 平台接口替换、alpha.1 验证记录、已知缺口 |
| [docs/ALPHA2.md](docs/ALPHA2.md) ～ [docs/ALPHA11.md](docs/ALPHA11.md) | 每轮改动说明与验证范围 |
| [docs/qa/README.md](docs/qa/README.md)、[docs/qa/survival/README.md](docs/qa/survival/README.md)、[docs/qa/creative/README.md](docs/qa/creative/README.md) | 开发夹具、命令与结果文件说明 |

## 来源与许可

原项目：https://github.com/juancarloscp52/BedrockIfy

上游标签：`1.9.1+mc1.20`  
上游提交：`907861f43f98622677ae5ed1e896ac7dd68b4aa3`

保留上游 GPL-3.0 与 Minecraft 链接例外；完整文本见 `LICENSE`，修改标记见 `NOTICE`。源码包不包含 Minecraft 本体。本仓库不是上游作者的官方发布。
