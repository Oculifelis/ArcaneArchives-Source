package com.aranaira.arcanearchives;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Retains native slot validation while applying the original crafting/input layout. */
public final class PositionedSlot extends Slot {
    private final Slot original;
    public PositionedSlot(Slot original,int x,int y){super(original.container,original.getSlotIndex(),x,y);this.original=original;index=original.index;}
    @Override public boolean mayPlace(ItemStack s){return original.mayPlace(s);}
    @Override public boolean mayPickup(Player p){return original.mayPickup(p);}
    @Override public int getMaxStackSize(){return original.getMaxStackSize();}
    @Override public int getMaxStackSize(ItemStack s){return original.getMaxStackSize(s);}
    @Override public ItemStack remove(int count){return original.remove(count);}
    @Override public void onTake(Player p,ItemStack stack){original.onTake(p,stack);}
    @Override public void onQuickCraft(ItemStack oldStack,ItemStack newStack){original.onQuickCraft(oldStack,newStack);}
}
