package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.block.Blocks;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.grant;

/** Native container checks and their before/after snapshots. Persisted keys stay unchanged. */
final class ContainerAchievements {
    private static final Map<AbstractFurnaceBlockEntity, FurnaceSnapshot> FURNACE_BEFORE = new WeakHashMap<>();
    private static final Map<ScreenHandler, ItemStack> FURNACE_SLOT_BEFORE = new WeakHashMap<>();
    private static final Map<ScreenHandler, Boolean> GRINDSTONE_TAKE_BEFORE = new WeakHashMap<>();
    private static final Map<ScreenHandler, LoomSnapshot> LOOM_BEFORE = new WeakHashMap<>();
    private record FurnaceSnapshot(ItemStack fuel, ItemStack output) {}
    private record LoomSnapshot(int banners, int dyes) {}

    private ContainerAchievements() {}

    static void register() {
        MinecraftForge.EVENT_BUS.addListener(ContainerAchievements::onContainerOpen);
        MinecraftForge.EVENT_BUS.addListener(ContainerAchievements::onItemSmelted);
        MinecraftForge.EVENT_BUS.addListener(ContainerAchievements::onAnvilRepair);
        MinecraftForge.EVENT_BUS.addListener(ContainerAchievements::onBlockPlaced);
    }

    static void clear() {
        FURNACE_BEFORE.clear();
        FURNACE_SLOT_BEFORE.clear();
        GRINDSTONE_TAKE_BEFORE.clear();
        LOOM_BEFORE.clear();
    }

    static void hopperTransferred(Inventory from, Inventory to, ItemStack stack) {
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

    static void beforeFurnaceTick(AbstractFurnaceBlockEntity furnace) {
        FURNACE_BEFORE.put(furnace, new FurnaceSnapshot(
                furnace.getStack(1).copy(), furnace.getStack(2).copy()));
    }

    static void afterFurnaceTick(AbstractFurnaceBlockEntity furnace) {
        FurnaceSnapshot before = FURNACE_BEFORE.remove(furnace);
        if (before == null) return;
        ItemStack fuelAfter = furnace.getStack(1);
        if (consumedFuel(before.fuel(), fuelAfter)) {
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

    private static boolean consumedFuel(ItemStack before, ItemStack after) {
        if (before.isEmpty() || ItemStack.areEqual(before, after)) return false;
        // Wet sponge fills an empty bucket; that replacement must not overwrite the burning fuel's provenance.
        return after.isEmpty()
                || after.isOf(before.getItem()) && after.getCount() < before.getCount()
                || before.hasCraftingRemainingItem() && ItemStack.canCombine(after, before.getCraftingRemainingItem());
    }

    static void beforeFurnaceSlotClick(ScreenHandler handler) {
        if (handler instanceof AbstractFurnaceScreenHandler furnaceScreen
                && furnaceScreen.getSlot(1).inventory instanceof AbstractFurnaceBlockEntity)
            FURNACE_SLOT_BEFORE.put(handler, furnaceScreen.getSlot(1).getStack().copy());
    }

    static void afterFurnaceSlotClick(ScreenHandler handler, PlayerEntity player) {
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

    static void beforeGrindstoneSlotClick(ScreenHandler handler, int slot) {
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

    static void afterGrindstoneSlotClick(ScreenHandler handler, PlayerEntity player) {
        Boolean before = GRINDSTONE_TAKE_BEFORE.remove(handler);
        if (Boolean.TRUE.equals(before) && player instanceof ServerPlayerEntity serverPlayer
                && handler.getSlot(2).getStack().isEmpty())
            grant(serverPlayer, "disenchanted");
    }

    static void beforeLoomSlotClick(ScreenHandler handler, int slot) {
        if (!(handler instanceof LoomScreenHandler) || slot != 3
                || !handler.getSlot(2).getStack().isOf(Items.MOJANG_BANNER_PATTERN)
                || handler.getSlot(3).getStack().isEmpty()) return;
        LOOM_BEFORE.put(handler, new LoomSnapshot(handler.getSlot(0).getStack().getCount(),
                handler.getSlot(1).getStack().getCount()));
    }

    static void afterLoomSlotClick(ScreenHandler handler, PlayerEntity player) {
        LoomSnapshot before = LOOM_BEFORE.remove(handler);
        if (before != null && player instanceof ServerPlayerEntity serverPlayer
                && (handler.getSlot(0).getStack().getCount() < before.banners()
                || handler.getSlot(1).getStack().getCount() < before.dyes()))
            grant(serverPlayer, "fruit_on_the_loom");
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

    private static void onAnvilRepair(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)
                || !(event.getLeft().getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof ShulkerBoxBlock)
                || !event.getOutput().hasCustomName()) return;
        if (!event.getOutput().getName().getString().equals(event.getLeft().getName().getString()))
            grant(player, "organizational_wizard");
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

    private static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)
                || !(event.getLevel() instanceof ServerWorld world)) return;
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
    }
}
