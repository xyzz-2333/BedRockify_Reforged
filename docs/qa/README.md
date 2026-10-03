# 开发环境验证

`PortVerification.java` 是 alpha.2 游戏复测使用的临时 Forge 模组，单独编译并放入 `run/client/mods`。它位于文档目录，不属于 main 源码集，也不包含在发布 JAR 中。只用于 Loom 的 Yarn 开发环境，请勿放入普通游戏实例或生产服务端。

使用 JDK 17，以主项目的 `sourceSets.main.compileClasspath` 和 `sourceSets.main.output.classesDirs` 为 classpath，通过 `javac -proc:none` 编译。另建临时 JAR，包含生成的 `qa/` 类文件、javafml 47 的 mods.toml（modId 为 `bedrockify_port_qa`）及 pack_format 15 的 pack.mcmeta。启动开发客户端，进入允许命令的独立测试世界。输出会追加到运行目录的 `qa-results.log`。

## 配方

依次运行 `/bifyqa modern`、`/reload`、`/bifyqa recipes`；再用 `legacy`、`off` 分别重复。每次等待重载结束后读取实际配方。最后恢复 modern 并重载。命令改变当前进程的设置，不写配置文件。

## 站立搭桥

```text
/gamemode creative
/setblock 64 99 4 minecraft:stone
/setblock 64 99 5 minecraft:air
/tp @s 64.5 100 4.85 0 60
/item replace entity @s weapon.mainhand with minecraft:stone 64
/bifyqa bridge
```

确保不在飞行、没有按潜行且地面判定已更新，日志应显示 ready=true、sneaking=false、grounded=true、requireSneaking=false。保持视角不变右键，再执行：

```text
/execute if block 64 99 5 minecraft:stone run tellraw @s {"text":"STANDING_BRIDGE_PASS","color":"green"}
```

## 飞行

进入创造飞行后传送到没有碰撞的高空：`/tp @s 64.5 250 4.5 0 0`。执行 `/bifyqa flight 25`，持续按前进约 1–2 秒后松开。该命令记录 160 刻客户端速度，在本模组减速执行后采样；只比较 flying=true 且确实从前进转为松键的样本。再分别用 100 和 0 复测，每次先回到相同的高空位置，排除碰撞。

25 应保留非零速度再逐步减小；100 的松键首刻应为零；0 应保留 Java 原版惯性。25 的垂直验证使用一次持续按跳跃后松开的采样。结束后恢复强度 25，通过配置界面保存；测试保存与恢复默认时核对 JSON 实际值。

临时模组、测试存档、依赖缓存与本机工具补丁都不属于发布物。
