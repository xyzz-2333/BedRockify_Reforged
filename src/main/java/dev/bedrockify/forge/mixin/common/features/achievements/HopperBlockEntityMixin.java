package dev.bedrockify.forge.mixin.common.features.achievements;

import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    private static final ThreadLocal<Integer> BEDROCKIFY_TRANSFER_COUNT = new ThreadLocal<>();

    @Inject(method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"))
    private static void bedrockify$beforeTransfer(Inventory from, Inventory to, ItemStack stack,
                                                   Direction side, CallbackInfoReturnable<ItemStack> info) {
        BEDROCKIFY_TRANSFER_COUNT.set(stack.getCount());
    }

    @Inject(method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"))
    private static void bedrockify$afterTransfer(Inventory from, Inventory to, ItemStack stack,
                                                  Direction side, CallbackInfoReturnable<ItemStack> info) {
        Integer before = BEDROCKIFY_TRANSFER_COUNT.get();
        BEDROCKIFY_TRANSFER_COUNT.remove();
        if (before != null && info.getReturnValue().getCount() < before)
            BedrockAchievements.hopperTransferred(from, to, stack);
    }
}
