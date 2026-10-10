# alpha.12 开发中增量源码快照

基准仓库：https://github.com/xyzz-2333/BedRockify_Reforged
基准提交：81127090f5f0781a77d431e7d15cb0c76bf1880a

## 使用
将 changes/ 内的文件按原有路径覆盖至上述提交的完整仓库。此包只含本轮新增或修改的文件；不是可独立构建的完整项目，也不是可放进 mods 的 JAR。

## 已写入的内容
- 可配置的慢速自然回血：跳过 Java 饱和度快速回血，默认每 80 tick 回 1 点生命值。
- 可配置的取消攻击蓄力，保留原版受伤无敌时间；包含服务端规则同步逻辑。
- 118 种元素及未知元素方块，材料分解器、输入/预览/领取/剩余产物持久化的代码。
- 水下火把，以及蓝、红、紫、绿火把；四种氯化物材料。
- 元素、火把、材料分解器和氯化物的自制像素素材及模型、掉落表。
- 五种火把的合成配方及 30 组数据驱动的分解配方。
- 元素/氯化物的创造栏折叠；教育版配方不受旧有原版配方替换开关过滤。

## 尚未完成及验证状态
- 新增物品、配置和界面文字的中英本地化尚未补齐；目前会显示部分翻译键。
- 首轮源码编译曾出现两处 Forge creative event API 类型错误，已修改为 Supplier 参数；修正后尚未取得编译通过结果。
- 后续构建遇到损坏依赖及 Loom 缓存/网络问题，最新构建未生成有效的通过报告。
- 尚未进行新增功能的游戏内、多人、存档或模组兼容测试；分解器防重复产出、Shift 转移、水下放置等均待验证。
- 此快照尚未推送 GitHub，也未打包发布 JAR。

## 实现范围
- 新增方块与物品的联机需要客户端和服务端安装匹配版本。
- 材料分解器及氯化物目前仅创造/命令获取，没有自定义生存合成配方。
- 彩色火把提供彩色外观/粒子，光照仍是原版光照。
- 纹理是项目自制素材，不保证与教育版原贴图逐像素一致。
- 分解默认只覆盖已列出的原版输入；模组物品可通过自定义 material_reducing 数据包配方扩展，不按物品名称猜测化学组成。

## 生成脚本
- tools/generate_education_assets.py：重新生成本轮贴图、模型、掉落表和火把配方，需要 Python + Pillow。
- tools/generate_reducing_recipes.py：重新生成默认分解配方，需要 Python 标准库。

资料参考：Minecraft Wiki 的 Material Reducer、Element、Hunger 条目及 Minecraft Education Chemistry Lab Journal：
https://education.minecraft.net/content/dam/education-edition/software-downloads/Chemistry-Lab-Journal.pdf
