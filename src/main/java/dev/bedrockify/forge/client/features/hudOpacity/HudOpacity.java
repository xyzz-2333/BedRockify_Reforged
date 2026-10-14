package dev.bedrockify.forge.client.features.hudOpacity;

import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.mixin.featureManager.MixinFeatureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class HudOpacity {

    private static final int maxInactiveTicks = 120; // 6 seconds.
    private static final int FADE_OUT_START_TICK = 100;
    private static final int FADE_IN_DURATION_TICK = 5;
    int inactiveTicks = 0;
    int previousSelectedSlot = -1;
    float currentOpacity = 1f;
    private net.minecraft.client.network.ClientPlayerEntity lastPlayer;
    private float previousHealth = -1;
    private int previousFood = -1, previousAir = -1, previousArmor = -1, previousExperience = -1;

    public float getHudOpacity(boolean isHotBarBackground){
        float max = BedrockifyClient.getInstance().settings.isBedrockToolbarEnabled() && isHotBarBackground? 0.6F:1.0F;
        return Math.min(MixinFeatureManager.features.getOrDefault("client.features.hudOpacity",true)?currentOpacity:1f, max);
    }

    public void resetTicks(){
        inactiveTicks=0;
    }

    public void tick(){
        if(inactiveTicks< maxInactiveTicks)
            inactiveTicks++;
        if(MinecraftClient.getInstance().player!= null){
            var player = MinecraftClient.getInstance().player;
            if (player != lastPlayer) { lastPlayer = player; currentOpacity = 1; resetTicks(); }
            int selectedSlot = player.getInventory().selectedSlot;
            if(selectedSlot != previousSelectedSlot){
                previousSelectedSlot=selectedSlot;
                resetTicks();
            }
            if (player.getHealth() != previousHealth || player.getHungerManager().getFoodLevel() != previousFood
                    || player.getAir() != previousAir || player.getArmor() != previousArmor || player.totalExperience != previousExperience
                    || player.isUsingItem() || player.handSwinging) resetTicks();
            previousHealth = player.getHealth(); previousFood = player.getHungerManager().getFoodLevel();
            previousAir = player.getAir(); previousArmor = player.getArmor(); previousExperience = player.totalExperience;
        }

        float opacity = MathHelper.clamp(BedrockifyClient.getInstance().settings.hudOpacity/100f, 0, 1);
        if (currentOpacity < opacity) currentOpacity = opacity;
        float delta = 1f - opacity;
        if(inactiveTicks > FADE_OUT_START_TICK && currentOpacity > opacity){
            currentOpacity -= delta / (maxInactiveTicks - FADE_OUT_START_TICK);
            clampOpacity(opacity);
        }else if(inactiveTicks<=FADE_IN_DURATION_TICK && currentOpacity < 1f){
            currentOpacity += delta / FADE_IN_DURATION_TICK;
            clampOpacity(opacity);
        }
    }

    private void clampOpacity(float minimum) {
        currentOpacity = MathHelper.clamp(currentOpacity, minimum, 1f);
    }
}
