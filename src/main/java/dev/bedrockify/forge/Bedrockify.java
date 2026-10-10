package dev.bedrockify.forge;

import com.google.gson.Gson;
import dev.bedrockify.forge.common.block.cauldron.BedrockCauldronBehavior;
import dev.bedrockify.forge.common.features.cauldron.BedrockCauldronBlocks;
import dev.bedrockify.forge.common.features.worldGeneration.DyingTrees;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Mod(Bedrockify.MOD_ID)
public class Bedrockify {
    public static final String MOD_ID = "bedrockify";
    public static final Logger LOGGER = LogManager.getLogger();
    public BedrockifySettings settings;
    private static Bedrockify instance;
    public static final Identifier EAT_PARTICLES = new Identifier(MOD_ID, "eat-particles");
    public static final Identifier CAULDRON_ACTION_PARTICLES = new Identifier(MOD_ID, "cauldron_particles");
    public static Bedrockify getInstance() {
        return instance;
    }

    public Bedrockify() {
        instance = this;
        loadSettings();
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(BedrockCauldronBlocks::register);
        bus.addListener(DyingTrees::register);
        bus.addListener(dev.bedrockify.forge.common.features.education.EducationContent::register);
        bus.addListener(dev.bedrockify.forge.common.features.education.EducationContent::creativeContents);
        dev.bedrockify.forge.common.features.mechanics.MechanicsNetwork.register();
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(BedrockCauldronBehavior::registerBehavior));
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> BedrockCauldronBehavior.registerBehavior());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                dev.bedrockify.forge.client.BedrockifyClient.register(bus));
        LOGGER.info("Initializing unofficial BedrockIfy Forge port.");
    }

    public void loadSettings() {
        File file = new File("./config/bedrockify/bedrockifyCommon.json");
        Gson gson = new Gson();
        settings = new BedrockifySettings();
        if (file.exists()) {
            try {
                try (FileReader fileReader = new FileReader(file)) {
                    settings = gson.fromJson(fileReader, BedrockifySettings.class);
                }
                if (settings == null) settings = new BedrockifySettings();
            } catch (IOException | com.google.gson.JsonParseException e) {
                LOGGER.warn("Could not load bedrockIfy settings: " + e.getLocalizedMessage());
            }
        } else {
            settings = new BedrockifySettings();
            saveSettings();
        }
    }

    public void saveSettings() {
        Gson gson = new Gson();
        File file = new File("./config/bedrockify/bedrockifyCommon.json");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(gson.toJson(settings));
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.warn("Could not save bedrockIfy settings: " + e.getLocalizedMessage());
        }
    }
}
