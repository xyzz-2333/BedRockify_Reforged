package dev.bedrockify.forge.client.features.survivalInventory;

import dev.bedrockify.forge.client.BedrockifyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/** A recovery control independent of mods which hide or move the vanilla book button. */
public final class RecipeBookToggle extends ButtonWidget {
    private final RecipeBookWidget book;

    public RecipeBookToggle(RecipeBookWidget book) {
        super(0, 0, 20, 18, title(book), button -> {
            book.toggleOpen();
            var mod = BedrockifyClient.getInstance();
            // Only a deliberate click changes the preference. Other mods may temporarily
            // close the native book before opening their own screen.
            mod.settings.survivalRecipeBookOpen = book.isOpen();
            mod.saveSettings();
        }, DEFAULT_NARRATION_SUPPLIER);
        this.book = book;
    }

    private static Text title(RecipeBookWidget book) {
        return Text.translatable(book.isOpen() ? "bedrockify.survival.hideRecipes" : "bedrockify.survival.showRecipes");
    }

    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        // The screen owns rendering and the recipe widget owns pointer input, so this
        // control still works if a compatibility mod removes native recipe-book widgets.
        visible = active = true;
        setMessage(title(book));
        render(context, mouseX, mouseY, delta);
        if (isHovered()) context.drawTooltip(MinecraftClient.getInstance().textRenderer, getMessage(), mouseX, mouseY);
    }

    @Override protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        SurvivalSprites.button(context, getX(), getY(), getWidth(), getHeight(), book.isOpen());
        context.drawItem(Items.KNOWLEDGE_BOOK.getDefaultStack(), getX() + 2, getY() + 1);
        if (isSelected()) {
            int x = getX(), y = getY();
            context.drawBorder(x, y, getWidth(), getHeight(), 0xffffffff);
        }
    }
}
