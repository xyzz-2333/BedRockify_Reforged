package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.block.Blocks;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.entity.ConduitBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.PistonEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.grant;

/** Block interactions and deferred world-state confirmation. */
final class WorldInteractionAchievements {
    private static final Map<UUID, State> PLAYERS = new HashMap<>();
    private static final Map<ServerWorld, Map<BlockPos, PistonPush>> PISTON_PUSHES = new WeakHashMap<>();
    private record PendingConduit(ServerWorld world, BlockPos pos, long expiresAt) {}
    private record PendingFrost(ServerWorld world, BlockPos pos) {}
    private record PistonPush(BlockPos original, Direction direction, UUID player, long tick) {}
    private record PendingInception(ServerWorld world, BlockPos original, Direction direction, long expiresAt) {}
    private static final class State {
        PendingFrost frost;
        PendingConduit conduit;
        PendingInception inception;
        boolean isEmpty() { return frost == null && conduit == null && inception == null; }
    }
    private WorldInteractionAchievements() {}
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(WorldInteractionAchievements::onBlockPlaced);
        MinecraftForge.EVENT_BUS.addListener(WorldInteractionAchievements::onBlockBroken);
        MinecraftForge.EVENT_BUS.addListener(WorldInteractionAchievements::onPistonPre);
        MinecraftForge.EVENT_BUS.addListener(WorldInteractionAchievements::onPistonPost);
    }
    static void clear() { PLAYERS.clear(); PISTON_PUSHES.clear(); }
    static void logout(UUID player) { PLAYERS.remove(player); }
    private static State pending(UUID player) { return PLAYERS.computeIfAbsent(player, ignored -> new State()); }
    static void tick(ServerPlayerEntity player) {
        State state = PLAYERS.get(player.getUuid());
        if (state == null) return;
        PendingFrost frost = state.frost;
        state.frost = null;
        if (frost != null && frost.world().getBlockState(frost.pos()).isOf(Blocks.FROSTED_ICE))
            grant(player, "let_it_go");
        PendingInception inception = state.inception;
        if (inception != null) {
            BlockPos pulled = inception.original().offset(inception.direction());
            if ((inception.world().getBlockState(pulled).isOf(Blocks.PISTON)
                    || inception.world().getBlockState(pulled).isOf(Blocks.STICKY_PISTON))
                    && inception.world().getBlockState(inception.original()).isAir()) {
                grant(player, "inception");
                state.inception = null;
            } else if (inception.world().getTime() >= inception.expiresAt()) {
                state.inception = null;
            }
        }
        PendingConduit conduit = state.conduit;
        if (conduit != null) {
            if (conduit.world().getBlockEntity(conduit.pos()) instanceof ConduitBlockEntity blockEntity
                    && blockEntity.isActive()) {
                grant(player, "moskstraumen");
                state.conduit = null;
            } else if (conduit.world().getTime() >= conduit.expiresAt()) {
                state.conduit = null;
            }
        }
        if (state.isEmpty()) PLAYERS.remove(player.getUuid(), state);
    }

    private static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)
                || !(event.getLevel() instanceof ServerWorld world)) return;
        if (world.getBlockState(event.getPos()).isOf(Blocks.WATER)
                && world.getBiome(event.getPos()).isIn(BiomeTags.IS_DEEP_OCEAN)
                && EnchantmentHelper.getLevel(Enchantments.FROST_WALKER,
                player.getEquippedStack(EquipmentSlot.FEET)) > 0)
            pending(player.getUuid()).frost = new PendingFrost(world, event.getPos().toImmutable());
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
                    pending(player.getUuid()).conduit = new PendingConduit(world, candidate, world.getTime() + 120);
                    return;
                }
            }
        }
    }
    private static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity player
                && event.getState().isIn(BlockTags.LOGS_THAT_BURN))
            grant(player, "getting_wood");
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
            pending(push.player()).inception =
                    new PendingInception(world, push.original(), push.direction(), world.getTime() + 10);
    }
}
