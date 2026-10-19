package dev.bedrockify.forge;

public class BedrockifySettings {

    public boolean bedrockRecipes = true;
    // Pre-1.20.10 boat, barrel and cobweb recipes are optional nostalgia rules.
    public boolean legacyBedrockRecipes = false;
    public boolean dyingTrees = true;
    public boolean fireAspectLight = true;
    public boolean fernBonemeal = true;
    public boolean bedrockArmorStand = true;
    public boolean armorStandExtendedRedstone = false;
    public boolean fallenTrees = true;
    public boolean bedrockCauldron = true;
    public boolean bedrockSlowRegeneration = true;
    public int regenerationIntervalTicks = 80;
    public boolean bedrockNoAttackCooldown = true;
    public boolean bedrockSharpness = true;
    public boolean bedrockProjectiles = true;
    public boolean bedrockPortalSpawning = true;
    public boolean bedrockTridentKiller = true;

    public boolean isBedrockRecipesEnabled() {
        return bedrockRecipes;
    }

    public boolean isLegacyBedrockRecipesEnabled() {
        return legacyBedrockRecipes;
    }


}
