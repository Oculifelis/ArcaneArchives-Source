package com.aranaira.arcanearchives;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Version-specific serialization lives here, never in the routing algorithm. */
public final class StackData {
    public static CompoundTag save(ItemStack stack, Level level) { return stack.save(new CompoundTag()); }
    public static ItemStack load(CompoundTag tag, Level level) { return ItemStack.of(tag); }
    public static CompoundTag data(ItemStack stack) { return stack.getOrCreateTag().copy(); }
    public static void data(ItemStack stack, CompoundTag tag) { stack.setTag(tag); }
    public static String customName(ItemStack stack){return stack.hasCustomHoverName()?stack.getHoverName().getString():"";}
}
