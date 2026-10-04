package me.juancarloscp52.bedrockify.mixin.client.features.survivalInventory;

import me.juancarloscp52.bedrockify.client.features.survivalInventory.*;
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
    protected CraftingScreenMixin(CraftingScreenHandler handler, PlayerInventory inventory, Text title) { super(handler, inventory, title); }
    @Override public SurvivalLayout bedrockify$survivalLayout() { return bedrockify$layout; }
    @Override public RecipeBookWidget bedrockify$recipeBook() { return recipeBook; }
    @Inject(method = "init", at = @At("HEAD"))
    private void bedrockify$prepare(CallbackInfo ci) {
        boolean previous = bedrockify$layout.active();
        bedrockify$layout.prepare((CraftingScreen)(Object)this, handler, true);
        if (bedrockify$layout.active()) { backgroundWidth = SurvivalLayout.WIDTH; backgroundHeight = SurvivalLayout.HEIGHT; }
        else if (previous) { backgroundWidth = 176; backgroundHeight = 166; }
        if (bedrockify$layout.active()) { titleX = 12; titleY = 10; playerInventoryTitleX = 18; playerInventoryTitleY = 116; }
        else if (previous) { titleX = 29; titleY = 6; playerInventoryTitleX = 8; playerInventoryTitleY = 72; }
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockify$controls(CallbackInfo ci) {
        if (!bedrockify$layout.active()) return;
        titleX = 12; titleY = 10; playerInventoryTitleX = 18; playerInventoryTitleY = 116;
        for (var child : children()) if (child instanceof TexturedButtonWidget button
                && button.getWidth() == 20 && button.getHeight() == 18 && button.getX() == x + 5) {
            bedrockify$layout.toggle(button); break;
        }
        bedrockify$align();
    }
    @Unique private void bedrockify$align() {
        if (!bedrockify$layout.active()) return;
        x = bedrockify$layout.left(recipeBook.isOpen()); y = bedrockify$layout.top();
        bedrockify$layout.positionToggle(recipeBook.isOpen());
    }
    @Inject(method = "render", at = @At("HEAD"))
    private void bedrockify$renderPosition(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) { bedrockify$align(); }
    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void bedrockify$background(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!bedrockify$layout.active()) return;
        SurvivalSprites.inventory(context, handler, x, y);
        context.drawText(textRenderer, ">", x + 132, y + 57, 0x404040, false); ci.cancel();
    }
}
