package dev.bedrockify.forge.client.features.education;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Vertical slider with native focus, narration, dragging and keyboard controls. */
final class ParticleSlider extends ClickableWidget {
    private final int maximum;
    private final IntSupplier value;
    private final IntConsumer change;
    ParticleSlider(int x,int y,int maximum,Text label,IntSupplier value,IntConsumer change) {
        super(x,y,16,138,label);this.maximum=maximum;this.value=value;this.change=change;
    }
    private void point(double y) { change.accept((int)Math.round(Math.max(0,Math.min(1,(getY()+133-y)/128.0))*maximum)); }
    @Override public void onClick(double mx,double my) { point(my); }
    @Override protected void onDrag(double mx,double my,double dx,double dy) { point(my); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        int current=value.getAsInt();
        if(key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_RIGHT)change.accept(Math.min(maximum,current+1));
        else if(key==GLFW.GLFW_KEY_DOWN||key==GLFW.GLFW_KEY_LEFT)change.accept(Math.max(0,current-1));
        else if(key==GLFW.GLFW_KEY_HOME)change.accept(0);
        else if(key==GLFW.GLFW_KEY_END)change.accept(maximum);
        else return false;
        return true;
    }
    @Override public void renderButton(DrawContext c,int mx,int my,float delta) {
        int x=getX(),y=getY(),knob=y+128-(int)Math.round(value.getAsInt()*128.0/maximum);
        c.fill(x+2,y,x+14,y+138,0xff333333);c.fill(x+3,y+1,x+13,y+137,isHovered()||isFocused()?0xff777777:0xff555555);
        c.fill(x,knob,x+16,knob+10,0xff333333);c.fill(x+1,knob+1,x+15,knob+9,0xfff2f2f2);c.fill(x+2,knob+2,x+14,knob+8,0xffc6c6c6);
    }
    @Override protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }
    @Override protected net.minecraft.text.MutableText getNarrationMessage() { return getMessage().copy().append(": "+value.getAsInt()); }
}
