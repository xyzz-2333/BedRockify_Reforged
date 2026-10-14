# 生存界面开发验证

本目录的夹具与验证器用于生存背包、工作台、配方栏、界面记忆与全景的实机检查。测试代码不进入发布构建，只用于临时测试世界。

## 夹具与基本流程

`SurvivalInventoryQa.java` 是开发测试模组。生成器默认注册 900 个物品和 10,900 条配方（含 10,000 个不同 NBT 输出），验证器默认使用这个完整数据集。

1. 使用完整 JDK 17 编译项目：`./gradlew compileJava validateRefmap`；只在准备发布时执行 `build`。
2. 设置 `JAVA_HOME` 后执行 `python3 docs/qa/survival/build_fixture.py`，生成 `run/client/mods/bedrockify-survival-qa.jar`。
3. 执行 `./gradlew runClient`，进入一个允许指令的临时单人世界。
4. 在游戏运行时执行 `python3 docs/qa/survival/validate.py run/client --performance`。

验证器通过临时文件与测试模组交换带请求 ID 的指令；结果原子写入，按请求 ID 匹配。槽位操作通过真实客户端交互管理器发送到集成服务器。测试会清空玩家物品、解锁所有配方、切换生存模式并放置工作台，请只用于临时世界。原始结果写入 `run/client/survival-validation.json`。

可用 `SURVIVAL_STRESS_RECIPES` 调整生成器的压力配方数量，但默认完整验证器要求 10,000 个变体。木材派生压力配方使用简化的无序配方；真实的 3 × 3 形状另用原版楼梯配方验证。

## 验证器索引

所有验证器都以运行目录为参数（通常 `run/client`），请串行运行；隔离环境的快照同步可能使临时指令重放或结果变旧。

| 验证器 | 覆盖内容 | 结果文件 |
| --- | --- | --- |
| `validate.py` | 综合布局、槽位、合成、界面恢复与性能基线 | `survival-validation.json` |
| `validate_recovery.py` | 经典与原版界面切换、布局与背景尺寸恢复 | `alpha*-recovery-validation.json` |
| `validate_quickcraft.py` | alpha.10 直接合成 | `draft-quickcraft-validation.json` → `results/alpha10-quickcraft-validation.json` |
| `validate_alpha7.py` | 真实鼠标点击、翻页按钮与原生槽位命中 | `results/alpha7-targeted-validation.json` |
| `validate_alpha8.py` | 搜索焦点、字符输入与布局收紧 | `alpha8-*` |
| `validate_alpha8_pointer.py` | JEI 点击区域与鼠标命中 | `alpha8-*` |
| `validate_alpha9.py` | 界面状态记忆、搜索记忆与两个开关 | `alpha9-*` |
| `validate_alpha11_creative.py` | 创造栏搜索绘制与特殊分页回归 | `results/alpha11-*` |
| `validate_alpha11_panorama.py` | 原版界面全景回归 | `results/alpha11-panorama-validation.json` |
| `validate_panorama.py` | alpha.10 全景验证（世界菜单、加载类界面） | `results/alpha10-panorama-validation.json` |

`validate_quickcraft.py` 需要原生 X11 显示与 XTest，并需加载 JEI 与上述完整压力数据。它检查选中后重复点击、Shift 成品堆叠上限、材料不足、2×2/3×3、满背包、持有物品、NBT、返还容器与 JEI 切换取消；发布存档为 `results/alpha10-quickcraft-validation.json`，之后运行 `validate_alpha9.py` 检查界面记忆回归。验证器按请求 ID 去重，并在工作台打开前等待集成服务器完成关闭。

`validate_alpha7.py` 需同时加载本目录与 `../creative/build_fixture.py` 生成的两份开发测试模组，脚本会切换生存/创造模式并清空物品。`validate_alpha9.py` 同样需要生存与创造夹具及 JEI，覆盖两个独立生存配置、创造标签/Forge 页、搜索与分组恢复、磁盘重新读取及两个记忆开关；之后关闭界面记忆并依次重跑 alpha.8 的四份脚本，最后重新启用。

界面记忆的完整重启验证须通过设置界面保存开关，再设置搜索词与页码、关闭背包、退出世界并退出客户端；重新启动后直接打开背包，勿调用 `memory_reload`。测试 RPC 只改变内存中的开关。

## 记忆持久化单元测试

`InventoryUiStateTest.java` 直接测试实际持久化类，可用 JDK 17 和 Gson 2.10.1 编译后运行。它使用临时目录，验证中文读写、配置独立性、容量限制和损坏文件回退；不依赖 Minecraft。

## 兼容性与结果说明

- JEI、Curios 兼容测试需把对应 Forge 开发依赖通过 Loom 的 `modRuntimeOnly` 加入；不要把尚未重映射的生产 JAR 直接用作 Yarn 开发环境的依赖。测试数据同时定义两个玩家 Curios 戒指槽并允许钻石放入，未安装 Curios 时这些数据不会生效。
- `results/compat-validation.json` 是最终 JEI / Curios / 压力数据的成功结果；`results/stress-validation.json` 是早期无 JEI / Curios 的成功结果。其他小型记录补充实际鼠标、避让区域、空闲与筛选检查。
- alpha.7～alpha.11 的最终记录分别使用 `alpha7-`～`alpha11-` 前缀，截图在 `images/` 下同名前缀。alpha.10 的背景记录与全景截图见 [../panorama/README.md](../panorama/README.md)。
- 计时属于对应开发环境，不代表任意整合包的性能保证。
