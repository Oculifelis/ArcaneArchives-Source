package com.aranaira.arcanearchives;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=AA.ID)
public final class ArchiveProgression{
    private static void giveBook(Player player,boolean bookshelf){
        if(player.level().isClientSide||!NetworkData.get(player.level()).grantBook(player.getUUID()))return;
        ItemStack book=new ItemStack(AA.item("tome_arcana"));if(!player.getInventory().add(book))player.drop(book,false);
        player.sendSystemMessage(Component.translatable("arcanearchives.message.book_received."+(bookshelf?"bookshelf":"resonator")));
        player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.WOOL_FALL,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)public static void bookshelf(BlockEvent.BreakEvent event){
        if(ArchiveConfig.BOOK_BOOKSHELF.get()&&event.getState().is(Blocks.BOOKSHELF))giveBook(event.getPlayer(),true);
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event){
        if(ArchiveConfig.BOOK_RESONATOR.get()&&event.getCrafting().is(AA.item("radiant_resonator")))giveBook(event.getEntity(),false);
    }
}
