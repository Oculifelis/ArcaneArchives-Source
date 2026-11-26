package com.aranaira.arcanearchives;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;

/** Client callback is installed only on the physical client. Payloads never execute commands. */
public final class ViewMessages {
    public static Consumer<CompoundTag> receiver = tag -> {};
    public static Consumer<ArchiveBlockEntity> blockTick=tile->{};
    public static java.util.function.BiConsumer<java.util.List<GemcutterRecipes.Recipe>,java.util.List<GemcutterRecipes.Recipe>> recipes=(oldRecipes,newRecipes)->{};
    private ViewMessages() {}
}
