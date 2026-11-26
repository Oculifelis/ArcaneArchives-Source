package com.aranaira.arcanearchives;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
public final class StackData {
    public static CompoundTag save(ItemStack s,Level l){return save(s,l.registryAccess());}
    public static CompoundTag save(ItemStack s,HolderLookup.Provider r){return s.isEmpty()?new CompoundTag():(CompoundTag)s.save(r);}
    public static ItemStack load(CompoundTag t,Level l){return load(t,l.registryAccess());}
    public static ItemStack load(CompoundTag t,HolderLookup.Provider r){return ItemStack.parseOptional(r,t);}
    public static CompoundTag data(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static void data(ItemStack s,CompoundTag t){s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    public static String customName(ItemStack s){var name=s.get(DataComponents.CUSTOM_NAME);return name==null?"":name.getString();}
}
