package dev.bedrockify.forge.client;

import com.google.gson.Gson;
import dev.bedrockify.forge.client.features.inventoryMemory.InventoryUiState;
import dev.bedrockify.forge.client.features.inventoryMemory.RememberedInventory;
import dev.bedrockify.forge.client.features.survivalInventory.SurvivalRecipeBook;
import dev.bedrockify.forge.client.features.survivalInventory.SurvivalScreen;
import net.minecraftforge.client.event.ScreenEvent;
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

import dev.bedrockify.forge.client.features.bedrockShading.BedrockBlockShading;
import dev.bedrockify.forge.client.features.bedrockShading.BedrockSunGlareShading;
import dev.bedrockify.forge.client.features.fishingBobber.FishingBobber3DModel;
import dev.bedrockify.forge.client.features.heldItemTooltips.HeldItemTooltips;
import dev.bedrockify.forge.client.features.hudOpacity.HudOpacity;
import dev.bedrockify.forge.client.features.reacharoundPlacement.ReachAroundPlacement;
import dev.bedrockify.forge.client.features.sheepColors.SheepSkinResource;
import dev.bedrockify.forge.client.features.worldColorNoise.WorldColorNoiseSampler;
import dev.bedrockify.forge.client.gui.Overlay;
import dev.bedrockify.forge.client.gui.SettingsGUI;
import dev.bedrockify.forge.common.block.entity.WaterCauldronBlockEntity;
import dev.bedrockify.forge.common.features.cauldron.BedrockCauldronBlocks;
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
    public InventoryUiState inventoryUiState;

    public static BedrockifyClient getInstance() {
        return instance;
    }
    public static void register(IEventBus bus) {
        instance = new BedrockifyClient();
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                dev.bedrockify.forge.common.features.mechanics.MechanicsRules.clearServerRule());
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() ->
                dev.bedrockify.forge.client.features.education.EducationClient.register()));
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
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Closing event) -> {
            if (event.getScreen() instanceof RememberedInventory screen) screen.bedrockify$rememberInventory();
            if (event.getScreen() instanceof SurvivalScreen screen
                    && screen.bedrockify$recipeBook() instanceof SurvivalRecipeBook book
                    && book.bedrockify$recipePanel() != null) {
                book.bedrockify$recipePanel().cancelCrafting();
                book.bedrockify$recipePanel().remember();
            }
        });
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> instance.settingsGUI.getConfigScreen(parent, false)));
    }

    public void onInitializeClient() {
        instance = this;
        loadSettings();
        inventoryUiState = new InventoryUiState(java.nio.file.Path.of("./config/bedrockify/inventoryUiState.json"), LOGGER::warn);
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
        if (inventoryUiState != null) inventoryUiState.save(settings.rememberInventorySearch);
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
