package com.aranaira.arcanearchives.client;

import net.minecraft.world.phys.Vec3;

public final class TrackingGeometry {
    private static final float[][] COLORS={{1,.5f,.5f},{1,.75f,.5f},{1,1,.5f},{.5f,1,.6f},{.5f,1,1},{.5f,.65f,1},{.8f,.5f,1},{1,.55f,1}};
    public static float[] color(long time){int t=(int)Math.floorMod(time,96),a=t/12,b=(a+1)%8;float u=(t%12)/12f;return new float[]{COLORS[a][0]+(COLORS[b][0]-COLORS[a][0])*u,COLORS[a][1]+(COLORS[b][1]-COLORS[a][1])*u,COLORS[a][2]+(COLORS[b][2]-COLORS[a][2])*u};}
    public static float width(Vec3 from,Vec3 to){double n=Math.max(0,Math.min(1,(from.distanceTo(to)-10)/60));return Math.max(.5f,(float)((1-(n*.7+.3))*10));}
}
