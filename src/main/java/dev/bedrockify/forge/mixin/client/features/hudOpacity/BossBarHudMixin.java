package dev.bedrockify.forge.mixin.client.features.hudOpacity;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bedrockify.forge.client.BedrockifyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Inject(method = "render", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/hud/BossBarHud;renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;)V"))
    public void applyAlphaBossBar (DrawContext context, CallbackInfo ci){
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1,1,1,BedrockifyClient.getInstance().hudOpacity.getHudOpacity(false));
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    private int bedrockify$drawBossName(DrawContext context, TextRenderer renderer, Text text, int x, int y, int color){
        int alpha = (int) Math.ceil(BedrockifyClient.getInstance().hudOpacity.getHudOpacity(false)*255);
        if (alpha < 4) return 0;
        return context.drawTextWithShadow(renderer,text,x,y,(color & 0xffffff) | (alpha << 24));
    }

    @Inject(method="render", at=@At("RETURN"))
    private void bedrockify$resetBossOpacity(DrawContext context, CallbackInfo ci) {
        context.draw();
        RenderSystem.setShaderColor(1,1,1,1);
    }
}
