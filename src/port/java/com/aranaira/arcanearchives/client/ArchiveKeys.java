package com.aranaira.arcanearchives.client;
import com.aranaira.arcanearchives.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber(modid=AA.ID,value=Dist.CLIENT)
public final class ArchiveKeys{
    private static net.minecraft.client.player.LocalPlayer configuredPlayer;
    public static final String CATEGORY="arcanearchives.gui.keygroup";
    public static final KeyMapping MANIFEST=new KeyMapping("arcanearchives.gui.keybinds.manifest",InputConstants.UNKNOWN.getValue(),CATEGORY);
    public static final KeyMapping SOCKET=new KeyMapping("arcanearchives.gui.keybinds.socket",InputConstants.UNKNOWN.getValue(),CATEGORY);
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        
        ArchiveViews.tick();
        var mc=Minecraft.getInstance();if(mc.player==null){configuredPlayer=null;return;}if(configuredPlayer!=mc.player){configuredPlayer=mc.player;ClientPreferences.sync();}if(mc.screen!=null)return;
        while(MANIFEST.consumeClick())ArchiveNetwork.action(0,mc.player.containerMenu.containerId,-1);
        while(SOCKET.consumeClick())if(ArchiveLegacyConfig.arsenal())ArchiveNetwork.action(1,mc.player.containerMenu.containerId,-1);
    }
    @SubscribeEvent public static void screenKey(ScreenEvent.KeyPressed.Pre event){
        var mc=Minecraft.getInstance();if(mc.player==null||!MANIFEST.matches(event.getKeyCode(),event.getScanCode()))return;
        var ingredient=JeiBridge.hover.get();if(!ingredient.isEmpty()){ClientSearch.track(ingredient);event.setCanceled(true);return;}
        if(event.getScreen() instanceof AbstractContainerScreen<?> screen){
            var slot=screen.getSlotUnderMouse();if(slot==null||slot.getItem().isEmpty())return;
            ArchiveNetwork.action(2,mc.player.containerMenu.containerId,slot.index);event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void highlight(ScreenEvent.Render.Post event){
        if(!(event.getScreen() instanceof AbstractContainerScreen<?> screen))return;
        for(var slot:screen.getMenu().slots)if(ArchiveViews.matches(slot.getItem())){
            int x=screen.getGuiLeft()+slot.x,y=screen.getGuiTop()+slot.y;
            event.getGuiGraphics().fill(x,y,x+16,y+16,0x667be7ff);
        }
    }
}
