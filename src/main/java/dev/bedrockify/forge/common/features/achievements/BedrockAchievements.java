package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.advancement.Advancement;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ConduitBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.item.map.MapIcon;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.stat.Stats;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.village.VillagerProfession;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.HashSet;
import java.util.Set;

public final class BedrockAchievements {
    private static final int TWO_MINUTES = 2 * 60 * 20;
    private static final int GAME_DAY = 24_000;
    private static final int ONE_HUNDRED_DAYS = 100 * GAME_DAY;
    private static final Map<UUID, Integer> SUBMERGED_TICKS = new HashMap<>();
    private static final Map<UUID, RailRide> RAIL_RIDES = new HashMap<>();
    private static final Map<EnderPearlEntity, Vec3d> PEARL_STARTS = new WeakHashMap<>();
    private static final Map<UUID, PendingConduit> CONDUIT_CHECKS = new HashMap<>();
    private static final Map<UUID, Boolean> STARVING_WHEN_EATING = new HashMap<>();
    private static final Map<UUID, List<TameableEntity>> PENDING_TAMES = new HashMap<>();
    private static final Map<UUID, PendingDolphin> PENDING_DOLPHINS = new HashMap<>();
    private static final Map<UUID, PendingFrost> PENDING_FROST = new HashMap<>();
    private static final Map<UUID, List<PendingGift>> PENDING_GIFTS = new HashMap<>();
    private static final Map<ItemEntity, Boolean> CAMPFIRE_COD = new WeakHashMap<>();
    private static final Map<ItemEntity, UUID> CAT_MORNING_GIFTS = new WeakHashMap<>();
    private static final Map<UUID, List<PendingPickup>> PENDING_CAT_PICKUPS = new HashMap<>();
    private static final Map<UUID, List<PendingPickup>> PENDING_CAMPFIRE_PICKUPS = new HashMap<>();
    private static final ThreadLocal<Boolean> SPAWNING_CAMPFIRE_COD = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<UUID> SPAWNING_CAT_GIFT = new ThreadLocal<>();
    private static final Map<AbstractFurnaceBlockEntity, FurnaceSnapshot> FURNACE_BEFORE = new WeakHashMap<>();
    private static final Map<ScreenHandler, ItemStack> FURNACE_SLOT_BEFORE = new WeakHashMap<>();
    private static final Map<ScreenHandler, Boolean> GRINDSTONE_TAKE_BEFORE = new WeakHashMap<>();
    private static final Map<ScreenHandler, LoomSnapshot> LOOM_BEFORE = new WeakHashMap<>();
    private static final Map<ServerWorld, List<HurtVillager>> HURT_VILLAGERS = new WeakHashMap<>();
    private static final Map<UUID, PendingBell> PENDING_BELLS = new HashMap<>();
    private static final Map<UUID, SlimeBounce> SLIME_BOUNCES = new HashMap<>();
    private static final Map<UUID, ItemFrameEntity> PENDING_MAP_ROOMS = new HashMap<>();
    private static final Map<ServerWorld, Map<BlockPos, PistonPush>> PISTON_PUSHES = new WeakHashMap<>();
    private static final Map<UUID, PendingInception> PENDING_INCEPTIONS = new HashMap<>();

    private record RailRide(UUID cart, Vec3d origin) {}
    private record PendingConduit(ServerWorld world, BlockPos pos, long expiresAt) {}
    private record PendingDolphin(DolphinEntity dolphin, long expiresAt) {}
    private record PendingFrost(ServerWorld world, BlockPos pos) {}
    private record PendingGift(ItemEntity item, int originalCount, UUID thrower, String achievement) {}
    private record PendingPickup(ItemEntity item, int originalCount) {}
    private record FurnaceSnapshot(ItemStack fuel, ItemStack output) {}
    private record LoomSnapshot(int banners, int dyes) {}
    private record HurtVillager(BlockPos pos, long tick) {}
    private record PendingBell(ServerWorld world, BlockPos pos) {}
    private record SlimeBounce(double originY, long expiresAt) {}
    private record PistonPush(BlockPos original, Direction direction, UUID player, long tick) {}
    private record PendingInception(ServerWorld world, BlockPos original, Direction direction, long expiresAt) {}

