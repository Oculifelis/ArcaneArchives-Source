package com.aranaira.arcanearchives;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;

/** In-place upgrades preserve the native chest's contents before it is replaced. */
public final class QuartzInteractions {
    public static InteractionResult convert(UseOnContext context){
        Player player=context.getPlayer();if(player==null||!player.isShiftKeyDown())return InteractionResult.PASS;
        var level=context.getLevel();var pos=context.getClickedPos();var state=level.getBlockState(pos);var held=context.getItemInHand();
        boolean chest=state.getBlock() instanceof ChestBlock,table=state.getBlock() instanceof CraftingTableBlock;
        if(!chest&&!table)return InteractionResult.PASS;
        if(!player.mayUseItemAt(pos,context.getClickedFace(),held)||!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        Container contents=chest?ChestBlock.getContainer((ChestBlock)state.getBlock(),state,level,pos,true):null;
        if(chest&&contents==null)return InteractionResult.FAIL;
        List<ItemStack> saved=new ArrayList<>();
        if(contents!=null){for(int i=0;i<contents.getContainerSize();i++)saved.add(contents.getItem(i).copy());contents.clearContent();}
        var replacement=AA.block(chest?"radiant_chest":"radiant_crafting_table").defaultBlockState().setValue(ArchiveBlock.FACING,player.getDirection().getOpposite());
        if(!level.setBlockAndUpdate(pos,replacement)){
            if(contents!=null)for(int i=0;i<saved.size();i++)contents.setItem(i,saved.get(i));
            return InteractionResult.FAIL;
        }
        if(level.getBlockEntity(pos) instanceof ArchiveBlockEntity tile){
            tile.owner=player.getUUID();tile.noNew=chest&&NetworkData.get(level).defaultNoNew(player.getUUID());
            for(var stack:saved){var rest=net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(tile.inventory,stack,false);if(!rest.isEmpty())Block.popResource(level,pos.above(),rest);}
            tile.changed();NetworkData.get(level).register(tile);
        }else for(var stack:saved)if(!stack.isEmpty())Block.popResource(level,pos.above(),stack);
        if(!player.getAbilities().instabuild||table)held.shrink(1);
        player.containerMenu.broadcastChanges();return InteractionResult.CONSUME;
    }
    /** Source probabilities: 20% consumes quartz for 8..23 slivers; 40% gives one. */
    public static ItemEntity chip(Player player,net.minecraft.core.BlockPos pos,net.minecraft.core.Direction face){
        var level=player.level();if(level.isClientSide||!player.getMainHandItem().is(AA.item("raw_quartz")))return null;
        var random=level.getRandom();int roll=random.nextInt(5),amount=roll==0?random.nextInt(16)+8:roll<=2?1:0;if(amount==0)return null;
        if(roll==0)player.getMainHandItem().shrink(1);
        var at=net.minecraft.world.phys.Vec3.atCenterOf(pos).add(face.getStepX()*.51,face.getStepY()*.51,face.getStepZ()*.51);
        var drop=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(AA.item("quartz_sliver"),amount));
        drop.setDeltaMovement(random.nextFloat()*.4f-.2f,random.nextFloat()*.2f+.2f,random.nextFloat()*.4f-.2f);return level.addFreshEntity(drop)?drop:null;
    }
}
