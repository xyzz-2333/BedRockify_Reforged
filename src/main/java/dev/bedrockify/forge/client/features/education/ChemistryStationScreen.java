package dev.bedrockify.forge.client.features.education;

import dev.bedrockify.forge.client.gui.BedrockGui;
import dev.bedrockify.forge.common.features.education.ChemistryLayout;
import dev.bedrockify.forge.common.features.education.ChemistryStationKind;
import dev.bedrockify.forge.common.features.education.ChemistryStationScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

public final class ChemistryStationScreen extends HandledScreen<ChemistryStationScreenHandler> {
    // Stored properties retain their existing p/n/e order. The reference UI displays p/e/n.
    private static final int[] DISPLAY = {0,2,1};
    private static final String[] ICON = {"proton","electron","neutron"};
    private static final String[] LABEL = {"protons","electrons","neutrons"};
    private final TextFieldWidget[] counts = new TextFieldWidget[3];
    private boolean syncing;
    private ButtonWidget combine;
    private int lastReaction;
    private long reactionStarted = -10000;

    public ChemistryStationScreen(ChemistryStationScreenHandler handler,PlayerInventory player,Text title) {
        super(handler,player,title);
        backgroundWidth=handler.layout.width(); backgroundHeight=handler.layout.height();
    }
    private void send(int id) { if(client!=null && client.interactionManager!=null)client.interactionManager.clickButton(handler.syncId,id); }
    /** Only the enlarged constructor result uses the periodic-table face, as in Bedrock. */
    public boolean drawElementResult(DrawContext c,net.minecraft.screen.slot.Slot slot) {
        if(handler.type != ChemistryStationKind.ELEMENT_CONSTRUCTOR || slot.id!=9 || !slot.hasStack()
                || !(slot.getStack().getItem() instanceof dev.bedrockify.forge.common.features.education.ElementBlockItem))return false;
        var id=net.minecraft.registry.Registries.ITEM.getId(slot.getStack().getItem());
        if(!id.getNamespace().equals("bedrockify") || !id.getPath().matches("element_[0-9]+"))return false;
        c.drawTexture(new net.minecraft.util.Identifier("bedrockify","textures/block/"+id.getPath()+".png"),slot.x,slot.y,16,16,0,0,32,32,32,32);
        c.drawItemInSlot(textRenderer,slot.getStack(),slot.x,slot.y);
        return true;
    }
    private void particle(int index,int value) {
        value=Math.max(0,Math.min(ChemistryLayout.particleLimit(index),value));
        if(handler.particle(index)==value)return;
        handler.displayParticle(index,value);
        send(ChemistryLayout.particleButton(index,value));
    }
    @Override protected void init() {
        super.init();
        addDrawableChild(ChemistrySprites.close(x+backgroundWidth-18,y+7,this::close));
        lastReaction=handler.reaction();
        if(handler.type == ChemistryStationKind.LAB_TABLE)
            combine=addDrawableChild(ChemistrySprites.button(x+38,y+92,100,14,Text.translatable("bedrockify.education.combine"),()->send(0)));
        if(handler.type != ChemistryStationKind.ELEMENT_CONSTRUCTOR)return;
        for(int col=0;col<3;col++) {
            int index=DISPLAY[col],column=x+4+col*26;
            var field=new TextFieldWidget(textRenderer,column+2,y+32,20,16,Text.translatable("bedrockify.education."+LABEL[col]));
            field.setDrawsBackground(false);field.setEditableColor(0xffffffff);field.setMaxLength(3);
            field.setTextPredicate(value->value.matches("[0-9]{0,3}"));
            field.setText(Integer.toString(handler.particle(index)));
            field.setChangedListener(value->{if(!syncing && !value.isEmpty())particle(index,Integer.parseInt(value));});
            counts[index]=addDrawableChild(field);
            addDrawableChild(ChemistrySprites.button(column+6,y+53,12,12,Text.literal("+"),()->particle(index,handler.particle(index)+1)));
            addDrawableChild(new ParticleSlider(column+4,y+65,ChemistryLayout.particleLimit(index),field.getMessage(),()->handler.particle(index),value->particle(index,value)));
            addDrawableChild(ChemistrySprites.button(column+6,y+203,12,12,Text.literal("−"),()->particle(index,handler.particle(index)-1)));
        }
    }
    @Override protected void handledScreenTick() {
        super.handledScreenTick();
        if(combine!=null)combine.active=handler.canCombine();
        if(lastReaction!=handler.reaction()) { lastReaction=handler.reaction();reactionStarted=Util.getMeasuringTimeMs(); }
        syncing=true;
        for(int i=0;i<3;i++)if(counts[i]!=null) {
            counts[i].tick();
            if(!counts[i].isFocused())counts[i].setText(Integer.toString(handler.particle(i)));
        }
        syncing=false;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(getFocused() instanceof TextFieldWidget field && field.isFocused() && key!=GLFW.GLFW_KEY_ESCAPE && key!=GLFW.GLFW_KEY_TAB) {
            if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)field.setFocused(false);
            else field.keyPressed(key,scan,modifiers);
            return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(handler.type == ChemistryStationKind.ELEMENT_CONSTRUCTOR && button<=1 && isPointWithinBounds(208,86,24,24,mx,my)) {
            if(client!=null && client.interactionManager!=null)client.interactionManager.clickSlot(handler.syncId,9,button,
                    hasShiftDown()?net.minecraft.screen.slot.SlotActionType.QUICK_MOVE:net.minecraft.screen.slot.SlotActionType.PICKUP,client.player);
            return true;
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(getFocused() instanceof ParticleSlider slider && button==0)return slider.mouseDragged(mx,my,button,dx,dy);
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override protected void drawBackground(DrawContext c,float delta,int mx,int my) {
        BedrockGui.panel(c,x,y,backgroundWidth,backgroundHeight,false);
        if(handler.type == ChemistryStationKind.ELEMENT_CONSTRUCTOR)drawElement(c);
        else if(handler.type == ChemistryStationKind.COMPOUND_CREATOR) {
            ChemistrySprites.nineSlice(c,"compound_background",x+30,y+15,84,68,79,61,27);
            ChemistrySprites.nineSlice(c,"dialog_indent_horiz",x+111,y+48,27,6,4,4,1);
        } else drawLab(c);
        for(var slot:handler.slots)if(slot.isEnabled()) {
            if(slot.id==9 && handler.type == ChemistryStationKind.ELEMENT_CONSTRUCTOR)continue;
            ChemistrySprites.cell(c,x+slot.x-1,y+slot.y-1,18,false);
        }
    }
    private void drawElement(DrawContext c) {
        ChemistrySprites.cell(c,x+83,y+18,110,false);
        for(int col=0;col<3;col++) {
            int column=x+4+col*26;
            c.fill(column,y+30,column+24,y+50,0xff191919);
            c.fill(column+1,y+31,column+23,y+49,0xff5b5b5b);
            c.fill(column+1,y+48,column+23,y+49,0xffaaaaaa);
            ChemistrySprites.image(c,ICON[col],column+8,y+17,8,8,9,9);
        }
        ChemistrySprites.image(c,"microscope",x+194,y+18,50,50,32,32);
        ChemistrySprites.nineSlice(c,"dialog_indent_horiz",x+193,y+97,17,4,4,4,1);
        ChemistrySprites.cell(c,x+208,y+86,24,false);
        atom(c);
    }
    private void atom(DrawContext c) {
        int cx=x+138,cy=y+73,electrons=handler.particle(2),remaining=electrons;
        int[] capacities={2,8,18,32,32,18,8};
        int shells=1,total=0;
        while(shells<capacities.length && (total+=capacities[shells-1])<electrons)shells++;
        double time=Util.getMeasuringTimeMs()/3000.0;
        for(int shell=0;shell<shells;shell++) {
            int radius=shells==1?38:14+shell*27/Math.max(1,shells-1);
            ChemistrySprites.ring(c,cx,cy,radius,0xff080808);
            int count=Math.min(remaining,capacities[shell]);remaining-=count;
            for(int i=0;i<count;i++) {
                double angle=time*(shell%2==0?1:-1)+Math.PI*2*i/Math.max(1,count);
                ChemistrySprites.image(c,"electron",cx+(int)(Math.cos(angle)*radius)-4,cy+(int)(Math.sin(angle)*radius)-4,8,8,9,9);
            }
        }
        int p=handler.particle(0),n=handler.particle(1),shown=Math.min(24,p+n);
        for(int i=0;i<shown;i++) {
            double angle=i*2.399963, radius=shown<=2?i*7:Math.sqrt(i)*2.7;
            boolean proton=(i+1)*p/Math.max(1,p+n)>i*p/Math.max(1,p+n);
            ChemistrySprites.image(c,proton?"proton":"neutron",cx+(int)(Math.cos(angle)*radius)-4,cy+(int)(Math.sin(angle)*radius)-4,8,8,9,9);
        }
    }
    private void drawLab(DrawContext c) {
        String[] tools={"beaker","flask","jar"};int[] frames={17,21,29};
        long elapsed=Util.getMeasuringTimeMs()-reactionStarted;
        for(int i=0;i<3;i++) {
            int frame=(int)(elapsed/100);
            if(elapsed>=0 && frame<frames[i])c.drawTexture(ChemistrySprites.texture(tools[i]+"_active"),x+13+i*50,y+17,45,45,frame*45,0,45,45,frames[i]*45,45);
            else ChemistrySprites.image(c,tools[i]+"_default",x+13+i*50,y+17,45,45,45,45);
        }
    }
    @Override protected void drawForeground(DrawContext c,int mx,int my) {
        c.drawText(textRenderer,title,(backgroundWidth-textRenderer.getWidth(title))/2,5,0xff4c4c4c,false);
        if(handler.type == ChemistryStationKind.ELEMENT_CONSTRUCTOR) {
            String[] letters={"p","e","n"};
            for(int col=0;col<3;col++)c.drawText(textRenderer,letters[col],4+col*26,17,0xff4c4c4c,false);
        } else if(handler.type == ChemistryStationKind.LAB_TABLE)c.drawText(textRenderer,playerInventoryTitle,7,112,0xff4c4c4c,false);
    }
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        renderBackground(c);super.render(c,mx,my,delta);drawMouseoverTooltip(c,mx,my);
    }
}
