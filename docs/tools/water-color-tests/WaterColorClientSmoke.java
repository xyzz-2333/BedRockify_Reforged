package dev.bedrockify.forge.tests;

import com.google.gson.JsonParser;
import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.features.biomeWaterColors.BedrockWaterColors;
import dev.bedrockify.forge.mixin.featureManager.MixinFeatureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;

/** Opt-in client check; no test world or distributable test classes. */
@Mod.EventBusSubscriber(modid="bedrockify", value=Dist.CLIENT)
public final class WaterColorClientSmoke {
    private static boolean completed;
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var client=MinecraftClient.getInstance();
        if (event.phase!=TickEvent.Phase.END || completed) return;
        if (client.currentScreen instanceof AccessibilityOnboardingScreen) client.setScreen(new TitleScreen());
        if (!(client.currentScreen instanceof TitleScreen)) return;
        completed=true;
        check(BedrockWaterColors.size()==64, "All 64 vanilla biomes loaded");
        int count=0;
        try {
            Class.forName("net.minecraft.world.biome.Biome");
            for (var field:BiomeKeys.class.getFields()) {
                if (field.getType()!=RegistryKey.class) continue;
                var id=((RegistryKey<?>)field.get(null)).getValue();
                check(BedrockWaterColors.lookup(id)!=null,"Missing vanilla biome: "+id);count++;
            }
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
        check(count==64,"Actual runtime BiomeKeys coverage");
        var mangrove=BedrockWaterColors.lookup(new Identifier("minecraft:mangrove_swamp"));
        check(mangrove.surface()==0x3a7a6a && mangrove.fog()==0x4d7a60,"Distinct surface and underwater colors");
        var warm=BedrockWaterColors.lookup(new Identifier("minecraft:warm_ocean"));
        check(warm.surface()==0x02b0e5 && warm.fog()==0x0289d5,"Warm ocean source colors");
        check(BedrockWaterColors.lookup(new Identifier("test:custom_biome"))==null,"Custom biomes keep original colors");
        var settings=BedrockifyClient.getInstance().settings;
        boolean saved=settings.bedrockWaterColors;
        boolean feature=MixinFeatureManager.features.getOrDefault("client.features.biomeWaterColors",true);
        try {
            settings.bedrockWaterColors=false;check(!BedrockWaterColors.enabled(),"Master switch disables palette");
            settings.bedrockWaterColors=true;MixinFeatureManager.features.put("client.features.biomeWaterColors",false);
            check(!BedrockWaterColors.enabled(),"Advanced feature switch disables palette");
        } finally { settings.bedrockWaterColors=saved;MixinFeatureManager.features.put("client.features.biomeWaterColors",feature); }
        var id=new Identifier("minecraft:plains");
        var table=new HashMap<Identifier,BedrockWaterColors.Colors>();
        table.put(id,new BedrockWaterColors.Colors(0x44aff5,0x44aff5));
        BedrockWaterColors.merge(JsonParser.parseString("{\"format\":1,\"biomes\":{\"minecraft:plains\":{\"surface\":\"#123456\"}}}").getAsJsonObject(),table);
        check(table.get(id).surface()==0x123456 && table.get(id).fog()==0x44aff5,"Partial resource override retains other color");
        BedrockWaterColors.merge(JsonParser.parseString("{\"format\":1,\"biomes\":{\"minecraft:plains\":null}}").getAsJsonObject(),table);
        check(!table.containsKey(id),"Null entry restores original biome color");
        boolean rejected=false;
        try {BedrockWaterColors.merge(JsonParser.parseString("{\"format\":1,\"biomes\":{\"test:bad\":{\"surface\":\"#GG0000\",\"fog\":\"#000000\"}}}").getAsJsonObject(),table);}
        catch(IllegalArgumentException ex) {rejected=true;}
        check(rejected,"Malformed colors rejected");
        check(BedrockifyClient.getInstance().settingsGUI.getConfigScreen(client.currentScreen,false)!=null,"Water settings screen builds");
        Bedrockify.LOGGER.info("ALPHA15 WATER SMOKE PASS: 64 runtime biomes, independent colors, custom-biome fallback, switches, pack overrides and malformed data");
        client.scheduleStop();
    }
}
