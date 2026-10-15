package dev.bedrockify.forge.client.features.education;
import dev.bedrockify.forge.common.features.education.ChemistryStationScreenHandler;
import dev.bedrockify.forge.client.features.survivalInventory.SurvivalSprites;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public final class ChemistryStationScreen extends HandledScreen<ChemistryStationScreenHandler> {
    public ChemistryStationScreen(ChemistryStationScreenHandler handler,PlayerInventory player,Text title){super(handler,player,title);backgroundWidth=204;backgroundHeight=212;titleX=10;titleY=8;playerInventoryTitleY=106;}
    private void send(int id){if(client!=null && client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,id);}
    @Override protected void init(){
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.translatable("bedrockify.education.combine"),b->send(0)).dimensions(x+116,y+94,76,20).build());
        if(handler.kind.equals("element_constructor"))for(int i=0;i<3;i++){
            final int index=i;
            addDrawableChild(ButtonWidget.builder(Text.literal("−"),b->send(index*2+1)).dimensions(x+106,y+26+i*22,18,20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("+"),b->send(index*2+2)).dimensions(x+128,y+26+i*22,18,20).build());
        }
    }
    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY){
        SurvivalSprites.panel(context,x,y,backgroundWidth,backgroundHeight,false);
        for(var slot:handler.slots)if(slot.isEnabled())SurvivalSprites.cell(context,x+slot.x-2,y+slot.y-2,false);
    }
    @Override protected void drawForeground(DrawContext context,int mouseX,int mouseY){
        context.drawText(textRenderer,title,titleX,titleY,0xff404040,false);
        if(handler.kind.equals("element_constructor")){
            String[] labels={"protons","neutrons","electrons"};
            for(int i=0;i<3;i++)context.drawText(textRenderer,Text.translatable("bedrockify.education."+labels[i]).append(": "+handler.particle(i)),10,32+i*22,0xff404040,false);
        }
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){renderBackground(context);super.render(context,mouseX,mouseY,delta);drawMouseoverTooltip(context,mouseX,mouseY);}
}
