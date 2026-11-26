package com.aranaira.arcanearchives;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

public final class MunchstoneFood {
    public static int value(BlockState state){
        for(String entry:ArchiveConfig.MUNCHSTONE_FOODS.get()){
            String[] parts=entry.split(",");boolean tag=parts[0].startsWith("#");
            var id=ResourceLocation.tryParse(tag?parts[0].substring(1):parts[0]);if(id==null)continue;
            if(tag?state.is(TagKey.create(Registries.BLOCK,id)):id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock())))try{return Math.min(1000,Integer.parseInt(parts[1].trim()));}catch(NumberFormatException ignored){}
        }
        return 0;
    }
}
