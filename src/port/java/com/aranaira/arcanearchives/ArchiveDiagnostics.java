package com.aranaira.arcanearchives;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Shared source tooltip content for native HUDs and optional probe providers. */
public final class ArchiveDiagnostics {
    public static List<Component> lines(ArchiveBlockEntity tile){
        List<Component> lines=new ArrayList<>();var level=tile.getLevel();if(level==null)return lines;
        if(tile.kind().equals("radiant_chest")){
            if(!tile.chestName.isEmpty())lines.add(Component.translatable("arcanearchives.data.tooltip.chest_name").append(" "+tile.chestName).withStyle(ChatFormatting.GOLD));
        }else if(tile.kind().equals("radiant_trove")){
            var stack=tile.inventory.getStackInSlot(0);lines.add((stack.isEmpty()?Component.translatable("arcanearchives.data.tooltip.empty"):Component.translatable("arcanearchives.data.tooltip.trove_count_waila",stack.getCount(),stack.getHoverName())).withStyle(ChatFormatting.GOLD));
        }else if(tile.kind().equals("radiant_resonator")){
            lines.add(Component.translatable("arcanearchives.data.tooltip.resonator_progress").append(" "+Math.min(100,100L*tile.progress/Math.max(1,ArchiveConfig.RESONATOR_TICKS.get()))+"%").withStyle(ChatFormatting.GOLD));
            String key;ChatFormatting color;
            if(!level.isEmptyBlock(tile.getBlockPos().above())){boolean quartz=level.getBlockState(tile.getBlockPos().above()).is(AA.block("raw_quartz_cluster"));key=quartz?"harvestable":"obstruction";color=quartz?ChatFormatting.GOLD:ChatFormatting.RED;}
            else{key=tile.resonating?"resonating":"offline";color=tile.resonating?ChatFormatting.GREEN:ChatFormatting.DARK_RED;}
            lines.add(Component.translatable("arcanearchives.data.tooltip.resonator_status."+key).withStyle(color));
        }
        return lines;
    }
    public static net.minecraft.nbt.CompoundTag snapshot(ArchiveBlockEntity tile){
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Kind",tile.kind());tag.putString("Name",tile.chestName);
        var stack=tile.inventory.getStackInSlot(0);tag.putInt("Count",stack.getCount());tag.put("Stack",StackData.save(stack.copyWithCount(1),tile.getLevel()));
        tag.putInt("Percent",(int)Math.min(100,100L*tile.progress/Math.max(1,ArchiveConfig.RESONATOR_TICKS.get())));
        var level=tile.getLevel();String status=!level.isEmptyBlock(tile.getBlockPos().above())?(level.getBlockState(tile.getBlockPos().above()).is(AA.block("raw_quartz_cluster"))?"harvestable":"obstruction"):(tile.resonating?"resonating":"offline");tag.putString("Status",status);return tag;
    }
    public static List<Component> lines(net.minecraft.nbt.CompoundTag tag,net.minecraft.world.level.Level level){
        var result=new ArrayList<Component>();switch(tag.getString("Kind")){
            case"radiant_chest"->{if(!tag.getString("Name").isEmpty())result.add(Component.translatable("arcanearchives.data.tooltip.chest_name").append(" "+tag.getString("Name")).withStyle(ChatFormatting.GOLD));}
            case"radiant_trove"->{var stack=StackData.load(tag.getCompound("Stack"),level);result.add((stack.isEmpty()?Component.translatable("arcanearchives.data.tooltip.empty"):Component.translatable("arcanearchives.data.tooltip.trove_count_waila",tag.getInt("Count"),stack.getHoverName())).withStyle(ChatFormatting.GOLD));}
            case"radiant_resonator"->{result.add(Component.translatable("arcanearchives.data.tooltip.resonator_progress").append(" "+tag.getInt("Percent")+"%").withStyle(ChatFormatting.GOLD));String status=tag.getString("Status");result.add(Component.translatable("arcanearchives.data.tooltip.resonator_status."+status).withStyle(switch(status){case"resonating"->ChatFormatting.GREEN;case"harvestable"->ChatFormatting.GOLD;case"obstruction"->ChatFormatting.RED;default->ChatFormatting.DARK_RED;}));}
        }return result;
    }
}
