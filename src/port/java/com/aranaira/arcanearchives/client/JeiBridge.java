package com.aranaira.arcanearchives.client;
import java.util.function.*;
public final class JeiBridge {
    public static Supplier<String> read=()->null;
    public static Consumer<String> write=text->{};
    public static Supplier<net.minecraft.world.item.ItemStack> hover=()->net.minecraft.world.item.ItemStack.EMPTY;
}
