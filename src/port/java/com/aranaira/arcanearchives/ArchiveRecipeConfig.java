package com.aranaira.arcanearchives;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

/** Optional server-owned recipe edits, including targets without CraftTweaker. */
public final class ArchiveRecipeConfig {
    public static final String FILE="arcanearchives-gemcutting.json";
    public static void load(Level level){
        Path file=FMLPaths.CONFIGDIR.get().resolve(FILE);
        if(!Files.exists(file))return;
        if(net.neoforged.fml.ModList.get().isLoaded("crafttweaker")){
            com.mojang.logging.LogUtils.getLogger().warn("Ignoring {} because CraftTweaker owns Gemcutter edits on this server",file);return;
        }
        try{
            if(Files.size(file)>1048576)throw new IllegalArgumentException("Recipe config exceeds1MiB");
            int count=apply(JsonParser.parseString(Files.readString(file)).getAsJsonObject(),level);
            com.mojang.logging.LogUtils.getLogger().info("Applied {} Arcane Archives Gemcutter config edits",count);
        }catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("Could not apply "+file+"; recipe edits rolled back",error);}
    }
    public static int apply(JsonObject document,Level level){
        var before=ArchiveRecipes.server();int epoch=ArchiveRecipes.generation();
        try{
            var edits=document.getAsJsonArray("recipes");
            if(edits==null||edits.size()>ArchiveRecipes.MAX_RECIPES)throw new IllegalArgumentException("Expected recipes array with at most512 edits");
            for(var element:edits){
                var edit=element.getAsJsonObject();String op=edit.get("op").getAsString();var output=stack(edit.getAsJsonObject("output"),level);
                if(op.equals("remove")){ArchiveRecipes.remove(output);continue;}
                if(!op.equals("add")&&!op.equals("replace"))throw new IllegalArgumentException("Unknown Gemcutter operation: "+op);
                var requirements=new ArrayList<GemcutterRecipes.Requirement>();
                for(var input:edit.getAsJsonArray("inputs")){
                    var spec=input.getAsJsonObject();int count=spec.has("count")?spec.get("count").getAsInt():1;
                    if(spec.has("stack")){
                        var exact=stack(spec,level);
                        requirements.add(new GemcutterRecipes.Requirement("arcanearchives:custom/stack",false,count,value->ItemStack.isSameItemSameComponents(value,exact),List.of(exact)));
                    }else{
                        boolean tag=spec.has("tag");String id=ResourceLocation.tryParse(spec.get(tag?"tag":"item").getAsString()).toString();
                        if(!tag&&!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)))throw new IllegalArgumentException("Unknown ingredient: "+id);
                        requirements.add(new GemcutterRecipes.Requirement(id,tag,count));
                    }
                }
                var recipe=new GemcutterRecipes.Recipe(ArchiveRecipes.key(edit.get("name").getAsString()),BuiltInRegistries.ITEM.getKey(output.getItem()).toString(),output.getCount(),false,requirements,output);
                ArchiveRecipeCodec.encode(recipe,level); // Reject oversized display payloads before committing edits.
                ArchiveRecipes.add(recipe,op.equals("replace"));
            }
            return edits.size();
        }catch(RuntimeException error){ArchiveRecipes.restore(before,epoch);throw error;}
    }
    private static ItemStack stack(JsonObject spec,Level level){
        ItemStack result;
        if(spec.has("stack")){
            try{result=StackData.load(TagParser.parseTag(spec.get("stack").getAsString()),level);}
            catch(Exception error){throw new IllegalArgumentException("Invalid native item stack SNBT",error);}
        }else{
            var id=ResourceLocation.tryParse(spec.get("item").getAsString());
            if(id==null||!BuiltInRegistries.ITEM.containsKey(id))throw new IllegalArgumentException("Unknown output item");
            result=new ItemStack(BuiltInRegistries.ITEM.get(id));
        }
        if(spec.has("count"))result.setCount(spec.get("count").getAsInt());
        if(result.isEmpty()||result.getCount()>result.getMaxStackSize())throw new IllegalArgumentException("Configured stack must fit one native item stack");
        return result;
    }
}