    private BedrockAchievements() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onItemUseStart);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onItemUseFinish);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onBlockPlaced);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onBlockBroken);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onEnderPearlTeleport);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onLogout);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onTrade);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onAnimalTame);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onEntityInteract);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onAnvilRepair);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onItemPickup);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onContainerOpen);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onItemSmelted);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onRightClickBlock);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onEffectAdded);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onPistonPre);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onPistonPost);
    }

    public static void inventoryOpened(ServerPlayerEntity player) {
        grant(player, "taking_inventory");
    }

    public static void raidStarted(ServerPlayerEntity player) {
        grant(player, "were_being_attacked");
    }

    public static void slimeBounce(Entity entity) {
        if (entity instanceof ServerPlayerEntity player && player.getVelocity().y > 0.0)
            SLIME_BOUNCES.put(player.getUuid(),
                    new SlimeBounce(player.getY(), player.getWorld().getTime() + 200));
    }

    public static void startCatMorningGift(PlayerEntity owner) {
        SPAWNING_CAT_GIFT.set(owner == null ? null : owner.getUuid());
    }

    public static void endCatMorningGift() {
        SPAWNING_CAT_GIFT.remove();
    }

    public static void hopperTransferred(Inventory from, Inventory to, ItemStack stack) {
        if (from instanceof ChestMinecartEntity
                && to instanceof HopperBlockEntity hopper
                && hopper.getWorld() instanceof ServerWorld world) {
            PlayerEntity nearby = world.getClosestPlayer(hopper.getPos().getX() + 0.5,
                    hopper.getPos().getY() + 0.5, hopper.getPos().getZ() + 0.5, 8, false);
            if (nearby instanceof ServerPlayerEntity player) {
                hopper.getPersistentData().putUuid("BedrockifyFreightOwner", player.getUuid());
                hopper.getPersistentData().putString("BedrockifyFreightItem",
                        Registries.ITEM.getId(stack.getItem()).toString());
                hopper.getPersistentData().putLong("BedrockifyFreightTime", world.getTime());
                hopper.markDirty();
            }
        } else if (from instanceof HopperBlockEntity hopper
                && to instanceof ChestBlockEntity
                && hopper.getWorld() instanceof ServerWorld world
                && hopper.getPersistentData().containsUuid("BedrockifyFreightOwner")
                && world.getTime() - hopper.getPersistentData().getLong("BedrockifyFreightTime") <= 1200
                && hopper.getPersistentData().getString("BedrockifyFreightItem")
                        .equals(Registries.ITEM.getId(stack.getItem()).toString())) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(
                    hopper.getPersistentData().getUuid("BedrockifyFreightOwner"));
            if (player != null) grant(player, "freight_station");
            hopper.getPersistentData().remove("BedrockifyFreightOwner");
            hopper.getPersistentData().remove("BedrockifyFreightItem");
            hopper.getPersistentData().remove("BedrockifyFreightTime");
            hopper.markDirty();
        }
    }

    public static void leatherArmorDyedInCauldron(PlayerEntity player, ItemStack original) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        int bit = original.isOf(Items.LEATHER_HELMET) ? 1
                : original.isOf(Items.LEATHER_CHESTPLATE) ? 2
                : original.isOf(Items.LEATHER_LEGGINGS) ? 4
                : original.isOf(Items.LEATHER_BOOTS) ? 8 : 0;
        if (bit == 0) return;
        NbtCompound playerData = serverPlayer.getPersistentData();
        NbtCompound persisted = playerData.getCompound("PlayerPersisted");
        int mask = persisted.getInt("BedrockifyCauldronDyedArmor") | bit;
        persisted.putInt("BedrockifyCauldronDyedArmor", mask);
        playerData.put("PlayerPersisted", persisted);
        if (mask == 15) grant(serverPlayer, "tie_dye_outfit");
    }

    public static void spawnCampfireOutput(World world, double x, double y, double z, net.minecraft.item.ItemStack stack) {
        boolean previous = SPAWNING_CAMPFIRE_COD.get();
        if (stack.isOf(Items.COOKED_COD)) SPAWNING_CAMPFIRE_COD.set(true);
        try {
            ItemScatterer.spawn(world, x, y, z, stack);
        } finally {
            SPAWNING_CAMPFIRE_COD.set(previous);
        }
    }

    public static void beforeFurnaceTick(AbstractFurnaceBlockEntity furnace) {
        FURNACE_BEFORE.put(furnace, new FurnaceSnapshot(
                furnace.getStack(1).copy(), furnace.getStack(2).copy()));
    }

    public static void beforeFurnaceSlotClick(ScreenHandler handler) {
        if (handler instanceof AbstractFurnaceScreenHandler furnaceScreen
                && furnaceScreen.getSlot(1).inventory instanceof AbstractFurnaceBlockEntity)
            FURNACE_SLOT_BEFORE.put(handler, furnaceScreen.getSlot(1).getStack().copy());
    }

    public static void beforeGrindstoneSlotClick(ScreenHandler handler, int slot) {
        if (!(handler instanceof GrindstoneScreenHandler) || slot != 2) return;
        boolean removesEnchantment = false;
        for (int input = 0; input < 2; input++) {
            for (var enchantment : EnchantmentHelper.get(handler.getSlot(input).getStack()).keySet()) {
                if (!enchantment.isCursed()) removesEnchantment = true;
            }
        }
        GRINDSTONE_TAKE_BEFORE.put(handler,
                removesEnchantment && !handler.getSlot(2).getStack().isEmpty());
    }

    public static void afterGrindstoneSlotClick(ScreenHandler handler, PlayerEntity player) {
        Boolean before = GRINDSTONE_TAKE_BEFORE.remove(handler);
        if (Boolean.TRUE.equals(before) && player instanceof ServerPlayerEntity serverPlayer
                && handler.getSlot(2).getStack().isEmpty())
            grant(serverPlayer, "disenchanted");
    }

    public static void beforeLoomSlotClick(ScreenHandler handler, int slot) {
        if (!(handler instanceof LoomScreenHandler) || slot != 3
                || !handler.getSlot(2).getStack().isOf(Items.MOJANG_BANNER_PATTERN)
                || handler.getSlot(3).getStack().isEmpty()) return;
        LOOM_BEFORE.put(handler, new LoomSnapshot(handler.getSlot(0).getStack().getCount(),
                handler.getSlot(1).getStack().getCount()));
    }

    public static void afterLoomSlotClick(ScreenHandler handler, PlayerEntity player) {
        LoomSnapshot before = LOOM_BEFORE.remove(handler);
        if (before != null && player instanceof ServerPlayerEntity serverPlayer
                && (handler.getSlot(0).getStack().getCount() < before.banners()
                || handler.getSlot(1).getStack().getCount() < before.dyes()))
            grant(serverPlayer, "fruit_on_the_loom");
    }

    public static void afterFurnaceSlotClick(ScreenHandler handler, PlayerEntity player) {
        ItemStack before = FURNACE_SLOT_BEFORE.remove(handler);
        if (before == null || !(player instanceof ServerPlayerEntity serverPlayer)
                || !(handler instanceof AbstractFurnaceScreenHandler furnaceScreen)
                || !(furnaceScreen.getSlot(1).inventory instanceof AbstractFurnaceBlockEntity furnace)) return;
        ItemStack after = furnaceScreen.getSlot(1).getStack();
        if (after.isOf(Items.DRIED_KELP_BLOCK)
                && (!before.isOf(Items.DRIED_KELP_BLOCK) || after.getCount() > before.getCount())) {
            furnace.getPersistentData().putUuid("BedrockifyManualKelpOwner", serverPlayer.getUuid());
            furnace.markDirty();
        } else if (!after.isOf(Items.DRIED_KELP_BLOCK)) {
            furnace.getPersistentData().remove("BedrockifyManualKelpOwner");
        }
    }

    public static void afterFurnaceTick(AbstractFurnaceBlockEntity furnace) {
        FurnaceSnapshot before = FURNACE_BEFORE.remove(furnace);
        if (before == null) return;
        ItemStack fuelAfter = furnace.getStack(1);
        if (!before.fuel().isEmpty() && (fuelAfter.getItem() != before.fuel().getItem()
                || fuelAfter.getCount() < before.fuel().getCount())) {
            String fuel = before.fuel().isOf(Items.CHARCOAL) ? "charcoal"
                    : before.fuel().isOf(Items.LAVA_BUCKET) ? "lava_bucket"
                    : before.fuel().isOf(Items.DRIED_KELP_BLOCK) ? "dried_kelp_block" : "other";
            furnace.getPersistentData().putString("BedrockifyLastFuel", fuel);
            if ("dried_kelp_block".equals(fuel)
                    && furnace.getPersistentData().containsUuid("BedrockifyManualKelpOwner")
                    && furnace.getWorld() instanceof ServerWorld serverWorld) {
                UUID owner = furnace.getPersistentData().getUuid("BedrockifyManualKelpOwner");
                ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(owner);
                if (player != null) grant(player, "alternative_fuel");
            }
            furnace.getPersistentData().remove("BedrockifyManualKelpOwner");
            furnace.markDirty();
        }
        ItemStack outputAfter = furnace.getStack(2);
        if (!outputAfter.isEmpty() && (before.output().getItem() != outputAfter.getItem()
                || outputAfter.getCount() > before.output().getCount())) {
            furnace.getPersistentData().putString("BedrockifyOutputFuel",
                    furnace.getPersistentData().getString("BedrockifyLastFuel"));
            furnace.markDirty();
        }
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity player)) return;
        List<TameableEntity> tames = PENDING_TAMES.remove(player.getUuid());
        if (tames != null) for (TameableEntity animal : tames) {
            if (!player.getUuid().equals(animal.getOwnerUuid())) continue;
            if (animal instanceof WolfEntity) {
                incrementPersisted(player, "BedrockifyWolvesTamed", 5, "leader_of_the_pack");
            } else if (animal instanceof CatEntity) {
                incrementPersisted(player, "BedrockifyCatsTamed", 20, "plethora_of_cats");
            }
        }
        PendingDolphin pendingDolphin = PENDING_DOLPHINS.get(player.getUuid());
        if (pendingDolphin != null) {
            DolphinEntity dolphin = pendingDolphin.dolphin();
            if (dolphin.hasFish() && !dolphin.getTreasurePos().equals(BlockPos.ORIGIN)) {
                grant(player, "echolocation");
                PENDING_DOLPHINS.remove(player.getUuid());
            } else if (!dolphin.isAlive() || player.getWorld().getTime() >= pendingDolphin.expiresAt()) {
                PENDING_DOLPHINS.remove(player.getUuid());
            }
        }
        PendingFrost frost = PENDING_FROST.remove(player.getUuid());
        if (frost != null && frost.world().getBlockState(frost.pos()).isOf(Blocks.FROSTED_ICE))
            grant(player, "let_it_go");
        ItemFrameEntity mapFrame = PENDING_MAP_ROOMS.remove(player.getUuid());
        if (mapFrame != null && !mapFrame.isRemoved() && isCompleteMapRoom(player.getServerWorld(), mapFrame))
            grant(player, "map_room");
        PendingInception inception = PENDING_INCEPTIONS.get(player.getUuid());
        if (inception != null) {
            BlockPos pulled = inception.original().offset(inception.direction());
            if ((inception.world().getBlockState(pulled).isOf(Blocks.PISTON)
                    || inception.world().getBlockState(pulled).isOf(Blocks.STICKY_PISTON))
                    && inception.world().getBlockState(inception.original()).isAir()) {
                grant(player, "inception");
                PENDING_INCEPTIONS.remove(player.getUuid());
            } else if (inception.world().getTime() >= inception.expiresAt()) {
                PENDING_INCEPTIONS.remove(player.getUuid());
            }
        }
        PendingBell bell = PENDING_BELLS.remove(player.getUuid());
        if (bell != null && bell.world().getBlockEntity(bell.pos()) instanceof BellBlockEntity bellEntity
                && bellEntity.ringing && bell.world().isNearOccupiedPointOfInterest(bell.pos())) {
            List<HurtVillager> hurt = HURT_VILLAGERS.get(bell.world());
            if (hurt != null && hurt.stream().anyMatch(entry ->
                    bell.world().getTime() - entry.tick() <= 1200
                            && entry.pos().isWithinDistance(bell.pos(), 32)))
                grant(player, "sound_the_alarm");
        }
        SlimeBounce bounce = SLIME_BOUNCES.get(player.getUuid());
        if (bounce != null) {
            if (player.getY() - bounce.originY() >= 30.0) {
                grant(player, "trampoline");
                SLIME_BOUNCES.remove(player.getUuid());
            } else if (player.getWorld().getTime() >= bounce.expiresAt() || player.isOnGround()) {
                SLIME_BOUNCES.remove(player.getUuid());
            }
        }
        List<PendingGift> gifts = PENDING_GIFTS.remove(player.getUuid());
        if (gifts != null) for (PendingGift gift : gifts) {
            if (!gift.item().isRemoved() && gift.item().getStack().getCount() >= gift.originalCount())
                continue;
            ServerPlayerEntity thrower = player.server.getPlayerManager().getPlayer(gift.thrower());
            if (thrower != null && thrower != player) grant(thrower, gift.achievement());
        }
        List<PendingPickup> campfirePickups = PENDING_CAMPFIRE_PICKUPS.remove(player.getUuid());
        if (campfirePickups != null) for (PendingPickup pickup : campfirePickups) {
            if (pickup.item().isRemoved() || pickup.item().getStack().getCount() < pickup.originalCount())
                grant(player, "delicious_fish");
        }
        List<PendingPickup> catPickups = PENDING_CAT_PICKUPS.remove(player.getUuid());
        if (catPickups != null) for (PendingPickup pickup : catPickups) {
            if (pickup.item().isRemoved() || pickup.item().getStack().getCount() < pickup.originalCount())
                grant(player, "where_have_you_been");
        }
        if (player.isUsingRiptide()) grant(player, "do_a_barrel_roll");
        if (player.isFallFlying() && player.getVelocity().lengthSquared() > 4.0
                && fliesThroughOneBlockOpening(player))
            grant(player, "super_sonic");
        if (player.age % 20 == 0) {
            int caravan = 0;
            for (LlamaEntity llama : player.getServerWorld().getEntitiesByClass(LlamaEntity.class,
                    player.getBoundingBox().expand(32), entity -> true)) {
                LlamaEntity head = llama;
                for (int depth = 0; depth < 10 && head.isFollowing(); depth++)
                    head = head.getFollowing();
                if (head.isLeashed() && head.getHoldingEntity() == player) caravan++;
            }
            if (caravan >= 5) grant(player, "so_i_got_that_going_for_me");
            checkExplorerMap(player);
            if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.IRON_HELMET)
                    && player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.IRON_CHESTPLATE)
                    && player.getEquippedStack(EquipmentSlot.LEGS).isOf(Items.IRON_LEGGINGS)
                    && player.getEquippedStack(EquipmentSlot.FEET).isOf(Items.IRON_BOOTS))
                grant(player, "iron_man");
            if (player.getStatHandler().getStat(Stats.CUSTOM, Stats.PLAY_TIME) >= ONE_HUNDRED_DAYS)
                grant(player, "passing_the_time");
            NbtCompound castaway = player.getPersistentData().getCompound("PlayerPersisted");
            long kelpStarted = castaway.getLong("BedrockifyKelpOnlySince");
            if (castaway.contains("BedrockifyKelpOnlySince")
                    && player.getWorld().getTime() - kelpStarted >= 3L * GAME_DAY)
                grant(player, "castaway");
        }

        if (player.isSubmergedInWater()) {
            int ticks = SUBMERGED_TICKS.getOrDefault(player.getUuid(), 0) + 1;
            SUBMERGED_TICKS.put(player.getUuid(), ticks);
            if (ticks >= TWO_MINUTES) grant(player, "free_diver");
            if (ticks >= GAME_DAY) grant(player, "sleep_with_the_fishes");
        } else {
            SUBMERGED_TICKS.remove(player.getUuid());
        }

        if (player.getVehicle() instanceof AbstractMinecartEntity cart) {
            RailRide ride = RAIL_RIDES.get(player.getUuid());
            if (ride == null || !ride.cart().equals(cart.getUuid())) {
                ride = new RailRide(cart.getUuid(), player.getPos());
                RAIL_RIDES.put(player.getUuid(), ride);
            }
            double dx = player.getX() - ride.origin().x;
            double dz = player.getZ() - ride.origin().z;
            if (dx * dx + dz * dz >= 500.0 * 500.0) grant(player, "on_a_rail");
        } else {
            RAIL_RIDES.remove(player.getUuid());
        }

        PendingConduit conduit = CONDUIT_CHECKS.get(player.getUuid());
        if (conduit != null) {
            if (conduit.world().getBlockEntity(conduit.pos()) instanceof ConduitBlockEntity blockEntity
                    && blockEntity.isActive()) {
                grant(player, "moskstraumen");
                CONDUIT_CHECKS.remove(player.getUuid());
            } else if (conduit.world().getTime() >= conduit.expiresAt()) {
                CONDUIT_CHECKS.remove(player.getUuid());
            }
        }
    }

    private static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().getWorld().isClient) return;
        if (event.getEntity() instanceof VillagerEntity villager
                && villager.getWorld() instanceof ServerWorld world && event.getAmount() > 0) {
            List<HurtVillager> hurt = HURT_VILLAGERS.computeIfAbsent(world, ignored -> new ArrayList<>());
            hurt.removeIf(entry -> world.getTime() - entry.tick() > 1200);
            hurt.add(new HurtVillager(villager.getBlockPos(), world.getTime()));
        }
        if (event.getSource().getAttacker() instanceof ServerPlayerEntity attacker
                && event.getAmount() >= 18.0F)
            grant(attacker, "overkill");
        if (event.getEntity() instanceof PigEntity pig
                && event.getSource().isOf(DamageTypes.FALL)
                && pig.getFirstPassenger() instanceof ServerPlayerEntity rider)
            grant(rider, "when_pigs_fly");
    }

    private static boolean fliesThroughOneBlockOpening(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos center = player.getBlockPos();
        if (!world.getBlockState(center).isAir()) return false;
        Vec3d speed = player.getVelocity();
        double x = Math.abs(speed.x), y = Math.abs(speed.y), z = Math.abs(speed.z);
        if (x >= y && x >= z)
            return enclosedByFullBlocks(world, center.up(), center.down(),
                    center.north(), center.south());
        if (z >= x && z >= y)
            return enclosedByFullBlocks(world, center.up(), center.down(),
                    center.east(), center.west());
        return enclosedByFullBlocks(world, center.north(), center.south(),
                center.east(), center.west());
    }

    private static boolean enclosedByFullBlocks(ServerWorld world, BlockPos a, BlockPos b,
                                                BlockPos c, BlockPos d) {
        return world.getBlockState(a).isOpaqueFullCube(world, a)
                && world.getBlockState(b).isOpaqueFullCube(world, b)
                && world.getBlockState(c).isOpaqueFullCube(world, c)
                && world.getBlockState(d).isOpaqueFullCube(world, d);
    }

    private static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            STARVING_WHEN_EATING.put(player.getUuid(),
                    event.getItem().isOf(Items.ROTTEN_FLESH)
                            && player.getHungerManager().getFoodLevel() == 0);
        }
    }

    private static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            if (event.getItem().isOf(Items.ROTTEN_FLESH)
                    && STARVING_WHEN_EATING.getOrDefault(player.getUuid(), false))
                grant(player, "iron_belly");
            if (event.getItem().isFood()) {
                NbtCompound playerData = player.getPersistentData();
                NbtCompound persisted = playerData.getCompound("PlayerPersisted");
                if (event.getItem().isOf(Items.DRIED_KELP)) {
                    if (!persisted.contains("BedrockifyKelpOnlySince"))
                        persisted.putLong("BedrockifyKelpOnlySince", player.getWorld().getTime());
                } else {
                    persisted.remove("BedrockifyKelpOnlySince");
                }
                playerData.put("PlayerPersisted", persisted);
            }
        }
        STARVING_WHEN_EATING.remove(event.getEntity().getUuid());
    }

    private static void onTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        var offer = event.getMerchantOffer();
        if (event.getAbstractVillager() instanceof VillagerEntity villager
                && villager.getVillagerData().getProfession() == VillagerProfession.CARTOGRAPHER)
            recordExplorerMap(player, offer.getSellItem());
        int earned = offer.getSellItem().isOf(Items.EMERALD) ? offer.getSellItem().getCount() : 0;
        int spent = (offer.getAdjustedFirstBuyItem().isOf(Items.EMERALD)
                ? offer.getAdjustedFirstBuyItem().getCount() : 0)
                + (offer.getSecondBuyItem().isOf(Items.EMERALD)
                ? offer.getSecondBuyItem().getCount() : 0);
        if ((offer.getAdjustedFirstBuyItem().isOf(Items.EMERALD)
                && offer.getAdjustedFirstBuyItem().getCount() == 1
                && offer.getSecondBuyItem().isEmpty())
                || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE))
            grant(player, "buy_low_sell_high");
        NbtCompound playerData = player.getPersistentData();
        NbtCompound persisted = playerData.getCompound("PlayerPersisted");
        if (earned > 0) {
            int total = Math.min(1000, persisted.getInt("BedrockifyEmeraldsEarned") + earned);
            persisted.putInt("BedrockifyEmeraldsEarned", total);
            if (total >= 1000) grant(player, "master_trader");
        }
        if (earned + spent > 0) {
            int total = Math.min(30, persisted.getInt("BedrockifyEmeraldsTraded") + earned + spent);
            persisted.putInt("BedrockifyEmeraldsTraded", total);
            if (total >= 30) grant(player, "the_haggler");
        }
        playerData.put("PlayerPersisted", persisted);
    }

    private static void recordExplorerMap(ServerPlayerEntity player, ItemStack sold) {
        Integer id = sold.isOf(Items.FILLED_MAP) ? FilledMapItem.getMapId(sold) : null;
        if (id == null || !sold.hasNbt()) return;
        NbtList decorations = sold.getNbt().getList("Decorations", NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < decorations.size(); index++) {
            NbtCompound decoration = decorations.getCompound(index);
            byte type = decoration.getByte("type");
            if (type != MapIcon.Type.MONUMENT.getId() && type != MapIcon.Type.MANSION.getId()) continue;
            NbtCompound playerData = player.getPersistentData();
            NbtCompound persisted = playerData.getCompound("PlayerPersisted");
            NbtList maps = persisted.getList("BedrockifyExplorerMaps", NbtElement.COMPOUND_TYPE);
            for (int existing = 0; existing < maps.size(); existing++)
                if (maps.getCompound(existing).getInt("id") == id) return;
            NbtCompound map = new NbtCompound();
            map.putInt("id", id);
            map.putDouble("x", decoration.getDouble("x"));
            map.putDouble("z", decoration.getDouble("z"));
            map.putByte("type", type);
            map.putString("dimension", player.getWorld().getRegistryKey().getValue().toString());
            maps.add(map);
            persisted.put("BedrockifyExplorerMaps", maps);
            playerData.put("PlayerPersisted", persisted);
            return;
        }
    }

    private static void checkExplorerMap(ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        Integer id = held.isOf(Items.FILLED_MAP) ? FilledMapItem.getMapId(held) : null;
        if (id == null) return;
        NbtList maps = player.getPersistentData().getCompound("PlayerPersisted")
                .getList("BedrockifyExplorerMaps", NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < maps.size(); index++) {
            NbtCompound map = maps.getCompound(index);
            if (map.getInt("id") != id
                    || !map.getString("dimension").equals(player.getWorld().getRegistryKey().getValue().toString()))
                continue;
            double dx = player.getX() - map.getDouble("x");
            double dz = player.getZ() - map.getDouble("z");
            if (dx * dx + dz * dz > 128.0 * 128.0) continue;
            var tag = map.getByte("type") == MapIcon.Type.MONUMENT.getId()
                    ? StructureTags.ON_OCEAN_EXPLORER_MAPS : StructureTags.ON_WOODLAND_EXPLORER_MAPS;
            if (player.getServerWorld().getStructureAccessor()
                    .getStructureContaining(player.getBlockPos(), tag).hasChildren()) {
                grant(player, "treasure_hunter");
                return;
            }
        }
    }

    private static void onAnimalTame(AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayerEntity player)
                || !(event.getAnimal() instanceof TameableEntity animal)) return;
        if (animal instanceof WolfEntity || animal instanceof CatEntity)
            PENDING_TAMES.computeIfAbsent(player.getUuid(), ignored -> new ArrayList<>()).add(animal);
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        if (event.getTarget() instanceof ItemFrameEntity frame
                && event.getItemStack().isOf(Items.FILLED_MAP))
            PENDING_MAP_ROOMS.put(player.getUuid(), frame);
        if (!(event.getTarget() instanceof DolphinEntity dolphin)) return;
        if (event.getItemStack().isOf(Items.COD) || event.getItemStack().isOf(Items.SALMON)
                || event.getItemStack().isOf(Items.TROPICAL_FISH)
                || event.getItemStack().isOf(Items.PUFFERFISH))
            PENDING_DOLPHINS.put(player.getUuid(),
                    new PendingDolphin(dolphin, player.getWorld().getTime() + 120));
    }

    private static boolean isCompleteMapRoom(ServerWorld world, ItemFrameEntity changed) {
        List<ItemFrameEntity> frames = world.getEntitiesByClass(ItemFrameEntity.class,
                changed.getBoundingBox().expand(3.5), frame ->
                        frame.getHorizontalFacing() == changed.getHorizontalFacing()
                                && frame.getHeldItemStack().isOf(Items.FILLED_MAP));
        Map<BlockPos, ItemFrameEntity> byPosition = new HashMap<>();
        for (ItemFrameEntity frame : frames)
            byPosition.put(frame.getDecorationBlockPos(), frame);
        Direction facing = changed.getHorizontalFacing();
        Direction across = facing.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        Direction up = facing.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
        for (ItemFrameEntity corner : frames) {
            Set<Integer> centersX = new HashSet<>();
            Set<Integer> centersZ = new HashSet<>();
            Set<Long> centers = new HashSet<>();
            Integer scale = null;
            boolean complete = true;
            BlockPos origin = corner.getDecorationBlockPos();
            for (int row = 0; row < 3 && complete; row++) for (int col = 0; col < 3; col++) {
                ItemFrameEntity frame = byPosition.get(origin.offset(across, col).offset(up, row));
                MapState state = frame == null ? null : FilledMapItem.getMapState(frame.getHeldItemStack(), world);
                if (state == null || (scale != null && scale != state.scale)) {
                    complete = false;
                    break;
                }
                for (byte color : state.colors) if (color == 0) {
                    complete = false;
                    break;
                }
                if (!complete) break;
                scale = (int) state.scale;
                centersX.add(state.centerX);
                centersZ.add(state.centerZ);
                centers.add(((long) state.centerX << 32) ^ (state.centerZ & 0xffffffffL));
            }
            if (complete && centers.size() == 9 && centersX.size() == 3 && centersZ.size() == 3) {
                List<Integer> xs = centersX.stream().sorted().toList();
                List<Integer> zs = centersZ.stream().sorted().toList();
                int width = 128 << scale;
                if (xs.get(1) - xs.get(0) == width && xs.get(2) - xs.get(1) == width
                        && zs.get(1) - zs.get(0) == width && zs.get(2) - zs.get(1) == width)
                    return true;
            }
        }
        return false;
    }

    private static void onAnvilRepair(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)
                || !(event.getLeft().getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof ShulkerBoxBlock)
                || !event.getOutput().hasCustomName()) return;
        if (!event.getOutput().getName().getString().equals(event.getLeft().getName().getString()))
            grant(player, "organizational_wizard");
    }

    private static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity receiver)) return;
        ItemEntity item = event.getItem();
        if (CAMPFIRE_COD.containsKey(item))
            PENDING_CAMPFIRE_PICKUPS.computeIfAbsent(receiver.getUuid(), ignored -> new ArrayList<>())
                    .add(new PendingPickup(item, item.getStack().getCount()));
        if (receiver.getUuid().equals(CAT_MORNING_GIFTS.get(item)))
            PENDING_CAT_PICKUPS.computeIfAbsent(receiver.getUuid(), ignored -> new ArrayList<>())
                    .add(new PendingPickup(item, item.getStack().getCount()));
        String achievement = item.getStack().isOf(Items.DIAMOND) ? "diamonds_to_you"
                : item.getStack().isOf(Items.SUSPICIOUS_STEW) ? "time_for_stew" : null;
        if (achievement == null) return;
        NbtCompound itemData = item.writeNbt(new NbtCompound());
        if (!itemData.containsUuid("Thrower")) return;
        UUID thrower = itemData.getUuid("Thrower");
        if (receiver.getUuid().equals(thrower)) return;
        PENDING_GIFTS.computeIfAbsent(receiver.getUuid(), ignored -> new ArrayList<>())
                .add(new PendingGift(item, item.getStack().getCount(), thrower, achievement));
    }

    private static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)
                || !(event.getContainer() instanceof GenericContainerScreenHandler container)) return;
        Inventory inventory = container.getInventory();
        if (!(inventory instanceof ChestBlockEntity) && !(inventory instanceof DoubleInventory)) return;
        int cobblestone = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStack(slot).isOf(Items.COBBLESTONE))
                cobblestone += inventory.getStack(slot).getCount();
        }
        if (cobblestone >= 1728) grant(player, "chestful_of_cobblestone");
    }

    private static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        if (event.getSmelting().isOf(Items.SPONGE)) grant(player, "dry_spell");
        if (event.getSmelting().isOf(Items.COOKED_COD)) grant(player, "delicious_fish");
        if (player.currentScreenHandler instanceof AbstractFurnaceScreenHandler furnaceScreen
                && furnaceScreen.getSlot(0).inventory instanceof AbstractFurnaceBlockEntity furnace) {
            String fuel = furnace.getPersistentData().getString("BedrockifyOutputFuel");
            if ("lava_bucket".equals(fuel)) grant(player, "super_fuel");
            if ("charcoal".equals(fuel) && event.getSmelting().isOf(Items.CHARCOAL))
                grant(player, "renewable_energy");
        }
    }

    private static void incrementPersisted(ServerPlayerEntity player, String key, int target, String achievement) {
        NbtCompound playerData = player.getPersistentData();
        NbtCompound persisted = playerData.getCompound("PlayerPersisted");
        int total = Math.min(target, persisted.getInt(key) + 1);
        persisted.putInt(key, total);
        playerData.put("PlayerPersisted", persisted);
        if (total >= target) grant(player, achievement);
    }

    private static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)
                || !(event.getLevel() instanceof ServerWorld world)) return;
        if (world.getBlockState(event.getPos()).isOf(Blocks.WATER)
                && world.getBiome(event.getPos()).isIn(BiomeTags.IS_DEEP_OCEAN)
                && EnchantmentHelper.getLevel(Enchantments.FROST_WALKER,
                player.getEquippedStack(EquipmentSlot.FEET)) > 0)
            PENDING_FROST.put(player.getUuid(), new PendingFrost(world, event.getPos()));
        if (event.getPlacedBlock().isOf(Blocks.FURNACE)
                || event.getPlacedBlock().isOf(Blocks.HOPPER)
                || event.getPlacedBlock().isOf(Blocks.CHEST)) {
            BlockPos placed = event.getPos();
            for (BlockPos candidate : BlockPos.iterate(placed.add(-3, -2, -3), placed.add(3, 2, 3))) {
                if (isSmeltEverythingRig(world, candidate)) {
                    grant(player, "smelt_everything");
                    break;
                }
            }
        }
        if (event.getPlacedBlock().isOf(Blocks.SCAFFOLDING)
                && event.getPos().getY() == world.getTopY() - 1)
            grant(player, "top_of_the_world");
        if (event.getPlacedBlock().isOf(Blocks.BOOKSHELF)) {
            for (BlockPos offset : EnchantingTableBlock.POWER_PROVIDER_OFFSETS) {
                BlockPos table = event.getPos().subtract(offset);
                if (world.getBlockState(table).isOf(Blocks.ENCHANTING_TABLE)
                        && EnchantingTableBlock.canAccessPowerProvider(world, table, offset)) {
                    grant(player, "librarian");
                    break;
                }
            }
        }
        if (event.getPlacedBlock().isOf(Blocks.CONDUIT)
                || event.getPlacedBlock().isOf(Blocks.PRISMARINE)
                || event.getPlacedBlock().isOf(Blocks.PRISMARINE_BRICKS)
                || event.getPlacedBlock().isOf(Blocks.DARK_PRISMARINE)
                || event.getPlacedBlock().isOf(Blocks.SEA_LANTERN)) {
            BlockPos placed = event.getPos();
            for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
                BlockPos candidate = placed.add(x, y, z);
                if (world.getBlockEntity(candidate) instanceof ConduitBlockEntity conduit && !conduit.isActive()) {
                    CONDUIT_CHECKS.put(player.getUuid(), new PendingConduit(world, candidate, world.getTime() + 120));
                    return;
                }
            }
        }
    }

    private static boolean isSmeltEverythingRig(ServerWorld world, BlockPos furnace) {
        if (!world.getBlockState(furnace).isOf(Blocks.FURNACE)) return false;
        BlockPos input = furnace.up();
        BlockPos output = furnace.down();
        if (!world.getBlockState(input).isOf(Blocks.HOPPER)
                || world.getBlockState(input).get(HopperBlock.FACING) != Direction.DOWN
                || !world.getBlockState(input.up()).isOf(Blocks.CHEST)
                || !world.getBlockState(output).isOf(Blocks.HOPPER)) return false;
        Direction outputDirection = world.getBlockState(output).get(HopperBlock.FACING);
        if (outputDirection == Direction.DOWN
                || !world.getBlockState(output.offset(outputDirection)).isOf(Blocks.CHEST)) return false;
        for (Direction side : Direction.Type.HORIZONTAL) {
            BlockPos fuel = furnace.offset(side);
            if (world.getBlockState(fuel).isOf(Blocks.HOPPER)
                    && world.getBlockState(fuel).get(HopperBlock.FACING) == side.getOpposite()
                    && world.getBlockState(fuel.up()).isOf(Blocks.CHEST)) return true;
        }
        return false;
    }

    private static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity player
                && event.getState().isIn(BlockTags.LOGS_THAT_BURN))
            grant(player, "getting_wood");
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SUBMERGED_TICKS.remove(event.getEntity().getUuid());
        RAIL_RIDES.remove(event.getEntity().getUuid());
        CONDUIT_CHECKS.remove(event.getEntity().getUuid());
        STARVING_WHEN_EATING.remove(event.getEntity().getUuid());
        PENDING_TAMES.remove(event.getEntity().getUuid());
        PENDING_DOLPHINS.remove(event.getEntity().getUuid());
        PENDING_FROST.remove(event.getEntity().getUuid());
        PENDING_GIFTS.remove(event.getEntity().getUuid());
        PENDING_CAMPFIRE_PICKUPS.remove(event.getEntity().getUuid());
        PENDING_CAT_PICKUPS.remove(event.getEntity().getUuid());
        PENDING_BELLS.remove(event.getEntity().getUuid());
        SLIME_BOUNCES.remove(event.getEntity().getUuid());
        PENDING_MAP_ROOMS.remove(event.getEntity().getUuid());
        PENDING_INCEPTIONS.remove(event.getEntity().getUuid());
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayerEntity player
                && event.getLevel() instanceof ServerWorld world
                && world.getBlockState(event.getPos()).isOf(Blocks.BELL))
            PENDING_BELLS.put(player.getUuid(), new PendingBell(world, event.getPos().toImmutable()));
    }

    private static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof WitchEntity)
                || event.getEffectInstance().getEffectType() != StatusEffects.POISON
                || !(event.getEffectSource() instanceof PotionEntity potion)
                || !(potion.getOwner() instanceof ServerPlayerEntity player)) return;
        grant(player, "taste_of_your_own_medicine");
    }

    private static void onPistonPre(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerWorld world)
                || event.getPistonMoveType() != PistonEvent.PistonMoveType.EXTEND) return;
        Direction direction = event.getDirection();
        BlockPos front = event.getPos().offset(direction);
        if (!world.getBlockState(front).isOf(Blocks.STICKY_PISTON)
                || world.getBlockState(front).get(PistonBlock.FACING) != direction.getOpposite()) return;
        var helper = event.getStructureHelper();
        if (helper == null || !helper.calculatePush() || !helper.getMovedBlocks().contains(front)) return;
        PlayerEntity nearby = world.getClosestPlayer(event.getPos().getX() + 0.5,
                event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, 8, false);
        if (nearby instanceof ServerPlayerEntity player)
            PISTON_PUSHES.computeIfAbsent(world, ignored -> new HashMap<>())
                    .put(event.getPos().offset(direction, 2),
                            new PistonPush(event.getPos().toImmutable(), direction, player.getUuid(), world.getTime()));
    }

    private static void onPistonPost(PistonEvent.Post event) {
        if (!(event.getLevel() instanceof ServerWorld world)
                || event.getPistonMoveType() != PistonEvent.PistonMoveType.RETRACT) return;
        Map<BlockPos, PistonPush> pushes = PISTON_PUSHES.get(world);
        if (pushes == null) return;
        PistonPush push = pushes.remove(event.getPos());
        if (push == null || world.getTime() - push.tick() > 1200
                || event.getDirection() != push.direction().getOpposite()) return;
        if (world.getServer().getPlayerManager().getPlayer(push.player()) != null)
            PENDING_INCEPTIONS.put(push.player(),
                    new PendingInception(world, push.original(), push.direction(), world.getTime() + 10));
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof EnderPearlEntity pearl && !pearl.getWorld().isClient)
            PEARL_STARTS.put(pearl, pearl.getPos());
        if (SPAWNING_CAMPFIRE_COD.get() && event.getEntity() instanceof ItemEntity item
                && item.getStack().isOf(Items.COOKED_COD))
            CAMPFIRE_COD.put(item, true);
        if (SPAWNING_CAT_GIFT.get() != null && event.getEntity() instanceof ItemEntity item)
            CAT_MORNING_GIFTS.put(item, SPAWNING_CAT_GIFT.get());
    }

    private static void onEnderPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        Vec3d origin = PEARL_STARTS.remove(event.getPearlEntity());
        if (origin == null) return;
        if (origin.squaredDistanceTo(event.getTargetX(), event.getTargetY(), event.getTargetZ()) > 100.0 * 100.0)
            grant(event.getPlayer(), "beam_me_up");
    }

    private static void grant(ServerPlayerEntity player, String path) {
        Advancement root = player.server.getAdvancementLoader()
                .get(new Identifier("bedrockify", "bedrock/root"));
        if (root != null && !player.getAdvancementTracker().getProgress(root).isDone())
            player.getAdvancementTracker().grantCriterion(root, "opened");
        Advancement advancement = player.server.getAdvancementLoader()
                .get(new Identifier("bedrockify", "bedrock/" + path));
        if (advancement != null && !player.getAdvancementTracker().getProgress(advancement).isDone())
            player.getAdvancementTracker().grantCriterion(advancement, "done");
    }
}

