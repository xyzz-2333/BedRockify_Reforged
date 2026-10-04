package me.juancarloscp52.bedrockify.mixin.client.features.survivalInventory;

import me.juancarloscp52.bedrockify.client.features.survivalInventory.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> implements SurvivalScreen {
    @Shadow @Final private RecipeBookWidget recipeBook;
    @Unique private final SurvivalLayout bedrockify$layout = new SurvivalLayout();
    protected InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) { super(handler, inventory, title); }
    @Override public SurvivalLayout bedrockify$survivalLayout() { return bedrockify$layout; }
    @Override public RecipeBookWidget bedrockify$recipeBook() { return recipeBook; }

    @Inject(method = "init", at = @At("HEAD"))
    private void bedrockify$prepare(CallbackInfo ci) {
        boolean previous = bedrockify$layout.active();
        bedrockify$layout.prepare((InventoryScreen)(Object)this, handler, false);
        if (bedrockify$layout.active()) { backgroundWidth = SurvivalLayout.WIDTH; backgroundHeight = SurvivalLayout.HEIGHT; }
        else if (previous) { backgroundWidth = 176; backgroundHeight = 166; }
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockify$controls(CallbackInfo ci) {
        if (!bedrockify$layout.active()) return;
        for (var child : children()) if (child instanceof TexturedButtonWidget button
                && button.getWidth() == 20 && button.getHeight() == 18 && button.getX() == x + 104 && button.getY() == height / 2 - 22) {
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
        SurvivalSprites.panel(context, x + 40, y + 30, 70, 64, true);
        InventoryScreen.drawEntity(context, x + 75, y + 91, 27, x + 75 - mouseX, y + 50 - mouseY, client.player);
        context.drawText(textRenderer, ">", x + 165, y + 57, 0x404040, false);
        ci.cancel();
    }
    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
    private void bedrockify$titles(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!bedrockify$layout.active()) return;
        context.drawText(textRenderer, Text.translatable("container.inventory"), 48, 10, 0x303030, false);
        context.drawText(textRenderer, Text.translatable("container.crafting"), 122, 28, 0x303030, false);
        context.drawText(textRenderer, playerInventoryTitle, 18, 116, 0x303030, false);
        ci.cancel();
    }
}
