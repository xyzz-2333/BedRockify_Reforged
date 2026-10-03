package me.juancarloscp52.bedrockify.client;

import com.google.gson.Gson;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import me.juancarloscp52.bedrockify.client.features.bedrockShading.BedrockBlockShading;
import me.juancarloscp52.bedrockify.client.features.bedrockShading.BedrockSunGlareShading;
import me.juancarloscp52.bedrockify.client.features.fishingBobber.FishingBobber3DModel;
import me.juancarloscp52.bedrockify.client.features.heldItemTooltips.HeldItemTooltips;
import me.juancarloscp52.bedrockify.client.features.hudOpacity.HudOpacity;
import me.juancarloscp52.bedrockify.client.features.reacharoundPlacement.ReachAroundPlacement;
import me.juancarloscp52.bedrockify.client.features.sheepColors.SheepSkinResource;
import me.juancarloscp52.bedrockify.client.features.worldColorNoise.WorldColorNoiseSampler;
import me.juancarloscp52.bedrockify.client.gui.Overlay;
import me.juancarloscp52.bedrockify.client.gui.SettingsGUI;
import me.juancarloscp52.bedrockify.common.block.entity.WaterCauldronBlockEntity;
import me.juancarloscp52.bedrockify.common.features.cauldron.BedrockCauldronBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BedrockifyClient {

    private static BedrockifyClient instance;
    public static final Logger LOGGER = LogManager.getLogger();
    public ReachAroundPlacement reachAroundPlacement;
    public Overlay overlay;
    public HeldItemTooltips heldItemTooltips;
    public SettingsGUI settingsGUI;
    public WorldColorNoiseSampler worldColorNoiseSampler;
    public BedrockBlockShading bedrockBlockShading;
    public BedrockSunGlareShading bedrockSunGlareShading;
    public HudOpacity hudOpacity;
    public long deltaTime = 0;
    private int timeFlying = 0;
    private static KeyBinding keyBinding;

    public BedrockifyClientSettings settings;

    public static BedrockifyClient getInstance() {
        return instance;
    }
    public static void register(IEventBus bus) {
        instance = new BedrockifyClient();
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(instance::onInitializeClient));
        bus.addListener((RegisterKeyMappingsEvent event) -> {
            keyBinding = new KeyBinding("bedrockIfy.key.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "BedrockIfy");
            event.register(keyBinding);
        });
        bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
                event.registerLayerDefinition(FishingBobber3DModel.MODEL_LAYER, FishingBobber3DModel::generateModel));
        bus.addListener((RegisterColorHandlersEvent.Block event) -> {
            event.register((state, world, pos, tintIndex) -> {
                if (world == null || pos == null) return -1;
                return world.getBlockEntity(pos, BedrockCauldronBlocks.WATER_CAULDRON_ENTITY)
                        .map(WaterCauldronBlockEntity::getTintColor).orElse(-1);
            }, BedrockCauldronBlocks.POTION_CAULDRON, BedrockCauldronBlocks.COLORED_WATER_CAULDRON);
        });
        bus.addListener((RegisterClientReloadListenersEvent event) -> event.registerReloadListener(new SheepSkinResource()));
        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
            if (instance.overlay != null) instance.overlay.renderOverlay(event.getGuiGraphics());
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && instance.hudOpacity != null) instance.tick(MinecraftClient.getInstance());
        });
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> instance.settingsGUI.getConfigScreen(parent, false)));
    }

    public void onInitializeClient() {
        instance = this;
        loadSettings();
        LOGGER.info("Initializing BedrockIfy Client.");
        overlay = new Overlay((MinecraftClient.getInstance()));
        reachAroundPlacement = new ReachAroundPlacement(MinecraftClient.getInstance());
        heldItemTooltips = new HeldItemTooltips();
        settingsGUI=new SettingsGUI();
        worldColorNoiseSampler = new WorldColorNoiseSampler();
        bedrockBlockShading = new BedrockBlockShading();
        bedrockSunGlareShading = new BedrockSunGlareShading();
        hudOpacity = new HudOpacity();
        LOGGER.info("Initialized BedrockIfy Forge Client");
    }

    private void tick(MinecraftClient client) {

            while (keyBinding != null && keyBinding.wasPressed()){
                client.setScreen(settingsGUI.getConfigScreen(client.currentScreen,true));
            }
            hudOpacity.tick();
            bedrockSunGlareShading.tick(client.getTickDelta());

            // Reduce drift progressively; 100% retains the old instant-stop behavior.
            if(settings.disableFlyingMomentum && null != client.player && client.player.getAbilities().flying){
                double retention = 1.0 - Math.max(0, Math.min(100, settings.flyingBrakeStrength)) / 100.0;
                if(!(client.options.leftKey.isPressed() || client.options.backKey.isPressed() ||client.options.rightKey.isPressed() ||client.options.forwardKey.isPressed())){
                    client.player.setVelocity(client.player.getVelocity().getX() * retention,
                            client.player.getVelocity().getY(), client.player.getVelocity().getZ() * retention);
                }
                if(!(client.options.sneakKey.isPressed()|| client.options.jumpKey.isPressed())){
                    client.player.setVelocity(client.player.getVelocity().getX(),
                            client.player.getVelocity().getY() * retention, client.player.getVelocity().getZ());

                }
            }

            // Stop elytra flying by pressing space
            if(null != client.player && settings.elytraStop && client.player.isFallFlying() && timeFlying > 10 && client.options.jumpKey.isPressed()){
                client.player.stopFallFlying();
                client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
            if(null != client.player && client.player.isFallFlying() && !client.options.jumpKey.isPressed())
                timeFlying++;
            else
                timeFlying = 0;

    }

    public void loadSettings() {
        File file = new File("./config/bedrockify/bedrockifyClient.json");
        Gson gson = new Gson();
        settings = new BedrockifyClientSettings();
        if (file.exists()) {
            try {
                try (FileReader fileReader = new FileReader(file)) {
                    settings = gson.fromJson(fileReader, BedrockifyClientSettings.class);
                }
                if (settings == null) settings = new BedrockifyClientSettings();
            } catch (IOException | com.google.gson.JsonParseException e) {
                LOGGER.warn("Could not load bedrockIfy settings: " + e.getLocalizedMessage());
            }
        } else {
            settings = new BedrockifyClientSettings();
            saveSettings();
        }
    }

    public void saveSettings() {
        Gson gson = new Gson();
        File file = new File("./config/bedrockify/bedrockifyClient.json");
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
