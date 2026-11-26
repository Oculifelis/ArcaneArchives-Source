package com.aranaira.arcanearchives;

import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

public final class MunchstoneFood {
    private static final java.util.Map<String,Integer> THAUMATURGE = java.util.Map.of(
            "sapling_greatwood",2,"sapling_silverwood",3,"log_greatwood",4,
            "log_silverwood",5,"leaves_greatwood",2,"leaves_silverwood",3,
            "cinderpearl",3,"shimmerleaf",3,"vishroom",3);
    public static int value(BlockState state){
        var block=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        // Exact pack entries can override the translated legacy defaults,
        // including zero to disable one food. Generic vanilla tags follow them.
        if(block.getNamespace().equals("thaumaturge")&&THAUMATURGE.containsKey(block.getPath())){
            for(String entry:ArchiveConfig.MUNCHSTONE_FOODS.get()){
                String[] parts=entry.split(",");
                if(parts.length==2&&parts[0].trim().equals(block.toString()))
                    try{return Math.min(1000,Integer.parseInt(parts[1].trim()));}catch(NumberFormatException ignored){}
            }
            return THAUMATURGE.get(block.getPath());
        }
        for(String entry:ArchiveConfig.MUNCHSTONE_FOODS.get()){
            String[] parts=entry.split(",");boolean tag=parts[0].startsWith("#");
            var id=ResourceLocation.tryParse(tag?parts[0].substring(1):parts[0]);if(id==null||parts.length<2)continue;
            if(tag?state.is(TagKey.create(Registries.BLOCK,id)):id.equals(block))
                try{return Math.min(1000,Integer.parseInt(parts[1].trim()));}catch(NumberFormatException ignored){}
        }
        return 0;
    }
}
