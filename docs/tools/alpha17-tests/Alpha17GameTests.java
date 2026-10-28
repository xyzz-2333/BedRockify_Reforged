package dev.bedrockify.forge.tests;

import com.mojang.authlib.GameProfile;
import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public class Alpha17GameTests {
    private static Advancement advancement(TestContext context, String path) {
        return context.getWorld().getServer().getAdvancementLoader()
                .get(new Identifier("bedrockify", "bedrock/" + path));
    }

    @GameTest(templateName="empty")
    public static void catalogueLoads(TestContext context) {
        long loaded = context.getWorld().getServer().getAdvancementLoader().getAdvancements().stream()
                .filter(node -> node.getId().getNamespace().equals("bedrockify")
                        && node.getId().getPath().startsWith("bedrock/"))
                .count();
        context.assertTrue(loaded == 90, "All 82 achievements, seven stages and root load; found " + loaded);
        Advancement root = advancement(context, "root");
        context.assertTrue(root != null, "Bedrock advancement root loads");
        String previous = "root";
        for (String stage : new String[]{"survival", "homestead", "industry", "exploration",
                "village", "combat", "mastery"}) {
            Advancement node = advancement(context, "stage_" + stage);
            context.assertTrue(node != null && node.getParent() == advancement(context, previous),
                    "Stage " + stage + " loads after " + previous);
            previous = "stage_" + stage;
        }
        context.assertTrue(advancement(context, "taking_inventory") != null,
                "Custom-granted achievement loads");
        context.assertTrue(advancement(context, "bake_bread") != null,
                "Vanilla-triggered achievement loads");
        context.complete();
    }

    @GameTest(templateName="empty")
    public static void playerProgressIsIndependent(TestContext context) {
        var world = context.getWorld();
        var first = new ServerPlayerEntity(world.getServer(), world,
                new GameProfile(UUID.randomUUID(), "brpFirst"));
        var second = new ServerPlayerEntity(world.getServer(), world,
                new GameProfile(UUID.randomUUID(), "brpSecond"));
        Advancement takingInventory = advancement(context, "taking_inventory");
        context.assertTrue(takingInventory != null, "Taking Inventory loads");
        context.assertFalse(first.getAdvancementTracker().getProgress(takingInventory).isDone(),
                "First player starts without the achievement");
        context.assertFalse(second.getAdvancementTracker().getProgress(takingInventory).isDone(),
                "Second player starts without the achievement");
        BedrockAchievements.inventoryOpened(first);
        context.assertTrue(first.getAdvancementTracker().getProgress(takingInventory).isDone(),
                "Inventory event grants the achievement to its player; root="
                        + first.getAdvancementTracker().getProgress(advancement(context, "root")).isDone()
                        + ", stage=" + first.getAdvancementTracker().getProgress(advancement(context, "stage_survival")).isDone()
                        + ", criterion=" + first.getAdvancementTracker().getProgress(takingInventory).getObtainedCriteria());
        context.assertFalse(second.getAdvancementTracker().getProgress(takingInventory).isDone(),
                "The other player's progress remains independent");
        context.complete();
    }
}
