package dev.bedrockify.forge.mixin.client.features.survivalInventory;

import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.survivalInventory.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeBookGhostSlots;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(RecipeBookWidget.class)
public abstract class RecipeBookWidgetMixin implements SurvivalRecipeBook {
    @Shadow protected MinecraftClient client;
    @Shadow protected AbstractRecipeScreenHandler<?> craftingScreenHandler;
    @Shadow @Final private RecipeMatcher recipeFinder;
    @Shadow @Final protected RecipeBookGhostSlots ghostSlots;
    @Shadow private ClientRecipeBook recipeBook;
    @Shadow private TextFieldWidget searchField;
    @Shadow private boolean searching;
    @Shadow private String searchText;
    @Shadow public abstract boolean isOpen();
    @Shadow public abstract void toggleOpen();
    @Shadow protected abstract void sendBookDataPacket();
    @Shadow private boolean toggleFilteringCraftable() { throw new AssertionError(); }
    @Shadow private void refreshResults(boolean resetCurrentPage) { throw new AssertionError(); }
    @Shadow private void drawGhostSlotTooltip(DrawContext context, int x, int y, int mouseX, int mouseY) { throw new AssertionError(); }
    @Unique private BedrockRecipePanel bedrockify$panel;
    @Unique private long bedrockify$inputVersion;
    @Unique private boolean bedrockify$initializing;
    @Override public BedrockRecipePanel bedrockify$recipePanel() { return bedrockify$panel; }

    @Inject(method = "initialize", at = @At("HEAD"))
    private void bedrockify$initialize(int width, int height, MinecraftClient client, boolean narrow,
                                      AbstractRecipeScreenHandler<?> handler, CallbackInfo ci) {
        bedrockify$initializing = true;
        if (bedrockify$panel != null) { bedrockify$panel.cancelCrafting(); bedrockify$panel.remember(); }
        bedrockify$panel = SurvivalLayout.current(client.currentScreen) != null ? new BedrockRecipePanel(client, handler) : null;
        bedrockify$inputVersion++;
    }
    @Inject(method = "initialize", at = @At("TAIL"))
    private void bedrockify$initialized(int width, int height, MinecraftClient client, boolean narrow,
                                      AbstractRecipeScreenHandler<?> handler, CallbackInfo ci) {
        if (bedrockify$panel != null) {
            if (isOpen() != BedrockifyClient.getInstance().settings.survivalRecipeBookOpen) toggleOpen();
            bedrockify$position();
        }
        bedrockify$initializing = false;
    }
    @Inject(method = {"refreshInputs", "reset"}, at = @At("HEAD"))
    private void bedrockify$inputsChanged(CallbackInfo ci) { bedrockify$inputVersion++; }
    @Inject(method = "reset", at = @At("TAIL"))
    private void bedrockify$restoreSearchCache(CallbackInfo ci) {
        if (bedrockify$active() && searchField != null)
            searchText = searchField.getText().toLowerCase(java.util.Locale.ROOT);
    }
    @Inject(method = "refresh", at = @At("HEAD"))
    private void bedrockify$recipesChanged(CallbackInfo ci) {
        if (bedrockify$panel != null) bedrockify$panel.invalidate();
    }
    @Inject(method = "refreshResults", at = @At("HEAD"), cancellable = true)
    private void bedrockify$results(boolean resetPage, CallbackInfo ci) {
        if (!bedrockify$active()) return;
        if (bedrockify$panel.restoreSearch(searchField)) {
            searchText = searchField.getText().toLowerCase(java.util.Locale.ROOT);
            searching = false;
        }
        bedrockify$panel.refresh(recipeFinder, recipeBook, searchField, bedrockify$inputVersion, resetPage);
        bedrockify$position(); ci.cancel();
    }
    @Unique private SurvivalLayout bedrockify$currentLayout() {
        if (client == null || !(client.currentScreen instanceof SurvivalScreen screen)
                || screen.bedrockify$recipeBook() != (Object)this) return null;
        return SurvivalLayout.current(client.currentScreen);
    }
    @Unique private boolean bedrockify$active() {
        return bedrockify$panel != null && bedrockify$currentLayout() != null;
    }
    @Unique private void bedrockify$position() {
        if (bedrockify$panel != null) bedrockify$panel.position(bedrockify$currentLayout(), searchField);
    }
    @Inject(method = "findLeftEdge", at = @At("HEAD"), cancellable = true)
    private void bedrockify$left(int width, int backgroundWidth, CallbackInfoReturnable<Integer> cir) {
        var layout = bedrockify$currentLayout();
        if (layout != null) cir.setReturnValue(layout.left(isOpen()));
    }
    @Inject(method = "toggleOpen", at = @At("TAIL"))
    private void bedrockify$toggled(CallbackInfo ci) {
        if (bedrockify$panel != null) bedrockify$panel.cancelCrafting();
        if (bedrockify$active() && !bedrockify$initializing) bedrockify$position();
    }
    @Inject(method = "update", at = @At("TAIL"))
    private void bedrockify$craftingTick(CallbackInfo ci) {
        if (bedrockify$panel != null) bedrockify$panel.tick(bedrockify$active() && isOpen());
    }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void bedrockify$draw(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!bedrockify$active()) return;
        if (isOpen()) { bedrockify$position(); bedrockify$panel.render(context, mouseX, mouseY, delta); }
        ci.cancel();
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void bedrockify$click(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        if (bedrockify$currentLayout() == null) return;
        var toggle = ((SurvivalScreen)client.currentScreen).bedrockify$recipeToggle();
        if (toggle != null && toggle.mouseClicked(x, y, button)) {
            cir.setReturnValue(true); return;
        }
        if (bedrockify$panel == null || !isOpen()) return;
        boolean consumed = bedrockify$panel.mouseClicked(x, y, button, () -> {
            toggleFilteringCraftable(); sendBookDataPacket(); refreshResults(true);
        }, ghostSlots::reset);
        // Native searching suppresses the first character of shortcut-opened search.
        // Mouse focus must accept the first charTyped event, including IME input.
        searching = false; cir.setReturnValue(consumed);
    }
    // RecipeBookWidget inherits this default method from Element in 1.20.1.
    public boolean mouseScrolled(double x, double y, double amount) {
        return bedrockify$active() && isOpen() && bedrockify$panel.scroll(x, y, amount);
    }
    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
    private void bedrockify$hover(double x, double y, CallbackInfoReturnable<Boolean> cir) {
        if (bedrockify$active()) cir.setReturnValue(isOpen() && bedrockify$panel.contains(x, y));
    }
    @Inject(method = "drawTooltip", at = @At("HEAD"), cancellable = true)
    private void bedrockify$tooltip(DrawContext context, int x, int y, int mx, int my, CallbackInfo ci) {
        if (!bedrockify$active()) return;
        if (isOpen()) bedrockify$panel.tooltip(context, mx, my);
        drawGhostSlotTooltip(context, x, y, mx, my); ci.cancel();
    }
    @Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
    private void bedrockify$outside(double mx, double my, int x, int y, int w, int h, int button, CallbackInfoReturnable<Boolean> cir) {
        if (bedrockify$active() && isOpen()) cir.setReturnValue(!bedrockify$panel.contains(mx, my));
    }
}
