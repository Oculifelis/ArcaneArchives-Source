package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=AA.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ArchiveClient {
    @SubscribeEvent public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event){event.register((stack,index)->index==1?ArchiveItemVisuals.fluidColor(stack):0xffffffff,AA.item("radiant_amphora"));}
    @SubscribeEvent public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event){event.register(ArchiveKeys.MANIFEST);event.register(ArchiveKeys.SOCKET);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        ViewMessages.receiver=ArchiveViews::accept;ViewMessages.blockTick=ArchiveAudio::tick;ClientPreferences.load();
        event.enqueueWork(()->{ArchiveItemVisuals.register();MenuScreens.register(AA.STORAGE_MENU.get(),ArchiveClient::storage);MenuScreens.register(AA.MANIFEST_MENU.get(),ManifestScreen::new);MenuScreens.register(AA.SOCKET_MENU.get(),SocketScreen::new);MenuScreens.register(AA.CRAFTING_MENU.get(),CraftingScreen::new);MenuScreens.register(AA.CHARM_MENU.get(),CharmScreen::new);MenuScreens.register(AA.UPGRADE_MENU.get(),UpgradeScreen::new);net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(AA.ARCHIVE.get(),ArchiveStorageRenderer::new);});
    }
    private static AbstractContainerScreen<ArchiveMenu> storage(ArchiveMenu menu,Inventory inv,Component title){return menu.station==3?new BrazierScreen(menu,inv,title):new StorageScreen(menu,inv,title);}
    public static final class StorageScreen extends LegacyScreens.Storage{public StorageScreen(ArchiveMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class ManifestScreen extends LegacyScreens.Manifest{public ManifestScreen(ManifestMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class SocketScreen extends LegacyScreens.Socket{public SocketScreen(SocketMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class CraftingScreen extends LegacyScreens.Crafting{public CraftingScreen(PersistentCraftingMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class CharmScreen extends LegacyScreens.Charm{public CharmScreen(DevouringCharmMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class UpgradeScreen extends LegacyScreens.Upgrades{public UpgradeScreen(UpgradeMenu m,Inventory i,Component t){super(m,i,t);}}
}
