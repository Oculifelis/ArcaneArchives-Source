package com.aranaira.arcanearchives;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
public final class ArchivePlatform {
    public static void init(IEventBus bus){bus.addListener(ArchivePlatform::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){
        event.registerItem(Capabilities.Fluid.ITEM,(stack,context)->new AmphoraHandler(stack,context),AA.item("radiant_amphora"));
        event.registerBlockEntity(Capabilities.Item.BLOCK,AA.ARCHIVE.get(),(tile,side)->tile.kind().equals("brazier_of_hoarding")?tile.router:java.util.Set.of("radiant_chest","radiant_trove","gemcutters_table","radiant_crafting_table").contains(tile.kind())?tile.inventory:null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK,AA.ARCHIVE.get(),(tile,side)->tile.kind().equals("radiant_tank")?tile.tank:null);
    }
}
