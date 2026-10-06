# 生存界面开发验证

本目录中的测试代码只用于临时测试世界，发布构建不会编译或打包它们。生成器默认注册 900 个物品和 10,900 条配方，验证器默认使用这个完整数据集。

1. 使用完整 JDK 17 编译项目：`./gradlew compileJava validateRefmap`；只在准备发布时执行 `build`。
2. 设置 `JAVA_HOME` 后执行 `python3 docs/qa/survival/build_fixture.py`，生成 `run/client/mods/bedrockify-survival-qa.jar`。
3. 执行 `./gradlew runClient`，进入一个允许指令的临时单人世界。
4. 在游戏运行时执行 `python3 docs/qa/survival/validate.py run/client --performance`。

验证器通过临时文件与测试模组交换带请求 ID 的指令；结果原子写入，按请求 ID 匹配。槽位操作通过真实客户端交互管理器发送到集成服务器。测试会清空玩家物品、解锁所有配方、切换生存模式并放置工作台，请只用于临时世界。原始结果写入 `run/client/survival-validation.json`。

alpha.10 直接合成用 `python3 docs/qa/survival/validate_quickcraft.py run/client` 验证，需原生 X11 显示和 XTest、JEI 与上述完整压力数据。它检查选中后重复点击、Shift 成品堆叠上限、材料不足、2×2/3×3、满背包、持有物品、NBT、返还容器与 JEI 切换取消；原始结果沿用草稿文件名 `run/client/draft-quickcraft-validation.json`，发布存档为 `results/alpha10-quickcraft-validation.json`。之后运行 `validate_alpha9.py` 检查界面记忆回归。验证器按请求 ID 去重，并在工作台打开前等待集成服务器完成关闭；请串行运行验证器。

可用 `SURVIVAL_STRESS_RECIPES` 调整生成器的压力配方数量，但默认完整验证器要求 10,000 个变体。木材派生压力配方使用简化的无序配方；真实的 3 × 3 形状另用原版楼梯配方验证。

JEI、Curios 兼容测试需把对应 Forge 开发依赖通过 Loom 的 `modRuntimeOnly` 加入；不要把尚未重映射的生产 JAR 直接用作 Yarn 开发环境的依赖。测试数据同时定义两个玩家 Curios 戒指槽，并允许钻石放入，未安装 Curios 时这些数据不会生效。

`results/compat-validation.json` 是最终 JEI / Curios / 压力数据的成功结果；`results/stress-validation.json` 是早期无 JEI / Curios 的成功结果。其他小型记录补充实际鼠标、避让区域、空闲与筛选检查。计时属于对应开发环境，不代表任意整合包的性能保证。

alpha.7 的针对性验证用 `python3 docs/qa/survival/validate_alpha7.py run/client`，需同时加载本目录与 `docs/qa/creative/build_fixture.py` 生成的两份开发测试模组。该脚本会切换生存/创造模式并清空物品，检查真实鼠标点击和原生槽位命中。原始记录为 `results/alpha7-targeted-validation.json`；合成、界面恢复、Curios、创造分类覆盖及截图均使用 `alpha7-` 前缀。

alpha.8 依次运行 `validate_alpha8.py`、`validate.py`、`validate_recovery.py`、`validate_alpha8_pointer.py`。测试器应与客户端共享同一文件系统环境，脚本之间不要并发；隔离环境的快照同步可能使临时指令重放或结果变旧。最终成功记录使用 `alpha8-` 前缀，补充实际输入、JEI 点击及 Curios 状态见 `results/alpha8-extra-checks.json`。

alpha.9 先运行 `python3 docs/qa/survival/validate_alpha9.py run/client`，需同时加载生存与创造开发夹具及 JEI。脚本覆盖两个独立生存配置、创造标签/Forge 页、搜索与分组恢复、磁盘重新读取及两个记忆开关。之后关闭界面记忆，依次重跑上述 alpha.8 四份验证脚本，最后重新启用记忆。测试 RPC 只改变内存中的开关；完整重启验证前，须通过设置界面保存开关，再设置搜索词与页码、关闭背包、退出世界并退出客户端。重新启动后直接打开背包，勿调用 `memory_reload`。最终结果、补充 Curios 操作、重启验证及截图使用 `alpha9-` 前缀。

`InventoryUiStateTest.java` 直接测试实际持久化类，可用 JDK 17 和 Gson 2.10.1 编译后运行。它使用临时目录，验证中文读写、配置独立性、容量限制和损坏文件回退；不依赖 Minecraft。

alpha.10 的背景验证另见 `../panorama/README.md`。最终背景、直接合成和记忆回归记录使用 `results/alpha10-` 前缀。全景截图来自真实原版界面；加载截图使用原生 `LevelLoadingScreen` 探针，实际进入测试世界也由验证器覆盖。
