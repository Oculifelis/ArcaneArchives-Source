package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public final class ArchiveItemVisuals{
    public static boolean state(String kind,ItemStack stack){
        return switch(kind){
            case "dun"->stack.getItem() instanceof GemItem&&GemItem.charge(stack)==0;
            case "colourblind"->ClientPreferences.colourblind;
            case "linked"->StackData.data(stack).contains("TankPos");
            case "empty_mode"->StackData.data(stack).getBoolean("EmptyMode");
            default->false;
        };
    }
    public static int fluidColor(ItemStack stack){
        var id=ResourceLocation.tryParse(StackData.data(stack).getString("LinkedFluid"));if(id==null)return 0xffffffff;
        var fluid=BuiltInRegistries.FLUID.get(id);
        return fluid==net.minecraft.world.level.material.Fluids.EMPTY?0xffffffff:IClientFluidTypeExtensions.of(fluid).getTintColor(new FluidStack(fluid,1000));
    }
    public static void register(){
        for(String name:GemItem.DEFINITIONS.keySet())for(String kind:java.util.List.of("dun","colourblind"))
            ItemProperties.register(AA.item(name),AA.id(kind),(stack,level,owner,seed)->state(kind,stack)?1:0);
        for(String kind:java.util.List.of("linked","empty_mode"))
            ItemProperties.register(AA.item("radiant_amphora"),AA.id(kind),(stack,level,owner,seed)->state(kind,stack)?1:0);
    }
}
