# 面向新版本的移植准备

当前基线仍为 Minecraft 1.20.1 / Forge / Java 17 / Yarn。本文只讨论向 1.21、26.1 及更高版本迁移。

## 结论与本轮改动

值得提前收拢已经确定会变化的数据操作；现在就引入多版本构建、反射探测或通用渲染框架，收益不足。实际移植时按目标版本维护分支，确定加载器后再选构建插件和映射，不在当前分支混入目标版本依赖。

本轮新增 `platform/item/ItemStackData`，承接元素中子数读写、潜影盒染色的数据复制、创造目录去重键。调用处不再直接操作物品 NBT；1.20.1 后端仍保持原格式和行为：

- 保留 `Neutrons` 的拼写与整数值，缺失与零仍可区分；不重写旧物品或另存第二份字段。
- 染色复制完整物品数据并深拷贝，保留容器内容、名称及其他模组数据。
- 目录键包含物品和完整数据，忽略数量；保持原先 null 与空标签的区别。键不暴露可变 NBT，原物品后续改变不会破坏 HashSet。

未来组件后端必须保留这些语义。去重不能只读取 `minecraft:custom_data`，否则药水、附魔、名称等组件不同的物品会被误合并。染色也不能只复制自定义数据，应根据目标版本的组件补丁语义保留源物品的数据和显式移除项，同时保留目标物品类型。

## 确定的断点与后续处理

| 边界 | 当前代码位置 | 实际移植时的工作 |
| --- | --- | --- |
| 工具链、映射与加载器 | `build.gradle`、`settings.gradle`、`gradle.properties`、`META-INF/mods.toml` | 1.21 使用 Java 21；26.1 使用 Java 25，且不再混淆。为确定的 Forge/NeoForge 目标选择匹配的插件、映射和元数据。当前 Java 17 与版本范围保持不变 |
| 物品数据 | `platform/item/ItemStackData`；炼药锅与提示中的 `PotionUtil` | 1.20.5 起物品 NBT 被数据组件替代。替换本后端，单独迁移原版药水/附魔 API。旧 `Neutrons` 读取应考虑升级后的 `minecraft:custom_data`；若改用自定义组件，需要明确迁移路径，不能仅注册组件就假定旧值会自动搬过去 |
| 方块实体和实体持久化 | `WaterCauldronBlockEntity`、教育工作站/分解器、盔甲架与水下 TNT | 与物品数据分别处理，适配目标版本的注册表访问、Codec 或存储接口。保留已有键、注册 ID 与命名空间；实体/方块 NBT 不通过物品后端处理 |
| 自定义配方 | `ChemistryRecipe`、`MaterialReducingRecipe` | 从手写 JSON / PacketByteBuf 序列化迁往目标版本的 Codec / StreamCodec，适配配方输入类型与查找接口；核对原版及自定义配方的客户端同步需求 |
| 资源包/数据包 | `src/main/resources`、`src/main/generated`、`docs/tools/generate_*` | 核对目标包格式、1.21 的单数数据目录（例如 recipe / loot_table）及配方结果格式；更新生成脚本与生成物。更高版本另需检查物品模型等变化，不能只修改 pack_format |
| 网络与逻辑侧 | `MechanicsNetwork`、`MechanicsRules`、工作站开屏调用 | 按目标加载器迁移通道/负载注册，保留服务端权威、主线程执行、断线清理及协议校验。有效逻辑侧不能替换为仅判断物理客户端 |
| GUI、HUD 与实体渲染 | `client/features`、`BedrockifyClient`、客户端 Mixin | 按目标版本适配渲染状态、提交接口、HUD 事件、屏幕输入及配方显示；1.21 系列内部也存在断点，不视为统一 API |
| Mixin 与访问权限 | `bedrockify.mixins.json`、`bedrockify.accesswidener`、`mixin/` | 对照目标映射逐条重定位描述符、注入点与权限，重新检查 refmap；不能用统一 `require=0` 隐藏失效的功能 |
| 外部模组兼容 | JEI、Curios、渲染及视角兼容代码 | 对照各自目标版本 API 重新验证，不能从当前模组检测结果推断未来版本可用 |

`modId=bedrockify`、`group=dev.bedrockify.forge`、配置与资源路径不因未来迁移准备而改名。包名里的 forge 目前只作为历史命名；不为此移动整棵源码树。

## 验证与范围

已有 `-Peducation2GameTests` 入口新增 `itemDataCompatibility`，检查旧中子数读取及物品存取、数量无关的目录去重、可变源数据隔离、药水变体与潜影盒数据复制。可运行 `bash gradlew -Peducation2GameTests runServer`；发布构建使用不带属性的 `bash gradlew clean build`，防止测试类进入分发包。

## 官方参考

- [1.20.4 → 1.20.5：Java 21、数据组件、StreamCodec 与配方序列化](https://docs.neoforged.net/primer/docs/1.20.5/)
- [1.20.6 → 1.21：数据目录及接口变化](https://docs.neoforged.net/primer/docs/1.21/)
- [1.21.1 → 1.21.2：渲染状态等变化](https://docs.neoforged.net/primer/docs/1.21.2/)
- [1.21.11 → 26.1：Java 25 与后续 API 变化](https://docs.neoforged.net/primer/docs/26.1/)
- [NeoForge 26.1 工具链与不再混淆的迁移说明](https://neoforged.net/news/26.1release/)
