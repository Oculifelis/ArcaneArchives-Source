package com.aranaira.arcanearchives;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=AA.ID)
public final class ArchiveInteractionEvents {
    @SubscribeEvent public static void leftClick(PlayerInteractEvent.LeftClickBlock event){
        var player=event.getEntity();if(player.level().isClientSide)return;
        var held=player.getMainHandItem();
        if(held.is(AA.item("raw_quartz")))QuartzInteractions.chip(player,event.getPos(),event.getFace()==null?net.minecraft.core.Direction.UP:event.getFace());
        else if(held.is(AA.item("radiant_amphora"))&&player.isShiftKeyDown())Amphora.use(player,held);
    }
    @SubscribeEvent public static void rightClick(PlayerInteractEvent.RightClickBlock event){
        if(!event.getEntity().level().isClientSide)com.aranaira.arcanearchives.integration.BotaniaBridge.interact(event.getEntity(),event.getPos());
    }
}
