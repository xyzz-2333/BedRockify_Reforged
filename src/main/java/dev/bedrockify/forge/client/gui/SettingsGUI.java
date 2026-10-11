package dev.bedrockify.forge.client.gui;

import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.BedrockifySettings;
import dev.bedrockify.forge.client.BedrockifyClient;
import dev.bedrockify.forge.client.BedrockifyClientSettings;
import dev.bedrockify.forge.mixin.featureManager.MixinFeatureManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Map;

public class SettingsGUI {

    BedrockifyClientSettings settingsClient = BedrockifyClient.getInstance().settings;
    BedrockifySettings settingsCommon = Bedrockify.getInstance().settings;

    public Screen getConfigScreen(Screen parent, boolean isTransparent){
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.translatable("bedrockify.options.settings"));
        builder.setSavingRunnable(()-> {
            Bedrockify.getInstance().saveSettings();
            BedrockifyClient.getInstance().saveSettings();
            MixinFeatureManager.saveMixinSettings();
        });

        builder.setDefaultBackgroundTexture(new Identifier("minecraft:textures/block/bedrock.png"));
        // Create Categories
        ConfigCategory gameplay = builder.getOrCreateCategory(Text.translatable("bedrockify.options.categories.gameplay"));
        ConfigCategory gui = builder.getOrCreateCategory(Text.translatable("bedrockify.options.categories.gui"));
        ConfigCategory visualImprovements = builder.getOrCreateCategory(Text.translatable("bedrockify.options.categories.visualImprovements"));
        ConfigCategory mixins = builder.getOrCreateCategory(Text.translatable("bedrockify.options.categories.mixins"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        /*
        *
        *   Gameplay Category
        *
        */

            // Reach Around Placement Sub Category
            SubCategoryBuilder reachAround = entryBuilder.startSubCategory(Text.translatable("bedrockify.options.subCategory.Reach-Around"));
            reachAround.add(entryBuilder.startTextDescription(Text.translatable("bedrockify.options.subCategory.Reach-Around.description")).build());
            reachAround.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.reachAround"), settingsClient.reacharound).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.reacharound=newValue).build());
            reachAround.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.reachAround.multiplayer"), settingsClient.reacharoundMultiplayer).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.reacharoundMultiplayer=newValue).build());
            reachAround.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.reachAround.sneaking"), settingsClient.reacharoundSneaking).setTooltip(wrapLines(Text.translatable("bedrockify.options.reachAround.sneaking.tooltip"))).setDefaultValue(false).setSaveConsumer(newValue -> settingsClient.reacharoundSneaking=newValue).build());
            reachAround.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.reachAround.indicator"), settingsClient.reacharoundIndicator).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.reacharoundIndicator=newValue).build());
            reachAround.add(entryBuilder.startIntSlider(Text.translatable("bedrockify.options.reachAround.pitch"), settingsClient.reacharoundPitchAngle, 0,90).setDefaultValue(25).setSaveConsumer(newValue -> settingsClient.reacharoundPitchAngle=newValue).build());
            reachAround.add(entryBuilder.startIntSlider(Text.translatable("bedrockify.options.reachAround.distance"), MathHelper.floor(settingsClient.reacharoundBlockDistance*100), 0,100).setTextGetter((integer -> Text.literal(String.valueOf(integer/100d)))).setDefaultValue(50).setSaveConsumer(newValue -> settingsClient.reacharoundBlockDistance=newValue/100d).build());
            gameplay.addEntry(reachAround.build());

            // Dying and Fallen Trees.
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.dyingTrees"), settingsCommon.dyingTrees).setDefaultValue(true).setSaveConsumer(newValue -> settingsCommon.dyingTrees=newValue).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.fallenTrees"), settingsCommon.fallenTrees).setDefaultValue(true).setSaveConsumer(newValue -> settingsCommon.fallenTrees=newValue).build());

            // Other Settings.
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.slowRegeneration"), settingsCommon.bedrockSlowRegeneration).setDefaultValue(true).setTooltip(wrapLines(Text.translatable("bedrockify.options.slowRegeneration.tooltip"))).setSaveConsumer(value -> settingsCommon.bedrockSlowRegeneration=value).build());
            gameplay.addEntry(entryBuilder.startIntSlider(Text.translatable("bedrockify.options.regenerationInterval"), Math.max(20, Math.min(200, settingsCommon.regenerationIntervalTicks)), 20, 200).setDefaultValue(80).setTooltip(wrapLines(Text.translatable("bedrockify.options.regenerationInterval.tooltip"))).setTextGetter(value -> Text.literal(String.format(java.util.Locale.ROOT, "%.1fs", value / 20.0))).setSaveConsumer(value -> settingsCommon.regenerationIntervalTicks=value).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.noAttackCooldown"), settingsCommon.bedrockNoAttackCooldown).setDefaultValue(true).setTooltip(wrapLines(Text.translatable("bedrockify.options.noAttackCooldown.tooltip"))).setSaveConsumer(value -> settingsCommon.bedrockNoAttackCooldown=value).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.recipes"), settingsCommon.bedrockRecipes).setTooltip(wrapLines(Text.translatable("bedrockify.options.recipes.tooltip"))).setDefaultValue(true).setSaveConsumer(newValue -> settingsCommon.bedrockRecipes=newValue).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.legacyRecipes"), settingsCommon.legacyBedrockRecipes).setTooltip(wrapLines(Text.translatable("bedrockify.options.legacyRecipes.tooltip"))).setDefaultValue(false).setSaveConsumer(newValue -> settingsCommon.legacyBedrockRecipes=newValue).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.fireAspectLight"), settingsCommon.fireAspectLight).setTooltip(wrapLines(Text.translatable("bedrockify.options.fireAspectLight.tooltip"))).setDefaultValue(true).setSaveConsumer(newValue -> settingsCommon.fireAspectLight=newValue).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.fernBonemeal"), settingsCommon.fernBonemeal).setDefaultValue(true).setSaveConsumer(newValue -> settingsCommon.fernBonemeal=newValue).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.disableFlyingMomentum"), settingsClient.disableFlyingMomentum).setTooltip(wrapLines(Text.translatable("bedrockify.options.disableFlyingMomentum.tooltip"))).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.disableFlyingMomentum =newValue).build());
            gameplay.addEntry(entryBuilder.startIntSlider(Text.translatable("bedrockify.options.flyingBrakeStrength"), settingsClient.flyingBrakeStrength, 0,100).setTooltip(wrapLines(Text.translatable("bedrockify.options.flyingBrakeStrength.tooltip"))).setTextGetter(value -> Text.literal(value + "%")).setDefaultValue(25).setSaveConsumer(newValue -> settingsClient.flyingBrakeStrength=newValue).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.elytraStop"), settingsClient.elytraStop).setTooltip(wrapLines(Text.translatable("bedrockify.options.elytraStop.tooltip"))).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.elytraStop=newValue).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.bedrockCauldron"), settingsCommon.bedrockCauldron).setTooltip(wrapLines(Text.translatable("bedrockify.options.bedrockCauldron.tooltip"))).setDefaultValue(true).setSaveConsumer(newValue -> settingsCommon.bedrockCauldron=newValue).build());
            gameplay.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.sheepcolors"), settingsClient.sheepColors).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.sheepColors=newValue).build());

        /*
         *
         *   GUI Category
         *
         */
            // Bedrock loading screens.
            gui.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.loadingScreen"), settingsClient.loadingScreen).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.loadingScreen=newValue).build());
            gui.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.menuPanorama"), settingsClient.menuPanorama).setDefaultValue(true).setTooltip(wrapLines(Text.translatable("bedrockify.options.menuPanorama.tooltip"))).setSaveConsumer(newValue -> settingsClient.menuPanorama=newValue).build());
            gui.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.loadingScreenChunkMap"), settingsClient.showChunkMap).setTooltip(wrapLines(Text.translatable("bedrockify.options.loadingScreenChunkMap.tooltip"))).setDefaultValue(false).setSaveConsumer(newValue -> settingsClient.showChunkMap=newValue).build());

            // Overlay
            SubCategoryBuilder bedrockOverlay = entryBuilder.startSubCategory(Text.translatable("bedrockify.options.subCategory.bedrockOverlay"));
            bedrockOverlay.add(entryBuilder.startTextDescription(Text.translatable("bedrockify.options.subCategory.bedrockOverlay.description")).build());
            bedrockOverlay.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.showCoordinates"), settingsClient.showPositionHUD).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.showPositionHUD=newValue).build());
            bedrockOverlay.add(entryBuilder.startSelector(Text.translatable("bedrockify.options.showFPS"), new Byte []{0,1,2}, settingsClient.FPSHUD).setDefaultValue((byte) 0).setNameProvider((value)-> switch (value) {
                case 0 -> Text.translatable("bedrockify.options.off");
                case 1 -> Text.translatable("bedrockify.options.withPosition");
                default -> Text.translatable("bedrockify.options.underPosition");
            }).setSaveConsumer((newValue)-> settingsClient.FPSHUD=newValue).build());
            bedrockOverlay.add(entryBuilder.startIntSlider(Text.translatable("bedrockify.options.coordinatesPosition"), settingsClient.positionHUDHeight,0,100).setDefaultValue(50).setSaveConsumer((newValue)-> settingsClient.positionHUDHeight=newValue).build());
            bedrockOverlay.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.showPaperDoll"), settingsClient.showPaperDoll).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.showPaperDoll=newValue).build());
            bedrockOverlay.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.showSavingOverlay"), settingsClient.savingOverlay).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.savingOverlay=newValue).build());
            gui.addEntry(bedrockOverlay.build());

            //Tooltips
            SubCategoryBuilder tooltips = entryBuilder.startSubCategory(Text.translatable("bedrockify.options.subCategory.tooltips"));
            tooltips.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.tooltips"), settingsClient.heldItemTooltips).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.heldItemTooltips =newValue).build());
            tooltips.add(entryBuilder.startIntSlider(Text.translatable("bedrockify.options.tooltips.background"),(int)Math.ceil(settingsClient.heldItemTooltipBackground*100),0,100).setDefaultValue(50).setSaveConsumer(newValue -> settingsClient.heldItemTooltipBackground=newValue/100d).build());
            gui.addEntry(tooltips.build());

            //Item slot Highlight
            SubCategoryBuilder itemHighlight = entryBuilder.startSubCategory(Text.translatable("bedrockify.options.subCategory.inventoryHighlight"));
            itemHighlight.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.inventoryHighlight"), settingsClient.slotHighlight).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.slotHighlight=newValue).build());
            itemHighlight.add(entryBuilder.startAlphaColorField(Text.translatable("bedrockify.options.inventoryHighlight.color1"), settingsClient.highLightColor1).setDefaultValue(0xffffffff).setSaveConsumer(newValue -> settingsClient.highLightColor1=newValue).build());
            itemHighlight.add(entryBuilder.startAlphaColorField(Text.translatable("bedrockify.options.inventoryHighlight.color2"), settingsClient.highLightColor2).setDefaultValue(0x8955ba00).setSaveConsumer(newValue -> settingsClient.highLightColor2=newValue).build());
            gui.addEntry(itemHighlight.build());

            // Screen Safe Area
            SubCategoryBuilder screenSafeArea = entryBuilder.startSubCategory(Text.translatable("bedrockify.options.subCategory.screenSafeArea"));
            screenSafeArea.add(entryBuilder.startIntSlider(Text.translatable("bedrockify.options.screenSafeArea"), settingsClient.screenSafeArea,0,30).setDefaultValue(0).setSaveConsumer((newValue)-> settingsClient.screenSafeArea=newValue).build());
            screenSafeArea.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.ignoreBorder"), settingsClient.overlayIgnoresSafeArea).setDefaultValue(false).setSaveConsumer(newValue -> settingsClient.overlayIgnoresSafeArea=newValue).build());
            gui.addEntry(screenSafeArea.build());

            //Other gui improvements.
            SubCategoryBuilder creativeInventory = entryBuilder.startSubCategory(Text.translatable("bedrockify.options.subCategory.creativeInventory"));
            creativeInventory.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.creativeInventory"), settingsClient.creativeInventory).setDefaultValue(true).setTooltip(wrapLines(Text.translatable("bedrockify.options.creativeInventory.tooltip"))).setSaveConsumer(value -> settingsClient.creativeInventory=value).build());
            creativeInventory.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.creativeInventoryGroups"), settingsClient.creativeInventoryGroups).setDefaultValue(true).setTooltip(wrapLines(Text.translatable("bedrockify.options.creativeInventoryGroups.tooltip"))).setSaveConsumer(value -> settingsClient.creativeInventoryGroups=value).build());
            gui.addEntry(creativeInventory.build());
            SubCategoryBuilder survivalInventory = entryBuilder.startSubCategory(Text.translatable("bedrockify.options.subCategory.survivalInventory"));
            survivalInventory.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.survivalInventory"), settingsClient.survivalInventory).setDefaultValue(true).setTooltip(wrapLines(Text.translatable("bedrockify.options.survivalInventory.tooltip"))).setSaveConsumer(value -> settingsClient.survivalInventory=value).build());
            survivalInventory.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.survivalRecipeGroups"), settingsClient.survivalRecipeGroups).setDefaultValue(true).setTooltip(wrapLines(Text.translatable("bedrockify.options.survivalRecipeGroups.tooltip"))).setSaveConsumer(value -> settingsClient.survivalRecipeGroups=value).build());
            gui.addEntry(survivalInventory.build());
            SubCategoryBuilder inventoryMemory = entryBuilder.startSubCategory(Text.translatable("bedrockify.options.subCategory.inventoryMemory"));
            inventoryMemory.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.rememberInventoryState"), settingsClient.rememberInventoryState).setDefaultValue(true).setTooltip(wrapLines(Text.translatable("bedrockify.options.rememberInventoryState.tooltip"))).setSaveConsumer(value -> settingsClient.rememberInventoryState=value).build());
            inventoryMemory.add(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.rememberInventorySearch"), settingsClient.rememberInventorySearch).setDefaultValue(true).setTooltip(wrapLines(Text.translatable("bedrockify.options.rememberInventorySearch.tooltip"))).setSaveConsumer(value -> settingsClient.rememberInventorySearch=value).build());
            gui.addEntry(inventoryMemory.build());
            gui.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.biggerItems"), settingsClient.biggerIcons).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.biggerIcons=newValue).build());
            gui.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.toolbarStyle"), settingsClient.bedrockToolbar).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.bedrockToolbar =newValue).setYesNoTextSupplier((value)->value ? Text.translatable("bedrockify.options.chatStyle.bedrock") : Text.translatable("bedrockify.options.chatStyle.vanilla")).setTooltip(wrapLines(Text.translatable("bedrockify.options.toolbarStyle.tooltip"))).build());
            gui.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.expTextStyle"), settingsClient.expTextStyle).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.expTextStyle=newValue).setYesNoTextSupplier((value)->value ? Text.translatable("bedrockify.options.chatStyle.bedrock") : Text.translatable("bedrockify.options.chatStyle.vanilla")).build());
            gui.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.chatStyle"), settingsClient.bedrockChat).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.bedrockChat=newValue).setYesNoTextSupplier((value)->value ? Text.translatable("bedrockify.options.chatStyle.bedrock") : Text.translatable("bedrockify.options.chatStyle.vanilla")).build());
            gui.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.pickupAnimations"), settingsClient.pickupAnimations).setTooltip(wrapLines(Text.translatable("bedrockify.options.pickupAnimations.tooltip"))).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.pickupAnimations=newValue).build());
            gui.addEntry(entryBuilder.startIntSlider(Text.translatable("bedrockify.options.hudOpacity"), settingsClient.hudOpacity,0,100).setDefaultValue(50).setSaveConsumer((newValue)-> {
                settingsClient.hudOpacity = newValue;
                BedrockifyClient.getInstance().hudOpacity.resetTicks();
            }).build());
            gui.addEntry(entryBuilder.startEnumSelector(Text.translatable("bedrockify.options.showBedrockIfyButton"), BedrockifyClientSettings.ButtonPosition.class, settingsClient.bedrockIfyButtonPosition).setTooltip(wrapLines(Text.translatable("bedrockify.options.showBedrockIfyButton.tooltip"))).setEnumNameProvider(anEnum -> Text.translatable(((BedrockifyClientSettings.ButtonPosition)anEnum).text)).setDefaultValue(BedrockifyClientSettings.ButtonPosition.BELOW_SLIDERS).setSaveConsumer(buttonPosition -> settingsClient.bedrockIfyButtonPosition =buttonPosition).build());
            gui.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.editionBranding"), settingsClient.hideEditionBranding).setTooltip(wrapLines(Text.translatable("bedrockify.options.editionBranding.tooltip"))).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.hideEditionBranding =newValue).build());


        /*
         *
         *   Visual Improvements Category
         *
         */
            visualImprovements.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.fishingBobber3D"), settingsClient.fishingBobber3D).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.fishingBobber3D=newValue).build());
            visualImprovements.addEntry(entryBuilder.startSelector(Text.translatable("bedrockify.options.idleAnimation"), new Float []{0.0f,0.5f,1.0f,1.5f,2.0f,2.5f,3.0f,4.0f}, settingsClient.idleAnimation).setDefaultValue(1.0f).setNameProvider((value)-> Text.literal("x"+ value)).setSaveConsumer((newValue)-> settingsClient.idleAnimation=newValue).build());
            visualImprovements.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.eatingAnimations"), settingsClient.eatingAnimations).setDefaultValue(true).setSaveConsumer(newValue -> settingsClient.eatingAnimations=newValue).build());
            visualImprovements.addEntry(entryBuilder.startBooleanToggle(Text.translatable("bedrockify.options.bedrockShading"), settingsClient.bedrockShading).setTooltip(wrapLines(Text.translatable("bedrockify.options.bedrockShading.tooltip"))).setDefaultValue(true).setSaveConsumer(newValue -> {
                settingsClient.bedrockShading=newValue;
                MinecraftClient.getInstance().worldRenderer.reload();
            }).build());
            visualImprovements.addEntry(entryBuilder.startIntSlider(Text.translatable("bedrockify.options.sunlightIntensity"), settingsClient.sunlightIntensity,0,100).setTooltip(wrapLines(Text.translatable("bedrockify.options.sunlightIntensity.tooltip"))).setDefaultValue(50).setSaveConsumer(newValue -> {
                settingsClient.sunlightIntensity = newValue;
                BedrockifyClient.getInstance().bedrockSunGlareShading.onSunlightIntensityChanged();
            }).build());

        /*
         *
         *   Mixins Category
         *
         */
            mixins.addEntry(entryBuilder.startTextDescription(Text.translatable("bedrockify.options.mixins.description")).build());
            for(Map.Entry<String, Boolean> elem : MixinFeatureManager.features.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()){
                String translationKey = "bedrockify.options.mixin." + elem.getKey();
                mixins.addEntry(entryBuilder.startBooleanToggle(Text.translatable(translationKey), elem.getValue()).setTooltip(wrapLines(Text.translatable(translationKey + ".tooltip"))).requireRestart().setDefaultValue(true).setSaveConsumer(newValue -> MixinFeatureManager.features.put(elem.getKey(),newValue)).build());
            }



        return builder.setTransparentBackground(isTransparent).build();
    }

    public Text[] wrapLines(Text text){
        List<StringVisitable> lines = MinecraftClient.getInstance().textRenderer.getTextHandler().wrapLines(text,Math.max(MinecraftClient.getInstance().getWindow().getScaledWidth()/2 - 43,170), Style.EMPTY);
        lines.get(0).getString();
        Text[] textLines = new Text[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            textLines[i]=Text.literal(lines.get(i).getString());
        }
        return textLines;
    }

}
