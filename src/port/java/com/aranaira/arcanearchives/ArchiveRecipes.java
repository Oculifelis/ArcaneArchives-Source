package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server definitions and client display snapshots remain separate in an integrated JVM. */
public final class ArchiveRecipes {
    public static final int MAX_RECIPES=512;
    private static volatile List<GemcutterRecipes.Recipe> server=GemcutterRecipes.ALL,client=GemcutterRecipes.ALL;
    private static int generation,revision;
    private static boolean dirty;
    public static String key(String id){var result=ResourceLocation.tryParse(id.contains(":")?id:AA.ID+":"+id);if(result==null)throw new IllegalArgumentException("Invalid recipe name: "+id);return result.toString();}
    public static List<GemcutterRecipes.Recipe> server(){return ArchiveLegacyConfig.arsenal()?server:server.stream().filter(r->!ArchiveLegacyConfig.arsenalItem(r.result().getItem())).toList();}
    public static List<GemcutterRecipes.Recipe> client(){return client;}
    public static List<GemcutterRecipes.Recipe> forLevel(Level level){return level.isClientSide?client:server();}
    public static int generation(){return generation;}
    public static void setClient(List<GemcutterRecipes.Recipe> recipes){var old=client;client=List.copyOf(recipes);ViewMessages.recipes.accept(old,client);}
    public static void clearClient(){setClient(GemcutterRecipes.ALL);}
    public static void resetServer(){server=GemcutterRecipes.ALL;generation++;revision++;dirty=true;}
    public static void restore(List<GemcutterRecipes.Recipe> snapshot,int epoch){if(epoch==generation){server=List.copyOf(snapshot);revision++;dirty=true;}}
    public static void add(GemcutterRecipes.Recipe recipe,boolean replace){
        String id=key(recipe.id());var result=recipe.result();
        if(result.isEmpty()||result.getCount()>result.getMaxStackSize())throw new IllegalArgumentException("Output must fit one native item stack");
        var definitions=new ArrayList<>(server);int at=-1;
        for(int i=0;i<definitions.size();i++)if(key(definitions.get(i).id()).equals(id)){at=i;break;}
        if(replace&&at<0)throw new IllegalArgumentException("Recipe does not exist: "+id);
        if(!replace&&at>=0)throw new IllegalArgumentException("Recipe already exists: "+id);
        if(at<0){if(definitions.size()>=MAX_RECIPES)throw new IllegalArgumentException("At most512 Gemcutter recipes are supported");definitions.add(recipe);}else definitions.set(at,recipe);
        server=List.copyOf(definitions);revision++;dirty=true;
    }
    public static void remove(ItemStack output){
        var definitions=new ArrayList<>(server);boolean removed=definitions.removeIf(recipe->{var result=recipe.result();return result.getCount()==output.getCount()&&ItemStack.isSameItemSameComponents(result,output);});
        if(!removed)throw new IllegalArgumentException("No Gemcutter recipe has that exact output and count");server=List.copyOf(definitions);revision++;dirty=true;
    }
    public static void flush(MinecraftServer minecraft){
        if(!dirty)return;dirty=false;
        for(var player:minecraft.getPlayerList().getPlayers()){
            if(player.containerMenu instanceof ArchiveMenu menu&&menu.station==1)player.closeContainer();sync(player);
        }
    }
    public static void sync(ServerPlayer player){
        var visible=server();
        var start=new net.minecraft.nbt.CompoundTag();start.putString("Kind","recipes_begin");start.putInt("Revision",revision);start.putInt("Count",visible.size());ArchiveNetwork.view(player,start);
        for(var recipe:visible){var tag=ArchiveRecipeCodec.encode(recipe,player.level());tag.putString("Kind","recipes_entry");tag.putInt("Revision",revision);ArchiveNetwork.view(player,tag);}
        var end=new net.minecraft.nbt.CompoundTag();end.putString("Kind","recipes_end");end.putInt("Revision",revision);ArchiveNetwork.view(player,end);
    }
}
