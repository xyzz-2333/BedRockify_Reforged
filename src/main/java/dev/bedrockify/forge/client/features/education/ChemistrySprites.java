package dev.bedrockify.forge.client.features.education;

import dev.bedrockify.forge.client.features.survivalInventory.SurvivalSprites;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Original Bedrock images and classic 18-pixel cells. No global render-state changes. */
public final class ChemistrySprites {
    private ChemistrySprites() {}
    public static Identifier texture(String name) { return new Identifier("bedrockify", "textures/gui/chemistry/"+name+".png"); }
    public static void image(DrawContext c, String name, int x, int y, int w, int h, int tw, int th) {
        c.drawTexture(texture(name),x,y,w,h,0,0,tw,th,tw,th);
    }
    public static void cell(DrawContext c, int x, int y, int size, boolean locked) {
        c.fill(x,y,x+size,y+size,locked ? 0xffb85454 : 0xff8b8b8b);
        c.fill(x,y,x+size-1,y+1,0xff373737);c.fill(x,y,x+1,y+size-1,0xff373737);
        c.fill(x+1,y+size-1,x+size,y+size,0xffffffff);c.fill(x+size-1,y+1,x+size,y+size,0xffffffff);
    }
    public static void nineSlice(DrawContext c, String name, int x, int y, int w, int h, int sw, int sh, int b) {
        for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
            int sx=col==0?0:col==1?b:sw-b, sy=row==0?0:row==1?b:sh-b;
            int sourceW=col==1?sw-2*b:b, sourceH=row==1?sh-2*b:b;
            int dx=x+(col==0?0:col==1?b:w-b), dy=y+(row==0?0:row==1?b:h-b);
            int dw=col==1?w-2*b:b, dh=row==1?h-2*b:b;
            c.drawTexture(texture(name),dx,dy,dw,dh,sx,sy,sourceW,sourceH,sw,sh);
        }
    }
    public static ButtonWidget button(int x, int y, int w, int h, Text text, Runnable action) {
        return new ButtonWidget(x,y,w,h,text,b->action.run(),narration->narration.get()) {
            @Override public void renderButton(DrawContext c,int mx,int my,float delta) {
                int face=active ? (isHovered()||isFocused() ? 0xff80b54b : 0xffc6c6c6) : 0xff777777;
                SurvivalSprites.frame(c,getX(),getY(),getWidth(),getHeight(),face);
                var font=MinecraftClient.getInstance().textRenderer;
                c.drawText(font,getMessage(),getX()+(getWidth()-font.getWidth(getMessage()))/2,getY()+(getHeight()-8)/2,active?0xff4c4c4c:0xff555555,false);
            }
        };
    }
    public static ButtonWidget close(int x,int y,Runnable action) {
        return new ButtonWidget(x,y,12,12,Text.translatable("gui.close"),b->action.run(),narration->narration.get()) {
            @Override public void renderButton(DrawContext c,int mx,int my,float delta) {
                int color=isHovered()||isFocused()?0xff111111:0xff4c4c4c;
                for(int i=0;i<5;i++) { c.fill(getX()+3+i,getY()+3+i,getX()+4+i,getY()+4+i,color);c.fill(getX()+7-i,getY()+3+i,getX()+8-i,getY()+4+i,color); }
            }
        };
    }
    public static void ring(DrawContext c,int cx,int cy,int radius,int color) {
        int x=radius,y=0,error=1-radius;
        while(x>=y) {
            int[][] points={{x,y},{y,x},{-y,x},{-x,y},{-x,-y},{-y,-x},{y,-x},{x,-y}};
            for(var p:points)c.fill(cx+p[0],cy+p[1],cx+p[0]+1,cy+p[1]+1,color);
            y++;if(error<0)error+=2*y+1;else{x--;error+=2*(y-x)+1;}
        }
    }
}
