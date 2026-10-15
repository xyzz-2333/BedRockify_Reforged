package dev.bedrockify.forge.client.features.education;
import dev.bedrockify.forge.common.features.education.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
public final class UnderwaterTntRenderer extends EntityRenderer<UnderwaterTntEntity> {
    private final net.minecraft.client.render.block.BlockRenderManager blocks;
    public UnderwaterTntRenderer(EntityRendererFactory.Context context) { super(context);blocks=context.getBlockRenderManager();shadowRadius=0.5F; }
    @Override public Identifier getTexture(UnderwaterTntEntity entity) { return net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE; }
    @Override public void render(UnderwaterTntEntity entity,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider vertices,int light) {
        matrices.push();
        float remaining=entity.getFuse()-delta+1;
        if(remaining<10) {float amount=net.minecraft.util.math.MathHelper.clamp(1-remaining/10,0,1);amount*=amount;amount*=amount;matrices.scale(1+amount*0.3F,1+amount*0.3F,1+amount*0.3F);}
        matrices.translate(-0.5,0,-0.5);
        TntMinecartEntityRenderer.renderFlashingBlock(blocks,EducationContent.UNDERWATER_TNT.getDefaultState(),matrices,vertices,light,entity.getFuse()/5%2==0);
        matrices.pop();super.render(entity,yaw,delta,matrices,vertices,light);
    }
}
