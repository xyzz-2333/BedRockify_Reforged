package dev.bedrockify.forge.common.features.mechanics;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/** Replace Java's aquatic-only Impaling bonus with Bedrock's water/rain condition. */
public final class BedrockTridentDamage {
    private BedrockTridentDamage() {}

    public static float withBedrockImpaling(ItemStack trident, LivingEntity target, float vanillaDamage) {
        int level = EnchantmentHelper.getLevel(Enchantments.IMPALING, trident);
        if (level == 0) return vanillaDamage;
        float bonus = 2.5F * level;
        if (target.getGroup() == EntityGroup.AQUATIC) vanillaDamage -= bonus;
        if (target.isTouchingWaterOrRain()) vanillaDamage += bonus;
        return vanillaDamage;
    }
}
