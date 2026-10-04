# 生存界面开发验证

本目录中的测试代码只用于临时测试世界，发布构建不会编译或打包它们。生成器默认注册 900 个物品和 10,900 条配方，验证器默认使用这个完整数据集。

1. 使用完整 JDK 17 构建项目：`./gradlew build`。
2. 设置 `JAVA_HOME` 后执行 `python3 docs/qa/survival/build_fixture.py`，生成 `run/client/mods/bedrockify-survival-qa.jar`。
3. 执行 `./gradlew runClient`，进入一个允许指令的临时单人世界。
4. 在游戏运行时执行 `python3 docs/qa/survival/validate.py run/client --performance`。

验证器通过临时文件与测试模组交换带请求 ID 的指令；结果原子写入，按请求 ID 匹配。槽位操作通过真实客户端交互管理器发送到集成服务器。测试会清空玩家物品、解锁所有配方、切换生存模式并放置工作台，请只用于临时世界。原始结果写入 `run/client/survival-validation.json`。

可用 `SURVIVAL_STRESS_RECIPES` 调整生成器的压力配方数量，但默认完整验证器要求 10,000 个变体。木材派生压力配方使用简化的无序配方；真实的 3 × 3 形状另用原版楼梯配方验证。

JEI、Curios 兼容测试需把对应 Forge 开发依赖通过 Loom 的 `modRuntimeOnly` 加入；不要把尚未重映射的生产 JAR 直接用作 Yarn 开发环境的依赖。测试数据同时定义两个玩家 Curios 戒指槽，并允许钻石放入，未安装 Curios 时这些数据不会生效。

`results/compat-validation.json` 是最终 JEI / Curios / 压力数据的成功结果；`results/stress-validation.json` 是早期无 JEI / Curios 的成功结果。其他小型记录补充实际鼠标、避让区域、空闲与筛选检查。计时属于对应开发环境，不代表任意整合包的性能保证。
