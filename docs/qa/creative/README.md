# 创造栏兼容测试

`CreativeInventoryQa.java` 是单独的开发测试模组，不属于 `src/main`，不会编译进 BedrockIfy 发布包。需在本项目 Yarn 名称的 Forge 开发环境中编译运行；反射调用使用开发名称。

该测试模组注册 12 个独立的、带搜索框的创造分类，以及一个模组物品；通过 Forge 的 `BuildCreativeModeTabContentsEvent` 向建筑标签添加该物品和两个带不同 `qa_variant` NBT 的钻石。

将测试类单独编译、打包为测试 JAR，添加匹配的 `META-INF/mods.toml`（modId 为 `bedrockifyqa`）与 `pack.mcmeta`，放到开发客户端的 `run/client/mods`。请使用单独的测试世界。

游戏进入创造模式后，可在客户端工作目录创建 `creative-qa-command.json`。测试模组在客户端主线程执行请求，删除请求文件，并写入 `creative-qa-result.json`。例如：

```json
{"action":"audit"}
```

审计检查四分类的物品/NBT 覆盖、展开后的成员完整性、Forge 追加的物品与变体、12 个独立分类以及分页。其他测试命令包括 `tab`、`search`、`click`、`page`、`resize`、`fallback`、`open` 与 `status`；参数见源码。`click` 使用原来的创造取物处理，包含 `PICKUP`、`QUICK_MOVE`、`SWAP` 和 `QUICK_CRAFT`。

alpha.4 新增保存栏回归：`seed_hotbars` 写入 9 行 × 9 个带 NBT 的模组物品和钻石，并强制下次选择页面从 `hotbar.nbt` 读取。默认每个物品含 128 KiB 测试数据，总载荷为 10.125 MiB。该命令会覆盖开发客户端的测试保存记录，必须使用独立的运行目录。

随后运行 `stress_hotbars`，默认循环 200 次建筑 → 保存栏 → 背包 → 保存栏 → 模组分类，每次检查原版库存容量保持 45、特殊页使用原版库存、经典页使用独立库存、切换后释放旧分类引用、已保存物品的行列/NBT 正确及滚动到最后五行。结果记录强制回收前后的堆使用量、槽位跟踪列表大小和独立库存清空状态。它不代表已复现用户的整合包崩溃。

alpha.4 最终构建分别运行 200 与 500 轮，新增保存栏原始槽位实例未被替换的检查，并重新验证取物、NBT 变体、拖动分配、搜索、分页和窗口回退。结果见 `alpha4/results.json`、`alpha4/build.log` 和 `alpha4/images`；旧的 `final-audit.json` 和 `images` 保留 alpha.3 的记录。

`final-audit.json` 是本轮最终审计输出。`images` 中的界面截图来自运行中的 Forge 开发客户端，分页中的钻石图标是上述测试模组的分类图标；配置截图显示的是“关闭折叠”的测试状态，两项初始默认均为开启。
