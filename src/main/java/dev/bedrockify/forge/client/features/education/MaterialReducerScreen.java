package dev.bedrockify.forge.client.features.education;

import dev.bedrockify.forge.client.gui.BedrockGui;
import dev.bedrockify.forge.common.features.education.ChemistryLayout;
import dev.bedrockify.forge.common.features.education.MaterialReducerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class MaterialReducerScreen extends HandledScreen<MaterialReducerScreenHandler> {
    public MaterialReducerScreen(MaterialReducerScreenHandler handler,PlayerInventory player,Text title) {
        super(handler,player,title);backgroundWidth=ChemistryLayout.REDUCER.width();backgroundHeight=ChemistryLayout.REDUCER.height();
    }
    @Override protected void init() { super.init();addDrawableChild(ChemistrySprites.close(x+backgroundWidth-18,y+7,this::close)); }
    @Override protected void drawBackground(DrawContext c,float delta,int mx,int my) {
        BedrockGui.panel(c,x,y,backgroundWidth,backgroundHeight,false);
        // The original connector texture joins the top input to five upper/four staggered lower outputs.
        ChemistrySprites.image(c,"material_reducer_connectors",x+49,y+25,108,54,108,54);
        for(var slot:handler.slots)ChemistrySprites.cell(c,x+slot.x-1,y+slot.y-1,18,slot.id==0&&handler.isLocked());
    }
    @Override protected void drawForeground(DrawContext c,int mx,int my) {
        c.drawText(textRenderer,title,(backgroundWidth-textRenderer.getWidth(title))/2,5,0xff4c4c4c,false);
        c.drawText(textRenderer,playerInventoryTitle,21,101,0xff4c4c4c,false);
        if(handler.isLocked() && isPointWithinBounds(93,17,18,18,mx,my))
            c.drawTooltip(textRenderer,Text.translatable("bedrockify.education.collectOutputs"),mx-x,my-y);
    }
    @Override public void render(DrawContext c,int mx,int my,float delta) { renderBackground(c);super.render(c,mx,my,delta);drawMouseoverTooltip(c,mx,my); }
}
