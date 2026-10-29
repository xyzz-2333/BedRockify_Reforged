package dev.bedrockify.forge.tests;

import dev.bedrockify.forge.Bedrockify;
import dev.bedrockify.forge.client.features.doublePageBooks.BookSpread;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Compares actual book framebuffer glyphs against native, flat black reference glyphs. */
@Mod.EventBusSubscriber(modid = "bedrockify", value = Dist.CLIENT)
public final class GuiTextClientTest {
    private static int ticks;
    private static final String[] CONTENT = {"Left 中文 text", "Right 中文 text"};
    private static final class ReferenceBook extends BookEditScreen {
        ReferenceBook(MinecraftClient c, ItemStack stack) { super(c.player, stack, Hand.MAIN_HAND); }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            // Reference regions are outside the book and do not replace any tested text.
            context.fill(0, 0, width, 45, 0xffffffff);
            for (int side = 0; side < 2; side++) {
                int x = BookSpread.textX(width, side);
                context.drawText(textRenderer, Text.translatable("book.pageIndicator", side + 1, 2), x, 5, 0xff000000, false);
                context.drawText(textRenderer, CONTENT[side], x, 23, 0xff000000, false);
            }
        }
    }
    private static boolean ink(int abgr) { return (abgr & 255) < 96 && (abgr >> 8 & 255) < 96 && (abgr >> 16 & 255) < 96; }
    private static void compare(NativeImage pixels, int x, int y, int refX, int refY, int width, int scale, String label) {
        int inkPixels = 0;
        for (int dy = 0; dy < 10 * scale; dy++) for (int dx = 0; dx < width * scale; dx++) {
            int actual = pixels.getColor(x * scale + dx, y * scale + dy);
            int expected = pixels.getColor(refX * scale + dx, refY * scale + dy);
            if (ink(actual) != ink(expected)) throw new IllegalStateException(label + " has displaced or shadow glyph pixels at " + dx + "," + dy);
            if (ink(expected)) {
                inkPixels++;
                if (actual != expected) throw new IllegalStateException(label + " has tinted ink");
            }
        }
        if (inkPixels == 0) throw new IllegalStateException(label + " has no visible text");
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var c = MinecraftClient.getInstance();
        if (c.currentScreen instanceof AccessibilityOnboardingScreen) c.setScreen(new TitleScreen());
        if (c.player == null || c.world == null) return;
        try {
            ticks++; c.options.pauseOnLostFocus = false;
            if (ticks == 1) { c.options.getGuiScale().setValue(2); c.onResolutionChanged(); }
            if (ticks == 20) {
                var stack = new ItemStack(Items.WRITABLE_BOOK);
                var pages = new NbtList();
                for (String text : CONTENT) pages.add(NbtString.of(text));
                stack.getOrCreateNbt().put("pages", pages);
                var screen = new ReferenceBook(c, stack); c.setScreen(screen);
                screen.keyPressed(GLFW.GLFW_KEY_END, 0, 0);
            }
            if (ticks == 30) {
                var screen = c.currentScreen;
                try (NativeImage pixels = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
                    int scale = (int)c.getWindow().getScaleFactor();
                    for (int side = 0; side < 2; side++) {
                        int rx = BookSpread.textX(screen.width, side);
                        var heading = Text.translatable("book.pageIndicator", side + 1, 2);
                        int w = c.textRenderer.getWidth(heading);
                        int hx = BookSpread.left(screen.width) + side * 160 + 80 - w / 2;
                        compare(pixels, hx, BookSpread.top(screen.height) + 13, rx, 5, w + 1, scale, "Page indicator " + side);
                        compare(pixels, rx, BookSpread.textY(screen.height), rx, 23, c.textRenderer.getWidth(CONTENT[side]) - 1, scale, "Page body " + side);
                    }
                }
                ScreenshotRecorder.saveScreenshot(c.runDirectory, "book-flat-text.png", c.getFramebuffer(), message -> {});
                Bedrockify.LOGGER.info("GUI TEXT PIXELS PASS: both page indicators and active/inactive Chinese/Latin book bodies match native black text without shadows");
                c.setScreen(null); c.scheduleStop();
            }
        } catch (Throwable e) { Bedrockify.LOGGER.error("GUI TEXT PIXELS FAIL", e); c.scheduleStop(); }
    }
}
