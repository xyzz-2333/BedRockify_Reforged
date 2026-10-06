package dev.bedrockify.forge.mixin.client.features.survivalInventory;

import dev.bedrockify.forge.client.features.survivalInventory.SurvivalLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Rect2i;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Capture the relocated arrow when JEI creates the area, before opening its recipes screen. */
@Pseudo
@Mixin(targets = "mezz.jei.api.registration.IGuiHandlerRegistration$1", remap = false)
public abstract class JeiCraftingClickAreaMixin {
    @Shadow @Final private int val$xPos;
    @Shadow @Final private int val$yPos;
    @Shadow @Final private int val$width;
    @Shadow @Final private int val$height;

    @Unique private Rect2i bedrockify$arrow() {
        var layout = SurvivalLayout.current(MinecraftClient.getInstance().currentScreen);
        if (layout == null) return null;
        boolean nativeArrow = layout.crafting()
                ? val$xPos == 88 && val$yPos == 32 && val$width == 28 && val$height == 23
                : val$xPos == 137 && val$yPos == 29 && val$width == 10 && val$height == 13;
        return nativeArrow ? layout.craftingArrow() : null;
    }

    @ModifyArg(method = "getGuiClickableAreas",
            at = @At(value = "INVOKE", target = "Lmezz/jei/api/gui/handlers/IGuiClickableArea;createBasic(IIII[Lmezz/jei/api/recipe/RecipeType;)Lmezz/jei/api/gui/handlers/IGuiClickableArea;"),
            index = 0, require = 0, remap = false)
    private int bedrockify$arrowX(int original) {
        var arrow = bedrockify$arrow();
        return arrow == null ? original : arrow.getX();
    }

    @ModifyArg(method = "getGuiClickableAreas",
            at = @At(value = "INVOKE", target = "Lmezz/jei/api/gui/handlers/IGuiClickableArea;createBasic(IIII[Lmezz/jei/api/recipe/RecipeType;)Lmezz/jei/api/gui/handlers/IGuiClickableArea;"),
            index = 1, require = 0, remap = false)
    private int bedrockify$arrowY(int original) {
        var arrow = bedrockify$arrow();
        return arrow == null ? original : arrow.getY();
    }

    @ModifyArg(method = "getGuiClickableAreas",
            at = @At(value = "INVOKE", target = "Lmezz/jei/api/gui/handlers/IGuiClickableArea;createBasic(IIII[Lmezz/jei/api/recipe/RecipeType;)Lmezz/jei/api/gui/handlers/IGuiClickableArea;"),
            index = 2, require = 0, remap = false)
    private int bedrockify$arrowWidth(int original) {
        var arrow = bedrockify$arrow();
        return arrow == null ? original : arrow.getWidth();
    }

    @ModifyArg(method = "getGuiClickableAreas",
            at = @At(value = "INVOKE", target = "Lmezz/jei/api/gui/handlers/IGuiClickableArea;createBasic(IIII[Lmezz/jei/api/recipe/RecipeType;)Lmezz/jei/api/gui/handlers/IGuiClickableArea;"),
            index = 3, require = 0, remap = false)
    private int bedrockify$arrowHeight(int original) {
        var arrow = bedrockify$arrow();
        return arrow == null ? original : arrow.getHeight();
    }
}
