package com.aranaira.arcanearchives;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(AA.ID)
public final class AA {
    public static final String ID = "arcanearchives";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ID);
    public static final Map<String, Supplier<Block>> BLOCK_MAP = new LinkedHashMap<>();
    public static final Map<String, Supplier<Item>> ITEM_MAP = new LinkedHashMap<>();
    public static final Set<String> MACHINES = Set.of("radiant_chest","radiant_trove","radiant_tank","radiant_resonator",
            "gemcutters_table","radiant_crafting_table","monitoring_crystal","brazier_of_hoarding","lectern_manifest");
    static {
        for (String name : List.of("storage_raw_quartz","storage_shaped_quartz","radiant_chest","radiant_trove",
                "radiant_crafting_table","radiant_lantern","radiant_resonator","raw_quartz_cluster","quartz_sliver",
                "gemcutters_table","monitoring_crystal","radiant_tank","brazier_of_hoarding","lectern_manifest")) {
            Supplier<Block> block = BLOCKS.register(name, () -> {
                BlockBehaviour.Properties p = BlockBehaviour.Properties.of().strength(2.0f).noOcclusion()
                        .sound(SoundType.GLASS).lightLevel(s -> name.equals("radiant_resonator") ? 0 : 15);
                if (java.util.Set.of("raw_quartz_cluster","brazier_of_hoarding").contains(name) && net.neoforged.fml.ModList.get().isLoaded("thaumaturge")) return com.aranaira.arcanearchives.integration.ThaumaturgeBridge.block(name, name.equals("raw_quartz_cluster") ? p.strength(1.4f).requiresCorrectToolForDrops() : p);
                if (MACHINES.contains(name)) return new ArchiveBlock(name, p);
                if (name.equals("raw_quartz_cluster")) return new QuartzCluster(p.strength(1.4f).requiresCorrectToolForDrops());
                if(name.equals("quartz_sliver")||name.equals("radiant_lantern"))return new DirectionalDecoration(name,p);
                return new Block(p);
            });
            BLOCK_MAP.put(name, block);
            ITEM_MAP.put(name, ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
        }
        for (String name : List.of("raw_quartz","shaped_quartz","manifest","radiant_amphora",
                "scepter_revelation","scepter_manipulation","tome_arcana","letter_invitation","letter_resignation",
                "writ_expulsion","containment_field","matrix_brace","material_interface","radiant_dust",
                "scintillating_inlay","devouring_charm","gemsocket","radiant_key","debugorb")) {
            ITEM_MAP.put(name, ITEMS.register(name, () -> new ArchiveItem(name, new Item.Properties()
                    .stacksTo(Set.of("raw_quartz","shaped_quartz","containment_field","matrix_brace",
                            "material_interface","radiant_dust","scintillating_inlay","devouring_charm").contains(name) ? 64 : 1))));
        }
        for(String name:GemItem.DEFINITIONS.keySet())ITEM_MAP.put(name,ITEMS.register(name,()->new GemItem(name,
                name.equals("mountaintear")?new Item.Properties().stacksTo(1).fireResistant():new Item.Properties().stacksTo(1))));
        {String name="fake_air";BLOCK_MAP.put(name,BLOCKS.register(name,()->new FleetingAir(BlockBehaviour.Properties.of().strength(-1))));}
    }
    public static final Supplier<BlockEntityType<ArchiveBlockEntity>> ARCHIVE = TILES.register("archive", () ->
            BlockEntityType.Builder.of(ArchiveBlockEntity::new, MACHINES.stream().map(n -> BLOCK_MAP.get(n).get()).toArray(Block[]::new)).build(null));
    public static final Supplier<MenuType<ArchiveMenu>> STORAGE_MENU = MENUS.register("storage", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,data)->ArchiveMenu.client(id,inv,data.readVarInt())));
    public static final Supplier<MenuType<ManifestMenu>> MANIFEST_MENU = MENUS.register("manifest", () -> new MenuType<>(ManifestMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<SocketMenu>> SOCKET_MENU = MENUS.register("socket", () -> new MenuType<>(SocketMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<PersistentCraftingMenu>> CRAFTING_MENU = MENUS.register("crafting", () -> new MenuType<>(PersistentCraftingMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<DevouringCharmMenu>> CHARM_MENU = MENUS.register("devouring_charm", () -> new MenuType<>(DevouringCharmMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<UpgradeMenu>> UPGRADE_MENU = MENUS.register("upgrades", () -> new MenuType<>(UpgradeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<SoundEvent> RESONATOR_COMPLETE=SOUNDS.register("resonator.complete",()->SoundEvent.createVariableRangeEvent(id("resonator.complete")));
    public static final Supplier<SoundEvent> RESONATOR_LOOP=SOUNDS.register("resonator.loop",()->SoundEvent.createVariableRangeEvent(id("resonator.loop")));
    public static final Supplier<SoundEvent> BRAZIER_ABSORB=SOUNDS.register("brazier.absorb",()->SoundEvent.createVariableRangeEvent(id("brazier.absorb")));
    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.literal("Arcane Archives")).icon(() -> new ItemStack(item("manifest")))
            .displayItems((p, out) -> ITEM_MAP.values().stream().filter(i->ArchiveLegacyConfig.arsenal()||!(i.get() instanceof GemItem)).forEach(i -> out.accept(i.get()))).build());

    public AA(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,ArchiveConfig.SPEC);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,ArchiveLegacyConfig.SPEC);
        BLOCKS.register(bus); ITEMS.register(bus); TILES.register(bus); MENUS.register(bus); TABS.register(bus); SOUNDS.register(bus);
        ArchiveNetwork.init(bus); ArchivePlatform.init(bus);
        bus.addListener(ArchiveDispenser::setup);
        bus.addListener(com.aranaira.arcanearchives.integration.ArchiveProbe::enqueue);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
    public static Item item(String name) { return ITEM_MAP.get(name).get(); }
    public static Block block(String name) { return BLOCK_MAP.get(name).get(); }
}
