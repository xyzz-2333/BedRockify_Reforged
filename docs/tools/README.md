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
| `import_armor_stand_poses.py` | 从基岩版动画文件提取 13 种盔甲架姿势的静态旋转 | `python docs/tools/import_armor_stand_poses.py <animation_file>` |
| `import_bedrock_water_colors.py` | 读取基岩版资源包（含带编号的覆盖层）生成群系水色表与来源清单，本机绝对路径不写入生成文件 | `python docs/tools/import_bedrock_water_colors.py <resource_packs 目录>` |

生成物应提交到源码树：

- 教育内容：`src/main/resources/assets/bedrockify/`、`data/bedrockify/recipes/education/reducing/`、来源清单 `docs/education_resources.json`。
- 盔甲架姿势：来源清单 `docs/armor_stand_poses.json`（转换规则与摘要）。
- 群系水色：`src/main/resources/assets/bedrockify/water_colors.json`、来源清单 `docs/water_colors_sources.json`。

## 校验脚本

| 脚本 | 检查内容 |
| --- | --- |
| `validate_alpha12_resources.py` | 教育内容资源链接、材质与语言项一致性、序列化上限 |
| `validate_water_colors.py` | 64 个 Java 群系的调色板覆盖、颜色格式、与来源清单逐项对应 |

## 开发测试入口

这些测试类由 Gradle 属性按需加入源码集，因此**生成分发 JAR 前必须不带属性重新执行 `clean build`**。

| 属性 | 命令 | 说明 |
| --- | --- | --- |
| `-ParmorStandGameTests` | `gradlew runServer` | 服务端 Forge GameTest：姿势与装备、红石与自定义存档、关闭功能与锁定槽位；运行目录 `run/armorstand-tests`，测试结束后自动退出 |
| `-Palpha14ClientSmoke` | `gradlew runClient` | 隔离客户端启动检查：注入加载与配置页构建后自动退出 |
| `-PwaterColorClientSmoke` | `gradlew runClient` | 隔离客户端水色检查：64 个群系覆盖、水面/雾色独立、开关、资源覆盖与非法值回退 |
| `-Palpha14Regression` | `gradlew runClient` | 客户端回归：盔甲架手臂射线与八方向朝向、潜行举盾移动输入。需先把 GameTest 生成的 world 复制到 `run/alpha14-regression/saves/regression`（不要复制 `session.lock`），且只使用测试副本 |

开发环境的客户端界面验证（生存/创造界面、JEI、Curios）使用 `../qa/` 下的夹具与验证器，见 [../qa/README.md](../qa/README.md)、[../qa/survival/README.md](../qa/survival/README.md) 和 [../qa/creative/README.md](../qa/creative/README.md)。
