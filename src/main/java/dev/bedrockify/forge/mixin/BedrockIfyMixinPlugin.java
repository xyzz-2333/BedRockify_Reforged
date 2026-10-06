package dev.bedrockify.forge.mixin;

import dev.bedrockify.forge.mixin.featureManager.MixinFeatureManager;
import dev.bedrockify.forge.platform.Platform;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BedrockIfyMixinPlugin  implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        MixinFeatureManager.loadMixinSettings();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".survivalInventory.Jei") && !Platform.isModLoaded("jei")) {
            return false;
        }
        if (mixinClassName.contains("client.features.bedrockShading.lightBlock") &&
                (Platform.isModLoaded("embeddium") || Platform.isModLoaded("rubidium"))) {
            return false;
        }
        if(mixinClassName.equals("dev.bedrockify.forge.mixin.common.features.fertilizableBlocks.SugarCaneBlockMixin") && Platform.isModLoaded("carpet-extra")){
            LogManager.getLogger().warn("BedrockIfy compatibility with \"Carpet Extra\" enabled.");
            LogManager.getLogger().warn("\t\\_ If you want to bonemeal sugar cane use carpet option /carpet betterBonemeal true");
            return false;
        }
        if(mixinClassName.equals("dev.bedrockify.forge.mixin.client.features.heldItemTooltips.ItemTooltipsMixin") && Platform.isModLoaded("held-item-info")){
            LogManager.getLogger().info("The mod \"Held Item Info\" has been detected. This mod is not totally compatible with BedrockIfy. BedrockIfy Held Item Tooltips has been disabled.");
            return false;
        }
        if(mixinClassName.contains("dev.bedrockify.forge.mixin.client.features.bedrockShading") && (Platform.isModLoaded("optifine") || Platform.isModLoaded("oculus"))){
            LogManager.getLogger().info("The mod \"OptiFabric\" has been detected. This mod is not totally compatible with BedrockIfy. BedrockIfy Bedrock Shading is now disabled.");
            return false;
        }
        if (mixinClassName.contains("dev.bedrockify.forge.mixin.client.compat.sodium")) {
            // Workaround of https://github.com/CaffeineMC/sodium-fabric/issues/895
            return Platform.isModLoaded("sodium");
        }
        if (mixinClassName.equals("dev.bedrockify.forge.mixin.client.features.sheepColors.SheepWoolFeatureRendererMixin") && (Platform.isModLoaded("optifine") || Platform.isModLoaded("oculus"))) {
            LogManager.getLogger().info("The mod \"OptiFabric\" has been detected. This mod is not totally compatible with BedrockIfy. BedrockIfy Sheep Colors is now disabled.");
            return false;
        }
        if (mixinClassName.contains("dev.bedrockify.forge.mixin.client.compat.fastload")) {
            return Platform.isModLoaded("fastload");
        }
        return MixinFeatureManager.isFeatureEnabled(mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
