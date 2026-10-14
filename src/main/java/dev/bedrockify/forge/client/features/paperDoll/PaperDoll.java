package dev.bedrockify.forge.client.features.paperDoll;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.BedrockifyClientSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;



public class PaperDoll {
    private final MinecraftClient client;
    private int size = 20;
    private int posY = 60;
    private int visibleUntilTick = -1;
    private ClientPlayerEntity lastPlayer;
    private BedrockifyClientSettings settings;
    /**
     * Uses in method <code>drawPaperDoll</code>; the custom shading vectors.
     *
     * @see net.minecraft.client.render.DiffuseLighting#disableGuiDepthLighting
     * @see RenderSystem#setupGuiFlatDiffuseLighting
     */
    private static final Vector3f FLAT_LIT_VEC1 = (new Vector3f(0.2F, 0.5F, -0.7F)).normalize();
    private static final Vector3f FLAT_LIT_VEC2 = (new Vector3f(-0.2F, 0.5F, 0.7F)).normalize();
    public PaperDoll(MinecraftClient client) {
        this.client = client;
    }


    /**
     * Render the player at the top left of the screen.
     * Shows action poses including swimming/crawling, flying, using items, blocking and riding.
     */
    public void renderPaperDoll(DrawContext drawContext) {
        settings = BedrockifyClient.getInstance().settings;
        size = MathHelper.clamp(settings.paperDollSize, 10, 40);
        if (!settings.isShowPaperDollEnabled())
            return;

        //Determine the position of the doll depending on the position of the overlay text.
        int textPosY = settings.getPositionHUDHeight();
        if (textPosY >= 2 * size + 10) {
            posY = textPosY - 5;
        } else {
            posY = textPosY + size * 2 + 5;
            if (settings.getFPSHUDoption()==2)
                posY += 10;
            if (settings.isShowPositionHUDEnabled())
                posY += 10;
        }

        if (client.player != null) {
            if (lastPlayer != client.player) { lastPlayer = client.player; visibleUntilTick = -1; }
            //If the player does an action that must show the player entity gui, set the counter to the current time.
            if (client.player.isSneaking() || client.player.isSubmergedInWater() || client.player.getPose().equals(EntityPose.SWIMMING) || client.player.isSprinting() || client.player.getAbilities().flying || client.player.isFallFlying() || client.player.isBlocking() || client.player.isUsingItem() || client.player.isRiding())
                visibleUntilTick = client.player.age + MathHelper.clamp(settings.paperDollTimeoutTicks, 0, 200);

            // Game ticks keep the display timeout stable across pause and wall-clock changes.
            if (!client.player.isSleeping() && (settings.paperDollAlwaysVisible || client.player.age <= visibleUntilTick))
                drawPaperDoll(drawContext);
        }
    }

    /**
     * Draw the player entity in the specified position on screen.
     */
    private void drawPaperDoll(DrawContext drawContext) {
        ClientPlayerEntity player = client.player;
        if (player == null)
            return;

        MatrixStack matrixStack = drawContext.getMatrices();
        matrixStack.push();

        int renderPosY = posY;
        // If the player is elytra flying, the entity must be manually centered depending on the pitch.
        if (player.isFallFlying())
            renderPosY = posY - MathHelper.ceil(size * 2 * toMaxAngleRatio(player.getPitch()));
        // If the player is swimming, the entity must also be centered in the Y axis.
        else if (player.getPose() == EntityPose.SWIMMING) {
            renderPosY = posY - size;
        }

        // Position the entity on screen.
        int posX = MathHelper.clamp(settings.paperDollX, size, Math.max(size, drawContext.getScaledWindowWidth() - size));
        int safeArea = settings.overlayIgnoresSafeArea? 0 : settings.getScreenSafeArea();
        int x = MathHelper.clamp(posX + safeArea, size, Math.max(size, drawContext.getScaledWindowWidth() - size));
        int y = MathHelper.clamp(renderPosY + safeArea, size * 2, Math.max(size * 2, drawContext.getScaledWindowHeight() - 5));
        matrixStack.translate(x, y, 0);
        matrixStack.scale((float) size, (float) size, -(float) size);
        Quaternionf quaternion = new Quaternionf().rotateZ((float)Math.PI);
        matrixStack.multiply(quaternion);

        // Store previous entity rotations.
        float bodyYaw = player.bodyYaw;
        float yaw = player.getYaw();
        float headYaw = player.headYaw;


        // Set the entity desired rotation for drawing.
        float angle = 145;
        if (player.isFallFlying() || player.isBlocking()) {
            player.headYaw = angle;
        } else {
            player.setYaw(headYaw - bodyYaw + angle);
            player.headYaw = player.getYaw();
        }
        player.bodyYaw = angle;

        // Set up shading.
        RenderSystem.setupGuiFlatDiffuseLighting(FLAT_LIT_VEC1, FLAT_LIT_VEC2);

        // Draw the entity.
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        try {
            RenderSystem.runAsFancy(() -> entityRenderDispatcher.render(player, 0, 0, 0, 0.0F, 1.0F, matrixStack, immediate, 0xF000F0));
            immediate.draw();
        } finally {
            entityRenderDispatcher.setRenderShadows(true);
            player.bodyYaw = bodyYaw;
            player.setYaw(yaw);
            player.headYaw = headYaw;
            matrixStack.pop();
            DiffuseLighting.enableGuiDepthLighting();
        }
    }

    private float toMaxAngleRatio(float angle) {
        return (90 + angle) / 180;
    }

}
