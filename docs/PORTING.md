# Forge 移植记录

日期：2026-10-03。基于固定上游提交 907861f43f98622677ae5ed1e896ac7dd68b4aa3。

## 平台替换

| 上游接口 | Forge 实现 |
|---|---|
| ModInitializer / ClientModInitializer | @Mod + FMLClientSetupEvent + DistExecutor |
| Fabric 注册入口 | RegisterEvent，在相应注册阶段登记方块、方块实体、Feature 和树装饰器 |
| ClientTickEvents | TickEvent.ClientTickEvent（END） |
| HudRenderCallback | RenderGuiEvent.Post |
| KeyBindingHelper | RegisterKeyMappingsEvent |
| EntityModelLayerRegistry | EntityRenderersEvent.RegisterLayerDefinitions |
| ColorProviderRegistry | RegisterColorHandlersEvent.Block |
| ResourceManagerHelper | RegisterClientReloadListenersEvent + SynchronousResourceReloader |
| Mod Menu 配置入口 | Forge ConfigScreenHandler + 原来的 B 快捷键 |
| Fabric 自定义粒子网络消息 | ServerWorld.spawnParticles，使用原版粒子包 |
| Fabric BiomeModifications | 带功能开关的 Forge BiomeModifier + 九份数据文件 |
| FabricLoader.isModLoaded | LoadingModList，供早期 Mixin 插件使用 |
| Access Widener | 构建时转换为 Forge Access Transformer |

同时修正配置目录只创建一层、空配置与无效 JSON 导致设置对象为空、未识别 Mixin 功能键触发拆箱异常等问题。

## alpha.1 验证记录

测试环境：Minecraft 1.20.1、Forge 47.4.0、Cloth Config Forge 11.1.136、JDK 17，Loom 开发运行环境。客户端以软件 OpenGL 渲染，测试世界为新建的 `ForgePortSmoke`，随后重新打开。

| 项目 | 实际结果 |
|---|---|
| `./gradlew build` | 通过，生成可安装的重映射 JAR 与源码 JAR；没有单元测试，`test` 为 NO-SOURCE |
| 发布包结构 | 53 个 Mixin 的类文件齐全，52 个引用映射条目，165 份 JSON 可解析，9 份 Forge biome modifier；包含有效 mods.toml、MixinConfigs 和 Access Transformer |
| Forge 客户端 | 启动、进入世界、重新打开世界、退出并保存全部维度通过 |
| 配置界面 | B 键打开成功；修改加载画面开关并保存，客户端 JSON 中 `loadingScreen` 为 false |
| 默认 HUD 与物品提示 | 坐标、热栏透明度、手持骨粉与药水名称正常显示 |
| 聊天 | 普通聊天、命令与方块实体数据输出正常，无原先的空设置对象或合成参数类错误 |
| 骨粉催生甘蔗 | 对一格甘蔗右键，增长到三格；服务端方块条件检查输出 `BONEMEAL_PASS` |
| 药水炼药锅 | 治疗药水右键倒入空锅后成为 `bedrockify:potion_cauldron`；NBT 保留 `minecraft:healing`、药水类型和颜色，输出 `POTION_CAULDRON_PASS` |
| 染色炼药锅 | 红染料右键满水炼药锅后成为 `bedrockify:colored_water_cauldron`，同步红色并记录 tint_color，输出 `DYED_CAULDRON_PASS` |
| 独立服务端 | SERVER 环境加载公共 Mixin 后到达 EULA 提示并正常结束；未继续启动世界，不能视为完整专用服测试 |

静态包检查不证明所有注入和玩法正确。发布 JAR 尚未在普通启动器安装的 Forge 实例中复测；当前游戏检查使用 Loom 的 Forge 开发环境。

首轮日志节选和截图在 `docs/evidence/alpha1/`。测试过程中首次染色方块查询误用了原版 `level` 属性，命令被拒绝；后续使用实际方块 ID 的条件查询通过。上游自定义炼药锅属性名为 `c_level`。

此环境无法连接 Mojang 认证与 Realms 服务，因此日志含认证密钥、离线开发账户皮肤和 Realms 的网络错误；联机认证没有验证。构建工具的 Unix socket 能力探测在本环境被禁止，测试时仅在本机缓存中让该探测失败返回 false，没有将修改后的构建工具放入源码包或游戏 JAR。

## alpha.2 验证记录

本轮已验证非潜行搭桥、创造飞行减速、现代／怀旧／关闭三种配方模式、中文配置与设置保存。修正了 30 份羊毛和床配方的覆盖目标，并增加只作用于本模组配置页的搜索框汉化注入。当前为 54 个 Mixin，仍为 52 个引用映射条目。详细记录与边界见 `ALPHA2.md`，证据在 `evidence/alpha2/`。

## 已知缺口

- ForgeGui 覆写的部分 HUD 路径尚未迁移到 Forge overlays：部分非零屏幕安全区偏移、经验条及血条样式可能不生效。默认坐标和热栏已测试。
- AppleSkin、Sodium/Fastload、Indigo 和 Panorama Screens 的配套集成未迁移，详见 README。
- 搭桥的多人场景、纸娃娃的全部动作、字幕、枯树/倒木出现频率、未覆盖到的配方组合与全部炼药锅交互尚未逐项验证。
- Embeddium、Rubidium、Oculus、OptiFine 及大型整合包兼容性、多人联机和存档升级尚未验证。
- 本次使用测试新世界；不建议直接把已有重要存档作为首轮测试对象。

## Forge 实际差异的修正

- Forge 的 Mixin 处理器不支持上游炼药锅接口中的注入。移除该注入，改为在注册完成后为原版交互表安装可重复调用的药水行为包装；水药瓶交互继续走原实现。alpha.1 为 53 个 Mixin。
- 本次环境中，旧 Mixin 注解处理器通过 Filer 输出的映射文件发生截断。改用绝对文件路径输出，并让 `jar` / `check` 依赖 `validateRefmap`，避免编译成功但交付无效映射。
- Forge 将手持物品名称渲染从原版方法主体搬到了 `renderSelectedItemName`。物品提示注入改为定位该 Forge 方法，同时保留原版调用目标的映射。
- 聊天与字幕原来的 ModifyArgs 会触发合成参数类加载错误，改为 ModifyArg / Redirect，保持同样的坐标调整。
- Forge 在客户端设置加载前构造 ChatHud，上游字段初始化会缓存 null。聊天设置改为在实际使用时读取，避免进入世界后聊天覆盖层每帧报错。
