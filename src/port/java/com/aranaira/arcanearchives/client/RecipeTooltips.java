package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
public final class RecipeTooltips {
    public static List<Component> forSlot(ArchiveMenu menu,int slot){
        if(menu.station!=1)return List.of();var recipe=menu.displayedRecipe(slot);if(recipe==null)return List.of();
        List<Component> lines=new ArrayList<>();lines.add(recipe.result().getHoverName());
        for(var req:recipe.ingredients()){
            var examples=req.examples();Component label=req.tag()?Component.literal("Any "+req.id().substring(req.id().lastIndexOf(':')+1).replace('_',' ')):
                    examples.isEmpty()?Component.literal("Custom ingredient"):examples.get(0).getHoverName();
            lines.add(Component.literal(req.count()+" x ").append(label));
        }
        return lines;
    }
}
