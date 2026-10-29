# alpha.17：基岩版成就、雪天树叶与书本界面

目标平台 Minecraft Java 1.20.1 / Forge。成就作为 `bedrockify:bedrock/*` 的 Java 进度实现，进度由原版按存档及玩家分别保存。

## 范围

以 [Minecraft Wiki 成就表](https://minecraft.wiki/w/Achievement) 的 133 项为目录。与 Java 1.20.1 原版进度语义重复的 43 项不另建进度；8 项依赖 1.20.1 尚无的内容，等待升级目标版本。其余 82 项进入本版实现。游戏内判定以本项目对应版本的机制为准，描述中的目标条件不能简化为仅持有物品。

82 项进度按游戏进程归入“初入世界 → 安家立业 → 制作与建造 → 探索世界 → 村庄生活 → 战斗挑战 → 技艺精通”七个章节。章节节点会自动解锁，只用于组织进度树；完成顺序不限制玩家取得具体成就，也不会替玩家完成任何成就。

## 待实现清单

勾选表示已接入进度判定；复杂交互仍需游戏内验收，尤其是漏斗转移、活塞往返、地图墙及探险家地图。游戏内验收记录保留在仓库外的 `outputs`。

- [x] **Ahoy!**：Discover a shipwreck.
- [x] **Alternative Fuel**：Manually insert and burn a dried kelp block in a furnace.
- [x] **Archer**：Defeat a creeper using arrows.
- [x] **Artificial Selection**：Breed a horse with a donkey to produce a mule.
- [x] **Atlantis?**：Discover an ocean ruin.
- [x] **Bake Bread**：Craft bread from wheat.
- [x] **Beam Me Up**：Move more than 100 blocks using one ender-pearl throw.
- [x] **Benchmaking**：Make a crafting table from four planks.
- [x] **Buy Low, Sell High**：Complete a trade at the lowest possible price.
- [x] **Camouflage**：Defeat a mob while wearing its matching head.
- [x] **Castaway**：Eat dried kelp once, then eat no other food for three game days.
- [x] **Chestful of Cobblestone**：Open a chest containing 1,728 cobblestone.
- [x] **Cow Tipper**：Collect leather.
- [x] **Delicious Fish**：Take freshly cooked cod from a furnace, smoker or campfire.
- [x] **Diamonds to you!**：Give another player diamonds by dropping them.
- [x] **Disenchanted**：Remove an item's non-curse enchantments with a grindstone to receive experience.
- [x] **Dispense with This**：Build a dispenser.
- [x] **Do a Barrel Roll!**：Propel yourself with a Riptide-enchanted trident.
- [x] **Dry Spell**：Use a furnace to remove water from a sponge.
- [x] **Echolocation**：Give fish to a dolphin so it guides you to treasure.
- [x] **Enchanter**：Build an enchanting table.
- [x] **Feeling Ill**：Defeat an evoker.
- [x] **Free Diver**：Remain submerged for two minutes.
- [x] **Freight Station**：Move an item from a chest minecart into a chest through a hopper.
- [x] **Fruit on the Loom**：Create a banner with the enchanted-apple pattern.
- [x] **Have a Shearful Day**：Remove a sheep's wool with shears.
- [x] **Hot Topic**：Build a furnace with eight cobblestone.
- [x] **Inception**：Use one piston to push another, then use that piston to pull the first.
- [x] **Iron Belly**：Eat rotten flesh while starving.
- [x] **Getting Wood**：Punch a tree until a block of wood pops out.
- [x] **Iron Man**：Equip iron armor in every armor slot.
- [x] **It's a Sign!**：Make and place an oak sign.
- [x] **Kill the Beast!**：Defeat a ravager.
- [x] **Leader of the Pack**：Tame five wolves.
- [x] **Let It Go!**：Freeze deep-ocean water using Frost Walker boots.
- [x] **Librarian**：Add bookshelves to improve an enchanting table.
- [x] **Lion Hunter**：Earn an ocelot's trust.
- [x] **Map Room**：Display nine fully explored neighboring maps in a square of item frames.
- [x] **Master Trader**：Trade enough goods to earn 1,000 emeralds.
- [x] **Me Gold!**：Excavate buried treasure.
- [x] **Moskstraumen**：Activate a Conduit.
- [x] **MOAR Tools**：Craft one of every tool type.
- [x] **On A Rail**：Ride a minecart at least 500 blocks away in one direction.
- [x] **One Pickle, Two Pickle, Sea Pickle, Four**：Put four sea pickles together in one block space.
- [x] **Organizational Wizard**：Rename a shulker box at an anvil.
- [x] **Overkill**：Inflict at least 18 damage points with one attack.
- [x] **Overpowered**：Consume an enchanted golden apple.
- [x] **Passing the Time**：Accumulate 100 in-game days of play.
- [x] **Plethora of Cats**：Tame twenty stray cats.
- [x] **Pork Chop**：Eat a porkchop that you have cooked.
- [x] **Pot Planter**：Make a flower pot and place it in the world.
- [x] **Rabbit Season**：Eat rabbit meat after cooking it.
- [x] **Rainbow Collection**：Collect wool in every one of its 16 colors.
- [x] **Renewable Energy**：Use charcoal to turn more logs into charcoal.
- [x] **Repopulation**：Feed wheat to two cows to produce a calf.
- [x] **Saddle Up**：Make a horse tame.
- [x] **Sail the 7 Seas**：Explore every ocean biome.
- [x] **Sleep with the Fishes**：Remain underwater for an entire game day.
- [x] **Smelt Everything!**：Use three hoppers to connect three chests with one furnace.
- [x] **So I Got That Going for Me**：Lead a caravan with five or more llamas.
- [x] **Sound the Alarm!**：Ring a village bell after a villager has been hurt nearby.
- [x] **Stayin' Frosty**：Enter lava with Fire Resistance active.
- [x] **Super Fuel**：Use lava as furnace fuel and take a smelted item.
- [x] **Super Sonic**：Fly through a one-block opening with elytra at more than 40 meters per second.
- [x] **Taking Inventory**：View your inventory.
- [x] **Taste of Your Own Medicine**：Apply poison to a witch using a splash potion.
- [x] **The Deep End**：Defeat an elder guardian.
- [x] **The Haggler**：Earn or spend 30 emeralds in villager or wandering-trader trades.
- [x] **The Lie**：Make cake with wheat, sugar, eggs and milk.
- [x] **Tie Dye Outfit**：Color each of the four leather armor pieces in a cauldron.
- [x] **Time for Stew**：Hand suspicious stew to another player.
- [x] **Time to Farm!**：Craft a hoe.
- [x] **Time to Mine!**：Make a wooden pickaxe with sticks and planks.
- [x] **Time to Strike!**：Craft a wooden sword.
- [x] **Top of the World**：Place scaffolding at the highest building level.
- [x] **Trampoline**：Bounce upward 30 blocks using slime.
- [x] **Treasure Hunter**：Buy an explorer map from a cartographer and reach its marked structure.
- [x] **Wax on, Wax off**：Wax copper blocks and scrape the wax away from every required copper type.
- [x] **We're being attacked!**：Start a raid by pillagers.
- [x] **When Pigs Fly**：Ride a saddled pig when it takes damage from falling.
- [x] **Where Have You Been?**：Collect a morning present from a tame cat.
- [x] **Zoologist**：Feed bamboo to two pandas to produce a baby.

## 与 Java 原版进度重复

- Acquire Hardware
- Adventuring Time
- Bee our guest
- Birthday song
- Body Guard
- Bullseye
- Careful restoration
- Caves & Cliffs
- Cheating Death
- Cover me in debris
- DIAMONDS!
- Feels Like Home
- Getting an Upgrade
- Great View From Up Here
- Hot tourist destination
- I am a Marine Biologist
- I've got a bad feeling about this
- Into Fire
- Into The Nether
- It spreads
- Local Brewery
- Monster Hunter
- Oooh, shiny!
- Planting the past
- Return to Sender
- Smithing with style
- Sneak 100
- Sniper Duel
- Sound of Music
- Star trader
- Sticky Situation
- The Beaconator
- The Beginning.
- The Beginning?
- The End
- The End... Again...
- The End?
- The Healing Power of Friendship!
- Total Beelocation
- Whatever Floats Your Goat
- With our powers combined!
- You Need a Mint
- Zombie Doctor

## 等待目标版本升级

- Crafters Crafting Crafters
- Heart Transplanter
- Mob Kabob
- Over-Overkill
- Revaulting
- Stay Hydrated!
- Uh-oh.
- Who Needs Rockets?

## 实施约定

- 不另建成就存档；使用 Java 原版进度文件与服务器判定。
- 原版进度条件可准确表达时使用数据包 JSON；需要累积、交易、多人交互或环境状态时用服务端事件和自定义判定。
- 先复核每项与 Java 原版进度的语义映射，再逐项完成判定，避免仅凭名称筛除。

## 同版界面修正

### 雪天树叶渐白

- 默认开启 `snowyLeaves`，设置位于“视觉改进”；高级 Mixin 开关为 `client.features.snowyLeaves`。
- 橡树、云杉、白桦、丛林、金合欢、深色橡树和红树树叶参与。杜鹃、盛开的杜鹃及樱花树叶保留原色；物品栏树叶不变。
- 按生物群系基础温度 `<0.15` 且允许降水判定，保留生物群系边缘的颜色混合；温暖群系因海拔而下雪时不因此变白。覆雪表现属于天气着色，不依赖叶片上方是否放置雪层。
- 降雪时分八级渐白，停止后逐级恢复；本移植每个方向使用 600 个客户端游戏刻（正常速度约 30 秒），这是尚待基岩版实机计时校准的参数，不宣称精确复刻。加入已在下雪的世界会直接采用当前风雪外观。
- 覆雪同时渐变着色和纹理亮度：根据当前资源包的七种树叶原图生成八级覆雪图集精灵，完全覆雪时将叶面 RGB 提亮至最多原值两倍并截断到 255，避免仅移除绿色染色后仍然灰暗。保留透明孔洞、动画帧与丛林树叶的黄色细节；使用正常方块光照和面阴影，不做自发光。雪停后恢复原图，资源重载时重新生成。
- 与基岩版的高品质树叶条件对应，Java 的“流畅”画质保留原色，“高品质”和“极佳”显示效果。暂停时进度暂停；切换画质或功能开关后保留当前天气进度。
- 仅在阶段改变时重建已渲染过、含相关冷区树叶的附近区段；不修改服务端天气、生物群系、方块状态或存档。
- 原版资源依据为 Bedrock 1.21.94 的 `renderchunk.fragment` 中 `SEASONS` 白色与植被调色板混合分支、原版生物群系气候数据和树叶材质表；官方协议亦单列 [foliage snow](https://mojang.github.io/bedrock-protocol-docs/1.26.50-preview.25/types/biome-definition-data/) 与气候字段。

### 书与笔双页界面

- 页码与两侧正文统一为原版黑色无阴影字；浅底搜索框及书页的文字绘制约定见 [GUI 文字绘制约定](GUI_RENDERING.md)。

- 默认开启 `doublePageBooks`，设置位于“视觉改进”；高级 Mixin 开关为 `client.features.doublePageBooks`。只调整书与笔的编辑界面，签名后的阅读界面仍使用原版。
- 双页尺寸参考 Bedrock 原版 `book_screen.json`：整体 320×207、每页 160×207。左右页可点击切换输入焦点；两侧保存为原版相邻两页；翻页按钮和 Page Up / Page Down 每次移动两页。
- 继续使用原版选区、拖选、复制粘贴、字符输入、光标和文字验证。每页保留 Java 的 114×128 排版区、最多 1024 个字符及 100 页上限；不放宽服务端格式与数量限制。点击尚不存在的右页时按原版方式创建空白页。
- 签名返回原版封面编辑；空标题限制、作者、末尾空页修剪、主手/副手提交和原版书本网络包均沿用原版。窗口小于 340×250 时使用原版单页布局；缩放或调整窗口不丢失页面文本。
- 书背景、书脊与左右页折痕 PNG 从 Bedrock 1.21.94 原版资源直接复制，未手绘或重采样；渲染按九宫格拉伸。源文件尺寸与 SHA-256 见 [资源清单](snow_book_resources.json)。

### 验证入口

`gradlew --gradle-user-home .gradle-user runClient -PsnowBookClient --offline` 在 `run/alpha17-snow-book/saves/regression` 测试世界运行集成客户端验证，覆盖左右页编辑与翻页、大小窗口切换、原版超长文本拒绝、签名、主副手服务端保存，以及天气渐白/恢复、群系与树种筛选、物品栏及流畅画质回退。覆雪检查实际世界模型选中的纹理、橡树叶不透明像素平均亮度、七种纹理逐像素透明度、丛林叶黄色细节和雪停后的原图恢复，并输出实际渲染截图；不只检查染色值是否为白色。测试类仅在显式启用属性时加入源码集，不进入正式 JAR。

### 其他界面修正

- 世界内打开原版设置及语言页面时保留世界背景；主菜单中的设置继续使用菜单全景。
- 创造目录增加砂岩（八种）、石砖、其他食物、农作物、迷之炖菜和蘑菇分组。熟食以熟鸡肉为图标；河豚与热带鱼归入自然页的生肉组，紫颂果单独显示。
- 创造目录上方的三枚图标在完整目录、双栏目录和仅物品栏之间切换，与原有快捷栏、搜索按钮保持同一排。双栏左侧提供分组、搜索及滚动，右侧保留原版玩家背包、装备与 2×2 合成，两侧紧邻；切换视图前需先放下光标上的物品。窗口过小时回退原版界面。
- 双栏目录与生存配方栏共用悬停及选中高亮。双栏目录采用无阴影的深灰搜索文字，并接入原有界面记忆：保存视图模式、分类、展开分组、滚动位置；搜索词遵守“记住搜索”开关。
- 创造目录背景与未选中页签使用 `#626262`。生存快捷合成移除底部配方计数和翻页按钮，改为逐行滚轮及可拖动滚动条，与背包面板保持同高；缺失材料的格子背景使用 `#d64747`。
- 三枚视图图标从基岩版 1.21.94 原版资源包直接复制，未绘制或重采样；来源与摘要见 [资源清单](inventory_ui_resources.json)。

