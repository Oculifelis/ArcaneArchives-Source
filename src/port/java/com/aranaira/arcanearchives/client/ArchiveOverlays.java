package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.*;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid=AA.ID,value=Dist.CLIENT)
public final class ArchiveOverlays {
    public record Line(Vec3 from,Vec3 to,float width,float r,float g,float b){}
    private static final ContextKey<List<Line>> TRACKING=new ContextKey<>(AA.id("tracking"));
    @SubscribeEvent public static void extract(ExtractLevelRenderStateEvent event){
        var mc=Minecraft.getInstance();List<Line> lines=new ArrayList<>();
        if(mc.player!=null){
            var from=mc.player.getPosition(event.getDeltaTracker().getGameTimeDeltaPartialTick(false)).add(0,1,0);
            float[] color=TrackingGeometry.color(event.getLevel().getGameTime());
            for(var target:ArchiveViews.targets()){
                var to=Vec3.atCenterOf(target.position());
                if(target.dimension().equals(event.getLevel().dimension().identifier().toString())&&from.distanceTo(to)<=ClientPreferences.trackingDistance)lines.add(new Line(from,to,TrackingGeometry.width(from,to),color[0],color[1],color[2]));
            }
        }
        for(var edge:BrazierRange.edges())lines.add(new Line(edge.from(),edge.to(),2,.6f,.85f,1));
        event.getRenderState().setRenderData(TRACKING,List.copyOf(lines));
    }
    @SubscribeEvent public static void submit(SubmitCustomGeometryEvent event){
        var lines=event.getLevelRenderState().getRenderData(TRACKING);if(lines==null||lines.isEmpty())return;
        var camera=event.getLevelRenderState().cameraRenderState.pos;
        event.getSubmitNodeCollector().submitCustomGeometry(event.getPoseStack(),TrackingRenderType.TYPE,(pose,out)->{
            for(var line:lines){
                var normal=line.to().subtract(line.from()).normalize();if(normal.lengthSqr()==0)continue;
                for(var point:List.of(line.from().subtract(camera),line.to().subtract(camera)))out.addVertex(pose,(float)point.x,(float)point.y,(float)point.z).setColor(line.r(),line.g(),line.b(),1).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z).setLineWidth(line.width());
            }
        });
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){drawHud(event.getGuiGraphics());}
    public static void drawHud(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.options.hideGui||!ClientPreferences.showHud)return;
        int x=mc.getWindow().getGuiScaledWidth()/2+10,y=mc.getWindow().getGuiScaledHeight()/2+15;
        int row=0;
        for(var gem:GemRules.held(mc.player)){
            g.item(gem,x,y+row*20);int charge=GemItem.charge(gem),maximum=GemItem.maximum(gem);
            g.fill(x+18,y+4+row*20,x+62,y+10+row*20,0xaa24172f);g.fill(x+19,y+5+row*20,x+19+42*charge/maximum,y+9+row*20,0xffd8b5ff);
            g.text(mc.font,charge+"/"+maximum,x+65,y+4+row*20,0xffffffff,true);row++;
        }
        if(mc.hitResult instanceof BlockHitResult hit&&mc.level.getBlockEntity(hit.getBlockPos()) instanceof ArchiveBlockEntity tile&&tile.kind().equals("radiant_trove")){
            var stored=tile.inventory.getStackInSlot(0);if(stored.isEmpty())stored=tile.reference;
            if(!stored.isEmpty()){g.item(stored,x-100,y);g.text(mc.font,stored.getHoverName(),x-80,y-10,0xffffffff,true);g.text(mc.font,"x "+tile.inventory.getStackInSlot(0).getCount(),x-80,y+3,0xffffffff,true);g.text(mc.font,"Upgrades: "+tile.upgradeStage,x-80,y+15,0xffe7be6c,true);}
        }
    }
}
