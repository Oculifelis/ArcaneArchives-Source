package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

public final class ArchiveStorageRenderer implements BlockEntityRenderer<ArchiveBlockEntity> {
    public ArchiveStorageRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(ArchiveBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(tile.kind().equals("radiant_chest")&&!tile.displayStack.isEmpty()&&!tile.displayFacing.getAxis().isVertical()){
            pose.pushPose();var facing=tile.displayFacing;
            pose.translate(.5+facing.getStepX()*.47,.435,.5+facing.getStepZ()*.47);
            pose.mulPose(Axis.YP.rotationDegrees(switch(facing){case SOUTH->180;case WEST->90;case EAST->270;default->0;}));pose.scale(.6f,.6f,.6f);
            Minecraft.getInstance().getItemRenderer().renderStatic(tile.displayStack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,tile.getLevel(),0);pose.popPose();
        }
        if(tile.kind().equals("radiant_tank")&&!tile.tank.getFluid().isEmpty()){
            var fluid=tile.tank.getFluid();var extension=IClientFluidTypeExtensions.of(fluid.getFluid());
            var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(extension.getStillTexture(fluid));
            FluidGeometry.draw(pose.last(),buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),sprite,(float)tile.tank.getFluidAmount()/tile.tank.getCapacity(),extension.getTintColor(fluid),light);
        }
    }
}
