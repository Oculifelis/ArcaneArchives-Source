package com.aranaira.arcanearchives;

import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Counts are serialized as ints separately from the native stack codec. */
public class BulkInventory extends ItemStackHandler {
    private final ArchiveBlockEntity parent;
    public BulkInventory(ArchiveBlockEntity parent, int slots) { super(slots); this.parent = parent; }
    @Override public int getSlotLimit(int slot) { return parent.kind().equals("radiant_trove") ? 32768 * (parent.upgrades + 1) : parent.kind().equals("radiant_chest")?64*ArchiveConfig.CHEST_MULTIPLIER.get():64; }
    public boolean acceptsNew() { return !parent.noNew; }
    @Override protected int getStackLimit(int slot, ItemStack stack) {
        return parent.kind().equals("radiant_trove") ? getSlotLimit(slot) : Math.min(getSlotLimit(slot), stack.getMaxStackSize() * (parent.kind().equals("radiant_chest")?ArchiveConfig.CHEST_MULTIPLIER.get():1));
    }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        if (!parent.kind().equals("radiant_trove")) return true;
        return parent.reference.isEmpty() || ItemStack.isSameItemSameComponents(parent.reference, stack);
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if(parent.busy)return stack;
        if (!isItemValid(slot, stack)) return stack;
        ItemStack rest = super.insertItem(slot, stack, simulate);
        if (!simulate && !stack.isEmpty() && rest.getCount() < stack.getCount() && parent.kind().equals("radiant_trove"))
            parent.reference = stack.copyWithCount(1);
        if (parent.voiding && parent.kind().equals("radiant_trove") && !rest.isEmpty()
                && (getStackInSlot(0).isEmpty()||ItemStack.isSameItemSameComponents(getStackInSlot(0),stack))
                && (simulate?getStackInSlot(0).getCount()+stack.getCount()-rest.getCount():getStackInSlot(0).getCount()) >= getSlotLimit(0)) return ItemStack.EMPTY;
        return rest;
    }
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate) { return parent.busy?ItemStack.EMPTY:super.extractItem(slot,amount,simulate); }
    @Override protected void onContentsChanged(int slot) {
        if (parent.kind().equals("radiant_trove")) {
            ItemStack s=getStackInSlot(0);
            if (!s.isEmpty()) parent.reference=s.copyWithCount(1);
            else if(!parent.locked) parent.reference=ItemStack.EMPTY;
        }
        parent.changed();
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag items = new ListTag();
        for (int i = 0; i < getSlots(); i++) {
            ItemStack s = getStackInSlot(i);
            if (s.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i); entry.putInt("Amount", s.getCount());
            entry.put("Stack", StackData.save(s.copyWithCount(1), parent.registries()));
            items.add(entry);
        }
        tag.put("Items", items);
        return tag;
    }
    public void load(CompoundTag tag) {
        for (int i = 0; i < getSlots(); i++) stacks.set(i, ItemStack.EMPTY);
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= getSlots()) continue;
            ItemStack s = StackData.load(entry.getCompound("Stack"), parent.registries());
            s.setCount(Math.max(0,entry.getInt("Amount")));
            stacks.set(slot, s);
        }
    }
}
