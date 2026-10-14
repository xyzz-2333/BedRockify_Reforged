package dev.bedrockify.forge.mixin.client.features.armorStand;

import dev.bedrockify.forge.Bedrockify;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.ArmorStandEntityModel;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandEntityModel.class)
public class ArmorStandEntityModelMixin {
    @Shadow @Final private ModelPart basePlate;
    @Inject(method="animateModel(Lnet/minecraft/entity/decoration/ArmorStandEntity;FFF)V", at=@At("TAIL"))
    private void bedrockify$rotateBaseWithStand(ArmorStandEntity stand, float limbAngle, float limbDistance, float tickDelta, CallbackInfo ci) {
        if (Bedrockify.getInstance()!=null && Bedrockify.getInstance().settings.bedrockArmorStand)
            basePlate.yaw=0;
    }
}
