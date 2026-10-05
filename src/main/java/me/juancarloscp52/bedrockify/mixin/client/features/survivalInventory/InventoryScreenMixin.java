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
    @Unique private RecipeBookToggle bedrockify$toggle;
    @Unique private boolean bedrockify$geometryCustomized;
    protected InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) { super(handler, inventory, title); }
    @Override public SurvivalLayout bedrockify$survivalLayout() { return bedrockify$layout; }
    @Override public RecipeBookWidget bedrockify$recipeBook() { return recipeBook; }
    @Override public RecipeBookToggle bedrockify$recipeToggle() { return bedrockify$toggle; }

    @Inject(method = "init", at = @At("HEAD"))
    private void bedrockify$prepare(CallbackInfo ci) {
        bedrockify$layout.prepare((InventoryScreen)(Object)this, handler, false);
        if (bedrockify$layout.active()) {
            backgroundWidth = SurvivalLayout.WIDTH; backgroundHeight = SurvivalLayout.HEIGHT;
            bedrockify$geometryCustomized = true;
        } else if (bedrockify$geometryCustomized) {
            backgroundWidth = 176; backgroundHeight = 166; bedrockify$geometryCustomized = false;
        }
        bedrockify$toggle = null;
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void bedrockify$controls(CallbackInfo ci) {
        if (!bedrockify$layout.active()) return;
        for (var child : children()) if (child instanceof TexturedButtonWidget button
                && button.getWidth() == 20 && button.getHeight() == 18 && button.getX() == x + 104 && button.getY() == height / 2 - 22) {
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
        if (client.currentScreen == (Object)this && bedrockify$layout.needsRefresh((InventoryScreen)(Object)this, handler, false)) clearAndInit();
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
        int portraitX = x + SurvivalLayout.PORTRAIT_X, portraitY = y + SurvivalLayout.PORTRAIT_Y;
        context.fill(portraitX, portraitY, portraitX + SurvivalLayout.PORTRAIT_WIDTH, portraitY + SurvivalLayout.PORTRAIT_HEIGHT, 0xff000000);
        int center = portraitX + SurvivalLayout.PORTRAIT_WIDTH / 2;
        InventoryScreen.drawEntity(context, center, portraitY + SurvivalLayout.PORTRAIT_HEIGHT - 4, 35,
                center - mouseX, portraitY + 35 - mouseY, client.player);
        var arrow = bedrockify$layout.craftingArrow();
        SurvivalSprites.craftingArrow(context, x + arrow.getX(), y + arrow.getY(), arrow.getWidth(), arrow.getHeight());
        ci.cancel();
    }
    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
    private void bedrockify$titles(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (!bedrockify$layout.active()) return;
        context.drawText(textRenderer, Text.translatable("container.inventory"), 46, 4, 0x303030, false);
        context.drawText(textRenderer, Text.translatable("container.crafting"), 114, 26, 0x303030, false);
        context.drawText(textRenderer, playerInventoryTitle, 12, 98, 0x303030, false);
        ci.cancel();
    }
}
