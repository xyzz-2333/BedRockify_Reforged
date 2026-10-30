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
    @GameTest(templateName="empty")
    public static void movementStateIsIndependentAndClearedOnLogout(TestContext context) {
        var world = context.getWorld();
        var first = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), "brpBounce"));
        var second = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), "brpNoBounce"));
        var departed = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), "brpDeparted"));
        var origin = context.getAbsolutePos(new net.minecraft.util.math.BlockPos(1, 2, 1));
        for (var player : new ServerPlayerEntity[]{first, second, departed}) {
            player.age = 1; // Leave the independent once-per-second checks out of this event scenario.
            player.setPosition(origin.getX(), origin.getY(), origin.getZ());
            player.setVelocity(0, 1, 0);
        }
        BedrockAchievements.slimeBounce(first);
        BedrockAchievements.slimeBounce(departed);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(departed));
        for (var player : new ServerPlayerEntity[]{first, second, departed}) {
            player.setPosition(origin.getX(), origin.getY() + 31, origin.getZ());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END, player));
        }
        Advancement trampoline = advancement(context, "trampoline");
        context.assertTrue(first.getAdvancementTracker().getProgress(trampoline).isDone(), "A tracked 30-block bounce grants Trampoline");
        context.assertFalse(second.getAdvancementTracker().getProgress(trampoline).isDone(), "Another player's upward movement does not inherit the bounce");
        context.assertFalse(departed.getAdvancementTracker().getProgress(trampoline).isDone(), "Logout discards the pending bounce");
        for (var player : new ServerPlayerEntity[]{first, second, departed})
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
        context.complete();
    }

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
