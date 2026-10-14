# 原生界面全景验证

alpha.10 与 alpha.11 曾用这套夹具核对 1.20.1 原版界面上的旋转全景。夹具是独立的开发测试模组，不属于 `src/main`，不会进入发布包。

## 组成

- `MenuPanoramaQa.java`：测试模组，在客户端主线程读写 `run/client` 下的命令/结果 JSON 文件，用于切换界面、检查列表背景与阴影、模拟鼠标键盘、缩放窗口、调整全景旋转速度并探测加载类界面。
- `build_fixture.py`：编译该测试模组，输出 `run/client/mods/bedrockify-panorama-qa.jar`（modId 为 `bedrockifypanoramaqa`）。需要已构建的 `build/classes/java/main`、`JAVA_HOME` 与 `GRADLE_USER_HOME`。
- `../survival/validate_alpha11_panorama.py`：alpha.11 的验证器，参数为运行目录（通常 `run/client`）。
- `../survival/validate_panorama.py`：alpha.10 的验证器，同样是运行目录参数。

## 使用

```sh
./gradlew compileJava
python3 docs/qa/panorama/build_fixture.py
./gradlew runClient
python3 docs/qa/survival/validate_alpha11_panorama.py run/client
```

验证器通过临时文件与测试模组交换带请求 ID 的指令，要求客户端已进入标题页；脚本之间不要并发运行。覆盖内容包括世界列表与搜索保持原版行为、关闭开关后恢复原背景、窗口缩放、全景旋转速度、加载/生成/准备类界面以及选项菜单。

原始记录见 `../survival/results/alpha10-panorama-validation.json` 与 `alpha11-panorama-validation.json`，截图见 `../survival/images/alpha10-*` 与 `alpha11-options-panorama.png`。
