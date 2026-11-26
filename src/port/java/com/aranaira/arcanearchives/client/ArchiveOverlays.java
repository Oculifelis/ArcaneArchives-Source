package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber(modid=AA.ID,value=Dist.CLIENT)
public final class ArchiveOverlays {
    @SubscribeEvent public static void world(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        var camera=event.getCamera().getPosition();var from=mc.player.getPosition(event.getPartialTick().getGameTimeDeltaPartialTick(false)).add(0,1,0);
        var pose=event.getPoseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();float[] color=TrackingGeometry.color(mc.level.getGameTime());
        for(var target:ArchiveViews.targets()){
            var to=Vec3.atCenterOf(target.position());if(!target.dimension().equals(mc.level.dimension().location().toString())||from.distanceTo(to)>ClientPreferences.trackingDistance)continue;
            var normal=to.subtract(from).normalize();if(normal.lengthSqr()==0)continue;
            var type=TrackingRenderType.create(TrackingGeometry.width(from,to));var out=buffers.getBuffer(type);
            for(var point:java.util.List.of(from,to))out.addVertex(pose.last(),(float)point.x,(float)point.y,(float)point.z).setColor(color[0],color[1],color[2],1).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
            buffers.endBatch(type);
        }
        var rangeType=TrackingRenderType.create(2);var rangeOut=buffers.getBuffer(rangeType);
        for(var edge:BrazierRange.edges()){
            var normal=edge.to().subtract(edge.from()).normalize();
            for(var point:java.util.List.of(edge.from(),edge.to()))rangeOut.addVertex(pose.last(),(float)point.x,(float)point.y,(float)point.z).setColor(0.6f,0.85f,1f,1).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
        }
        buffers.endBatch(rangeType);pose.popPose();
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){drawHud(event.getGuiGraphics());}
    public static void drawHud(GuiGraphics g){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.options.hideGui||!ClientPreferences.showHud)return;
        int x=mc.getWindow().getGuiScaledWidth()/2+10,y=mc.getWindow().getGuiScaledHeight()/2+15;
        int row=0;
        for(var gem:GemRules.held(mc.player)){
            g.renderItem(gem,x,y+row*20);int charge=GemItem.charge(gem),maximum=GemItem.maximum(gem);
            g.fill(x+18,y+4+row*20,x+62,y+10+row*20,0xaa24172f);g.fill(x+19,y+5+row*20,x+19+42*charge/maximum,y+9+row*20,0xffd8b5ff);
            g.drawString(mc.font,charge+"/"+maximum,x+65,y+4+row*20,0xffffff,true);row++;
        }
        if(mc.hitResult instanceof BlockHitResult hit&&mc.level.getBlockEntity(hit.getBlockPos()) instanceof ArchiveBlockEntity tile&&tile.kind().equals("radiant_trove")){
            var stored=tile.inventory.getStackInSlot(0);if(stored.isEmpty())stored=tile.reference;
            if(!stored.isEmpty()){g.renderItem(stored,x-100,y);g.drawString(mc.font,stored.getHoverName(),x-80,y-10,0xffffff,true);g.drawString(mc.font,"x "+tile.inventory.getStackInSlot(0).getCount(),x-80,y+3,0xffffff,true);g.drawString(mc.font,"Upgrades: "+tile.upgradeStage,x-80,y+15,0xffe7be6c,true);}
        }
    }
}
