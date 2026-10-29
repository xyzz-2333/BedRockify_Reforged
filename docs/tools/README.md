# 开发工具与测试入口

本目录只服务于开发与验证，不进入发布 JAR。资源导入类脚本从外部基岩版资源包提取数据，源文件与 ZIP 保留在 Git 之外；生成结果、来源清单和校验脚本纳入版本管理。

## 资源导入与生成

均为 Python 3.10+，可从任意工作目录运行。涉及材质的脚本另需 Pillow。

| 脚本 | 用途 | 典型调用 |
| --- | --- | --- |
| `import_education_pack.py` | 从基岩版教育内容 ZIP 导入材质与显示名称，写出逐文件 SHA-256 来源清单 | `python docs/tools/import_education_pack.py path/to/chemistry.zip` |
| `generate_education_assets.py` | 生成元素模型、掉落与火把配方；保留已导入材质，避免覆盖 | `python docs/tools/generate_education_assets.py` |
| `generate_reducing_recipes.py` | 生成 30 组默认材料分解配方（仅限已知的 1.20.1 输入） | `python docs/tools/generate_reducing_recipes.py` |
| `update_alpha12_ui_lang.py` | 补齐 alpha.12 的中英文配置与界面文字，不覆盖已有翻译 | `python docs/tools/update_alpha12_ui_lang.py` |
| `import_education2.py` | 导入强化玻璃、工作站和实验产物资源与来源清单 | `python docs/tools/import_education2.py path/to/chemistry.zip` |
| `import_armor_stand_poses.py` | 从基岩版动画文件提取 13 种盔甲架姿势的静态旋转 | `python docs/tools/import_armor_stand_poses.py <animation_file>` |
| `import_bedrock_water_colors.py` | 读取基岩版资源包（含带编号的覆盖层）生成群系水色表与来源清单，本机绝对路径不写入生成文件 | `python docs/tools/import_bedrock_water_colors.py <resource_packs 目录>` |

生成物应提交到源码树：

- 教育内容：`src/main/resources/assets/bedrockify/`、`src/main/resources/data/bedrockify/recipes/education/reducing/`、来源清单 `docs/education_resources.json`。
- 盔甲架姿势：来源清单 `docs/armor_stand_poses.json`（转换规则与摘要）。
- 群系水色：`src/main/resources/assets/bedrockify/water_colors.json`、来源清单 `docs/water_colors_sources.json`。

## 校验脚本

| 脚本 | 检查内容 |
| --- | --- |
| `validate_alpha12_resources.py` | 教育内容资源链接、材质与语言项一致性、序列化上限 |
| `validate_education2.py` | 教育更新 2 的材质来源、配方及模型引用 |
| `validate_water_colors.py` | 64 个 Java 群系的调色板覆盖、颜色格式、与来源清单逐项对应 |

## 开发测试入口

使用 JDK 17，从仓库根目录运行。先以 `gradlew compileJava validateRefmap validateBedrockAdvancements` 做源码与资源检查；仅准备发布时使用不带测试属性的 `gradlew clean build`。Windows 使用 `gradlew.bat`，其他平台使用 `./gradlew`。

以下每行属性单独使用，例如 `gradlew runServer -Palpha16GameTests`。测试类由属性加入源码集，不能带入正式包。`--offline` 仅在所需缓存完整时使用。

| 属性 | 任务 | 范围与运行目录 |
| --- | --- | --- |
| `armorStandGameTests` | `runServer` | 盔甲架姿势、装备、红石和存档；`run/armorstand-tests` |
| `education2GameTests` | `runServer` | 教育配方、库存、工作站、玻璃、爆炸和物品数据兼容；`run/education2-tests` |
| `chemistryUiGameTests` | `runServer` | 教育基础及预览领取、Shift、旧输出、实验结果、单用户占用；`run/chemistry-ui-tests` |
| `mechanicsGameTests` | `runServer` | 投射物、穿刺、闪门与搅拌机；`run/mechanics-tests` |
| `alpha16GameTests` | `runServer` | 七项经典内容测试，含 0.11 塔形、单次破损、拆核、最低层限制与基岩替换；`run/alpha16-tests` |
| `alpha17GameTests` | `runServer` | 成就服务端判定；`run/alpha17-tests` |
| `alpha14ClientSmoke` | `runClient` | 客户端初始化和配置构建；`run/alpha14-client-smoke` |
| `waterColorClientSmoke` | `runClient` | 64 群系水色、开关、资源覆盖及回退；`run/alpha15-water-smoke` |
| `alpha14Regression` | `runClient` | 盔甲架手臂射线、方向、举盾输入；`run/alpha14-regression` |
| `education2Client` | `runClient` | 教育模型、工作站菜单与实体同步；`run/education2-client` |
| `chemistryUiClient` | `runClient` | 化学菜单网络、输入、滑条、结果与截图；`run/chemistry-ui-client` |
| `inventoryUiClient` | `runClient` | 创造目录、生存配方栏和界面记忆；`run/inventory-ui-client` |
| `snowBookClient` | `runClient` | 双页书本编辑、签名、主副手保存及覆雪/融雪纹理；`run/alpha17-snow-book` |
| `guiTextClient` | `runClient` | 页码与中英文正文实际帧缓冲比较；`run/alpha17-snow-book` |

服务端 GameTest 自动创建测试环境并退出。需要 `regression` 存档的客户端入口仅使用独立副本，不复制 `session.lock`；`alpha14Regression` 的准备方法见 [ALPHA14](../ALPHA14.md#alpha14-update1-回归验证)。同一运行目录的测试串行执行。

反应核存档提取器：`python docs/tools/extract_reactor_011_fixture.py world.zip --check-fixture docs/tools/alpha16-tests/resources/data/bedrockify/reactor/mcpe011-spire.json`。输入必须是对应的原始 ZIP；容器重新封装会改变来源摘要。

历史生存/创造界面与 JEI、Curios 夹具统一从 [QA 索引](../qa/README.md)进入。具体测试覆盖和结果由对应专题记录，临时日志与本机路径不写入本文。
