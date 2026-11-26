package com.aranaira.arcanearchives.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.texture.*;

/** Six fluid faces using the original tank's internal bounds, independent of the static OBJ shell. */
public final class FluidGeometry {
    public static void draw(PoseStack.Pose pose,VertexConsumer out,TextureAtlasSprite sprite,float amount,int color,int light){
        float a=.08f,b=.76f,y=.05f,h=Math.max(y,Math.min(.9f,amount*.9f));
        if(h<=y)return;
        float[][][] faces={
            {{a,y,a},{b,y,a},{b,h,a},{a,h,a}},{{b,y,b},{a,y,b},{a,h,b},{b,h,b}},
            {{a,y,b},{a,y,a},{a,h,a},{a,h,b}},{{b,y,a},{b,y,b},{b,h,b},{b,h,a}},
            {{a,h,a},{b,h,a},{b,h,b},{a,h,b}},{{a,y,b},{b,y,b},{b,y,a},{a,y,a}}
        };
        float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        int r=(color>>16)&255,g=(color>>8)&255,bl=color&255,alpha=(color>>>24)&255;if(alpha==0)alpha=255;
        for(int f=0;f<6;f++)for(int i=0;i<4;i++){
            float u=sprite.getU0()+(sprite.getU1()-sprite.getU0())*(i==0||i==3?a:b);
            float v=sprite.getV0()+(sprite.getV1()-sprite.getV0())*(i<2?(f<4?h:b):a);
            float[] p=faces[f][i],n=normals[f];
            out.vertex(pose.pose(),p[0],p[1],p[2]).color(r,g,bl,alpha).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),n[0],n[1],n[2]).endVertex();
        }
    }
}
