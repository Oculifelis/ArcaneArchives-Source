package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid=AA.ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ArchiveClient {
    @SubscribeEvent public static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event){event.register((stack,index)->index==1?ArchiveItemVisuals.fluidColor(stack):0xffffffff,AA.item("radiant_amphora"));}
    @SubscribeEvent public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event){event.register(ArchiveKeys.MANIFEST);event.register(ArchiveKeys.SOCKET);}
    @SubscribeEvent public static void setup(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        ViewMessages.receiver=ArchiveViews::accept;ViewMessages.blockTick=ArchiveAudio::tick;ClientPreferences.load();
        ArchiveItemVisuals.register();event.register(AA.STORAGE_MENU.get(),ArchiveClient::storage);event.register(AA.MANIFEST_MENU.get(),ManifestScreen::new);event.register(AA.SOCKET_MENU.get(),SocketScreen::new);event.register(AA.CRAFTING_MENU.get(),CraftingScreen::new);event.register(AA.CHARM_MENU.get(),CharmScreen::new);event.register(AA.UPGRADE_MENU.get(),UpgradeScreen::new);net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(AA.ARCHIVE.get(),ArchiveStorageRenderer::new);
    }
    private static AbstractContainerScreen<ArchiveMenu> storage(ArchiveMenu menu,Inventory inv,Component title){return menu.station==3?new BrazierScreen(menu,inv,title):new StorageScreen(menu,inv,title);}
    public static final class StorageScreen extends LegacyScreens.Storage{public StorageScreen(ArchiveMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class ManifestScreen extends LegacyScreens.Manifest{public ManifestScreen(ManifestMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class SocketScreen extends LegacyScreens.Socket{public SocketScreen(SocketMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class CraftingScreen extends LegacyScreens.Crafting{public CraftingScreen(PersistentCraftingMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class CharmScreen extends LegacyScreens.Charm{public CharmScreen(DevouringCharmMenu m,Inventory i,Component t){super(m,i,t);}}
    public static final class UpgradeScreen extends LegacyScreens.Upgrades{public UpgradeScreen(UpgradeMenu m,Inventory i,Component t){super(m,i,t);}}
}
