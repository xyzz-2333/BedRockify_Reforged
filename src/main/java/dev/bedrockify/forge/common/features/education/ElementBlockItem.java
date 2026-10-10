package dev.bedrockify.forge.common.features.education;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import java.util.List;

public final class ElementBlockItem extends BlockItem {
    private final int atomicNumber;
    public ElementBlockItem(Block block, int atomicNumber) { super(block, new Settings()); this.atomicNumber = atomicNumber; }
    @Override public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        if (atomicNumber == 0) tooltip.add(Text.translatable("bedrockify.education.unknown").formatted(Formatting.GRAY));
        else tooltip.add(Text.translatable("bedrockify.education.atomicNumber", atomicNumber).formatted(Formatting.GRAY));
    }
}
