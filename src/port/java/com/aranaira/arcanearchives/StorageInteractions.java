package com.aranaira.arcanearchives;

import java.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Legacy Trove clicks. Timing belongs to the block, rather than the held stack. */
public final class StorageInteractions {
    private static final Map<ArchiveBlockEntity,Clicks> clicks=Collections.synchronizedMap(new WeakHashMap<>());
    private static final class Clicks { UUID player;long deposit,extract; }
    private static Clicks clicks(ArchiveBlockEntity tile){return clicks.computeIfAbsent(tile,key->new Clicks());}
    public static void deposit(ArchiveBlockEntity tile,Player player,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND||tile.getLevel().isClientSide||!tile.allowed(player))return;
        ItemStack held=player.getMainHandItem(),reference=tile.reference;
        if(held.isEmpty()&&tile.inventory.getStackInSlot(0).isEmpty())return;
        if(reference.isEmpty())reference=held.copyWithCount(1);
        var click=clicks(tile);long now=System.currentTimeMillis();
        boolean twice=player.getUUID().equals(click.player)&&now-click.deposit<=800;
        click.player=player.getUUID();click.deposit=now;
        if(!held.isEmpty()&&ItemStack.isSameItemSameTags(reference,held)){
            var rest=tile.inventory.insertItem(0,held,false);player.setItemInHand(hand,rest);
            if(!rest.isEmpty()){full(player);player.containerMenu.broadcastChanges();return;}
        }else if(!held.isEmpty()&&!twice)return;
        if(twice){
            for(int i=0;i<36;i++){
                var candidate=player.getInventory().getItem(i);
                if(candidate.isEmpty()||!ItemStack.isSameItemSameTags(reference,candidate))continue;
                var rest=tile.inventory.insertItem(0,candidate,false);player.getInventory().setItem(i,rest);
                if(!rest.isEmpty()){full(player);break;}
            }
        }
        player.getInventory().setChanged();player.containerMenu.broadcastChanges();
    }
    private static void full(Player player){player.displayClientMessage(net.minecraft.network.chat.Component.translatable("arcanearchives.error.trove_insertion_failed.full"),true);}
    public static boolean canExtract(ArchiveBlockEntity tile,Player player){
        var hit=player.pick(6,1,false);var facing=tile.getBlockState().getValue(ArchiveBlock.FACING);
        return hit instanceof net.minecraft.world.phys.BlockHitResult block&&block.getBlockPos().equals(tile.getBlockPos())&&(block.getDirection()==net.minecraft.core.Direction.UP||!facing.getAxis().isVertical()&&block.getDirection()==facing.getCounterClockWise());
    }
    public static void extract(ArchiveBlockEntity tile,Player player){
        if(tile.getLevel().isClientSide||!tile.allowed(player))return;
        var held=player.getMainHandItem();
        if(player.isShiftKeyDown()&&held.getItem() instanceof ArchiveItem item&&Set.of("scepter_revelation","scepter_manipulation").contains(item.name))return;
        var click=clicks(tile);long now=System.currentTimeMillis();if(now-click.extract<150)return;click.extract=now;
        var preview=tile.inventory.extractItem(0,1,true);if(preview.isEmpty())return;
        boolean single=NetworkData.get(player.level()).trovesDispense(tile.owner);
        int amount=player.isShiftKeyDown()?(single?preview.getMaxStackSize():1):(single?1:preview.getMaxStackSize());
        var out=tile.inventory.extractItem(0,amount,false);
        if(!player.getInventory().add(out))player.drop(out,false);
        player.getInventory().setChanged();player.containerMenu.broadcastChanges();
    }
}
