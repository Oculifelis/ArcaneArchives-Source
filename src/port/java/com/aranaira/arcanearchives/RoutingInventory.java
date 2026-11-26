package com.aranaira.arcanearchives;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
/** A sink, not a container: hopper insertion is routed directly into storage. */
public final class RoutingInventory implements IItemHandler {
    private final ArchiveBlockEntity parent;
    public RoutingInventory(ArchiveBlockEntity p){parent=p;}
    public int getSlots(){return 1;}
    public ItemStack getStackInSlot(int slot){return ItemStack.EMPTY;}
    public int getSlotLimit(int slot){return 64;}
    public boolean isItemValid(int slot,ItemStack stack){return slot==0;}
    public ItemStack extractItem(int slot,int amount,boolean simulate){return ItemStack.EMPTY;}
    public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
        if(slot!=0||parent.getLevel()==null||parent.getLevel().isClientSide||parent.getLevel().hasNeighborSignal(parent.getBlockPos()))return stack;
        return NetworkData.get(parent.getLevel()).insertFromBrazier(parent,stack,simulate);
    }
}
