package dev.bedrockify.forge.mixin.client.features.screenSafeArea;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.mixin.featureManager.MixinFeatureManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Forge renders these bars itself rather than calling vanilla renderStatusBars. */
@Mixin(value=ForgeGui.class,remap=false)
public class ForgeGuiMixin {
    @Inject(method={"renderHealth","renderFood","renderAir","renderHealthMount"},at=@At("HEAD"))
    private void bedrockify$start(int width,int height,DrawContext context,CallbackInfo ci) { begin(context); }
    @Inject(method={"renderHealth","renderFood","renderAir","renderHealthMount"},at=@At("RETURN"))
    private void bedrockify$end(int width,int height,DrawContext context,CallbackInfo ci) { end(context); }
    @Inject(method="renderArmor",at=@At("HEAD"))
    private void bedrockify$startArmor(DrawContext context,int width,int height,CallbackInfo ci) { begin(context); }
    @Inject(method="renderArmor",at=@At("RETURN"))
    private void bedrockify$endArmor(DrawContext context,int width,int height,CallbackInfo ci) { end(context); }
    @Unique
    private static void begin(DrawContext context) {
        var mod=BedrockifyClient.getInstance();
        context.draw();context.getMatrices().push();
        int border=MixinFeatureManager.features.getOrDefault("client.features.screenSafeArea",true)?mod.settings.getScreenSafeArea():0;
        context.getMatrices().translate(0,-border,0);
        float alpha=MixinFeatureManager.features.getOrDefault("client.features.hudOpacity",true)?mod.hudOpacity.getHudOpacity(false):1;
        context.setShaderColor(1,1,1,alpha);
    }
    @Unique
    private static void end(DrawContext context) {
        context.draw();context.getMatrices().pop();context.setShaderColor(1,1,1,1);
    }
}
