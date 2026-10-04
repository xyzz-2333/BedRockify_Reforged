package me.juancarloscp52.bedrockify.mixin.client.features.survivalInventory;

import me.juancarloscp52.bedrockify.client.features.survivalInventory.SurvivalLayout;
import me.juancarloscp52.bedrockify.client.features.survivalInventory.SurvivalScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Optional JEI 15 integration. Its native recipe-book rectangle is 147x166. */
@Pseudo
@Mixin(targets = "mezz.jei.library.plugins.vanilla.gui.RecipeBookGuiHandler", remap = false)
public abstract class JeiRecipeBookGuiHandlerMixin {
    // Class literals let Loom remap Minecraft types even in this external method.
    // Select the container overload explicitly, excluding its Screen bridge method.
    @Inject(target = @Desc(value = "getGuiExtraAreas", args = HandledScreen.class, ret = List.class),
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void bedrockify$recipeArea(HandledScreen<?> screen, CallbackInfoReturnable<List<Rect2i>> cir) {
        SurvivalLayout layout = SurvivalLayout.current(screen);
        if (layout != null && ((SurvivalScreen)screen).bedrockify$recipeBook().isOpen())
            cir.setReturnValue(List.of(new Rect2i(layout.recipeLeft(), layout.top(),
                    SurvivalLayout.RECIPE_WIDTH, SurvivalLayout.HEIGHT)));
    }
}
