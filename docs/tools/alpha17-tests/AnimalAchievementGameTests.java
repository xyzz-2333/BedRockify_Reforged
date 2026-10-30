package dev.bedrockify.forge.tests;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public final class AnimalAchievementGameTests {
    private static ServerPlayerEntity player(TestContext context, String name) {
        var world = context.getWorld();
        var player = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), name));
        player.age = 1;
        return player;
    }
    private static void tick(ServerPlayerEntity player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
    private static int wolves(ServerPlayerEntity player) {
        return player.getPersistentData().getCompound("PlayerPersisted").getInt("BedrockifyWolvesTamed");
    }
    @GameTest(templateName="empty")
    public static void tameConfirmationThresholdAndLogout(TestContext context) {
        var first = player(context, "BRTamer");
        var other = player(context, "BROtherTamer");
        var departed = player(context, "BRDepartedTame");
        WolfEntity unconfirmed = EntityType.WOLF.create(context.getWorld());
        MinecraftForge.EVENT_BUS.post(new AnimalTameEvent(unconfirmed, first));
        tick(first);
        context.assertTrue(wolves(first) == 0, "An unconfirmed tame does not count");
        for (int index = 0; index < 5; index++) {
            WolfEntity wolf = EntityType.WOLF.create(context.getWorld());
            MinecraftForge.EVENT_BUS.post(new AnimalTameEvent(wolf, first));
            wolf.setOwnerUuid(first.getUuid());
            context.assertTrue(wolves(first) == index, "Tame progress is deferred until the tick");
            tick(first);
            tick(first);
            context.assertTrue(wolves(first) == index + 1, "Each confirmed tame is counted once");
        }
        var achievement = context.getWorld().getServer().getAdvancementLoader()
                .get(new Identifier("bedrockify", "bedrock/leader_of_the_pack"));
        context.assertTrue(first.getAdvancementTracker().getProgress(achievement).isDone(), "Five confirmed wolves grant the achievement");
        tick(other);
        context.assertTrue(wolves(other) == 0 && !other.getAdvancementTracker().getProgress(achievement).isDone(),
                "Another player's progress remains independent");
        WolfEntity late = EntityType.WOLF.create(context.getWorld());
        MinecraftForge.EVENT_BUS.post(new AnimalTameEvent(late, departed));
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(departed));
        late.setOwnerUuid(departed.getUuid());
        tick(departed);
        context.assertTrue(wolves(departed) == 0, "Logout discards pending tame confirmation");
        for (var player : new ServerPlayerEntity[]{first, other, departed})
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
        context.complete();
    }
}
