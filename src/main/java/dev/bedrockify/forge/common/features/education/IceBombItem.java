package dev.bedrockify.forge.common.features.education;
import net.minecraft.item.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
public final class IceBombItem extends Item {
    public IceBombItem(Settings settings){super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){
        var stack=player.getStackInHand(hand);
        if(!world.isClient){var bomb=new IceBombEntity(EducationContent.ICE_BOMB_ENTITY,world);bomb.setOwner(player);bomb.setPosition(player.getX(),player.getEyeY()-0.1,player.getZ());bomb.setItem(stack);bomb.setVelocity(player,player.getPitch(),player.getYaw(),0,1.5F,1);world.spawnEntity(bomb);}
        player.getItemCooldownManager().set(this,10);if(!player.isCreative())stack.decrement(1);
        return TypedActionResult.success(stack,world.isClient);
    }
}
