package com.aranaira.arcanearchives;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=AA.ID)
public final class ArchiveRecipeEvents {
    private static net.minecraft.server.MinecraftServer configured;
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event){
        if(event.getPlayer()!=null)ArchiveRecipes.sync(event.getPlayer());else for(var player:event.getPlayerList().getPlayers())ArchiveRecipes.sync(player);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END)return;
        if(configured!=event.getServer()){configured=event.getServer();ArchiveRecipeConfig.load(configured.overworld());}
        ArchiveRecipes.flush(event.getServer());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){configured=null;ArchiveRecipes.resetServer();}
}
