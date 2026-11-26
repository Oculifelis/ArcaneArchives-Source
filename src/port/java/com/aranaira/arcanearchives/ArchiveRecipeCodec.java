package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Each recipe is sent independently, with bounded display alternatives. Server predicates stay on the server. */
public final class ArchiveRecipeCodec {
    public static CompoundTag encode(GemcutterRecipes.Recipe recipe,Level level){
        var tag=new CompoundTag();tag.putString("Id",recipe.id());tag.putBoolean("Creator",recipe.creator());
        tag.put("Output",StackData.save(recipe.result(),level));var requirements=new ListTag();
        for(var requirement:recipe.ingredients()){
            var entry=new CompoundTag();entry.putString("Id",requirement.id());entry.putBoolean("Tag",requirement.tag());entry.putInt("Count",requirement.count());
            var examples=new ListTag();for(var stack:requirement.examples().stream().limit(16).toList())examples.add(StackData.save(stack.copyWithCount(1),level));
            entry.put("Examples",examples);requirements.add(entry);
        }
        tag.put("Ingredients",requirements);
        if(tag.toString().length()>262144)throw new IllegalArgumentException("Recipe display data exceeds the network size limit: "+recipe.id());
        return tag;
    }
    public static GemcutterRecipes.Recipe decode(CompoundTag tag,Level level){
        ItemStack output=StackData.load(tag.getCompoundOrEmpty("Output"),level);if(output.isEmpty())throw new IllegalArgumentException("Missing recipe output");
        var requirements=new ArrayList<GemcutterRecipes.Requirement>();var list=tag.getListOrEmpty("Ingredients");
        if(list.size()>18)throw new IllegalArgumentException("Too many ingredients");
        for(int i=0;i<list.size();i++){
            var entry=list.getCompoundOrEmpty(i);var examples=new ArrayList<ItemStack>();var stacks=entry.getListOrEmpty("Examples");
            for(int n=0;n<Math.min(16,stacks.size());n++){var stack=StackData.load(stacks.getCompoundOrEmpty(n),level);if(!stack.isEmpty())examples.add(stack);}
            requirements.add(new GemcutterRecipes.Requirement(entry.getStringOr("Id",""),entry.getBooleanOr("Tag",false),entry.getIntOr("Count",0),null,examples));
        }
        return new GemcutterRecipes.Recipe(tag.getStringOr("Id",""),net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(output.getItem()).toString(),output.getCount(),tag.getBooleanOr("Creator",false),requirements,output);
    }
}
