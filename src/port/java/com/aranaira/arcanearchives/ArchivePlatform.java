package com.aranaira.arcanearchives;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
public final class ArchivePlatform {
    public static void init(IEventBus bus){bus.addListener(ArchivePlatform::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){
        event.registerItem(Capabilities.FluidHandler.ITEM,(stack,context)->new AmphoraHandler(stack),AA.item("radiant_amphora"));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,AA.ARCHIVE.get(),(tile,side)->java.util.Set.of("radiant_chest","radiant_trove","brazier_of_hoarding","gemcutters_table","radiant_crafting_table").contains(tile.kind())?tile.itemHandler():null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,AA.ARCHIVE.get(),(tile,side)->tile.kind().equals("radiant_tank")?tile.tank:null);
    }
}
