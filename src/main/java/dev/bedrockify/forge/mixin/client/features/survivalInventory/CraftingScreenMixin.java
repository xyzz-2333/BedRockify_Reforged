package dev.bedrockify.forge.mixin.client.features.survivalInventory;

import dev.bedrockify.forge.client.features.survivalInventory.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends HandledScreen<CraftingScreenHandler> implements SurvivalScreen {
    @Shadow @Final private RecipeBookWidget recipeBook;
    @Unique private final SurvivalLayout bedrockify$layout = new SurvivalLayout();
    @Unique private RecipeBookToggle bedrockify$toggle;
    @Unique private boolean bedrockify$geometryCustomized;
    protected CraftingScreenMixin(CraftingScreenHandler handler, PlayerInventory inventory, Text title) { super(handler, inventory, title); }
    @Override public SurvivalLayout bedrockify$survivalLayout() { return bedrockify$layout; }
    @Override public RecipeBookWidget bedrockify$recipeBook() { return recipeBook; }
    @Override public RecipeBookToggle bedrockify$recipeToggle() { return bedrockify$toggle; }
    @Inject(method = "init", at = @At("HEAD"))
    private void bedrockify$prepare(CallbackInfo ci) {
        bedrockify$layout.prepare((CraftingScreen)(Object)this, handler, true);
        if (bedrockify$layout.active()) {
            backgroundWidth = SurvivalLayout.WIDTH; backgroundHeight = SurvivalLayout.HEIGHT;
            titleX = 12; titleY = 6; playerInventoryTitleX = 12; playerInventoryTitleY = 98;
            bedrockify$geometryCustomized = true;
        } else if (bedrockify$geometryCustomized) {
            backgroundWidth = 176; backgroundHeight = 166;
            titleX = 29; titleY = 6; playerInventoryTitleX = 8; playerInventoryTitleY = 72;
            bedrockify$geometryCustomized = false;
        }
        bedrockify$toggle = null;
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockify$controls(CallbackInfo ci) {
        if (!bedrockify$layout.active()) return;
        titleX = 12; titleY = 6; playerInventoryTitleX = 12; playerInventoryTitleY = 98;
        for (var child : children()) if (child instanceof TexturedButtonWidget button
                && button.getWidth() == 20 && button.getHeight() == 18 && button.getX() == x + 5) {
            button.visible = false; break;
        }
        bedrockify$toggle = addSelectableChild(new RecipeBookToggle(recipeBook));
        bedrockify$layout.toggle(bedrockify$toggle);
        bedrockify$align();
    }
    @Unique private void bedrockify$align() {
        if (!bedrockify$layout.active()) return;
        x = bedrockify$layout.left(recipeBook.isOpen()); y = bedrockify$layout.top();
        bedrockify$layout.positionToggle(recipeBook.isOpen());
    }
    @Inject(method = "render", at = @At("HEAD"))
    private void bedrockify$renderPosition(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (client.currentScreen == (Object)this && bedrockify$layout.needsRefresh((CraftingScreen)(Object)this, handler, true)) clearAndInit();
        bedrockify$align();
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void bedrockify$renderToggle(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (bedrockify$layout.active() && bedrockify$toggle != null) bedrockify$toggle.draw(context, mouseX, mouseY, delta);
    }
    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void bedrockify$background(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!bedrockify$layout.active()) return;
        SurvivalSprites.inventory(context, handler, x, y);
        var arrow = bedrockify$layout.craftingArrow();
        SurvivalSprites.craftingArrow(context, x + arrow.getX(), y + arrow.getY(), arrow.getWidth(), arrow.getHeight()); ci.cancel();
    }
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        if (!bedrockify$layout.active()) { super.drawForeground(context, mouseX, mouseY); return; }
        context.drawText(textRenderer, title, 12, 6, 0x303030, false);
    }
}
