package com.aranaira.arcanearchives;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
/** Routing participates in the caller's transaction across all destination containers. */
public final class RoutingInventory implements ResourceHandler<ItemResource>,IItemHandler {
    private final ArchiveBlockEntity parent;
    private final IItemHandler compat=IItemHandler.of(this);
    public RoutingInventory(ArchiveBlockEntity p){parent=p;}
    public int size(){return 1;}
    public int getSlots(){return 1;}
    public ItemResource getResource(int i){return ItemResource.EMPTY;}
    public long getAmountAsLong(int i){return 0;}
    public long getCapacityAsLong(int i,ItemResource resource){return Integer.MAX_VALUE;}
    public boolean isValid(int i,ItemResource resource){return i==0;}
    public int extract(int i,ItemResource r,int amount,TransactionContext tx){return 0;}
    public ItemStack getStackInSlot(int i){return ItemStack.EMPTY;}
    public int getSlotLimit(int i){return 64;}
    public boolean isItemValid(int i,ItemStack s){return i==0;}
    public ItemStack extractItem(int i,int amount,boolean simulate){return ItemStack.EMPTY;}
    public ItemStack insertItem(int i,ItemStack s,boolean simulate){return compat.insertItem(i,s,simulate);}
    public int insert(int index,ItemResource resource,int amount,TransactionContext tx){
        if(index!=0||parent.owner==null||!(parent.getLevel() instanceof ServerLevel level)||level.hasNeighborSignal(parent.getBlockPos()))return 0;
        NetworkData data=NetworkData.get(level);
        var targets=data.brazierTargets(parent,resource.toStack(1));
        int remaining=amount;
        for(var tile:targets){
            var target=tile.inventory;
            for(boolean existing:new boolean[]{true,false})for(int i=0;i<target.size()&&remaining>0;i++){
                if(target.getResource(i).isEmpty()==existing||tile.kind().equals("gemcutters_table")&&!resource.matches(target.getStackInSlot(i)))continue;
                remaining-=target.insert(i,resource,remaining,tx);
            }
        }
        return amount-remaining;
    }
}
