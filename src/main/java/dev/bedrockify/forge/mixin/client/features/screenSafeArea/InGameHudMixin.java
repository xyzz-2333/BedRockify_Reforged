package dev.bedrockify.forge.mixin.client.features.screenSafeArea;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.platform.Platform;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    private int screenBorder;

    /**
     * Set the screenBorder area before anything renders.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void setScreenBorder(CallbackInfo info) {
        this.screenBorder = BedrockifyClient.getInstance().settings.getScreenSafeArea();
    }

    /**
     * Render the item Hotbar applying the screen border distance and transparency.
     */
    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"))
    private void drawTextureHotbar(DrawContext drawContext, Identifier texture, int x, int y, int u, int v, int width, int height) {
        if (width == 24 && u == 0 && v == 22 && BedrockifyClient.getInstance().settings.isBedrockToolbarEnabled()) {
            // Keep the classic selection neutral, independent of the atlas's green tint.
            float opacity = BedrockifyClient.getInstance().hudOpacity.getHudOpacity(false);
            drawContext.draw();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            int alpha = Math.round(opacity * 255) << 24;
            int frameHeight = height + (Platform.isModLoaded("raised") ? 0 : 2);
            int top = y - screenBorder, bottom = top + frameHeight;
            drawContext.drawBorder(x, top, width, frameHeight, alpha | 0x303030);
            drawContext.drawBorder(x + 1, top + 1, width - 2, frameHeight - 2, alpha | 0xeeeeee);
            drawContext.fill(x + 2, bottom - 3, x + width - 2, bottom - 2, alpha | 0xa0a0a0);
            drawContext.draw();
            RenderSystem.setShaderColor(1, 1, 1, opacity);
            return;
        }
        if((width ==29 && height == 24) || width == 182){
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, BedrockifyClient.getInstance().hudOpacity.getHudOpacity(true));
            drawContext.drawTexture(texture, x, y - screenBorder, u, v, width, height);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, BedrockifyClient.getInstance().hudOpacity.getHudOpacity(false));
        }else{
            boolean raisedEnabled = Platform.isModLoaded("raised");
            if (width == 24 && !raisedEnabled) {
                // Scale the selected frame without reading two extra atlas rows.
                // Keep the original selection UV rectangle for resource-pack hotbars.
                drawContext.drawTexture(texture, x, y - screenBorder, width, height + 2,
                        (float)u, (float)v, width, height, 256, 256);
            } else {
                drawContext.drawTexture(texture, x, y - screenBorder, u, v, width, height);
            }
        }
    }

    /**
     * Render the items in the Hotbar with the screen border distance.
     */
    @ModifyArg(method = "renderHotbar", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V"),index = 2)
    public int modifyHotbarItemPossition(int y){
        return y-screenBorder;
    }

    /**
     * Apply screen border offset to experience bars.
     */
    @ModifyArg(method = "renderExperienceBar", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"),index = 2)
    public int modifyTextureExperienceBar(int y){
        return y - screenBorder;
    }

    /**
     * Apply screen border offset to experience bar text.
     */
    @Redirect(method = "renderExperienceBar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"))
    private int drawExperienceBar(DrawContext drawContext, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        int alpha = (int) Math.ceil(BedrockifyClient.getInstance().hudOpacity.getHudOpacity(false)*255);

        if(!BedrockifyClient.getInstance().settings.isExpTextStyle()){
            return drawContext.drawText(textRenderer, text, x, y-screenBorder, color | ((alpha) << 24),false);
        }

        if(color == 0)
            return 0;
        return drawContext.drawTextWithShadow(textRenderer, text, x, y-screenBorder-3, ColorHelper.Argb.getArgb(alpha,127, 252, 32));
    }

    /**
     * Apply screen border offset to mount bars.
     */
    @ModifyArg(method = "renderMountJumpBar", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"),index = 2)
    public int modifyTextureMountJumpBar(int y){
        return y-screenBorder;
    }

    /**
     * Apply screen border offset to mount health bars.
     */
    @ModifyArg(method = "renderMountHealth", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"),index = 2)
    public int modifyTextureMountHealth(int y){
        return y-screenBorder;
    }

    /**
     * Apply screen border offset to status bars.
     */
    @ModifyArg(method = "renderStatusBars", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"),index = 2)
    public int modifyTextureStatusBar(int y){
        return y - screenBorder;
    }

    /**
     * Apply screen border offset to health bars.
     */
    @ModifyArg(method = "renderStatusBars", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHealthBar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;IIIIFIIIZ)V"),index = 3)
    private int modifyTextureStatusBarsHearts(int y){
        return y-screenBorder;
    }

    /**
     * Render the status effect overlay with the screen border distance applied.
     */
    @ModifyVariable(method = "renderStatusEffectOverlay", at = @At("STORE"),ordinal = 2)
    public int modifyStatusEffectOverlayX(int x){
        return x-screenBorder;
    }
    @ModifyVariable(method = "renderStatusEffectOverlay", at = @At("STORE"),ordinal = 3)
    public int modifyStatusEffectOverlayY(int y){
        return y+screenBorder;
    }

    // Apply screen borders to Titles, subtitles and other messages.
    @ModifyArg(method = "render", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I",ordinal = 0),index = 3)
    public int modifyOverlayMessage(int y){
        return y-screenBorder;
    }

}
