package dev.bedrockify.forge.mixin.client.core.bedrockIfyButton;

import me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = SearchFieldEntry.class, remap = false)
public abstract class SearchFieldEntryMixin {
    @ModifyConstant(method = "render", constant = @Constant(stringValue = "Search..."), remap = false)
    private String bedrockify$localizeSearch(String original) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null && screen.getTitle().equals(Text.translatable("bedrockify.options.settings"))) {
            return Text.translatable("bedrockify.options.search").getString();
        }
        return original;
    }
}
