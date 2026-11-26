package com.aranaira.arcanearchives;
import net.minecraftforge.common.ForgeConfigSpec;
/** Defaults are taken from the pinned ServerSideConfig, not from outdated guide text. */
public final class ArchiveConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue RESONATOR_LIMIT,RESONATOR_TICKS,CHEST_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue SOUNDS,MANIFEST_PRESENCE,BOOK_BOOKSHELF,BOOK_RESONATOR;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> MUNCHSTONE_FOODS;
    static{
        ForgeConfigSpec.Builder b=new ForgeConfigSpec.Builder();
        RESONATOR_LIMIT=b.comment("Maximum resonators in one shared archive network.").defineInRange("resonatorLimit",3,1,1024);
        RESONATOR_TICKS=b.comment("Online-owner ticks required to grow one raw quartz cluster.").defineInRange("resonatorTicks",6000,1,12000000);
        CHEST_MULTIPLIER=b.comment("Multiplier on each Radiant Chest slot's normal stack size. Existing over-capacity contents are retained.").defineInRange("chestMultiplier",4,1,8);
        MANIFEST_PRESENCE=b.comment("Require a Manifest in the player's inventory for its key binding.").define("manifestPresence",true);
        BOOK_BOOKSHELF=b.define("bookFromBookshelf",true);BOOK_RESONATOR=b.define("bookFromResonator",true);
        MUNCHSTONE_FOODS=b.comment("Munchstone edible blocks/tags and food restored, separated by a comma. Prefix block tags with #.")
                .defineList("munchstoneFoods",java.util.List.of("#minecraft:logs,4","#minecraft:leaves,2","#minecraft:saplings,2","#minecraft:flowers,2","#minecraft:crops,4",
                    "minecraft:hay_block,15","minecraft:melon,15","minecraft:pumpkin,15","minecraft:jack_o_lantern,16","minecraft:brown_mushroom_block,8","minecraft:red_mushroom_block,8",
                    "minecraft:brown_mushroom,4","minecraft:red_mushroom,4","minecraft:nether_wart_block,15","minecraft:chorus_flower,6","minecraft:chorus_plant,6","minecraft:cactus,6",
                    "minecraft:cocoa,4","minecraft:dead_bush,2","minecraft:pumpkin_stem,4","minecraft:melon_stem,4","minecraft:sugar_cane,4","minecraft:short_grass,1","minecraft:grass,1",
                    "minecraft:tall_grass,2","minecraft:large_fern,2","minecraft:vine,2","minecraft:lily_pad,4"),v->v instanceof String text&&text.matches("#?[a-z0-9_.-]+:[a-z0-9_./-]+,\\s*[1-9][0-9]*"));
        SOUNDS=b.define("sounds",true);SPEC=b.build();
    }
}
