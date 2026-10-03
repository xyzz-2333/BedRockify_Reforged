package me.juancarloscp52.bedrockify.mixin.client.features.screenSafeArea;

import me.juancarloscp52.bedrockify.client.BedrockifyClient;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies screen borders without creating synthetic ModifyArgs classes. */
@Mixin(SubtitlesHud.class)
public class SubtitlesHudMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"))
    private int bedrockify$drawString(DrawContext context, TextRenderer font, String text, int x, int y, int color) {
        int border = BedrockifyClient.getInstance().settings.getScreenSafeArea();
        return context.drawTextWithShadow(font, text, x - border, y - border, color);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    private int bedrockify$drawText(DrawContext context, TextRenderer font, Text text, int x, int y, int color) {
        int border = BedrockifyClient.getInstance().settings.getScreenSafeArea();
        return context.drawTextWithShadow(font, text, x - border, y - border, color);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void bedrockify$drawBackground(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int border = BedrockifyClient.getInstance().settings.getScreenSafeArea();
        context.fill(x1 - border, y1 - border, x2 - border, y2 - border, color);
    }
}
