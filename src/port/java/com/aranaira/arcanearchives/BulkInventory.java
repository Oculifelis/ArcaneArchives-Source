package com.aranaira.arcanearchives;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.item.*;
import net.neoforged.neoforge.transfer.transaction.*;

/** Canonical modern storage: transactions roll back both contents and reference selection. */
public class BulkInventory extends ItemStacksResourceHandler implements IItemHandler {
    private final ArchiveBlockEntity parent;
    private final IItemHandler compat=IItemHandler.of(this);
    public BulkInventory(ArchiveBlockEntity p,int slots){super(slots);parent=p;}
    public int getSlots(){return size();}
    public int getSlotLimit(int index){return parent.kind().equals("radiant_trove")?32768*(parent.upgrades+1):parent.kind().equals("radiant_chest")?64*ArchiveConfig.CHEST_MULTIPLIER.get():64;}
    public boolean acceptsNew(){return !parent.noNew;}
    public ItemStack getStackInSlot(int index){return stacks.get(index);}
    public void setStackInSlot(int index,ItemStack stack){set(index,ItemResource.of(stack),stack.getCount());}
    @Override protected int getCapacity(int index,ItemResource resource){return parent.kind().equals("radiant_trove")?getSlotLimit(index):Math.min(getSlotLimit(index),resource.isEmpty()?getSlotLimit(index):resource.getMaxStackSize()*(parent.kind().equals("radiant_chest")?ArchiveConfig.CHEST_MULTIPLIER.get():1));}
    public boolean isItemValid(int index,ItemStack stack){return isValid(index,ItemResource.of(stack));}
    @Override public boolean isValid(int index,ItemResource resource){return !parent.busy&&(!parent.kind().equals("radiant_trove")||parent.reference.isEmpty()||resource.matches(parent.reference));}
    @Override protected void onContentsChanged(int index,ItemStack before){
        if(parent.kind().equals("radiant_trove")){
            ItemStack after=getStackInSlot(0);
            if(!after.isEmpty())parent.reference=after.copyWithCount(1);
            else if(!parent.locked)parent.reference=ItemStack.EMPTY;
        }
        parent.changed();
    }
    @Override public int insert(int index,ItemResource resource,int amount,TransactionContext tx){
        int inserted=super.insert(index,resource,amount,tx);
        // Voiding is a sink. No state changes occur for discarded overflow, including in aborted simulations.
        return parent.voiding&&parent.kind().equals("radiant_trove")&&resource.matches(getStackInSlot(index))?amount:inserted;
    }
    @Override public int extract(int index,ItemResource resource,int amount,TransactionContext tx){return parent.busy?0:super.extract(index,resource,amount,tx);}
    public ItemStack insertItem(int index,ItemStack stack,boolean simulate){return compat.insertItem(index,stack,simulate);}
    public ItemStack extractItem(int index,int amount,boolean simulate){return compat.extractItem(index,amount,simulate);}
    public CompoundTag save(){
        CompoundTag tag=new CompoundTag();ListTag list=new ListTag();
        for(int i=0;i<size();i++){ItemStack s=getStackInSlot(i);if(s.isEmpty())continue;
            CompoundTag entry=new CompoundTag();entry.putInt("Slot",i);entry.putInt("Amount",s.getCount());
            entry.put("Stack",StackData.save(s.copyWithCount(1),parent.registries()));list.add(entry);}
        tag.put("Items",list);return tag;
    }
    public void load(CompoundTag tag){
        for(int i=0;i<size();i++)stacks.set(i,ItemStack.EMPTY);
        ListTag list=tag.getListOrEmpty("Items");
        for(int i=0;i<list.size();i++){CompoundTag entry=list.getCompoundOrEmpty(i);int slot=entry.getIntOr("Slot",-1);if(slot<0||slot>=size())continue;
            ItemStack s=StackData.load(entry.getCompoundOrEmpty("Stack"),parent.registries());
            s.setCount(Math.max(0,entry.getIntOr("Amount",0)));stacks.set(slot,s);}
    }
}
