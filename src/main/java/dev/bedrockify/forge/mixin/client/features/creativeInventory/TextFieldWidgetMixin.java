package dev.bedrockify.forge.mixin.client.features.creativeInventory;

import dev.bedrockify.forge.client.features.creativeInventory.CreativeSearchStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @Unique private boolean bedrockify$flatSearch() {
        return MinecraftClient.getInstance().currentScreen instanceof CreativeSearchStyle screen
                && screen.bedrockify$flatSearch((TextFieldWidget)(Object)this);
    }

    // Keep the native return width, including its one-pixel shadow advance, so
    // caret, selection and mid-string editing retain the original layout.
    @Redirect(method = "renderButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"))
    private int bedrockify$ordered(DrawContext context, TextRenderer font, OrderedText text, int x, int y, int color) {
        return bedrockify$flatSearch() ? context.drawText(font, text, x, y, color, false) + 1
                : context.drawTextWithShadow(font, text, x, y, color);
    }

    @Redirect(method = "renderButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    private int bedrockify$text(DrawContext context, TextRenderer font, Text text, int x, int y, int color) {
        return bedrockify$flatSearch() ? context.drawText(font, text, x, y, color, false) + 1
                : context.drawTextWithShadow(font, text, x, y, color);
    }

    @Redirect(method = "renderButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"))
    private int bedrockify$string(DrawContext context, TextRenderer font, String text, int x, int y, int color) {
        return bedrockify$flatSearch() ? context.drawText(font, text, x, y, color, false) + 1
                : context.drawTextWithShadow(font, text, x, y, color);
    }
}
