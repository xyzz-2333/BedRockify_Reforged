package dev.bedrockify.forge.client.features.sneakingShield;

import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.mixin.featureManager.MixinFeatureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;

/** Sends normal item-use packets; the server owns blocking and durability. */
public final class SneakingShield {
    private static ClientPlayerEntity owner;
    private static Hand automaticHand;
    private SneakingShield() {}
    public static void tick(MinecraftClient client) {
        var player=client.player;
        if (player!=owner) { owner=player; automaticHand=null; }
        if (player==null || client.interactionManager==null) return;
        boolean enabled=shouldAutoBlock(client);
        if (automaticHand!=null) {
            boolean stillOurs=player.isUsingItem() && player.getActiveHand()==automaticHand
                    && player.getActiveItem().getItem() instanceof ShieldItem;
            if (!stillOurs) automaticHand=null;
            else if (!enabled || !(player.getStackInHand(automaticHand).getItem() instanceof ShieldItem)) {
                if (!client.options.useKey.isPressed()) client.interactionManager.stopUsingItem(player);
                automaticHand=null;
            }
        }
        if (!enabled || player.isUsingItem() || client.options.useKey.isPressed()) return;
        Hand hand=player.getMainHandStack().getItem() instanceof ShieldItem ? Hand.MAIN_HAND
                : player.getOffHandStack().getItem() instanceof ShieldItem ? Hand.OFF_HAND : null;
        if (hand==null || player.getItemCooldownManager().isCoolingDown(player.getStackInHand(hand).getItem())) return;
        client.interactionManager.interactItem(player,hand);
        if (player.isUsingItem() && player.getActiveHand()==hand) automaticHand=hand;
    }
    private static boolean shouldAutoBlock(MinecraftClient client) {
        var mod=BedrockifyClient.getInstance();
        return mod!=null && mod.settings!=null && mod.settings.isSneakingShieldEnabled()
                && MixinFeatureManager.features.getOrDefault("client.features.sneakingShield",true)
                && client.player!=null && client.currentScreen==null
                && client.player.isSneaking() && !client.player.isSpectator();
    }
    /** Vanilla releases item use when right-click is up; preserve only our automatic shield. */
    public static boolean shouldKeepUsing(MinecraftClient client) {
        return shouldAutoBlock(client) && client.player==owner && automaticHand!=null
                && owner.isUsingItem() && owner.getActiveHand()==automaticHand
                && owner.getActiveItem().getItem() instanceof ShieldItem;
    }
}
