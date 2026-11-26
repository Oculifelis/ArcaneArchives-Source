package com.aranaira.arcanearchives.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/** Session-only outline; does not request or load distant chunks. */
public final class BrazierRange {
    public record Edge(Vec3 from,Vec3 to){}
    private static BlockPos position;
    private static String dimension="";
    private static int radius;
    public static void clear(){position=null;}
    public static boolean showing(){return position!=null;}
    public static boolean showing(long pos,String dim){return position!=null&&position.asLong()==pos&&dimension.equals(dim);}
    public static void accept(CompoundTag tag,boolean toggle){
        var pos=BlockPos.of(tag.getLongOr("Position",0L));var dim=tag.getStringOr("Dimension","");
        if(toggle&&pos.equals(position)&&dim.equals(dimension)){clear();return;}
        if(!toggle&&(!pos.equals(position)||!dim.equals(dimension)))return;
        position=pos;dimension=dim;radius=Math.max(0,Math.min(300,tag.getIntOr("Radius",0)));
    }
    public static List<Edge> edges(){
        var level=Minecraft.getInstance().level;
        if(position==null||level==null||!dimension.equals(level.dimension().identifier().toString()))return List.of();
        // Routing is a horizontal cylinder; world height follows the target dimension.
        double low=level.getMinY(),high=level.getMaxY();
        var result=new ArrayList<Edge>();
        for(int i=0;i<96;i++){
            double a=i*Math.PI/48,b=(i+1)*Math.PI/48;
            double x=position.getX()+.5+Math.cos(a)*radius,z=position.getZ()+.5+Math.sin(a)*radius;
            double nx=position.getX()+.5+Math.cos(b)*radius,nz=position.getZ()+.5+Math.sin(b)*radius;
            result.add(new Edge(new Vec3(x,low,z),new Vec3(nx,low,nz)));result.add(new Edge(new Vec3(x,high,z),new Vec3(nx,high,nz)));
            result.add(new Edge(new Vec3(x,position.getY()+.1,z),new Vec3(nx,position.getY()+.1,nz)));
            if(i%12==0)result.add(new Edge(new Vec3(x,low,z),new Vec3(x,high,z)));
        }
        return result;
    }
}
