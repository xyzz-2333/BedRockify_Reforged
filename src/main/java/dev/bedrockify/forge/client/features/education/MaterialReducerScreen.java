package dev.bedrockify.forge.client.features.education;

import dev.bedrockify.forge.client.features.survivalInventory.SurvivalSprites;
import dev.bedrockify.forge.common.features.education.MaterialReducerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class MaterialReducerScreen extends HandledScreen<MaterialReducerScreenHandler> {
    public MaterialReducerScreen(MaterialReducerScreenHandler handler, PlayerInventory player, Text title) {
        super(handler, player, title); backgroundWidth = 204; backgroundHeight = 196; titleX = 10; titleY = 8;
    }
    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        SurvivalSprites.panel(context, x, y, backgroundWidth, backgroundHeight, false);
        int color = handler.isLocked() ? 0xff873f3f : 0xff8b8b8b;
        context.fill(x+99, y+50, x+102, y+61, color);
        context.fill(x+19, y+59, x+182, y+62, color);
        for (int n = 0; n < 9; n++) context.fill(x+19+n*20, y+61, x+22+n*20, y+72, color);
        for (var slot : handler.slots) SurvivalSprites.cell(context, x+slot.x-2, y+slot.y-2, slot.id == 0 && handler.isLocked());
    }
    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, 0xff404040, false);
        if (handler.isLocked()) context.drawText(textRenderer, Text.translatable("bedrockify.education.collectOutputs"), 10, 22, 0xff703333, false);
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context); super.render(context, mouseX, mouseY, delta); drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
