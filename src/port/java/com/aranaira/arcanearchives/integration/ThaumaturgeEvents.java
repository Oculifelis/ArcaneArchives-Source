package com.aranaira.arcanearchives.integration;

import com.aranaira.arcanearchives.AA;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = AA.ID)
public final class ThaumaturgeEvents {
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() && event.getUseBlock() != TriState.FALSE
                && ModList.get().isLoaded("thaumaturge")) {
            ThaumaturgeBridge.interact(event.getEntity(), event.getPos());
        }
    }
}
