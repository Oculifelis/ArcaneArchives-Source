package com.aranaira.arcanearchives;
import net.minecraft.world.item.ItemStack;

/** Short-lived collation key; counts are deliberately excluded from item identity. */
final class StackIdentity{
    private final ItemStack stack;
    StackIdentity(ItemStack stack){this.stack=stack;}
    @Override public int hashCode(){return 31*System.identityHashCode(stack.getItem())+java.util.Objects.hashCode(stack.getTag());}
    @Override public boolean equals(Object other){return other instanceof StackIdentity key&&ItemStack.isSameItemSameTags(stack,key.stack);}
}
