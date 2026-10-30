package dev.bedrockify.forge.common.features.achievements;

import net.minecraft.advancement.Advancement;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class BedrockAchievements {

    private BedrockAchievements() {}

    private static void clearSession() {
        MovementAchievements.clear();
        WorldInteractionAchievements.clear();
        ItemPickupAchievements.clear();
        AnimalAchievements.clear();
        VillageAchievements.clear();
        MapAchievements.clear();
        ContainerAchievements.clear();
        SurvivalAchievements.clear();
    }

    public static void register() {
        MovementAchievements.register();
        WorldInteractionAchievements.register();
        ItemPickupAchievements.register();
        ContainerAchievements.register();
        AnimalAchievements.register();
        VillageAchievements.register();
        MapAchievements.register();
        SurvivalAchievements.register();
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent event) -> clearSession());
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onLivingDamage);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onLogout);
        MinecraftForge.EVENT_BUS.addListener(BedrockAchievements::onEffectAdded);
    }

    public static void hopperTransferred(Inventory from, Inventory to, ItemStack stack) {
        ContainerAchievements.hopperTransferred(from, to, stack);
    }

    public static void beforeFurnaceTick(AbstractFurnaceBlockEntity furnace) {
        ContainerAchievements.beforeFurnaceTick(furnace);
    }

    public static void afterFurnaceTick(AbstractFurnaceBlockEntity furnace) {
        ContainerAchievements.afterFurnaceTick(furnace);
    }

    public static void beforeFurnaceSlotClick(ScreenHandler handler) {
        ContainerAchievements.beforeFurnaceSlotClick(handler);
    }

    public static void afterFurnaceSlotClick(ScreenHandler handler, PlayerEntity player) {
        ContainerAchievements.afterFurnaceSlotClick(handler, player);
    }

    public static void beforeGrindstoneSlotClick(ScreenHandler handler, int slot) {
        ContainerAchievements.beforeGrindstoneSlotClick(handler, slot);
    }

    public static void afterGrindstoneSlotClick(ScreenHandler handler, PlayerEntity player) {
        ContainerAchievements.afterGrindstoneSlotClick(handler, player);
    }

    public static void beforeLoomSlotClick(ScreenHandler handler, int slot) {
        ContainerAchievements.beforeLoomSlotClick(handler, slot);
    }

    public static void afterLoomSlotClick(ScreenHandler handler, PlayerEntity player) {
        ContainerAchievements.afterLoomSlotClick(handler, player);
    }

    public static void inventoryOpened(ServerPlayerEntity player) {
        grant(player, "taking_inventory");
    }

    public static void raidStarted(ServerPlayerEntity player) {
        grant(player, "were_being_attacked");
    }

    public static void slimeBounce(Entity entity) { MovementAchievements.slimeBounce(entity); }

    public static void startCatMorningGift(PlayerEntity owner) {
        ItemPickupAchievements.startCatMorningGift(owner);
    }

    public static void endCatMorningGift() {
        ItemPickupAchievements.endCatMorningGift();
    }

    public static void leatherArmorDyedInCauldron(PlayerEntity player, ItemStack original) {
        SurvivalAchievements.leatherArmorDyedInCauldron(player, original);
    }

    public static void spawnCampfireOutput(World world, double x, double y, double z, net.minecraft.item.ItemStack stack) {
        ItemPickupAchievements.spawnCampfireOutput(world, x, y, z, stack);
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity player)) return;
        WorldInteractionAchievements.tick(player);
        ItemPickupAchievements.tick(player);
        AnimalAchievements.tick(player);
        VillageAchievements.tick(player);
        MapAchievements.tick(player);
        MovementAchievements.tick(player);
        SurvivalAchievements.tick(player);

    }

    private static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().getWorld().isClient) return;
        if (event.getSource().getAttacker() instanceof ServerPlayerEntity attacker
                && event.getAmount() >= 18.0F)
            grant(attacker, "overkill");
        if (event.getEntity() instanceof PigEntity pig
                && event.getSource().isOf(DamageTypes.FALL)
                && pig.getFirstPassenger() instanceof ServerPlayerEntity rider)
            grant(rider, "when_pigs_fly");
    }

    static void incrementPersisted(ServerPlayerEntity player, String key, int target, String achievement) {
        incrementPersisted(player, key, 1, target, achievement);
    }

    static void incrementPersisted(ServerPlayerEntity player, String key, int amount, int target, String achievement) {
        NbtCompound playerData = player.getPersistentData();
        NbtCompound persisted = playerData.getCompound("PlayerPersisted");
        int total = Math.min(target, persisted.getInt(key) + amount);
        persisted.putInt(key, total);
        playerData.put("PlayerPersisted", persisted);
        if (total >= target) grant(player, achievement);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MovementAchievements.logout(event.getEntity().getUuid());
        WorldInteractionAchievements.logout(event.getEntity().getUuid());
        ItemPickupAchievements.logout(event.getEntity().getUuid());
        AnimalAchievements.logout(event.getEntity().getUuid());
        VillageAchievements.logout(event.getEntity().getUuid());
        MapAchievements.logout(event.getEntity().getUuid());
        SurvivalAchievements.logout(event.getEntity().getUuid());
    }

    private static void onEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof WitchEntity)
                || event.getEffectInstance().getEffectType() != StatusEffects.POISON
                || !(event.getEffectSource() instanceof PotionEntity potion)
                || !(potion.getOwner() instanceof ServerPlayerEntity player)) return;
        grant(player, "taste_of_your_own_medicine");
    }

    static void grant(ServerPlayerEntity player, String path) {
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

