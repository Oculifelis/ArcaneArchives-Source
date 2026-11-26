package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
public final class ArchiveRecipeClient {
    private static final List<CompoundTag> pending=new ArrayList<>();
    private static int revision=-1,count;
    private static boolean ready,applied;
    public static boolean received(){return applied;}
    public static void clear(){pending.clear();ready=false;applied=false;revision=-1;ArchiveRecipes.clearClient();}
    public static void accept(CompoundTag tag){
        String kind=tag.getString("Kind");int incoming=tag.getInt("Revision");
        if(kind.equals("recipes_begin")){pending.clear();ready=false;revision=incoming;count=tag.getInt("Count");if(count<0||count>ArchiveRecipes.MAX_RECIPES)throw new IllegalArgumentException("Invalid recipe count");}
        else if(incoming==revision&&kind.equals("recipes_entry")&&pending.size()<count)pending.add(tag.copy());
        else if(incoming==revision&&kind.equals("recipes_end")){if(pending.size()!=count)throw new IllegalArgumentException("Incomplete recipe snapshot");ready=true;tick();}
    }
    public static void tick(){
        var level=Minecraft.getInstance().level;if(!ready||level==null)return;
        var recipes=new ArrayList<GemcutterRecipes.Recipe>();for(var tag:pending)recipes.add(ArchiveRecipeCodec.decode(tag,level));
        ready=false;applied=true;pending.clear();ArchiveRecipes.setClient(recipes);
    }
}
