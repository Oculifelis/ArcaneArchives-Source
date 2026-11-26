package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jspecify.annotations.Nullable;

public final class ArchiveStorageRenderer implements BlockEntityRenderer<ArchiveBlockEntity,ArchiveStorageRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item=new ItemStackRenderState();
        Direction facing=Direction.NORTH;
        TextureAtlasSprite fluidSprite;
        float fraction;
        int color;
    }
    public ArchiveStorageRenderer(BlockEntityRendererProvider.Context context){}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ArchiveBlockEntity tile,State state,float partial,Vec3 camera,ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress){
        BlockEntityRenderer.super.extractRenderState(tile,state,partial,camera,breakProgress);
        // Every branch resets its state; renderer instances are shared by all blocks.
        state.item.clear();state.fluidSprite=null;state.fraction=0;state.facing=tile.displayFacing;
        var mc=Minecraft.getInstance();
        if(tile.kind().equals("radiant_chest")&&!tile.displayStack.isEmpty()&&!state.facing.getAxis().isVertical())mc.getItemModelResolver().updateForTopItem(state.item,tile.displayStack,ItemDisplayContext.FIXED,tile.getLevel(),null,0);
        if(tile.kind().equals("radiant_tank")&&!tile.tank.getFluid().isEmpty()){
            var fluid=tile.tank.getFluid();var model=mc.getModelManager().getFluidStateModelSet().get(fluid.getFluid().defaultFluidState());
            state.fluidSprite=model.stillMaterial().sprite();
            state.color=model.fluidTintSource()==null?0xffffffff:model.fluidTintSource().colorAsStack(fluid);state.fraction=(float)tile.tank.getFluidAmount()/tile.tank.getCapacity();
        }
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(!state.item.isEmpty()){
            pose.pushPose();var facing=state.facing;
            pose.translate(.5+facing.getStepX()*.47,.435,.5+facing.getStepZ()*.47);
            pose.mulPose(Axis.YP.rotationDegrees(switch(facing){case SOUTH->180;case WEST->90;case EAST->270;default->0;}));pose.scale(.6f,.6f,.6f);
            state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
        }
        if(state.fluidSprite!=null){
            var sprite=state.fluidSprite;float fraction=state.fraction;int color=state.color,light=state.lightCoords;
            collector.submitCustomGeometry(pose,RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS),(p,out)->FluidGeometry.draw(p,out,sprite,fraction,color,light));
        }
    }
}
