# alpha.17：基岩版成就移植

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

