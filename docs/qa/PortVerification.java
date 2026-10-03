package qa;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.juancarloscp52.bedrockify.Bedrockify;
import me.juancarloscp52.bedrockify.client.BedrockifyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Temporary development-only mod; never included in the release JAR. */
@Mod("bedrockify_port_qa")
public final class PortVerification {
    private static final Logger LOG = LogManager.getLogger("BedrockIfyPortQA");
    private int traceRemaining;
    private int sample;

    private static synchronized void record(String message, Object... values) {
        for (Object value : values) {
            int at = message.indexOf("{}");
            if (at >= 0) message = message.substring(0, at) + value + message.substring(at + 2);
        }
        try {
            Files.writeString(Path.of("qa-results.log"), message + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        LOG.info("{}", message);
    }

    public PortVerification() {
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::tick);
    }

    private void commands(RegisterCommandsEvent event) {
        var root = literal("bifyqa").requires(source -> source.hasPermissionLevel(2));
        for (String mode : new String[]{"modern", "legacy", "off"}) {
            root.then(literal(mode).executes(context -> {
                var settings = Bedrockify.getInstance().settings;
                settings.bedrockRecipes = !mode.equals("off");
                settings.legacyBedrockRecipes = mode.equals("legacy");
                record("RULE_MODE {} (run /reload, then /bifyqa recipes)", mode);
                return 1;
            }));
        }
        root.then(literal("recipes").executes(context -> verifyRecipes(context.getSource())));
        root.then(literal("bridge").executes(context -> {
            MinecraftClient.getInstance().execute(() -> {
                var client = MinecraftClient.getInstance();
                record("BRIDGE_READY ready={} sneaking={} grounded={} requireSneaking={}",
                        BedrockifyClient.getInstance().reachAroundPlacement.canReachAround(),
                        client.player.isSneaking(), client.player.isOnGround(),
                        BedrockifyClient.getInstance().settings.reacharoundSneaking);
            });
            return 1;
        }));
        root.then(literal("flight").then(argument("strength", IntegerArgumentType.integer(0, 100))
                .executes(context -> {
                    int strength = IntegerArgumentType.getInteger(context, "strength");
                    MinecraftClient.getInstance().execute(() -> {
                        BedrockifyClient.getInstance().settings.disableFlyingMomentum = true;
                        BedrockifyClient.getInstance().settings.flyingBrakeStrength = strength;
                        traceRemaining = 160;
                        sample = 0;
                        record("FLIGHT_TRACE_START strength={}", strength);
                    });
                    return 1;
                })));
        event.getDispatcher().register(root);
    }

    private static boolean accepts(Recipe<?> recipe, Item item) {
        return recipe.getIngredients().stream().anyMatch(ingredient ->
                Arrays.stream(ingredient.getMatchingStacks()).anyMatch(stack -> stack.isOf(item)));
    }

    private static int verifyRecipes(ServerCommandSource source) {
        var manager = source.getServer().getRecipeManager();
        boolean shovel = accepts(manager.get(new Identifier("minecraft", "oak_boat")).orElseThrow(), Items.WOODEN_SHOVEL);
        boolean sticks = accepts(manager.get(new Identifier("minecraft", "barrel")).orElseThrow(), Items.STICK);
        boolean cobweb = manager.get(new Identifier("bedrockify", "string_from_cobweb")).isPresent();
        boolean recolor = accepts(manager.get(new Identifier("minecraft", "dye_red_wool")).orElseThrow(), Items.RED_WOOL);
        boolean bedRecolor = accepts(manager.get(new Identifier("minecraft", "dye_red_bed")).orElseThrow(), Items.RED_BED);
        boolean inkPrismarine = manager.get(new Identifier("bedrockify", "dark_prismarine_extra")).isPresent();
        boolean boneMealGlass = manager.get(new Identifier("bedrockify", "white_stained_glass_bone_meal")).isPresent();
        int carrotHeight = ((ShapedRecipe) manager.get(new Identifier("minecraft", "carrot_on_a_stick")).orElseThrow()).getHeight();
        var settings = Bedrockify.getInstance().settings;
        boolean legacy = settings.bedrockRecipes && settings.legacyBedrockRecipes;
        boolean allBoats = Arrays.stream(new String[]{"oak_boat", "spruce_boat", "birch_boat", "jungle_boat",
                "acacia_boat", "dark_oak_boat", "mangrove_boat", "cherry_boat", "bamboo_raft"}).allMatch(id ->
                accepts(manager.get(new Identifier("minecraft", id)).orElseThrow(), Items.WOODEN_SHOVEL) == legacy);
        boolean passed = allBoats && sticks == legacy && cobweb == legacy && recolor == settings.bedrockRecipes
                && bedRecolor == settings.bedrockRecipes && inkPrismarine == settings.bedrockRecipes
                && boneMealGlass == settings.bedrockRecipes && carrotHeight == (settings.bedrockRecipes ? 1 : 2);
        record("RECIPE_VERIFY {} mode={} boatShovel={} allNineBoatRules={} barrelSticks={} cobwebRecipe={} sameColorWoolInput={} sameColorBedInput={} inkPrismarine={} boneMealGlass={} carrotHeight={}",
                passed ? "PASS" : "FAIL", !settings.bedrockRecipes ? "off" : legacy ? "legacy" : "modern",
                shovel, allBoats, sticks, cobweb, recolor, bedRecolor, inkPrismarine, boneMealGlass, carrotHeight);
        return passed ? 1 : 0;
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || traceRemaining <= 0) return;
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
        traceRemaining--;
        var velocity = client.player.getVelocity();
        record("FLIGHT_SAMPLE strength={} tick={} flying={} forward={} vx={} vy={} vz={}",
                BedrockifyClient.getInstance().settings.flyingBrakeStrength, sample++,
                client.player.getAbilities().flying, client.options.forwardKey.isPressed(),
                velocity.x, velocity.y, velocity.z);
    }
}
