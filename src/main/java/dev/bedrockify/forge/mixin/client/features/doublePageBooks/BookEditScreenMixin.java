package dev.bedrockify.forge.mixin.client.features.doublePageBooks;

import dev.bedrockify.forge.client.features.doublePageBooks.BookSpread;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Reuses native selection, validation, signing, NBT and BookUpdateC2SPacket. */
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow private boolean signing;
    @Shadow private int currentPage;
    @Shadow @Final private List<String> pages;
    @Shadow private Text pageIndicatorText;
    @Shadow private long lastClickTime;
    @Shadow private int lastClickIndex;
    @Shadow private PageTurnWidget nextPageButton;
    @Shadow private PageTurnWidget previousPageButton;
    @Shadow private ButtonWidget doneButton;
    @Shadow private ButtonWidget signButton;
    @Shadow private ButtonWidget finalizeButton;
    @Shadow private ButtonWidget cancelButton;
    @Shadow protected abstract void changePage();
    @Shadow protected abstract void updateButtons();
    @Shadow protected abstract void appendNewPage();
    @Unique private boolean bedrockify$matrix;
    @Unique private boolean bedrockify$routing;
    @Unique private boolean bedrockify$textDrag;

    protected BookEditScreenMixin(Text title) { super(title); }
    @Unique private boolean bedrockify$spread() { return !signing && BookSpread.enabled(width, height); }
    @Unique private int bedrockify$dx() { return BookSpread.textX(width, currentPage % 2) - ((width - 192) / 2 + 36); }
    @Unique private int bedrockify$dy() { return BookSpread.textY(height) - 32; }

    @Inject(method={"init", "updateButtons"}, at=@At("RETURN"))
    private void bedrockify$buttons(CallbackInfo ci) {
        if (doneButton == null) return;
        int y = bedrockify$spread() ? BookSpread.top(height) + BookSpread.HEIGHT + 4 : 196;
        doneButton.setY(y); signButton.setY(y); finalizeButton.setY(196); cancelButton.setY(196);
        if (bedrockify$spread()) {
            previousPageButton.setX(BookSpread.left(width) + 16);
            nextPageButton.setX(BookSpread.left(width) + BookSpread.WIDTH - 39);
            previousPageButton.setY(BookSpread.top(height) + 176);
            nextPageButton.setY(BookSpread.top(height) + 176);
            previousPageButton.visible = BookSpread.firstPage(currentPage) > 0;
            nextPageButton.visible = BookSpread.firstPage(currentPage) < 98;
        } else {
            previousPageButton.setX((width - 192) / 2 + 43);
            nextPageButton.setX((width - 192) / 2 + 116);
            previousPageButton.setY(159); nextPageButton.setY(159);
        }
    }

    @Inject(method="openNextPage", at=@At("HEAD"), cancellable=true)
    private void bedrockify$next(CallbackInfo ci) {
        if (!bedrockify$spread()) return;
        bedrockify$selectPage(Math.min(98, BookSpread.firstPage(currentPage) + 2));
        ci.cancel();
    }
    @Inject(method="openPreviousPage", at=@At("HEAD"), cancellable=true)
    private void bedrockify$previous(CallbackInfo ci) {
        if (!bedrockify$spread()) return;
        bedrockify$selectPage(Math.max(0, BookSpread.firstPage(currentPage) - 2));
        ci.cancel();
    }
    @Unique private void bedrockify$selectPage(int page) {
        if (page < 0 || page >= 100) return;
        while (pages.size() <= page) appendNewPage();
        currentPage = page;
        lastClickTime = 0; lastClickIndex = -1;
        changePage(); updateButtons();
    }

    @Redirect(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;renderBackground(Lnet/minecraft/client/gui/DrawContext;)V"))
    private void bedrockify$paper(BookEditScreen screen, DrawContext context) {
        screen.renderBackground(context);
        if (!bedrockify$spread()) return;
        BookSpread.draw(context, width, height);
        int first = BookSpread.firstPage(currentPage);
        for (int side = 0; side < 2; side++) {
            int page = first + side;
            // Match native book ink. A dark shadow looks like a second glyph on pale paper.
            Text indicator = Text.translatable("book.pageIndicator", page + 1, Math.max(page + 1, pages.size()));
            context.drawText(textRenderer, indicator,
                    BookSpread.left(width) + side * BookSpread.PAGE_WIDTH + BookSpread.PAGE_WIDTH / 2 - textRenderer.getWidth(indicator) / 2,
                    BookSpread.top(height) + 13, 0xff000000, false);
            if (page != currentPage && page < pages.size()) {
                int y = BookSpread.textY(height);
                for (var line : textRenderer.wrapLines(Text.literal(pages.get(page)), BookSpread.TEXT_WIDTH)) {
                    if (y + 9 > BookSpread.textY(height) + BookSpread.TEXT_HEIGHT) break;
                    context.drawText(textRenderer, line, BookSpread.textX(width, side), y, 0xff000000, false);
                    y += 9;
                }
            }
        }
        context.getMatrices().push();
        context.getMatrices().translate(bedrockify$dx(), bedrockify$dy(), 0);
        bedrockify$matrix = true;
    }

    @Redirect(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"))
    private void bedrockify$hideSinglePage(DrawContext context, Identifier texture, int x, int y, int u, int v, int w, int h) {
        if (!bedrockify$spread()) context.drawTexture(texture, x, y, u, v, w, h);
    }
    @Redirect(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"))
    private int bedrockify$hideIndicator(DrawContext context, TextRenderer renderer, Text text, int x, int y, int color, boolean shadow) {
        if (bedrockify$spread() && text == pageIndicatorText) return 0;
        return context.drawText(renderer, text, x, y, color, shadow);
    }
    @Inject(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void bedrockify$restoreMatrix(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (bedrockify$matrix) { context.getMatrices().pop(); bedrockify$matrix = false; }
    }

    @Inject(method="mouseClicked", at=@At("HEAD"), cancellable=true)
    private void bedrockify$focusPage(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!bedrockify$spread() || bedrockify$routing) return;
        bedrockify$textDrag = false;
        if (super.mouseClicked(mouseX, mouseY, button)) { cir.setReturnValue(true); return; }
        int side = BookSpread.sideAt(width, height, mouseX, mouseY);
        if (button != 0 || side < 0) { cir.setReturnValue(false); return; }
        int page = BookSpread.firstPage(currentPage) + side;
        if (page != currentPage) bedrockify$selectPage(page);
        bedrockify$textDrag = true;
        bedrockify$routing = true;
        try {
            cir.setReturnValue(((BookEditScreen)(Object)this).mouseClicked(mouseX - bedrockify$dx(), mouseY - bedrockify$dy(), button));
        } finally { bedrockify$routing = false; }
    }
    @Inject(method="mouseDragged", at=@At("HEAD"), cancellable=true)
    private void bedrockify$drag(double x, double y, int button, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (!bedrockify$spread() || bedrockify$routing) return;
        if (!bedrockify$textDrag || button != 0) { cir.setReturnValue(false); return; }
        bedrockify$routing = true;
        try {
            cir.setReturnValue(((BookEditScreen)(Object)this).mouseDragged(x - bedrockify$dx(), y - bedrockify$dy(), button, dx, dy));
        } finally { bedrockify$routing = false; }
    }
}
