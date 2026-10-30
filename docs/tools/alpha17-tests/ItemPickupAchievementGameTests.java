package dev.bedrockify.forge.tests;

import com.mojang.authlib.GameProfile;
import dev.bedrockify.forge.common.features.achievements.BedrockAchievements;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("bedrockify") @PrefixGameTestTemplate(false)
public final class ItemPickupAchievementGameTests {
    private static ServerPlayerEntity player(TestContext context, String name) {
        var world = context.getWorld();
        var player = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), name));
        player.age = 1;
        return player;
    }
    private static boolean granted(ServerPlayerEntity player, String path) {
        var advancement = player.server.getAdvancementLoader().get(new Identifier("bedrockify", "bedrock/" + path));
        return player.getAdvancementTracker().getProgress(advancement).isDone();
    }
    private static void pickup(ServerPlayerEntity receiver, ItemEntity item) {
        MinecraftForge.EVENT_BUS.post(new EntityItemPickupEvent(receiver, item));
    }
    private static void tick(ServerPlayerEntity player) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }
    private static void logout(ServerPlayerEntity player) {
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
    }
    // Only populate the online UUID lookup: no network connection or world player tick is needed.
    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayerEntity> online(TestContext context) throws ReflectiveOperationException {
        var field = PlayerManager.class.getDeclaredField("playerMap");
        field.setAccessible(true);
        return (Map<UUID, ServerPlayerEntity>)field.get(context.getWorld().getServer().getPlayerManager());
    }
    @GameTest(templateName="empty")
    public static void giftsConfirmPickupAndCreditThrower(TestContext context) throws Exception {
        var thrower = player(context, "BRGiftFrom");
        var receiver = player(context, "BRGiftTo");
        var online = online(context);
        var items = new ArrayList<ItemEntity>();
        online.put(thrower.getUuid(), thrower);
        try {
            for (var type : new net.minecraft.item.Item[]{Items.DIAMOND, Items.SUSPICIOUS_STEW}) {
                String achievement = type == Items.DIAMOND ? "diamonds_to_you" : "time_for_stew";
                var item = new ItemEntity(context.getWorld(), 0, 0, 0, new ItemStack(type, 3));
                items.add(item); item.setThrower(thrower.getUuid());
                pickup(receiver, item); tick(receiver);
                context.assertTrue(!granted(thrower, achievement), "An unchanged item count does not confirm a gift");
                item.getStack().decrement(1); tick(receiver);
                context.assertTrue(!granted(thrower, achievement), "An unsuccessful check is consumed until the next pickup event");
                pickup(receiver, item); logout(receiver); item.getStack().decrement(1); tick(receiver);
                context.assertTrue(!granted(thrower, achievement), "Logout clears the receiver's pending gift confirmations");
                pickup(receiver, item);
                context.assertTrue(!granted(thrower, achievement), "The pickup event itself does not grant the achievement");
                item.discard(); tick(receiver);
                context.assertTrue(granted(thrower, achievement) && !granted(receiver, achievement), "Confirmed gifts credit the online thrower, independently of receiver progress");
            }
            var self = new ItemEntity(context.getWorld(), 0, 0, 0, new ItemStack(Items.DIAMOND));
            items.add(self); self.setThrower(receiver.getUuid());
            pickup(receiver, self); self.discard(); tick(receiver);
            context.assertTrue(!granted(receiver, "diamonds_to_you"), "Picking up one's own item is not a gift");
            context.complete();
        } finally {
            online.remove(thrower.getUuid());
            for (var item : items) item.discard();
            logout(thrower); logout(receiver);
        }
    }
    @GameTest(templateName="empty")
    public static void campfireAndCatProvenanceAndScope(TestContext context) {
        var world = context.getWorld();
        var owner = player(context, "BRCatOwner");
        var other = player(context, "BROtherPickup");
        var origin = context.getAbsolutePos(new BlockPos(1, 2, 1));
        List<ItemEntity> items = new ArrayList<>();
        try {
            var ordinary = new ItemEntity(world, origin.getX(), origin.getY(), origin.getZ(), new ItemStack(Items.COOKED_COD));
            items.add(ordinary); world.spawnEntity(ordinary);
            pickup(owner, ordinary); ordinary.discard(); tick(owner);
            context.assertTrue(!granted(owner, "delicious_fish"), "Ordinary cooked cod has no campfire provenance");
            BedrockAchievements.spawnCampfireOutput(world, origin.getX(), origin.getY(), origin.getZ(), new ItemStack(Items.COOKED_COD, 4));
            var outputs = world.getEntitiesByClass(ItemEntity.class, new Box(origin).expand(2), item -> item.getStack().isOf(Items.COOKED_COD));
            context.assertTrue(!outputs.isEmpty(), "The native campfire output path spawned item entities");
            items.addAll(outputs);
            var cod = outputs.get(0);
            pickup(owner, cod); tick(owner);
            context.assertTrue(!granted(owner, "delicious_fish"), "An unconsumed campfire output does not count as picked up");
            pickup(owner, cod); cod.getStack().decrement(1); tick(owner);
            context.assertTrue(granted(owner, "delicious_fish"), "A partial pickup confirms campfire output provenance");
            var gift = new ItemEntity(world, origin.getX(), origin.getY(), origin.getZ(), new ItemStack(Items.RABBIT_HIDE, 3));
            items.add(gift);
            BedrockAchievements.startCatMorningGift(owner);
            try { world.spawnEntity(gift); } finally { BedrockAchievements.endCatMorningGift(); }
            var unrelated = new ItemEntity(world, origin.getX(), origin.getY(), origin.getZ(), new ItemStack(Items.RABBIT_HIDE));
            items.add(unrelated); world.spawnEntity(unrelated);
            pickup(owner, unrelated); unrelated.discard(); tick(owner);
            context.assertTrue(!granted(owner, "where_have_you_been"), "The cat-spawn scope does not leak to later items");
            pickup(other, gift); gift.getStack().decrement(1); tick(other);
            context.assertTrue(!granted(other, "where_have_you_been"), "Only the cat's owner can collect its morning gift achievement");
            pickup(owner, gift); gift.getStack().decrement(1); tick(owner);
            context.assertTrue(granted(owner, "where_have_you_been"), "The owner receives the achievement after confirmed pickup");
            context.complete();
        } finally {
            BedrockAchievements.endCatMorningGift();
            for (var item : items) item.discard();
            logout(owner); logout(other);
        }
    }
}
