package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.item.map.MapIcon;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import static dev.bedrockify.forge.common.features.achievements.BedrockAchievements.grant;

/** Explorer-map provenance and deferred map-room confirmation. */
final class MapAchievements {
    private static final Map<UUID, ItemFrameEntity> PENDING_MAP_ROOMS = new HashMap<>();
    private MapAchievements() {}
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(MapAchievements::onEntityInteract);
    }
    static void clear() { PENDING_MAP_ROOMS.clear(); }
    static void logout(UUID player) { PENDING_MAP_ROOMS.remove(player); }
    static void tick(ServerPlayerEntity player) {
        ItemFrameEntity mapFrame = PENDING_MAP_ROOMS.remove(player.getUuid());
        if (mapFrame != null && !mapFrame.isRemoved() && isCompleteMapRoom(player.getServerWorld(), mapFrame))
            grant(player, "map_room");
        if (player.age % 20 == 0) checkExplorerMap(player);
    }

    static void recordExplorerMap(ServerPlayerEntity player, ItemStack sold) {
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
    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayerEntity player)) return;
        if (event.getTarget() instanceof ItemFrameEntity frame
                && event.getItemStack().isOf(Items.FILLED_MAP))
            PENDING_MAP_ROOMS.put(player.getUuid(), frame);
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
                if (!isFullyExplored(state)) {
                    complete = false;
                    break;
                }
                scale = (int) state.scale;
                centersX.add(state.centerX);
                centersZ.add(state.centerZ);
                centers.add(((long) state.centerX << 32) ^ (state.centerZ & 0xffffffffL));
            }
            if (complete && centers.size() == 9 && centersX.size() == 3 && centersZ.size() == 3) {
                int width = 128 << scale;
                if (hasAdjacentCenters(centersX, width) && hasAdjacentCenters(centersZ, width)) return true;
            }
        }
        return false;
    }
    private static boolean isFullyExplored(MapState state) {
        for (byte color : state.colors) if (color == 0) return false;
        return true;
    }

    // Called only after the grid has exactly three distinct centers on this axis.
    private static boolean hasAdjacentCenters(Set<Integer> centers, int width) {
        List<Integer> sorted = centers.stream().sorted().toList();
        return sorted.get(1) - sorted.get(0) == width && sorted.get(2) - sorted.get(1) == width;
    }
}
